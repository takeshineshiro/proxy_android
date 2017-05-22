package com.subao.common.msg;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.subao.common.data.AppType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息体构建器
 * <p>Created by YinHaiBo on 2016/12/16.</p>
 */

public class MessageBuilder {

    private final AppType appType;
    private final Message_VersionInfo versionInfo;
    private final Message_DeviceInfo deviceInfo;

    public MessageBuilder(Context context, AppType appType, Message_VersionInfo versionInfo) {
        this.appType = appType;
        this.versionInfo = versionInfo;
        this.deviceInfo = new Message_DeviceInfo(context);
    }

    /**
     * 创建本游戏（如果是SDK）或迅游加速器APP自身的 {@link Message_App}
     */
    public static Message_App buildMessageApp(Context context) {
        String appLabel = null;
        String packageName = context.getPackageName();
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            ApplicationInfo appInfo = context.getApplicationInfo();
            if (appInfo != null) {
                CharSequence label = appInfo.loadLabel(pm);
                if (label != null) {
                    appLabel = label.toString();
                }
            }
        }
        return new Message_App(appLabel, packageName);
    }

    /**
     * 构建一个{@link Message_Gaming} 对象
     *
     * @param messageUserId     {@link MessageUserId}
     * @param timeForUTCSeconds 时刻，UTC的秒数
     * @param game              {@link Message_App} 对于SDK来说，即集成SDK的游戏本身；对于APP来说，是当前玩的那个游戏
     * @param accelMode         对于SDK来说，此值填为
     *                          {@link com.subao.common.msg.Message_Gaming.AccelMode#UNKNOWN_ACCEL_MODE}即可。
     * @return {@link Message_Gaming}
     */
    public Message_Gaming buildMessageGaming(
        MessageUserId messageUserId, long timeForUTCSeconds,
        MessageTools.GameInfo game,
        Message_Gaming.AccelMode accelMode) {
        return new Message_Gaming(
            messageUserId,
            timeForUTCSeconds,
            game.appType,
            game.messageApp,
            accelMode,
            this.versionInfo,
            this.deviceInfo);
    }

    public Message_VersionInfo getVersionInfo() {
        return this.versionInfo;
    }

    public Message_DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    public AppType getAppType() {
        return this.appType;
    }

    /**
     * 创建{@link Message_Start}
     */
    public Message_Start buildMessageStart(
        MessageUserId msgUserId, int nodeNum,
        int gameNum, List<Message_App> appList
    ) {
        return new Message_Start(
            msgUserId,
            Message_Start.StartType.START,
            nodeNum, gameNum,
            null, appList,
            this.versionInfo,
            this.appType);
    }

    public Message_Installation buildMessageInstallation(
        long unixTime, Message_Installation.UserInfo userInfo,
        List<Message_App> appList
    ) {
        return new Message_Installation(
            this.appType, unixTime,
            userInfo, this.deviceInfo, this.versionInfo, appList);
    }

    /////////////////////////////////////////////////////////////////////
    //        以下是Event相关的
    /////////////////////////////////////////////////////////////////////

    /**
     * 根据给定的数据创建一个 {@link Message_EventMsg}
     *
     * @param msgUserId {@link MessageUserId}
     * @param events    {@link com.subao.common.msg.Message_EventMsg.Event} 列表
     * @return {@link Message_EventMsg}
     */
    public Message_EventMsg buildMessageEvent(MessageUserId msgUserId, List<Message_EventMsg.Event> events) {
        return new Message_EventMsg(
            msgUserId,
            this.appType,
            this.versionInfo,
            events);
    }

    public Message_EventMsg buildMessageEvent(MessageUserId msgUserId, String eventId, String eventParam) {
        Map<String, String> params = new HashMap<String, String>(1);
        params.put("param", eventParam);
        return buildMessageEvent(msgUserId, buildEvents(eventId, params));
    }

    public Message_EventMsg buildMessageEvent_TencentGameDelayInfo(MessageUserId msgUserId, int avg, int sd, float dr) {
        Map<String, String> params = new HashMap<String, String>(3);
        params.put("avg", Integer.toString(avg));
        params.put("sd", Integer.toString(sd));
        params.put("dr", Float.toString(dr));
        return buildMessageEvent(msgUserId, buildEvents("tg_delay", params));
    }

    public Message_EventMsg buildMessageEvent_AccelRecommendation(MessageUserId msgUserId, int result, int netType, boolean isAccelOpened) {
        Map<String, String> params = new HashMap<String, String>(3);
        params.put("result", Integer.toString(result));
        params.put("net", Integer.toString(netType));
        params.put("accel", isAccelOpened ? "1" : "0");
        return buildMessageEvent(msgUserId, buildEvents("tg_accel_recommend", params));
    }

    private List<Message_EventMsg.Event> buildEvents(String eventId, Map<String, String> params) {
        List<Message_EventMsg.Event> events = new ArrayList<Message_EventMsg.Event>(1);
        events.add(new Message_EventMsg.Event(eventId, System.currentTimeMillis() / 1000, params));
        return events;
    }


}
