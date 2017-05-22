package com.subao.vpn;

import android.text.TextUtils;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.net.IPv4;
import com.subao.common.thread.MainHandler;
import com.subao.common.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * VPNJni
 * <p>Created by YinHaiBo on 2017/1/5.</p>
 */
@SuppressWarnings("JniMissingFunction")
public class VPNJni {

    /**
     * 初始化模式：UDP游戏
     *
     * @see #init(int, int, int, byte[], byte[], byte[])
     */
    public static final int INIT_MODE_UDP = 0;
    /**
     * 初始化模式：TCP游戏
     *
     * @see #init(int, int, int, byte[], byte[], byte[])
     */
    public static final int INIT_MODE_TCP = 1;
    /**
     * 初始化模式：VPN（迅游手游加速器APP或手机ROM）
     *
     * @see #init(int, int, int, byte[], byte[], byte[])
     */
    public static final int INIT_MODE_VPN = 2;


    private static final String TAG = LogTag.PROXY;
    /**
     * JNI对Java的所有调用，均委托给{@link JniCallback}处理
     */
    private static JniCallback callback;

    private static List<VpnEventObserver> observerList = new ArrayList<VpnEventObserver>(4);

    private static boolean libraryLoaded;

    private VPNJni() {
    }

    /**
     * 初始化，加载SO库
     */
    public static void loadLibrary(JniCallback callback, String libName) {
        synchronized (VPNJni.class) {
            if (libraryLoaded) {
                return;
            }
            libraryLoaded = true;
        }
        VPNJni.callback = callback;
        System.loadLibrary(libName);
    }

    /**
     * 设置“JNI对Java的调用委托”
     *
     * @param callback {@link JniCallback}
     */
    public static JniCallback setCallback(JniCallback callback) {
        JniCallback old = VPNJni.callback;
        VPNJni.callback = callback;
        return old;
    }

    public static JniCallback getCallback() {
        return VPNJni.callback;
    }

    /**
     * 添加一个{@link VpnEventObserver}（观察者）
     */
    public static void registerVpnEventObserver(VpnEventObserver o) {
        synchronized (observerList) {
            if (!observerList.contains(o)) {
                observerList.add(o);
            }
        }
    }

    /**
     * 从VPNJni移除一个观察者 {@link VpnEventObserver}
     */
    public static void unregisterVpnEventObserver(VpnEventObserver o) {
        synchronized (observerList) {
            observerList.remove(o);
        }
    }

    /**
     * JNI函数：初始化代理层
     *
     * @param cid                 Call ID
     * @param net_state           当前的网络状态
     * @param mode                模式。参见常量定义{@link #INIT_MODE_UDP}, {@link #INIT_MODE_TCP}, {@link #INIT_MODE_VPN}
     * @param lua_pcode           Lua脚本，如果没有则传null
     * @param node_list           按约定格式传递的节点列表
     * @param convergenceNodeList 按约定格式传递的节点列表
     * @return 成功返回true，失败返回false
     * @see #INIT_MODE_UDP
     * @see #INIT_MODE_TCP
     * @see #INIT_MODE_VPN
     */
    public native static boolean init(
        int cid, int net_state, int mode, byte[] lua_pcode,
        byte[] node_list, byte[] convergenceNodeList);

    /**
     * JNI函数：开启加速
     *
     * @param cid Call ID
     * @return 成功返回tre，失败返回false
     */
    public native static boolean startProxy(int cid);

    /**
     * JNI函数：关闭加速
     *
     * @param cid Call ID
     */
    public native static void stopProxy(int cid);

    /**
     * 在专门的java线程调用，调用JNI的这个函数，分发回调
     */
    public native static void processEvent();

    /**
     * 设置String值
     *
     * @param cid   Call ID
     * @param key   Key
     * @param value Value
     */
    public native static void setString(int cid, byte[] key, byte[] value);

    /**
     * 设置Integer值
     *
     * @param cid   Call ID
     * @param key   Key
     * @param value Value
     */
    public native static void setInt(int cid, byte[] key, int value);

    /**
     * 设置UDP Echo端口
     *
     * @param cid  Call ID
     * @param port 端口
     */
    public native static void setUDPEchoPort(int cid, int port);

    /**
     * 游戏取迅游提供的Web页面的URL
     *
     * @param cid Call ID
     * @return URL
     */
    public native static String getWebUIUrl(int cid);

    public native static int getAccelerationStatus(int cid);

    public native static boolean getSDKUDPIsProxy(int cid);

    public native static String getVIPValidTime(int cid);

