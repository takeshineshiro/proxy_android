package com.subao.common.qos;

import android.util.Pair;

import com.subao.common.Logger;
import com.subao.common.MockProxyEngineCommunicator;
import com.subao.common.RoboBase;
import com.subao.common.data.Defines;
import com.subao.common.data.QosRegionConfig;
import com.subao.common.data.RegionAndISP;
import com.subao.common.net.IPInfoQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QosUser4GRegionAndISPTest extends RoboBase {

    private static final String EXPECTED_IP = "1.2.3.4";
    private static final int EXPECTED_REGION = 31;
    private static final int EXPECTED_ISP = 10;
    private static final String EXPECTED_DETAIL = "detail";

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
    }

    @After
    public void tearDown() {
        Logger.setLoggableChecker(null);
    }

    @Test
    public void test() {
        QosUser4GRegionAndISP target = QosUser4GRegionAndISP.getInstance();
        target.setCurrent(new RegionAndISP(32, 10));
        assertEquals(new RegionAndISP(32, 10), target.getCurrent());
        target.onNetChangeToNot4G();
        assertNull(target.getCurrent());
        target.onNetChangeTo4G();
    }

    @Test
    @Config(shadows = ShadowQosRegionConfig.class)
    public void testNotifyProxyQosParams() {
        QosParam qosParam = QosUser4GRegionAndISP.getInstance().getQosParam();
        MockProxyEngineCommunicator mockProxyEngineCommunicator = new MockProxyEngineCommunicator();
        assertTrue(QosUser4GRegionAndISP.notifyProxyQosParams(mockProxyEngineCommunicator, qosParam));
    }

    @Test
    public void testNotifyProxyQosSwitch() {
        assertFalse(QosUser4GRegionAndISP.notifyProxyQosParams(null, null));
        MockProxyEngineCommunicator mockProxyEngineCommunicator = new MockProxyEngineCommunicator();
        //
        mockProxyEngineCommunicator.reset();
        assertTrue(QosUser4GRegionAndISP.notifyProxyQosParams(mockProxyEngineCommunicator, null));
        assertEquals(Defines.VPNJniStrKey.KEY_ENABLE_QOS, mockProxyEngineCommunicator.getLastKey());
        assertEquals(0, mockProxyEngineCommunicator.getLastIntValue());
        //
        mockProxyEngineCommunicator.reset();
        QosParam qosParam = new QosParam(1, 2, QosParam.Provider.DEFAULT, 7, 8);
        assertTrue(QosUser4GRegionAndISP.notifyProxyQosParams(mockProxyEngineCommunicator, qosParam));
//        assertEquals(4, mockProxyEngineCommunicator.countOfSet());

        Map<String, Object> map = new HashMap<String, Object>(4);
        for (Pair<String, Object> pair : mockProxyEngineCommunicator) {
            String key = pair.first;
            assertFalse(map.containsKey(key));
            Object value = pair.second;
            map.put(key, value);
//            if (key.equals(Defines.VPNJniStrKey.KEY_ENABLE_QOS)) {
//                assertEquals(1, (int) (Integer) value);
//            } else if (key.equals(Defines.VPNJniStrKey.KEY_ENABLE_QOS_AVERAGE)) {
//                assertEquals(3, (int) (Integer) value);
//            } else if (key.equals(Defines.VPNJniStrKey.KEY_QOS_MEASURE_TIME_LONG)) {
//                assertEquals(2, (int) (Integer) value);
//            } else if (key.equals(Defines.VPNJniStrKey.KEY_QOS_OPEN_THRESHOLD)) {
//                assertEquals(1, (int) (Integer) value);
//            } else {
//                fail();
//            }
        }


    }

    @Test
    @Config(shadows = ShadowIPInfoQuery.class)
    public void onNetChangeTo4G() {
        QosUser4GRegionAndISP target = QosUser4GRegionAndISP.getInstance();
        target.onNetChangeTo4G();
        RegionAndISP current = target.getCurrent();
        assertEquals(EXPECTED_REGION, current.region);
        assertEquals(EXPECTED_ISP, current.isp);
    }

    @Implements(QosRegionConfig.class)
    public static class ShadowQosRegionConfig {
        @Implementation
        public static boolean getSwitch() {
            return true;
        }
    }

    @Implements(IPInfoQuery.class)
    public static class ShadowIPInfoQuery {
        @Implementation
        public static void execute(
            String ip,
            IPInfoQuery.Callback callback, Object callbackContext
        ) {
            IPInfoQuery.Result result = new IPInfoQuery.Result(
                EXPECTED_IP, EXPECTED_REGION, EXPECTED_ISP, EXPECTED_DETAIL
            );
            callback.onIPInfoQueryResult(callbackContext, result);
        }
    }
}
