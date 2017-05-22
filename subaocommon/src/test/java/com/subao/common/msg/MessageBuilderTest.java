package com.subao.common.msg;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.subao.common.RoboBase;
import com.subao.common.data.AppType;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MessageBuilderTest
 * <p>Created by YinHaiBo on 2016/12/20.</p>
 */
public class MessageBuilderTest extends RoboBase {

    private MessageBuilder messageBuilder;
    private Message_VersionInfo versionInfo;

    @Before
    public void setUp() {
        this.versionInfo = Message_VersionInfo.create("1.2.3.4", "test channel");
        this.messageBuilder = new MessageBuilder(
            RuntimeEnvironment.application,
            AppType.UNKNOWN_APPTYPE,
            this.versionInfo
        );
    }

    @Test
    public void buildMessageApp() throws Exception {
        Context context = RuntimeEnvironment.application;
        Message_App messageApp = MessageBuilder.buildMessageApp(context);
        assertEquals(context.getPackageName(), messageApp.pkgName);
        assertEquals(context.getApplicationInfo().loadLabel(context.getPackageManager()), messageApp.appLabel);
        //
        context = mock(Context.class);
        when(context.getPackageManager()).thenReturn(null);
        messageApp = MessageBuilder.buildMessageApp(context);
        assertNull(messageApp.appLabel);
        //
        context = mock(Context.class);
        when(context.getApplicationInfo()).thenReturn(null);
        messageApp = MessageBuilder.buildMessageApp(context);
        assertNull(messageApp.appLabel);
        //
        context = mock(Context.class);
        ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        when(context.getApplicationInfo()).thenReturn(applicationInfo);
        when(applicationInfo.loadLabel(any(PackageManager.class))).thenReturn(null);
        messageApp = MessageBuilder.buildMessageApp(context);
        assertNull(messageApp.appLabel);
    }

    @Test
    public void buildMessageGaming() throws Exception {
        MessageUserId messageUserId = MessageUserId.build();
        long timeForUTCSeconds = 12345678L;
        MessageTools.GameInfo gameInfo = new MessageTools.GameInfo(MessageBuilder.buildMessageApp(RuntimeEnvironment.application), AppType.ANDROID_SDK_EMBEDED);
        Message_Gaming.AccelMode accelMode = Message_Gaming.AccelMode.VPN_MODE;
        Message_Gaming messageGaming = messageBuilder.buildMessageGaming(
            messageUserId,
            timeForUTCSeconds,
            gameInfo,
            accelMode);
        assertEquals(timeForUTCSeconds, messageGaming.time);
        assertEquals(messageUserId, messageGaming.id);
        assertEquals(gameInfo.appType, messageGaming.appType);
        assertEquals(gameInfo.messageApp, messageGaming.game);
        assertEquals(accelMode, messageGaming.mode);
    }

    @Test
    public void getVersionInfo() throws Exception {
        assertEquals(this.versionInfo, messageBuilder.getVersionInfo());
    }

    @Test
    public void getDeviceInfo() throws Exception {
        assertEquals(new Message_DeviceInfo(RuntimeEnvironment.application), messageBuilder.getDeviceInfo());
    }

    @Test
    public void getAppType() throws Exception {
        assertEquals(AppType.UNKNOWN_APPTYPE, messageBuilder.getAppType());
    }

    @Test
    public void buildMessageStart() throws Exception {
        MessageUserId messageUserId = MessageUserId.build();
        Message_Start start = messageBuilder.buildMessageStart(
            messageUserId, 123, 456, createAppList());
        assertEquals(messageUserId, start.id);
        assertEquals(123, start.nodeNum);
        assertEquals(456, start.gameNum);
        assertEquals(createAppList(), start.getAppList());
    }

    private static List<Message_App> createAppList() {
        List<Message_App> appList = new ArrayList<Message_App>(4);
        for (int i = 0; i < 4; ++i) {
            appList.add(new Message_App(Integer.toString(i), Integer.toString(i + 1)));
        }
        return appList;
    }

    @Test
    public void buildMessageInstallation() throws Exception {
        Context context = RuntimeEnvironment.application;
        Message_Installation msg = messageBuilder.buildMessageInstallation(
            1234L, Message_Installation.UserInfo.create(context),
            createAppList());
        assertEquals(1234L, msg.unixTime);
        assertEquals(createAppList(), msg.getAppList());
        assertEquals(Message_Installation.UserInfo.create(context), msg.userInfo);
        assertEquals(messageBuilder.getAppType(), msg.appType);
    }

    @Test
    public void buildMessageEvent() throws Exception {
        List<Message_EventMsg.Event> events = new ArrayList<Message_EventMsg.Event>(2);
        events.add(new Message_EventMsg.Event("1", 2, null));
        events.add(new Message_EventMsg.Event("3", 4, new HashMap<String, String>()));
        Message_EventMsg event = messageBuilder.buildMessageEvent(
            MessageUserId.build(), events);
        assertEquals(event.appType, messageBuilder.getAppType());
        assertEquals(event.msgUserId, MessageUserId.build());
        assertEquals(event.versionInfo, messageBuilder.getVersionInfo());
    }

    @Test
    public void buildMessageEvent2() throws Exception {
        Message_EventMsg event = messageBuilder.buildMessageEvent(
            MessageUserId.build(), "hello", "world");
        assertEquals(event.appType, messageBuilder.getAppType());
        assertEquals(event.msgUserId, MessageUserId.build());
        assertEquals(event.versionInfo, messageBuilder.getVersionInfo());
        //
        int i = 0, j = 0;
        for (Message_EventMsg.Event e : event) {
            ++i;
            assertEquals(1, e.getParamsCount());
            assertEquals("hello", e.id);
            for (Map.Entry<String, String> p : e) {
                assertEquals("param", p.getKey());
                assertEquals("world", p.getValue());
                ++j;
            }
        }
        assertEquals(1, i);
        assertEquals(1, j);
    }

    @Test
    public void buildMessageEvent_TencentGameDelayInfo() {
        Message_EventMsg event = messageBuilder.buildMessageEvent_TencentGameDelayInfo(
            MessageUserId.build(), 1, 2, 3.45f);
        int i = 0;
        for (Message_EventMsg.Event e : event) {
            ++i;
            assertEquals(3, e.getParamsCount());
            assertEquals("tg_delay", e.id);
            assertEquals("1", e.getParamValue("avg"));
            assertEquals("2", e.getParamValue("sd"));
            assertEquals(3.45f, Float.parseFloat(e.getParamValue("dr")), Float.MIN_NORMAL);
        }
        assertEquals(1, i);
    }

    @Test
    public void buildMessageEvent_AccelRecommendation() {
        Message_EventMsg event = messageBuilder.buildMessageEvent_AccelRecommendation(
            MessageUserId.build(), 1, 2, true);
        int i = 0;
        for (Message_EventMsg.Event e : event) {
            ++i;
            assertEquals(3, e.getParamsCount());
            assertEquals("tg_accel_recommend", e.id);
            assertEquals("1", e.getParamValue("result"));
            assertEquals("2", e.getParamValue("net"));
            assertEquals("1", e.getParamValue("accel"));
        }
        assertEquals(1, i);
    }

}