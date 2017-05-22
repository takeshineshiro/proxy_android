package cn.wsds.gamemaster.tools;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Message;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.data.ConfigManager;

public class AppsWithUsageAccess {

	private static boolean doesApiExist() {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
	}

	/**
	 * 获得最近的应用统计数据 不需要考虑新版本问题，方法中低版本默认返回null
	 * 
	 * @param duration
	 *            持续在这段时间内
	 * @return return null 持续在 duration这个时间内应用没有更新 或者新版本手机
	 */
	public static String getRecentUsagePackageName(Context context, long duration) {
		if (doesApiExist()) {
			return AppsWithUsageAccessImpl.getRecentUsagePackageName(context, duration);
		} else {
			return null;
		}
	}

	public static boolean hasEnable() {
		if (doesApiExist()) {
			return AppsWithUsageAccessImpl.hasEnable(AppMain.getContext());
		} else {
			return false;
		}
	}

	/**
	 * has Apps with usage access module
	 * 
	 * @return
	 */
	public static boolean hasModule() {
		try {
			PackageManager packageManager = AppMain.getContext().getPackageManager();
			Intent intent = new Intent("android.settings.USAGE_ACCESS_SETTINGS");
			List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
			return list.size() > 0;
		} catch (RuntimeException e) {
			// 在某个三星的设备上出现过安全异常
			// 在某个ZTE的设备上出现过RuntimeException
			return false;
		}
	}

	public static void toImpower(Activity context) {
		context.startActivity(new Intent("android.settings.USAGE_ACCESS_SETTINGS"));
	}

	public static boolean isSupport() {
		return hasModule() && hasEnable();
	}

	public static void sendUsageStateNotification() {
		new UsageStateNotification().sendUsageStateNotification();
	}

	private static final class UsageStateNotification {
		private static final int ACCEL_TIME_SENCONDS = 24 * 60 * 60 * 1000;
		@SuppressLint("HandlerLeak")
		private Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				sendUsageStateNotification();
			};
		};

		public void sendUsageStateNotification() {
			if (ConfigManager.getInstance().isToSendUsageStateNotification()) {
				return;
			}
			long fromActiveByNowDeltaTime = getFromActiveByNowDeltaTime();
			if (fromActiveByNowDeltaTime < ACCEL_TIME_SENCONDS) {
				handler.sendEmptyMessageDelayed(0, ACCEL_TIME_SENCONDS - fromActiveByNowDeltaTime);
				return;
			}
			if (hasModule() && !hasEnable()) {
				AppNotificationManager.sendUsageStateHelp();
				ConfigManager.getInstance().setToSendUsageStateNotification();
			}
			handler = null;
		}

		private long getFromActiveByNowDeltaTime() {
			return System.currentTimeMillis() - ConfigManager.getInstance().getTimeInOfMillisActivateV40();
		}
	}

}
