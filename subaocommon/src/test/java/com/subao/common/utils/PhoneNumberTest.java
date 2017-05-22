package com.subao.common.utils;

import android.annotation.SuppressLint;

import com.subao.common.utils.PhoneNumber.IMSIType;
import com.subao.common.utils.PhoneNumber.PhoneOperator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PhoneNumberTest {

	@Test
	public void testConstructor() {
		new PhoneNumber();
	}

	@Test
	public void testIMSIType() {
		assertFalse(PhoneNumber.isIMSIValid(null));
		assertFalse(PhoneNumber.isIMSIValid(""));
		assertFalse(PhoneNumber.isIMSIValid("234234234"));
		assertTrue(PhoneNumber.isIMSIValid("460004444445555"));
		assertEquals(5, IMSIType.values().length);
		assertEquals(IMSIType.NULL, PhoneNumber.getIMSIType(null));
		assertEquals(IMSIType.NULL, PhoneNumber.getIMSIType(""));
		assertEquals(IMSIType.UNKNOWN, PhoneNumber.getIMSIType("123"));
		assertEquals(IMSIType.CHINA_MOBILE, PhoneNumber.getIMSIType("46000"));
		assertEquals(IMSIType.CHINA_TELECOM, PhoneNumber.getIMSIType("46003"));
		assertEquals(IMSIType.CHINA_UNICOM, PhoneNumber.getIMSIType("46001"));
	}

	@Test
	public void testIsIMSICHinaTelecom() {
		for (int i = 45000; i <= 47999; ++i) {
			String s = Integer.toString(i);
			boolean b = PhoneNumber.isIMSIChinaTelecom(s);
			if (i == 46003 || i == 46005 || i == 46011) {
				assertTrue(b);
			} else {
				assertFalse(b);
			}
		}
		assertFalse(PhoneNumber.isIMSIChinaTelecom(null));
		assertFalse(PhoneNumber.isIMSIChinaTelecom(""));
		assertFalse(PhoneNumber.isIMSIChinaTelecom("1234"));

	}

	@Test
	public void testIsIMSIChinaUnicom(){
		assertFalse(PhoneNumber.isIMSIChinaUnicom(null));
		assertFalse(PhoneNumber.isIMSIChinaUnicom(""));
		assertFalse(PhoneNumber.isIMSIChinaUnicom("12345"));

		for(int i=46000 ; i<=46300 ; ++i){
			String str = Integer.toString(i);
			boolean res = PhoneNumber.isIMSIChinaUnicom(str);
			if(i==46001){
				assertTrue(res);
			}else{
				assertFalse(res);
			}
		}
	}

	@Test
	public void testTelecom4G_JiangSu() {
		// 规则，从460110140000000 至 460110189999999
		for (int i = 13; i <= 19; ++i) {
			for (int j = 0; j <= 9999999; j += 3333) {
				@SuppressLint("DefaultLocale") String imsi = String.format("46011%03d%07d", i, j);
				boolean b = PhoneNumber.isIMSIChinaTelecom4G_JiangSu(imsi);
				if (i < 14 || i > 18) {
					assertFalse(b);
				} else {
					assertTrue(b);
				}
			}
		}
		assertFalse(PhoneNumber.isIMSIChinaTelecom4G_JiangSu(null));
		assertFalse(PhoneNumber.isIMSIChinaTelecom4G_JiangSu("23234"));
		assertFalse(PhoneNumber.isIMSIChinaTelecom4G_JiangSu("4601100000000000000000000000000"));
		assertFalse(PhoneNumber.isIMSIChinaTelecom4G_JiangSu("46011014" + '/' + "000000000000000000000000"));
		assertFalse(PhoneNumber.isIMSIChinaTelecom4G_JiangSu("46011014" + ':' + "000000000000000000000000"));

	}

	@Test
	public void testTelecomNanJingCheck() {
		assertFalse(PhoneNumber.isIMSIChinaTelecom4G_NanJing(null));
		for (int i = 46011013; i <= 46011015; ++i) {
			String prefix = Integer.toString(i);
			for (int j = 0; j <= 999; ++j) {
				@SuppressLint("DefaultLocale") String imsi = String.format("%s%03d8888", prefix, j);
				boolean b = PhoneNumber.isIMSIChinaTelecom4G_NanJing(imsi);
				if (i != 46011014) {
					assertFalse(b);
					continue;
				}
				if (j <= 5) {
					assertTrue(b);
					continue;
				}
				if (j < 78 || j > 677) {
					assertFalse(b);
				}
			}
		}
		for (int i = 770000; i <= 7001000; i += 100) {
			@SuppressLint("DefaultLocale") String imsi = String.format("46011014%07d", i);
			boolean b = PhoneNumber.isIMSIChinaTelecom4G_NanJing(imsi);
			if (i < 780000 || i > 6779999) {
				assertFalse(b);
			} else {
				assertTrue(b);
			}
		}
		//
		for (int i = -1; i < 10; ++i) {
			char ch = (char) (i + '0');
			String imsi = "4601101400" + ch;
			imsi += "0000000000";
			boolean b = PhoneNumber.isIMSIChinaTelecom4G_NanJing(imsi);
			if (i >= 0 && i <= 5) {
				assertTrue(b);
			} else {
				assertFalse(b);
			}
		}
		//
		assertFalse(PhoneNumber.isIMSIChinaTelecom4G_NanJing("46011014abcde234234234324234234"));
	}

	@Test
	public void testIsIMSIChinaMobile() {
		assertFalse(PhoneNumber.isIMSIChinaMobile(null));
		assertFalse(PhoneNumber.isIMSIChinaMobile(""));
		assertFalse(PhoneNumber.isIMSIChinaMobile("46014444"));
		//
        assertFalse(PhoneNumber.isIMSIChinaMobile("46001"));
		assertFalse(PhoneNumber.isIMSIChinaMobile("46003"));
		assertFalse(PhoneNumber.isIMSIChinaMobile("46005"));
		assertFalse(PhoneNumber.isIMSIChinaMobile("46006"));
		assertFalse(PhoneNumber.isIMSIChinaMobile("46008"));
		assertFalse(PhoneNumber.isIMSIChinaMobile("46009"));
		//
		assertTrue(PhoneNumber.isIMSIChinaMobile("46000"));
		assertTrue(PhoneNumber.isIMSIChinaMobile("46002"));
		assertTrue(PhoneNumber.isIMSIChinaMobile("46004"));
		assertTrue(PhoneNumber.isIMSIChinaMobile("46007"));
	}
	
	@Test
	public void testPhoneOperator() {
		assertEquals(5, PhoneOperator.values().length);
		assertEquals(PhoneOperator.INVALID, PhoneNumber.getPhoneOperator(null));
		for (int i = 0; i < 11; ++i) {
			byte[] array = new byte[i];
			for (int j = 0; j < i; ++j) {
				array[j] = '5';
			}
			String phoneNumber = new String(array);
			assertEquals(PhoneOperator.INVALID, PhoneNumber.getPhoneOperator(phoneNumber));
		}
		byte[] array = new byte[11];
		for (int i = 0; i < array.length; ++i) {
			array[i] = '1';
		}
		for (int i = 0; i < array.length; ++i) {
			array[i] = '/';
			String phoneNumber = new String(array);
			assertEquals(PhoneOperator.INVALID, PhoneNumber.getPhoneOperator(phoneNumber));
			array[i] = ':';
			phoneNumber = new String(array);
			assertEquals(PhoneOperator.INVALID, PhoneNumber.getPhoneOperator(phoneNumber));
			array[i] = '1';
		}
		// 首位不为1的
		assertEquals(PhoneOperator.UNKNOWN, PhoneNumber.getPhoneOperator("23888888888"));
		//
		testPrefix(PHONE_NUM_PREFIX_CHINA_MOBILE, PhoneOperator.CHINA_MOBILE);
		testPrefix(PHONE_NUM_PREFIX_CHINA_TELECOM, PhoneOperator.CHINA_TELECOM);
		testPrefix(PHONE_NUM_PREFIX_CHINA_UNICOM, PhoneOperator.CHINA_UNICOM);
		assertEquals(PhoneOperator.UNKNOWN, PhoneNumber.getPhoneOperator("11111111111"));
	}
	
	private static void testPrefix(String[] prefixList, PhoneOperator expected) {
		StringBuilder sb = new StringBuilder(11);
		for (String prefix : prefixList) {
			sb.delete(0, sb.length());
			sb.append(prefix);
			while (sb.length() < 11) {
				sb.append('0');
			}
			assertEquals(expected, PhoneNumber.getPhoneOperator(sb.toString()));
		}
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
