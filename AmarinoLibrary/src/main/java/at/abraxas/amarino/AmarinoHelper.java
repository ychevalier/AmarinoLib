package at.abraxas.amarino;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This is the only main interface to communicate with Amarino Service.
 * Through this Helper, you can call the Service functionnalies from your android activity.
 */
public class AmarinoHelper {

	private static List<AmarinoListener> mListeners = new LinkedList<AmarinoListener>();

	public static void setAmarinoDebugMode(boolean enable) {
		Logger.DEBUG = enable;
	}

	public static boolean registerListener(Context context, AmarinoListener listener) {
		if(context == null || listener == null || listener.getAmarinoReceiver() == null) return false;

		registerReceiver(context, listener.getAmarinoReceiver());
		mListeners.add(listener);
		return true;
	}

	public static boolean unregisterListener(Context context, AmarinoListener listener) {
		if(context == null || listener == null || listener.getAmarinoReceiver() == null) return false;

		unregisterReceiver(context, listener.getAmarinoReceiver());
		return mListeners.remove(listener);
	}

	private static void registerReceiver(Context context, AmarinoReceiver receiver) {
		if (context == null || receiver == null) return;

		context.registerReceiver(receiver, new IntentFilter(
				AmarinoIntent.ACTION_CONNECTED));
		context.registerReceiver(receiver, new IntentFilter(
				AmarinoIntent.ACTION_CONNECTION_FAILED));
		context.registerReceiver(receiver, new IntentFilter(
				AmarinoIntent.ACTION_DISCONNECTED));
		context.registerReceiver(receiver, new IntentFilter(
				AmarinoIntent.ACTION_PAIRING_REQUESTED));
		context.registerReceiver(receiver, new IntentFilter(
				AmarinoIntent.ACTION_RECEIVED));
		context.registerReceiver(receiver, new IntentFilter(
				AmarinoIntent.ACTION_CONNECTED_DEVICES));
		context.registerReceiver(receiver, new IntentFilter(
				AmarinoIntent.ACTION_NEARBY_DEVICES));
	}

	private static void unregisterReceiver(Context context, AmarinoReceiver receiver) {
		if (context == null || receiver == null) return;

		context.unregisterReceiver(receiver);
	}

	static void transferMessage(String message, Bundle b) {
		if(message == null || mListeners == null || b == null) return;

		String from = b.getString(AmarinoIntent.EXTRA_DEVICE_ADDRESS);

		if (message.equals(AmarinoIntent.ACTION_CONNECTED)) {

			Logger.d("Device Connected");

			for(AmarinoListener l : mListeners) {
				if(l != null) {
					l.onConnectResult(AmarinoListener.CONNECT_SUCCEDED, from);
				}
			}
		} else if (message.equals(AmarinoIntent.ACTION_CONNECTION_FAILED)) {

			Logger.d("Device Connection Failed");

			for(AmarinoListener l : mListeners) {
				if(l != null) {
					l.onConnectResult(AmarinoListener.CONNECT_FAILED, from);
				}
			}
		} else if (message.equals(AmarinoIntent.ACTION_DISCONNECTED)) {

			Logger.d("Device Connection Disconnected");

			for(AmarinoListener l : mListeners) {
				if(l != null) {
					l.onDisconnectResult(from);
				}
			}
		} else if (message.equals(AmarinoIntent.ACTION_PAIRING_REQUESTED)) {

			Logger.d("Device Request Pairing");

			for(AmarinoListener l : mListeners) {
				if(l != null) {
					l.onConnectResult(AmarinoListener.CONNECT_PAIRING_REQUESTED, from);
				}
			}
		} else if (message.equals(AmarinoIntent.ACTION_RECEIVED)) {

			Logger.d("Device Sent Something");

			String data = b.getString(AmarinoIntent.EXTRA_DATA);

			for(AmarinoListener l : mListeners) {
				if(l != null) {
					l.onReceiveData(data, from);
				}
			}
		} else if (message.equals(AmarinoIntent.ACTION_CONNECTED_DEVICES)) {

			Logger.d("Received Connected Devices");

			String[] devs = b.getStringArray(AmarinoIntent.EXTRA_DEVICE_ADDRESSES);

			for(AmarinoListener l : mListeners) {
				if(l != null) {
					l.onReceiveConnectedDevices(devs);
				}
			}
		} else if (message.equals(AmarinoIntent.ACTION_NEARBY_DEVICES)) {

			Logger.d("Received Nearby Devices");

			String[] names = b.getStringArray(AmarinoIntent.EXTRA_DEVICE_NAMES);
			String[] addresses = b.getStringArray(AmarinoIntent.EXTRA_DEVICE_ADDRESSES);

			Map<String, String> devs = new HashMap<String, String>();

			if(names != null && addresses != null
					&& (names.length == addresses.length)) {
				for(int i = 0; i < names.length; i++) {
					devs.put(names[i], addresses[i]);
				}
			}

			for(AmarinoListener l : mListeners) {
				if(l != null) {
					l.onReceiveNearbyDevices(devs);
				}
			}
		}
	}

