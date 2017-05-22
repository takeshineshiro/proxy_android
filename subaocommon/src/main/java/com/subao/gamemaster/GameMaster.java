package com.subao.gamemaster;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ConditionVariable;
import android.text.TextUtils;
import android.util.JsonWriter;
import android.util.Log;
import android.util.Pair;

import com.subao.common.ErrorCode;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.accel.AccelEngineInstance;
import com.subao.common.accel.EngineWrapper;
import com.subao.common.data.Address;
import com.subao.common.data.BeaconCounter;
import com.subao.common.data.Defines;
import com.subao.common.data.HRDataTrans;
import com.subao.common.data.LocalScripts;
import com.subao.common.data.ServiceConfig;
import com.subao.common.data.ServiceLocation;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.data.SupportGameList;
import com.subao.common.io.FileOperator;
import com.subao.common.jni.InitJNIMode;
import com.subao.common.jni.JniWrapper;
import com.subao.common.msg.MessageUserId;
import com.subao.common.msg.Message_DeviceInfo;
import com.subao.common.net.IPInfoQuery;
import com.subao.common.net.NetManager;
import com.subao.common.net.SignalWatcher;
import com.subao.common.net.SignalWatcherForCellular;
import com.subao.common.parallel.NetworkWatcher;
import com.subao.common.utils.AppLauncher;
import com.subao.common.utils.ThreadUtils;
import com.subao.vpn.VpnEventObserver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 迅游手游加速器SDK Main Class
 */
public class GameMaster {

    // ====[ SDK的版本信息 ]====

    /**
     * SDK的Version Code
     */
    public static final int VERSION_CODE = BuildConfig.VERSION_CODE;
    /**
     * SDK的版本号
     */
    public static final String VERSION_NAME = BuildConfig.VERSION_NAME;
    /**
     * SDK的Build Number
     */
    public static final String BUILD_NUM = BuildConfig.BUILD_NUM;
    /**
     * SDK的Build时间
     */
    public static final String BUILD_TIME = BuildConfig.BUILD_TIME;

    // TODO: 在Gradle里自动填充
    public static final String COMMIT_ID = "ad1e2968a0049dc3f26fd19f786baf2e6c3e222d";

    // ====[ GameMaster.init() 的返回值定义 ]====

    public static final int GM_INIT_ILLEGAL_ARGUMENT = EngineWrapper.INIT_ILLEGAL_ARGUMENT;
    public static final int GM_INIT_NOT_IN_MAIN_THREAD = EngineWrapper.INIT_RESULT_NOT_IN_MAIN_THREAD;
    public static final int GM_INIT_NO_PERMISSION = EngineWrapper.INIT_RESULT_NO_PERMISSION;
    public static final int GM_INIT_FAILURE = EngineWrapper.INIT_RESULT_FAILURE;
    public static final int GM_INIT_SUCCESS = EngineWrapper.INIT_RESULT_SUCCEED;
    public static final int GM_INIT_ALREADY = EngineWrapper.INIT_RESULT_ALREADY;


    // ====[ GameMaster.init() 的HookType定义 ]====

    public static final int HOOK_TYPE_CONNECT = 0;
    public static final int HOOK_TYPE_SENDTO_RECVFROM = 1;
    public static final int HOOK_TYPE_SENDMSG_RECVMSG = 2;

    // ====[ 网络连接类型 ]====

    public static final int NETWORK_CLASS_DISCONNECT = -1;
    public static final int NETWORK_CLASS_UNKNOWN = 0;
    public static final int NETWORK_CLASS_WIFI = 1;
    public static final int NETWORK_CLASS_2G = 2;
    public static final int NETWORK_CLASS_3G = 3;
    public static final int NETWORK_CLASS_4G = 4;

    @Deprecated
    public static final int SDK_MODE_GRAY = 1;
    @Deprecated
    public static final int SDK_MODE_OFFICIAL = 2;
    @Deprecated
    public static final int SDK_MODE_FREE = 3;

    // ====[ 用户状态 (GameMaster.getAccelerationStatus()的返回值) ]====

    public static final int SDK_NOT_QUALIFIED = 0;
    public static final int SDK_QUALIFIED = 1;
    public static final int SDK_FREE_TRIAL = 2;
    public static final int SDK_TRIAL_EXPIRED = 3;
    public static final int SDK_IN_USE = 4;
    public static final int SDK_EXPIRED = 5;
    public static final int SDK_FREE = 6;

