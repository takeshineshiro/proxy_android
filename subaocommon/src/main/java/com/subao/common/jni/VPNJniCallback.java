package com.subao.common.jni;

public interface VPNJniCallback {

    /**
     * 底层调用Java层的VPNService，保护指定的FD。（SDK无此需求）
     */
    int protectFd(int fd);

    void updateState(int state);

    /**
     * JNI底层的游戏延迟数据发生改变时调用这个函数
     *
     * @param delayMilliseconds 延迟的毫秒数
     */
    void onGameDelayDetectResult(int delayMilliseconds);

    /**
     * 保存指定数据到指定文件
     */
    void saveToFile(String filename, String data);

    /**
     * 当游戏发生连接的时候
     */
    void onGameConnected(int uid, int connTime);

    /**
     * 当重线重连发生的时候
     */
    void onRepairConnection(int uid, int taskId, boolean succ, int reconnCount);

    void onCloseConnect(int errcode);

    void onCreateConnect(int errcode, boolean transparent);

    /**
     * C层上报的Link信息的字符串
     */
    void onLinkMessage(int uid, String jsonFromJNI);

//    /**
//     * 被C层调用：已经有Link消息了，但是暂时没发（要等UDP超时10秒以后）
//     *
//     * @deprecated V3版的消息上报废弃本接口 (by YHB 2016.12.15)
//     */
//    @Deprecated
//    void onLinkMessageBegin(int uid);

    void onQosMessage(String jsonFromJNI);

    void onNetMeasureMessage(String jsonFromJNI);

    /**
     * 底层上传游戏日志（相关数据）
     *
     * @param log 数据
     */
    void onGameLog(String log);

    /**
     * 底层通知：已检测到节点
     */
    void onNodeDetect(int code, int uid, boolean succ);

    /**
     * C层通知：节点到游戏服的时延值
     *
     * @param uid   是哪个游戏？
     * @param delay 时延毫秒数。负数表示无效值
     */
    void onNode2GameServerDelay(int uid, int delay);

    /**
     * C层通知：透传了
     *
     * @param uid
     * @param port
     * @param delay
     */
    void onDirectTrans(int uid, int port, int delay);

    /**
     * 被C层调用：开启Qos提速通道
     *
     * @param id          一个id，用于标识本次要提速的“Socket五元组”
     * @param node        向哪个节点发Qos请求？
     * @param accessToken 访问Token
     * @param sourIp      源IP。如果为null或empty，则上层自动检测
     * @param sourPort    源端口，本机序
     * @param destIp      目标IP
     * @param destPort    目标端口
     * @param protocol    协议类型。"UDP" or "TCP"
     * @param timeSeconds 加速时长，单位秒
     */
    void openQosAccel(int id, String node, String accessToken, String sourIp, int sourPort, String destIp, int destPort, String protocol, int timeSeconds);

    /**
     * 被C层调用：关闭Qos提速通道
     *
     * @param id          调用openQosAccel时设置的id
     * @param node        向哪个节点发Qos请求？
     * @param accessToken 访问Token
     * @see #openQosAccel(int, String, String, String, int, String, int, String, int)
     */
    void closeQosAccel(int id, String node, String accessToken);

    /**
     * 被C层调用：对指定的Qos通道续订
     *
     * @param id          调用openQosAccel时设置的id
     * @param node        向哪个节点发Qos请求？
     * @param accessToken 访问Token
     * @param timeSeconds 时长
     * @see #openQosAccel(int, String, String, String, int, String, int, String, int)
     */
    void modifyQosAccel(int id, String node, String accessToken, int timeSeconds);

    /**
     * 被C层调用：通知Java层，双链接辅路流量
     */
    void onQPPVicePathFlow(int uid, String protocol, int flowBytes, int totalTime, int enableTime);

    void onTencentSGameDelayInfo(int avg, int sd, float dr);

    /**
     * 被C层调用：底层通知获取token
     *
     * @param ip   ip
     * @param auth 认证Header相关数据
     */
    void onGetToken(String ip, String auth);

    /**
     * 被C层调用：底层通知获取jwt token
     *
     * @param userId user标识
     * @param token  token
     */
    void onGetJWTToken(String userId, String token, String appId);

    /**
     * 被C层调用：底层通知获取用户加速状态
     *
     * @param userId   user id
     * @param jwtToken jwt token
     */
    void onGetUserAccelStatus(String userId, String jwtToken);

    /**
     * 被C层调用：底层通知获取用户配置
     *
     * @param jwtToken jwt token
     * @param openId   open id
     */
    void onGetUserConfig(String openId, String jwtToken);

    /**
     * C层调用：开/关加速
     */
    void switchAccel(boolean on);


    /**
     * C调用：Wifi加速状态改变
     *
     * @param isWiFiAccelActivated 当前是否处于WiFi加速状态中
     */
    void onWifiAccelStateChange(boolean isWiFiAccelActivated);

}
