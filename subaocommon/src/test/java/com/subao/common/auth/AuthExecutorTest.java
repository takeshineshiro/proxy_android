package com.subao.common.auth;

import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.data.ServiceConfigForTest;
import com.subao.common.io.Persistent;
import com.subao.common.msg.MessageEvent;
import com.subao.common.net.ResponseCallback;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * Created by YHB on 2016/8/5.
 */
public class AuthExecutorTest extends RoboBase {

    private static class MockReporter implements MessageEvent.Reporter {

        public String eventName, eventParam;

        @Override
        public void reportEvent(String eventName, String eventParam) {
            this.eventName = eventName;
            this.eventParam = eventParam;
        }
    }

    private static class MockController implements AuthExecutor.Controller {

        public final MockReporter mockReporter = new MockReporter();

        public boolean netConnected;

        @Override
        public boolean isNetConnected() {
            return netConnected;
        }

        @Override
        public MessageEvent.Reporter getEventReporter() {
            return this.mockReporter;
        }

        @Override
        public String getClientVersion() {
            return "client_version";
        }
    }

    private static class MockResponseCallback extends ResponseCallback {

        public int codeOnSuccess;
        public byte[] responseOnSuccess;
        public int codeOnFail;

        public boolean onSuccessCall;
        public boolean onFailCall;

        public final String eventName = "test_event_name";

        public MockResponseCallback(MessageEvent.Reporter eventReporter) {
            super(eventReporter, Integer.valueOf(1));
        }

        @Override
        protected String getEventName() {
            return eventName;
        }

        @Override
        protected void onSuccess(int code, byte[] response) {
            onSuccessCall = true;
            codeOnSuccess = code;
            responseOnSuccess = response;
        }

        @Override
        protected void onFail(int code, byte[] responseData) {
            onFailCall = true;
            codeOnFail = code;
        }
    }

    private static class MockAuthResultReceiver implements AuthResultReceiver {

        @Override
        public void onGetJWTTokenResult(int cid, String jwtToken, long expires, String shortId, int userStatus, String expiredTime, boolean result, int code) {

        }

        @Override
        public void onGetUserAccelStatusResult(int cid, String shortId, int status, String expiredTime, boolean result, int code) {

        }

        @Override
        public void onGetTokenResult(int cid, String ip, byte[] token, int length, int expires, boolean result, int code) {

        }

        @Override
        public void onGetUserConfigResult(int cid, String jwtToken, String userId, AuthExecutor.Configs configs, int code, boolean result) {

        }

    }

    private static class MockPersistent implements Persistent {

        private ByteBuffer buffer;

        public boolean readError, writeError;

        @Override
        public boolean exists() {
            return buffer != null;
        }

        @Override
        public InputStream openInput() throws IOException {
            if (!exists()) {
                throw new FileNotFoundException();
            }
            buffer.position(0);
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    if (readError) {
                        throw new IOException();
                    }
                    try {
                        return buffer.get();
                    } catch (RuntimeException e) {
                        throw new IOException();
                    }
                }
            };
        }

        @Override
        public OutputStream openOutput() throws IOException {
            buffer = ByteBuffer.allocate(4096);
            return new OutputStream() {
                @Override
                public void write(int oneByte) throws IOException {
                    if (writeError) {
                        throw new IOException();
                    }
                    try {
                        buffer.put((byte) oneByte);
                    } catch (RuntimeException e) {
                        throw new IOException();
                    }
                }

                @Override
                public void close() throws IOException {
                    flush();
                }

                @Override
                public void flush() throws IOException {
                    buffer.limit(buffer.position());
                }
            };
        }

        @Override
        public boolean delete() {
            if (buffer == null) {
                return false;
            } else {
                buffer = null;
                return true;
            }
        }

        @Override
        public Persistent createChild(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] read() throws IOException {
            InputStream inputStream = null;
            ByteArrayOutputStream outputStream = null;
            try {
                inputStream = this.openInput();
                outputStream = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                while (true) {
                    int size = inputStream.read(buffer);
                    if (size <= 0) {
                        break;
                    }
                    outputStream.write(buffer, 0, size);
                }
                return outputStream.toByteArray();
            } finally {
                Misc.close(inputStream);
                Misc.close(outputStream);
            }
        }
    }

    @Before
    public void setUp() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
    }

    @Test
    public void testPreCheck_NetDisconned() {
        MockReporter eventReporter = new MockReporter();
        MockResponseCallback callback = new MockResponseCallback(eventReporter);
        //
        assertFalse(AuthExecutor.preCheck(false, callback));
        assertFalse(callback.onSuccessCall);
        assertTrue(callback.onFailCall);
        assertEquals(-1, callback.codeOnFail);
        assertEquals(callback.eventName, eventReporter.eventName);
        assertEquals("net", eventReporter.eventParam);
    }

    @Test
    public void testPreCheck_NetConnected() {
        MockReporter eventReporter = new MockReporter();
        MockResponseCallback callback = new MockResponseCallback(eventReporter);
        //
        assertTrue(AuthExecutor.preCheck(true, callback));
        assertFalse(callback.onFailCall);
        assertFalse(callback.onSuccessCall);
        assertEquals(null, eventReporter.eventName);
        assertEquals(null, eventReporter.eventParam);
    }

    private static class MockBox {
        public MockAuthResultReceiver authResultReceiver;
        public MockController controller;
        public MockReporter reporter;

        public MockBox() {
            authResultReceiver = new MockAuthResultReceiver();
            controller = new MockController();
            reporter = controller.mockReporter;
        }
    }

    @Test
    public void testGetJWTToken() {
        MockBox mockBox = new MockBox();
        mockBox.controller.netConnected = false;
        AuthExecutor.getJWTToken(mockBox.controller, 1, "userid", "token", "appid", mockBox.authResultReceiver);
        assertEquals("auth_get_jwt_token", mockBox.reporter.eventName);
        assertEquals("net", mockBox.reporter.eventParam);
        //
        mockBox.controller.netConnected = true;
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        AuthExecutor.getJWTToken(mockBox.controller, 1, "userid", "token", "appid", mockBox.authResultReceiver);
    }

    @Test
    public void testGetUserConfig() {
        MockBox mockBox = new MockBox();
        AuthExecutor.getConfigs(mockBox.controller, 1, "token", "userId", mockBox.authResultReceiver);
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
    }