	public static boolean connect(Context context, String address) {
		if (context == null || !Utils.isCorrectAddressFormat(address))
			return false;

		Intent intent = new Intent(context, AmarinoService.class);
		intent.setAction(AmarinoIntent.ACTION_CONNECT);
		intent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address);
		context.startService(intent);

		return true;
	}

	public static boolean disconnect(Context context, String address) {
		if (address == null || context == null) return false;

		Intent intent = new Intent(context, AmarinoService.class);
		intent.setAction(AmarinoIntent.ACTION_DISCONNECT);
		intent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address);
		context.startService(intent);

		return true;
	}

	public static boolean getNearbyDevices(Context context) {
		if (context == null || mListeners == null) return false;

		Intent intent = new Intent(context, AmarinoService.class);
		intent.setAction(AmarinoIntent.ACTION_GET_NEARBY_DEVICES);
		context.startService(intent);

		return true;
	}

	/**
	 * Sends a boolean value to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, boolean data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.BOOLEAN_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a byte value to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, byte data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.BYTE_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a char value to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, char data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.CHAR_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a short value to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, short data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.SHORT_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends an int value to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, int data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.INT_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a long value to Arduino
	 *
	 * <p><i>If you do not exactly know what you do, you absolutely shouldn't really use this method,
	 * since Arduino cannot receive Android's 32-bit long values.</i></p>
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, long data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.LONG_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a float value to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, float data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.FLOAT_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a double value to Arduino
	 *
	 * <p><i>If you do not exactly know what you do, you absolutely shouldn't really use this method,
	 * since Arduino cannot receive Android's 32-bit double values.</i></p>
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, double data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.DOUBLE_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a String to Arduino
	 *
	 * <p><i>The buffer of an Arduino is small, your String should not be longer than 62 characters</i></p>
	 * @assertion: (data.length() <= 62)
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, String data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.STRING_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends an boolean array to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, boolean[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.BOOLEAN_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends an byte array to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, byte[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.BYTE_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a char array to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, char[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.CHAR_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a short array to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, short[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.SHORT_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends an int array to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, int[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.INT_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a long array to Arduino
	 *
	 * <p><i>If you do not exactly know what you do, you absolutely shouldn't really use this method,
	 * since Arduino cannot receive Android's 32-bit long values.</i></p>
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, long[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.LONG_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a float array to Arduino
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, float[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.FLOAT_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a double array to Arduino
	 *
	 * <p><i>If you do not exactly know what you do, you absolutely shouldn't really use this method,
	 * since Arduino cannot receive Android's 32-bit double values.</i></p>
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, double[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.DOUBLE_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	/**
	 * Sends a String array to Arduino
	 *
	 * <p><i>The buffer of an Arduino is small, your String should not be longer than 62 characters.</i></p>
	 * @assertion: for each (String s : data) { assert(s.length() <= 62); }
	 *
	 * @param context the context
	 * @param address the Bluetooth device you want to send data to
	 * @param flag the flag Arduino has registered a function for to receive this data
	 * @param data your data you want to send
	 */
	public static boolean sendDataToArduino(Context context, String address, char flag, String[] data) {
		if(context == null || address == null || !Utils.isCorrectAddressFormat(address)) return false;

		Intent intent = getSendIntent(address, AmarinoIntent.STRING_ARRAY_EXTRA, flag);
		intent.putExtra(AmarinoIntent.EXTRA_DATA, data);
		context.sendBroadcast(intent);

		return true;
	}

	private static Intent getSendIntent(String address, int dataType, char flag) {
		Intent intent = new Intent(AmarinoIntent.ACTION_SEND);
		intent.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address);
		intent.putExtra(AmarinoIntent.EXTRA_DATA_TYPE, dataType);
		intent.putExtra(AmarinoIntent.EXTRA_FLAG, flag);
		return intent;
	}
}
