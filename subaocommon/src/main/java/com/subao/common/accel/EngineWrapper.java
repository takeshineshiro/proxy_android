package com.subao.common.accel;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.subao.common.Disposable;
import com.subao.common.ErrorCode;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.ProxyEngineCommunicator;
import com.subao.common.SwitchState;
import com.subao.common.auth.AuthExecutor;
import com.subao.common.auth.AuthResultReceiverImpl;
import com.subao.common.auth.UserConfig;
import com.subao.common.data.AccelGame;
import com.subao.common.data.AccelGamesDownloader;
import com.subao.common.data.AccelNodesDownloader;
import com.subao.common.data.Address;
import com.subao.common.data.AppType;
import com.subao.common.data.BeaconCounter;
import com.subao.common.data.ChinaISP;
import com.subao.common.data.Config;
import com.subao.common.data.ConvergenceNodesDownloader;
import com.subao.common.data.Defines;
import com.subao.common.data.HRDataTrans;
import com.subao.common.data.InstalledApp;
import com.subao.common.data.LocalScripts;
import com.subao.common.data.ParallelConfigDownloader;
import com.subao.common.data.PersistentData;
import com.subao.common.data.PortalDataDownloader;
import com.subao.common.data.PortalDataEx;
import com.subao.common.data.PortalGeneralConfigDownloader;
import com.subao.common.data.PortalMiscConfigDownloader;
import com.subao.common.data.PortalScriptDownloader;
import com.subao.common.data.QosRegionConfig;
import com.subao.common.data.ServiceConfig;
import com.subao.common.data.ServiceLocation;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.data.SupportGame;
import com.subao.common.data.SupportGameList;
import com.subao.common.data.UserAccelInfoUploader;
import com.subao.common.io.FileOperator;
import com.subao.common.io.Persistent;
import com.subao.common.io.PersistentFactory;
import com.subao.common.jni.InitJNIMode;
import com.subao.common.jni.JniWrapper;
import com.subao.common.model.AccelGameListManager;
import com.subao.common.msg.MessageBuilder;
import com.subao.common.msg.MessageEvent;
import com.subao.common.msg.MessagePersistent;
import com.subao.common.msg.MessageSender;
import com.subao.common.msg.MessageSenderImpl;
import com.subao.common.msg.MessageTools;
import com.subao.common.msg.MessageToolsImpl;
import com.subao.common.msg.MessageUserId;
import com.subao.common.msg.Message_EventMsg;
import com.subao.common.msg.Message_Installation;
import com.subao.common.msg.Message_Link;
import com.subao.common.net.IPInfoQuery;
import com.subao.common.net.NetManager;
import com.subao.common.net.NetSwitch;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.Protocol;
import com.subao.common.net.SignalWatcher;
import com.subao.common.net.SignalWatcherForCellular;
import com.subao.common.parallel.CellularOperator;
import com.subao.common.parallel.NetworkWatcher;
import com.subao.common.qos.QosHelper;
import com.subao.common.qos.QosManager;
import com.subao.common.qos.QosUser4GRegionAndISP;
import com.subao.common.thread.MainHandler;
import com.subao.common.utils.CalendarUtils;
import com.subao.common.utils.InfoUtils;
import com.subao.common.utils.StringUtils;
import com.subao.common.utils.ThreadUtils;
import com.subao.gamemaster.GameMaster;
import com.subao.gamemaster.GameMasterVpnService;
import com.subao.gamemaster.GameMasterVpnServiceInterface;
import com.subao.vpn.JniCallback;
import com.subao.vpn.VPNJni;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * EngineWrapper
 * <p>Created by YinHaiBo on 2017/1/5.</p>
 */

public class EngineWrapper implements AccelEngine, Disposable {

    public static final int INIT_RESULT_ALREADY = 1;
    public static final int INIT_RESULT_SUCCEED = 0;
    public static final int INIT_RESULT_FAILURE = -1;
    public static final int INIT_RESULT_NO_PERMISSION = -2;
    public static final int INIT_RESULT_NOT_IN_MAIN_THREAD = -3;
    public static final int INIT_ILLEGAL_ARGUMENT = -4;

    /**
     * 缺省的UDP ECHO端口
     */
    public static final int DEFAULT_UDP_ECHO_PORT = 222;
    public static final int INVALID_FREE_FLOW_TYPE_VALUE = -1;
    private final static String TAG = LogTag.GAME;

    private final Context context;
    private final int myUID;
    private final HRDataTrans.Arguments hrArguments;
    private final JniWrapper jniWrapper;
    private final String gameGuidOrChannel;
    private final boolean isSDK;
    private final String clientVersion;
    private final String imsi;
    private final NetManager netManager;

    private final PersistentData persistentData;

    private final ServiceConfig serviceConfig = new ServiceConfig();
    private final PortalDataDownloaderArguments portalDataDownloaderArguments;

    private ProxyLooper proxyLooper;
    private volatile boolean accelOpened;
    private int accelNodeCount;

    private WiFiAccel wifiAccel = new WiFiAccelError(ErrorCode.NOT_INIT);
    private MessageSender messageSender;
    private MessageTools messageTools;
    private int freeFlowType = INVALID_FREE_FLOW_TYPE_VALUE;

    private String userId, appId;

    private OnAccelSwitchListener onAccelSwitchListener;

    private ServiceConnectionImpl serviceConnection;

    private ServiceLocation messageServiceLocation;

    /**
     * 本机安装的且支持的游戏列表
     */
    private SupportGameList supportGameList;

    /**
     * 构造一个对象实例
     *
     * @param context           {@link Context}
     * @param moduleType        表示当前为加速模块类型
     * @param gameGuidOrChannel 如果是SDK，则此参数为游戏的GUID；如果是APP，则为APP的友盟渠道号
     * @param clientVersion     客户端版本号
     * @param netManager        {@link NetManager} 对象实例
     * @param jniWrapper        {@link JniWrapper} 对JNI的代理包装对象实例
     */
    public EngineWrapper(
        Context context, Defines.ModuleType moduleType,
        String gameGuidOrChannel, String clientVersion,
        NetManager netManager, JniWrapper jniWrapper,
        boolean useDefaultJniCallback
    ) {
        if (context == null || netManager == null || jniWrapper == null) {
            throw new NullPointerException("Null argument");
        }
        //
        this.context = context.getApplicationContext();
        this.myUID = getMyUID(this.context);
        this.isSDK = Defines.ModuleType.SDK.equals(moduleType);
        this.gameGuidOrChannel = gameGuidOrChannel;
        this.clientVersion = clientVersion;
        this.imsi = getIMSI(context);
        this.jniWrapper = jniWrapper;
        this.netManager = netManager;
        ProxyEngineCommunicator.Instance.set(jniWrapper);
        //
        // 初始化文件系统
        FileOperator.init(context, isSDK);
        this.persistentData = new PersistentData(PersistentFactory.createByFile(
            new File(FileOperator.getDataDirectory(), "proxy_data")
        ));
        // 初始化SubaoId
        initSubaoIdManager(context);
        // 读取为调试/测试而预留的特殊文件
        serviceConfig.loadFromFile(null, isSDK);
        // Portal下载系统所需的参数
        portalDataDownloaderArguments = new PortalDataDownloaderArguments(
            getClientTypeForRequest(),
            clientVersion,
            this.serviceConfig.getPortalServiceLocation(),
            this.netManager);

        //
        // WiFi加速
        try {
            this.wifiAccel = new WiFiAccelImpl(context, jniWrapper,
                portalDataDownloaderArguments);
        } catch (NetworkWatcher.OperationException e) {
            this.wifiAccel = new WiFiAccelError(e.getErrorCode());
        }
        messageTools = new MessageToolsImpl(
            context,
            clientVersion, gameGuidOrChannel,
            imsi,
            this.netManager,
            new MessagePersistent(new MessagePersistentOperator())
        );

        messageServiceLocation = serviceConfig.getMessageServiceLocation();
        if (messageServiceLocation == null) {
            messageServiceLocation = new ServiceLocation(null,
                Address.EndPoint.MESSAGE.host, Address.EndPoint.MESSAGE.port);
        }
        this.messageSender = MessageSenderImpl.create(
            messageServiceLocation,
            messageTools
        );
        this.netManager.setListener(new NetChangeListener(jniWrapper));
        //
        // HR数据上行下行所城的参数
        this.hrArguments = new HRDataTrans.Arguments(
            getClientTypeForRequest(),
            clientVersion,
            this.serviceConfig.getAuthServiceLocation(),
            this.netManager);

        if (useDefaultJniCallback) {
            JniCallback jniCallback = new JniCallbackImpl(this,
                this.jniWrapper,
                new AuthExecutorController(this.netManager, this.messageSender),
                messageServiceLocation,
                hrArguments
            );

            setJniCallBack(jniCallback);
        }
    }

