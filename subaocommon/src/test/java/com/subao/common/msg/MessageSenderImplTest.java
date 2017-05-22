package com.subao.common.msg;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.subao.common.Logger;
import com.subao.common.MockWebServerDispatcher;
import com.subao.common.RoboBase;
import com.subao.common.collection.Ref;
import com.subao.common.data.AppType;
import com.subao.common.data.ServiceLocation;
import com.subao.common.net.Http;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.ThreadUtils;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by YHB on 2016/8/15.
 */
public class MessageSenderImplTest extends RoboBase {

    private static final String TEST_APPLABEL = "Test_AppLabel";
    private static final String TEST_PKGNAME = "Test_PkgName";
    private static final String TEST_VERSION = "Test_Version";
    private static final String TEST_IMSI = "Test_IMSI";
    private static final String TEST_CHANNEL = "Test_Channel";
    private static final AppType TEST_APPTYPE = AppType.ANDROID_APP;
    private static final long TEST_TIME = System.currentTimeMillis() / 1000;


    private MockWebServer mockWebServer;
    private MessageSenderImpl msgSender;
    private MessageTools msgTools;
    private MessageUserId messageUserId;

    private String expireTime;

    private static List<Message_App> createAppList() {
        List<Message_App> appList = new ArrayList<Message_App>(3);
        for (int i = 0; i < 3; ++i) {
            appList.add(new Message_App("appLabel_" + i, "pkgName_" + i));
        }
        return appList;
    }

    private static Message_VersionInfo createMessageVersionInfo() {
        return Message_VersionInfo.create("1.0.1", "channel");
    }

    @Before
    public void setUp() throws Exception {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        msgTools = new MsgTools();
        msgSender = (MessageSenderImpl) MessageSenderImpl.create(new ServiceLocation(null,
                mockWebServer.getHostName(), mockWebServer.getPort()),
            msgTools);
        this.messageUserId = MessageUserId.build();
        this.expireTime = MessageUserId.getCurrentExpireTime();
        this.msgSender.setHttpTimeout(1000);
    }

    @After
    public void ternDown() throws IOException {
        MessageUserId.setCurrentUserInfo(messageUserId.userId, messageUserId.serviceId, messageUserId.userStatus, this.expireTime);
        MessageUserId.setCurrentUserConfig(messageUserId.userId, messageUserId.userConfig);
        msgSender.quit();
        mockWebServer.shutdown();
        Logger.setLoggableChecker(null);
    }

    @Test
    public void testInvokerTimeout() throws IOException {
        MessageSenderImpl.MessageHandler msgHandler = msgSender.messageHandler;
        MockPerformer invoker = new MockPerformer(msgHandler);
        //
        // 没有提供HttpMethod，会抛异常
        try {
            invoker.run();
            fail();
        } catch (NullPointerException e) {

        }
        //
        // 提供HttpMethod，但不开启WebServer
        mockWebServer.shutdown();
        invoker.httpMethod = Http.Method.POST;
        invoker.run();
        Assert.assertNull(invoker.httpResponse);
    }

    @Test
    public void testInvoker2() throws IOException {
        MessageSenderImpl.MessageHandler msgHandler = msgSender.messageHandler;
        MockPerformer invoker = new MockPerformer(msgHandler);
        for (int i = 0; i < 1; ++i) {
            invoker.httpResName = (i == 0) ? null : "/foo/";
            for (Http.Method method : Http.Method.values()) {
                MockResponse mr = new MockResponse();
                mr.setResponseCode(200);
                mr.setBody("hello");
                mockWebServer.enqueue(mr);
                invoker.httpMethod = method;
                invoker.run();
                assertNotNull(invoker.httpResponse);
                assertEquals(200, invoker.httpResponse.code);
                assertEquals("hello", new String(invoker.httpResponse.data));
            }
        }
    }

    @Test
    public void setGameServerId() {
        String gameServerId = "GameServerId";
        msgSender.setGameServerId(gameServerId);
        assertEquals(gameServerId, msgSender.getGameServerId());
    }

    @Test
    public void setFreeFlowType() {
        for (MessageSender.FreeFlowType freeFlowType : MessageSender.FreeFlowType.values()) {
            msgSender.setFreeFlowType(freeFlowType);
            assertEquals(freeFlowType, msgSender.getFreeFlowType());
        }
    }

    @Test
    public void testMessageInstallation() throws IOException, JSONException {
        Context context = RuntimeEnvironment.application;
        List<Message_App> appList = createAppList();
        Message_Installation msg = new Message_Installation(
            AppType.ANDROID_SDK, System.currentTimeMillis() / 1000,
            Message_Installation.UserInfo.create(context),
            new Message_DeviceInfo(context),
            Message_VersionInfo.create("versionName", "channel"),
            appList);
        msgSender.offerInstallation(msg);
        //
        MessageSenderImpl.MessageHandler msgHandler = msgSender.messageHandler;
        MessageSenderImpl.MessageHandler.Performer_Installation performer =
            msgHandler.new Performer_Installation(msg);
        MockWebServerDispatcher dispatcher = MockWebServerDispatcher.createAndSetToWebServer(mockWebServer);
        performer.run();
        assertEquals("/v3/report/client/installation/android", dispatcher.getLastRequest().getPath());
        assertEquals(Http.Method.POST.str, dispatcher.getLastRequest().getMethod());
        //
        String json = dispatcher.getLastRequest().getBody().readUtf8();
        JSONObject jsonObject = new JSONObject(json);
        //TODO: 检查格式
    }

