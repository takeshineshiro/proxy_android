package cn.wsds.gamemaster.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.accel.AccelEngineInstance;
import com.subao.common.accel.EngineWrapper;
import com.subao.common.data.AccelGame;
import com.subao.common.data.Defines;
import com.subao.common.jni.InitJNIMode;
import com.subao.common.jni.JniWrapper;
import com.subao.common.model.AccelGameListManager;
import com.subao.common.msg.MessageUserId;
import com.subao.common.net.NetManager;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.Protocol;
import com.subao.common.thread.ThreadPool;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.R;

import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.service.VPNGlobalDefines.VPNEvent;
import cn.wsds.gamemaster.service.aidl.IGameVpnService;
import cn.wsds.gamemaster.service.aidl.VpnAccelGame;
import cn.wsds.gamemaster.service.aidl.VpnSupportGame;
import cn.wsds.gamemaster.socket.SocketServer;

public class GameVpnService extends VpnService {
	private static final byte[] INTERFACE_ADDRESS = new byte[] {
		(byte) 198,
		51,
		100,
		10
	};



	//	private static final String[] PATH_LIST = {
	//			"/system/bin/", "/system/xbin/", "/sbin/", "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/",
	//			"/system/bin/failsafe/", "/data/local/"
	//	};

	/*private static class FDListener implements CellularWatcher.OnNewFDMadeListener {

		@Override
		public void onNewFD(int fd) {
			VPNManagerUtils.onNewMobileNetworkFD(fd);
		}
	}*/

	private static final String TAG = LogTag.GAME;
	private static final String FORMAT = "%d.%d.%d.%d";

	private static int FORGROUND_NOTIFICATION_ID = 5;

	private static GameVpnService instance;

	private ParcelFileDescriptor mInterface;

	private boolean foreground;

	private boolean initFailException;

	private Context context;

	private KeepAliveChecker keepAliveChecker;

	private boolean floatWindowSwitch;

	private JniWrapper jniWrapper ;
	private NetManager netManager ;
	private EngineWrapper engineWrapper ;
	private int jniInitResult ;

	private boolean isVPNStart ;

	static GameVpnService getInstance() {
		return instance;
	}

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

