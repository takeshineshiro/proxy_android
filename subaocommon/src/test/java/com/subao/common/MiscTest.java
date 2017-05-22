package com.subao.common;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MiscTest {

	private static class ClosableImpl implements Closeable {

		public boolean closeInvoked;
		public boolean throwExceptionWhenClose;
        public boolean throwRuntimeExceptionWhenClose;

		@Override
		public void close() throws IOException {
			closeInvoked = true;
			if (throwExceptionWhenClose) {
				throw new IOException();
			}
            if (throwRuntimeExceptionWhenClose) {
                throw new RuntimeException();
            }
		}
	}
	
	@Test
	public void testConstructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		RoboBase.testPrivateConstructor(Misc.class);
	}

	@Test
	public void close() {
		Misc.close(null);
		ClosableImpl c = new ClosableImpl();
		assertFalse(c.closeInvoked);
		Misc.close(c);
		assertTrue(c.closeInvoked);
        //
        c = new ClosableImpl();
        c.throwRuntimeExceptionWhenClose = true;
        Misc.close(c);
        //
        c = new ClosableImpl();
        c.throwExceptionWhenClose = true;
        Misc.close(c);
	}
	
	@Test
	public void testReadStreamToByteArray() throws IOException {
		byte[] array = new byte[2000];
		for (int i = 0; i < array.length; ++i) {
			array[i] = (byte) (i & 0xff);
		}
		InputStream input = new ByteArrayInputStream(array);
		byte[] result = Misc.readStreamToByteArray(input);
		assertEquals(array.length, result.length);
		for (int i = 0; i < array.length; ++i) {
			assertEquals(array[i], result[i]);
		}
		assertNull(Misc.readStreamToByteArray(null));
	}

	@Test
	public void testIsEquals() {
		assertFalse(Misc.isEquals("", null));
		assertFalse(Misc.isEquals(null, ""));
		assertTrue(Misc.isEquals(null, null));
		String s1 = "abcd";
		String s2 = "a" + "bcd";
		String s3 = "1234";
		assertTrue(Misc.isEquals(s1, s2));
		assertFalse(Misc.isEquals(s1, s3));
	}

	@Test
	public void testIsApplicationsUID() {
		int[] falseList = new int[] {
			100,
			1000,
			9999,
			20000,
			20001
		};
		for (int uid : falseList) {
			assertFalse(Misc.isApplicationsUID(uid));
		}
		for (int uid = 10000; uid < 20000; uid += 1000) {
			assertTrue(Misc.isApplicationsUID(uid));
		}
		assertTrue(Misc.isApplicationsUID(19999));
	}

	@Test
	public void testExtractLong() {
		String[] strings = new String[] {
			"",
			"abc123abc",
			"a0012",
			"a0012a",
		};
		long[] values = new long[] {
			-1,
			123,
			12,
			12,
		};
		long[] values2 = new long[] {
			-1,
			123,
			1,
			12,
		};
		for (int i = 0; i < strings.length; ++i) {
			byte[] buf = strings[i].getBytes();
			long v = Misc.extractLong(buf, 0, buf.length, -1);
			assertEquals(values[i], v);
			v = Misc.extractLong(buf, 0, buf.length + 1, -1);
			assertEquals(values[i], v);
			v = Misc.extractLong(buf, 0, buf.length - 1, -1);
			assertEquals(values2[i], v);
		}
	}

	@Test
	public void isFloatEquals() {
		Float f = Float.valueOf(1.2f);
		assertFalse(Misc.isEquals(f, null));
		assertFalse(Misc.isEquals(null, f));
		assertTrue(Misc.isEquals(null, null));
		//
		Float f2 = Float.valueOf(1.2f);
		assertTrue(Misc.isEquals(f, f));
		assertTrue(Misc.isEquals(f, f2));
		assertFalse(Misc.isEquals(f, Float.valueOf(2f)));
	}

    @Test
    public void transNullValue() {
        assertEquals("hello", Misc.transNullValue(null, "hello"));
        assertEquals("hello", Misc.transNullValue("hello", "world"));
        assertEquals("", Misc.transNullValue("", "world"));
    }

    @Test
    public void encodeUrl() {
        assertEquals("", Misc.encodeUrl(null));
        assertEquals("", Misc.encodeUrl(""));
        assertEquals("%3D", Misc.encodeUrl("="));
    }

    @Test
    public void encodeUrlException() {
        assertEquals("=", Misc.encodeUrl("=", "Not exists"));
    }

}