    //鉴权相关
    public native static void setUserToken(int cid, byte[] openid, byte[] token, byte[] appid);

    public native static void linkAuthResult(int cid, boolean succ, int code, int ip, byte[] token, int expire);

    public native static void userAuthResult(int cid, boolean succ, int code, byte[] jwt_token, int expire, byte[] sid, int state, byte[] expired_date);

    public native static void userStateResult(int cid, boolean succ, int code, int state, byte[] sid, byte[] expired_date);

    public native static void userConfigResult(int cid, boolean succ, int code, byte[] cfg);

    /**
     * 对{@link #requestMobileFD(int)}的异步应答
     *
     * @param cid       Call ID
     * @param fd        申请到的FD
     * @param errorCode 错误代码（0表示没有错误）
     * @param canRetry  为true表示“即使本次失败，但还可以重试，重试后有概率能成功”，为false表示“不用再重试了，重试也会失败”
     */
    public native static void requestMobileFDResult(int cid, int fd, int errorCode, boolean canRetry);

    /**
     * 游戏每隔N秒传来延迟数据
     *
     * @param cid   Call ID，填0
     * @param delay 延迟毫秒数
     */
    public native static void onUDPDelay(int cid, int delay);

    /**
     * 游戏询问“是否推荐开启加速”
     *
     * @param cid 填0
     * @return 推荐结果
     */
    public native static int getAccelRecommendation(int cid);

    /**
     * 游戏设置“要对哪个IP和端口进行推荐”
     *
     * @param cid       填0
     * @param game_ip   IP地址
     * @param game_port 端口
     */
    public native static void setRecommendationGameIP(int cid, int game_ip, int game_port);

    /**
     * TCP游戏设置：游戏服务器的IP
     */
    public native static void setSDKGameServerIP(int cid, byte[] ip);

    /**
     * Java通知JNI：上一次JNI试图开启Qos加速通道的结果
     *
     * @param cid       当C调用{@link #openQosAccel(int, String, String, String, int, String, int, String, int)}时传递的Call ID
     * @param sessionId 与服务服进行通讯所用的会话Id
     * @param speedId   第三方厂商返回的SpeedId
     * @param error     错误码
     * @see #openQosAccel(int, String, String, String, int, String, int, String, int)
     */
    public static native void openQosAccelResult(int cid, byte[] sessionId, byte[] speedId, int error);

    /**
     * Java通知JNI：上一次JNI试图关闭Qos加速的结果
     *
     * @param cid   当C调用{@link #closeQosAccel(int, String, String, String)} 时传递的Call ID
     * @param error 0表示成功，其它为错误代码
     */
    public static native void closeQosAccelResult(int cid, int error);

    /**
     * Java通知JNI：上一次JNI试图续订Qos的结果
     *
     * @param cid         当C调用 {@link #modifyQosAccel(int, String, String, String, int)} 时传递的CallID
     * @param timeSeconds 时长，单位秒
     * @param error       0表示成功，其它为错误代码
     */
    public static native void modifyQosAccelResult(int cid, int timeSeconds, int error);

    /**
     * Java通知JNI：修改lua定义的一些常量，通常用于Portal配置
     *
     * @param cid   Call ID
     * @param type  健（lua变量名）
     * @param value 值
     */
    public static native void defineConst(int cid, byte[] type, byte[] value);

    /**
     * 注入Lua PCode
     */
    public static native void injectPCode(int cid, byte[] pcode);

    /**
     * 向JNI报告：“JNI请求加载本地缓存数据”的加载结果
     *
     * @param cid  Call ID
     * @param data 数据
     * @see JniCallback#requestSaveData(String, String)
     * @see JniCallback#requestLoadData(int, String)
     */
    public static native void onLoadDataResult(int cid, byte[] data);

    /**
     * 当{@link #getAccelRecommendation(int)}返回如下返回“加速建议”扩展数据
     *
     * @param type 类型
     * @return 相关方案或URL
     */
    public static native String getAccelRecommendationData(int cid, int type);

    /**
     * 提醒反馈
     * <p>当{@link #getAccelRecommendation(int)}返回值引发弹出对话框后，用户点击确认或取消都通知SDK</p>
     *
     * @param isConfirm true表示用户点击了确认，false表示点击了取消
     */
    public static native void onAccelRecommendationResult(int cid, int type, boolean isConfirm);

    private static native boolean startVPN(int cid, int vpn_fd);

