package com.subao.common.qos;

import android.os.ConditionVariable;

import com.subao.common.ErrorCode;
import com.subao.common.Logger;
import com.subao.common.MockWebServerDispatcher;
import com.subao.common.RoboBase;
import com.subao.common.data.AppType;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.NetTypeDetector.NetType;
import com.subao.common.net.Protocol;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@Config(shadows = QosHelperTester.ShadowWorkFactory.class)
public class QosHelperTester extends RoboBase {

    private static final int CID = 123;
    private static final String ACCESS_TOKEN = "access token";

    private MockWebServer mockWebServer;
    private MockWebServerDispatcher mockDispatcher;
    private MockCallback mockCallback;

    private QosManager.Key createQosKey() {
        return new QosManager.Key(CID, mockWebServer.getHostName(), mockWebServer.getPort(), ACCESS_TOKEN);
    }

    @Before
    public void setUp() throws IOException {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        mockCallback = new MockCallback();
        mockWebServer = new MockWebServer();
        mockDispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(mockDispatcher);
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        Logger.setLoggableChecker(null);
    }

    @Test
    public void testCloser() {
        QosManager.Key key = createQosKey();
        String sessionId = "sessionId";
        QosHelper.Closer closer = new QosHelper.Closer(key, sessionId, mockCallback);
        assertEquals(QosManager.Action.CLOSE, closer.getAction());
        QosManager.Requester r = closer.createRequester("hello");
        assertEquals(closer.getAction(), r.getAction());
        assertEquals(key, r.key);
        mockDispatcher.setResponseCode(204);
        closer.run();
        //
        mockCallback.waitForCall();
        assertEquals(QosManager.Action.CLOSE, mockCallback.action);
        assertEquals(key.cid, mockCallback.param.cid);
        assertEquals(0, mockCallback.param.error);
        assertNull(mockCallback.param.event);
        assertEquals(sessionId, mockCallback.param.sessionId);
    }

    @Test
    public void testModifier() {
        QosManager.Key key = createQosKey();
        String sessionId = "sessionId";
        int timeLength = 60;
        QosHelper.Modifier modifier = new QosHelper.Modifier(key, sessionId, mockCallback, timeLength);
        assertEquals(QosManager.Action.MODIFY, modifier.getAction());
        QosManager.Requester r = modifier.createRequester("hello");
        assertEquals(modifier.getAction(), r.getAction());
        assertEquals(key, r.key);
        mockDispatcher.setResponseCode(200);
        mockDispatcher.setResponseBody("{\"resultCode\":0}");
        modifier.run();
        //
        mockCallback.waitForCall();
        assertEquals(QosManager.Action.MODIFY, mockCallback.action);
        assertEquals(key.cid, mockCallback.param.cid);
        assertEquals(0, mockCallback.param.error);
        assertEquals(timeLength, mockCallback.param.timeLength);
        assertNull(mockCallback.param.event);
        assertEquals(sessionId, mockCallback.param.sessionId);
    }

    private QosHelper.Opener createOpener(QosHelper.Tools tools, int timeSeconds) {
        return createOpener(tools, timeSeconds, null);
    }

    private QosHelper.Opener createOpener(QosHelper.Tools tools, int timeSeconds, String srcIp) {
        QosManager.EndPoint2EndPoint e2e = createE2E(srcIp);
        QosManager.Key key = createQosKey();
        mockCallback = new MockCallback();
        return new QosHelper.Opener(key, tools.getNetTypeDetector(), e2e,
            timeSeconds, tools, mockCallback);
    }