//    @Test
//    public void testMakeGetJWTToken() {
//        for (int code = 200; code < 300; ++code) {
//            assertEquals(AuthResultReceiver.GetJWTTokenResult.OK, AuthExecutor.makeGetJWTTokenResult(code, null));
//        }
//        assertEquals(AuthResultReceiver.GetJWTTokenResult.RETRY, AuthExecutor.makeGetJWTTokenResult(401, null));
//        byte[] responseData = "test".getBytes();
//        for (int code = 400; code < 405; ++code) {
//            assertEquals(AuthResultReceiver.GetJWTTokenResult.RETRY, AuthExecutor.makeGetJWTTokenResult(code, responseData));
//        }
//        //
//        responseData = "{\"hello\":\"world\",\"error\":\"black list\"}".getBytes();
//        for (int code = 400; code < 405; ++code) {
//            AuthResultReceiver.GetJWTTokenResult result = AuthExecutor.makeGetJWTTokenResult(code, responseData);
//            if (code == 401) {
//                assertEquals(AuthResultReceiver.GetJWTTokenResult.FAIL, result);
//            } else {
//                assertEquals(AuthResultReceiver.GetJWTTokenResult.RETRY, result);
//            }
//        }
//        //
//        responseData = "{\"error\":\"black list2\"}".getBytes();
//        for (int code = 400; code < 405; ++code) {
//            AuthResultReceiver.GetJWTTokenResult result = AuthExecutor.makeGetJWTTokenResult(code, responseData);
//            assertEquals(AuthResultReceiver.GetJWTTokenResult.RETRY, result);
//        }
//    }

    @Test
    public void testJWTTokenCacheEncode1() {
        for (int i = 0; i < 256; ++i) {
            for (int j = 0; j < 256; ++j) {
                byte a = (byte) i;
                byte b = (byte) j;
                byte c = (byte) (a ^ b);
                byte d = (byte) (c ^ b);
                byte e = (byte) (c ^ a);
                assertEquals(a, d);
                assertEquals(b, e);
            }
        }
    }

    @Test
    public void testJWTTokenCacheEncode2() {
        byte[] data = new byte[256];
        for (int i = -128; i <= 127; ++i) {
            data[i + 128] = (byte) i;
        }
        byte[] encoded = Arrays.copyOf(data, data.length);
        AuthExecutor.Cache.encode(encoded);
        AuthExecutor.Cache.encode(encoded);
        assertTrue(Arrays.equals(data, encoded));
    }

    @Test
    public void testCache() {
        MockPersistent mp = new MockPersistent();
        AuthExecutor.Cache cache = new AuthExecutor.Cache(mp);
        assertNull(cache.get(null));
        //
        String user = "John";
        assertNull(cache.get(user));
        //
        JWTTokenResp resp = JWTTokenRespTest.createJWTTokenResp();
        cache.set(user, resp);
        JWTTokenResp r2 = cache.get(user);
        assertNotNull(r2);
        assertTrue(resp.expiresIn - r2.expiresIn <= 1);
        //
        assertNull(cache.get(user, resp.expiresIn * 1000 + System.currentTimeMillis()));
        assertNull(cache.get(user));
        //
        cache.set(user, resp);
        cache.set(user, null);
        assertNull(cache.get(user));
    }

    @Test
    public void testCache2() {
        MockPersistent mp = new MockPersistent();
        AuthExecutor.Cache cache = new AuthExecutor.Cache(mp);
        String user = "Bill";
        JWTTokenResp resp = JWTTokenRespTest.createJWTTokenResp();
        cache.set(user, resp);
        //
        AuthExecutor.Cache cache2 = new AuthExecutor.Cache(mp);
        JWTTokenResp r2 = cache2.get(user);
        JWTTokenResp r3 = cache.get(user);
        assertEquals(r3, r2);
        //
        mp.readError = true;
        AuthExecutor.Cache cache3 = new AuthExecutor.Cache(mp);
        assertNull(cache3.get(user));
    }

    @Test
    public void testCache3() {
        MockPersistent mp = new MockPersistent();
        AuthExecutor.Cache cache = new AuthExecutor.Cache(mp);
        String user1 = "Bill";
        String user2 = "John";

        JWTTokenResp data1 = JWTTokenRespTest.createJWTTokenResp();
        cache.set(user1, data1);
        assertTrue(data1.same(cache.get(user1), 1));
        assertNull(cache.get(user2));
        //
        JWTTokenResp data2 = new JWTTokenResp("hello", 1234L, "world", 2, "who are you", 5, 13579L);
        cache.set(user2, data2);
        assertTrue(data2.same(cache.get(user2), 1));
        assertTrue(data1.same(cache.get(user1), 10));
        //
        assertFalse(data1.equals(data2));
    }

    @Test
    public void testCache_Persistent() {
        MockPersistent mp = new MockPersistent();
        AuthExecutor.Cache cache = new AuthExecutor.Cache(mp);
        for (int i = 0; i < 3; ++i) {
            String user = "user_" + i;
            cache.set(user, JWTTokenRespTest.createJWTTokenResp());
        }
        //
        AuthExecutor.Cache cache2 = new AuthExecutor.Cache(mp);
        assertTrue(cache.isDataEquals(cache2));
    }

    @Test
    public void testCacheRemoveToken() {
        MockPersistent mp = new MockPersistent();
        AuthExecutor.Cache cache = new AuthExecutor.Cache(mp);
        cache.removeJWTToken("foo");
        cache.removeJWTToken(null);
        cache.removeJWTToken("");
        for (int i = 0; i < 3; ++i) {
            String user = "user_" + i;
            cache.set(user, new JWTTokenResp("token_" + i, 1000L + i * 2, "sid", i, "time", i + 1, 157897L));
        }
        assertEquals(3, cache.size());
        cache.removeJWTToken("foo");
        assertEquals(3, cache.size());
        //
        for (int i = 0; i < 3; ++i) {
            String user = "user_" + i;
            JWTTokenResp token = cache.get(user);
            assertEquals("token_" + i, token.accelToken);
            cache.removeJWTToken(token.accelToken);
            assertNull(cache.get(user));
        }
    }

    @Test
    public void testCache_isDataEquals() {
        AuthExecutor.Cache c1 = new AuthExecutor.Cache(null);
        assertFalse(c1.isDataEquals(null));
        assertTrue(c1.isDataEquals(c1));
        //
        AuthExecutor.Cache c2 = new AuthExecutor.Cache(null);
        assertTrue(c1.isDataEquals(c2));
    }

    @Test
    public void testAuthErrorCounter() {
        String user_1 = "User 1";
        String user_2 = "User 2";
        assertEquals(AuthExecutor.AuthErrorCounter.UpdateResult.SUCCEED,
            AuthExecutor.AuthErrorCounter.update(user_1, 2));
        assertEquals(AuthExecutor.AuthErrorCounter.UpdateResult.SUCCEED,
            AuthExecutor.AuthErrorCounter.update(user_2, 2));
        assertEquals(AuthExecutor.AuthErrorCounter.UpdateResult.TOO_MANY_ERROR,
            AuthExecutor.AuthErrorCounter.update(user_1, 2));
        assertEquals(AuthExecutor.AuthErrorCounter.UpdateResult.SUCCEED,
            AuthExecutor.AuthErrorCounter.update(user_1, 2));
        assertEquals(AuthExecutor.AuthErrorCounter.UpdateResult.SUCCEED,
            AuthExecutor.AuthErrorCounter.update(user_1, 3));
        assertTrue(AuthExecutor.AuthErrorCounter.remove(user_1));
        assertTrue(AuthExecutor.AuthErrorCounter.remove(user_2));
        assertFalse(AuthExecutor.AuthErrorCounter.remove(user_1));
        assertFalse(AuthExecutor.AuthErrorCounter.remove(user_2));
    }

//    @Test
//    public void configsConstrutor() {
//        AuthExecutor.Configs configs = new AuthExecutor.Configs("120", "serverConfig", "scriptId", "scriptCheckSum");
//
//    }
}