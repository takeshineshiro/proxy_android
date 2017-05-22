package com.subao.common.jni;

import com.subao.vpn.VPNJni;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * InitJNIModeTest
 * <p>Created by YinHaiBo on 2017/3/24.</p>
 */
public class InitJNIModeTest {

    @Test
    public void test() {
        assertEquals(3, InitJNIMode.values().length);
        assertEquals(VPNJni.INIT_MODE_UDP, InitJNIMode.UDP.intValue);
        assertEquals(VPNJni.INIT_MODE_TCP, InitJNIMode.TCP.intValue);
        assertEquals(VPNJni.INIT_MODE_VPN, InitJNIMode.VPN.intValue);
    }

}