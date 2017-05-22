package com.subao.common.qos;

import android.text.TextUtils;

import com.subao.common.ErrorCode;
import com.subao.common.Logger;
import com.subao.common.MockWebServerDispatcher;
import com.subao.common.RoboBase;
import com.subao.common.net.ShadowHttpForRuntimeException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * SecurityTokenGetterTest
 * <p>Created by YinHaiBo on 2017/2/10.</p>
 */
public class SecurityTokenGetterTest extends RoboBase {

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
    }

    @After
    public void tearDown() {
        Logger.setLoggableChecker(null);
    }

    @Test
    public void testResultOk() {
        String token = "the token";
        QosManager.SecurityTokenGetter.Result result = new QosManager.SecurityTokenGetter.Result(token);
        assertEquals(0, result.error);
        assertNull(result.event);
        assertTrue(result.equals(result));
        assertFalse(result.equals(null));
        assertFalse(result.equals(this));
        assertTrue(result.equals(new QosManager.SecurityTokenGetter.Result(token)));
    }

    @Test
    public void testResultError() {
        QosManager.Action action = QosManager.Action.OPEN;
        assertNull(QosManager.SecurityTokenGetter.Result.createByException(action, 0, null, new RuntimeException()).event);
        int error = 10086;
        byte[] rawData = "Hello".getBytes();
        QosSetupRequest qosSetupRequest = null;
        QosManager.SecurityTokenGetter.Result r1 = QosManager.SecurityTokenGetter.Result.createByRawData(action, error, qosSetupRequest, rawData);
        assertEquals(r1, r1);
        assertFalse(r1.equals(null));
        assertFalse(r1.equals(this));
        assertFalse(r1.equals(QosManager.SecurityTokenGetter.Result.createByRawData(action, error + 1, qosSetupRequest, rawData)));
        assertFalse(r1.equals(QosManager.SecurityTokenGetter.Result.createByRawData(action, error, qosSetupRequest, "world".getBytes())));
        assertNotNull(QosManager.SecurityTokenGetter.Result.createByRawData(action, error, qosSetupRequest, rawData).toString());
    }

    @Test
    public void isTokenRequired() {
        assertFalse(QosManager.SecurityTokenGetter.isTokenRequired(null));
        for (QosParam.Provider provider : QosParam.Provider.values()) {
            QosParam qosParam = new QosParam(1, 2, provider, 7, 8);
            boolean expected = provider == QosParam.Provider.IVTIME;
            assertEquals(expected, QosManager.SecurityTokenGetter.isTokenRequired(qosParam));
        }
    }

    @Test
    public void constructor() {
        new QosManager.SecurityTokenGetter();
    }

    @Test
    public void appKey() {
        assertEquals("6ed68a7c-7ac3-4156-be61-a3cfbdab9c89", QosManager.SecurityTokenGetter.APP_KEY);
    }

    @Test
    public void execute() throws IOException {
        QosManager.Action action = QosManager.Action.OPEN;
        QosSetupRequest qosSetupRequest = null;
        MockWebServer mockWebServer = new MockWebServer();
        MyDispatcher dispatcher = new MyDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        mockWebServer.start();
        try {
            // 空Response
            dispatcher.setResponseBody(null);
            String host = mockWebServer.getHostName();
            int port = mockWebServer.getPort();
            QosManager.SecurityTokenGetter.Result result = QosManager.SecurityTokenGetter.execute(action, null, host, port);
            assertEquals(ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR, result.error);
            assertEquals("", result.event.getParamValue("raw"));
            assertNull(result.token);
            //
            // 非法Response，解析失败
            dispatcher.setResponseBody("{[]");
            result = QosManager.SecurityTokenGetter.execute(action, null, host, port);
            assertEquals(ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR, result.error);
            assertEquals("{[]", result.event.getParamValue("raw"));
            assertNull(result.token);
            //
            // 正常情况
            dispatcher.setResponseCode(200);
            dispatcher.setResponseBody("{\"hello\":null, \"result\":\"a dog\"}");
            result = QosManager.SecurityTokenGetter.execute(action, qosSetupRequest, host, port);
            assertEquals(ErrorCode.OK, result.error);
            assertEquals("a dog", result.token);
            assertNull(result.event);
            //
            // 返回的Json里有error字段
            dispatcher.setResponseCode(200);
            String json = "{\"hello\":null, \"result\":\"hahaha\", \"error\":\"any\"}";
            dispatcher.setResponseBody(json);
            result = QosManager.SecurityTokenGetter.execute(action, qosSetupRequest, host, port);
            assertEquals(ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_ERROR_CODE, result.error);
            assertEquals(json, result.event.getParamValue("raw"));
            assertNull(result.token);
            assertEquals(action.getDesc(), result.event.getParamValue("action"));
            //
            // 返回的Json里有error字段，但error为null, result为空串
            dispatcher.setResponseCode(200);
            json = "{\"hello\":null, \"result\":\"\", \"error\":null}";
            dispatcher.setResponseBody(json);
            result = QosManager.SecurityTokenGetter.execute(action, qosSetupRequest, host, port);
            assertEquals(ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR, result.error);
            assertEquals(json, result.event.getParamValue("raw"));
            assertNull(result.token);
            assertEquals(action.getDesc(), result.event.getParamValue("action"));
            //
            // 返回的Json里有error字段，但error为null
            dispatcher.setResponseCode(200);
            dispatcher.setResponseBody("{\"hello\":null, \"result\":\"a dog\", \"error\":null}");
            result = QosManager.SecurityTokenGetter.execute(action, qosSetupRequest, host, port);
            assertNull(result.event);
            assertEquals(ErrorCode.OK, result.error);
            assertEquals("a dog", result.token);
            //
            // HTTP Response Code 不是200
            dispatcher.setResponseCode(500);
            dispatcher.setResponseBody(null);
            result = QosManager.SecurityTokenGetter.execute(action, qosSetupRequest, host, port);
            assertTrue(TextUtils.isEmpty(result.event.getParamValue("raw")));
            assertEquals(ErrorCode.QOS_THIRD_PROVIDER_BASE + 500, result.error);
            assertEquals(action.getDesc(), result.event.getParamValue("action"));
            //
            // Response Json 内容无效引发的IOException
            dispatcher.setResponseCode(200);
            json = "{";
            dispatcher.setResponseBody(json);
            result = QosManager.SecurityTokenGetter.execute(action, qosSetupRequest, host, port);
            assertEquals(ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR, result.error);
            assertEquals(json, result.event.getParamValue("raw"));
            assertNull(result.token);
            assertEquals(action.getDesc(), result.event.getParamValue("action"));
            //
            // Response Json 内容无效引发的RuntimeException
            dispatcher.setResponseCode(200);
            json = "{\"result\":[]}";
            dispatcher.setResponseBody(json);
            result = QosManager.SecurityTokenGetter.execute(action, qosSetupRequest, host, port);
            assertEquals(ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR, result.error);
            assertEquals(json, result.event.getParamValue("raw"));
            assertNull(result.token);
            assertEquals(action.getDesc(), result.event.getParamValue("action"));
        } finally {
            mockWebServer.shutdown();
        }
    }

    @Test
    public void executeWhenHttpIOException() throws IOException {
        QosManager.SecurityTokenGetter.Result result = QosManager.SecurityTokenGetter.execute(QosManager.Action.OPEN, null, "localhost", 1);
        assertEquals(ErrorCode.QOS_THIRD_PROVIDER_IO_ERROR, result.error);
        assertNull(result.token);
    }

    @Test
    @Config(shadows = ShadowHttpForRuntimeException.class)
    public void executeWhenHttpRuntimeException() throws IOException {
        QosManager.SecurityTokenGetter.Result result = QosManager.SecurityTokenGetter.execute(QosManager.Action.OPEN, null, "localhost", 1);
        assertEquals(ErrorCode.QOS_THIRD_PROVIDER_IO_RUNTIME_EXCEPTION, result.error);
        assertNull(result.token);
    }

    private static class MyDispatcher extends MockWebServerDispatcher {

        private final static String EXPECTED_PATH = "t1?appid=" + QosManager.SecurityTokenGetter.APP_KEY;

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            if (!EXPECTED_PATH.equals(request.getPath())) {
                return new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED);
            }
            return super.dispatch(request);
        }
    }
}