    /**
     * 开启JNI的VPN代理，如果成功，将触发{@link VpnEventObserver}的{@link VpnEventObserver#onVPNStateChanged(boolean)} 事件
     *
     * @param vpnFD VPN接口的FD
     * @return true表示开启成功，false表示开启失败。仅当开启成功时才会
     */
    public static boolean doStartVPN(int vpnFD) {
        boolean result = startVPN(0, vpnFD);
        if (result) {
            ObserverNotifier.notify(true);
        }
        return result;
    }

    private static native void stopVPN(int cid);

    public static void doStopVPN() {
        stopVPN(0);
        ObserverNotifier.notify(false);
    }

    public static native void startNodeDetect(int cid, int uid);

    public static native boolean isNodeDetected(int cid, int uid);

    public static native void proxyLoop(int cid, boolean new_thread);

    /**
     * JNI开关加速的时候通知Java
     *
     * @param cid  Call id
     * @param open true表示开加速，false表示关加速
     */
    @SuppressWarnings("unused")
    public static void onProxyActive(int cid, boolean open) {
        callback.onProxyActive(open);
    }

    // =======================================================================================
    // ===========================[ Methods called by JNI ]===================================
    // =======================================================================================

    /**
     * JNI调用Java：发起鉴权请求
     *
     * @param cid    Call ID
     * @param userId 用户ID
     * @param token  鉴权Token
     * @param appId  APP-ID
     */
    public static void userAuth(int cid, String userId, String token, String appId) {
        callback.requestUserAuth(cid, userId, token, appId);
    }

    /**
     * JNI调用Java：发起节点鉴权请求
     *
     * @param cid      Call ID
     * @param node     网络序的IP
     * @param jwtToken JWT Token
     */
    public static void linkAuth(int cid, int node, String jwtToken) {
        String nodeIP = IPv4.ipToString(IPv4.ntohl(node));
        callback.requestLinkAuth(cid, nodeIP, jwtToken);
    }

    /**
     * JNI调用Java：请求User的配置
     *
     * @param cid      Call ID
     * @param userId   用户ID
     * @param jwtToken JWT Token
     */
    public static void userConfig(int cid, String userId, String jwtToken) {
        callback.requestUserConfig(cid, userId, jwtToken);
    }

    /**
     * JNI调用Java：请求用户当前身份状态
     *
     * @param cid      Call ID
     * @param userId   用户ID
     * @param jwtToken JWTToken
     */
    public static void userState(int cid, String userId, String jwtToken) {
        callback.requestUserState(cid, userId, jwtToken);
    }

    /**
     * 当Lua发生错误的时候被调用
     *
     * @param cid     Call ID
     * @param content 错误信息
     */
    @SuppressWarnings("unused")
    public static void onLuaError(int cid, String content) {
        callback.onLuaError(content);
    }

    /**
     * JNI向Java层申请基于移动网络的FD
     *
     * @param cid Call ID
     * @see #requestMobileFDResult(int, int, int, boolean)
     */
    public static void requestMobileFD(int cid) {
        callback.requestMobileFD(cid);
    }

    /**
     * JNI产生了一条已序列化好的Link消息，通知Java
     *
     * @param cid     Call ID
     * @param msgId   用来区分不同消息的标识
     * @param msgBody 已序列化好的Link消息
     * @param finish  为true表示此条Link已结束，不会再有新的数据再来了（可以上报了）
     */
    public static void onLinkMessage(int cid, String msgId, String msgBody, boolean finish) {
        callback.onLinkMessage(msgId, msgBody, finish);
    }

    /**
     * 被C层调用：开启Qos提速通道
     *
     * @param cid         call id
     * @param node        向哪个节点发Qos请求？
     * @param accessToken 访问Token
     * @param sourIp      源IP。如果为null或empty，则上层自动检测
     * @param sourPort    源端口，本机序
     * @param destIp      目标IP
     * @param destPort    目标端口
     * @param protocol    协议类型。"UDP" or "TCP"
     * @param timeSeconds 加速时长，单位秒
     */
    public static void openQosAccel(
        int cid, String node, String accessToken,
        String sourIp, int sourPort,
        String destIp, int destPort,
        String protocol,
        int timeSeconds
    ) {
        callback.openQosAccel(cid, node, accessToken, sourIp, sourPort, destIp, destPort, protocol, timeSeconds);
    }

    /**
     * 被C层调用：关闭Qos提速通道
     *
     * @param cid         Call ID
     * @param sessionId   {@link #openQosAccelResult(int, byte[], byte[], int)}传递的sessionId
     * @param node        向哪个节点发Qos请求？
     * @param accessToken 访问Token
     * @see #openQosAccel(int, String, String, String, int, String, int, String, int)
     */
    public static void closeQosAccel(int cid, String sessionId, String node, String accessToken) {
        callback.closeQosAccel(cid, sessionId, node, accessToken);
    }

