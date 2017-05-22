package com.subao.common.net;

import android.telephony.TelephonyManager;

/**
 * 移动网络类型判断
 */
public class MobileNetTypeDetector {

    private static final int _NETWORK_TYPE_GSM = 16;
    private static final int _NETWORK_TYPE_TD_SCDMA = 17;
//    private static final int _NETWORK_TYPE_IWLAN = 18;

    private MobileNetTypeDetector() {}

    public static NetTypeDetector.NetType getMobileNetworkType(int subType) {
        switch (subType) {
        case TelephonyManager.NETWORK_TYPE_GPRS:
        case TelephonyManager.NETWORK_TYPE_EDGE:
        case TelephonyManager.NETWORK_TYPE_CDMA:
        case TelephonyManager.NETWORK_TYPE_1xRTT:
        case TelephonyManager.NETWORK_TYPE_IDEN:
        case _NETWORK_TYPE_GSM:
            return NetTypeDetector.NetType.MOBILE_2G;
        case TelephonyManager.NETWORK_TYPE_UMTS:
        case TelephonyManager.NETWORK_TYPE_EVDO_0:
        case TelephonyManager.NETWORK_TYPE_EVDO_A:
        case TelephonyManager.NETWORK_TYPE_HSDPA:
        case TelephonyManager.NETWORK_TYPE_HSUPA:
        case TelephonyManager.NETWORK_TYPE_HSPA:
        case TelephonyManager.NETWORK_TYPE_EVDO_B:
        case TelephonyManager.NETWORK_TYPE_EHRPD:
        case TelephonyManager.NETWORK_TYPE_HSPAP:
        case _NETWORK_TYPE_TD_SCDMA:
            return NetTypeDetector.NetType.MOBILE_3G;
        case TelephonyManager.NETWORK_TYPE_LTE:
        case 139:
            return NetTypeDetector.NetType.MOBILE_4G;
        default:
            return NetTypeDetector.NetType.UNKNOWN;
        }
    }

}
