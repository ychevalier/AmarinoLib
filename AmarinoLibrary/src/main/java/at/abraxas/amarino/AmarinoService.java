package at.abraxas.amarino;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import it.gerdavax.easybluetooth.BtSocket;
import it.gerdavax.easybluetooth.LocalDevice;
import it.gerdavax.easybluetooth.ReadyListener;
import it.gerdavax.easybluetooth.RemoteDevice;
import it.gerdavax.easybluetooth.ScanListener;

public class AmarinoService extends Service {

	private static final String TAG = AmarinoService.class.getSimpleName();

	private static final int BUSY = 1;
	private static final int ACTIVE_CONNECTIONS = 2;
	private static final int NO_CONNECTIONS = 3;

	private IBinder mBinder;

	private LocalDevice mLocalDevice;

	private HashMap<String, ConnectedThread> mConnections;

	private int mServiceState;

	@Override
	public void onCreate() {
		super.onCreate();

		mBinder = new AmarinoServiceBinder();

		mServiceState = NO_CONNECTIONS;
		// most ppl will only use one Bluetooth device, thus lets start with capacity 1
		mConnections = new HashMap<String, ConnectedThread>(1);

		IntentFilter filter = new IntentFilter(AmarinoIntent.ACTION_SEND);
		registerReceiver(receiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent == null) {
			// here we might restore our state if we got killed by the system
			// TODO
			return START_STICKY;
		}

		String action = intent.getAction();
		if (action == null) return START_STICKY;

		// someone wants to send data to arduino
		if (action.equals(AmarinoIntent.ACTION_SEND)) {
			forwardDataToArduino(intent);
			return START_NOT_STICKY;
		}

		// publish the state of devices
		if (action.equals(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES)) {
			broadcastConnectedDevicesList();
			return START_NOT_STICKY;
		}

		// publish the state of devices
		if (action.equals(AmarinoIntent.ACTION_GET_NEARBY_DEVICES)) {
			scanForNearbyDevices();
			return START_NOT_STICKY;
		}
		
		/* --- CONNECT and DISCONNECT part --- */
		String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
		if (address == null) {
			Logger.d(TAG, "EXTRA_DEVICE_ADDRESS not found!");
			return START_NOT_STICKY;
		}

		// connect and disconnect operations may take some time
		// we don't want to shutdown our service while it does some work
		mServiceState = BUSY;

		if (!Utils.isCorrectAddressFormat(address)) {
			Logger.d(TAG, getString(R.string.service_address_invalid, address));
			sendConnectionFailed(address);
			shutdownServiceIfNecessary();
		} else {

			if (AmarinoIntent.ACTION_CONNECT.equals(action)) {
				Logger.d(TAG, "ACTION_CONNECT request received");
				connect(address);
			} else if (AmarinoIntent.ACTION_DISCONNECT.equals(action)) {
				Logger.d(TAG, "ACTION_DISCONNECT request received");
				disconnect(address);
			}
		}

		return START_STICKY;
	}

