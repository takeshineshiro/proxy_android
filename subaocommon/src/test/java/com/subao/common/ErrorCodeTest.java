package com.subao.common;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ErrorCodeTest
 * <p>Created by YinHaiBo on 2017/1/18.</p>
 */
public class ErrorCodeTest {

    @Test
    public void testConstDefines() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<ErrorCode> cs = ErrorCode.class.getDeclaredConstructor();
        assertTrue(0 != (cs.getModifiers() & Modifier.PRIVATE));
        cs.setAccessible(true);
        assertNotNull(cs.newInstance());
        assertEquals(0, ErrorCode.OK);
        assertEquals(1000, ErrorCode.NOT_INIT);
        assertEquals(2000, ErrorCode.WIFI_ACCEL_ANDROID_VERSION_TOO_LOW); //  WiFi加速相关操作：Android版本号过低（5.0及以上才支持WiFi加速）
        assertEquals(2001, ErrorCode.WIFI_ACCEL_NO_PERMISSION); //  WiFi加速相关操作：APP（游戏）未申请CHANGE_NETWORK_STATE权限
        assertEquals(2002, ErrorCode.WIFI_ACCEL_REGISTER_FAIL); //  WiFi加速相关操作：注册回调失败
        assertEquals(2003, ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_OFF); //  WiFi加速相关操作：没有可用的蜂窝网络
        assertEquals(2004, ErrorCode.WIFI_ACCEL_NOT_4G); //  WiFi加速相关操作：当前不是4G/3G
        assertEquals(2005, ErrorCode.WIFI_ACCEL_CREATE_FD_FAIL); //  WiFi加速相关操作：创建FD失败
        assertEquals(2006, ErrorCode.WIFI_ACCEL_MODEL_NOT_ALLOW); //  WiFi加速相关操作：本机型不在允许列表里
        assertEquals(2007, ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON); //  WiFi加速相关操作：没有可用的蜂窝网络
        assertEquals(2008, ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN); //  WiFi加速相关操作：没有可用的蜂窝网络
        assertEquals(2009, ErrorCode.WIFI_ACCEL_BIND_FD_FAIL_VER22);
        assertEquals(2010, ErrorCode.WIFI_ACCEL_FAIL_GET_NET_ID);
        assertEquals(2011, ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR);
        assertEquals(2012, ErrorCode.WIFI_ACCEL_FAIL_GET_FD);
        assertEquals(2013, ErrorCode.WIFI_ACCEL_BIND_FD_FAIL_VER21);
        assertEquals(2014, ErrorCode.WIFI_ACCEL_NOT_MOBILE);
        assertEquals(2015, ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR_AFTER_BIND);
        assertEquals(2100, ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_SIGNAL_STRENGTH);
    }

    @Test
    public void canRetryWhenWifiAccelError() throws Exception {
        for (int code = 2000; code < 2200; ++code) {
            boolean canRetry = ErrorCode.canRetryWhenWifiAccelError(code);
            boolean expected =
                (code == ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON)
                    || (code == ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_OFF)
                    || (code == ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN)
                    || (code == ErrorCode.WIFI_ACCEL_NOT_4G)
                    || (code == ErrorCode.WIFI_ACCEL_CREATE_FD_FAIL)
                    || (code == ErrorCode.WIFI_ACCEL_BIND_FD_FAIL_VER22)
                    || (code == ErrorCode.WIFI_ACCEL_FAIL_GET_NET_ID)
                    || (code == ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR)
                    || (code == ErrorCode.WIFI_ACCEL_FAIL_GET_FD)
                    || (code == ErrorCode.WIFI_ACCEL_NOT_MOBILE)
                    || (code == ErrorCode.WIFI_ACCEL_BIND_FD_FAIL_VER21)
                    || (code == ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR_AFTER_BIND)
                    || (code >= ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_SIGNAL_STRENGTH && code <= ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_SIGNAL_STRENGTH + 100);
            assertEquals(expected, canRetry);
        }
    }

}