    private void doTestHttpRequest(MessageSenderImpl.MessageHandler.Performer performer) throws IOException {
        int[] responseCodeList = new int[]{200, 201, 400, 404, 500};
        for (int code : responseCodeList) {
            MockResponse mr = new MockResponse();
            mr.setResponseCode(code);
            if (code == 201) {
                mr.setBody("{\"id\":{\"id\":\"subao_id\"}}");
            }
            mockWebServer.enqueue(mr);
            performer.run();
        }
    }

    @Test
    public void testMessageUpgrade() throws Exception {
        Message_Upgrade msg = new Message_Upgrade(
            MessageUserId.build(),
            System.currentTimeMillis(),
            Message_VersionInfo.create("1.0.0", "channel"),
            createMessageVersionInfo(),
            AppType.ANDROID_APP);
        msgSender.offerUpgrade(msg);
        MessageSenderImpl.MessageHandler msgHandler = msgSender.messageHandler;
        MessageSenderImpl.MessageHandler.Performer_Upgrade invoker =
            msgHandler.new Performer_Upgrade(msg);
        doTestHttpRequest(invoker);
        //
        // TODO: 测试上报的内容是否正确
    }

    @Test
    public void testMessageStart() throws Exception {
        List<Message_App> appList = new ArrayList<Message_App>(2);
        appList.add(new Message_App("1", "2"));
        appList.add(new Message_App("3", "4"));
        msgSender.offerStart(10, 20, appList);
        MessageSenderImpl.MessageHandler.Performer_Start invoker =
            msgSender.messageHandler.new Performer_Start(10, 20, appList);
        doTestHttpRequest(invoker);
        // TODO: 测试上报内容是否正确
    }

//    @Test
//    public void testMessageGaming() throws Exception {
//        Message_Gaming msg = new Message_Gaming(
//            MessageUserId.build(), System.currentTimeMillis(),
//            AppType.ANDROID_APP,
//            new Message_App("label", "pkgName"),
//            Message_Gaming.AccelMode.VPN_MODE,
//            createMessageVersionInfo(),
//            new Message_DeviceInfo("model", 1234, 8, 5678));
//        MessageSenderImpl.MessageHandler.Performer_Gaming invoker =
//            msgSender.messageHandler.new Performer_Gaming(1, msg);
//        doTestHttpRequest(invoker);
//        // TODO: 测试上报内容是否正确
//    }