    // ====[ GameMaster.getAccelRecommendation()的返回值 ]====

    /**
     * {@link #getAccelRecommendation()}的返回值
     * <p>
     * 因为尚未调用过GameMaster.init，所以无法推荐
     * </p>
     *
     * @see #getAccelRecommendation()
     */
    public static final int ACCEL_RECOMMENDATION_UNKNOWN = -1;
    /**
     * {@link #getAccelRecommendation()}的返回值
     * <p>
     * 勿需开启加速
     * </p>
     *
     * @see #getAccelRecommendation()
     */
    public static final int ACCEL_RECOMMENDATION_NONE = 0;
    /**
     * {@link #getAccelRecommendation()}的返回值
     * <p>
     * 需要开启加速
     * </p>
     *
     * @see #getAccelRecommendation()
     */
    public static final int ACCEL_RECOMMENDATION_NOTICE = 1;
    /**
     * {@link #getAccelRecommendation()}的返回值
     * <p>
     * 需要开启WiFi加速
     * </p>
     *
     * @see #getAccelRecommendation()
     */
    public static final int ACCEL_RECOMMENDATION_WIFI = 2;

    /**
     * {@link #getAccelRecommendation()}的返回值
     * <p>有新功能/改进，询问用户是否再将次试用</p>
     */
    public static final int ACCEL_RECOMMENDATION_HAS_NEW_FEATURE = 3;

    /**
     * {@link #getAccelRecommendation()}的返回值
     * <p>用户使用满一个月，邮件提示查看上月加速报告</p>
     */
    public static final int ACCEL_RECOMMENDATION_PROMPT_MONTH_REPORT = 4;

    /**
     * {@link #getAccelRecommendation()}的返回值
     * <p>用户VIP到期，邮件提示查看加速报告</p>
     */
    public static final int ACCEL_RECOMMENDATION_VIP_EXPIRED = 5;

    /**
     * 默认的UDP ECHO端口号
     */
    public static final int DEFAULT_UDP_ECHO_PORT = EngineWrapper.DEFAULT_UDP_ECHO_PORT;

    // ================[ 以下定义支付方式 ] ================

    /**
     * 支付宝支付
     */
    public static final int PAY_TYPE_ALIPAY = 0;
    /**
     * 微信支付
     */
    public static final int PAY_TYPE_WECHAT = 1;
    /**
     * QQ支付
     */
    public static final int PAY_TYPE_QQ = 2;
    /**
     * 银联支付
     */
    public static final int PAY_TYPE_UNIONPAY = 3;
    /**
     * 话费支付
     */
    public static final int PAY_TYPE_PHONE = 4;
    /**
     * 其它支付
     */
    public static final int PAY_TYPE_OTHER = 5;

    static final int PAY_TYPE_START = 0;
    static final int PAY_TYPE_END = 6;

    // =======================================================

    private final static String TAG = LogTag.GAME;

    /**
     * 所有公开接口委托给{@link EngineWrapper}实现
     */
    @SuppressLint("StaticFieldLeak")
    static EngineWrapper engineWrapper;

    static {
        Defines.moduleType = Defines.ModuleType.SDK;
    }

    private GameMaster() {
    }

    static InitJNIMode transHookTypeToInitJNIMode(int hookType) {
        switch (hookType) {
        case HOOK_TYPE_CONNECT:
            return InitJNIMode.TCP;
        case HOOK_TYPE_SENDMSG_RECVMSG:
        case HOOK_TYPE_SENDTO_RECVFROM:
            return InitJNIMode.UDP;
        default:
            return null;
        }
    }

    /**
     * 以“单游戏SDK的形式”初始化加速引擎
     * <p><b>本函数必须在安卓的主线程里被调用</b></p>
     * <p><i>本函数的原型在早期版本已经发布，不要更改</i></p>
     *
     * @param context    {@link Context}
     * @param hookType   游戏要加速的协议类型，参见{@link #HOOK_TYPE_CONNECT}、{@link #HOOK_TYPE_SENDMSG_RECVMSG}和{@link #HOOK_TYPE_SENDTO_RECVFROM}
     * @param gameGuid   迅游加速器分配给游戏的唯一GUID
     * @param channel    为与前一版本兼容而保留，其值被忽略
     * @param hookModule 要HOOK的SO
     * @param echoPort   ECHO端口号
     * @return 0或正数表示成功，负数表示失败。参见{@link #GM_INIT_SUCCESS}等
     * @see #GM_INIT_SUCCESS
     * @see #GM_INIT_ALREADY
     * @see #GM_INIT_FAILURE
     * @see #GM_INIT_NO_PERMISSION
     * @see #GM_INIT_NOT_IN_MAIN_THREAD
     */
    @SuppressWarnings("unused")
    public static int init(Context context, int hookType, String gameGuid, String channel, String hookModule, int echoPort) {
        InitJNIMode mode = transHookTypeToInitJNIMode(hookType);
        if (null == mode) {
            return GM_INIT_ILLEGAL_ARGUMENT;
        }
        return init(context, gameGuid, mode, hookModule, echoPort, null, null, new RequiredPermissionCheckerDefaultImpl());
    }

