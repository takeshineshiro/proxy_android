package com.subao.common.net;

import android.annotation.SuppressLint;

import java.nio.ByteOrder;

@SuppressLint("DefaultLocale") 
public class IPv4 {

	private static final String FORMAT = "%d.%d.%d.%d";

	public static final boolean lton_need_swap_byte_order;

	static {
		// 如果本地是小端序，则在本机转网络序时，需要转换字节序
		lton_need_swap_byte_order = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);
	}

	public static int htonl(int value) {
		if (lton_need_swap_byte_order) {
			return Integer.reverseBytes(value);
		} else {
			return value;
		}
	}

	public static int ntohl(int value) {
		return htonl(value);
	}

	/**
	 * 将一个用网络序uint32表示的IPv4转换成字符串输出
	 * 
	 * @param ip
	 *            用32位整型表示的IP
	 * @return 形如“xx.xxx.x.xx”的IPv4字串
	 */
	public static String ipToString(int ip) {
		int a = (0xff & ip);
		int b = (0xff & (ip >> 8));
		int c = (0xff & (ip >> 16));
		int d = (0xff & (ip >> 24));
		return String.format(FORMAT, d, c, b, a);
	}

	/**
	 * 将一个用网络序byte数组表示的IPv4转换成字符串输出
	 * 
	 * @param ip
	 *            用32位整型表示的IP
	 * @return 形如“xx.xxx.x.xx”的IPv4字串
	 */
	public static String ipToString(byte[] ip) {
		if (ip == null || ip.length != 4) {
			return "";
		}
		return String.format(FORMAT, (0xff & ip[0]), (0xff & ip[1]), (0xff & ip[2]), (0xff & ip[3]));
	}

	/**
	 * 将一个四字节的IP地址转换成32位整型（网络序）
	 * 
	 * @param addr
	 *            四字节IP地址
	 * @return 32位整型（网络序）
	 */
	public static int ipToInt(byte[] addr) {
		if (addr == null || addr.length != 4) {
			return -1;
		}
		return ((addr[0] & 0xff) << 24)
			| ((addr[1] & 0xff) << 16)
			| ((addr[2] & 0xff) << 8)
			| (addr[3] & 0xff);
	}

	/**
	 * 将一个四字节的IP地址转换成32位整型（网络序）
	 *
     * @return 32位整型（网络序）
     */
	public static int ipToInt(int a, int b, int c, int d) {
		return ((a & 0xff) << 24)
			| ((b & 0xff) << 16)
			| ((c & 0xff) << 8)
			| (d & 0xff);
	}

	public static byte[] ipToBytes(int a, int b, int c, int d) {
		return new byte[] {
			(byte) a,
			(byte) b,
			(byte) c,
			(byte) d
		};
	}

	public static byte[] ipToBytes(int ip) {
		return ipToBytes(
			(0xff & (ip >> 24)), (0xff & (ip >> 16)),
			(0xff & (ip >> 8)), (0xff & ip));
	}
	
	public static byte[] parseIp(String ip) {
		if (ip == null) {
			return null;
		}
		if (ip.length() < 7) {
			return null;
		}
		byte[] result = new byte[4];
		int idx = 0;
		int end = ip.length();
		int i = 0;
		int value = -1;
		while (idx < 4 && i < end) {
			char ch = ip.charAt(i++);
			if (ch == '.') {
				result[idx++] = (byte)(value & 0xff);
				value = -1;
			} else if (ch < '0' || ch > '9') {
				return null;
			} else {
                int n = ch - '0';
                if (value == -1) {
                    value = n;
                } else {
                    value = value * 10 + n;
                }
				if (value > 255) {
					return null;
				}
			}
		}
		if (idx != 3 || value == -1) {
			return null;
		}
		result[3] = (byte)(value & 0xff);
		return result;
	}
	
	public static boolean reverse(byte[] ip) {
		if (ip == null) {
			throw new NullPointerException();
		}
		if (ip.length != 4) {
			return false;
		}
		byte b = ip[0];
		ip[0] = ip[3];
		ip[3] = b;
		//
		b = ip[1];
		ip[1] = ip[2];
		ip[2] = b;
		return true;
	}
}
