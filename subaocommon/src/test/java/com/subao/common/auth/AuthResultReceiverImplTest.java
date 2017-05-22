package com.subao.common.auth;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.data.Defines;
import com.subao.common.data.HRDataTrans;
import com.subao.common.data.ServiceLocation;
import com.subao.common.jni.JniWrapper;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.net.IPInfoQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * AuthResultReceiverImplTest
 * <p>Created by YinHaiBo on 2017/3/22.</p>
 */
public class AuthResultReceiverImplTest extends RoboBase {

    private static final int CID = 8;
    private static final String JWT_TOKEN = "this is jwt token";
    private static final int EXPIRES = 123;
    private static final String SHORT_ID = "this is short id";
    private static final String EXPIRED_TIME = "2017-1-1";
    private static final String USER_ID = "USER_ID";

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
    @Config(shadows = ShadowIPInfoQuery.class)
    public void onGetJWTTokenResult() throws Exception {
        ServiceLocation serviceLocation = new ServiceLocation(
            null, "127.0.0.1", 1
        );
        for (int status = 0; status <= 7; ++status) {
            JniWrapper jniWrapper = mock(JniWrapper.class);
            AuthResultReceiverImpl target = new AuthResultReceiverImpl(jniWrapper, null, serviceLocation);
            // 断言一定会用正确的参数调用JniWrapper
            final int finalStatus = status;
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    assertEquals(CID, invocation.getArgument(0));
                    assertEquals(true, invocation.getArgument(1));
                    assertEquals(0, invocation.getArgument(2));
                    assertEquals(JWT_TOKEN, invocation.getArgument(3));
                    assertEquals(EXPIRES, invocation.getArgument(4));
                    assertEquals(SHORT_ID, invocation.getArgument(5));
                    assertEquals(finalStatus, invocation.getArgument(6));
                    assertEquals(EXPIRED_TIME, invocation.getArgument(7));
                    return null;
                }
            }).when(jniWrapper).userAuthResult(
                anyInt(), anyBoolean(), anyInt(),
                anyString(), anyInt(), anyString(), anyInt(), anyString());
            ShadowIPInfoQuery.arguments = null;
            target.onGetJWTTokenResult(CID, JWT_TOKEN, EXPIRES, SHORT_ID, status, EXPIRED_TIME, true, 0);
            // 断言一定会通知IPInfoQuery
            boolean isVIP = (status == 2 || status == 4 || status == 6);
            assertEquals(isVIP, ShadowIPInfoQuery.arguments[0]);
            assertEquals(serviceLocation, ShadowIPInfoQuery.arguments[1]);
        }
    }

    @Test
    @Config(shadows = ShadowIPInfoQuery.class)
    public void onGetUserAccelStatusResult() throws Exception {
        ServiceLocation serviceLocation = new ServiceLocation(
            null, "127.0.0.1", 1
        );
        for (int status = 0; status <= 7; ++status) {
            JniWrapper jniWrapper = mock(JniWrapper.class);
            AuthResultReceiverImpl target = new AuthResultReceiverImpl(jniWrapper, null, serviceLocation);
            // 断言一定会用正确的参数调用JniWrapper
            final int finalStatus = status;
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    assertEquals(CID, invocation.getArgument(0));
                    assertEquals(true, invocation.getArgument(1));
                    assertEquals(0, invocation.getArgument(2));
                    assertEquals(finalStatus, invocation.getArgument(3));
                    assertEquals(SHORT_ID, invocation.getArgument(4));
                    assertEquals(EXPIRED_TIME, invocation.getArgument(5));
                    return null;
                }
            }).when(jniWrapper).userStateResult(
                anyInt(), anyBoolean(), anyInt(), anyInt(), anyString(), anyString());
            ShadowIPInfoQuery.arguments = null;
            target.onGetUserAccelStatusResult(CID, SHORT_ID, status, EXPIRED_TIME, true, 0);
            // 断言一定会通知IPInfoQuery
            boolean isVIP = (status == 2 || status == 4 || status == 6);
            assertEquals(isVIP, ShadowIPInfoQuery.arguments[0]);
            assertEquals(serviceLocation, ShadowIPInfoQuery.arguments[1]);
        }
    }

    @Test
    public void onGetTokenResult() throws Exception {
        JniWrapper jniWrapper = mock(JniWrapper.class);
        AuthResultReceiverImpl target = new AuthResultReceiverImpl(jniWrapper, null, null);
        final String ip = "1.2.3.4";
        final byte[] token = "the token".getBytes();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(CID, invocation.getArgument(0));
                assertEquals(true, invocation.getArgument(1));
                assertEquals(0, invocation.getArgument(2));
                assertEquals(ip, invocation.getArgument(3));
                assertArrayEquals(token, (byte[]) invocation.getArgument(4));
                assertEquals(EXPIRES, invocation.getArgument(5));
                return null;
            }
        }).when(jniWrapper).linkAuthResult(anyInt(), anyBoolean(), anyInt(), anyString(), any(byte[].class), anyInt());
        target.onGetTokenResult(CID, ip, token, 123, EXPIRES, true, 0);
    }

    @Test
    @Config(shadows = ShadowDownloader.class)
    public void onGetUserConfigResult() throws Exception {
        JniWrapper jniWrapper = mock(JniWrapper.class);
        HRDataTrans.Arguments hrDataTransArguments = new HRDataTrans.Arguments(
            Defines.REQUEST_CLIENT_TYPE_FOR_APP, "2.0.0.1",
            new ServiceLocation(null, "127.0.0.1", 2),
            new MockNetTypeDetector()
        );
        ServiceLocation serviceLocation = new ServiceLocation(null, "127.0.0.1", 1);
        AuthResultReceiverImpl target = new AuthResultReceiverImpl(jniWrapper, hrDataTransArguments, serviceLocation);
        final String cfg = "110";
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(CID, invocation.getArgument(0));
                assertEquals(true, invocation.getArgument(1));
                assertEquals(0, invocation.getArgument(2));
                assertEquals(cfg, invocation.getArgument(3));
                return null;
            }
        }).when(jniWrapper).userConfigResult(anyInt(), anyBoolean(), anyInt(), anyString());
        AuthExecutor.Configs configs = new AuthExecutor.Configs(cfg, "server", "script-id");
        target.onGetUserConfigResult(CID, JWT_TOKEN, USER_ID, configs, 0, true);
        assertEquals(4, ShadowDownloader.arguments.length);
        assertEquals(hrDataTransArguments, ShadowDownloader.arguments[0]);
        assertEquals(jniWrapper, ShadowDownloader.arguments[1]);
        assertEquals(JWT_TOKEN, ShadowDownloader.arguments[2]);
        assertEquals(USER_ID, ShadowDownloader.arguments[3]);
    }

    @Test
    public void testScriptDownload() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        try {
            HRDataTrans.Arguments arguments = new HRDataTrans.Arguments(
                Defines.REQUEST_CLIENT_TYPE_FOR_APP, "2.0.0",
                new ServiceLocation(null, mockWebServer.getHostName(), mockWebServer.getPort()),
                new MockNetTypeDetector()
            );
            JniWrapper jniWrapper = mock(JniWrapper.class);
            //
            MockResponse mockResponse = new MockResponse();
            mockResponse.setResponseCode(200).setBody("test");
            mockWebServer.enqueue(mockResponse);
            AuthResultReceiverImpl.ScriptDownloader.start(arguments, jniWrapper, JWT_TOKEN, USER_ID);
            sleep();
        } finally {
            mockWebServer.shutdown();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Implements(value = IPInfoQuery.class, isInAndroidSdk = false)
    public static class ShadowIPInfoQuery {

        static Object[] arguments;

        @Implementation
        public static void onUserAuthComplete(boolean isVIP, ServiceLocation serviceLocation) {
            arguments = new Object[] { isVIP, serviceLocation };
        }
    }

    @Implements(value = AuthResultReceiverImpl.ScriptDownloader.class, isInAndroidSdk = false)
    public static class ShadowDownloader {

        static Object[] arguments;

        @Implementation
        public static boolean start(HRDataTrans.Arguments arguments, JniWrapper jniWrapper, String jwtToken, String userId) {
            ShadowDownloader.arguments = new Object[]{
                arguments, jniWrapper, jwtToken, userId
            };
            return false;
        }
    }
}