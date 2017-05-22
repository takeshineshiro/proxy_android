package com.subao.common.msg;

import java.util.List;

public interface MessageSender {

    /**
     * 设置游戏区服ID（SDK使用）
     */
    void setGameServerId(String gameServerId);

    /**
     * 设置玩家免流类型（王者荣耀SDK使用）
     */
    void setFreeFlowType(FreeFlowType type);

    /**
     * 将一个{@link Message_Installation}消息入队
     */
    void offerInstallation(Message_Installation msg);

    /**
     * 将一个{@link Message_Upgrade}消息入队
     * <p>
     * <i>（SDK无此消息）</i>
     * </p>
     */
    void offerUpgrade(Message_Upgrade msg);

    /**
     * 请求发送{@link Message_Start}消息
     */
    void offerStart(int nodeNum, int gameNum, List<Message_App> appList);

    /**
     * 将指定的事件入队
     */
    void offerEvent(String eventName, String eventParam);

    /**
     * 将指定的事件入队
     */
    void offerEvent(Message_EventMsg event);

    /**
     * 将指定的事件入队
     */
    void offerEvent(Message_EventMsg.Event event);

    /**
     * 将一个已构造好的事件入队
     * @param packedEvent 已按协议格式制作后的事件数据Body
     */
    void offerStructureEvent(String packedEvent);

    /**
     * 收到底层传来的LinkMsg时被调用
     *
     * @param messageId   消息ID
     * @param messageBody 消息体
     * @param finish      true表示此消息ID的消息已结束，可以立刻上报了
     */
    void onJNILinkMsg(String messageId, String messageBody, boolean finish);

    /**
     * 收到底层传来的NetMeasurement消息
     *
     * @param jsonFromJNI NetMeasurementV2的Json串
     */
    void onJNINetMeasureMsg(String jsonFromJNI);

    /**
     * 收到底层传来的Qos消息
     */
    void onJNIQosMsg(String jsonFromJNI);

    /**
     * 收到《王者荣耀》传来的延迟值统计数据
     *
     * @param delayQualityV2 V2版的延迟值统计数据 {@link Message_Link.DelayQualityV2}
     */
    void onNetDelayQualityV2(Message_Link.DelayQualityV2 delayQualityV2);

    enum FreeFlowType {
        UNKNOWN(0),

        /**
         * 中国移动
         */
        CMCC(1),

        /**
         * 中国联通
         */
        CUCC(2),

        /**
         * 中国电信
         */
        CTCC(3);

        public final int intValue;

        FreeFlowType(int intValue) {
            this.intValue = intValue;
        }
    }


}