    private static String getIMSI(Context context) {
        String imsi = InfoUtils.getIMSI(context);
        if (TextUtils.isEmpty(imsi)) {
            imsi = "Unknown-IMSI";
        }
        return imsi;
    }

    private static int getMyUID(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        return applicationInfo == null ? 0 : applicationInfo.uid;
    }

    public static String buildDefaultWebUIUrl(String appId, String userId) {
        return String.format("http://service.xunyou.mobi/?appid=%s&userid=%s",
            Misc.encodeUrl(appId), Misc.encodeUrl(userId));
    }

    private static String replaceUrlH5(String url, String replacement) {
        int idx = url.indexOf('?');
        if (idx < 0) {
            return replacement;
        }
        return replacement + url.substring(idx);
    }

    private static void notifyRequestMobileFDResult(JniWrapper jniWrapper, int cid, int error, int fd) {
        boolean canRetry = ErrorCode.canRetryWhenWifiAccelError(error);
        if (Logger.isLoggableDebug(LogTag.PARALLEL)) {
            Log.d(LogTag.PARALLEL, String.format("requestMobileFD() return fd=%d, error=%d, canRetry=%b", fd, error, canRetry));
        }
        jniWrapper.requestMobileFDResult(cid, fd, error, canRetry);
    }

    /**
     * 根据指定{@link InitJNIMode}，决定传递JNI的协议参数
     *
     * @param mode {@link InitJNIMode}
     * @return {@link Protocol}
     * FIXME 这里要跟坤叔确定一下，ROM/APP传递什么协议
     */
    static Protocol getNetProtocol(InitJNIMode mode) {
        switch (mode) {
        case TCP:
            return Protocol.TCP;
        default:
            return Protocol.UDP;
        }
    }

    /**
     * 返回“本机安装且支持的游戏列表”
     *
     * @return {@link SupportGameList} or null
     */
    public SupportGameList getSupportGameList() {
        return this.supportGameList;
    }

    public void setJniCallBack(JniCallback jniCallback) {
        this.jniWrapper.setJniCallback(jniCallback);
    }

    /**
     * 返回 {@link HRDataTrans.Arguments}
     * <i>APP需用</i>
     */
    public HRDataTrans.Arguments getHrArguments() {
        return hrArguments;
    }

    /**
     * 返回{@link MessageSender}
     * FIXME 这里存疑：谁要取这个消息发送器？它只能在APP的Service模块里工作，不应该同时出现在两个模块中？？
     */
    public MessageSender getMessageSender() {
        return messageSender;
    }

    public ServiceLocation getMessageServiceLocation() {
        return messageServiceLocation;
    }

    private String getClientTypeForRequest() {
        return isSDK ? this.gameGuidOrChannel : Defines.REQUEST_CLIENT_TYPE_FOR_APP;
    }

    /**
     * 初始化{@link SubaoIdManager}
     *
     * @param context {@link Context}
     */
    private void initSubaoIdManager(Context context) {
        SubaoIdManager subaoIdManager = SubaoIdManager.getInstance();
        subaoIdManager.registerObserver(new SubaoIdManager.Observer() {
            @Override
            public void onSubaoIdChange(String subaoId) {
                MessageUserId.setCurrentSubaoId(subaoId);
                jniWrapper.setString(0, Defines.VPNJniStrKey.KEY_SUBAO_ID, subaoId);
            }
        });
        subaoIdManager.init(context);
        MessageUserId.setCurrentSubaoId(SubaoIdManager.getInstance().getSubaoId());
    }

    @Override
    public void dispose() {
        synchronized (this) {
            ProxyLooper proxyLooper = this.proxyLooper;
            this.proxyLooper = null;
            if (proxyLooper != null) {
                proxyLooper.setTerminateFlag();
            }
            netManager.dispose();
            jniWrapper.dispose();
            if (this.wifiAccel != null) {
                this.wifiAccel.dispose();
            }
        }
    }

    /**
     * 初始化加速引擎
     *
     * @param mode                       初始化模式
     * @param hookModule                 要HOOK的模块
     * @param udpEchoPort                UDP的ECHO端口
     * @param jsonOfDefaultAccelGameList 缺省的“加速游戏列表”，JSON格式，与Portal原始数据格式一致，在没有本地缓存数据的时候使用
     * @return 0表示成功，其它值表示失败
     */
    @SuppressLint("DefaultLocale")
    public int init(
        InitJNIMode mode,
        String hookModule,
        int udpEchoPort,
        byte[] jsonOfDefaultAccelGameList
    ) {
        int result = InitExecutor.beforeInitPreCheck(this);
        if (0 != result) {
            return result;
        }
        // 初始化鉴权模块
        AuthExecutor.init(
            serviceConfig.getAuthServiceLocation(),
            getClientTypeForRequest(),
            null);
        // 发起Portal脚本下载的请求，并取上一次缓存在本地的脚本
        byte[] luaPCode = InitExecutor.loadLocalScriptAndStartDownload(portalDataDownloaderArguments);
        // 发起Portal加速节点列表下载的请求，并取上一次缓存在本地的加速节点列表
        AccelNodesDownloader.NodesInfo accelNodesInfo = InitExecutor.loadLocalAccelNodesAndStartDownload(
            portalDataDownloaderArguments, serviceConfig.getNodesInfo());
        this.accelNodeCount = (accelNodesInfo == null) ? 0 : accelNodesInfo.count;  // 记录一下节点个数，上报消息时要用
        // 汇聚节点列表
        String convergenceNodesForJNI = ConvergenceNodesDownloader.start(portalDataDownloaderArguments, jniWrapper);
        if (Logger.isLoggableDebug(LogTag.DATA)) {
            Log.d(LogTag.DATA,
                String.format("Init Proxy, Accel Nodes=%s, Convergence=%s",
                    StringUtils.objToString(accelNodesInfo),
                    convergenceNodesForJNI == null ? "null" : String.format("%d chars", convergenceNodesForJNI.length())
                ));
        }
        // 初始化
        String accelNodesForJNI = (accelNodesInfo == null) ? null : accelNodesInfo.dataForJNI;
        boolean r = jniWrapper.initJNI(
            netManager.getCurrentNetworkType().value, mode,
            luaPCode,
            accelNodesForJNI, convergenceNodesForJNI
        );
        // 如果初始化成功，要做的事情
        if (r) {
            InitExecutor.startJNIProxyLoop(mode);
            InitExecutor.processLocalDebugScripts(jniWrapper);
            jniWrapper.setString(0, Defines.VPNJniStrKey.KEY_SDK_GUID, gameGuidOrChannel); // 无论什么模式均需设置
            if (mode == InitJNIMode.VPN) {
                this.supportGameList = InitExecutor.processAccelGameList(context, jniWrapper, portalDataDownloaderArguments, jsonOfDefaultAccelGameList);
            } else {
                InitExecutor.processGameSDK(context, jniWrapper, getNetProtocol(mode), hookModule);
            }
            InitExecutor.setEchoPort(jniWrapper, udpEchoPort);
            InitExecutor.processPortalDownload(jniWrapper, portalDataDownloaderArguments);
            this.proxyLooper = InitExecutor.createAndStartProxyLooper(jniWrapper);
            // 告之JNI一些必要的信息（主要用于消息上报）
            MessageBuilder messageBuilder = messageTools.getMessageBuilder();
            jniWrapper.setMessageInformation(messageBuilder.getVersionInfo(), messageBuilder.getDeviceInfo());
            //
            // init被调用时，游戏肯定在前台
            this.gameForeground(true);
        }
        return r ? INIT_RESULT_SUCCEED : INIT_RESULT_FAILURE;
    }

