package cn.wsds.gamemaster;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;
import com.subao.common.LogTag;
import com.subao.common.data.Defines;
import com.subao.common.data.Defines.ModuleType;
import com.subao.common.data.ServiceConfig;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.msg.MessageUserId;
import com.subao.common.utils.InfoUtils;
import com.subao.net.NetManager;
import com.subao.utils.FileUtils;
import com.subao.utils.UrlConfig;
import com.umeng.analytics.AnalyticsConfig;
import com.umeng.analytics.MobclickAgent;

import java.util.List;

import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.DeviceInfo;
import cn.wsds.gamemaster.event.TaskManager;
import cn.wsds.gamemaster.netdelay.NetDelayDataManager;
import cn.wsds.gamemaster.service.GameVpnService;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.socket.SocketClient;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.UserActionSerializer;
import cn.wsds.gamemaster.tools.AppsWithUsageAccess;
import cn.wsds.gamemaster.tools.JPushUtils;
import cn.wsds.gamemaster.ui.ActivityBase;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.floatwindow.BoxInGame;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowInGame;
import cn.wsds.gamemaster.useraction.UserActionManager;
import cn.wsds.gamemaster.useraction.VersionInfo;


/**
 * 程序入口
 * <p>
 * 这里可视为本App的入口，无论是正常启动还是服务自启，总会实例化Application对象
 * </p>
 * 
 * @see {@link Application}
 */
public class AppMain extends Application {

	private static final String TAG = LogTag.GAME;

	private static AppMain instance;

	/** 本次程序启动时刻（UTC毫秒数） */
	private static long startTime;

	public static long getStartTime() {
		return startTime;
	}

	//	public static AppMain getInstance() {
	//		return instance;
	//	}

	public static Context getContext() {
		return instance.getApplicationContext();
	}

	@Override
	public void onCreate() {
        if (Build.VERSION.SDK_INT <= 20) {
            MultiDex.install(getApplicationContext());
        }
		super.onCreate();
		if (initLeakCanary()) {
			return;
		}
		instance = this;
		startTime = System.currentTimeMillis();
		Defines.moduleType = getModuleType();
		// 下面的操作必须耗时很少，并且需要注意顺序
		Context context = getApplicationContext();
		UIUtils.init(context);
		FileUtils.init(context);
		NetManager.createInstance(context);
		ConfigManager.createInstance(context).setTimeInMillisOfActivateV40();
		if (ConfigManager.getInstance().getUseTestUmengKey()) {
			AnalyticsConfig.setAppkey(null, "5716f6ee67e58e01e400092c");
		}
		//
		ServiceConfig serviceConfig = new ServiceConfig();
		serviceConfig.loadFromFile(null,false);
		//
		if (Defines.moduleType != ModuleType.SERVICE) {
			SelfUpgrade.getInstance().init(context).check(null);
			AppsWithUsageAccess.sendUsageStateNotification();
			initStatistic(context);
		}
		//
		initUrlConfig();
		MainHandler.getInstance();
		ErrorReportor.init(context);
		//
		
		// JPush
        JPushUtils.init(context);
        
		Log.i(TAG, String.format("App start, module type is: %s", Defines.moduleType == null ? "Unknown" : Defines.moduleType.name));
	}

	private boolean initLeakCanary() {
		if (LeakCanary.isInAnalyzerProcess(this)) {
			// This process is dedicated to LeakCanary for heap analysis.
			// You should not init your app in this process.
			return true;
		}
		LeakCanary.install(this);
		return false;
	}

	/**
	 * 判断当前进程是UI还是Service
	 */
	private Defines.ModuleType getModuleType() {
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		if (am == null) {
			return null;
		}
		List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
		if (processes == null || processes.size() == 0) {
			return null;
		}
		int myPid = android.os.Process.myPid();
		String myPkgName = getPackageName();
		for (RunningAppProcessInfo info : processes) {
			if (info.pid == myPid) {
				return myPkgName.equals(info.processName) ? ModuleType.UI : ModuleType.SERVICE;
			}
		}
		return null;
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		if (Defines.moduleType == Defines.ModuleType.UI) {
			if (level > ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
				if (null != TaskManager.getInstance().getCurrentForegroundGame()) {
					ActivityBase.finishAll(null);
				}
			}
		}
	}

	private void initUrlConfig() {

		UrlConfig.ServerType serverType = ConfigManager.getInstance().getServerType();
		if (serverType != UrlConfig.ServerType.NORMAL) {
			UrlConfig.instance.setServerType(serverType);
			Log.w(TAG, "Use test server now.");
		}
	}

	private static void initStatistic(Context context) {
		StringBuilder sb = new StringBuilder(256);
		sb.append(UrlConfig.instance.getDomainOfUserAction());
		sb.append("/v1/report/client/event");
		try {
			Statistic.init(context);
			UserActionManager.createInstance(
				new VersionInfo(InfoUtils.getVersionName(context), DeviceInfo.getUmengChannel(context),
					android.os.Build.DISPLAY,
					Integer.toString(android.os.Build.VERSION.SDK_INT)),
				new UserActionSerializer(), sb.toString());
			SubaoIdManager.getInstance().registerObserver(new SubaoIdObserver());
			MessageUserId.setUpdateListener(new MessageUserIdObserver());
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	//	public static enum ExitCode {
	//		/** 正常退出 */
	//		NO_ERROR,
	//		/** 从手机存储中加载数据失败时异常退出 */
	//		LOADDATA_ERROR,
	//		/** 初始化错误 */
	//		INIT_ERROR,
	//		/** 启动代理层失败时，异常退出 */
	//		START_PROXY_ERROR,
	//		/** 捕获到未Catch异常时，异常退出 */
	//		CATCH_EXCEPTION_EXIT,
	//	}

	/**
	 * 程序正常退出。（仅当用户选择菜单里的退出功能时）
	 * 
	 * @param needSave
	 *            保存数据 true 保存 false 不保存
	 */
	public static void exit(boolean needSave) {
		try {
			Log.i(TAG, "Application exit");
			FloatWindowInGame.destroyInstance();
			BoxInGame.destroyInstance();
			//			boolean vpnOpened = OpenManager.isStarted();
			AccelOpenManager.close(CloseReason.APP_EXIT);
			ServiceConnection connection = AppInitializer.instance.getServiceConnection();
			SocketClient.getInstance().stop();
			GameVpnService.stopService(instance, connection);
			if (needSave) {
				GameManager.getInstance().save(false);
				NetDelayDataManager.getInstance().save();
			}
			UserActionManager.getInstance().stopAndWait(1000);
			//VPNManager.waitFor(1000);
			ActivityBase.finishAll(null);
			//			ConfigManager.getInstance().setVpnOpened(vpnOpened);
			MobclickAgent.onKillProcess(AppMain.getContext());
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

	private static class SubaoIdObserver implements SubaoIdManager.Observer {
		@Override
		public void onSubaoIdChange(String subaoId) {
			if (subaoId != null) {
				UserActionManager.getInstance().updateSubaoId(subaoId);
			}
		}
	}

	private static class MessageUserIdObserver implements MessageUserId.UpdateListener {

		@Override
		public void onSubaoIdUpdate(String s) {
			// do nothing			
		}

		@Override
		public void onUserInfoUpdate(String userId, String serviceId, int userStatus) {
			UserActionManager.getInstance().udpateUserId(userId);
		}

	}


}
