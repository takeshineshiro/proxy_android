package com.subao.common.utils;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {
	
	private static String makeString() {
		return new String(new byte[] { '1', '2', '3' });
	}

	@Test
	public void testConstructor() {
		new StringUtils();
	}
	
	@Test
	public void testIsStringEmpty() {
		assertTrue(StringUtils.isStringEmpty(null));
		assertTrue(StringUtils.isStringEmpty(""));
		assertTrue(StringUtils.isStringEmpty(StringUtils.EMPTY));
		assertFalse(StringUtils.isStringEmpty("1"));
	}
	
	@Test
	public void testIsStringEqual() {
		assertTrue(StringUtils.isStringEqual(null, null));
		assertTrue(StringUtils.isStringEqual(StringUtils.EMPTY, StringUtils.EMPTY));
		assertTrue(StringUtils.isStringEqual("123", "123"));
		assertTrue(StringUtils.isStringEqual(makeString(), makeString()));
		assertFalse(StringUtils.isStringEqual(null, StringUtils.EMPTY));
		assertFalse(StringUtils.isStringEqual(StringUtils.EMPTY, null));
	}
	
	@Test
	public void testIsStringSame() {
		assertTrue(StringUtils.isStringSame(null, null));
		assertTrue(StringUtils.isStringSame(StringUtils.EMPTY, StringUtils.EMPTY));
		assertTrue(StringUtils.isStringSame("123", "123"));
		assertTrue(StringUtils.isStringSame(makeString(), makeString()));
		assertFalse(StringUtils.isStringSame("123", null));		
		assertFalse(StringUtils.isStringSame(null, "123"));
		assertTrue(StringUtils.isStringSame(null, StringUtils.EMPTY));
		assertTrue(StringUtils.isStringSame(StringUtils.EMPTY, null));
	}
	
	@Test
	public void testCompare() {
		assertEquals(0, StringUtils.compare(null, null));
		assertEquals(0, StringUtils.compare("", ""));
		assertTrue(StringUtils.compare(null, "") < 0);
		assertTrue(StringUtils.compare("", null) > 0);
		assertTrue(StringUtils.compare("1", "2") < 0);
		assertTrue(StringUtils.compare("2", "1") > 0);
		assertEquals(0, StringUtils.compare("1", "1"));
	}

	@Test
    public void testBytesToString() {
        assertEquals("null", StringUtils.bytesToString(null));
        assertEquals("[0x01, 0x03, 0x05, 0x07, 0x0A, 0x0C, 0x06, 0x08]", StringUtils.bytesToString(new byte[] { 1, 3, 5, 7, 10, 12, 6, 8}));
        assertEquals("[0x01, 0x03, 0x05, 0x07, 0x0A, 0x0C, 0x06, 0x08, ... (Total 9 bytes)]", StringUtils.bytesToString(new byte[] { 1, 3, 5, 7, 10, 12, 6, 8, 15}));
    }

    @Test
    public void objToString() {
        assertEquals("null", StringUtils.objToString(null));
        assertEquals(this.toString(), StringUtils.objToString(this));
    }

    private final byte[] data = new byte[] {
		0x01,
		0x23,
		0x45,
		0x67,
		(byte) 0x89,
		(byte) 0xab,
		(byte) 0xcd,
		(byte) 0xef,
		0x02,
		0x13,
		0x46,
		0x57,
		(byte) 0x8a,
		(byte) 0x9b,
		(byte) 0xce,
		(byte) 0xdf
	};
	private final static String hexLower = "0123456789abcdef021346578a9bcedf";
	private final static String hexUpper = "0123456789ABCDEF021346578A9BCEDF";
	private final static String guidLower = "01234567-89ab-cdef-0213-46578a9bcedf";
	private final static String guidUpper = "01234567-89AB-CDEF-0213-46578A9BCEDF";
	private final static String guidMix = "01234567-89ab-CDeF-0213-46578A9BcEDF";

	@Test
	public void testToHexString() {
		assertEquals("", StringUtils.toHexString(null, false));
		assertEquals("", StringUtils.toHexString(data, 0, 0, false));
		assertEquals("", StringUtils.toHexString(data, 1, 0, false));
		assertEquals("", StringUtils.toHexString(new byte[0], false));
		assertEquals("", StringUtils.toHexString(data, 16, 16, false));
		assertEquals("", StringUtils.toHexString(data, 17, 17, false));
		//
		assertEquals(hexLower, StringUtils.toHexString(data, false));
		assertEquals(hexUpper, StringUtils.toHexString(data, true));
		for (int charCase = 0; charCase < 2; ++charCase) {
			boolean upperCase = charCase != 0;
			String hex = upperCase ? hexUpper : hexLower;
			for (int end = 0; end <= 16; ++end) {
				for (int start = 0; start <= end; ++start) {
					String expect = hex.substring(start * 2, end * 2);
					assertEquals(expect, StringUtils.toHexString(data, start, end, upperCase));
				}
			}
		}
	}

	@Test
	public void testToGuidString() {
		assertEquals("", StringUtils.toGUIDString(null, false));
		assertEquals("", StringUtils.toGUIDString(new byte[15], false));
		assertEquals(guidLower, StringUtils.toGUIDString(data, false));
		assertEquals(guidUpper, StringUtils.toGUIDString(data, true));
	}

	@Test
	public void testGuidToByteArray() {
		assertNull(StringUtils.guidStringToByteArray(null));
		assertNull(StringUtils.guidStringToByteArray(null, 0, 36));
		assertNull(StringUtils.guidStringToByteArray(guidLower, 0, 35));
		assertNull(StringUtils.guidStringToByteArray(guidLower, 0, 37));
		//
		assertTrue(Arrays.equals(data, StringUtils.guidStringToByteArray(guidLower)));
		assertTrue(Arrays.equals(data, StringUtils.guidStringToByteArray(guidUpper)));
		assertTrue(Arrays.equals(data, StringUtils.guidStringToByteArray(guidMix)));

		assertNull(StringUtils.guidStringToByteArray("asdfjk;lkj"));
		assertNull(StringUtils.guidStringToByteArray("01234567-89ab-CDeF-0213-46578A9BcEDJ"));
		assertNull(StringUtils.guidStringToByteArray("/1234567-89ab-CDeF-0213-46578A9BcEDJ"));
		assertNull(StringUtils.guidStringToByteArray(":1234567-89ab-CDeF-0213-46578A9BcEDJ"));
		assertNull(StringUtils.guidStringToByteArray("@1234567-89ab-CDeF-0213-46578A9BcEDJ"));
		assertNull(StringUtils.guidStringToByteArray("[1234567-89ab-CDeF-0213-46578A9BcEDJ"));
		assertNull(StringUtils.guidStringToByteArray("(1234567-89ab-CDeF-0213-46578A9BcEDJ"));
		assertNull(StringUtils.guidStringToByteArray("g1234567-89ab-CDeF-0213-46578A9BcEDJ"));


		
	}
	
	@Test
	public void testVersionCompare() {
		
		String[] small = new String[] {
			"1.2.3", "1.2.4",
			"1.2.3", "1.2.4.0",
			"1.2.3", "1.2.3.0",
			"2.2.3", "3",
			"", "1.2.4",
			"1.2.3", "2",
			"1.2.3.1", "1.2.4",
			"1.2.3.6", "1.2.4",
			"1", "1.1",
			"1.1", "1.1.1",
			"1.1.1", "1.1.1.1",
			"9.9.9.9", "10",
			"1", "2",
			null, "1"
		};
		
		String[] equal = new String[] {
			null, null,
			"", "",
			"1", "1",
			"1.2", "1.2",
			"1.3", "1.3",
			"1.2.45.123", "1.2.45.123",
		};
		
		for (int i = 0; i < small.length - 1; ) {
			String s1 = small[i++];
			String s2 = small[i++];
			assertTrue(StringUtils.compareVersion(s1, s2) < 0);
			assertTrue(StringUtils.compareVersion(s2, s1) > 0);
		}
		
		for (int i = 0; i < equal.length - 1;) {
			String s1 = equal[i++];
			String s2 = equal[i++];
			assertEquals(0,  StringUtils.compareVersion(s1, s2));
			assertEquals(0, StringUtils.compareVersion(s2, s1));
		}

	}
	
	///////////////////////////
	
	@Test
	public void testSerializeList() throws IOException {
		List<String> list = Arrays.asList("123", "abc", "", "a,b", null, ",a", "b,", "\\a", "a\\", ",", "\\", "abc");
		StringWriter writer = null;
		StringReader reader = null;
		try {
			writer = new StringWriter(128);
			StringUtils.serializeList(writer, list);
			String s = writer.toString();
			assertEquals("123,abc,a\\,b,\\,a,b\\,,\\\\a,a\\\\,\\,,\\\\,abc", s);
			//
			List<String> list2 = new ArrayList<String>(8);
			reader = new StringReader(s);
			int count = StringUtils.deserializeList(reader, list2);
			assertEquals(10, count);
			assertEquals(count, list2.size());
			int j = 0;
			for (int i = 0; i < list2.size(); ++i) {
				String s2 = list2.get(i);
				String s1 = list.get(j++);
				while (StringUtils.isStringEmpty(s1)) {
					s1 = list.get(j++);
				}
				assertEquals(s1, s2);
			}
		} finally {
			com.subao.common.Misc.close(writer);
			com.subao.common.Misc.close(reader);
		}
	}
	
	private static class Param {
		public final String source;
		public final String[] expected;
		public Param(String source, String[] expected) {
			this.source = source;
			this.expected = expected;
		}
	}
	
	private static final Param[] PARAM_LIST = new Param[] {
		new Param("", null),
		new Param(",", null),
		new Param("abc", new String[] {"abc"}),
		new Param(",abc", new String[] {"abc"}),
		new Param("abc,", new String[] {"abc"}),
		new Param(",abc,", new String[] {"abc"}),
		new Param("abc,,\\", new String[] {"abc", "\\"}),
		new Param("abc,,\\\0", new String[] {"abc", "\\\0"}),
		new Param("abc,\\,\\", new String[] {"abc", ",\\"}),
		new Param("abc,\\,,\\\\", new String[] {"abc", ",", "\\"}),
		new Param("abc,123,abcd efg,h 1 2 3", new String[] {"abc", "123", "abcd efg", "h 1 2 3"}),

	};
	
	@Test
	public void testDeserializeList() throws IOException {
		List<String> list = new ArrayList<String>(32);
		for (Param param : PARAM_LIST) {
			StringReader reader = new StringReader(param.source);
			try {
				list.clear();
				int count = StringUtils.deserializeList(reader, list);
				assertEquals(list.size(), count);
				if (param.expected == null || param.expected.length == 0) {
					assertEquals(0, count);
					assertTrue(list.isEmpty());
				} else {
					assertEquals(param.expected.length, count);
					for (int i = 0; i < count; ++i) {
						assertEquals(param.expected[i], list.get(i));
					}
				}
			} finally {
				com.subao.common.Misc.close(reader);
			}
		}
	}

	@Test
	public void testParsePositiveInteger() {
		assertEquals(123, StringUtils.parsePositiveInteger("123"));
		assertEquals(0, StringUtils.parsePositiveInteger(""));
		assertEquals(-1, StringUtils.parsePositiveInteger(" "));
		assertEquals(-1, StringUtils.parsePositiveInteger(":"));
	}

}
