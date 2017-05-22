package com.subao.common.net;

import android.content.Context;
import android.net.ConnectivityManager;

import com.subao.common.SwitchState;

import java.lang.reflect.Method;

/**
 * 网络开关状态获取
 * <p>Created by YinHaiBo on 2017/3/7.</p>
 */
public class NetSwitch {

//    public static boolean setWiFiSwitch(Context context, boolean enabled) {
//        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        return wifiManager.setWifiEnabled(enabled);
//    }
//
//    public static SwitchState getWiFiSwitchState(Context context) {
//        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        int state = wifiManager.getWifiState();
//        if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING) {
//            return SwitchState.ON;
//        } else {
//            return SwitchState.OFF;
//        }
//    }

    /**
     * 获取当前移动网络开关状态
     *
     * @param context {@link Context}
     * @return {@link SwitchState}
     */
    public static SwitchState getMobileSwitchState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return SwitchState.UNKNOWN;
        }
        try {
            Method method = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            boolean result = (boolean) (Boolean)method.invoke(cm);
            return result ? SwitchState.ON : SwitchState.OFF;
        } catch (Exception e) {
            return SwitchState.UNKNOWN;
        }
    }

}