    /**
     * 2.0.2 新接口，以“VPN模式”初始化SDK
     *
     * @param context  {@link Context}
     * @param gameGuid 迅游加速器分配给手机ROM厂商，或迅游手游加速器APP的唯一GUID
     * @return
     */
    public static int initWithVPN(Context context, String gameGuid, byte[] jsonOfDefaultAccelGameList) {
        return init(context, gameGuid, InitJNIMode.VPN, null, 0, jsonOfDefaultAccelGameList, null, null);
    }

    static int init(
        Context context, String gameGuid,
        InitJNIMode mode, String hookModule,
        int udpEchoPort,
        byte[] jsonOfDefaultAccelGameList,
        EngineWrapper engineWrapper,
        RequiredPermissionChecker requiredPermissionChecker
    ) {
        Log.i(TAG, String.format(
            "GameMaster %s build %s (%s)\ncommit-id: %s",
            GameMaster.VERSION_NAME, GameMaster.BUILD_NUM, GameMaster.BUILD_TIME,
            GameMaster.COMMIT_ID
        ));
        if (TextUtils.isEmpty(gameGuid)) {
            Log.e(TAG, "Null game-guid, init failed");
            return GM_INIT_ILLEGAL_ARGUMENT;
        }
        // 必须要在UI主线程里被调用
        if (!ThreadUtils.isInAndroidUIThread()) {
            Log.e(TAG, "init() must be called in android UI thread");
            return GM_INIT_NOT_IN_MAIN_THREAD;
        }
        // 必须要有自定义权限
        if (requiredPermissionChecker != null) {
            if (!requiredPermissionChecker.hasRequiredPermission(context)) {
                Log.e(TAG, "You are not granted to use GameMaster SDK, please add related permission to your Manifest.xml!");
                return GM_INIT_NO_PERMISSION;
            }
        }
        //
        synchronized (GameMaster.class) {
            if (GameMaster.engineWrapper != null) {
                return GM_INIT_ALREADY;
            }
            if (engineWrapper == null) {
                engineWrapper = new EngineWrapper(context, Defines.ModuleType.SDK,
                    gameGuid, VERSION_NAME, new NetManager(context),
                    new JniWrapper("gamemaster"), true);
            }
            GameMaster.engineWrapper = engineWrapper;
        }
        //
        int result = engineWrapper.init(mode, hookModule, udpEchoPort, jsonOfDefaultAccelGameList);
        if (result == 0) {
            AccelEngineInstance.set(engineWrapper);
        } else {
            engineWrapper.dispose();
            GameMaster.engineWrapper = null;
        }
        return result;
    }

