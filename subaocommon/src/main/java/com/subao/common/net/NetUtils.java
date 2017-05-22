package com.subao.common.net;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;

public class NetUtils {

    private NetUtils() {}

    /**
     * 返回网络序的本地IP地址
     *
     * @param filter 检查枚举到的IP是否真是Local IP
     * @return null表示没找到，否则是一个网络序的IPv4地址
     */
    public static byte[] getLocalIp(LocalIpFilter filter) {
        return getLocalIp(new NetworkInterfaceImpl(), filter);
    }

    static byte[] getLocalIp(NetworkInterfaceImpl ni, LocalIpFilter filter) {
        try {
            Enumeration<NetworkInterface> networkInterfaces = ni.getNetworkInterfaces();
            return getLocalIp(networkInterfaces, filter);
        } catch (SocketException e) {
            return null;
        }
    }

    private static byte[] getLocalIp(Enumeration<NetworkInterface> en, LocalIpFilter filter) {
        while (en.hasMoreElements()) {
            NetworkInterface ni = en.nextElement();
            Enumeration<InetAddress> enIp = ni.getInetAddresses();
            while (enIp.hasMoreElements()) {
                InetAddress ia = enIp.nextElement();
                if (ia.isLoopbackAddress() || ia.isAnyLocalAddress() || ia.isLinkLocalAddress()) {
                    continue;
                }
                if (ia instanceof Inet4Address) {
                    byte[] ip = ia.getAddress();
                    if (filter == null || filter.isValidLocalIp(ip)) {
                        return Arrays.copyOf(ip, ip.length);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 将给定的域名解析成IP
     * <p>
     * （此函数需在非UI线程里使用）
     * </p>
     */
    public static String hostToIP(String host) throws IOException {
        return hostToIP(host, new NameSolver());
    }

    static String hostToIP(String host, NameSolver nameSolver) throws IOException {
        try {
            InetAddress[] addresses = nameSolver.getAllByName(host);
            if (addresses == null || addresses.length == 0) {
                throw new IOException();
            }
            int index = (int) System.currentTimeMillis();
            if (index < 0) {
                index = -index;
            }
            index %= addresses.length;
            return addresses[index].getHostAddress();
        } catch (RuntimeException e) {
            throw new NetIOException();
        }
    }

    /**
     * 取当前网络“名字”。如果是WiFi，返回SSID，如果是Mobile
     *
     * @return null表示当前未连网、或获取失败
     */
    @SuppressWarnings("MissingPermission")
    public static String getCurrentNetName(Context context, NetTypeDetector netTypeDetector) {
        if (netTypeDetector.isWiFiConnected()) {
            try {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    WifiInfo info = wifiManager.getConnectionInfo();
                    if (info != null) {
                        return info.getSSID();
                    }
                }
            } catch (RuntimeException e) {
            }
        } else if (netTypeDetector.isMobileConnected()) {
            return getGSMCellLocation(context);
        }
        return null;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private static String getGSMCellLocation(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                return null;
            }
            // 返回值MCC + MNC
            String operator = tm.getNetworkOperator();
            if (operator == null || operator.length() < 4) {
                return null;
            }
            int mcc = Integer.parseInt(operator.substring(0, 3));
            int mnc = Integer.parseInt(operator.substring(3));
            // 中国移动和中国联通获取LAC、CID的方式
            CellLocation location = tm.getCellLocation();
            if (location == null) {
                return null;
            }
            if (!(location instanceof GsmCellLocation)) {
                return null;
            }
            GsmCellLocation cgl = (GsmCellLocation) location;
            int lac = cgl.getLac();
            int cellId = cgl.getCid();
            StringBuilder sb = new StringBuilder(256);
            sb.append("MCC:").append(mcc).append("MNC:").append(mnc).append("LAC:").append(lac).append("CID:").append(cellId);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断给定IP是否为Local IP的接口
     */
    public interface LocalIpFilter {
        /**
         * 判断给定的IP是否为一个合法的Local IP
         *
         * @return true表示给定ip是一个合法的Local IP，否则返回false
         */
        boolean isValidLocalIp(byte[] ip);
    }

    static class NetworkInterfaceImpl {
        Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
            return NetworkInterface.getNetworkInterfaces();
        }
    }

    static class NameSolver {
        InetAddress[] getAllByName(String host) throws UnknownHostException {
            return InetAddress.getAllByName(host);
        }
    }
}
