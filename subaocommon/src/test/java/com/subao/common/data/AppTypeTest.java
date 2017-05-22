package com.subao.common.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * AppTypeTest
 * <p>Created by YinHaiBo on 2016/11/15.</p>
 */
public class AppTypeTest {
    @Test
    public void testEnumAppType() {
        assertEquals(11, AppType.values().length);
        assertEquals(0, AppType.UNKNOWN_APPTYPE.getId());
        assertEquals(1, AppType.ANDROID_APP.getId());
        assertEquals(2, AppType.ANDROID_SDK_EMBEDED.getId());
        assertEquals(3, AppType.ANDROID_SDK.getId());
        assertEquals(4, AppType.IOS_APP.getId());
        assertEquals(5, AppType.IOS_SDK_EMBEDED.getId());
        assertEquals(6, AppType.IOS_SDK.getId());
        assertEquals(7, AppType.WIN_APP.getId());
        assertEquals(8, AppType.WIN_SDK_EMBEDED.getId());
        assertEquals(9, AppType.WIN_SDK.getId());
        assertEquals(10, AppType.WEB_SDK.getId());
    }
}