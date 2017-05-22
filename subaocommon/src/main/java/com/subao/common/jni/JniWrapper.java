package com.subao.common.jni;

import android.util.Log;

import com.subao.common.Disposable;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.ProxyEngineCommunicator;
import com.subao.common.data.Defines;
import com.subao.common.msg.Message_DeviceInfo;
import com.subao.common.msg.Message_VersionInfo;
import com.subao.common.net.IPv4;
import com.subao.common.net.Protocol;
import com.subao.common.utils.StringUtils;
import com.subao.vpn.JniCallback;
import com.subao.vpn.VPNJni;

/**
 * JniWrapper
 * <p>Created by YinHaiBo on 2017/1/6.</p>
 */

public class JniWrapper implements ProxyEngineCommunicator, Disposable {

    private static final String TAG = LogTag.DATA;
    private static final byte[] EMPTY_BYTES_ARRAY = new byte[0];

    public JniWrapper(String libName) {
        VPNJni.loadLibrary(new JniCallbackNull(), libName);
    }

    static byte[] strToBytes(String s) {
        if (s == null || s.length() == 0) {
            return EMPTY_BYTES_ARRAY;
        } else {
            return s.getBytes();
        }
    }

    static int transIpStringToInt(String ip) {
        byte[] ipBytes = IPv4.parseIp(ip);
        if (ipBytes == null) {
            return 0;
        } else {
            return IPv4.ntohl(IPv4.ipToInt(ipBytes));
        }
    }

    @Override
    public void dispose() {
        VPNJni.setCallback(new JniCallbackNull());
    }

    public JniCallback setJniCallback(JniCallback jniCallback) {
        return VPNJni.setCallback(jniCallback);
    }

    public JniCallback getJniCallback() {
        return VPNJni.getCallback();
    }