	private final IGameVpnService.Stub binder = new IGameVpnService.Stub() {

		@Override
		public boolean isVPNStarted() throws RemoteException {
			return isVPNStart;
		}

		@Override
		public void setToForeground() throws RemoteException {
			GameVpnService.setToForeground();
		}

		@Override
		public void setToBackground() throws RemoteException {
			GameVpnService.setToBackground();
		}

		@Override
		public void closeVPN(int reason) throws RemoteException {
			CloseReason closeReson = CloseReason.fromOrdinal(reason);
			GameVpnService.closeVPN(closeReson);
		}

		@Override
		public int startVPN(List<String> supportPackageNames) throws RemoteException {
			StartVpnResult result = GameVpnService.startVPN(supportPackageNames);
			return result.ordinal();
		}

		@Override
		public boolean isIpEqualInterfaceAddress(byte[] ip)
			throws RemoteException {
			return GameVpnService.isIpEqualInterfaceAddress(ip);
		}

		@Override
		public boolean protect(int fd) throws RemoteException {
			if (instance == null) {
				return false;
			} else {
				return instance.protect(fd);
			}
		}

		@Override
		public int getVpnAccelState() throws RemoteException {
			return jniWrapper.getAccelerationStatus();
		}

		@Override
		public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
			throws RemoteException {

			if (code == IBinder.LAST_CALL_TRANSACTION) {
				onRevoke();
			}
			return super.onTransact(code, data, reply, flags);
		}

		@Override
		public boolean isInitFailException() throws RemoteException {

			if (instance == null) {
				return false;
			} else {
				return instance.isJniInitFailException();
			}
		}

		@Override
		public int vpnInit() throws RemoteException {
			return jniInitResult;
		}

		/*@Override
		public int vpnInit(VPNStartParamTransporter transporter) throws RemoteException {
			if (instance == null) {
				return -1;
			}
			VPNManager vpnManager = VPNManager.getInstance();
			if (vpnManager.getProxyMode() == VPNManager.ProxyMode.NONE) {
				ProxyEngineCommunicator.Instance.set(vpnManager);
				VPNJni.setCallback(JNICallbackPoster.createInstance());
				Thread.State state = vpnManager.getState();//临时方案，UI被杀死后，手动断开vpn 
				//，这时service会被停止，但 VPNManager 仍处于工作状态，如果此时调用VPNManager.init(),程序将崩溃。

				if (Thread.State.NEW.equals(state)) {
					int result = VPNManagerUtils.init(transporter.param);
					if (result == 0) {
						ServiceConfig serviceConfig = new ServiceConfig();

						serviceConfig.loadFromFile(null,false);
						Persistent persistent = PersistentFactory.createByFile(FileUtils.getDataFile(".subao.tc"));
						AuthExecutor.init(serviceConfig.getAuthServiceLocation(), Defines.REQUEST_CLIENT_TYPE_FOR_APP, persistent);
					}
					return result;
				} else if (Thread.State.TERMINATED.equals(state)) {
					return -1;
				} else {
					return 0;
				}

			} else {
				return 0;
			}
		}*/

		@Override
		public void vpnSendPutSupportGame(VpnSupportGame supportGame) throws RemoteException {
			jniWrapper.addSupportGame(supportGame.getUid(),supportGame.getPackageName(),
					supportGame.getPackageName(),getProtocol(supportGame.getProtocol()));
		}

		public void vpnSendPutSupportGames(List<VpnSupportGame> supportGames) throws RemoteException {
			for(VpnSupportGame supportGame : supportGames){
				jniWrapper.addSupportGame(supportGame.getUid(),supportGame.getPackageName(),
						supportGame.getPackageName(),getProtocol(supportGame.getProtocol()));
			}
		}

		@Override
		public void vpnSendSetLogLevel(int level) throws RemoteException {
			//JniWrapperProxy.sendSetLogLevel(level);
		}

		@Override
		public int vpnCheckSocketState() throws RemoteException {

			return 0;
		}

		@Override
		public int vpnGetAccelStatus() throws RemoteException {

			return jniWrapper.getAccelerationStatus();
		}

		@Override
		public boolean vpnNetworkCheck() throws RemoteException {

			return true;
		}

		@Override
		public boolean vpnSetRootMode() throws RemoteException {

			return false;
		}

		@Override
		public boolean vpnStartProxy(int mode, int vpnfd) throws RemoteException {

			return isVPNStart ;
		}

		@Override
		public void vpnStopProxy() throws RemoteException {

			jniWrapper.stopProxy();
		}

		@Override
		public void vpnSendUnionAccelSwitch(boolean checked) throws RemoteException {
			//JniWrapperProxy.sendUnionAccelSwitch(checked);
		}

		@Override
		public boolean vpnIsNodeAlreadyDetected(int uid) throws RemoteException {
			return jniWrapper.isNodeDetected(uid);
		}

		@Override
		public void vpnStartNodeDetect(int gameUID, boolean force) throws RemoteException {
			jniWrapper.startNodeDetect(gameUID);
		}

		@Override
		public String vpnGetAppLogCache() throws RemoteException {
			return "" ;
		}

		@Override
		public void vpnOpenQosAccelResult(int id, String speedId, int error) throws RemoteException {
			//FIXME sessionId
			jniWrapper.openQosAccelResult(0,"",speedId,error);
		}

		@Override
		public void vpnModifyQosAccelResult(int id, int timeSeconds, int error) throws RemoteException {
			jniWrapper.modifyQosAccelResult(0,timeSeconds,error);
		}

		@Override
		public void vpnCloseQosAccelResult(int id, int error) throws RemoteException {
			jniWrapper.closeQosAccelResult(0,error);
		}

		@Override
		public void vpnSendSetNetworkState(int type) throws RemoteException {
			NetTypeDetector_ForService.getInstance().onNetChange(NetTypeDetector.NetType.fromValue(type));
			//JniWrapperProxy.sendSetNetworkState(type);
		}

		@Override
		public void vpnSendStartGameDelayDetect() throws RemoteException {
			//JniWrapperProxy.sendStartGameDelayDetect();
		}

		@Override
		public void vpnSendStopGameDelayDetect() throws RemoteException {
			//JniWrapperProxy.sendStopGameDelayDetect();
		}

		@Override
		public void vpnSendSetFrontGameUid(int uid) throws RemoteException {
            // FIXME: 2017/3/31
            //JniWrapperProxy.sendSetFrontGameUid(uid);
		}

		@Override
		public int getLocalPort() throws RemoteException {
			return SocketServer.getInstance().getPort();
		}

		@Override
		public void vpnSendSetJNIBooleanValue(int key, boolean value) throws RemoteException {
			//JniWrapperProxy.sendSetJNIBooleanValue(key, value);
		}

		@Override
		public void vpnSendSetJNIIntValue(int key, int value) throws RemoteException {
			jniWrapper.setInt(0,String.format("%d",key),value);
		}
		
		public void vpnSendSetJNIStringValue(int key, String value) throws RemoteException {
			jniWrapper.setString(0,String.format("%d",key),value);
		};

		@Override
		public void setUserToken(String openId, String token, String appId) throws RemoteException {
			jniWrapper.setUserToken(openId, token, appId);
		}

		@Override
		public void registCellularWatcher() throws RemoteException {

			//CellularWatcher.getInstance().register(context, new FDListener());
		}

		@Override
		public void vpnOnNewMobileNetworkFD(int fd) throws RemoteException {
			//JniWrapperProxy.onNewMobileNetworkFD(fd);
		}

		@Override
		public void onSubaoIdUpdate(String subaoId) throws RemoteException {
			MessageUserId.setCurrentSubaoId(subaoId);
		}

		@Override
		public void sendKeepalive() throws RemoteException {
			KeepAliveChecker checker = keepAliveChecker;
			if (checker != null) {
				checker.onKeepAliveReceiver();
			}
		}

		@Override
		public void configChange(int name, String value) throws RemoteException {
			if (name == GlobalDefines.ConfigName.FLOAT_WINDOW_SWITCH.ordinal()) {
				floatWindowSwitch = Boolean.parseBoolean(value);
			}
		}

		@Override
		public List<VpnAccelGame> getAccelGameList() throws RemoteException {
			List<AccelGame> gameList = AccelGameListManager.getInstance().getAccelGameList();
			if (gameList == null) {
				return null;
			}
			List<VpnAccelGame> vpnAccelGameList = new ArrayList<>(gameList.size());
			for (AccelGame accelGame : gameList) {
				VpnAccelGame vpnAccelGame = new VpnAccelGame();
				vpnAccelGame.setAppLabel(accelGame.appLabel);
				vpnAccelGame.setAccelMode(accelGame.accelMode);
				vpnAccelGame.setBlackIps(accelGame.getBlackIps());
				vpnAccelGame.setWhiteIps(accelGame.getWhiteIps());
				vpnAccelGame.setBlackPorts(accelGame.getBlackPorts());
				vpnAccelGame.setWhitePorts(accelGame.getWhitePorts());
				vpnAccelGame.setFlags(accelGame.flags);
				vpnAccelGame.setLabelThreeAsciiChar(accelGame.isLabelThreeAsciiChar);
				vpnAccelGameList.add(vpnAccelGame);
			}
			return vpnAccelGameList;
		}

		@Override
		public String getVIPValidTime() throws RemoteException {
			return "";
			//return VPNManagerUtils.getVIPValidTime();
		}

		@Override
		public int getAccelerationStatus() throws RemoteException {
			return 0;
			//return VPNManagerUtils.getAccelerationStatus();
		}

		private Protocol getProtocol(int ordinal){
			for(Protocol protocol : Protocol.values()){

				if(protocol.ordinal()==ordinal){
					return  protocol ;
				}
			}

			return null ;
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private void createKeepAliveChecker() {
		if (keepAliveChecker == null) {
			keepAliveChecker = new KeepAliveChecker(this);
			keepAliveChecker.executeOnExecutor(ThreadPool.getExecutor());
		}
	}

	private void destroyKeepAliveChecker() {
		if (keepAliveChecker != null) {
			keepAliveChecker.requestTerminate();
			keepAliveChecker = null;
		}
	}

	// 多次调用startService，该Service的onCreate()只会触发一次
	@Override
	public void onCreate() {
		super.onCreate();
		Logger.d(TAG, "Service create");
		Defines.moduleType = Defines.ModuleType.SERVICE;
		this.foreground = false;
		this.initFailException = false;
		instance = this;
		this.context = getApplicationContext();
		createKeepAliveChecker();
		jniInitResult = 0 ;
		isVPNStart = false ;

		if(!doInit(context)){
			Logger.e(TAG,"Jni init failed !");
			return;
		}

		MessageUserId.setUpdateListener(new MessageUserIdUpdateListener());

		SocketServer.getInstance().start();

		// 触发启动事件
		sendMessage(context, VPNGlobalDefines.ACTION_VPN_SERVICE_CREATED, null);
		//TriggerManager.getInstance().raiseVpnServiceCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.d(TAG, "Service destroy");
		setToBackground();
		destroyKeepAliveChecker();
		//AccelOpenManager.close(CloseReason.VPN_SERVICE_DESTROY);

		Bundle bundle = new Bundle();
		bundle.putInt(VPNGlobalDefines.KEY_ACTION_VPN_CLOSE_REASON, CloseReason.VPN_SERVICE_DESTROY.ordinal());
		sendMessage(context, VPNGlobalDefines.ACTION_VPN_ACCEL_MANAGER_CLOSE, bundle);

		SocketServer.getInstance().stop();

		checkVPNAccelState();

		super.onDestroy();
		instance = null;
		System.exit(0);
	}

	@Override
	public void onRevoke() {
		//AccelOpenManager.close(CloseReason.VPN_REVOKE);

		Bundle bundle = new Bundle();
		bundle.putInt(VPNGlobalDefines.KEY_ACTION_VPN_CLOSE_REASON, CloseReason.VPN_REVOKE.ordinal());

		sendMessage(context, VPNGlobalDefines.ACTION_VPN_ACCEL_MANAGER_CLOSE, bundle);
		Logger.d(TAG, "Service revoke");
		super.onRevoke();
	}

	static boolean isVPNStarted() {
		GameVpnService inst = instance;
		return inst != null && inst.mInterface != null;
	}

	private boolean doInit(Context context) {
        //FIXME
		jniWrapper = new JniWrapper("gamemaster") ;
		netManager = new NetManager(context);
		engineWrapper = new EngineWrapper(context, Defines.ModuleType.SERVICE,
				"g_official", context.getResources().getString(R.string.app_version),
				netManager,
				jniWrapper,false);
		byte[] defaultAccelGameList;
		try {
			defaultAccelGameList = loadJsonOfDefaultAccelGameList(AppMain.getContext());
		} catch (IOException e) {
			return false;
		}

		jniInitResult = engineWrapper.init(InitJNIMode.VPN, null, 0, defaultAccelGameList);
		if (jniInitResult == 0) {
			AccelEngineInstance.set(engineWrapper);
		} else {
			engineWrapper.dispose();
			engineWrapper = null;
			return false;
		}

		JniCallbackPoster jniCallbackPoster = new JniCallbackPoster(
				engineWrapper,jniWrapper,
				new EngineWrapper.AuthExecutorController(netManager,engineWrapper.getMessageSender()),
				engineWrapper.getMessageServiceLocation(),
				engineWrapper.getHrArguments());

		engineWrapper.setJniCallBack(jniCallbackPoster);
		return true ;
		/*try {
			VPNJni.loadLibrary();
			return true;
		} catch (Throwable t) {
			initFailException = true;
			return false;
		}*/
	}

	private byte[] loadJsonOfDefaultAccelGameList(Context context) throws IOException {
		byte[] result;
		InputStream inputStream = context.getAssets().open("games.json");
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream(12 * 1024);
			try {
				byte[] buffer = new byte[1024];
				for (; ; ) {
					int size = inputStream.read(buffer);
					if (size > 0) {
						outputStream.write(buffer, 0, size);
					} else {
						break;
					}
				}
			} finally {
				outputStream.close();
			}
			result = outputStream.toByteArray();
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
		return result;
	}

	private boolean isJniInitFailException() {
		return initFailException;
	}

	@SuppressLint("NewApi")
	private StartVpnResult doStartVPN(List<String> supportPackageNames) {
		if (mInterface != null) {
			return StartVpnResult.OK;
		}

		Builder builder = new Builder();
		
		try {			 
			
			if(supportPackageNames!=null){  //添加白名单，即GameVpnService只服务于游戏
				int count = supportPackageNames.size();		 
				for(int i = 0 ; i<count ; i++){				
					builder.addAllowedApplication(supportPackageNames.get(i));				
				}
				
				builder.addAllowedApplication(getPackageName());							
			}
			
			builder.addAddress(ipToString(INTERFACE_ADDRESS), 32);
			builder.addRoute("0.0.0.0", 0);
			// 在某些机型的5.0手机上面老用户卸载时VPN没有手动断开 也会创建失败
			mInterface = builder.setSession("迅游加速服务已开启").setConfigureIntent(null).establish();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mInterface == null) {
			return StartVpnResult.ESTABLISH_FAIL;
		} else {
			int fd = mInterface.getFd();
			if (fd <= 0) {
				return StartVpnResult.ESTABLISH_FAIL;
			}

			if (AccelEngineInstance.get().startVPN(fd)) {
				isVPNStart = true ;
				sendBroadcast(new Intent(VPNGlobalDefines.ACTION_VPN_OPEN));
				//TriggerManager.getInstance().raiseVPNOpen();
				return StartVpnResult.OK;
			} else {
				return StartVpnResult.ESTABLISH_FAIL;
			}
		}
	}

	private void doCloseVPN(CloseReason reason) {
		if (mInterface == null)
			return;
		// 统计
		doStatisticCloseEvent(reason);
		//
		try {
			AccelEngineInstance.get().stopVPN();
			isVPNStart = false ;
			if (mInterface != null) {
				mInterface.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		mInterface = null;
		//MobclickAgent.onEvent(AppContext.getInstance(), EventId.VPN_STOP);

		sendMessage(context, VPNGlobalDefines.ACTION_VPN_CLOSE, null);
		//TriggerManager.getInstance().raiseVPNClose();
	}

	private void doStatisticCloseEvent(CloseReason reason) {
		if (reason == null) {
			return;
		}

		Bundle bundle = new Bundle();
		bundle.putInt(VPNGlobalDefines.KEY_VPN_EVENT_ID, VPNEvent.NETWORK_VPN_STOP_SEASON.ordinal());
		bundle.putString(VPNGlobalDefines.KEY_ACTION_VPN_EVENT_PARAM, reason.desc);

		sendMessage(context, VPNGlobalDefines.ACTION_VPN_ADD_EVENT, bundle);

		//Statistic.addEvent(context, Statistic.Event.NETWORK_VPN_STOP_SEASON, reason.desc);
		if (reason == CloseReason.BY_PROXY) {

			Bundle bundle_by_proxy = new Bundle();
			bundle_by_proxy.putInt(VPNGlobalDefines.KEY_VPN_EVENT_ID, VPNEvent.CLOSE_VPN_BY_PROXY_MODEL.ordinal());

			sendMessage(context, VPNGlobalDefines.ACTION_VPN_ADD_EVENT, bundle_by_proxy);

			//Statistic.addEvent(context, Statistic.Event.CLOSE_VPN_BY_PROXY_MODEL, sb.toString()); 
		}
	}

	private void checkVPNAccelState() {
		CloseReason reason = CloseReason.VPN_REVOKE;
		closeVPN(reason);
	}

	@SuppressWarnings("deprecation")
	private static void setToForeground() {
		GameVpnService inst = instance;
		if (inst != null && !inst.foreground) {
			Context context = inst.context;
			int id = FORGROUND_NOTIFICATION_ID;
			Notification.Builder builder = createNotificationBuilder(context, "迅游手游正在为您加速游戏", "点击查看", null,
				R.drawable.notify_icon_normal_big,
				R.drawable.notify_icon_normal);

			Intent[] intents = new Intent[1];
			//intents[0].setClassName("cn.wsds.gamemaster", "cn.wsds.gamemaster.ui.ActivityMain");
			intents[0] = Intent.makeMainActivity(new ComponentName("cn.wsds.gamemaster", "cn.wsds.gamemaster.ui.ActivityMain"));
			PendingIntent pi = PendingIntent.getActivities(inst, id, intents, Intent.FLAG_ACTIVITY_NEW_TASK);
			builder.setContentIntent(pi);
			try {
				// build()方法基于API16，所以这里只能用getNotfication()
				Notification notification = builder.getNotification();
				inst.startForeground(id, notification);
				inst.foreground = true;
			} catch (Exception e) {}
		}
	}

	private static void setToBackground() {
		GameVpnService inst = instance;
		if (inst != null && inst.foreground) {
			inst.stopForeground(true);
			inst.foreground = false;
		}
	}

	private static void closeVPN(CloseReason reason) {
		GameVpnService inst = instance;
		if (inst != null) {
			inst.doCloseVPN(reason);
		}
	}

	/**
	 * 这个函数被UIManager.init()调用
	 * 
	 * @param context
	 */
	public static void startService(Context context, ServiceConnection connection) {
		/*
		 * GameVpnService inst = instance; if (inst == null) {
		 * context.startService(new Intent(context, GameVpnService.class)); }
		 */

		Intent intent = new Intent(context, GameVpnService.class);
		context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
		context.startService(intent);
	}

	public static void stopService(Context context, ServiceConnection connection) {
		//context.stopService(new Intent(context, GameVpnService.class));
		context.unbindService(connection);
		context.stopService(new Intent(context, GameVpnService.class));
	}

	public static enum StartVpnResult {
		OK,
		SERVICE_NOT_EXISTS,
		WIFI_AP_OPENED,
		ESTABLISH_FAIL,
		Start_FAIL,
	}

	private static StartVpnResult startVPN(List<String> supportPackageNames) {
		StartVpnResult result;
		GameVpnService inst = instance;
		if (inst != null) {
			result = inst.doStartVPN(supportPackageNames);
		} else {
			result = StartVpnResult.SERVICE_NOT_EXISTS;
		}
		if (result != StartVpnResult.OK) {
			sendMessage(instance.getApplicationContext(), VPNGlobalDefines.ACTION_VPN_START_FAILED, null);
			//TriggerManager.getInstance().raiseStartVPNFailed(false);
		}
		return result;
	}

	private static boolean isIpEqualInterfaceAddress(byte[] ip) {
		return Arrays.equals(ip, INTERFACE_ADDRESS);
	}


	private static Notification.Builder createNotificationBuilder(Context context, String title,
		String content, String ticker, int largeicon, int smallicon) {

		Bitmap icon = BitmapFactory.decodeResource(context.getResources(), largeicon);
		Notification.Builder builder = new Notification.Builder(context);
		builder.setLargeIcon(icon).setSmallIcon(smallicon);
		builder.setContentTitle(title).setContentText(content);
		if (!TextUtils.isEmpty(ticker)) {
			builder.setTicker(ticker);
		}

		return builder;
	}

	/**
	 * 将一个用网络序byte数组表示的IPv4转换成字符串输出
	 * 
	 * @param ip
	 *            用32位整型表示的IP
	 * @return 形如“xx.xxx.x.xx”的IPv4字串
	 */
	@SuppressLint("DefaultLocale")
	private static String ipToString(byte[] ip) {
		if (ip == null || ip.length != 4) {
			return "";
		}
		return String.format(FORMAT, (0xff & ip[0]), (0xff & ip[1]), (0xff & ip[2]), (0xff & ip[3]));
	}

	//	/** 判断手机是否root，不弹出root请求框 */
	//	private static boolean isRoot() {
	//		if (alreadyRoot == null) {
	//			for (String path : PATH_LIST) {
	//				String filename = path + "su";
	//				if (isFileExists(filename) && isExecutable(filename)) {
	//					alreadyRoot = true;
	//					return true;
	//				}
	//			}
	//			alreadyRoot = false;
	//		}
	//
	//		return alreadyRoot.booleanValue();
	//	}
	//	
	//	private static boolean isFileExists(String filename) {
	//		try {
	//			File file = new File(filename);
	//			return file.exists();
	//		} catch (Exception ex) {
	//			return false;
	//		}
	//	}
	//
	//	// 是否可以执行
	//	private static boolean isExecutable(String filePath) {
	//		try {
	//			File file = new File(filePath);
	//			boolean canExecute = file.canExecute();
	//			long length = file.length();
	//			return canExecute && length > 0;
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//			return false;
	//		}
	//	}

	private static void sendMessage(Context context, String action, Bundle extras) {
		if ((context == null) || (action == null) || (action.isEmpty())) {
			return;
		}

		Intent intent = new Intent(action);
		if (extras != null) {
			intent.putExtras(extras);
		}

		context.sendBroadcast(intent);
	}

	private static class MessageUserIdUpdateListener implements MessageUserId.UpdateListener {

		@Override
		public void onUserInfoUpdate(String userId, String serviceId, int userStatus) {
			//JniCallbackPoster.onMessagUserIdUpdate(userId, serviceId, userStatus);
		}

		@Override
		public void onSubaoIdUpdate(String subaoId) {
			// 什么也不做，因为SubaoId总是在上层改变			
		}
	}

	/**
	 * 定时检查UI层有没有发来Keep Alive消息的线程
	 */
	private static class KeepAliveChecker extends AsyncTask<Void, Void, Void> {

		private final WeakReference<GameVpnService> service;
		private final ConditionVariable active = new ConditionVariable();
		private long updateTime; // 上次保活更新时间

		public KeepAliveChecker(GameVpnService service) {
			this.service = new WeakReference<GameVpnService>(service);
			this.updateTime = now();
		}

		private static long now() {
			return SystemClock.uptimeMillis();
		}

		private synchronized long getUpdateTime() {
			return this.updateTime;
		}

		private synchronized void setUpdateTime(long time) {
			this.updateTime = time;
		}

		/**
		 * 设置Cancel标志，并唤醒线程
		 */
		public void requestTerminate() {
			if (!isCancelled()) {
				cancel(true);
			}
			active.open();
		}

		/**
		 * 收到UI层发来的KeepAlive消息
		 */
		public void onKeepAliveReceiver() {
			setUpdateTime(now());
			active.open();
		}

		/**
		 * 判断手机屏幕是否亮着
		 */
		@SuppressWarnings("deprecation")
		private boolean isScreenOn(Context context) {
			PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			return powerManager != null && powerManager.isScreenOn();	// 这里不要用isInteractive()，那是API 20才提供的
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled()) {
				long delta = now() - getUpdateTime();
				if (delta > 5000) {
					GameVpnService service = this.service.get();
					if (service != null && service.floatWindowSwitch
						&& service.isVPNStart
						&& isScreenOn(service)) {
						// 服务在+悬浮窗开关ON+加速已开启+屏幕亮着，发出通知
						Logger.d(TAG, "UI loss, notify");
						publishProgress();
					} else {
						// 屏幕关闭着的，不发通知
						Logger.d(TAG, "UI loss, but do not notify");
					}
					// 本线程挂起
					active.close();
					active.block();
					if (isCancelled()) {
						break;
					}
				}
				try {
					Thread.sleep(2500);
				} catch (InterruptedException e) {}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			GameVpnService service = this.service.get();
			if (service == null) {
				return;
			}
			Context context = service.getApplicationContext();
			try {
				Intent intent = Intent.makeMainActivity(new ComponentName("cn.wsds.gamemaster", "cn.wsds.gamemaster.ui.ActivityStart"));
				PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
				Notification.Builder builder = new Notification.Builder(context);
				String tickerText = "迅游防清理模式开启";
				builder.setTicker(tickerText);
				builder.setContentTitle(tickerText);
				builder.setContentText("为保证游戏正常加速，悬浮窗等用户服务已关闭。");
				builder.setSmallIcon(R.drawable.notify_icon_normal);
				builder.setContentIntent(pendingIntent);
				@SuppressWarnings("deprecation")
				Notification notification = builder.getNotification();
				//点击跳转后消失
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				if (manager != null) {
					manager.notify(1, notification);
				}
			} catch (RuntimeException e) {}
		}
	}
}
