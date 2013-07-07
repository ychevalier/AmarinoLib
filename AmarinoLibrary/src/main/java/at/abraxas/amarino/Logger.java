package at.abraxas.amarino;

import android.util.Log;

public class Logger {

	static boolean DEBUG = true;
	private static final String TAG = "AmarinoLogger";

	public static void d(String tag, String msg) {
		String text = tag + ": " + msg;
		if (DEBUG)
			Log.d(TAG, text);
	}

	public static void d(String msg) {
		if (DEBUG)
			Log.d(TAG, msg);
	}
}
