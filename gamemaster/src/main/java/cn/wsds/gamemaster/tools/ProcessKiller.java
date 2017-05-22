package cn.wsds.gamemaster.tools;

import android.app.ActivityManager;
import android.content.Context;

public class ProcessKiller {

	public static boolean execute(Context context, Iterable<String> packageNameList) {

		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (String packageName : packageNameList) {
			try {
				am.killBackgroundProcesses(packageName);
			} catch (SecurityException se) {
				return false;
			} catch (RuntimeException e) {
				// 在某个设备上出现NullPointerException
			}
		}
		return true;
	}
}
