package com.subao.common.net;

import android.telephony.TelephonyManager;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static junit.framework.Assert.assertEquals;

/**
 * Created by nosound on 2016/8/18.
 */
public class MobileNetTypeDetectorTest {

    @Test
    public void testConstructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RoboBase.testPrivateConstructor(MobileNetTypeDetector.class);
    }

    @Test
    public void test() {
        for (int type = 0; type < 20; ++type) {
            NetTypeDetector.NetType nt = MobileNetTypeDetector.getMobileNetworkType(type);
            switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case 16:
                assertEquals(NetTypeDetector.NetType.MOBILE_2G, nt);
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case 17:
                assertEquals(NetTypeDetector.NetType.MOBILE_3G, nt);
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case 139:
                assertEquals(NetTypeDetector.NetType.MOBILE_4G, nt);
                break;
            default:
                assertEquals(NetTypeDetector.NetType.UNKNOWN, nt);
                break;
            }
        }
    }
}