    /**
     * 初始化（调用JNI的init）
     *
     * @param netState            当前网络类型
     * @param mode                初始化模式
     * @param luaPCode            Lua脚本。如果没有则传null
     * @param nodeList            按约定格式传递的节点列表
     * @param convergenceNodeList 按约定格式传递的汇聚节点列表
     * @return true表示初始化成功，false表示失败
     */
    public boolean initJNI(
        int netState, InitJNIMode mode,
        byte[] luaPCode,
        String nodeList, String convergenceNodeList
    ) {
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, "Init with PCode: " + StringUtils.bytesToString(luaPCode));
        }
        boolean result = VPNJni.init(0, netState,
            mode.intValue,
            (luaPCode == null || luaPCode.length == 0) ? EMPTY_BYTES_ARRAY : luaPCode,
            strToBytes(nodeList),
            strToBytes(convergenceNodeList)
        );
        if (result) {
            if (Logger.isLoggableDebug(LogTag.PROXY)) {
                openProxyLog();
            }
        }
        return result;
    }

    /**
     * 开启Proxy层的日志
     */
    public final void openProxyLog() {
        injectScript("log_test = function(str) log_info(str) end");
    }

    /**
     * 加载指定的Lua脚本源码
     */
    public final void injectScript(byte[] script) {
        setString(0, Defines.VPNJniStrKey.KEY_INJECT, script);
    }

    /**
     * 加载指定的Lua脚本源码
     */
    public final void injectScript(String script) {
        setString(0, Defines.VPNJniStrKey.KEY_INJECT, script);
    }

    /**
     * 开启加速
     *
     * @return 成功返回true，失败返回false
     */
    public boolean startProxy() {
        return VPNJni.startProxy(0);
    }

    /**
     * 关闭加速
     */
    public void stopProxy() {
        VPNJni.stopProxy(0);
    }

    /**
     * 在一个线程中被轮询：让JNI处理事件
     */
    public void processEvent() {
        VPNJni.processEvent();
    }

    /**
     * 设置String值
     *
     * @param cid   Call ID
     * @param key   Key
     * @param value Value
     */
    @Override
    public void setString(int cid, String key, String value) {
        if (Logger.isLoggableDebug(TAG)) {
            String v = (value == null) ? "null" : String.format("\"%s\"", value);
            Log.d(TAG, String.format("setString(%d, \"%s\", %s)", cid, key, v));
        }
        VPNJni.setString(cid, key.getBytes(), strToBytes(value));
    }

    public void setString(int cid, String key, byte[] value) {
        if (Logger.isLoggableDebug(TAG)) {
            int len = (value == null) ? 0 : value.length;
            Log.d(TAG, String.format("setString(%d, \"%s\", %d bytes)", cid, key, len));
        }
        VPNJni.setString(cid, key.getBytes(), value == null ? EMPTY_BYTES_ARRAY : value);
    }

    /**
     * 设置Integer值
     *
     * @param cid   Call ID
     * @param key   Key
     * @param value Value
     */
    @Override
    public void setInt(int cid, String key, int value) {
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, String.format("setInt(%d, \"%s\", %d)", cid, key, value));
        }
        VPNJni.setInt(cid, key.getBytes(), value);
    }

    public void setUDPEchoPort(int port) {
        VPNJni.setUDPEchoPort(0, port);
    }

    public void setUserToken(String userId, String token, String appid) {
        VPNJni.setUserToken(0, strToBytes(userId), strToBytes(token), strToBytes(appid));
    }

    public void userAuthResult(int cid, boolean succeed, int code, String jwt_token, int expire, String sid, int state, String expired_date) {
        VPNJni.userAuthResult(cid, succeed, code,
            strToBytes(jwt_token),
            expire,
            strToBytes(sid),
            state,
            strToBytes(expired_date));
    }

    public void linkAuthResult(int cid, boolean succeed, int code, String ip, byte[] token, int expire) {
        VPNJni.linkAuthResult(cid, succeed, code, transIpStringToInt(ip), token, expire);
    }

    public void userStateResult(int cid, boolean succ, int code, int state, String sid, String expired_date) {
        VPNJni.userStateResult(cid, succ, code, state, strToBytes(sid), strToBytes(expired_date));
    }

    public void userConfigResult(int cid, boolean succ, int code, String cfg) {
        VPNJni.userConfigResult(cid, succ, code, strToBytes(cfg));
    }

    /**
     * 调用JNI：能JNI发起的 {@link JniCallback#requestMobileFD(int)} 的回应
     *
     * @param cid      Call ID
     * @param fd       FD
     * @param error    错误代码
     * @param canRetry 如果失败，重试是否有概率成功？
     */
    public void requestMobileFDResult(int cid, int fd, int error, boolean canRetry) {
        VPNJni.requestMobileFDResult(cid, fd, error, canRetry);
    }

    /**
     * 游戏通知GameMaster当前的网络时延
     *
     * @param millis 网络时延，毫秒数
     */
    public void onUDPDelay(int millis) {
        VPNJni.onUDPDelay(0, millis);
    }

    public int getAccelRecommendation() {
        return VPNJni.getAccelRecommendation(0);
    }

    public void setRecommendationGameIP(String ip, int port) {
        VPNJni.setRecommendationGameIP(0, transIpStringToInt(ip), port);
    }

    public void setSDKGameServerIP(String ip) {
        VPNJni.setSDKGameServerIP(0, strToBytes(ip));
    }


    public String getWebUIUrl() {
        return VPNJni.getWebUIUrl(0);
    }

    public int getAccelerationStatus() {
        return VPNJni.getAccelerationStatus(0);
    }

    public boolean getSDKUDPIsProxy() {
        return VPNJni.getSDKUDPIsProxy(0);
    }

    public String getVIPValidTime() {
        return VPNJni.getVIPValidTime(0);
    }

    public void openQosAccelResult(int cid, String sessionId, String speedId, int error) {
        VPNJni.openQosAccelResult(cid, strToBytes(sessionId), strToBytes(speedId), error);
    }

    public void closeQosAccelResult(int cid, int error) {
        VPNJni.closeQosAccelResult(cid, error);
    }

    public void modifyQosAccelResult(int cid, int timeLength, int error) {
        VPNJni.modifyQosAccelResult(cid, timeLength, error);
    }

    public void defineConst(String key, String value) {
        VPNJni.defineConst(0, strToBytes(key), strToBytes(value));
    }

    /**
     * 注入Lua-PCode
     */
    public void injectPCode(byte[] pcode) {
        VPNJni.injectPCode(0, pcode);
    }

    /**
     * 设置一些消息上报所需的值给JNI
     */
    public void setMessageInformation(
        Message_VersionInfo version,
        Message_DeviceInfo deviceInfo
    ) {
        this.setString(0, Defines.VPNJniStrKey.KEY_VERSION, version.number);
        this.setString(0, Defines.VPNJniStrKey.KEY_CHANNEL, version.channel);
        this.setString(0, Defines.VPNJniStrKey.KEY_OS_VERSION, version.osVersion);
        this.setString(0, Defines.VPNJniStrKey.KEY_ANDROID_VERSION, version.androidVersion);
        //
        this.setString(0, Defines.VPNJniStrKey.KEY_PHONE_MODEL, deviceInfo.getModel());
        this.setString(0, Defines.VPNJniStrKey.KEY_ROM, deviceInfo.getROM());
        this.setInt(0, Defines.VPNJniStrKey.KEY_CPU_SPEED, deviceInfo.getCpuSpeed());
        this.setInt(0, Defines.VPNJniStrKey.KEY_CPU_CORE, deviceInfo.getCpuCore());
        this.setInt(0, Defines.VPNJniStrKey.KEY_MEMORY, deviceInfo.getMemory());
    }

    /**
     * 向JNI报告：“JNI请求加载本地缓存数据”的加载结果
     *
     * @param cid  Call ID
     * @param data 数据
     * @see JniCallback#requestSaveData(String, String)
     * @see JniCallback#requestLoadData(int, String)
     */
    public void requestLoadDataResult(int cid, byte[] data) {
        VPNJni.onLoadDataResult(cid, data);
    }

    /**
     * 当{@link #getAccelRecommendation()}返回如下返回“加速建议”扩展数据
     *
     * @param type 类型
     * @return 相关方案或URL
     */
    public String getAccelRecommendationData(int type) {
        return VPNJni.getAccelRecommendationData(0, type);
    }

    /**
     * 提醒反馈
     * <p>当{@link #getAccelRecommendation()}返回值引发弹出对话框后，用户点击确认或取消都通知SDK</p>
     *
     * @param isConfirm true表示用户点击了确认，false表示点击了取消
     */
    public void onAccelRecommendationResult(int type, boolean isConfirm) {
        VPNJni.onAccelRecommendationResult(0, type, isConfirm);
    }

    /**
     * APP或ROM专用：针对某一款游戏，开始检测节点
     *
     * @param uid 游戏的UID
     */
    public void startNodeDetect(int uid) {
        VPNJni.startNodeDetect(0, uid);
    }

    /**
     * APP或ROM专用：判断给定的UID，是否已完成节点检测
     *
     * @param uid 游戏的UID
     */
    public boolean isNodeDetected(int uid) {
        return VPNJni.isNodeDetected(0, uid);
    }

    /**
     * 添加支持的游戏
     *
     * @param uid         UID
     * @param packageName 包名
     * @param label       名字
     * @param protocol    协议
     */
    public void addSupportGame(int uid, String packageName, String label, Protocol protocol) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(uid).append(':').append(packageName).append(':');
        if (label != null) {
            sb.append(label);
        }
        sb.append(':').append(protocol.lowerText);
        this.setString(0, Defines.VPNJniStrKey.KEY_ADD_GAME, sb.toString());
    }

    /**
     * 启动VPN模式的代理
     *
     * @param fd VPN Interface 的FD
     * @return true表示成功，false表示失败
     */
    public boolean startVPN(int fd) {
        return VPNJni.doStartVPN(fd);
    }

    /**
     * 结束VPN模式的代理
     */
    public void stopVPN() {
        VPNJni.doStopVPN();
    }
}