    private void afterAccelSwitch() {
        if (accelOpened) {
            OnAccelStartRunner.runInAndroidUIThread(
                context,
                this,
                messageSender, messageTools,
                this.accelNodeCount,
                onAccelSwitchListener);
        } else {
            OnAccelStopRunner.runInAndroidUIThread(onAccelSwitchListener);
        }
    }

    @Override
    public boolean startAccel() {
        if (proxyLooper == null) {
            return false; // 尚未初始化
        }
        boolean result;
        synchronized (this) {
            if (!accelOpened) {
                accelOpened = jniWrapper.startProxy();
                afterAccelSwitch();
            }
            result = accelOpened;
        }
        return result;
    }

    @Override
    public void stopAccel() {
        if (proxyLooper == null) {
            return;
        }
        synchronized (this) {
            if (accelOpened) {
                jniWrapper.stopProxy();
                accelOpened = false;
                afterAccelSwitch();
            }
        }
    }

    @Override
    public boolean isAccelOpened() {
        return accelOpened;
    }

    @Override
    public boolean startVPN(int fd) {
        return jniWrapper.startVPN(fd);
    }

    @Override
    public void stopVPN() {
        jniWrapper.stopVPN();
    }

    public void setOnAccelSwitchListener(OnAccelSwitchListener listener) {
        this.onAccelSwitchListener = listener;
    }

