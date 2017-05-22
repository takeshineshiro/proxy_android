package cn.wsds.gamemaster.event;

import java.util.ArrayList;
import java.util.List;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.utils.ThreadUtils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.tools.AppsWithUsageAccess;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

public class TaskManager implements GameForegroundDetector {

	private static final String TAG = LogTag.GAME;

	private static final int DELAY_SEC = 2;
	private static final int DELAY_MS = DELAY_SEC * 1000;

	private static final TaskManager instance = new TaskManager();

	private Strategy strategy;

	private final Handler handler = new Handler();

	private TaskUtils taskUtils;

	/** 当前顶层游戏 */
	private GameInfo currentForegroundGame;
	
	/** 桌面的包名 */
	private List<String> packageNameOfHomes;

	@Override
	public GameInfo getCurrentForegroundGame() {
		return currentForegroundGame;
	}

	private TaskManager() {
		resetStrategy(ConfigManager.getInstance().getFloatWindowMode());
	}

	public static TaskManager getInstance() {
		if (GlobalDefines.CHECK_MAIN_THREAD) {
			if (!ThreadUtils.isInAndroidUIThread()) {
				MainHandler.getInstance().showDebugMessage("Call TaskManager.getInstance not in main thread");
			}
		}
		return instance;
	}

	/**
	 * 重置策略
	 */
	public void resetStrategy(int mode) {
		String name;
		switch (mode) {
		case 1:
			strategy = new Strategy_Ver4();
			name = "v4";
			break;
		case 2:
			strategy = new Strategy_Ver4_4();
			name = "v4.4";
			break;
		case 3:
			strategy = new Strategy_Ver5();
			name = "v5";
			break;
		default:
			strategy = createStrategyAuto();
			name = "auto";
			break;
		}
		Logger.d(TAG, "[TaskManager] Current strategy: " + name);
	}

