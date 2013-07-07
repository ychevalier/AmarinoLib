package at.abraxas.amarino;

import java.util.Map;

public interface AmarinoListener {

	public static final int CONNECT_SUCCEDED          = 10;
	public static final int CONNECT_FAILED            = 11;
	public static final int CONNECT_PAIRING_REQUESTED = 12;

	void onConnectResult(int result, String from);

	void onDisconnectResult(String from);

	void onReceiveData(String data, String from);

	void onReceiveNearbyDevices(Map<String, String> nearbyDevs);

	void onReceiveConnectedDevices(String[] connectedDevs);

	AmarinoReceiver getAmarinoReceiver();
}