    public void setUserToken(String userId, String token, String appId) {
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, String.format("setUserToken(%s, %s, %s)", userId, token, appId));
        }
        this.stopAccel(); // 重要！
        this.appId = appId;
        this.userId = userId;
        MessageUserId.resetUserInfo(userId);
        IPInfoQuery.onUserAuthComplete(false, null);
        jniWrapper.setUserToken(userId, token, appId);
    }

    /**
     * 申请一个新Mobile FD
     *
     * @return 成功返回一个基于移动网络的FD，失败抛出{@link NetworkWatcher.OperationException}
     * @throws NetworkWatcher.OperationException
     */
    public int requestNewMobileFD() throws NetworkWatcher.OperationException {
        return this.wifiAccel.requestNewMobileFD(this.context);
    }

    public int getCurrentUserFreeFlowType() {
        return this.freeFlowType;
    }

    public void onNetDelay(int millis) {
        jniWrapper.onUDPDelay(millis);
    }

    /**
     * 由游戏厂商调用，设置UDP Echo服务器的端口
     */
    public void setUdpEchoPort(int port) {
        jniWrapper.setUDPEchoPort(port);
    }

    /**
     * 返回是否需要开启加速的建议结果
     */
    public int getAccelRecommendation() {
        // 就算是要用测试配置文件里的值，也需要先调用JNI的相关函数
        int resultFromJNI = jniWrapper.getAccelRecommendation();
        if (serviceConfig.getAccelRecommendation() != null) {
            return serviceConfig.getAccelRecommendation();
        }
        boolean isAccelOpened = this.isAccelOpened();
        NetTypeDetector.NetType netType = netManager.getCurrentNetworkType();
        if (MessageEvent.ReportAllow.getTencent()) {
            messageSender.offerEvent(
                messageTools.getMessageBuilder().buildMessageEvent_AccelRecommendation(
                    MessageUserId.build(),
                    resultFromJNI, netType.value, isAccelOpened));
        }
        return resultFromJNI;
    }

    public boolean isFreeFlowUser() {
        return this.freeFlowType >= 0 && this.freeFlowType <= 2;
    }

    /**
     * 王者荣耀特殊需求：设置当前用户是哪种免流用户
     *
     * @param type 类型，取值为0、1、2（参见{@link FreeFlowType}）或-1表示“非免流用户”
     */
    public void setFreeFlowUser(int type) {
        this.freeFlowType = type;
        jniWrapper.setInt(0, Defines.VPNJniStrKey.KEY_FREE_FLOW_TYPE, type);
    }

    /**
     * 游戏告知引擎，要对哪个游戏地址和端口进行加速建议？
     */
    public void setRecommendationGameIP(String ip, int port) {
        jniWrapper.setRecommendationGameIP(ip, port);
    }

    /**
     * 王者荣耀将本局的延迟值统计数据通知SDK（Version 2，2016.12.12）
     *
     * @param average         平均值
     * @param variance        方差
     * @param lostRate        丢包率
     * @param exRateOfNewPing newping大于300的异常包占比
     * @param exRateOfPing    ping大于150的异常包占比
     */
    public void onNetDelayQuality2(float average, float variance, float lostRate, float exRateOfNewPing, float exRateOfPing) {
        Message_Link.DelayQualityV2 delayQualityV2 = new Message_Link.DelayQualityV2(
            average, (float) Math.sqrt(variance), lostRate, exRateOfNewPing, exRateOfPing, 0f
        );
        messageSender.onNetDelayQualityV2(delayQualityV2);
        boolean log = Logger.isLoggableDebug(TAG);
        if (log) {
            Log.d(TAG, "onNetDelayQualityV2: " + delayQualityV2.toString());
        }
        if (isAccelOpened()) {
            int state = 0;
            String currentUserConfig = MessageUserId.getCurrentUserConfig();
            if (log) {
                Log.d(TAG, "Current User Config: " + currentUserConfig);
            }
            boolean isParallelOn = UserConfig.isParallelSwitchOn(currentUserConfig);
            if (isParallelOn) {
                if (ParallelConfigDownloader.isPhoneParallelSupported()) {
                    state = 1;
                } else {
                    if (log) {
                        Log.d(TAG, "Phone parallel not support");
                    }
                }
            }
            if (log) {
                Log.d(TAG, "The state of feedback: " + state);
            }
        } else {
            if (log) {
                Log.d(TAG, "Accel not opened");
            }
        }
    }

    /**
     * 游戏向SDK请求内嵌页面的网址
     */
    public String getWebUIUrl() {
        // 临时逻辑，先取一下移动网络开关状态
        SwitchState switchState = NetSwitch.getMobileSwitchState(context);
        jniWrapper.setInt(0, Defines.VPNJniStrKey.KEY_MOBILE_SWITCH_STATE, switchState.getId());
        //
        String result = jniWrapper.getWebUIUrl();
        if (!TextUtils.isEmpty(result)) {
            String urlH5 = serviceConfig.getUrlH5();
            if (!TextUtils.isEmpty(urlH5)) {
                result = replaceUrlH5(result, urlH5);
            }
        }
        if (TextUtils.isEmpty(result)) {
            result = buildDefaultWebUIUrl(appId, userId);
        }
        Logger.d(TAG, result);
        return result;
    }

    public void gameForeground(boolean isForeground) {
        jniWrapper.setInt(0, Defines.VPNJniStrKey.KEY_FRONT_GAME_UID,
            isForeground ? myUID : -1);
    }

    public String getVIPValidTime() {
        return jniWrapper.getVIPValidTime();
    }

    public void setGameId(String id) {
        jniWrapper.setString(0, Defines.VPNJniStrKey.KEY_GAME_SERVER_ID, id);
    }

    public int getAccelerationStatus() {
        return jniWrapper.getAccelerationStatus();
    }

    public void uploadUserConfig(String userId, String userConfig) {
        AuthExecutor.setUserConfig(
            new AuthExecutorController(netManager, messageSender),
            0, null, userId, userConfig
        );
    }

    public boolean isUDPProxy() {
        return jniWrapper.getSDKUDPIsProxy();
    }

    public int getCurrentConnectionType() {
        return netManager.getCurrentNetworkType().value;
    }

    public void openProxyLog() {
        jniWrapper.openProxyLog();
    }

    public void setGameServerIP(String ip) {
        jniWrapper.setSDKGameServerIP(ip);
    }

    /**
     * 当{@link #getAccelRecommendation()}返回如下返回“加速建议”扩展数据
     *
     * @param type 类型
     * @return 相关方案或URL
     */
    public String getAccelRecommendationData(int type) {
        return jniWrapper.getAccelRecommendationData(type);
    }

    /**
     * 提醒反馈
     * <p>当{@link #getAccelRecommendation()}返回值引发弹出对话框后，用户点击确认或取消都通知SDK</p>
     *
     * @param isConfirm true表示用户点击了确认，false表示点击了取消
     */
    public void onAccelRecommendationResult(int type, boolean isConfirm) {
        jniWrapper.onAccelRecommendationResult(type, isConfirm);
    }

    public HRDataTrans.Arguments getHRArguments() {
        return this.hrArguments;
    }

    /**
     * 游戏向SDK设置“支付方式白名单”，即：只允许哪些支付方式出现
     *
     * @param whiteList 支付方式白名单字符串，每个字符表示特定的允许方式
     */
    public void setPayTypeWhiteList(String whiteList) {
        jniWrapper.setString(0, Defines.VPNJniStrKey.KEY_PAY_TYPE_WHITE_LIST, whiteList);
    }

    /**
     * 游戏通知SDK：用户允许/禁止了WiFi加速功能
     *
     * @param on true表示允许，false表示禁止
     */
    public void setWiFiAccelSwitch(boolean on) {
        jniWrapper.setInt(0, Defines.VPNJniStrKey.KEY_USER_WIFI_ACCEL, on ? 1 : 0);
        String userId = MessageUserId.getCurrentUserId();
        if (!TextUtils.isEmpty(userId)) {
            AuthExecutor.setUserConfig_ParallelOnly(
                new AuthExecutorController(netManager, messageSender),
                0, // cid
                userId,
                on
            );
        }
    }

    /**
     * 开启并连接VPN
     */
    public synchronized boolean openVPN() {
        boolean result;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            result = false;
        } else if (this.serviceConnection != null) {
            return this.serviceConnection.startProxy();
        } else {
            ServiceConnectionImpl serviceConnection = new ServiceConnectionImpl(this.getSupportGameList());
            result = GameMasterVpnService.open(context, serviceConnection);
            if (result) {
                this.serviceConnection = serviceConnection;
            }
        }
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, "open vpn result: " + result);
        }
        return result;
    }

    /**
     * 关闭VPN
     */
    public synchronized void closeVPN() {
        if (serviceConnection != null) {
            serviceConnection.stopProxy();
//            context.unbindService(serviceConnection);
//            serviceConnection = null;
        }
    }

    /**
     * APP或ROM专用：针对某一款游戏，开始检测节点
     *
     * @param uid 游戏的UID
     */
    public void startNodeDetect(int uid) {
        jniWrapper.startNodeDetect(uid);
    }

    /**
     * APP或ROM专用：判断给定的UID，是否已完成节点检测
     *
     * @param uid 游戏的UID
     */
    public boolean isNodeDetected(int uid) {
        return jniWrapper.isNodeDetected(uid);
    }

    /**
     * 免流类型
     */
    public enum FreeFlowType {
        /**
         * 移动
         */
        CMCC(0),

        /**
         * 联通
         */
        CNC(1),

        /**
         * 电信
         */
        CTC(2);

        public final int id;

        FreeFlowType(int id) {
            this.id = id;
        }
    }

    private interface WiFiAccel extends Disposable {

        /**
         * 请求创建一个基于4G网络的FD
         *
         * @param context {@link Context}
         * @return 成功返回FD，失败抛出异常
         * @throws NetworkWatcher.OperationException
         */
        int requestNewMobileFD(Context context) throws NetworkWatcher.OperationException;
    }

    /**
     * 加速开关侦听器
     */
    public interface OnAccelSwitchListener {

        /**
         * 当加速开关时被调用
         *
         * @param accelOn true表示当前切换到“开”，否则为false
         */
        void onAccelSwitch(boolean accelOn);
    }

    private static class ProxyLooper extends Thread {

        private JniWrapper jniWrapper;
        private volatile boolean terminateFlag;

        ProxyLooper(JniWrapper jniWrapper) {
            this.jniWrapper = jniWrapper;
        }

        @Override
        public void run() {
            while (!terminateFlag) {
                this.jniWrapper.processEvent(); // JNI的processEvent会阻塞
            }
            jniWrapper = null;
        }

        public void setTerminateFlag() {
            this.terminateFlag = true;
        }
    }

    private static class WiFiAccelError implements WiFiAccel {

        private int errorCode;

        WiFiAccelError(int errorCode) {
            this.errorCode = errorCode;
        }

        @Override
        public int requestNewMobileFD(Context context) throws NetworkWatcher.OperationException {
            throw new NetworkWatcher.OperationException(errorCode);
        }

        @Override
        public void dispose() {
            // do nothing
        }
    }

    private static class WiFiAccelImpl implements WiFiAccel {

        private static final String TAG = LogTag.PARALLEL;

        /**
         * {@link CellularOperator}，用于创建基于移动网络的FD
         */
        private final CellularOperator cellularOperator;

        WiFiAccelImpl(
            Context context, JniWrapper jniWrapper, PortalDataDownloader.Arguments portalArguments
        ) throws NetworkWatcher.OperationException {
            this.cellularOperator = CellularOperator.create(context, new CellularStateListener(context, jniWrapper));
            ParallelConfigDownloader.start(portalArguments, jniWrapper);
        }

        @Override
        public void dispose() {
            cellularOperator.dispose();
        }

        /**
         * 申请一个基于4G的FD
         *
         * @param context {@link Context}
         * @return 成功返回FD，失败抛出{@link NetworkWatcher.OperationException}
         * @throws NetworkWatcher.OperationException
         */
        @Override
        public int requestNewMobileFD(Context context) throws NetworkWatcher.OperationException {
            if (!ParallelConfigDownloader.isPhoneParallelSupported()) {
                throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_MODEL_NOT_ALLOW);
            }
            return cellularOperator.requestNewMobileFD(context);
        }

    }

    private static class MessagePersistentOperator implements MessagePersistent.Operator {

        private final File directory;

        MessagePersistentOperator() {
            directory = new File(FileOperator.getDataDirectory(), "links");
            if (!directory.exists() || !directory.isDirectory()) {
                directory.mkdirs();
            }
        }

        @Override
        public RandomAccessFile openFile(String filename, boolean readonly) throws IOException {
            return new RandomAccessFile(new File(directory, filename), readonly ? "r" : "rw");
        }

        @Override
        public String[] enumFilenames() {
            return directory.list();
        }

        @Override
        public void delete(String filename) {
            File file = new File(directory, filename);
            if (!file.delete()) {
                Log.w(TAG, "Delete File Failed: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * 当开启加速的时候要执行的一些事情
     */
    private static class OnAccelStartRunner implements Runnable {

        private final Context context;
        private final AccelEngine accelEngine;
        private final MessageSender messageSender;
        private final MessageTools messageTools;
        private final int nodeCount;
        private final OnAccelSwitchListener onAccelSwitchListener;

        private OnAccelStartRunner(
            Context context,
            AccelEngine accelEngine,
            MessageSender messageSender, MessageTools messageTools,
            int nodeCount,
            OnAccelSwitchListener onAccelSwitchListener
        ) {
            this.context = context;
            this.accelEngine = accelEngine;
            this.messageSender = messageSender;
            this.messageTools = messageTools;
            this.nodeCount = nodeCount;
            this.onAccelSwitchListener = onAccelSwitchListener;
        }

        static void runInAndroidUIThread(
            Context context,
            AccelEngine accelEngine,
            MessageSender messageSender, MessageTools messageTools,
            int nodeCount,
            OnAccelSwitchListener onAccelSwitchListener
        ) {
            OnAccelStartRunner runner = new OnAccelStartRunner(
                context,
                accelEngine,
                messageSender, messageTools,
                nodeCount,
                onAccelSwitchListener);
            if (ThreadUtils.isInAndroidUIThread()) {
                runner.run();
            } else {
                MainHandler.getInstance().post(runner);
            }
        }

        /**
         * 如果没有SubaoId，上报Installation消息
         *
         * @return null表示生成了Installation消息并投入消息上报队列。返回合法的SubaoId表示勿需上报
         */
        private String sendInstallationMessagesIfNeed() {
            String subaoId = SubaoIdManager.getInstance().getSubaoId();
            if (SubaoIdManager.isSubaoIdValid(subaoId)) {
                Logger.d(LogTag.MESSAGE, "SubaoId already exists, do not send INSTALLATION message.");
                return subaoId;
            }
            Logger.d(LogTag.MESSAGE, "No SubaoId found, make INSTALLATION message.");
            Message_Installation msg = messageTools.getMessageBuilder().buildMessageInstallation(
                System.currentTimeMillis() / 1000,
                Message_Installation.UserInfo.create(context),
                null);
            messageSender.offerInstallation(msg);
            return null;
        }

        @Override
        public void run() {
            if (onAccelSwitchListener != null) {
                onAccelSwitchListener.onAccelSwitch(true);
            }
            String subaoId = sendInstallationMessagesIfNeed();
            if (subaoId == null) {
                // 还没有SubaoId，本次就不发Start消息和Event了
                return;
            }
            if (StartMessageCheckLooper.isMessageStartAlreadySentToday()) {
                // 今天已报过，启动一个轮询，跨零点的时候再报Start
                StartMessageCheckLooper.start(accelEngine, messageSender, nodeCount);
            } else {
                StartMessageCheckLooper.sendMessageStart(messageSender, nodeCount);
            }
        }

    }

    /**
     * 当关闭加速时要执行的一些事情
     */
    private static class OnAccelStopRunner implements Runnable {

        private final OnAccelSwitchListener onAccelSwitchListener;

        OnAccelStopRunner(OnAccelSwitchListener onAccelSwitchListener) {
            this.onAccelSwitchListener = onAccelSwitchListener;
        }

        static void runInAndroidUIThread(OnAccelSwitchListener onAccelSwitchListener) {
            OnAccelStopRunner runner = new OnAccelStopRunner(onAccelSwitchListener);
            if (ThreadUtils.isInAndroidUIThread()) {
                runner.run();
            } else {
                MainHandler.getInstance().post(runner);
            }
        }

        @Override
        public void run() {
            if (onAccelSwitchListener != null) {
                onAccelSwitchListener.onAccelSwitch(false);
            }
            StartMessageCheckLooper.stop();
        }
    }

    private static class StartMessageCheckLooper {

        private static Worker worker;

        static void postWorker() {
            MainHandler.getInstance().postDelayed(worker, 1000 * 60 * 10);
        }

        static void start(AccelEngine accelEngine, MessageSender messageSender, int nodeCount) {
            if (worker == null) {
                worker = new Worker(accelEngine, messageSender, nodeCount);
                postWorker();
            }
        }

        static void stop() {
            if (worker != null) {
                MainHandler.getInstance().removeCallbacks(worker);
                worker = null;
            }
        }

        /**
         * 今日是否已经上报过Start消息
         */
        static boolean isMessageStartAlreadySentToday() {
            return (CalendarUtils.todayCST() == Config.getInstance().getDayReportStartMessage());
        }

        static void sendMessageStart(MessageSender messageSender, int nodeCount) {
            messageSender.offerStart(
                nodeCount,  // 节点个数
                0,  // 游戏个数（SDK总是填0）
                null    // 安装的应用列表（SDK总是填空）
            );
        }

        private static class Worker implements Runnable {

            private final AccelEngine accelEngine;
            private final MessageSender messageSender;
            private final int nodeCount;

            Worker(AccelEngine accelEngine, MessageSender messageSender, int nodeCount) {
                this.accelEngine = accelEngine;
                this.messageSender = messageSender;
                this.nodeCount = nodeCount;
            }

            @Override
            public void run() {
                if (accelEngine.isAccelOpened()) {
                    String subaoId = MessageUserId.getCurrentSubaoId();
                    if (SubaoIdManager.isSubaoIdValid(subaoId) && !isMessageStartAlreadySentToday()) {
                        sendMessageStart(messageSender, nodeCount);
                    }
                }
                postWorker();
            }
        }
    }

    static class QosTools implements QosHelper.Tools {

        private final String channel;
        private final String clientVersion;
        private final NetTypeDetector netTypeDetector;
        private final String imsi;

        QosTools(String channel, String clientVersion, NetTypeDetector netTypeDetector, String imsi) {
            this.channel = channel;
            this.clientVersion = clientVersion;
            this.netTypeDetector = netTypeDetector;
            this.imsi = imsi;
        }

        @Override
        public boolean isValidLocalIp(byte[] ip) {
            return ip != null;
        }

        @Override
        public AppType getAppType() {
            return AppType.ANDROID_SDK;
        }

        @Override
        public String getChannel() {
            return channel;
        }

        @Override
        public String getVersionNum() {
            return this.clientVersion;
        }

        @Override
        public String getSubaoId() {
            return SubaoIdManager.getInstance().getSubaoId();
        }

        @Override
        public NetTypeDetector getNetTypeDetector() {
            return this.netTypeDetector;
        }

        @Override
        public String getIMSI() {
            return this.imsi;
        }

    }

    private static class PortalDataDownloaderArguments extends PortalDataDownloader.Arguments {
        public PortalDataDownloaderArguments(String clientType, String version, ServiceLocation serviceLocation, NetTypeDetector netTypeDetector) {
            super(clientType, version, serviceLocation, netTypeDetector);
        }

        @Override
        public Persistent createPersistent(String filename) {
            File file = FileOperator.getDataFile(filename);
            return PersistentFactory.createByFile(file);
        }
    }

    /**
     * 侦听网络改变的 {@link NetManager.Listener}
     */
    private static class NetChangeListener implements NetManager.Listener {

        private final JniWrapper jniWrapper;

        public NetChangeListener(JniWrapper jniWrapper) {
            this.jniWrapper = jniWrapper;
        }

        @Override
        public void onConnectivityChange(NetTypeDetector.NetType netType) {
            // 这里做一个兜底优化，Unknown认为是4G
            if (netType == NetTypeDetector.NetType.UNKNOWN) {
                netType = NetTypeDetector.NetType.MOBILE_4G;
            }
            jniWrapper.setInt(0, Defines.VPNJniStrKey.KEY_NET_STATE, netType.value);
            switch (netType) {
            case MOBILE_4G:
            case UNKNOWN:
                QosUser4GRegionAndISP.getInstance().onNetChangeTo4G();
                break;
            default:
                QosUser4GRegionAndISP.getInstance().onNetChangeToNot4G();
                break;
            }
        }
    }

    public static class AuthExecutorController implements AuthExecutor.Controller {

        private final NetTypeDetector netTypeDetector;
        private final MessageSender messageSender;

        public AuthExecutorController(NetTypeDetector netTypeDetector, MessageSender messageSender) {
            if (netTypeDetector == null) {
                throw new NullPointerException("NetTypeDetector cannot be null");
            }
            if (messageSender == null) {
                throw new NullPointerException("MessageSender cannot be null");
            }
            this.netTypeDetector = netTypeDetector;
            this.messageSender = messageSender;
        }

        @Override
        public boolean isNetConnected() {
            return netTypeDetector.isConnected();
        }

        @Override
        public MessageEvent.Reporter getEventReporter() {
            return new MessageEvent.Reporter() {
                @Override
                public void reportEvent(String eventName, String eventParam) {
                    if (MessageEvent.ReportAllow.getAuth()) {
                        messageSender.offerEvent(eventName, eventParam);
                    }
                }
            };
        }

        @Override
        public String getClientVersion() {
            return GameMaster.VERSION_NAME;
        }

    }

    /**
     * 当申请移动FD失败，且网络开关状态为开的时候，延时一段时间以检测网络信号强度
     */
    static class SignalStrengthDetector {

        private SignalStrengthDetector() {
        }

        /**
         * 延时一段时间以得到网络信号强度，重新生成新的错误码通知JNI
         *
         * @param context      {@link Context}
         * @param handler      用哪个{@link Handler}来执行延时操作
         * @param rawErrorCode 原始的错误码，可能是{@link ErrorCode#WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON}或{@link ErrorCode#WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN}
         * @param jniWrapper   工作结束后向这个{@link JniWrapper}发送通知
         * @param cid          通知JNI所用的call Id
         */
        static void execute(Context context, Handler handler, int rawErrorCode, JniWrapper jniWrapper, int cid) {
            Worker worker = new Worker(rawErrorCode, jniWrapper, cid);
            worker.start(context, handler);
        }

        private static class Worker implements Runnable, SignalWatcher.Listener {

            /**
             * 最早的错误代码，可能是{@link ErrorCode#WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON}
             * 或{@link ErrorCode#WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN}中的一个
             */
            private final int rawErrorCode;
            private final JniWrapper jniWrapper;
            private final int cid;

            private SignalWatcher signalWatcher;
            private int strengthPercent = -1;

            public Worker(int rawErrorCode, JniWrapper jniWrapper, int cid) {
                this.rawErrorCode = rawErrorCode;
                this.jniWrapper = jniWrapper;
                this.cid = cid;
            }

            public void start(Context context, Handler handler) {
                assert this.signalWatcher == null;
                this.signalWatcher = new SignalWatcherForCellular(this);
                this.signalWatcher.start(context);
                handler.postDelayed(this, 1000L);
            }

            @Override
            public void onSignalChange(int strengthPercent) {
                this.strengthPercent = strengthPercent;
            }

            @Override
            public void run() {
                this.signalWatcher.shutdown();
                int error;
                if (this.strengthPercent < 0) {
                    error = rawErrorCode;
                } else {
                    error = ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_SIGNAL_STRENGTH + this.strengthPercent;
                }
                notifyRequestMobileFDResult(jniWrapper, cid, error, -1);
            }
        }
    }

    /**
     * 蜂窝网状态侦听器
     * <p>当蜂窝网可用/不可用时通知JNI</p>
     */
    static class CellularStateListener implements CellularOperator.CellularStateListener {

        private final Context context;
        private final JniWrapper jniWrapper;

        CellularStateListener(Context context, JniWrapper jniWrapper) {
            this.context = context.getApplicationContext();
            this.jniWrapper = jniWrapper;
        }

        @Override
        public void onCellularStateChange(boolean available) {
            jniWrapper.setInt(0, Defines.VPNJniStrKey.KEY_CELLULAR_STATE_CHANGE, available ? 1 : 0);
            String tag = LogTag.PARALLEL;
            if (Logger.isLoggableDebug(tag)) {
                Log.d(tag, available ? "Cellular available" : "Cellular lost");
                Log.d(tag, "Mobile Switch State: " + NetSwitch.getMobileSwitchState(context).getId());
            }
        }
    }

    static class ServiceConnectionImpl implements ServiceConnection {

        private final List<String> supportPackageNameList;
        private GameMasterVpnServiceInterface vpnServiceInterface;

        ServiceConnectionImpl(SupportGameList supportGameList) {
            if (supportGameList != null) {
                supportPackageNameList = supportGameList.getPackageNameList();
            } else {
                supportPackageNameList = null;
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d(TAG, "service connected: " + name);
            this.vpnServiceInterface = GameMasterVpnServiceInterface.Stub.asInterface(service);
            startProxy();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.d(TAG, "service disconnected: " + name);
            vpnServiceInterface = null;
        }

        public boolean startProxy() {
            GameMasterVpnServiceInterface vpnServiceInterface = this.vpnServiceInterface;
            if (vpnServiceInterface != null) {
                try {
                    return 0 == vpnServiceInterface.startProxy(supportPackageNameList);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        public void stopProxy() {
            GameMasterVpnServiceInterface vpnServiceInterface = this.vpnServiceInterface;
            if (vpnServiceInterface != null) {
                try {
                    vpnServiceInterface.stopProxy();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public int protectSocket(int socket) {
            GameMasterVpnServiceInterface vpnServiceInterface = this.vpnServiceInterface;
            if (vpnServiceInterface != null) {
                try {
                    if (vpnServiceInterface.protectSocket(socket)) {
                        return ErrorCode.OK;
                    } else {
                        return ErrorCode.VPN_PROTECT_SOCKET_FAIL;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return ErrorCode.VPN_PROTECT_SOCKET_FAIL;
                }
            }
            return ErrorCode.VPN_SERVICE_NOT_EXISTS;
        }
    }

    public static class JniCallbackImpl implements JniCallback {

        private final EngineWrapper engineWrapper;
        private final JniWrapper jniWrapper;
        private final AuthExecutor.Controller authExecutorController;
        private final ServiceLocation messageServiceLocation;
        private final HRDataTrans.Arguments hrTransArguments;

        public JniCallbackImpl(EngineWrapper engineWrapper,
                               JniWrapper jniWrapper, AuthExecutorController authExecutorController,
                               ServiceLocation messageServiceLocation,
                               HRDataTrans.Arguments hrTransArguments
        ) {
            this.engineWrapper = engineWrapper;
            this.jniWrapper = jniWrapper;
            this.authExecutorController = authExecutorController;
            this.messageServiceLocation = messageServiceLocation;
            this.hrTransArguments = hrTransArguments;
        }

        @Override
        public void onProxyActive(boolean open) {
            synchronized (engineWrapper) {
                if (engineWrapper.accelOpened != open) {
                    engineWrapper.accelOpened = open;
                    engineWrapper.afterAccelSwitch();
                }
            }
        }

        private AuthResultReceiverImpl createAuthResultReceiver() {
            ServiceLocation serviceLocation = engineWrapper.serviceConfig.getAuthServiceLocation();
            HRDataTrans.Arguments arguments
                = new HRDataTrans.Arguments(engineWrapper.getClientTypeForRequest(),
                engineWrapper.clientVersion, serviceLocation, engineWrapper.netManager);
            return new AuthResultReceiverImpl(jniWrapper, arguments, messageServiceLocation);
        }

        @Override
        public void requestUserAuth(final int cid, final String userId, final String token, final String appId) {
            MainHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    AuthExecutor.getJWTToken(authExecutorController, cid, userId, token, appId, createAuthResultReceiver());
                }
            });
        }

        @Override
        public void requestLinkAuth(final int cid, final String nodeIP, final String jwtToken) {
            MainHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    AuthExecutor.getToken(authExecutorController, cid, nodeIP, jwtToken, createAuthResultReceiver());
                }
            });
        }

        @Override
        public void requestUserConfig(final int cid, final String userId, final String jwtToken) {
            MainHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    AuthExecutor.getConfigs(authExecutorController, cid, jwtToken, userId, createAuthResultReceiver());
                }
            });
        }

        @Override
        public void requestUserState(final int cid, final String userId, final String jwtToken) {
            MainHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    AuthExecutor.getUserAccelStatus(authExecutorController, cid, userId, jwtToken, createAuthResultReceiver());
                }
            });
        }

        @Override
        public void requestMobileFD(final int cid) {
            Logger.d(LogTag.PARALLEL, "Proxy request mobile fd ...");
            MainHandler.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    int fd, error;
                    try {
                        fd = engineWrapper.requestNewMobileFD();
                        error = 0;
                    } catch (NetworkWatcher.OperationException e) {
                        fd = -1;
                        error = e.getErrorCode();
                    }
                    //
                    if (error == ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON
                        || error == ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN
                        ) {
                        // 没有可用的移动FD，而网络开关又是ON或Unknown的状态
                        // 尝试得到信号强度以后，重新生成错误码通知JNI
                        SignalStrengthDetector.execute(engineWrapper.context,
                            MainHandler.getInstance(), error, jniWrapper, cid);
                    } else {
                        notifyRequestMobileFDResult(jniWrapper, cid, error, fd);
                    }
                }
            });
        }

        @Override
        public void requestISPInformation(int cid) {
            Logger.d(LogTag.DATA, "Proxy request region and isp ...");
            IPInfoQuery.execute(null, new IPInfoQuery.Callback() {
                @Override
                public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
                    if (result != null) {
                        ChinaISP chinaISP = result.getISP();
                        String numISP = (chinaISP == null) ? "1" : Integer.toString(chinaISP.num);
                        int cid = (Integer) callbackContext;
                        jniWrapper.setString(cid, Defines.VPNJniStrKey.KEY_ISP, Integer.toString(result.region) + '.' + numISP);
                    }
                }
            }, cid);
        }

        @Override
        public void onLinkMessage(String messageId, String messageBody, boolean isEnd) {
            engineWrapper.messageSender.onJNILinkMsg(messageId, messageBody, isEnd);
        }

        @Override
        public void onJNIReportEvent(String message) {
            engineWrapper.messageSender.offerStructureEvent(message);
        }

        @Override
        public void onLuaError(String content) {
            engineWrapper.messageSender.offerEvent("lua_error", content);
        }

        @Override
        public void openQosAccel(int cid, String node, String accessToken, String sourIp, int sourPort, String destIp, int destPort, String protocol, int timeSeconds) {
            QosManager.Key qosKey = new QosManager.Key(cid, node, accessToken);
            QosManager.EndPoint2EndPoint e2e = new QosManager.EndPoint2EndPoint(
                sourIp, sourPort,    // 源IP和端口
                destIp, destPort,    // 目标IP和端口
                "TCP".equalsIgnoreCase(protocol) ? Protocol.TCP : Protocol.UDP
            );
            QosHelper.Opener qosOpener = new QosHelper.Opener(
                qosKey,
                engineWrapper.netManager,
                e2e,
                timeSeconds,
                new QosTools(engineWrapper.gameGuidOrChannel,
                    engineWrapper.clientVersion,
                    engineWrapper.netManager, engineWrapper.imsi),
                new QosHelper.Callback() {
                    @Override
                    public void onQosResult(QosManager.Action action, QosManager.CallbackParam param) {
                        jniWrapper.openQosAccelResult(param.cid, param.sessionId, param.speedId, param.error);
                        Message_EventMsg.Event event = param.event;
                        if (event != null) {
                            engineWrapper.messageSender.offerEvent(event);
                        }
                    }
                }
            );
            MainHandler.getInstance().post(qosOpener);
        }

        @Override
        public void closeQosAccel(int cid, String sessionId, String node, String accessToken) {
            QosManager.Key qosKey = new QosManager.Key(cid, node, accessToken);
            QosHelper.Closer closer = new QosHelper.Closer(qosKey, sessionId, new QosHelper.Callback() {
                @Override
                public void onQosResult(QosManager.Action action, QosManager.CallbackParam param) {
                    jniWrapper.closeQosAccelResult(param.cid, param.error);
                }
            });
            MainHandler.getInstance().post(closer);
        }

        @Override
        public void modifyQosAccel(int cid, String sessionId, String node, String accessToken, int timeSeconds) {
            QosManager.Key qosKey = new QosManager.Key(cid, node, accessToken);
            QosHelper.Modifier modifier = new QosHelper.Modifier(
                qosKey,
                sessionId,
                new QosHelper.Callback() {
                    @Override
                    public void onQosResult(QosManager.Action action, QosManager.CallbackParam param) {
                        jniWrapper.modifyQosAccelResult(param.cid, param.timeLength, param.error);
                    }
                },
                timeSeconds
            );
            MainHandler.getInstance().post(modifier);
        }

        @Override
        public void onQosMessage(String message) {
            engineWrapper.messageSender.onJNIQosMsg(message);
        }

        @Override
        public void onAccelInfoUpload(String content, String userId, String jwtToken) {
            if (Logger.isLoggableDebug(LogTag.DATA)) {
                Log.d(LogTag.DATA, "Accel-Info: " + content);
            }
            UserAccelInfoUploader.start(
                hrTransArguments,
                new HRDataTrans.UserInfo(userId, jwtToken),
                content.getBytes());
        }

        @Override
        public void requestSaveData(String name, String value) {
            try {
                engineWrapper.persistentData.save(name, TextUtils.isEmpty(value) ? null : value.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void requestLoadData(int cid, String name) {
            try {
                byte[] data = engineWrapper.persistentData.load(name);
                jniWrapper.requestLoadDataResult(cid, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void requestBeaconCounter(final int cid, String counterName) {
            BeaconCounter.start(
                hrTransArguments.clientType,
                hrTransArguments.serviceLocation,
                counterName,
                new BeaconCounter.Callback() {
                    @Override
                    public void onCounter(boolean succeed) {
                        jniWrapper.setInt(cid, Defines.VPNJniStrKey.KEY_BEACON_COUNTER_RESULT, succeed ? 1 : 0);
                    }
                }
            );
        }

        @Override
        public int protectFD(int socket) {
            ServiceConnectionImpl serviceConnection = engineWrapper.serviceConnection;
            if (serviceConnection == null) {
                return ErrorCode.VPN_SERVICE_NOT_EXISTS;
            }
            return serviceConnection.protectSocket(socket);
        }
    }

    /**
     * 负责执行初始化操作的类
     * <i>初始化相关的代码放在一个类里易于维护和阅读</i>
     */
    private static class InitExecutor {

        /**
         * 初始化预检查
         *
         * @return 0表示条件满足，可以执行初始化，其它值表示不能继续执行初始化
         */
        static int beforeInitPreCheck(EngineWrapper engineWrapper) {
            if (engineWrapper.proxyLooper != null) {
                return INIT_RESULT_ALREADY;
            }
            if (!ThreadUtils.isInAndroidUIThread()) {
                Logger.e(TAG, "init() must be called in android UI thread");
                return INIT_RESULT_NOT_IN_MAIN_THREAD;
            }
            if (engineWrapper.serviceConfig.isInitAlwaysFail()) {
                return INIT_RESULT_FAILURE;
            }
            return INIT_RESULT_SUCCEED;
        }

        /**
         * 发起Portal脚本下载的请求，并取上一次缓存的本地Portal脚本
         *
         * @return 本地缓存
         */
        static byte[] loadLocalScriptAndStartDownload(PortalDataDownloader.Arguments portalArguments) {
            PortalDataEx validPortalData = PortalScriptDownloader.start(portalArguments);
            byte[] luaPCode = (validPortalData != null) ? validPortalData.getData() : null;
            if (Logger.isLoggableDebug(LogTag.DATA)) {
                Log.d(LogTag.DATA, "PCode: " + StringUtils.bytesToString(luaPCode));
            }
            return luaPCode;
        }

        /**
         * 发起Portal加速节点下载的请求，并取上一次缓存在本地的加速节点数据
         *
         * @return 本地缓存
         */
        static AccelNodesDownloader.NodesInfo loadLocalAccelNodesAndStartDownload(
            PortalDataDownloader.Arguments portalArguments,
            AccelNodesDownloader.NodesInfo debugNodesInfo
        ) {
            AccelNodesDownloader.NodesInfo accelNodesInfo = AccelNodesDownloader.start(portalArguments);
            if (debugNodesInfo != null) {
                Log.w(LogTag.DATA, "Use Debug Nodes: " + debugNodesInfo);
                accelNodesInfo = debugNodesInfo;
            }
            return accelNodesInfo;
        }

        /**
         * 以合适的方式启动JNI的工作循环
         *
         * @param mode 引擎以哪种方式工作？不同的方式，JNI的工作循环动作方式有所不同
         */
        static void startJNIProxyLoop(InitJNIMode mode) {
            if (mode == InitJNIMode.VPN) {
                new Thread("JNI-ProxyLoop") {
                    @Override
                    public void run() {
                        VPNJni.proxyLoop(0, false);
                    }
                }.start();
            } else {
                VPNJni.proxyLoop(0, true);
            }
        }

        /**
         * 处理加速游戏列表的下载、对比（与本机安装的应用列表取交集）等
         * FIXME: 17-3-30 需要考虑“监控用户卸载和安装新游戏”行为？？？
         */
        static SupportGameList processAccelGameList(
            Context context,
            JniWrapper jniWrapper,
            PortalDataDownloader.Arguments portalArguments,
            byte[] jsonOfDefaultAccelGameList
        ) {
            // 加载本地缓存的游戏列表，并启动下载
            // FIXME: 2017/4/1 Capacity 设置为多少合适 ？（APP有超过15000款，ROM可能只有几十款）
            List<AccelGame> accelGameList = AccelGamesDownloader.start(
                portalArguments, 16000,
                new AccelGamesDownloader.Listener() {
                    @Override
                    public void onAccelGameListDownload(List<AccelGame> list) {
                        AccelGameListManager.getInstance().setAccelGameList(list);
                    }
                },
                jsonOfDefaultAccelGameList);

            AccelGameListManager.getInstance().setAccelGameList(accelGameList);

            SupportGameList supportGameList = SupportGameList.build(accelGameList, InstalledApp.getInstalledAppList(context));
            if (supportGameList != null) {
                for (SupportGame supportGame : supportGameList) {
                    jniWrapper.addSupportGame(supportGame.uid, supportGame.packageName, supportGame.appLabel, supportGame.protocol);
                }
            }
            return supportGameList;
        }

        /**
         * 加载本地测试人员编写的脚本
         *
         * @param jniWrapper {@link JniWrapper} 对象实例
         */
        static void processLocalDebugScripts(JniWrapper jniWrapper) {
            byte[] script = null;
            try {
                script = LocalScripts.load(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (script != null) {
                jniWrapper.setString(0, Defines.VPNJniStrKey.KEY_INJECT, script);
            }
        }

        /**
         * 如果需要，设置初始的ECHO端口。
         *
         * @param jniWrapper {@link JniWrapper} 对象实例
         * @param echoPort   ECHO端口。如果为0，则用{{@link #DEFAULT_UDP_ECHO_PORT}}代替，
         *                   如果为正数由直接使用。负数被忽略
         */
        static void setEchoPort(JniWrapper jniWrapper, int echoPort) {
            if (echoPort >= 0) {
                jniWrapper.setUDPEchoPort(echoPort == 0 ? DEFAULT_UDP_ECHO_PORT : echoPort);
            }
        }

        /**
         * 如果是SDK模式，进行HOOK等操作
         *
         * @param context    {@link Context}
         * @param jniWrapper {@link JniWrapper}
         * @param protocol   游戏所用的协议
         * @param hookModule 要HOOK的库
         */
        static void processGameSDK(Context context, JniWrapper jniWrapper, Protocol protocol, String hookModule) {
            jniWrapper.setString(0, Defines.VPNJniStrKey.KEY_HOOK_MODULE, hookModule);
            // 仅当是SDK的时候，将自身加入到支持的游戏列表里（支持的游戏只有自身一个）
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            jniWrapper.addSupportGame(
                applicationInfo.uid,
                context.getPackageName(),
                InfoUtils.loadAppLabel(context, applicationInfo),
                protocol);
        }

        /**
         * 发起Portal数据下载的请求
         */
        static void processPortalDownload(JniWrapper jniWrapper, PortalDataDownloader.Arguments portalArguments) {
            // General 参数处理和下载
            PortalGeneralConfigDownloader.start(portalArguments, jniWrapper);
            // QosRegion 配置下载
            QosRegionConfig.start(portalArguments);
            // Misc
            PortalMiscConfigDownloader.start(portalArguments);
        }

        /**
         * 创建一个{@link ProxyLooper}对象实例并启动它
         *
         * @param jniWrapper {@link JniWrapper}
         * @return 创建并已启动的 {@link ProxyLooper}
         */
        static ProxyLooper createAndStartProxyLooper(JniWrapper jniWrapper) {
            ProxyLooper proxyLooper = new ProxyLooper(jniWrapper);
            proxyLooper.start();
            return proxyLooper;
        }

    }
}
