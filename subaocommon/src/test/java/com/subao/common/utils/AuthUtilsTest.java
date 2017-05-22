package com.subao.common.utils;

import android.annotation.SuppressLint;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthUtilsTest {
	
	private static final int[] YEARS = new int[] { 1999, 2001 };
	private static final int[] MONTHS = new int[] { 3, 10 };
	private static final int[] DAYS = new int[] { 3, 13 };
	private static final int[] HOURS = new int[] { 0, 3, 13, 20 };
	private static final int[] MINUTES = new int[] { 0, 4, 14 };
	private static final int[] SECONDS = new int[] { 0, 5, 15 };

	private void doTestGenerateTimestamp(TimeZone timeZone) {
		Calendar c = Calendar.getInstance(timeZone);
        for (int YEAR : YEARS) {
            for (int month = 0; month < MONTHS.length; ++month) {
                for (int day = 0; day < DAYS.length; ++day) {
                    for (int hour = 0; hour < HOURS.length; ++hour) {
                        for (int minute = 0; minute < MINUTES.length; ++minute) {
                            for (int second = 0; second < SECONDS.length; ++second) {
                                c.set(YEAR, MONTHS[month], DAYS[day], HOURS[hour], MINUTES[minute], SECONDS[second]);
                                String timestamp = AuthUtils.generateTimestamp(c);
                                assertEquals("yyyy-MM-ddTHH:mm:ssZ".length(), timestamp.length());
                                //
                                Calendar utc = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_UTC);
                                utc.setTimeInMillis(c.getTimeInMillis());
                                @SuppressLint("DefaultLocale") String expected = String.format("%04d-%02d-%02dT%02d:%02d:%02dZ",
                                    utc.get(Calendar.YEAR), utc.get(Calendar.MONTH) + 1, utc.get(Calendar.DAY_OF_MONTH),
                                    utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE), utc.get(Calendar.SECOND));
                                assertEquals(expected, timestamp);
                            }
                        }
                    }
                }
            }
        }
	}
	
	@Test
	public void testGenerateTimestampUTC() {
		doTestGenerateTimestamp(CalendarUtils.TIME_ZONE_OF_UTC);
	}
	
	@Test
	public void testGenerateTimestampCST() {
		doTestGenerateTimestamp(CalendarUtils.TIME_ZONE_OF_BEIJING);
	}
	
	@Test
	public void testGenerateTimestamp() {
		String s = AuthUtils.generateTimestamp();
		assertNotNull(s);
	}

	@Test
	public void testGenerateNonce() {
		String s = AuthUtils.generateNonce();
		assertTrue(s.length() > 4);
	}
	
	@Test
	public void testGenerateDigest() throws NoSuchAlgorithmException {
		byte[] digest = AuthUtils.generateSHA1("123", "abc");
		assertEquals(20, digest.length);
		digest = AuthUtils.generateMD5("123", "abc");
		assertEquals(16, digest.length);
		//
		digest = AuthUtils.generateMD5("hello");
		assertEquals("5d41402abc4b2a76b9719d911017c592", StringUtils.toHexString(digest, false));
		digest = AuthUtils.generateSHA1("hello");
		assertEquals("AAF4C61DDCC5E8A2DABEDE0F3B482CD9AEA9434D", StringUtils.toHexString(digest, true));
	}
	
	@Test
	public void testConstructor() {
		new AuthUtils();
	}
}
