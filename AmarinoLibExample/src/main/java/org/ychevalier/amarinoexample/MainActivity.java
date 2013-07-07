package org.ychevalier.amarinoexample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.abraxas.amarino.AmarinoHelper;
import at.abraxas.amarino.AmarinoListener;
import at.abraxas.amarino.AmarinoReceiver;

public class MainActivity extends Activity implements AmarinoListener, View.OnClickListener, AdapterView.OnItemClickListener {

	private static final boolean DEBUG_MODE = true;
	private static final String TAG = MainActivity.class.getSimpleName();

	private static final String DEVICE_NAME = "name";
	private static final String DEVICE_ADDRESS = "address";

	// ==== Amarino Code ====
	private AmarinoHelper mHelper;
	private AmarinoReceiver mReceiver;
	// ======================

	private SimpleAdapter mConnectedAdapter;
	private List<Map<String, String>> mConnectedDevices;

	private SimpleAdapter mNearbyAdapter;
	private List<Map<String, String>> mNearbyDevices;

	private Button mRefreshBt;
	private ProgressBar mRefreshing;
	private View mContent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContent = findViewById(R.id.activity_main_container);

		mRefreshing = (ProgressBar) findViewById(R.id.activity_main_refreshing_pb);
		mRefreshing.setVisibility(View.GONE);

		mRefreshBt = (Button) findViewById(R.id.activity_main_refresh_bt);
		mRefreshBt.setOnClickListener(this);

		// Adapters Mapping
		String[] from = new String[]{DEVICE_NAME, DEVICE_ADDRESS};
		int[] to = new int[]{R.id.row_device_name, R.id.row_device_address};

		// Devices List
		mConnectedDevices = new ArrayList<Map<String, String>>();
		mConnectedAdapter = new SimpleAdapter(this, mConnectedDevices, R.layout.row_device, from, to);

		ListView lvConnected = (ListView) findViewById(R.id.activity_main_devices_connected_lv);
		lvConnected.setAdapter(mConnectedAdapter);
		lvConnected.setOnItemClickListener(this);

		// Devices List
		mNearbyDevices = new ArrayList<Map<String, String>>();
		mNearbyAdapter = new SimpleAdapter(this, mNearbyDevices, R.layout.row_device, from, to);

		ListView lvNearby = (ListView) findViewById(R.id.activity_main_devices_nearby_lv);
		lvNearby.setAdapter(mNearbyAdapter);
		lvNearby.setOnItemClickListener(this);

		// ==== Amarino Code ====
		mReceiver = new AmarinoReceiver();
		// ======================
	}

	@Override
	protected void onStart() {
		super.onStart();

		// ==== Amarino Code ====
		AmarinoHelper.registerListener(this, this);
		// ======================

		if (mHelper.getNearbyDevices(this)) {
			setRefreshingMode(true);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		// ==== Amarino Code ====
		mHelper.unregisterListener(this, this);
		// ======================
	}

	private void setRefreshingMode(boolean state) {
		if(state) {
			mRefreshBt.setEnabled(false);
			mRefreshing.setVisibility(View.VISIBLE);
			mContent.setVisibility(View.INVISIBLE);
		} else {
			mRefreshBt.setEnabled(true);
			mRefreshing.setVisibility(View.GONE);
			mContent.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onConnectResult(int result, String from) {
		setRefreshingMode(false);
		if (DEBUG_MODE) {
			switch (result) {
				case AmarinoListener.CONNECT_SUCCEDED:
					Log.d(TAG, "Connected with " + from);
					break;
				case AmarinoListener.CONNECT_FAILED:
					Log.d(TAG, "Connection failed with " + from);
					break;
				case AmarinoListener.CONNECT_PAIRING_REQUESTED:
					Log.d(TAG, "Pairing requested with " + from);
					break;
			}
		}
	}

	@Override
	public void onDisconnectResult(String from) {
		setRefreshingMode(false);
		if(from == null) return;

		Map<String, String> dev = null;
		for(Map<String, String> m : mConnectedDevices) {
			for(Map.Entry<String, String> e : m.entrySet()) {
				if(from.equals(e.getValue())) {
					dev = m;
					break;
				}
			}
		}
		mConnectedDevices.remove(dev);
		mConnectedAdapter.notifyDataSetChanged();
	}

	@Override
	public void onReceiveData(String data, String from) {

	}

	@Override
	public void onReceiveNearbyDevices(Map<String, String> nearbyDevs) {
		setRefreshingMode(false);

		if (nearbyDevs == null) return;

		mNearbyDevices.clear();

		for (Map.Entry<String, String> e : nearbyDevs.entrySet()) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put(DEVICE_NAME, e.getKey());
			map.put(DEVICE_ADDRESS, e.getValue());
			mNearbyDevices.add(map);
		}

		mNearbyAdapter.notifyDataSetChanged();
	}

	@Override
	public void onReceiveConnectedDevices(String[] connectedDevs) {
		mConnectedDevices.clear();

		int size = connectedDevs == null? 0 : connectedDevs.length;
		for(int i = 0; i < size; i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put(DEVICE_NAME, "");
			map.put(DEVICE_ADDRESS, connectedDevs[i]);
			mConnectedDevices.add(map);
		}

		mConnectedAdapter.notifyDataSetChanged();
	}

	// ==== Amarino Code ====
	@Override
	public AmarinoReceiver getAmarinoReceiver() {
		return mReceiver;
	}
	// ======================

	@Override
	public void onClick(View view) {
		if (view.getId() == mRefreshBt.getId()) {
			if (mHelper.getNearbyDevices(this)) {
				setRefreshingMode(true);
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

		if(adapterView.getId() == R.id.activity_main_devices_connected_lv) {
			if(mConnectedDevices == null || mConnectedDevices.get(position) == null) return;

			final String name = mConnectedDevices.get(position).get(DEVICE_NAME);
			final String address = mConnectedDevices.get(position).get(DEVICE_ADDRESS);

			AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Connected Device").setMessage(name + " - " + address);
			builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					mHelper.sendDataToArduino(MainActivity.this, address, 'a', "data");
				}
			});
			builder.setNegativeButton("Disconnect", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					mHelper.disconnect(MainActivity.this, address);
					setRefreshingMode(true);
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		} else if(adapterView.getId() == R.id.activity_main_devices_nearby_lv) {
			if(mNearbyDevices == null || mNearbyDevices.get(position) == null) return;

			final String name = mNearbyDevices.get(position).get(DEVICE_NAME);
			final String address = mNearbyDevices.get(position).get(DEVICE_ADDRESS);

			AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Nearby Device").setMessage(name + " - " + address);
			builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					mHelper.connect(MainActivity.this, address);
					setRefreshingMode(true);
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {

				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}
}
