package com.subao.common.net;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by hujd on 16-7-21.
 */
public class ResponseCallbackTest {

    @Test
    public void testIsHttpResponseCodeSuccess() {
        for (int code = 200; code < 300; ++code) {
            assertTrue(ResponseCallback.isHttpResponseCodeSuccess(code));
        }
        assertFalse(ResponseCallback.isHttpResponseCodeSuccess(199));
        assertFalse(ResponseCallback.isHttpResponseCodeSuccess(300));
    }
}