    /**
     * 建立VPN之前的准备工作
     * <p>
     * 此接口不用先调用初始化
     *
     * @return 如果不为null，应使用{@link android.app.Activity#startActivityForResult(Intent, int)}来调
     * 出系统的VPN授权框让用户选择
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Intent prepareVPN(Context context) {
        try {
            return VpnService.prepare(context);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 开启并连接VpnService
     */
    public static boolean openVPN() {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper != null) {
            return engineWrapper.openVPN();
        } else {
            return false;
        }
    }

    /**
     * 关闭VpnService，并结束加速
     */
    public static void closeVPN() {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper != null) {
            engineWrapper.closeVPN();
        }
    }

    /**
     * 开启加速
     *
     * @param unused 未使用，填0
     * @return 成功返回true，失败返回false
     */
    @SuppressWarnings("unused")
    public static boolean start(int unused) {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper == null) {
            return false;
        }
        return wrapper.startAccel();
    }

    /**
     * 关闭加速
     */
    public static void stop() {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            wrapper.stopAccel();
        }
    }

    /**
     * 加速引擎是否已初始化成功？
     *
     * @return True表示已成功初始化（但不表示已开启加速），False表示尚未成功初始化
     */
    public static boolean isEngineRunning() {
        return engineWrapper != null;
    }

    /**
     * 判断最近一次UDP通讯是否代理
     *
     * @return true表示代理，false表示透传
     */
    public static boolean isUDPProxy() {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            return wrapper.isUDPProxy();
        } else {
            return false;
        }
    }

    /**
     * 是否已开启加速？
     *
     * @return True表示已开启加速，False表示尚未开启加速
     * @see #start(int)
     * @see #stop()
     */
    public static boolean isAccelOpened() {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            return wrapper.isAccelOpened();
        } else {
            return false;
        }
    }

    /**
     * APP或ROM专用：针对某一款游戏，开始进行节点检测
     *
     * @param uid 游戏的UID
     */
    public static void startNodeDetect(int uid) {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            wrapper.startNodeDetect(uid);
        }
    }

    /**
     * APP或ROM专用：查询指定的游戏的节点检测工作是否已完成
     *
     * @param uid
     * @return
     */
    public static boolean isNodeDetected(int uid) {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            return wrapper.isNodeDetected(uid);
        } else {
            return false;
        }
    }

    /**
     * 鉴权
     *
     * @param userId 用户ID
     * @param token  用户Token
     * @param appId  AppId
     */
    public static void setUserToken(String userId, String token, String appId) {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            wrapper.setUserToken(userId, token, appId);
        }
    }

    /**
     * 返回当前用户的免流类型
     *
     * @see EngineWrapper.FreeFlowType
     */
    public static int getCurrentUserFreeFlowType() {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            return wrapper.getCurrentUserFreeFlowType();
        } else {
            return -1;
        }
    }

    /**
     * 王者荣耀特殊需求：设置当前用户是哪种免流用户
     *
     * @param type 类型，取值为0、1、2（参见{@link EngineWrapper.FreeFlowType}）或-1表示“非免流用户”
     */
    public static void setFreeFlowUser(int type) {
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, "Free flow user: " + type);
        }
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            wrapper.setFreeFlowUser(type);
        }
    }

    /**
     * 游戏定时通知SDK：当前游戏自己检测到的网络时延
     *
     * @param millis 网络时延，毫秒数
     */
    public static void onNetDelay(int millis) {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            wrapper.onNetDelay(millis);
        }
    }

    /**
     * GameMaster返回是否需要开启加速的建议结果
     *
     * @see GameMaster#onNetDelay(int)
     * @see #ACCEL_RECOMMENDATION_UNKNOWN
     * @see #ACCEL_RECOMMENDATION_NONE
     * @see #ACCEL_RECOMMENDATION_NOTICE
     * @see #ACCEL_RECOMMENDATION_WIFI
     * @see #ACCEL_RECOMMENDATION_HAS_NEW_FEATURE
     * @see #ACCEL_RECOMMENDATION_PROMPT_MONTH_REPORT
     * @see #ACCEL_RECOMMENDATION_VIP_EXPIRED
     */
    public static int getAccelRecommendation() {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            return wrapper.getAccelRecommendation();
        } else {
            return ACCEL_RECOMMENDATION_UNKNOWN;
        }
    }

    /**
     * 由游戏厂商调用，设置UDP Echo服务器的端口
     */
    public static void setUdpEchoPort(int port) {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            wrapper.setUdpEchoPort(port);
        }
    }

    /**
     * 返回当前的网络类型
     */
    public static int getCurrentConnectionType() {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            return wrapper.getCurrentConnectionType();
        } else {
            return GameMaster.NETWORK_CLASS_UNKNOWN;
        }
    }

    /**
     * 设置游戏服IP
     */
    public static void setGameServerIP(String ip) {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            wrapper.setGameServerIP(ip);
        }
    }

    @SuppressWarnings("unused")
    @Deprecated
    public static boolean isNodeDetectSucceed() {
        return true;
    }

    @SuppressWarnings("unused")
    @Deprecated
    public static String getString(int key) {
        return "";
    }

    @SuppressWarnings("unused")
    @Deprecated
    public static void setString(int key, String value) {
        // do nothing
    }

    @SuppressWarnings("unused")
    @Deprecated
    public static long getLong(int key) {
        return 0L;
    }

    @SuppressWarnings("unused")
    @Deprecated
    public static void setLong(int key, long value) {
        // do nothing
    }

    /**
     * 设置SDK的使用模式
     *
     * @param mode
     * @see #SDK_MODE_FREE
     * @see #SDK_MODE_GRAY
     * @see #SDK_MODE_OFFICIAL
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static void setSDKMode(int mode) {
        // deprecated, do nothing
    }

    /**
     * 游戏向SDK请求内嵌页面的网址
     */
    public static String getWebUIUrl() {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            return wrapper.getWebUIUrl();
        } else {
            return EngineWrapper.buildDefaultWebUIUrl("", "");
        }
    }

    /**
     * 游戏将本局（某个特定时间划分）的延迟值统计数据通知SDK
     *
     * @param average  平均值
     * @param variance 方差
     * @param lostRate 丢包率
     * @param exPkgNum 异常包个数（延迟值超过某个阈值的定义为“异常包”）
     * @see #onNetDelayQuality2(float, float, float, float, float)
     * @deprecated 用onNetDelayQualityV2()代替
     */
    @SuppressWarnings("unused")
    @Deprecated
    public static void onNetDelayQuality(float average, float variance, float lostRate, int exPkgNum) {
        // Deprecated, do nothing
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
    public static void onNetDelayQuality2(float average, float variance, float lostRate, float exRateOfNewPing, float exRateOfPing) {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            wrapper.onNetDelayQuality2(average, variance, lostRate, exRateOfNewPing, exRateOfPing);
        }
    }

    /**
     * 返回当前用户的到期时间字符串
     */
    public static String getVIPValidTime() {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            String result = wrapper.getVIPValidTime();
            return result == null ? "" : result;
        } else {
            return "";
        }
    }

    /**
     * 取用户状态（123456）
     */
    public static int getAccelerationStatus() {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            return wrapper.getAccelerationStatus();
        }
        return SDK_NOT_QUALIFIED;
    }

    public static String getUserConfig() {
        return MessageUserId.getCurrentUserConfig();
    }

    public static void gameForeground() {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper != null) {
            engineWrapper.gameForeground(true);
        }
    }

    public static void gameBackground() {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper != null) {
            engineWrapper.gameForeground(false);
        }
    }

    @Deprecated
    public static void clearUDPCache() {
        // do nothing
    }

    public static void setGameId(int id) {
        setGameId(Integer.toString(id));
    }

    public static void setGameId(String id) {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper != null) {
            engineWrapper.setGameId(id);
        }
    }

    public static void setRecommendationGameIP(String ip, int port) {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            wrapper.setRecommendationGameIP(ip, port);
        }
    }

    /**
     * 当{@link #getAccelRecommendation()}返回如下返回“加速建议”扩展数据
     *
     * @param type 类型
     * @return 相关方案或URL
     */
    public static String getAccelRecommendationData(int type) {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            return wrapper.getAccelRecommendationData(type);
        } else {
            return "";
        }
    }

    /**
     * 提醒反馈
     * <p>当{@link #getAccelRecommendation()}返回值引发弹出对话框后，用户点击确认或取消都通知SDK</p>
     *
     * @param isConfirm true表示用户点击了确认，false表示点击了取消
     */
    public static void onAccelRecommendationResult(int type, boolean isConfirm) {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            wrapper.onAccelRecommendationResult(type, isConfirm);
        }
    }

    /**
     * 游戏通知SDK：打开/关闭WiFi加速
     *
     * @param on true表示打开（使用），false表示关闭（不使用）
     */
    public static void setWiFiAccelSwitch(boolean on) {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            wrapper.setWiFiAccelSwitch(on);
        }
    }

    public static void enableWiFiAccelSwitch() {
        setWiFiAccelSwitch(true);
    }

    /**
     * 游戏向SDK设置“支付方式白名单”，即：只允许哪些支付方式出现
     *
     * @param whiteList 支付方式白名单字符串，每个字符表示特定的允许方式
     *                  例如：字符串 "234" 表示只允许微信的三种方式支付
     * @see #PAY_TYPE_ALIPAY
     * @see #PAY_TYPE_WECHAT
     * @see #PAY_TYPE_QQ
     * @see #PAY_TYPE_UNIONPAY
     * @see #PAY_TYPE_PHONE
     * @see #PAY_TYPE_OTHER
     */
    public static void setPayTypeWhiteList(String whiteList) {
        EngineWrapper wrapper = GameMaster.engineWrapper;
        if (wrapper != null) {
            wrapper.setPayTypeWhiteList(whiteList);
        }
    }

    /**
     * 以位掩码参数的形式提供“设置支付白名单”功能
     *
     * @param allowBits 位掩码，相应位置位的表示允许，置零的表示不允许
     *                  例如：
     *                  <ul>
     *                  <li>值0：都不允许</li>
     *                  <li>值1：只允许支付宝APP支付</li>
     *                  <li>值3：允许支付宝APP和微信H5支付</li>
     *                  <li>...</li>
     *                  </ul>
     * @see #setPayTypeWhiteList(String)
     * @see #PAY_TYPE_ALIPAY
     * @see #PAY_TYPE_WECHAT
     * @see #PAY_TYPE_QQ
     * @see #PAY_TYPE_UNIONPAY
     * @see #PAY_TYPE_PHONE
     * @see #PAY_TYPE_OTHER
     */
    public static void setPayTypeWhiteList(int allowBits) {
        if (allowBits == 0) {
            setPayTypeWhiteList(null);
            return;
        }
        StringBuilder sb = new StringBuilder(PAY_TYPE_END - PAY_TYPE_START);
        for (int i = PAY_TYPE_START; i < PAY_TYPE_END; ++i) {
            int mask = (1 << i);
            if ((allowBits & mask) != 0) {
                sb.append(i);
            }
        }
        setPayTypeWhiteList(sb.toString());
    }

    /**
     * 返回“本机安装且被支持的游戏”的包名列表
     *
     * @return 一个包名列表
     */
    public static List<String> getSupportGameList() {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper != null) {
            SupportGameList supportGameList = engineWrapper.getSupportGameList();
            if (supportGameList != null) {
                return supportGameList.getPackageNameList();
            }
        }
        return new ArrayList<String>();
    }

    /**
     * 启动指定的APP（游戏）
     *
     * @param packageName 包名
     * @return 成功返回true，失败返回false
     */
    public static boolean launcherGame(Context context, String packageName) {
        return AppLauncher.execute(context, packageName);
    }

    // ==============[ 调试接口 ]================================

    /**
     * 调试用的，不公开
     * <p>申请一个Mobile FD</p>
     */
    static Pair<Integer, Integer> x1() {
        int fd, error;
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper == null) {
            fd = -1;
            error = ErrorCode.NOT_INIT;
        } else {
            try {
                fd = engineWrapper.requestNewMobileFD();
                error = 0;
            } catch (NetworkWatcher.OperationException e) {
                fd = -1;
                error = e.getErrorCode();
            }
        }
        return new Pair<Integer, Integer>(fd, error);
    }

    /**
     * 测试用，不公开。设置用户配置
     */
    static void x2(String userId, String userConfig) {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper == null) {
            return;
        }
        engineWrapper.uploadUserConfig(userId, userConfig);
    }

    /**
     * 测试用，不公开。获取设备信息
     *
     * @return 设备信息的Json串
     */
    static String x3(Context context) {
        Message_DeviceInfo deviceInfo = new Message_DeviceInfo(context);
        StringWriter stringWriter = new StringWriter(2048);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            deviceInfo.serialize(jsonWriter);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            Misc.close(jsonWriter);
        }
        return stringWriter.toString();
    }

    /**
     * 调试程序专用，不公开。打开Proxy日志输出
     */
    static void x4() {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        if (engineWrapper != null) {
            engineWrapper.openProxyLog();
        }
    }

    /**
     * 调试用，不公开
     */
    static File x5(File dir, boolean isSDK) {
        return ServiceConfig.createFile(dir, isSDK);
    }

    /**
     * 调试用，不公开
     */
    static File x6(boolean isSDK) {
        return LocalScripts.getFile(isSDK);
    }

    /**
     * 设置“加速开关改变侦听器”
     *
     * @param listener {@link OnAccelSwitchListener}
     * @return 成功返回true，否则返回false（尚未初始化）
     */
    static boolean x7(OnAccelSwitchListener listener) {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            wrapper.setOnAccelSwitchListener(new EngineWrapperOnAccelSwitchListener(listener));
            return true;
        } else {
            return false;
        }
    }

    /**
     * 返回数据存储路径
     */
    static File x8(Context context) {
        FileOperator.init(context, true);
        return FileOperator.getDataDirectory();
    }

    static void x9(String s, final I1 i) {
        EngineWrapper wrapper = engineWrapper;
        if (wrapper != null) {
            HRDataTrans.Arguments hrArguments = wrapper.getHRArguments();
            BeaconCounter.start(hrArguments.clientType, hrArguments.serviceLocation,
                s, new BeaconCounter.Callback() {
                    @Override
                    public void onCounter(final boolean succeed) {
                        i.a(succeed);
                    }
                });
        } else {
            i.a(false);
        }
    }

    static Object x10(Context context, I2 i2) {
        SignalWatcherForCellular watcher = new SignalWatcherForCellular(new SignalWatcherListener(i2));
        watcher.start(context);
        return watcher;
    }

    static void x11(Object w) {
        ((SignalWatcher) w).shutdown();
    }

    /**
     * 调试用函数：给定IP地址，返回归属地信息
     * <p><b>注意：本函数数的调用者线程可能被阻塞几秒。</b></p>
     */
    static String x12(String s) {
        IPQueryCallback callback = new IPQueryCallback();
        IPInfoQuery.executeByVIPMode(s, callback, null, true,
            new ServiceLocation(null, Address.EndPoint.MESSAGE.host, Address.EndPoint.MESSAGE.port)
        );
        IPInfoQuery.Result result = callback.watiResult();
        return result != null ? result.toString() : "fail";
    }

    static void x13(VpnEventObserver vpnEventObserver) {
        AccelEngineInstance.registerVpnEventObserver(vpnEventObserver);
    }

    static void x14(VpnEventObserver vpnEventObserver) {
        AccelEngineInstance.unregisterVpnEventObserver(vpnEventObserver);
    }

    /**
     * 调试用的，不公开
     * <p>
     * 清理SubaoId和Config
     * </p>
     */
    static void xy(Context context) {
        SubaoIdManager.getInstance().setSubaoId(null);
        FileOperator.init(context, true);
        File dir = new File(FileOperator.getDataDirectoryAbsolutePath());
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        dir.delete();
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

    public interface I1 {
        void a(boolean s);
    }

    public interface I2 {
        void a(int p);
    }

    /**
     * 初始化时，对“游戏必须申请的Android权限”做判断
     */
    interface RequiredPermissionChecker {
        boolean hasRequiredPermission(Context context);
    }

    static class SignalWatcherListener implements SignalWatcher.Listener {

        private final I2 i2;

        SignalWatcherListener(I2 i2) {
            this.i2 = i2;
        }

        @Override
        public void onSignalChange(int strengthPercent) {
            i2.a(strengthPercent);
        }
    }

    private static class EngineWrapperOnAccelSwitchListener implements EngineWrapper.OnAccelSwitchListener {

        private final OnAccelSwitchListener gameMasterListener;

        public EngineWrapperOnAccelSwitchListener(OnAccelSwitchListener gameMasterListener) {
            this.gameMasterListener = gameMasterListener;
        }

        @Override
        public void onAccelSwitch(boolean accelOn) {
            this.gameMasterListener.onAccelSwitch(accelOn);
        }
    }

    /**
     * 检查“游戏是否申请了速宝要求的自定义权限”
     */
    static class RequiredPermissionCheckerDefaultImpl implements RequiredPermissionChecker {

        private static final String PERMISSION_KEYWORD = "com.subao.permission.USE_SDK";
        private static final int PERMISSION_KEYWORD_LENGTH = PERMISSION_KEYWORD.length();

        @Override
        public boolean hasRequiredPermission(Context context) {
            PackageManager pm = context.getPackageManager();
            if (pm == null) {
                return false;
            }
            String packageName = context.getPackageName();
            if (TextUtils.isEmpty(packageName)) {
                return false;
            }
            try {
                PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                if (pi == null) {
                    return false;
                }
                String[] permissions = pi.requestedPermissions;
                if (permissions == null) {
                    return false;
                }
                for (String s : permissions) {
                    if (s != null && s.length() >= PERMISSION_KEYWORD_LENGTH) {
                        if (s.startsWith(PERMISSION_KEYWORD)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                // 所有异常均返回false
            }
            return false;
        }
    }

    private static class IPQueryCallback extends ConditionVariable implements IPInfoQuery.Callback {

        private IPInfoQuery.Result result;

        @Override
        public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
            this.result = result;
            this.open();
        }

        public IPInfoQuery.Result watiResult() {
            this.block();
            return this.result;
        }

    }

}
