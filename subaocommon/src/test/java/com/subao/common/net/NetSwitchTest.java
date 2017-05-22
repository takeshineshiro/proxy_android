package com.subao.common.net;

import android.content.Context;
import android.net.ConnectivityManager;

import com.subao.common.RoboBase;
import com.subao.common.SwitchState;

import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * NetSwitchTest
 * <p>Created by YinHaiBo on 2017/3/7.</p>
 */
public class NetSwitchTest extends RoboBase {

    @Test
    public void test() {
        assertNotNull(NetSwitch.getMobileSwitchState(getContext()));
    }

    @Test
    public void testConnectivityManagerNull() {
        Context context = mock(Context.class);
        doReturn(null).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(context).when(context).getApplicationContext();
        assertEquals(SwitchState.UNKNOWN, NetSwitch.getMobileSwitchState(context));
    }

    @Test
    @Config(shadows = ShadowConnectivityManager_MobileOn.class)
    public void testOn() {
        assertEquals(SwitchState.ON, NetSwitch.getMobileSwitchState(getContext()));
    }

    @Test
    @Config(shadows = ShadowConnectivityManager_MobileOff.class)
    public void testOff() {
        assertEquals(SwitchState.OFF, NetSwitch.getMobileSwitchState(getContext()));
    }

    @Test
    @Config(shadows = ShadowConnectivityManager_Exception.class)
    public void testException() {
        assertEquals(SwitchState.UNKNOWN, NetSwitch.getMobileSwitchState(getContext()));
    }

    @Implements(ConnectivityManager.class)
    public static class ShadowConnectivityManager_MobileOn {

        @Implementation
        public boolean getMobileDataEnabled() {
            return true;
        }
    }

    @Implements(ConnectivityManager.class)
    public static class ShadowConnectivityManager_MobileOff {

        @Implementation
        public boolean getMobileDataEnabled() {
            return false;
        }
    }

    @Implements(ConnectivityManager.class)
    public static class ShadowConnectivityManager_Exception {

        @Implementation
        public boolean getMobileDataEnabled() {
            throw new SecurityException();
        }
    }

}