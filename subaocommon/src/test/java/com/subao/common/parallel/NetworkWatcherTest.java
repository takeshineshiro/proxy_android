package com.subao.common.parallel;

import android.content.Context;
import android.content.pm.PackageManager;

import com.subao.common.ErrorCode;
import com.subao.common.RoboBase;
import com.subao.common.parallel.NetworkWatcher.Network;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NetworkWatcherTest extends RoboBase {

//    private static NetworkWatcher.TransportType toTransportType(int ordinal) {
//        NetworkWatcher.TransportType[] all = NetworkWatcher.TransportType.values();
//        int length = all.length;
//        if ((ordinal < 0) || (ordinal >= length)) {
//            return null;
//        }
//        return all[ordinal];
//    }

    @Test
    public void testConstructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RoboBase.testPrivateConstructor(NetworkWatcher.class);
    }

    @Test
    public void testRegisterNoPermission() throws NetworkWatcher.OperationException {
        // 没CHANGE_NETWORK_STATE权限，初始化会失败
        try {
            NetworkWatcher.init(getContext());
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.WIFI_ACCEL_NO_PERMISSION, e.getErrorCode());
        }
        MockCallback mockCallback = new MockCallback();
        try {
            NetworkWatcher.register(NetworkWatcher.TransportType.CELLULAR, mockCallback);
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.WIFI_ACCEL_NO_PERMISSION, e.getErrorCode());
        }
    }

    @Test
    public void testRegisterHasPermission() throws NetworkWatcher.OperationException {
        Context context = mock(Context.class);
        when(context.checkCallingOrSelfPermission("android.permission.CHANGE_NETWORK_STATE")).thenReturn(PackageManager.PERMISSION_GRANTED);
        NetworkWatcher.init(context);
        //
        MockCallback mockCallback = new MockCallback();
        try {
            Object registerObj = NetworkWatcher.register(NetworkWatcher.TransportType.CELLULAR, mockCallback);
            assertNotNull(registerObj);
            NetworkWatcher.unregister(registerObj);
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.WIFI_ACCEL_REGISTER_FAIL, e.getErrorCode());
        }
    }

    @Test
    public void testTransportType() {
        assertEquals(NetworkWatcher.TransportType.values().length, 5);
        assertEquals(NetworkWatcher.TransportType.CELLULAR.ordinal(), 0);
        assertEquals(NetworkWatcher.TransportType.WIFI.ordinal(), 1);
        assertEquals(NetworkWatcher.TransportType.BLUETOOTH.ordinal(), 2);
        assertEquals(NetworkWatcher.TransportType.ETHERNET.ordinal(), 3);
        assertEquals(NetworkWatcher.TransportType.VPN.ordinal(), 4);
    }

//    @Test
//    public void testRegister() throws NetworkWatcher.OperationException {
//        int count = NetworkWatcher.TransportType.values().length;
//        for (int i = 0; i < count; i++) {
//            NetworkWatcher.TransportType type = toTransportType(i);
//            assertNotNull(type);
//            try {
//                NetworkWatcher.register(getContext(), type, testCallback);
//                fail();
//            } catch (NetworkWatcher.OperationException e) {}
//            try {
//                NetworkWatcher.register(getContext(), type, null);
//                fail();
//            } catch (NullPointerException e) {}
//        }
//    }
//
//	@Test
//	public void testIsSupported(){
//		assertFalse(NetworkWatcher.isSupported());
//	}
//
//	@Test
//	public void testHasRequiedPermission(){
//		assertFalse(NetworkWatcher.hasRequiredPermission(context));
//	}

    @Test
    public void testUnregister() {
        for (int i = -1; i < 5; i++) {
            NetworkWatcher.unregister(i);
        }
    }

    static class MockCallback implements NetworkWatcher.Callback {

        @Override
        public void onAvailable(Network network) {
            assertNotNull(network);
        }

        @Override
        public void onLost(Network network) {
            assertNotNull(network);
        }

    }
}