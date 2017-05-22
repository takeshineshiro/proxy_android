package cn.wsds.gamemaster;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.ProxyEngineCommunicator;
import com.subao.common.data.Defines;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.msg.MessageUserId;
import com.subao.common.net.NetTypeDetector;
import com.subao.data.InstalledAppInfo;
import com.subao.net.NetManager;
import com.subao.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.app.EventObserver_Inject;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.AccelGameList;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.InstalledAppManager;
import cn.wsds.gamemaster.data.ProcessCleanRecords;
import cn.wsds.gamemaster.data.UserFeedback;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.CommonDesktopDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.event.AccelTimeChangedListener;
import cn.wsds.gamemaster.event.DynamicRecerver;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.NewGameInstalledEvent;
import cn.wsds.gamemaster.event.TaskManager;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.message.MessageManager;
import cn.wsds.gamemaster.messageuploader.MessageUploaderManager;
import cn.wsds.gamemaster.net.http.UpdateAccessTokenRequestor;
import cn.wsds.gamemaster.netdelay.NetDelayDataManager;
import cn.wsds.gamemaster.netdelay.NetDelayExceptionWatcher;
import cn.wsds.gamemaster.service.GameVpnService;
import cn.wsds.gamemaster.service.aidl.IGameVpnService;
import cn.wsds.gamemaster.socket.SocketClient;
import cn.wsds.gamemaster.statistic.SmobaQQIpStatistic;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.StatisticUtils;
import cn.wsds.gamemaster.thread.AccelDetailsObserver;
import cn.wsds.gamemaster.thread.GameRunningTimeObserver;
import cn.wsds.gamemaster.thread.InactiveUserReminder;
import cn.wsds.gamemaster.tools.ContactsUtils;
import cn.wsds.gamemaster.tools.RootUtil;
import cn.wsds.gamemaster.tools.SystemInfoUtil;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.tools.onlineconfig.OnlineConfigAgent;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.NoticeOpenGameInside;
import cn.wsds.gamemaster.ui.exchange.ActivityExchangeCenter;
import cn.wsds.gamemaster.ui.floatwindow.GameForgroundDetect;
import cn.wsds.gamemaster.ui.floatwindow.ScreenShotMask;
import cn.wsds.gamemaster.ui.floatwindow.ToastAccel;
import cn.wsds.gamemaster.ui.user.Identify;
import cn.wsds.gamemaster.ui.user.UserTaskManager;
import cn.wsds.gamemaster.vpn.VPNEvent;

public class AppInitializer {

	private static final String TAG = "AppInitializer";

	public static class InitFailException extends Exception {

		private static final long serialVersionUID = -4629636894881241838L;

		public static final int ERROR_BAD_BASE_DATA = -1;
		public static final int ERROR_GAME_MANAGER = -2;
		public static final int ERROR_PROXY_SO_LOAD = -3;

		public final int errorCode;

		public InitFailException(String msg, int errorCode) {
			super(msg);
			this.errorCode = errorCode;
		}

	}

	private static class BaseDataInitException extends InitFailException {

		private static final long serialVersionUID = 3836277990657977881L;

		public BaseDataInitException() {
			super("应用数据被破坏，无法启动。您可能需要卸载后重新安装！", InitFailException.ERROR_BAD_BASE_DATA);
		}

	}

	public static enum InitReason {
		/**
		 * 常规启动
		 */
		START_ACTIVITY,
		/**
		 * 开机自启
		 */
		BOOT,
		/**
		 * 非ActivityStart页面
		 */
		OTHER_ACTIVITY,
	}

	public static final AppInitializer instance = new AppInitializer();

	private IGameVpnService iVpnService;