	private void forwardDataToArduino(Intent intent) {

		// intent sent from another app which is not a plugin
		final String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
		if (address == null) {
			Logger.d(TAG, "Data not sent! EXTRA_DEVICE_ADDRESS not set.");
			return;
		}

		String message = MessageBuilder.getMessage(intent);
		if (message == null) return;

		// cutoff leading flag and ACK_FLAG for logger
		Logger.d(TAG, getString(R.string.service_message_to_send, message.substring(1, message.length() - 1)));

		try {
			sendData(address, message.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			// use default encoding as fallback alternative if encoding ISO 8859-1 is not possible
			Logger.d(TAG, "Encoding message using ISO-8859-1 not possible");
			sendData(address, message.getBytes());
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.d(TAG, "Background service stopped");

		// we do only stop our service if no mConnections are active, however Android may kill our service without warning
		// clean up in case service gets killed from the system due to low memory condition
		if (mServiceState == ACTIVE_CONNECTIONS) {
			// TODO save which mConnections are active for recreating later when service is restarted
			for (ConnectedThread t : mConnections.values()) {
				t.cancel();
			}
		}
		unregisterReceiver(receiver);
	}

	private void shutdownService() {
		if (mServiceState == NO_CONNECTIONS) {
			Logger.d(TAG, getString(R.string.service_ready_to_shutdown));
			stopSelf();
		}
	}

	private void shutdownServiceIfNecessary() {
		if (mConnections.size() == 0) {
			mServiceState = NO_CONNECTIONS;
			shutdownService();
		} else {
			mServiceState = ACTIVE_CONNECTIONS;
		}
	}


	protected void connect(final String address) {
		if (address == null || mConnections == null || mConnections.containsKey(address)) return;

		mLocalDevice = LocalDevice.getInstance();
		mLocalDevice.init(this, new ReadyListener() {
			@Override
			public void ready() {
				RemoteDevice device = mLocalDevice.getRemoteForAddr(address);
				mLocalDevice.destroy();
				new ConnectThread(device).start();
			}
		});

	}

	public void disconnect(final String address) {
		ConnectedThread ct = mConnections.remove(address);
		if (ct != null)
			ct.cancel();

		// end service if this was the last connection to disconnect
		if (mConnections.size() == 0) {
			mServiceState = NO_CONNECTIONS;
			shutdownService();
		} else {
			mServiceState = ACTIVE_CONNECTIONS;
		}
	}

	public void sendData(final String address, byte[] data) {
		ConnectedThread ct = mConnections.get(address);
		if (ct != null)
			ct.write(data);
	}

	private void scanForNearbyDevices() {

		final List<String> names = new LinkedList<String>();
		final List<String> addresses = new LinkedList<String>();
		final Object lock = new Object();

		mLocalDevice = LocalDevice.getInstance();
		mLocalDevice.init(this, new ReadyListener() {
			@Override
			public void ready() {
				mLocalDevice.scan(new ScanListener() {
					@Override
					public void deviceFound(RemoteDevice device) {
						synchronized (lock) {
							names.add(device.getFriendlyName());
							addresses.add(device.getAddress());
						}
					}

					@Override
					public void scanCompleted() {
						Intent returnIntent = new Intent(AmarinoIntent.ACTION_NEARBY_DEVICES);

						String[] namesBis = new String[names.size()];
						namesBis = names.toArray(namesBis);
						returnIntent.putExtra(AmarinoIntent.EXTRA_DEVICE_NAMES, namesBis);

						String[] addresesBis = new String[addresses.size()];
						addresesBis = addresses.toArray(addresesBis);
						returnIntent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESSES, addresesBis);

						sendBroadcast(returnIntent);
					}
				});
			}
		});
	}

	private void broadcastConnectedDevicesList() {
		Intent returnIntent = new Intent(AmarinoIntent.ACTION_CONNECTED_DEVICES);
		if (mConnections.size() == 0) {
			sendBroadcast(returnIntent);
			shutdownService();
			return;
		}
		Set<String> addresses = mConnections.keySet();
		String[] result = new String[addresses.size()];
		result = addresses.toArray(result);
		returnIntent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESSES, result);
		sendBroadcast(returnIntent);
	}

	private void sendConnectionDisconnected(String address) {
		String info = getString(R.string.service_disconnected_from, address);
		Logger.d(TAG, info);

		sendBroadcast(new Intent(AmarinoIntent.ACTION_DISCONNECTED)
				.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address));

