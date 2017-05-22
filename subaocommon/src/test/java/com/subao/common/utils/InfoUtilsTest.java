package com.subao.common.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import com.subao.common.RoboBase;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressLint("HardwareIds")
public class InfoUtilsTest extends RoboBase {

    private static final String IMSI = "012345678901234";

    @Test
    public void testConstructor() {
        new InfoUtils();
    }

    @Test
    public void testEmptyUniqueID() {
        assertTrue(InfoUtils.isEmptyUniqueID(InfoUtils.IMSI_EMPTY));
        assertEquals(15, InfoUtils.IMSI_EMPTY.length());
        for (int i = InfoUtils.IMSI_EMPTY.length() - 1; i >= 0; --i) {
            assertEquals('0', InfoUtils.IMSI_EMPTY.charAt(i));
        }
    }

    @Test
    public void getUniqueID() {
        Context context = mock(Context.class);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(null);
        assertEquals(InfoUtils.IMSI_EMPTY, InfoUtils.getUniqueID(context));
        //
        context = mock(Context.class);
        TelephonyManager tm = mock(TelephonyManager.class);
        when(tm.getSubscriberId()).thenThrow(RuntimeException.class);
        when(tm.getDeviceId()).thenThrow(RuntimeException.class);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(tm);
        assertEquals(InfoUtils.IMSI_EMPTY, InfoUtils.getUniqueID(context));
        //
        context = mock(Context.class);
        tm = mock(TelephonyManager.class);
        when(tm.getSubscriberId()).thenReturn(IMSI);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(tm);
        assertEquals(IMSI, InfoUtils.getUniqueID(context));
        //
        when(tm.getSubscriberId()).thenThrow(RuntimeException.class);
        when(tm.getDeviceId()).thenReturn(IMSI);
        assertEquals(IMSI, InfoUtils.getUniqueID(context));
    }

    @Test
    public void testIsIMSIValid() {
        assertFalse(InfoUtils.isIMSIValid(null));
        assertFalse(InfoUtils.isIMSIValid(""));
        assertFalse(InfoUtils.isIMSIValid("a22222222"));
        assertFalse(InfoUtils.isIMSIValid(" a222"));
        assertFalse(InfoUtils.isIMSIValid("12222224344444444444444444444444222"));
        assertTrue(InfoUtils.isIMSIValid("2222222222222222222"));
    }

    @Test
    public void getIMSI() throws Exception {
        Context context = mock(Context.class);
        TelephonyManager tm = mock(TelephonyManager.class);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(tm);
        when(tm.getSubscriberId()).thenReturn(IMSI);
        assertEquals(IMSI, InfoUtils.getIMSI(context));
        //
        when(tm.getSubscriberId()).thenReturn(null);
        assertNull(InfoUtils.getIMSI(context));
        //
        when(tm.getSubscriberId()).thenThrow(Exception.class);
        assertNull(InfoUtils.getIMSI(context));
        //
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(null);
        assertNull(InfoUtils.getIMSI(context));
        //
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenThrow(RuntimeException.class);
        assertNull(InfoUtils.getIMSI(context));
    }

    @Test
    public void getIMEI() throws Exception {
        Context context = mock(Context.class);
        TelephonyManager tm = mock(TelephonyManager.class);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(tm);
        when(tm.getDeviceId()).thenReturn(IMSI);
        assertEquals(IMSI, InfoUtils.getIMEI(context));
        //
        when(tm.getDeviceId()).thenReturn(null);
        assertNull(InfoUtils.getIMEI(context));
        //
        when(tm.getDeviceId()).thenThrow(Exception.class);
        assertNull(InfoUtils.getIMEI(context));
        //
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(null);
        assertNull(InfoUtils.getIMEI(context));
        //
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenThrow(RuntimeException.class);
        assertNull(InfoUtils.getIMEI(context));
    }

    @SuppressWarnings("MissingPermission")
    @SuppressLint("WifiManagerLeak")
    @Test
    public void getMacAddress() throws Exception {
        final String MAC_ADDRESS = "this is a mac address";
        Context context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
        WifiManager wm = mock(WifiManager.class);
        WifiInfo info = mock(WifiInfo.class);
        when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(wm);
        when(wm.getConnectionInfo()).thenReturn(info);
        when(info.getMacAddress()).thenReturn(MAC_ADDRESS);
        assertEquals(MAC_ADDRESS, InfoUtils.getMacAddress(context));
        //
        when(context.getSystemService(Context.WIFI_SERVICE)).thenThrow(RuntimeException.class);
        assertNull(InfoUtils.getMacAddress(context));
        //
        context = mock(Context.class);
        when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(null);
        assertNull(InfoUtils.getMacAddress(context));
        //
        context = mock(Context.class);
        wm = mock(WifiManager.class);
        when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(wm);
        when(wm.getConnectionInfo()).thenReturn(null);
        assertNull(InfoUtils.getMacAddress(context));
    }

    @Test
    public void testGetVersionName() throws NameNotFoundException {
        Context context = RuntimeEnvironment.application;
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
        assertEquals(info.versionName, InfoUtils.getVersionName(context));
        assertEquals(info.versionCode, InfoUtils.getVersionCode(context));
    }

    @Test
    public void testGetSystemProperty() throws IOException {
        assertNull(InfoUtils.getSystemProperty("hello"));
        InputStream inputStream = new ByteArrayInputStream("world\r\n".getBytes());
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(inputStream);
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec("getprop hello")).thenReturn(process);
        assertEquals("world", InfoUtils.getSystemProperty(runtime, "hello"));

    }

    @Test
    public void testGetTotalMemory() {
        Context context = RuntimeEnvironment.application;
        InfoUtils.getTotalMemory(context);
        InfoUtils.getTotalMemory(context, 1);
        InfoUtils.getTotalMemory(context, 22);
    }

    @Test
    public void testParseForValue() {
        byte[] data = "aa: 1234 kB\nbb:  5678 kB".getBytes();
        assertEquals(1234, InfoUtils.parseForValue(data, "aa", -1));
        assertEquals(5678, InfoUtils.parseForValue(data, "bb", -1));
        assertEquals(-1, InfoUtils.parseForValue(data, "cc", -1));
    }

    @Test
    public void testGetTotalMemoryVerLow() {
        InfoUtils.getTotalMemoryVerLow();
    }

    @Test
    public void getPackageInfo() {
//        PackageManager packageManager =
        Context context = mock(Context.class);
        when(context.getPackageManager()).thenThrow(RuntimeException.class);
        assertNull(InfoUtils.getPackageInfo(context, null));
    }

    @Test
    public void loadAppLabel() {
        Context context = getContext();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        String expected = applicationInfo.loadLabel(context.getPackageManager()).toString();
        assertEquals(expected, InfoUtils.loadAppLabel(context, null));
        //
        context = mock(Context.class);
        doReturn(null).when(context).getApplicationInfo();
        assertNull(InfoUtils.loadAppLabel(context, null));
    }
}
