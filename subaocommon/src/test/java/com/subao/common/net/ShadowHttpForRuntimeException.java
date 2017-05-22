package com.subao.common.net;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.net.HttpURLConnection;

/**
 * ShadowHttpForRuntimeException
 * <p>Created by YinHaiBo on 2017/2/9.</p>
 */
@Implements(value = Http.class, isInAndroidSdk = false)
public class ShadowHttpForRuntimeException {
    @Implementation
    public static Http.Response doGet(HttpURLConnection connection) {
        throw new SecurityException("for test");
    }
}