	private static Strategy createStrategyAuto() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			if (AppsWithUsageAccess.isSupport()) {
				return new Strategy_Ver5();
			} else {
				return new Strategy_Ver4_4();
			}
		} else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			return new Strategy_Ver4_4();
		} else {
			return new Strategy_Ver4();
		}
	}

	/**
	 * 启动TaskManager
	 * 
	 * @param context
	 * @return
	 */
	public boolean start(Context context) {
		if (this.taskUtils != null) {
			throw new RuntimeException("TaskManager already start");
		}
		this.taskUtils = TaskUtils.create(context.getApplicationContext());
		if (this.taskUtils != null) {
			handler.postDelayed(topTaskRunnable, DELAY_MS);
			return true;
		} else {
			return false;
		}
	}

	private final Runnable topTaskRunnable = new Runnable() {
		@Override
		public void run() {
			GameInfo top = doCheck();
			if (currentForegroundGame != top) {
				currentForegroundGame = top;
				TriggerManager.getInstance().raiseTopTaskChange(currentForegroundGame);
			}
			handler.postDelayed(this, DELAY_MS);
		}
	};

	/**
	 * 执行检查操作
	 * 
	 * @return {@link GameInfo}，如果为null表示顶层不是游戏，否则为游戏相关信息
	 */
	private GameInfo doCheck() {
		GameInfo top = strategy.getForegroundGame(taskUtils);
		if (top == null) {
			return top;
		}
		if (top == currentForegroundGame) {
			// 没变化
			GameManager.getInstance().incGameTimeSeconds(top.getPackageName(), DELAY_SEC, AccelOpenManager.isStarted());
			return currentForegroundGame;
		}
		// Task ID
		int taskId = getTopTaskId();
		if (taskId != -1) {
			top.setTaskId(taskId);
		}
		return top;
	}

	private int getTopTaskId() {
		RunningTaskInfo rt = taskUtils.getTopTask();
		if (rt == null) {
			return -1;
		}
		return rt.id;
	}

	/////////////////////////////////////////////////////////////////////

	private interface Strategy {
		public GameInfo getForegroundGame(TaskUtils tu);

		public String getForegroundPackageName(TaskUtils tu);
	}

	private static class Strategy_Ver5 implements Strategy {
		//		private final UsageStatsManager usageStatsManager=(UsageStatsManager)AppMain.getContext().getSystemService("usagestats");// Context.USAGE_STATS_SERVICE);
		private final int duration = DELAY_MS * 5;

		private GameInfo lastGameInfo;

		@SuppressLint("NewApi")
		@Override
		public GameInfo getForegroundGame(TaskUtils tu) {
			String packageName = getForegroundPackageName(tu);
			if (packageName == null) {
				if (lastGameInfo != null) {
					return lastGameInfo;
				}
				return null;
			}
			GameInfo gameInfo = GameManager.getInstance().getGameInfo(packageName);
			if (gameInfo != null) {
				int pid = tu.getPID(packageName);
				if (pid > 0) {
					//TODO PID 获取为-1
					gameInfo.setPid(pid);
				}
			}
			lastGameInfo = gameInfo;
			return gameInfo;
		}

		@Override
		public String getForegroundPackageName(TaskUtils tu) {
			return AppsWithUsageAccess.getRecentUsagePackageName(AppMain.getContext(), duration);
		}

	}

	private static class Strategy_Ver4_4 implements Strategy {

		private static Object findForegroundApp(TaskUtils tu, boolean packageNameOnly) {
			if (AppsWithUsageAccess.isSupport()) {
				TaskManager taskManager = TaskManager.getInstance();
				taskManager.resetStrategy(3);
				if (packageNameOnly) {
					return taskManager.strategy.getForegroundPackageName(tu);
				} else {
					return taskManager.strategy.getForegroundGame(tu);
				}
			}
			List<RunningAppProcessInfo> lr = tu.getRunningAppProcesses();
			if (lr == null) {
				return null;
			}
			for (RunningAppProcessInfo ra : lr) {
				if (ra.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE
					|| ra.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
				{
					if (packageNameOnly) {
						return ra.processName;
					}
					GameInfo gameInfo = GameManager.getInstance().getGameInfo(ra.processName);
					if (gameInfo != null) {
						gameInfo.setPid(ra.pid);
						return gameInfo;
					}
					// 注意：这里不能break，因为实践证明同一时刻不止一个App拥有IMPORTANCE_FOREGROUND标志
				}
			}
			return null;
		}

		@Override
		public GameInfo getForegroundGame(TaskUtils tu) {
			return (GameInfo) findForegroundApp(tu, false);
		}

		@Override
		public String getForegroundPackageName(TaskUtils tu) {
			return (String) findForegroundApp(tu, true);
		}

	}

	private static class Strategy_Ver4 implements Strategy {

		private GameInfo gameInfo;

		@Override
		public GameInfo getForegroundGame(TaskUtils tu) {
			String pkNameOfTopTask = getForegroundPackageName(tu);
			if (pkNameOfTopTask == null) {
				gameInfo = null;
				return null;
			}
			if (gameInfo != null && pkNameOfTopTask.equals(gameInfo.getPackageName())) {
				// 未发生变化，直接返回
				return gameInfo;
			}
			gameInfo = GameManager.getInstance().getGameInfo(pkNameOfTopTask);
			if (gameInfo != null) {
				int pid = tu.getPID(pkNameOfTopTask);
				if (pid > 0) {
					gameInfo.setPid(pid);
				}
			}
			return gameInfo;
		}

		@Override
		public String getForegroundPackageName(TaskUtils tu) {
			try {
				RunningTaskInfo rt = tu.getTopTask();
				if (rt == null) {
					return null;
				}
				String pn = rt.topActivity.getPackageName();
				if (pn == null) {
					return null;
				}
				return pn;
			} catch (SecurityException se) {
				se.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * 判断本程序是否在前台
	 */
	public boolean amIForeground(Context context) {
		if (taskUtils == null) {
			return false;
		}
		List<RunningAppProcessInfo> appProcesses = taskUtils.activityManager.getRunningAppProcesses();
		if (appProcesses == null || appProcesses.size() == 0) {
			return false;
		}
		ApplicationInfo ai = context.getApplicationInfo();
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.uid == ai.uid) {
				if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	private static class TaskUtils {

		private final ActivityManager activityManager;

		public static TaskUtils create(Context context) {
			ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			if (am == null) {
				return null;
			}
			return new TaskUtils(am);
		}

		private TaskUtils(ActivityManager am) {
			this.activityManager = am;
		}

		public RunningTaskInfo getTopTask() {
			@SuppressWarnings("deprecation")
			List<RunningTaskInfo> list = activityManager.getRunningTasks(1);
			if (list == null || list.isEmpty()) {
				return null;
			}
			return list.get(0);
		}

		public List<RunningAppProcessInfo> getRunningAppProcesses() {
			return activityManager.getRunningAppProcesses();
		}

		public int getPID(String pn) {
			List<RunningAppProcessInfo> lr = getRunningAppProcesses();
			if (lr != null) {
				for (RunningAppProcessInfo ra : lr) {
					if (ra.processName.equals(pn))
						return ra.pid;
				}
			}
			return -1;
		}
	}

	public boolean isDesktopForeground(Context context) {
		if (packageNameOfHomes == null) {
			packageNameOfHomes = getHomesPackageName(context);
		}
		if (packageNameOfHomes == null) {
			return false;
		}
		String packageName = this.strategy.getForegroundPackageName(this.taskUtils);
		if (null == packageName) {
			return false;
		}
		for (String s : this.packageNameOfHomes) {
			if (packageName.equalsIgnoreCase(s)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> getHomesPackageName(Context context) {
		List<String> names = new ArrayList<String>();
		PackageManager packageManager = context.getPackageManager();
		//属性  
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		for (ResolveInfo ri : resolveInfo) {
			names.add(ri.activityInfo.packageName);
		}
		return names;
	}
}
