package com.subao.common.net;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import com.subao.common.RoboBase;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.net.NetTypeDetector.NetType;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Created by hujd on 16-7-21.
 */
public class NetUtilsTest extends RoboBase {

    private static Context mockContext() {
        Context context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        return context;
    }

    @Test
    public void tessConstructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RoboBase.testPrivateConstructor(NetUtils.class);
    }

    @Test
    public void testGetLocalIp() throws SocketException {
        assertNotNull(NetUtils.getLocalIp(null));
        MockLocalIpFilter filter = new MockLocalIpFilter();
        filter.result = true;
        byte[] localIp = NetUtils.getLocalIp(filter);
        assertArrayEquals(filter.ip, localIp);
        //
        filter.result = false;
        assertNull(NetUtils.getLocalIp(filter));
        //
        NetUtils.NetworkInterfaceImpl ni = mock(NetUtils.NetworkInterfaceImpl.class);
        doThrow(SocketException.class).when(ni).getNetworkInterfaces();
        assertNull(NetUtils.getLocalIp(ni, null));
    }

    @Test
    public void testHostToIP() throws IOException {
        String ipLoopback = "127.0.0.1";
        assertEquals(ipLoopback, NetUtils.hostToIP(null));
    }

    @Test(expected = IOException.class)
    public void testHostToIPFail() throws IOException {
        NetUtils.NameSolver nameSolver = mock(NetUtils.NameSolver.class);
        doReturn(null).when(nameSolver).getAllByName(anyString());
        NetUtils.hostToIP("www.example.com", nameSolver);
    }

    @Test(expected = IOException.class)
    public void testHostToIPRuntimeException() throws IOException {
        NetUtils.NameSolver nameSolver = mock(NetUtils.NameSolver.class);
        doThrow(SecurityException.class).when(nameSolver).getAllByName(anyString());
        NetUtils.hostToIP("www.example.com", nameSolver);
    }

    @Test
    public void testGetCurrentNetName_NoConnection() {
        MockNetTypeDetector netDetector = new MockNetTypeDetector();
        netDetector.setCurrentNetworkType(NetType.DISCONNECT);
        String name = NetUtils.getCurrentNetName(getContext(), netDetector);
        assertNull(name);
    }

    @SuppressLint("WifiManagerLeak")
    @Test
    public void testGetCurrentNetName_WiFi() {
        String ssid = "Hello, world";
        MockNetTypeDetector netDetector = new MockNetTypeDetector();
        netDetector.setCurrentNetworkType(NetType.WIFI);
        Context context = mockContext();
        WifiManager wifiManager = mock(WifiManager.class);
        doReturn(wifiManager).when(context).getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mock(WifiInfo.class);
        doReturn(wifiInfo).when(wifiManager).getConnectionInfo();
        doReturn(ssid).when(wifiInfo).getSSID();
        //
        String name = NetUtils.getCurrentNetName(context, netDetector);
        assertEquals(ssid, name);
    }

    @SuppressLint("WifiManagerLeak")
    @Test
    public void testGetCurrentNetName_WiFi_SecurityException() {
        MockNetTypeDetector netDetector = new MockNetTypeDetector();
        netDetector.setCurrentNetworkType(NetType.WIFI);
        Context context = mockContext();
        WifiManager wifiManager = mock(WifiManager.class);
        doReturn(wifiManager).when(context).getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mock(WifiInfo.class);
        doReturn(wifiInfo).when(wifiManager).getConnectionInfo();
        doThrow(SecurityException.class).when(wifiInfo).getSSID();
        assertNull(NetUtils.getCurrentNetName(context, netDetector));
    }

    @Test
    public void testGetCurrentNetName_Mobile() {
        MockNetTypeDetector netDetector = new MockNetTypeDetector();
        netDetector.setCurrentNetworkType(NetType.MOBILE_4G);
        //
        Context context = mockContext();
        doReturn(null).when(context).getSystemService(Context.TELEPHONY_SERVICE);
        assertNull(NetUtils.getCurrentNetName(context, netDetector));
        //
        context = mockContext();
        doThrow(SecurityException.class).when(context).getSystemService(Context.TELEPHONY_SERVICE);
        assertNull(NetUtils.getCurrentNetName(context, netDetector));
        //
        context = mockContext();
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        doReturn(telephonyManager).when(context).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn("abc").when(telephonyManager).getNetworkOperator();
        assertNull(NetUtils.getCurrentNetName(context, netDetector));
        //
        context = mockContext();
        telephonyManager = mock(TelephonyManager.class);
        doReturn(telephonyManager).when(context).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn("1234").when(telephonyManager).getNetworkOperator();
        doReturn(null).when(telephonyManager).getCellLocation();
        assertNull(NetUtils.getCurrentNetName(context, netDetector));
        //
        context = mockContext();
        telephonyManager = mock(TelephonyManager.class);
        doReturn(telephonyManager).when(context).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn("1234").when(telephonyManager).getNetworkOperator();
        doReturn(mock(CellLocation.class)).when(telephonyManager).getCellLocation();
        assertNull(NetUtils.getCurrentNetName(context, netDetector));
        //
        context = mockContext();
        telephonyManager = mock(TelephonyManager.class);
        doReturn(telephonyManager).when(context).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn("1234").when(telephonyManager).getNetworkOperator();
        GsmCellLocation gsmCellLocation = mock(GsmCellLocation.class);
        doReturn(gsmCellLocation).when(telephonyManager).getCellLocation();
        assertNotNull(NetUtils.getCurrentNetName(context, netDetector));
    }

    private static class MockLocalIpFilter implements NetUtils.LocalIpFilter {

        boolean result;
        byte[] ip;

        @Override
        public boolean isValidLocalIp(byte[] ip) {
            if (result) {
                this.ip = Arrays.copyOf(ip, ip.length);
                return true;
            } else {
                return false;
            }
        }
    }

}
