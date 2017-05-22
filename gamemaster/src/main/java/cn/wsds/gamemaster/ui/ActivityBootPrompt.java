package cn.wsds.gamemaster.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.TaskManager;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenDesktopListener;
import cn.wsds.gamemaster.ui.accel.AccelOpenDesktopListener.OnEndListener;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpener;
import cn.wsds.gamemaster.ui.accel.AccelOpener.OpenSource;

public class ActivityBootPrompt extends ActivityBase {

	private static final String TAG = "GameMaster";

	private AccelOpener accelOpener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainHandler.getInstance().postAtFrontOfQueue(new Runnable() {
			@Override
			public void run() {
				startAccel();
			}
		});
	}

	private void startAccel() {
		if (AccelOpenManager.isStarted()) {
			finish();
			return;
		}
		if (this.accelOpener == null) {
			this.accelOpener = AccelOpenManager.createOpener(new AccelOpenDesktopListener(new OnEndListener() {

				@Override
				public void onEnd(boolean result) {
					//					StatisticUtils.statisticAccStartUpEvent(AccelOpenManager.isRootModel(), result);
					finish();
				}
			}), OpenSource.Boot);
			this.accelOpener.tryOpen(this);
		}
	}

	@Override
	public void onActivityResult(int request, int result, Intent data) {
		if (this.accelOpener != null) {
			this.accelOpener.checkResult(request, result, data);
		}
	}

	private static class Executer implements Runnable {

		private final Context context;

		public Executer(Context context) {
			this.context = context.getApplicationContext();
		}

		@Override
		public void run() {
			if (TaskManager.getInstance().amIForeground(context)) {
				Log.d(TAG, "I am foreground now");
				return;
			}

			if (VPNUtils.getIGameVpnService() == null) {
				MainHandler.getInstance().postDelayed(this, 100);
			} else {
				ActivityBootPrompt.tryOpenAccel(context);
			}
		}

	}

	/**
	 * 开机自启时被调用
	 */
	public static void onBoot(Context context) {
		new Executer(context).run();
	}

	public static void tryOpenAccel(Context context) {
		if (AccelOpenManager.isStarted()) {
			return;
		}
		if (ConfigManager.getInstance().getBootAutoAccel()) {
			// 自动开启加速
			AccelOpener opener = AccelOpenManager.createOpener(new AccelOpenDesktopListener(null), OpenSource.Boot);
			if (opener.isRootModel()) {
				//root 模式直接开启
				opener.tryOpen(null);
			} else if (opener.isGotPermission()) {
				//VPN模式，已获授权，直接开启
				opener.tryOpen(null);
			} else {
				//vpn模式需要 activity 则打开prompt界面
				openPrompt(context);
			}
		}
	}

	/** 打开提示打开加速界面 */
	private static void openPrompt(Context context) {
		Intent intent = new Intent(context, ActivityBootPrompt.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

}
