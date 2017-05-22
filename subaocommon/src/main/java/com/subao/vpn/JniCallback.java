package com.subao.vpn;

/**
 * JniCallback
 * <p>Created by YinHaiBo on 2017/1/6.</p>
 */

public interface JniCallback {

    /**
     * JNI开关加速的时候通知Java
     *
     * @param open true表示开加速，false表示关加速
     */
    void onProxyActive(boolean open);

    void requestUserAuth(int cid, String userId, String token, String appId);

    void requestLinkAuth(int cid, String nodeIP, String jwtToken);

    void requestUserConfig(int cid, String userId, String jwtToken);

    void requestUserState(int cid, String userId, String jwtToken);

    /**
     * JNI向Java层申请基于移动网络的FD
     *
     * @param cid Call ID
     */
    void requestMobileFD(int cid);

    /**
     * JNI请求ISP信息
     *
     * @param cid Call ID
     */
    void requestISPInformation(int cid);

    /**
     * JNI产生了一条已序列化好的Link消息，通知Java
     *
     * @param msgId      用来区分不同消息的标识
     * @param msgContent 已序列化好的Link消息
     * @param end        为true表示此条Link已结束，不会再有新的数据再来了（可以上报了）
     */
    void onLinkMessage(String msgId, String msgContent, boolean end);

    void openQosAccel(int cid, String node, String accessToken, String sourIp, int sourPort, String destIp, int destPort, String protocol, int timeSeconds);

    void closeQosAccel(int cid, String sessionId, String node, String accessToken);

    void modifyQosAccel(int cid, String sessionId, String node, String accessToken, int timeSeconds);

    /**
     * JNI产生了一条已序列化好的Qos消息，通知Java
     *
     * @param message 消息体
     */
    void onQosMessage(String message);

    /**
     * JNI产生了一条Event的信息
     *
     * @param message 消息内容
     */
    void onJNIReportEvent(String message);

    /**
     * JNI产生了一条Lua错误信息
     *
     * @param content
     */
    void onLuaError(String content);

    void onAccelInfoUpload(String content, String userId, String jwtToken);

    /**
     * JNI请求在本地磁盘保存一段数据
     *
     * @param name  数据的名称。保证可用作文件名
     * @param value 数据的内容
     */
    void requestSaveData(String name, String value);

    /**
     * JNI请求加载先前通过{@link #requestSaveData(String, String)}保存的数据
     *
     * @param cid  Call ID
     * @param name 数据的名称。
     * @see #requestSaveData(String, String)
     */
    void requestLoadData(int cid, String name);

    /**
     * JNI请求Java：向服务器发起一个计数器请求
     *
     * @param cid         Call ID
     * @param counterName 计数器名称（类型）
     * @see com.subao.common.data.Defines.VPNJniStrKey#KEY_BEACON_COUNTER_RESULT
     */
    void requestBeaconCounter(int cid, String counterName);

    /**
     * JNI请求VPNService保护一个Socket句柄
     * @param socket FD
     * @return 0表示成功，其它值为错误代码
     */
    int protectFD(int socket);
}
