package com.subao.common.utils;


public class PhoneNumber {
	
	public enum IMSIType {
		UNKNOWN("UNKNOWN"),
		NULL("NULL"),
		CHINA_MOBILE("CHAINA_MOBILE"),
		CHINA_TELECOM("CHINA_TELECOM"),
		CHINA_UNICOM("CHINA_UNICOM");
		
		public final String strValue;
		
		IMSIType(String strValue) {
			this.strValue = strValue;
		}
	}
	
	public static IMSIType getIMSIType(String imsi) {
		if (imsi == null || imsi.length() == 0) {
			return IMSIType.NULL;
		}
		if (isIMSIChinaTelecom(imsi)) {
			return IMSIType.CHINA_TELECOM;
		}
		if(isIMSIChinaUnicom(imsi)){
			return IMSIType.CHINA_UNICOM ;
		}
		if (isIMSIChinaMobile(imsi)) {
			return IMSIType.CHINA_MOBILE;
		}
		return IMSIType.UNKNOWN;
	}
	
	public static boolean isIMSIValid(String imsi) {
		return imsi != null && imsi.length() >= 15;
	}
	
	public static boolean isIMSIChinaMobile(String imsi) {
		if (imsi == null || imsi.length() < 5) {
			return false;
		}
		if (!imsi.startsWith("4600")) {
			return false;
		}
        switch (imsi.charAt(4)) {
		case '0':
		case '2':
		case '4':
		case '7':
			return true;
		default:
			return false;
		}
	}

	/**
	 * 判断给定的IMSI号是否为中国联通
	 */
	public static boolean isIMSIChinaUnicom(String imsi) {
		/* 规则：以46001 为前缀 */
		if (imsi == null || imsi.length() < 5) {
			return false;
		}
		if (imsi.startsWith("46001")) {
			return true;
		}

		return false ;
	}

	/**
	 * 判断给定的IMSI号是否为中国电信
	 */
	public static boolean isIMSIChinaTelecom(String imsi) {
		/* 规则：以46003 46005 或 46011 为前缀 */
		if (imsi == null || imsi.length() < 5) {
			return false;
		}
		if (!imsi.startsWith("460")) {
			return false;
		}
		char ch = imsi.charAt(3);
		if (ch == '0') {
			ch = imsi.charAt(4);
			return ch == '3' || ch == '5';
		} else if (ch == '1') {
			return imsi.charAt(4) == '1';
		} else {
			return false;
		}
	}

	/**
	 * 判断给定的IMSI号是否为南京电信4G
	 */
	public static boolean isIMSIChinaTelecom4G_NanJing(String imsi) {
		// 规则，以下列号码为前缀
		// 46011014000 46011014001 46011014002 46011014003
		// 46011014004 46011014005
		// 或从 46011014 0780000 到 46011014 6779999
		if (!isIMSIValid(imsi)) {
			return false;
		}
		if (!imsi.startsWith("46011014")) {
			return false;
		}
		if (imsi.charAt(8) == '0' && imsi.charAt(9) == '0') {
			char ch = imsi.charAt(10);
			return ch >= '0' && ch <= '5';
		}
		try {
			int value = Integer.parseInt(imsi.substring(8, 15));
			return value >= 780000 && value <= 6779999;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * 判断给定IMSI号是否为江苏电信4G
	 */
	public static boolean isIMSIChinaTelecom4G_JiangSu(String imsi) {
		// 规则，从460110140000000 至 460110189999999
		if (!isIMSIValid(imsi)) {
			return false;
		}
		if (!imsi.startsWith("4601101")) {
			return false;
		}
		int i = 7;
		char ch = imsi.charAt(i);
		if (ch < '4' || ch > '8') {
			return false;
		}
		while (i < imsi.length()) {
			ch = imsi.charAt(i);
			if (ch < '0' || ch > '9') {
				return false;
			}
			++i;
		}
		return true;
	}

	/**
	 * 运营商
	 */
	public enum PhoneOperator {
		/** 非法的、无效的（比如不是11位数字） */
		INVALID,
		/** 是11位数字，但不属于中国三大运营商的 */
		UNKNOWN,
		/** 中移动 */
		CHINA_MOBILE,
		/** 中国联通 */
		CHINA_UNICOM,
		/** 中国电信 */
		CHINA_TELECOM,
	}

	/**
	 * 判断给定手机号码的运营商
	 * 
	 * @param phoneNumber
	 *            11位手机号码
	 */
	public static PhoneOperator getPhoneOperator(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() != 11) {
			return PhoneOperator.INVALID;
		}
		for (int i = phoneNumber.length() - 1; i >= 0; --i) {
			char ch = phoneNumber.charAt(i);
			if (ch < '0' || ch > '9') {
				return PhoneOperator.INVALID;
			}
		}
		char ch = phoneNumber.charAt(0);
		if (ch != '1') {
			return PhoneOperator.UNKNOWN;
		}
		if (isStartsWith(phoneNumber, PHONE_NUM_PREFIX_CHINA_MOBILE)) {
			return PhoneOperator.CHINA_MOBILE;
		}
		if (isStartsWith(phoneNumber, PHONE_NUM_PREFIX_CHINA_UNICOM)) {
			return PhoneOperator.CHINA_UNICOM;
		}
		if (isStartsWith(phoneNumber, PHONE_NUM_PREFIX_CHINA_TELECOM)) {
			return PhoneOperator.CHINA_TELECOM;
		}
		return PhoneOperator.UNKNOWN;
	}
	
	private static boolean isStartsWith(String phoneNumber, String[] prefixList) {
		for (String prefix : prefixList) {
			if (phoneNumber.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
	
	private static final String[] PHONE_NUM_PREFIX_CHINA_MOBILE = {
		// GSM
		"1340", "1341", "1342", "1343", "1344", "1345", "1346", "1347", "1348",
		"135", "136", "137", "138", "139", "150", "151", "152", "158", "159", "182", "183", "184",
		// 3G
		"157", "187", "188",
		// 3G 上网卡
		"147",
		// 4G
		"178",
		// 虚拟运营商
		"1705"
	};
	
	private static final String[] PHONE_NUM_PREFIX_CHINA_UNICOM = {
		// 2G号段（GSM网络）
		"130", "131", "132", "155", "156",
		// 3G号段（WCDMA网络）
		"185", "186",
		// 3G上网卡
		"145",
		// 4G号段 
		"176", "185",
		// 虚拟运营商专属号段
		"1709"
	};
	
	private static final String[] PHONE_NUM_PREFIX_CHINA_TELECOM = {
		// 2G/3G号段CDMA 2000网络）
		"133", "153", "180", "181", "189",
		// 3G上网卡，没有
		// 4G号段 
		"177",
		// 虚拟运营商专属号段
		"1700",
	};
}