		broadcastConnectedDevicesList();
	}

	private void sendConnectionFailed(String address) {
		String info = getString(R.string.service_connection_to_failed, address);
		Logger.d(TAG, info);

		sendBroadcast(new Intent(AmarinoIntent.ACTION_CONNECTION_FAILED)
				.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address));

		broadcastConnectedDevicesList();
	}

	private void sendPairingRequested(String address) {
		Logger.d(TAG, getString(R.string.service_pairing_request, address));
		sendBroadcast(new Intent(AmarinoIntent.ACTION_PAIRING_REQUESTED)
				.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address));

		broadcastConnectedDevicesList();
	}

	private void sendConnectionEstablished(String address) {
		String info = getString(R.string.service_connected_to, address);
		Logger.d(TAG, info);

		sendBroadcast(new Intent(AmarinoIntent.ACTION_CONNECTED)
				.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address));

		broadcastConnectedDevicesList();
	}

	/* ---------- Binder ---------- */

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class AmarinoServiceBinder extends Binder {
		AmarinoService getService() {
			return AmarinoService.this;
		}
	}

	/* ---------- Connection Threads ---------- */

	/**
	 * ConnectThread tries to establish a connection and starts the communication thread
	 */
	private class ConnectThread extends Thread {

		//private static final String TAG = "ConnectThread";
		private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

		private final RemoteDevice mDevice;
		private BtSocket mSocket;

		public ConnectThread(RemoteDevice device) {
			mDevice = device;
		}

		public void run() {
			try {
				String info = getString(R.string.service_connecting_to, mDevice.getAddress());
				Logger.d(TAG, info);

				boolean isPaired = false;

				try {
					isPaired = mDevice.ensurePaired();
				} catch (RuntimeException re) {
					re.printStackTrace();
				}

				if (!isPaired) {
					Log.d(TAG, "not paired!");
					sendPairingRequested(mDevice.getAddress());
					shutdownServiceIfNecessary();
				} else {
					//Log.d(TAG, "is paired!");
					// Let main thread do some stuff to render UI immediately
					Thread.yield();
					// Get a BluetoothSocket to connect with the given BluetoothDevice
					try {
						mSocket = mDevice.openSocket(SPP_UUID);
					} catch (Exception e) {
						Logger.d(TAG, "Connection via SDP unsuccessful, try to connect via port directly");
						// 1.x Android devices only work this way since SDP was not part of their firmware then
						mSocket = mDevice.openSocket(1);
					}

					// Do work to manage the connection (in a separate thread)
					manageConnectedSocket(mSocket);
				}
			} catch (Exception e) {
				sendConnectionFailed(mDevice.getAddress());
				e.printStackTrace();
				if (mSocket != null)
					try {
						mSocket.close();
					} catch (IOException e1) {
					}
				shutdownServiceIfNecessary();
				return;
			}
		}

		/**
		 * Will cancel an in-progress connection, and close the socket
		 */
		@SuppressWarnings("unused")
		public void cancel() {
			try {
				if (mSocket != null) mSocket.close();
				sendConnectionDisconnected(mDevice.getAddress());
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket to " + mDevice.getAddress());
			}
		}

		private void manageConnectedSocket(BtSocket socket) {
			Logger.d(TAG, "connection established.");
			// pass the socket to a worker thread
			String address = mDevice.getAddress();
			ConnectedThread t = new ConnectedThread(socket, address);
			mConnections.put(address, t);
			t.start();

			mServiceState = ACTIVE_CONNECTIONS;
			// now it is time to enable the plug-ins so that they can use our socket
			//informPlugins(address, true);
		}
	}

	/**
	 * ConnectedThread is holding the socket for communication with a Bluetooth device
	 */
	private class ConnectedThread extends Thread {
		private final BtSocket mSocket;
		private final InputStream mInStream;
		private final OutputStream mOutStream;
		private final String mAddress;
		private StringBuffer forwardBuffer = new StringBuffer();

		public ConnectedThread(BtSocket socket, String address) {
			mSocket = socket;
			this.mAddress = address;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (Exception e) {
			}

			mInStream = tmpIn;
			mOutStream = tmpOut;
		}

		@Override
		public void run() {

			byte[] buffer = new byte[1024];  // buffer store for the stream
			int bytes = 0; // bytes returned from read()
			String msg;

			sendConnectionEstablished(mAddress);

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mInStream.read(buffer);

					// Send the obtained bytes to the UI Activity
					msg = new String(buffer, 0, (bytes != -1) ? bytes : 0);
					//Log.d(TAG, msg); // raw data with control flags

					forwardDataToOtherApps(msg);

				} catch (IOException e) {
					disconnect(mAddress);
					Logger.d(TAG, "communication to " + mAddress + " halted");
					break;
				}
			}
		}

		private void forwardDataToOtherApps(String msg) {
			Logger.d(TAG, "Arduino says: " + msg);
			Intent intent = new Intent(AmarinoIntent.ACTION_RECEIVED);
			intent.putExtra(AmarinoIntent.EXTRA_DATA, msg);
			intent.putExtra(AmarinoIntent.EXTRA_DATA_TYPE, AmarinoIntent.STRING_EXTRA);
			intent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, mAddress);
			sendBroadcast(intent);
		}

		/* Call this from the main Activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mOutStream.write(bytes);
				Logger.d(TAG, "send to Arduino: " + new String(bytes));
			} catch (IOException e) {
			}
		}

		/* Call this from the main Activity to shutdown the connection.
		 */
		public void cancel() {
			try {
				if(mSocket != null) mSocket.close();
				sendConnectionDisconnected(mAddress);
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket to " + mAddress);
			}
		}
	}
	
	/* ---------- BroadcastReceiver ---------- */

	BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action == null) return;
			//Log.d(TAG, action);

			if (AmarinoIntent.ACTION_SEND.equals(action)) {
				intent.setClass(context, AmarinoService.class);
				startService(intent);
			}
		}
	};
}
