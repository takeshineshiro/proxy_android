package com.subao.common.jni;

import com.subao.vpn.VPNJni;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * ShadowVPNJni
 * <p>Created by YinHaiBo on 2017/2/12.</p>
 */
@Implements(value = VPNJni.class, isInAndroidSdk = false)
public class ShadowVPNJni {

//    @Implementation
//    public static int loadLibrary(JniCallback callback, String libName) {
//        return 0;
//    }

    private static Object[] workflow;

    public static void cleanup() {
        workflow = null;
    }

    public static Object getLastCallMethodName() {
        return workflow[0];
    }

    public static Object getWorkflowItem(int index) {
        return workflow[index];
    }

    @SuppressWarnings("unused")
    @Implementation
    public static boolean init(
        int cid, int net_state, int is_tcp, byte[] lua_pcode,
        byte[] node_list, byte[] convergenceNodeList)
    {
        return true;
    }

    @Implementation
    public static boolean startVPN(int cid, int fd) {
        workflow = new Object[] {
            "startVPN", cid, fd
        };
        return true;
    }

    @Implementation
    public static void stopVPN(int cid) {
        workflow = new Object[] {
            "stopVPN", cid
        };
    }

}
