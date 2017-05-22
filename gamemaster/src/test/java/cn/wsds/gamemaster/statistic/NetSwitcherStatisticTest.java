package cn.wsds.gamemaster.statistic;

import android.content.Context;

import com.subao.common.net.NetTypeDetector;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.lang.reflect.InvocationTargetException;

import cn.wsds.gamemaster.MockNetTypeDetector;
import cn.wsds.gamemaster.RoboBase;
import cn.wsds.gamemaster.net.NetSwitcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 * NetSwitcherStatisticTest
 * <p>Created by YinHaiBo on 2017/3/3.</p>
 */
@Config(shadows = {NetSwitcherStatisticTest.ShadowStatistic.class, NetSwitcherStatisticTest.ShadowNetSwitcher.class})
public class NetSwitcherStatisticTest extends RoboBase {

    private MockNetTypeDetector mockNetTypeDetector;

    @Before
    public void setUp() {
        ShadowStatistic.actualEvent = null;
        ShadowNetSwitcher.mobileDataSwitchState = null;
        mockNetTypeDetector = new MockNetTypeDetector();
    }

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        testPrivateConstructor(NetSwitcherStatistic.class);
    }

    @Test
    public void testBelowAndroid5() {
        mockNetTypeDetector.currentNetworkType = NetTypeDetector.NetType.MOBILE_4G;
        Context context = mock(Context.class);
        NetSwitcherStatistic.execute(context, mockNetTypeDetector);
        assertNull(ShadowStatistic.actualEvent);
    }

    @Test
    @Config(sdk = 21)
    public void testDisconnectedOrUnknown() {
        mockNetTypeDetector.currentNetworkType = NetTypeDetector.NetType.DISCONNECT;
        NetSwitcherStatistic.execute(getContext(), mockNetTypeDetector);
        assertEquals(Statistic.Event.ACC_GAME_ONTOP_NET_NULL, ShadowStatistic.actualEvent);
        mockNetTypeDetector.currentNetworkType = NetTypeDetector.NetType.UNKNOWN;
        assertEquals(Statistic.Event.ACC_GAME_ONTOP_NET_NULL, ShadowStatistic.actualEvent);
    }

    @Test
    @Config(sdk = 21)
    public void testWiFi() {
        mockNetTypeDetector.currentNetworkType = NetTypeDetector.NetType.WIFI;
        //
        ShadowNetSwitcher.mobileDataSwitchState = NetSwitcher.SwitchState.OFF;
        NetSwitcherStatistic.execute(getContext(), mockNetTypeDetector);
        assertEquals(Statistic.Event.ACC_GAME_ONTOP_NET_WIFI, ShadowStatistic.actualEvent);
        //
        ShadowNetSwitcher.mobileDataSwitchState = NetSwitcher.SwitchState.ON;
        NetSwitcherStatistic.execute(getContext(), mockNetTypeDetector);
        assertEquals(Statistic.Event.ACC_GAME_ONTOP_NET_WIFI_AND_FLOW, ShadowStatistic.actualEvent);
        //
        ShadowNetSwitcher.mobileDataSwitchState = NetSwitcher.SwitchState.UNKNOWN;
        NetSwitcherStatistic.execute(getContext(), mockNetTypeDetector);
        assertEquals(Statistic.Event.ACC_GAME_ONTOP_WIFI_AND_MOBILE_UNKNOWN, ShadowStatistic.actualEvent);
    }

    @Test
    @Config(sdk = 21)
    public void testMobile() {
        NetTypeDetector.NetType[] mobileNetTypeList = new NetTypeDetector.NetType[] {
            NetTypeDetector.NetType.MOBILE_4G,
            NetTypeDetector.NetType.MOBILE_3G,
            NetTypeDetector.NetType.MOBILE_2G
        };
        for (NetTypeDetector.NetType netType : mobileNetTypeList) {
            mockNetTypeDetector.currentNetworkType = netType;
            NetSwitcherStatistic.execute(getContext(), mockNetTypeDetector);
            assertEquals(Statistic.Event.ACC_GAME_ONTOP_NET_FLOW, ShadowStatistic.actualEvent);
        }
    }


    @Implements(value = Statistic.class, isInAndroidSdk = false)
    public static class ShadowStatistic {

        static Statistic.Event actualEvent;

        @Implementation
        @SuppressWarnings("unused")
        public static void addEvent(Context context, Statistic.Event event) {
            actualEvent = event;
        }
    }

    @Implements(value = NetSwitcher.class, isInAndroidSdk = false)
    public static class ShadowNetSwitcher {

        static NetSwitcher.SwitchState mobileDataSwitchState;

        @SuppressWarnings("unused")
        public static NetSwitcher.SwitchState getMobileDataSwitchState(Context context) {
            return mobileDataSwitchState;
        }
    }
}