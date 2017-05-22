package com.subao.common.parallel;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.telephony.TelephonyManager;

import com.subao.common.ErrorCode;
import com.subao.common.RoboBase;
import com.subao.common.SwitchState;

import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.lang.reflect.Constructor;
import java.net.DatagramSocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class CellularOperatorTest extends RoboBase {

    private static CellularOperator createCellularOperator(CellularOperator.CellularStateListener listener) throws Exception {
        Constructor<CellularOperator> cs = CellularOperator.class.getDeclaredConstructor(CellularOperator.CellularStateListener.class);
        cs.setAccessible(true);
        return cs.newInstance(listener);
    }

    @Test
    public void testNoPermission() throws NetworkWatcher.OperationException {
        try {
            CellularOperator.create(getContext(), null);
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.WIFI_ACCEL_NO_PERMISSION, e.getErrorCode());
        }
    }

    @SuppressWarnings("CheckResult")
    @Test
    @Config(shadows = ShadowNetworkWatcher.class)
    public void testCellularWatcher() throws NetworkWatcher.OperationException {
        Context context = mock(Context.class);
        doReturn(PackageManager.PERMISSION_GRANTED).when(context).checkCallingOrSelfPermission(anyString());
        doReturn(getContext()).when(context).getApplicationContext();
        CellularOperator target = CellularOperator.create(context, new CellularOperator.CellularStateListener() {
            @Override
            public void onCellularStateChange(boolean available) {

            }
        });
        try {
            target.requestNewMobileFD(context);
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertTrue(e.getErrorCode() >= 2000 && e.getErrorCode() < 3000);
        }
        target.dispose();
    }

    @Test
    public void beforeCreateNewMobileFD() throws NetworkWatcher.OperationException {
        // 如果Network.getInfo()返回null了，也应该尝试一下（不要抛异常）
        NetworkWatcher.Network network = mock(NetworkWatcher.Network.class);
        doReturn(null).when(network).getInfo(any(Context.class));
        CellularOperator.Impl.beforeCreateNewMobileFD(getContext(), network);
    }

    @Test
    public void beforeCreateNewMobileFD_RuntimeException() throws NetworkWatcher.OperationException {
        NetworkWatcher.Network network = mock(NetworkWatcher.Network.class);
        doThrow(SecurityException.class).when(network).getInfo(any(Context.class));
        CellularOperator.Impl.beforeCreateNewMobileFD(getContext(), network);
    }

    @Test
    public void beforeCreateNewMobileFD_NotMobile() {
        NetworkInfo info = mock(NetworkInfo.class);
        doReturn(ConnectivityManager.TYPE_WIFI).when(info).getType();
        NetworkWatcher.Network network = mock(NetworkWatcher.Network.class);
        doReturn(info).when(network).getInfo(any(Context.class));
        try {
            CellularOperator.Impl.beforeCreateNewMobileFD(getContext(), network);
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.WIFI_ACCEL_NOT_MOBILE, e.getErrorCode());
        }
    }

    @Test
    public void beforeCreateNewMobileFD_Not4G() {
        NetworkInfo info = mock(NetworkInfo.class);
        doReturn(ConnectivityManager.TYPE_MOBILE).when(info).getType();
        doReturn(TelephonyManager.NETWORK_TYPE_GPRS).when(info).getSubtype();
        NetworkWatcher.Network network = mock(NetworkWatcher.Network.class);
        doReturn(info).when(network).getInfo(any(Context.class));
        try {
            CellularOperator.Impl.beforeCreateNewMobileFD(getContext(), network);
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.WIFI_ACCEL_NOT_4G, e.getErrorCode());
        }
    }

    @Test
    public void getErrorCodeWhenNoAvailableMobileNetwork() {
        assertEquals(ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_OFF,
            CellularOperator.Impl.getErrorCodeWhenNoAvailableMobileNetwork(SwitchState.OFF));
        assertEquals(ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON,
            CellularOperator.Impl.getErrorCodeWhenNoAvailableMobileNetwork(SwitchState.ON));
        assertEquals(ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN,
            CellularOperator.Impl.getErrorCodeWhenNoAvailableMobileNetwork(SwitchState.UNKNOWN));
    }

    @Test
    public void onAvailable() throws NetworkWatcher.OperationException {
        Listener listener = new Listener();
        CellularOperator.Impl impl = new CellularOperator.Impl(listener);
        // 一开始，没收到通知，应该为null;
        assertNull(listener.available);
        // Available #1，断言为 true;
        impl.onAvailable(new MockNetwork(1));
        assertTrue(listener.available);
        //
        // Lost #2，不会收到通知
        impl.onLost(new MockNetwork(2));
        assertTrue(listener.available);
        // Lost #1，会收到通知
        impl.onLost(new MockNetwork(1));
        assertFalse(listener.available);
        // Available #2，有可用网络
        impl.onAvailable(new MockNetwork(2));
        // 再次 Available #2，不会再通知
        listener.available = null;
        impl.onAvailable(new MockNetwork(2));
        assertNull(listener.available);
        //
        // Lost #1，不会收到通知
        listener.available = true;
        impl.onLost(new MockNetwork(1));
        assertTrue(listener.available);
        // Available #3，不会收到通知（因为#2的时候已经通知过一次了）
        listener.available = null;
        impl.onAvailable(new MockNetwork(3));
        assertNull(listener.available);
        //
        // Lost #2，不会收到通知（因为有#3存在）
        listener.available = null;
        impl.onLost(new MockNetwork(2));
        assertNull(listener.available);
        // Lost #3，会收到通知
        impl.onLost(new MockNetwork(3));
        assertFalse(listener.available);
        // 再次 Lost #3，不会收到通知
        listener.available = null;
        impl.onLost(new MockNetwork(3));
        assertNull(listener.available);
    }

    @Test
    @Config(shadows = ShadowParcelFileDescriptor.class)
    public void createNewMobileFD() throws NetworkWatcher.OperationException {
        CellularOperator.Impl impl = new CellularOperator.Impl(null);
        Context context = mock(Context.class);
        doReturn(null).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(context).when(context).getApplicationContext();
        try {
            impl.createNewMobileFD(context);
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertTrue(ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN == e.getErrorCode()
                || ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_OFF == e.getErrorCode()
                || ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON == e.getErrorCode());
        }
        //
        impl.onAvailable(new MockNetwork(123));
        impl.createNewMobileFD(getContext());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void findAvailableMobileNetwork() throws NetworkWatcher.OperationException {
        Context context = mock(Context.class);
        doReturn(null).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(context).when(context).getApplicationContext();
        assertNull(CellularOperator.Impl.findAvailableMobileNetwork(context));
        //
        context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        ConnectivityManager cm = mock(ConnectivityManager.class);
        doReturn(cm).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(null).when(cm).getAllNetworks();
        assertNull(CellularOperator.Impl.findAvailableMobileNetwork(context));
        //
        context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        cm = mock(ConnectivityManager.class);
        doReturn(cm).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(new android.net.Network[0]).when(cm).getAllNetworks();
        assertNull(CellularOperator.Impl.findAvailableMobileNetwork(context));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void findAvailableMobileNetwork_2G() throws NetworkWatcher.OperationException {
        Context context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        ConnectivityManager cm = mock(ConnectivityManager.class);
        doReturn(cm).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        //
        Network[] networks = new Network[1];
        doReturn(networks).when(cm).getAllNetworks();
        NetworkInfo info = mock(NetworkInfo.class);
        doReturn(TelephonyManager.NETWORK_TYPE_GPRS).when(info).getSubtype(); // 2G
        doReturn(ConnectivityManager.TYPE_MOBILE).when(info).getType();
        doReturn(info).when(cm).getNetworkInfo(any(Network.class));
        assertNull(CellularOperator.Impl.findAvailableMobileNetwork(context));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void findAvailableMobileNetwork_NullInfo() throws NetworkWatcher.OperationException {
        Context context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        ConnectivityManager cm = mock(ConnectivityManager.class);
        doReturn(cm).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        //
        Network[] networks = new Network[1];
        doReturn(networks).when(cm).getAllNetworks();
        doReturn(null).when(cm).getNetworkInfo(any(Network.class));
        assertNull(CellularOperator.Impl.findAvailableMobileNetwork(context));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void findAvailableMobileNetwork_WiFi() throws NetworkWatcher.OperationException {
        Context context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        ConnectivityManager cm = mock(ConnectivityManager.class);
        doReturn(cm).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        //
        Network[] networks = new Network[1];
        doReturn(networks).when(cm).getAllNetworks();
        NetworkInfo info = mock(NetworkInfo.class);
        doReturn(ConnectivityManager.TYPE_WIFI).when(info).getType();
        doReturn(info).when(cm).getNetworkInfo(any(Network.class));
        assertNull(CellularOperator.Impl.findAvailableMobileNetwork(context));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void findAvailableMobileNetwork_4G() throws NetworkWatcher.OperationException {
        Context context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        ConnectivityManager cm = mock(ConnectivityManager.class);
        doReturn(cm).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        //
        Network[] networks = new Network[] {
            mock(Network.class)
        };
        doReturn(networks).when(cm).getAllNetworks();
        NetworkInfo info = mock(NetworkInfo.class);
        doReturn(TelephonyManager.NETWORK_TYPE_LTE).when(info).getSubtype(); // 4G
        doReturn(ConnectivityManager.TYPE_MOBILE).when(info).getType();
        doReturn(info).when(cm).getNetworkInfo(any(Network.class));
        assertNotNull(CellularOperator.Impl.findAvailableMobileNetwork(context));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    @Config(shadows = ShadowParcelFileDescriptor_fromDatagramSocketNull.class)
    public void createNewMobileFDFromNetwork() {
        MockNetwork network = new MockNetwork(123);
        try {
            CellularOperator.Impl.createNewMobileFD(network);
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR_AFTER_BIND, e.getErrorCode());
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    @Config(shadows = ShadowParcelFileDescriptor.class)
    public void createNewMobileFDFromNetwork_RuntimeException() {
        MockNetwork network = new MockNetwork(123) {
            @Override
            public void bindToSocket(DatagramSocket socket) throws NetworkWatcher.OperationException {
                throw new SecurityException();
            }
        };
        try {
            CellularOperator.Impl.createNewMobileFD(network);
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR_AFTER_BIND, e.getErrorCode());
        }
    }

    @Implements(value = NetworkWatcher.class, isInAndroidSdk = false)
    public static class ShadowNetworkWatcher {

        @Implementation()
        public static Object register(NetworkWatcher.TransportType type, NetworkWatcher.Callback callback) throws NetworkWatcher.OperationException {
            if (callback == null) {
                throw new NullPointerException("Callback cannot be null");
            }
            return new Object();
        }
    }

    private static class Listener implements CellularOperator.CellularStateListener {

        Boolean available;

        @Override
        public void onCellularStateChange(boolean available) {
            this.available = available;
        }

    }

    private static class MockNetwork implements NetworkWatcher.Network {

        private final int id;

        MockNetwork(int id) {
            this.id = id;
        }

        @Override
        public void bindToSocket(DatagramSocket socket) throws NetworkWatcher.OperationException {

        }

        @Override
        public NetworkInfo getInfo(Context context) {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (!(o instanceof MockNetwork)) {
                return false;
            }
            MockNetwork other = (MockNetwork) o;
            return this.id == other.id;
        }
    }

    @Implements(ParcelFileDescriptor.class)
    public static class ShadowParcelFileDescriptor {

        @Implementation
        public static ParcelFileDescriptor fromDatagramSocket(DatagramSocket datagramSocket) {
            return mock(ParcelFileDescriptor.class);
        }

        @Implementation
        public int detachFd() {
            return 123;
        }
    }

    @Implements(ParcelFileDescriptor.class)
    public static class ShadowParcelFileDescriptor_fromDatagramSocketNull {

        @Implementation
        public static ParcelFileDescriptor fromDatagramSocket(DatagramSocket datagramSocket) {
            return null;
        }

        @Implementation
        public int detachFd() {
            return 123;
        }
    }


}