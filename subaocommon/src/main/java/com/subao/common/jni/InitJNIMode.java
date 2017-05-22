package com.subao.common.jni;

import com.subao.vpn.VPNJni;

/**
 * 初始化所用的模式
 */
public enum InitJNIMode {
    UDP(VPNJni.INIT_MODE_UDP),
    TCP(VPNJni.INIT_MODE_TCP),
    VPN(VPNJni.INIT_MODE_VPN);

    public final int intValue;

    InitJNIMode(int intValue) {
        this.intValue = intValue;
    }
}
