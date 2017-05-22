package cn.wsds.gamemaster.net;


import android.content.Context;
import android.net.ConnectivityManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NetSwitcher {

    //	public static boolean switchWifiConnection(Context context, boolean enabled) {
//		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//		return wifiManager.setWifiEnabled(enabled);
//	}
//
//	public static boolean getWifiSwitch(Context context) {
//		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//		int state = wifiManager.getWifiState();
//		return (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING);
//	}
//
//	public static void setMobileDataSwitch(Context context, boolean on) {
//		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//		try {
//			Method method = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
//			method.setAccessible(true);
//			method.invoke(cm, on);
//		} catch (Exception e) {
//		}
//	}
//
    public static SwitchState getMobileDataSwitchState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Method method;
        try {
            method = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
        } catch (NoSuchMethodException e) {
            return SwitchState.UNKNOWN;
        }
        method.setAccessible(true);
        try {
            return (Boolean) method.invoke(cm) ? SwitchState.ON : SwitchState.OFF;
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return SwitchState.UNKNOWN;
    }

    public enum SwitchState {
        UNKNOWN,
        OFF,
        ON
    }
//	
//	public static void resetCurrentNetSwitch(final Context context) {
//		switch (NetManager.getInstance().getCurrentNetworkType(context)) {
//		case NetTypeDetector.NETWORK_CLASS_UNKNOWN:
//			return;
//		case NetTypeDetector.NETWORK_CLASS_WIFI:
//			switchWifiConnection(context, false);
//			MainHandler.getInstance().postDelayed(new Runnable() {
//				@Override public void run() {
//					switchWifiConnection(context, true);
//				}
//			}, 1000);
//			break;
//		default:
//			setMobileDataSwitch(context, false);
//			MainHandler.getInstance().postDelayed(new Runnable() {
//				@Override public void run() {
//					setMobileDataSwitch(context, true);
//				}
//			}, 1000);
//			break;
//		}
//	}
}
