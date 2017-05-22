package com.subao.common.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;


public class AuthUtils {

	private static StringBuilder appendNum(StringBuilder sb, int value) {
		if (value < 10) {
			sb.append('0');
			sb.append((char)(value + '0'));
		} else {
			sb.append(value);
		}
		return sb;
	}
	
	/**
	 * 将当前时刻，格式化为 "yyyy-MM-dd'T'HH:mm:ss'Z'" 的字串
	 */
	public static String generateTimestamp() {
		Calendar now = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_UTC);
		return generateTimestamp(now);
	}
	
	/**
	 * 将指定的时刻，格式化为 "yyyy-MM-dd'T'HH:mm:ss'Z'" 的字串
	 */
	public static String generateTimestamp(Calendar c) {
		if (c.getTimeZone().getRawOffset() != 0) {
			long ms = c.getTimeInMillis();
			c = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_UTC);
			c.setTimeInMillis(ms);
		}
		StringBuilder sb = new StringBuilder(64);
		sb.append(c.get(Calendar.YEAR)).append('-');
		appendNum(sb, c.get(Calendar.MONTH) + 1).append('-');
		appendNum(sb, c.get(Calendar.DAY_OF_MONTH));
		sb.append('T');
		appendNum(sb, c.get(Calendar.HOUR_OF_DAY)).append(':');
		appendNum(sb, c.get(Calendar.MINUTE)).append(':');
		appendNum(sb, c.get(Calendar.SECOND));
		sb.append('Z');
		return sb.toString();
	}
	
	/**
	 * 生成随机的字符串
	 */
	public static String generateNonce() {
		return Long.toHexString((long)(Math.random() * Integer.MAX_VALUE)) + Long.toHexString(System.currentTimeMillis());
	}
	
	private static byte[] generateDigest(String algorithm, String... params) throws NoSuchAlgorithmException {
		StringBuilder sb = new StringBuilder(512);
		for (String s : params) {
			sb.append(s);
		}
		return MessageDigest.getInstance(algorithm).digest(sb.toString().getBytes());
	}
	
	/**
	 * 根据输入的各字符串，生成SHA1签名
	 * @throws NoSuchAlgorithmException 
	 */
	public static byte[] generateSHA1(String ... params) throws NoSuchAlgorithmException {
		return generateDigest("SHA1", params);
	}

	/**
	 * 根据输入的各字符串，生成MD5签名
	 * @throws NoSuchAlgorithmException 
	 */
	public static byte[] generateMD5(String ... params) throws NoSuchAlgorithmException {
		return generateDigest("MD5", params);
	}
}
