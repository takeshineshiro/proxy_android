package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.data.Address;
import com.subao.common.data.AppType;
import com.subao.common.net.Http;
import com.subao.common.net.Protocol;
import com.subao.common.qos.QosManager.Action;
import com.subao.common.qos.QosManager.CallbackParam;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QosManagerTest extends RoboBase {

    private static final String SERVERHOST = Address.HostName.TEST_SERVER;
    private static final int PORT = 501;
    private final TestCallback testCallback = new TestCallback();
    private Http.Response response;
    private QosManager.Key key;

    private static QosManager.Key createKey() {
        return new QosManager.Key(1, "192.168.1.232", "token");
    }

    private static Http.Response createResponse(int code) {
        Http.Response response = null;
        StringWriter stringWriter = new StringWriter(1024);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            jsonWriter.beginObject();
            jsonWriter.name("resultCode").value(code);
            jsonWriter.name("other").value("value");
            jsonWriter.name("errorInfo").value("");
            jsonWriter.endObject();
            jsonWriter.flush();
            response = new Http.Response(code, stringWriter.toString().getBytes());
        } catch (IOException e) {
        } finally {
            Misc.close(jsonWriter);
        }
        return response;
    }

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
    public void testAction() {
        assertEquals(Action.values().length, 3);
        assertEquals(Action.OPEN.ordinal(), 0);
        assertEquals(Action.CLOSE.ordinal(), 1);
        assertEquals(Action.MODIFY.ordinal(), 2);
        assertEquals("OPEN", Action.OPEN.getDesc());
        assertEquals("CLOSE", Action.CLOSE.getDesc());
        assertEquals("MODIFY", Action.MODIFY.getDesc());
    }

    @Test
    public void testEndPoint2EndPoint() {
        QosManager.EndPoint2EndPoint e2e = new QosManager.EndPoint2EndPoint(
            "192.168.1.241", 1006,
            "192.168.1.222", 201,
            Protocol.UDP);
        String str = e2e.toString();
        assertNotNull(str);
    }

    @Test
    public void testKey() {
        QosManager.Key key = new QosManager.Key(1, "192.168.1.232", "token");
        assertNotNull(key);

        QosManager.Key key1 = new QosManager.Key(1, "192.168.1.232", "token");
        QosManager.Key key2 = new QosManager.Key(2, "192.168.1.232", "token");
        QosManager.Key key3 = new QosManager.Key(3, "192.168.1.256", "token");
        QosManager.Key key4 = new QosManager.Key(3, "192.168.1.256", "stoken");
        QosManager.Key key5 = new QosManager.Key(1, "192.168.1.232", null);
        QosManager.Key key6 = new QosManager.Key(1, null, "token");

        assertTrue(key.equals(key));
        assertTrue(key.equals(key1));
        assertFalse(key.equals(key2));
        assertFalse(key.equals(key3));
        assertFalse(key.equals(key4));
        assertFalse(key.equals(key5));
        assertFalse(key5.equals(key));
        assertFalse(key.equals(key6));
        assertFalse(key6.equals(key));
        assertFalse(key.equals(null));

        String str = "avc";
        assertFalse(key.equals(str));
    }

    @Test
    public void testRequesterOpen() {
        QosManager.Key key = createKey();
        QosTerminalInfo terminalInfo = new QosTerminalInfo("192.168.1.241", 8765, "192.168.1.222", "imsi", "msisdn");
        QosMediaInfo mediaInfo = new QosMediaInfo("192.168.1.241", 8467, "192.168.1.243", 245, "UDP");
        QosSetupRequest qosSetup = new QosSetupRequest(
            AppType.ANDROID_APP,
            "g_official", "2.2.4", "userId",
            1000,
            terminalInfo,
            mediaInfo);
        QosParam qosParam = new QosParam(1, 2, QosParam.Provider.IVTIME, 0, 0);
        QosManager.Requester_Open requester = new QosManager.Requester_Open(key, qosParam, qosSetup);
        assertEquals(Action.OPEN, requester.getAction());

        try {
            requester.prepare();
            String ss = requester.buildRequestParam();
            assertNotNull(ss);

            String srt = requester.toString();
            assertNotNull(srt);

            response = createResponse(HttpURLConnection.HTTP_CREATED);

            assertNotNull(response);
            CallbackParam callback = requester.buildCallbackParam(response);
            assertNotNull(callback.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

        Http.Method method = requester.getHttpMethod();
        assertEquals(Http.Method.POST, method);

        sendRequest(requester);
    }

    @Test
    public void testRequesterModify() {
        if (key == null) {
            key = createKey();
        }

        assertNotNull(key);
        QosManager.Requester_Modify requester = new QosManager.Requester_Modify(key, "sessionId", 1000);

        assertNotNull(requester);
        assertEquals(Action.MODIFY, requester.getAction());
        assertEquals(Http.Method.PUT, requester.getHttpMethod());

        assertNotNull(requester.getUrlPath());

        String par;
        try {
            par = requester.buildRequestParam();
            assertNotNull(par);

            requester.prepare();


            response = createResponse(HttpURLConnection.HTTP_OK);


            assertNotNull(response);
            CallbackParam callback = requester.buildCallbackParam(response);
            assertNotNull(callback);

        } catch (IOException e) {
            e.printStackTrace();
        }

        sendRequest(requester);
    }

    @Test
    public void testRequesterClose() {
        if (key == null) {
            key = createKey();
        }

        assertNotNull(key);

        QosManager.Requester_Close requester = new QosManager.Requester_Close(key, "sessionId");
        assertNotNull(requester);

        assertEquals(Action.CLOSE, requester.getAction());
        assertEquals(Http.Method.DELETE, requester.getHttpMethod());

        String urlPath = requester.getUrlPath();
        assertNotNull(urlPath);

        response = createResponse(204);
        CallbackParam callback = requester.buildCallbackParam(response);
        sendRequest(requester);
    }


    @Test
    public void testPhoneNumberAndPrivateIp() {
        String[] phoneNumbers = new String[]{
            null, "1357", "",
        };
        String[] privateIpList = new String[]{
            null, "1.2.3.4", "",
        };
        for (int i = phoneNumbers.length - 1; i >= 0; --i) {
            String phoneNumber = phoneNumbers[i];
            for (int j = privateIpList.length - 1; j >= 0; --j) {
                String privateIp = privateIpList[j];
                QosManager.PhoneNumberAndPrivateIp target = new QosManager.PhoneNumberAndPrivateIp(phoneNumber, privateIp);
                assertTrue(target.toString().length() > 0);
                assertEquals(target, target);
                assertFalse(target.equals(null));
                assertFalse(target.equals(this));
                QosManager.PhoneNumberAndPrivateIp t2 = new QosManager.PhoneNumberAndPrivateIp("a", "b");
                assertFalse(target.equals(t2));
                QosManager.PhoneNumberAndPrivateIp t3 = new QosManager.PhoneNumberAndPrivateIp(phoneNumber, privateIp);
                assertEquals(target, t3);
            }
        }
    }

    @Test
    public void testSetAuthParam() throws MalformedURLException {
        MockConnection mock = new MockConnection();
        QosManager.Worker.setAuthParam(mock);
        assertEquals(mock.properties.get("Authorization"), "WSSE profile=\"UsernameToken\"");
        assertNotNull(mock.properties.get("X-WSSE"));
    }

    private void sendRequest(QosManager.Requester requester) {
        QosManager manager = QosManager.getInstance();
        assertNotNull(manager);
        manager.sendRequest(SERVERHOST, PORT, requester, testCallback);
    }

    private static abstract class Foo implements JsonSerializable {

        public abstract String getExpectedString();

    }

    private static class FooArray extends Foo {

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginArray();
            writer.value(1);
            writer.value(2);
            writer.endArray();
        }

        @Override
        public String getExpectedString() {
            return "[1,2]";
        }
    }

    private static class TestCallback implements QosManager.Callback {

        @Override
        public void onQosResult(Action action, int managerPort, CallbackParam param) {
            assertNotNull(action);
            assertNotNull(param);
        }

    }

    private static class MockConnection extends HttpURLConnection {

        public final Map<String, String> properties = new HashMap<String, String>();

        MockConnection() throws MalformedURLException {
            super(new URL("http://127.0.0.1"));
        }

        @Override
        public void disconnect() {

        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {

        }

        @Override
        public void setRequestProperty(String field, String newValue) {
            properties.put(field, newValue);
        }
    }

}