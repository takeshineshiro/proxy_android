package cn.wsds.gamemaster.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cn.wsds.gamemaster.AppInitializer;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.ui.ActivityBootPrompt;

public class BootRecerver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			ConfigManager.createInstance(context);
			if (ConfigManager.getInstance().getBootAutoAccel()) {
				if (AppInitializer.instance.execute(AppInitializer.InitReason.BOOT, null)) {
					ActivityBootPrompt.onBoot(context);
				}
			}
		}
	}
}