	private final ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			iVpnService = IGameVpnService.Stub.asInterface(service);
			doInitAfterVpnServicePrepared();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			iVpnService = null;
		}
	};

	private AppInitializer() {}

	private boolean alreadyInitialized;

	public boolean isInitialized() {
		return alreadyInitialized;
	}

	public IGameVpnService getIVpnService() {
		return iVpnService;
	}

	public ServiceConnection getServiceConnection() {
		return connection;
	}

	private void showInitErrorBox(final Throwable ex, Activity activity) {
		CommonDialog dialog;
		if (activity == null) {
			dialog = new CommonDesktopDialog();
		} else {
			if (activity.isFinishing()) {
				return;
			}
			dialog = new CommonAlertDialog(activity);
		}

		dialog.setTitle(R.string.app_name);
		dialog.setMessage(ex.getMessage());
		dialog.setPositiveButton(R.string.close, null);
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				System.exit(0);
			}
		});
		dialog.show();
	}

	/**
	 * 执行初始化操作（必须在UI线程里调用）
	 */
	public boolean execute(InitReason reason, Activity activity) {
		if (alreadyInitialized) {
			return true;
		}
		alreadyInitialized = true;
		Defines.moduleType = Defines.ModuleType.UI;
		Context context = AppMain.getContext();
		try {
			doInit(context);
			switch (reason) {
			case START_ACTIVITY:
				onAppNormalStart(context);
				break;
			default:
				break;
			}
		} catch (Exception ex) {
			if (ex instanceof InitFailException) {
				Statistic.addEvent(context, Statistic.Event.BACKSTAGE_APP_INIT_FAIL,
					Integer.toString(((InitFailException) ex).errorCode));
			}
			ex.printStackTrace();
			UIUtils.showToast(ex.getMessage(), Toast.LENGTH_LONG);
			//
			switch (reason) {
			case START_ACTIVITY:
			case OTHER_ACTIVITY:
				showInitErrorBox(ex, activity);
				break;
			default:
				AppMain.exit(false);
				break;
			}
			return false;
		}
		return true;
	}

	/**
	 * 当APP正常启动时，被调用
	 */
	private static void onAppNormalStart(Context context) {
		if (SystemInfoUtil.isCurrentWiFiConnectionUnsafe(context)) {
			Statistic.addEvent(context, Statistic.Event.NETWORK_UNSAFE_WIFI_CONNECT);
		}
	}

	private static File getInitErrorTestFile() {
		return FileUtils.getDataFile("init.error");
	}

	public static void writeInitErrorTestFile(int errorCode) {
		File file = getInitErrorTestFile();
		if (errorCode == 0) {
			file.delete();
		} else {
			FileWriter writer = null;
			try {
				writer = new FileWriter(file);
				writer.write(Integer.toString(errorCode));
			} catch (IOException e) {

			} finally {
				com.subao.common.Misc.close(writer);
			}
		}
	}

	private static int readInitErrorTestFile() {
		File file = getInitErrorTestFile();
		if (!file.exists() || !file.isFile()) {
			return 0;
		}
		int result = 0;
		FileReader reader = null;
		try {
			reader = new FileReader(file);
			char[] buf = new char[16];
			int chars = reader.read(buf);
			if (chars > 0) {
				try {
					result = Integer.parseInt(new String(buf, 0, chars));
				} catch (NumberFormatException e) {}
			}
		} catch (IOException e) {

		} finally {
			com.subao.common.Misc.close(reader);
			file.delete();
		}
		return result;
	}

	private void doInit(Context context) throws InitFailException {

		//doRegisters(context);

		/*
		 * try { VPNJni.loadLibrary(); } catch (Throwable t) { throw new
		 * InitFailException("程序所需模块已损坏，无法启动。请卸载后重新安装本应用。",
		 * InitFailException.ERROR_PROXY_SO_LOAD); }
		 */

		// !!! 初始化操作，顺序不能修改
		NetManager.getInstance().refreshNetState();

		// Subao ID 和 MessageUserId
		SubaoIdManager subaoIdManager = SubaoIdManager.getInstance();
		subaoIdManager.registerObserver(new SubaoIdManager.Observer() {
			@Override
			public void onSubaoIdChange(String s) {
				updateMessageUserIdAndNotifyBinder(s);
			}
		});
		subaoIdManager.init(context);

		GameVpnService.startService(context, connection);

		// 已安装应用列表
		InstalledAppManager.init(context);

		int initErrorTestCode = readInitErrorTestFile();
		if (initErrorTestCode != 0) {
			throw new InitFailException("测试初始化失败，错误代码：" + initErrorTestCode, initErrorTestCode);
		}

		// 启动C层代理模块
		//VPNJni.setCallback(JNICallback.createInstance(context));		
		/*
		 * 
		 * VPNStartParam vsp = createVPNStartParam(context); int error =
		 * VPNManager.getInstance().init(vsp);
		 * 
		 * if (error != 0) { throw new
		 * InitFailException(String.format("加速服务启动失败(#%d)。\n您是否禁用了“%s”的网络权限？",
		 * error, context.getString(R.string.app_name)), error); }
		 */

		// 加载游戏管理器 ivpnService ==  null !!
		/*
		 * GameManager gm = GameManager.getInstance(); if
		 * (!gm.initFromLocal(context)) { throw new
		 * InitFailException("初始化游戏管理器失败，您可能需要卸载后重新安装。",
		 * InitFailException.ERROR_GAME_MANAGER); }
		 */

		// 初始化事件监听对象
		TriggerManager triggerManager = TriggerManager.getInstance();
		triggerManager.addObserver(new VPNEvent());
		triggerManager.addObserver(new EventObserver_Inject());
		triggerManager.addObserver(new NewGameInstalledEvent());

		// 初始化ToastAccel
		ToastAccel.init();
		//
		triggerManager.addObserver(this.taskObserver);

		//初始化MainHandler
		MainHandler.getInstance().init(context);

		//加载在线参数
		OnlineConfigAgent.getInstance();

		// 反馈初始化
		UserFeedback.History.instance.init();

		GameManager.getInstance().registerObserver(new AccelTimeChangedListener());

		RootUtil.init(context);
		ContactsUtils.init(context); // 读取通讯录

		//ivpnService ==  null !!
		/*
		 * if (!InactiveUserReminder.instance.isRunning() &&
		 * !AccelOpenManager.isStarted()) {
		 * InactiveUserReminder.instance.restart(); }
		 */

		triggerManager.addObserver(GameRunningTimeObserver.instance);
		AccelDetailsObserver.getInstance();
		//triggerManager.addObserver(ReconnectObserver.instance);
		triggerManager.addObserver(new EventObserver() {
			@Override
			public void onTopTaskChange(GameInfo info) {
				if (info == null) {
					ScreenShotMask.destroyInstance();
				} else {
					//VPNManager.getInstance().startNodeDetect(info.getUid(), false);
					VPNUtils.startNodeDetect(info.getUid(), false, TAG);
				}
			}

			@Override
			public void onNodeDetectResult(int code, int uid, boolean succeed) {
				NetTypeDetector.NetType netType = NetManager.getInstance().getCurrentNetworkType();
				StatisticUtils.statisticSpeedTestCause(AppMain.getContext(), null, netType, code);
			}

			@Override
			public void onStartNewGame(GameInfo info) {
				info.onStart(AccelOpenManager.isStarted());
			}
		});
		triggerManager.addObserver(new EventObserver_ForNetDelayExceptionWatcher());
		triggerManager.addObserver(new EventObserver_DNSStatistic(context));
		ProcessCleanRecords.getInstance().init();
		// TASK MANAGER 初始化
		if (!TaskManager.getInstance().start(context)) {
			throw new RuntimeException("TaskManager init failed");
		}

		triggerManager.addObserver(GameForgroundDetect.instance);
		NetDelayDataManager.getInstance();
		NoticeOpenGameInside.init();

		AutoProcessCleanTrigger.getInstance();
		ResUsageChecker.getInstance();
		//        triggerManager.addObserver(new ResUsageWatcher());

		// 加载用户及令牌信息
		UserSession.init();
		if (UserSession.isLogined()) {
			UpdateAccessTokenRequestor.startCheck();
		}
		UserSession.getInstance().registerUserSessionObserver(new UpdateAccessTokenRequestor.SessionInfoObserver());

		// QosConfig   ivpnService ==  null !!
		//QosConfigDownloader.execute();

		UserTaskManager.getInstance().init();
		//设置兑换中心用户观察者
		UserSession.getInstance().registerUserSessionObserver(new ActivityExchangeCenter.UserSessionChangeObserver());

		DynamicRecerver.init(context);

		initImageLoader(context);
	}

	//初始化图片加载库
	private static void initImageLoader(Context context) {
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
				.threadPriority(Thread.NORM_PRIORITY - 2)//加载图片的线程数
				.denyCacheImageMultipleSizesInMemory() //解码图像的大尺寸将在内存中缓存先前解码图像的小尺寸。
				.tasksProcessingOrder(QueueProcessingType.LIFO)//设置加载显示图片队列进程
				.writeDebugLogs() // Remove for release app
				.build();
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}

	/*private static class QosConfigDownloader {

		private static class Controller implements QosConfig.Controller {

			@Override
			public void onQosConfigDownloadFail() {}

			@Override
			public void onQosConfigDownloadOk(Iterable<Pair<Integer, Integer>> keyAndValues) {
				//VPNManager.getInstance().sendSetJNIValues(keyAndValues);
				List<JNIKeyValue> jniKeyValues = new ArrayList<JNIKeyValue>(64);
				if (keyAndValues != null) {
					for (Pair<Integer, Integer> pair : keyAndValues) {
						JNIKeyValue kv = new JNIKeyValue(pair.first, pair.second);
						jniKeyValues.add(kv);
					}
				}
				VPNUtils.sendSetJNIValues(jniKeyValues, TAG);
			}

		}

		private static class QosArguments extends PortalDataDownloader.Arguments {
			public QosArguments(String clientType, String version,
								ServiceLocation serviceLocation, NetTypeDetector netTypeDetector) {
				super(clientType, version, serviceLocation, netTypeDetector);
			}

			@Override
			public Persistent createPersistent(String filename) {
				return PersistentFactory.createByFile(FileUtils.getDataFile("qos_region.portal"));
			}
		}

		public static void execute() {
			QosConfig qosConfig = new QosConfig(
				NetManager.getInstance(),
				PersistentFactory.createByFile(FileUtils.getDataFile(QosConfig.FILE_NAME)),
				new Controller());
            Executor threadExecutor = com.subao.common.thread.ThreadPool.getExecutor();
			qosConfig.executeOnExecutor(threadExecutor);
			//QosRegionConfig

			QosArguments arguments = new QosArguments("android", GameMaster.VERSION_NAME,
					null,NetManager.getInstance());
			QosRegionConfig.start(arguments);
			qosRegionConfig.executeOnExecutor(threadExecutor);
		}
	}*/

	private EventObserver taskObserver = new EventObserver() {

		@Override
		public void onVPNOpen() {
			ConfigManager.getInstance().setFirstStartVpn(false);
			InactiveUserReminder.instance.stop();
		}

		@Override
		public void onVPNClose() {
			//			ConfigManager.getInstance().setVpnOpened(false);
			InactiveUserReminder.instance.restart();
		}

		@Override
		public void onAPStateChange(int state) {
			if (state == NetManager.WIFI_AP_STATE_ENABLED) {
				if (!AccelOpenManager.isStarted()) {
					return;
				}
				CommonDesktopDialog apdialog = new CommonDesktopDialog();
				apdialog.setMessage(R.string.prompt_when_wifi_ap);
				apdialog.setCanceledOnTouchOutside(true);
				apdialog.setPositiveButton(R.string.i_known, null);
				apdialog.show();
			}
		}

		@Override
		public void onAppInstalled(InstalledAppInfo info) {
			if (GameManager.isSupportGame(info)) {
				String appLabel = info.getAppLabel();
				AppNotificationManager.sendNewAppNotify(appLabel);
				if(SystemInfoUtil.isStrictOs()){
					UIUtils.showToast(AppMain.getContext().getResources().getString(R.string.strict_os_new_game_notify_content));
				}
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.NOTIFICATION_NEW_GAME);
			}
		}

	};



	//    /**
	//     * 如果需要，上报安装列表
	//     */
	//    private static void uploadInstalledListIfNeed() {
	//        if (ConfigManager.getInstance().getInstalledListReported()) {
	//            return;
	//        }
	//        InstalledAppInfo[] infoList = GameManager.getInstance().getInstalledApps();
	//        if (infoList == null || infoList.length == 0) {
	//            return;
	//        }
	//        StringBuilder sb = new StringBuilder(infoList.length * 128);
	//        for (InstalledAppInfo info : infoList) {
	//            sb.append(info.getAppLabel());
	//            sb.append(',');
	//            sb.append(info.getPackageName());
	//            sb.append('\n');
	//        }
	//        byte[] data = sb.toString().getBytes();
	//        DataUploader.getInstance().addInstalledList(data, new OnInstalledListUpdateCompleted());
	//        if (LOG) {
	//            Log.d(TAG, "Installed list upload task scheduled");
	//        }
	//    }

	//    private static class OnInstalledListUpdateCompleted implements DataUploader.OnUploadCompletedCallback {
	//
	//		@Override
	//		public boolean onUploadCompleted(boolean succeeded, byte[] data) {
	//			if (LOG) {
	//				Log.d(TAG, "Installed list upload completed: " + succeeded);
	//			}
	//			if (succeeded) {
	//				ConfigManager.getInstance().setInstalledListReported(succeeded);
	//			}
	//			if (UrlConfig.instance.getServerType() == UrlConfig.ServerType.TEST) {
	//				// 测试服未安装这个服务，所以不用重试了
	//				return false;
	//			} else {
	//				// 返回True，表示如果失败了，让DataUploder自动重试
	//				return true;
	//			}
	//		}
	//
	//    }

	/////////////////////////////////////////////////////////

	//	private static class ResUsageWatcher extends EventObserver implements ResUsageChecker.Observer {
	//
	//		@Override
	//		public void onResUsageCheckResult(ResUsage resUsage) {
	//			// 未达阈值，不清理
	//			if (!Misc.isResUsageOverflow(resUsage)) {
	//				return;
	//			}
	//			// 没开加速，不清理
	//			if (!AccelOpenManager.isStarted()) {
	//				return;
	//			}
	//			// 已经不是游戏在前台了，不清理
	//			if (TaskManager.getInstance().getCurrentForegroundGame() == null) {
	//				return;
	//			}
	//			// 清理
	//			FloatWindowInGame wnd = FloatWindowInGame.getInstance();
	//			boolean showEffect = (wnd != null && wnd.getVisibility() == View.VISIBLE);
	//			int x, y;
	//			if (wnd != null) {
	//				x = wnd.getX();
	//				y = wnd.getY();
	//			} else {
	//				x = y = 0;
	//			}
	//			Misc.cleanMemory(AppMain.getContext(), resUsage.runningAppList, showEffect, x, y);
	//		}
	//
	//		@Override
	//		public void onTopTaskChange(GameInfo info) {
	//			if (info != null && AccelOpenManager.isStarted()) {
	//				ResUsageChecker.getInstance().enter(this, 30 * 1000);
	//			} else {
	//				ResUsageChecker.getInstance().leave(this);
	//			}
	//		}
	//
	//	}

	private static class EventObserver_ForNetDelayExceptionWatcher extends EventObserver {

		private static class Provider implements NetDelayExceptionWatcher.Provider {
			@Override
			public boolean isDisconnectOr2G() {
				NetTypeDetector.NetType type = NetManager.getInstance().getCurrentNetworkType();
				return NetTypeDetector.NetType.DISCONNECT == type || NetTypeDetector.NetType.MOBILE_2G == type;
			}

			@Override
			public Context getContext() {
				return AppMain.getContext();
			}

		}

		private final NetDelayExceptionWatcher watcher;

		public EventObserver_ForNetDelayExceptionWatcher() {
			this.watcher = new NetDelayExceptionWatcher(new Provider(), NetDelayExceptionWatcher.Params.createDefault());
		}

		@Override
		public void onTopTaskChange(GameInfo info) {
			if (info != null) {
				this.watcher.start(info.getUid());
			} else {
				this.watcher.stop();
			}
		}

		@Override
		public void onFirstSegmentNetDelayChange(int delayMilliseconds) {
			watcher.onNetDelayData(delayMilliseconds);
		}
	}

	private static class EventObserver_DNSStatistic extends EventObserver {

		private static final String FILE_NAME = "smoba_q_dns";
		private static final String KEY_AWX = "awx";
		private static final String KEY_APP = "app";

		private final Context context;
		private NetTypeDetector.NetType lastNetState;
		private long lastCheckTime;

		private static class StatisticRunner {
			private final Set<String> already = new HashSet<String>(16);
			private final Statistic.Event event;

			public StatisticRunner(Statistic.Event event) {
				this.event = event;
			}

			public boolean report(Context context, InetAddress[] addrList) {
				if (addrList == null || addrList.length == 0) {
					return false;
				}
				StringBuilder sb = null;
				for (InetAddress ia : addrList) {
					String ip = ia.getHostAddress();
					if (!already.contains(ip)) {
						already.add(ip);
						if (sb == null) {
							sb = new StringBuilder(1024);
						}
						sb.append(ip).append(',');
					}
				}
				if (sb != null) {
					Statistic.addEvent(context, this.event, sb.toString());
					return true;
				}
				return false;
			}

			public void writeToJson(JsonWriter writer) throws IOException {
				writer.beginArray();
				for (String s : already) {
					writer.value(s);
				}
				writer.endArray();
			}

			public void loadFromJson(JsonReader reader) throws IOException {
				reader.beginArray();
				while (reader.hasNext()) {
					String s = reader.nextString();
					if (s != null) {
						this.already.add(s);
					}
				}
				reader.endArray();
			}
		}

		private final StatisticRunner awxStatistic = new StatisticRunner(Statistic.Event.SMOBA_QQ_COM_AWX);
		private final StatisticRunner appStatistic = new StatisticRunner(Statistic.Event.SMOBA_QQ_COM_APP);

		private class Listener implements SmobaQQIpStatistic.Listener {

			@Override
			public void onAddressTook(NetTypeDetector.NetType netState,
				SmobaQQIpStatistic.AddressList addressList) {
				if (addressList == null) {
					return;
				}
				boolean changed = awxStatistic.report(context, addressList.awx_smoba_qq_com);
				if (appStatistic.report(context, addressList.app_smoba_qq_com)) {
					changed = true;
				}
				if (changed) {
					try {
						saveToFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}

		public EventObserver_DNSStatistic(Context context) {
			try {
				loadFromFile();
			} catch (IOException e) {}
			this.context = context.getApplicationContext();
			this.onNetChange(NetManager.getInstance().getCurrentNetworkType());
		}

		private void loadFromFile() throws IOException {
			JsonReader reader = new JsonReader(new BufferedReader(new FileReader(createFile()), 4096));
			try {
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (KEY_AWX.equals(name)) {
						awxStatistic.loadFromJson(reader);
					} else if (KEY_APP.equals(name)) {
						appStatistic.loadFromJson(reader);
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
			} finally {
				com.subao.common.Misc.close(reader);
			}
		}

		private void saveToFile() throws IOException {
			JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(createFile()), 4096));
			try {
				writer.beginObject();
				writer.name(KEY_AWX);
				this.awxStatistic.writeToJson(writer);
				writer.name(KEY_APP);
				this.appStatistic.writeToJson(writer);
				writer.endObject();
			} finally {
				com.subao.common.Misc.close(writer);
			}
		}

		private static File createFile() {
			return FileUtils.getDataFile(FILE_NAME);
		}

		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			if (state == NetTypeDetector.NetType.DISCONNECT) {
				lastNetState = state;
				return;
			}
			long now = SystemClock.elapsedRealtime();
			if (state == lastNetState) {
				if (now - lastCheckTime < 5000) {
					return;
				}
			} else {
				lastNetState = state;
			}
			lastCheckTime = now;
			SmobaQQIpStatistic.start(state, new Listener());
		}

	}

	//由于doInit()时，VpnService还没有connect，导致一些初始化处理失效，
	//因此将原本在doInit()中的一些处理放在VpnService connected 后进行
	private void doInitAfterVpnServicePrepared() {

		if (iVpnService == null) {
			UIUtils.showToast("vpn 服务初始化失败！");
			Logger.e(TAG, "vpn serivce init failed！");
			return;
		}

		boolean jniLoadException = false;
		try {
			jniLoadException = iVpnService.isInitFailException();
		} catch (RemoteException e) {
			Logger.e(TAG, e.toString());
		}

		if (jniLoadException) {
			showInitErrorBox(new InitFailException("程序所需模块已损坏，无法启动,请卸载后重新安装本应用。",
				InitFailException.ERROR_PROXY_SO_LOAD), null);
		}

		ProxyEngineCommunicator.Instance.set(VPNUtils.instance);

		Context context = AppMain.getContext();
		int error = VPNUtils.init(TAG);

		if (error != 0) {
			String errorInfo ;
			/*if (error == -1) {
				errorInfo = "对不起，残留进程导致初始化失败，请手动清理进程后重试！";
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.SERVICE_LAST_LEFT, "true");
			} else */{
				errorInfo = String.format("加速服务启动失败(#%d)。\n您是否禁用了“%s”的网络权限？", error,
					context.getString(R.string.app_name));
			}

			showInitErrorBox(new InitFailException(errorInfo, error), null);
		}

		try {
			int port = iVpnService.getLocalPort();
			if (port > 0) {
				SocketClient.getInstance().start(context, port);
			}
		} catch (RemoteException e) {
			Logger.e(TAG, e.toString());
		}

		// 将SubaoId赋值给MessageUserId，并传递给Service
		updateMessageUserIdAndNotifyBinder(SubaoIdManager.getInstance().getSubaoId());
		// 初始化游戏列表
		AccelGameList.init(AppMain.getContext());

		GameManager gm = GameManager.getInstance();
		if (!gm.initFromLocal(context)) {
			showInitErrorBox(new InitFailException("初始化游戏管理器失败，您可能需要卸载后重新安装。",
				InitFailException.ERROR_GAME_MANAGER), null);
		}

		if (!InactiveUserReminder.instance.isRunning() && !AccelOpenManager.isStarted()) {
			InactiveUserReminder.instance.restart();
		}

		//QosConfigDownloader.execute();

		// 消息记录, 这里有用的并联加速的配置， 所以要后启动， 这个下一步要解偶
		MessageManager.getInstance();

//		PortalMisc.tryDownload(NetManager.getInstance(),
//			PersistentFactory.createByFile(FileUtils.getDataFile(PortalMisc.FILE_NAME)),
//			new PortalMisc.Callback() {
//				@Override
//				public void onPortalMiscDownload(PortalMisc portalmisc) {
//					IGameVpnService intf = iVpnService;
//					if (intf != null) {
//						Data data = portalmisc.getData();
//						if (data != null) {
//							VPNUtils vpnUtils = VPNUtils.instance;
//							vpnUtils.sendSetJNIValue(Defines.VPNJniKey.KEY_NET_MEASURE_ALLOWED, data.netMeasureAllowed);
//							vpnUtils.sendSetJNIValue(Defines.VPNJniKey.KEY_DEFAULT_USER_CONFIG, data.defaultConfig);
//	                        //AuthExecutor.setProtocol(data.authHTTP);
//						}
//					}
//				}
//			}
//		);
		
		ConfigManager.getInstance().registerObserver(new ConfigManager.Observer() {
			
			@Override
			public void onShowDelayInFloatWindowChange(boolean show) {}
			
			@Override
			public void onFloatWindowSwitchChange(boolean on) {
				notifyFloatWindowSwitchToService();
			}
			
			@Override
			public void onAutoCleanProgressSwitchChange(boolean on) {}
		});
		notifyFloatWindowSwitchToService();


		//向vpn server发送心跳包
		sendKeepalive();

		TriggerManager.getInstance().raiseNetChange(NetManager.getInstance().
				getCurrentNetworkType());

		Identify.notifyStartCheck();
	}

	private void sendKeepalive() {
		MainHandler.getInstance().post(new Runnable() {
			@Override
			public void run() {
				try {
					if (iVpnService == null) {
						Logger.e(TAG, "vpn service disconnected！");
						return;
					}
					iVpnService.sendKeepalive();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				MainHandler.getInstance().postDelayed(this, 2500);
			}
		});
	}

	private void updateMessageUserIdAndNotifyBinder(String subaoId) {

		MessageUserId.setCurrentSubaoId(subaoId);

		IGameVpnService intf = getIVpnService();
		if (intf == null) {
			Log.w(LogTag.GAME, "SubaoId updated, but service not found");
		} else {
			try {
				intf.onSubaoIdUpdate(subaoId);
			} catch (RemoteException e) {
				Log.w(LogTag.GAME, "SubaoId updated, but notify service failed");
			}
		}
	}
	
	private void notifyFloatWindowSwitchToService() {
		IGameVpnService gvs = iVpnService;
		if (gvs != null) {
			try {
				gvs.configChange(GlobalDefines.ConfigName.FLOAT_WINDOW_SWITCH.ordinal(),
						Boolean.toString(ConfigManager.getInstance().getShowFloatWindowInGame()));
			} catch (RemoteException e) { }
		}
	}
}