    @Test
    public void offerEvent() throws Exception {
        List<Message_EventMsg.Event> events = new ArrayList<Message_EventMsg.Event>(1);
        Message_EventMsg.Event event = new Message_EventMsg.Event("hello", 12345678, null);
        events.add(event);
        Message_EventMsg msg = new Message_EventMsg(
            MessageUserId.build(), AppType.ANDROID_APP,
            createMessageVersionInfo(),
            events);
        msgSender.offerEvent(msg);
        msgSender.offerEvent("hello", "world");
        msgSender.offerStructureEvent("test");
        msgSender.offerEvent(event);
        //
        MessageSenderImpl.MessageHandler.Performer_Event e =
            msgSender.messageHandler.new Performer_EventEntire(msg);
        final Ref<RecordedRequest> requestRef = new Ref<RecordedRequest>();
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                requestRef.set(request);
                return super.dispatch(request);
            }
        };
        mockWebServer.setDispatcher(dispatcher);
        new Thread(e).start();
        dispatcher.waitForResponse(1000);
        assertEquals(Http.Method.POST.str, requestRef.get().getMethod());
        assertEquals("/v3/report/client/event", requestRef.get().getPath());
        assertNull(e.buildPostData());
        //
        requestRef.set(null);
        e = msgSender.messageHandler.new Performer_EventBody(event);
        dispatcher.resetOverFlag();
        new Thread(e).start();
        dispatcher.waitForResponse(1000);
        assertEquals(Http.Method.POST.str, requestRef.get().getMethod());
        assertEquals("/v3/report/client/event", requestRef.get().getPath());
        assertNull(e.buildPostData());
        //
        requestRef.set(null);
        e = msgSender.messageHandler.new Performer_EventByNameValue("hello", "world");
        dispatcher.resetOverFlag();
        new Thread(e).start();
        dispatcher.waitForResponse(1000);
        assertEquals(Http.Method.POST.str, requestRef.get().getMethod());
        assertEquals("/v3/report/client/event", requestRef.get().getPath());
        assertNull(e.buildPostData());
        //
        requestRef.set(null);
        MessageSenderImpl.MessageHandler.Performer_EventStructure e2 = msgSender.messageHandler.new Performer_EventStructure("test");
        assertArrayEquals("test".getBytes(), e2.buildPostData());
        dispatcher.resetOverFlag();
        new Thread(e2).start();
        dispatcher.waitForResponse(1000);
        assertEquals(Http.Method.POST.str, requestRef.get().getMethod());
        assertEquals("/v3/report/client/event", requestRef.get().getPath());
        assertNull(e.buildPostData());
    }


    @Test
    public void onJNILinkMsg() throws Exception {
        msgSender.onJNILinkMsg("1", null, false);
        msgSender.onJNILinkMsg("1", "", false);
        msgSender.onJNILinkMsg("1", "body", true);
    }

    @Test
    public void onJNINetMeasureMsg() throws Exception {
        msgSender.onJNINetMeasureMsg(null);
        msgSender.onJNINetMeasureMsg("");
        msgSender.onJNINetMeasureMsg("json");
    }

    @Test
    public void onJNIQosMsg() throws Exception {
        msgSender.onJNIQosMsg(null);
        msgSender.onJNIQosMsg("");
        msgSender.onJNIQosMsg("test");
    }

    @Test
    public void doesDelayQualityFeedbackNeedReport() {
        for (int i = 0; i < 10; ++i) {
            MessageUserId.setCurrentUserInfo(messageUserId.userId, messageUserId.serviceId, i, expireTime);
            boolean need = MessageSenderImpl.doesDelayQualityFeedbackNeedReport();
            assertEquals(need, i == 2 || i == 4 || i == 6);
        }
    }

    @Test
    public void onNetDelayQualityV2() {
        Message_Link.DelayQualityV2 dq = new Message_Link.DelayQualityV2(
            1.2f, 3.4f, 0.56f, 0.22f, 0.33f, 30f
        );
        MessageUserId.setCurrentUserInfo(messageUserId.userId, messageUserId.serviceId, 2, expireTime);
        msgSender.onNetDelayQualityV2(dq);
    }

    @Test
    public void testRunnable_ProcessLastLinkMessage() {
        Runnable r = msgSender.messageHandler.new Runnable_ProcessLastLinkMessage();
        r.run();
    }

    /////////////////////////////////////////////////////

    private static class MsgTools implements MessageTools {
        private final Handler handler = new MyHandler();
        private final List<Message_App> appList;
        private final MessageBuilder messageBuilder;
        private final MessagePersistent mockMessagePersistent = new MessagePersistent(
            new MockMessagePersistentOperator());
        public boolean messageStartSent;

        public MsgTools() {
            appList = new ArrayList<Message_App>(3);
            for (int i = 0; i < 3; ++i) {
                Message_App app = new Message_App("label_" + i, "pkg_" + i);
                appList.add(app);
            }
            //
            messageBuilder = new MessageBuilder(RuntimeEnvironment.application,
                AppType.ANDROID_SDK, Message_VersionInfo.create("1.2.3", "The Channel"));
        }

        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }

        @Override
        public NetTypeDetector getNetTypeDetector() {
            return null;
        }

        @Override
        public AppType getAppType() {
            return TEST_APPTYPE;
        }

        @Override
        public void runInMainThread(Runnable r) {
            if (ThreadUtils.isInAndroidUIThread()) {
                r.run();
            } else {
                handler.post(r);
            }
        }

        @Override
        public void onMessageStartSent() {
            messageStartSent = true;
        }

        @Override
        public String getIMSI() {
            return TEST_IMSI;
        }

        @Override
        public Message_Gaming.AccelMode getCurrentAccelMode() {
            return Message_Gaming.AccelMode.UNKNOWN_ACCEL_MODE;
        }

        @Override
        public MessageBuilder getMessageBuilder() {
            return this.messageBuilder;
        }

        @Override
        public MessagePersistent getMessagePersistent() {
            return this.mockMessagePersistent;
        }

        private static class MyHandler extends Handler {
            public MyHandler() {
                super(Looper.getMainLooper());
            }
        }
    }

    private static class MockPerformer extends MessageSenderImpl.MessageHandler.Performer {

        public String httpResName;
        public byte[] postData;
        public Http.Response httpResponse;
        public Http.Method httpMethod;

        public MockPerformer(MessageSenderImpl.MessageHandler msgHandler) {
            msgHandler.super("MockPerformer");
        }

        @Override
        protected String getHttpResName() {
            return httpResName;
        }

        @Override
        protected byte[] buildPostData() throws IOException {
            return postData;
        }

        @Override
        protected void onIOException() {
            this.httpResponse = null;
        }

        @Override
        protected void onHttpResponse(Http.Response response) {
            this.httpResponse = response;
        }

        @Override
        protected Http.Method getHttpMethod() {
            return httpMethod;
        }
    }

    private static class MockMessagePersistentOperator implements MessagePersistent.Operator {

        @Override
        public RandomAccessFile openFile(String filename, boolean readonly) throws IOException {
            return null;
        }

        @Override
        public String[] enumFilenames() {
            return null;
        }

        @Override
        public void delete(String filename) {

        }
    }

}