    private QosManager.EndPoint2EndPoint createE2E(String srcIp) {
        return new QosManager.EndPoint2EndPoint(
            srcIp, 81,    // 源IP和端口
            "192.168.1.1", 80,    // 目标IP和端口
            Protocol.TCP
        );
    }

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        testPrivateConstructor(QosHelper.class);
    }

    @Test(expected = RuntimeException.class)
    public void constructorError() {
        QosManager.EndPoint2EndPoint e2e = createE2E(null);
        QosManager.Key key = createQosKey();
        new QosHelper.Opener(key, null, e2e, 1000, new MockQosHelperTools(), null);
    }

    @Test
    public void openerInvalidTimeLength() {
        MockQosHelperTools tools = new MockQosHelperTools();
        QosHelper.Opener opener = createOpener(tools, -1);
        QosManager.Requester requester = opener.createRequester(null, null);
        assertEquals(QosManager.Action.OPEN, requester.getAction());
        opener.run();
        //
        mockCallback.waitForCall();
        assertEquals(QosManager.Action.OPEN, mockCallback.action);
        assertEquals(opener.qosKey.cid, mockCallback.param.cid);
        int error = ErrorCode.QOS_INVALID_TIME_LENGTH;
        assertEquals(error, mockCallback.param.error);
        assertEquals(Integer.toString(error), mockCallback.param.event.getParamValue("error"));
    }

    @Test
    public void openerNetFail() {
        MockQosHelperTools tools = new MockQosHelperTools();
        QosHelper.Opener opener = createOpener(tools, 60);
        tools.setCurrentNetworkType(NetType.DISCONNECT);    // 断网的情况
        opener.run();
        mockCallback.waitForCall();
        assertEquals(QosManager.Action.OPEN, mockCallback.action);
        assertEquals(opener.qosKey.cid, mockCallback.param.cid);
        int error = ErrorCode.QOS_NETWORK_DISCONNECT;
        assertEquals(error, mockCallback.param.error);
        assertEquals(Integer.toString(error), mockCallback.param.event.getParamValue("error"));
        //
        opener = createOpener(tools, 60);
        tools.setCurrentNetworkType(NetType.MOBILE_3G);
        opener.run();
        mockCallback.waitForCall();
        assertEquals(QosManager.Action.OPEN, mockCallback.action);
        assertEquals(opener.qosKey.cid, mockCallback.param.cid);
        error = ErrorCode.QOS_NOT_4G;
        assertEquals(error, mockCallback.param.error);
        assertEquals(Integer.toString(error), mockCallback.param.event.getParamValue("error"));
    }

    @Test
    public void openerRegionNotSupport() {
        MockQosHelperTools tools = new MockQosHelperTools();
        QosHelper.Opener opener = createOpener(tools, 60);
        tools.setCurrentNetworkType(NetType.MOBILE_4G);
        opener.run();
        mockCallback.waitForCall();
        assertEquals(QosManager.Action.OPEN, mockCallback.action);
        assertEquals(opener.qosKey.cid, mockCallback.param.cid);
        int error = ErrorCode.QOS_REGION_NOT_SUPPORT;
        assertEquals(error, mockCallback.param.error);
        assertEquals(Integer.toString(error), mockCallback.param.event.getParamValue("error"));
    }

    @Test
    @Config(shadows = ShadowQosUser4GRegionAndISP.class)
    public void openerOk() {
        MockQosHelperTools tools = new MockQosHelperTools();
        QosHelper.Opener opener = createOpener(tools, 60, "192.168.1.1");
        tools.setCurrentNetworkType(NetType.MOBILE_4G);
        mockDispatcher.setResponseCode(201);
        mockDispatcher.setResponseBody("{\"resultCode\":0, \"sessionId\":\"test\"}");
        opener.run();
        mockCallback.waitForCall();
        assertEquals(QosManager.Action.OPEN, mockCallback.action);
        assertEquals(opener.qosKey.cid, mockCallback.param.cid);
        int error = 0;
        assertEquals(error, mockCallback.param.error);
        assertNull(mockCallback.param.event);
    }

//        tools.setCurrentNetworkType(NetType.MOBILE_4G);
//        opener.run();
//        //
//        QosManager.EndPoint2EndPoint e2e = createE2E();
//        QosManager.Key key = createQosKey();
//        opener = new QosHelper.Opener(key, tools.getNetTypeDetector(), e2e, 60, tools, mockCallback);
//        opener.run();
//        //
//        QosParam qosParam = new QosParam(1, 2, null, 0, 0);
//        QosManager.Requester requester = opener.createRequester(qosParam, "imsi");
//        assertEquals(requester.key, opener.qosKey);
//        assertEquals(requester.getAction(), QosManager.Action.OPEN);
//        assertNotNull(requester.buildRequestParam());
//        //
//        opener = new QosHelper.Opener(createQosKey(),
//            tools.getNetTypeDetector(), new QosManager.EndPoint2EndPoint(null, 8000, "192.168.1.1", 9000, Protocol.UDP),
//            10, tools, mockCallback);
//        opener.createRequester(qosParam, "imsi");
//    }

    private static class MockCallback implements QosHelper.Callback {

        QosManager.Action action;
        QosManager.CallbackParam param;

        private final ConditionVariable flag = new ConditionVariable();

        @Override
        public void onQosResult(QosManager.Action action, QosManager.CallbackParam param) {
            this.action = action;
            this.param = param;
            flag.open();
        }

        public void waitForCall() {
            flag.block();
        }
    }

    private static class MockQosHelperTools implements QosHelper.Tools {

        private final MockNetTypeDetector netTypeDetector = new MockNetTypeDetector();

        @Override
        public boolean isValidLocalIp(byte[] ip) {
            return false;
        }

        @Override
        public AppType getAppType() {
            return AppType.UNKNOWN_APPTYPE;
        }

        @Override
        public String getChannel() {
            return null;
        }

        @Override
        public String getVersionNum() {
            return null;
        }

        @Override
        public String getSubaoId() {
            return null;
        }

        @Override
        public NetTypeDetector getNetTypeDetector() {
            return this.netTypeDetector;
        }

        @Override
        public String getIMSI() {
            return null;
        }

        public void setCurrentNetworkType(NetType netType) {
            this.netTypeDetector.setCurrentNetworkType(netType);
        }

    }

    @Implements(value = QosManager.WorkerFactory.class, isInAndroidSdk = false)
    public static class ShadowWorkFactory {
        @Implementation
        public static QosManager.Worker create(String serverHost, int serverPort, QosManager.Requester requester, QosManager.Callback callback) {
            return new Worker_CallbackWorkThread(serverHost, serverPort, requester, callback);
        }

        private static class Worker_CallbackWorkThread extends QosManager.Worker {

            Worker_CallbackWorkThread(String serverHost, int serverPort, QosManager.Requester requester, QosManager.Callback callback) {
                super(serverHost, serverPort, requester, callback);
            }

            @Override
            protected QosManager.CallbackParam doInBackground(Void... params) {
                QosManager.CallbackParam callbackParam = executeInBackground();
                notifyCallback(callbackParam);
                return callbackParam;
            }

            @Override
            protected void onPostExecute(QosManager.CallbackParam callbackParam) {
                // do nothing
            }
        }
    }

    @Implements(value = QosUser4GRegionAndISP.class)
    public static class ShadowQosUser4GRegionAndISP {
        @Implementation
        public QosParam getQosParam() {
            return new QosParam(100, 100, QosParam.Provider.DEFAULT, 10, 10);
        }
    }
}
