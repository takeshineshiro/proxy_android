package com.subao.common.accel;

import com.subao.vpn.VPNJni;
import com.subao.vpn.VpnEventObserver;

/**
 * AccelEngineInstance
 * <p>Created by YinHaiBo on 2017/3/30.</p>
 * <p>
 * 这是一个全局变量，用于存放当前的{@link AccelEngine}，方便各模块（特别是VpnService）之间的通讯
 */
public class AccelEngineInstance {

    private static AccelEngine currentAccelEngine;

    private AccelEngineInstance() {
    }

    public static void set(AccelEngine accelEngine) {
        AccelEngineInstance.currentAccelEngine = accelEngine;
    }

    public static AccelEngine get() {
        return currentAccelEngine;
    }

    public static void registerVpnEventObserver(VpnEventObserver o) {
        VPNJni.registerVpnEventObserver(o);
    }

    public static void unregisterVpnEventObserver(VpnEventObserver o) {
        VPNJni.unregisterVpnEventObserver(o);
    }


}
