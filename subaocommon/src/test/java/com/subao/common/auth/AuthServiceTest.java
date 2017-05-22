package com.subao.common.auth;

import com.subao.common.RoboBase;
import com.subao.common.data.Defines;
import com.subao.common.data.ServiceConfigForTest;
import com.subao.common.net.ResponseCallback;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by hujd on 16-7-21.
 */
public class AuthServiceTest extends RoboBase {

    private final ResponseCallback callback = new ResponseCallback(null, 1) {

        @Override
        protected void onSuccess(int code, byte[] response) {
            assertTrue(code >= 200 && code < 300);
        }

        @Override
        protected void onFail(int code, byte[] responseData) {
            assertFalse(code >= 200 && code < 300);
        }

        @Override
        protected String getEventName() {
            return null;
        }
    };
    private String openId = "";
    private String jwtToken = "";

    @Test
    public void testClientType() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        assertEquals(Defines.REQUEST_CLIENT_TYPE_FOR_APP, AuthService.getClientType());
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), "", null);
        assertEquals(Defines.REQUEST_CLIENT_TYPE_FOR_APP, AuthService.getClientType());
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), "1234", null);
        assertEquals("1234", AuthService.getClientType());
    }

    @Test(expected = NullPointerException.class)
    public void testGetTokenNULL() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        AuthService.getToken(null, null, null);
    }

    @Test
    public void testGetToken() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        AuthService.getToken("122.224.73.168", "11111", callback);
    }

    @Test(expected = NullPointerException.class)
    public void testGetJWTToken() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), "1234", null);
        AuthService.getJWTToken(new AuthService.JWTTokenParams("", "", AuthService.getClientType(), "", callback));
        AuthService.getJWTToken(null);
    }

    public void testJWTTokenParams() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), "1234", null);
        String appId = "";
        String token = "";
        new AuthService.JWTTokenParams(null, token, AuthService.getClientType(), appId, callback);
    }

    public void testGetUserAccelStatusNull() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        AuthService.getUserAccelStatus(null, jwtToken, callback);
    }

    @Test
    public void testGetUserAccelStatus() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        AuthService.getUserAccelStatus(openId, jwtToken, callback);
    }

    public void testGetWebpageUserConfigNull() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        AuthService.getConfig(null, null, "clientVersion", callback);
    }

    @Test
    public void testGetWebpageUserConfig() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        AuthService.getConfig(jwtToken, openId, "clientVersion", callback);
    }

    @Test
    public void setProtocol() {
        AuthExecutor.init(null, null, null);
        Pattern pattern = Pattern.compile("(https?)://api\\.xunyou\\.mobi/api/v[12]/");
        for (int i = 0; i < 2; ++i) {
            for (int version = 1; version <= 2; ++version) {
                boolean useHttpProtocol = i != 0;
                AuthService.setProtocol(useHttpProtocol);
                String urlBase = AuthService.getUrlBase(version);
                Matcher matcher = pattern.matcher(urlBase);
                assertTrue(matcher.find());
                String protocol = matcher.group(1);
                assertEquals(useHttpProtocol ? "http" : "https", protocol);
                assertEquals(urlBase, matcher.group(0));
                assertEquals((char) ('0' + version), urlBase.charAt(urlBase.length() - 2));
            }
        }
    }

    @Test
    public void setUserConfig() {
        AuthExecutor.init(ServiceConfigForTest.getServiceConfig().getAuthServiceLocation(), null, null);
        AuthService.setUserConfig("jwtToken", "userId", "111".getBytes(), new ResponseCallback(null, 1) {
            @Override
            protected String getEventName() {
                return null;
            }

            @Override
            protected void onSuccess(int code, byte[] responseData) {

            }

            @Override
            protected void onFail(int code, byte[] responseData) {

            }
        });
    }
}