package com.subao.common.accel;

import com.subao.common.RoboBase;
import com.subao.common.jni.ShadowVPNJni;
import com.subao.vpn.VPNJni;
import com.subao.vpn.VpnEventObserver;

import org.junit.Test;
import org.robolectric.annotation.Config;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * AccelEngineInstanceTest
 * <p>Created by YinHaiBo on 2017/3/30.</p>
 */
public class AccelEngineInstanceTest extends RoboBase {

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RoboBase.testPrivateConstructor(AccelEngineInstance.class);
    }

    @Test
    public void set() {
        AccelEngine old = AccelEngineInstance.get();
        try {
            AccelEngine accelEngine = mock(AccelEngine.class);
            AccelEngineInstance.set(accelEngine);
            assertEquals(accelEngine, AccelEngineInstance.get());
        } finally {
            AccelEngineInstance.set(old);
        }
    }

    @Test
    @Config(shadows = ShadowVPNJni.class)
    public void observer() {
        MockObserver mockObserver = new MockObserver();
        AccelEngineInstance.registerVpnEventObserver(mockObserver);
        try {
            AccelEngineInstance.registerVpnEventObserver(mockObserver);
            try {
                assertNull(mockObserver.active);
                VPNJni.doStartVPN(123);
                assertTrue(mockObserver.active);
                VPNJni.doStopVPN();
                assertFalse(mockObserver.active);
            } finally {
                AccelEngineInstance.unregisterVpnEventObserver(mockObserver);
            }
        } finally {
            AccelEngineInstance.unregisterVpnEventObserver(mockObserver);
        }
        //
        mockObserver.active = null;
        VPNJni.doStartVPN(123);
        assertNull(mockObserver.active);
        VPNJni.doStopVPN();
        assertNull(mockObserver.active);
    }

    private static class MockObserver implements VpnEventObserver {

        Boolean active;

        @Override
        public void onVPNStateChanged(boolean active) {
            this.active = active;
        }
    }

}