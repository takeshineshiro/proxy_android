package cn.wsds.gamemaster.app;

import java.util.Locale;

import android.util.Log;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.statistic.StatisticUtils;
import cn.wsds.gamemaster.tools.RootUtil;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

public class EventObserver_Inject extends EventObserver {
	private static final boolean LOG = false;
	private static final String TAG = "Inject";

	private GameInfo lastInfo = null;

	private void inject(int pid) {
		if (!ConfigManager.getInstance().isRootMode()) {
			return;
		}
		if (!AccelOpenManager.isStarted()) {
			return;
		}

		if (LOG) {
			Log.d(TAG, String.format("Try to inject, pid = %d", pid));
		}

		String libdir = AppMain.getContext().getFilesDir().getAbsolutePath();
		libdir = libdir.replace("files", "lib");
		String cmd = String.format(Locale.getDefault(), "%s %d %s %s", RootUtil.INJECT_NAME, pid, libdir, "tmp");
		if (LOG) {
			Log.d(TAG, cmd);
		}
		RootUtil.postExecuteInThread(cmd, new RootUtil.OnExecCommandListener() {
			@Override
			public void onExecCommand(int result) {
				if (LOG) {
					Log.d(TAG, String.format("exec command result: %d", result));
				}
				StatisticUtils.statisticHookResult(AppMain.getContext(), result);
			}
		});
	}

	@Override
	public void onTopTaskChange(GameInfo info) {
		if (lastInfo != null) {
			if (LOG) {
				Log.d(TAG, lastInfo.getPackageName() + " background");
			}
		}
		lastInfo = info;
		if (info == null) {
			return;
		}
		if (LOG) {
			Log.d(TAG, info.getPackageName() + " frontground");
		}
		inject(info.getPid());
	}

	@Override
	public void onAccelSwitchChanged(boolean state) {
		if (state && lastInfo != null) {
			inject(lastInfo.getPid());
		}
	}
}
