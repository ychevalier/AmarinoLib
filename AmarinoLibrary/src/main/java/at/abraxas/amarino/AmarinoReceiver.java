package at.abraxas.amarino;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AmarinoReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) return;

		AmarinoHelper.transferMessage(intent.getAction(), intent.getExtras());
	}
}
