package com.subao.common.qos;

import android.text.TextUtils;
import android.util.JsonWriter;

import com.subao.common.ErrorCode;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.MockWebServerDispatcher;
import com.subao.common.RoboBase;
import com.subao.common.net.ShadowHttpForRuntimeException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.StringWriter;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * PhoneNumberGetterTest
 * <p>Created by YinHaiBo on 2017/2/9.</p>
 */
public class PhoneNumberGetterTest extends RoboBase {

    private MockWebServer mockWebServer;
    private MyDispatcher myDispatcher;

    private static String buildJson(String phoneNumber, String privateIp, boolean nullValueExists) throws IOException {
        StringWriter stringWriter = new StringWriter(128);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        jsonWriter.beginObject();
        jsonWriter.name("salt").value("test");
        if (phoneNumber == null) {
            if (nullValueExists) {
                jsonWriter.name("result").nullValue();
            }
        } else {
            jsonWriter.name("result").value(phoneNumber);
        }
        if (privateIp == null) {
            if (nullValueExists) {
                jsonWriter.name("privateip").nullValue();
            }
        } else {
            jsonWriter.name("privateip").value(privateIp);
        }
        jsonWriter.endObject();
        Misc.close(jsonWriter);
        return stringWriter.toString();
    }

    @Before
    public void setUp() throws IOException {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        mockWebServer = new MockWebServer();
        myDispatcher = new MyDispatcher();
        mockWebServer.setDispatcher(myDispatcher);
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        Logger.setLoggableChecker(null);
        mockWebServer.shutdown();
    }

    @Test
    public void doesRequired() throws Exception {
        assertFalse(QosManager.PhoneNumberGetter.doesRequired(null));
        assertFalse(QosManager.PhoneNumberGetter.doesRequired(QosParam.DEFAULT));
        assertTrue(QosManager.PhoneNumberGetter.doesRequired(new QosParam(1, 2, QosParam.Provider.ZTE, 0, 0)));
    }

    @Test
    public void execute() throws Exception {
        new QosManager.PhoneNumberGetter();
        String[] phoneNumbers = new String[]{
            null, "1357", "",
        };
        String[] privateIpList = new String[]{
            null, "1.2.3.4", "",
        };
        for (int n = 0; n < 2; ++n) {
            for (int i = phoneNumbers.length - 1; i >= 0; --i) {
                String phoneNumber = phoneNumbers[i];
                for (int j = privateIpList.length - 1; j >= 0; --j) {
                    String privateIp = privateIpList[j];
                    String responseBody = buildJson(phoneNumber, privateIp, n == 0);
                    myDispatcher.setResponseBody(responseBody);
                    QosManager.PhoneNumberAndPrivateIp result =
                        QosManager.PhoneNumberGetter.execute(QosManager.Action.OPEN, null, mockWebServer.getHostName(), mockWebServer.getPort());
                    if (TextUtils.isEmpty(phoneNumber)) {
                        assertEquals(result.error, ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR);
                        assertEquals(responseBody, result.event.getParamValue("raw"));
                        assertEquals(QosManager.Action.OPEN.getDesc(), result.event.getParamValue("action"));
                        assertEquals(Integer.toString(result.error), result.event.getParamValue("error"));
                    } else {
                        assertEquals(ErrorCode.OK, result.error);
                        assertEquals(phoneNumber, result.phoneNumber);
                        assertEquals(privateIp, result.privateIp);
                        assertNull(result.event);
                    }
                }
            }
        }
    }

    @Test
    public void executeWhenInvalidResponse() throws IOException {
        QosManager.Action action = QosManager.Action.OPEN;
        QosManager.PhoneNumberAndPrivateIp result =
            QosManager.PhoneNumberGetter.execute(action, null, mockWebServer.getHostName(), mockWebServer.getPort());
        assertEquals(result.error, ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR);
        assertEquals(action.getDesc(), result.event.getParamValue("action"));
        //
        // IOException
        myDispatcher.setResponseBody("{");
        myDispatcher.setResponseCode(200);
        result = QosManager.PhoneNumberGetter.execute(action, null, mockWebServer.getHostName(), mockWebServer.getPort());
        assertEquals(result.error, ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR);
        assertEquals(action.getDesc(), result.event.getParamValue("action"));
        //
        // RuntimeException
        action = QosManager.Action.MODIFY;
        myDispatcher.setResponseBody("{\"result\":[]}");
        myDispatcher.setResponseCode(200);
        result = QosManager.PhoneNumberGetter.execute(action, null, mockWebServer.getHostName(), mockWebServer.getPort());
        assertEquals(result.error, ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR);
        assertEquals(action.getDesc(), result.event.getParamValue("action"));

    }

    @Test
    public void executeWhenServerError() throws IOException {
        myDispatcher.setResponseCode(500);
        QosManager.PhoneNumberAndPrivateIp result =
            QosManager.PhoneNumberGetter.execute(QosManager.Action.OPEN, null, mockWebServer.getHostName(), mockWebServer.getPort());
        assertEquals(result.error, ErrorCode.QOS_THIRD_PROVIDER_BASE + 500);
    }

    @Test
    public void executeWhenIOException() throws IOException {
        mockWebServer.shutdown();
        QosManager.PhoneNumberAndPrivateIp result =
            QosManager.PhoneNumberGetter.execute(QosManager.Action.OPEN, null, mockWebServer.getHostName(), mockWebServer.getPort());
        assertEquals(result.error, ErrorCode.QOS_THIRD_PROVIDER_IO_ERROR);
    }

    @Test
    @Config(shadows = ShadowHttpForRuntimeException.class)
    public void executeWhenRuntimeException() throws IOException {
        QosManager.PhoneNumberAndPrivateIp result =
            QosManager.PhoneNumberGetter.execute(QosManager.Action.OPEN, null, mockWebServer.getHostName(), mockWebServer.getPort());
        assertEquals(result.error, ErrorCode.QOS_THIRD_PROVIDER_IO_ERROR);
    }

    private static class MyDispatcher extends MockWebServerDispatcher {

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            if (!"bdproxy/?appid=xunyou".equals(request.getPath())) {
                return new MockResponse().setResponseCode(404);
            }
            return super.dispatch(request);
        }
    }

}