    /**
     * 被C层调用：续订Qos
     *
     * @param cid         Call id
     * @param sessionId   {@link #openQosAccelResult(int, byte[], byte[], int)}传递的sessionId
     * @param node        向哪个节点发Qos请求？
     * @param accessToken 访问Token
     * @param timeSeconds 时长
     */
    public static void modifyQosAccel(int cid, String sessionId, String node, String accessToken, int timeSeconds) {
        callback.modifyQosAccel(cid, sessionId, node, accessToken, timeSeconds);
    }

    /**
     * 被C层调用：Qos Message
     *
     * @param cid     Call Id
     * @param message 消息体
     */
    public static void onQosMessage(int cid, String message) {
        callback.onQosMessage(message);
    }

    /**
     * JNI通知Java：需要取ISP信息
     *
     * @param cid Call ID
     */
    public static void getISP(int cid) {
        callback.requestISPInformation(cid);
    }

    /**
     * JNI通知Java，需要上传一个Event
     *
     * @param cid Call Id
     * @param msg Event
     */
    @SuppressWarnings("unused")
    public static void onReportEvent(int cid, String msg) {
        callback.onJNIReportEvent(msg);
    }

    /**
     * JNI通知Java，需要上传一个用户的加速信息
     *
     * @param cid      Call Id
     * @param content  要上传的内容
     * @param userId   UserId
     * @param jwtToken Token
     */
    public static void onAccelInfoUpload(int cid, String content, String userId, String jwtToken) {
        if (TextUtils.isEmpty(userId)) {
            Log.w(TAG, "onAccelInfoUpload, userId is empty");
            return;
        }
        if (TextUtils.isEmpty(content)) {
            Log.w(TAG, "onAccelInfoUpload, content is empty");
            return;
        }
        callback.onAccelInfoUpload(content, userId, jwtToken);
    }

    /**
     * JNI请求Java：在本地磁盘缓存一段数据
     *
     * @param cid   Call ID
     * @param name  数据的名字，保证是可作为文件名的
     * @param value 数据的内容
     * @see #onLoadData(int, String)
     */
    public static void onCacheData(int cid, String name, String value) {
        callback.requestSaveData(name, value);
    }

    /**
     * JNI请求Java：从本地磁盘加载先前保存的数据
     *
     * @param cid  Call ID
     * @param name 数据的名字
     * @see #onCacheData(int, String, String)
     */
    public static void onLoadData(int cid, String name) {
        callback.requestLoadData(cid, name);
    }

    /**
     * JNI请求Java：向服务器发起一个计数器请求
     *
     * @param cid         Call ID
     * @param counterName 计数器名称（类型）
     * @see com.subao.common.data.Defines.VPNJniStrKey#KEY_BEACON_COUNTER_RESULT
     */
    public static void requestBeaconCounter(int cid, String counterName) {
        callback.requestBeaconCounter(cid, counterName);
    }

    /**
     * JNI请求Java：VpnService保护指定的FD
     *
     * @param socket 给定的FD
     * @return 0表示成功，其它值表示错误代码
     */
    public static int protectFD(int socket) {
        return callback.protectFD(socket);
    }

    /**
     * 从{@link #observerList}安全地clone出一个{@link VpnEventObserver}的数组
     */
    private static VpnEventObserver[] cloneObservers() {
        VpnEventObserver[] observers = null;
        synchronized (observerList) {
            int count = observerList.size();
            if (count > 0) {
                observers = new VpnEventObserver[count];
                observers = observerList.toArray(observers);
            }
        }
        return observers;
    }


    // ===================================================

    /**
     * 负责通知{@link VpnEventObserver}
     */
    static class ObserverNotifier implements Runnable {

        private final VpnEventObserver[] observers;
        private final boolean active;

        ObserverNotifier(VpnEventObserver[] observers, boolean active) {
            this.observers = observers;
            this.active = active;
        }

        public static void notify(boolean active) {
            VpnEventObserver[] observers = cloneObservers();
            if (observers == null) {
                return;
            }
            if (ThreadUtils.isInAndroidUIThread()) {
                for (VpnEventObserver o : observers) {
                    o.onVPNStateChanged(active);
                }
            } else {
                MainHandler.getInstance().post(new ObserverNotifier(observers, active));
            }
        }

        @Override
        public void run() {
            for (VpnEventObserver o : observers) {
                o.onVPNStateChanged(active);
            }
        }
    }
}
