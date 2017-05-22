package com.subao.common.net;

import android.annotation.SuppressLint;

import org.junit.Test;

import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressLint("DefaultLocale") 
public class IPv4Test {
	
	private static final int[] VALUES = new int[] {
		-300, -255, -1, 0, 128, 255, 256, 300,
	};
    private byte[] mIp = IPv4.parseIp("192.168.8.1");

    @Test
	public void testParseIp() {
		for (int a : VALUES) {
			for (int b : VALUES) {
				for (int c : VALUES) {
					for (int d : VALUES) {
						String ip = String.format("%d.%d.%d.%d", a, b, c, d);
						byte[] bytes = IPv4.parseIp(ip);
						if (a < 0 || b < 0 || c < 0 || d < 0 || a > 255 || b > 255 || c > 255 || d > 255) {
							assertNull(bytes);
						} else {
							assertNotNull(bytes);
							assertEquals(4, bytes.length);
							assertEquals(a, bytes[0] & 0xff);
							assertEquals(b, bytes[1] & 0xff);
							assertEquals(c, bytes[2] & 0xff);
							assertEquals(d, bytes[3] & 0xff);
						}
					}
				}
			}
		}
	}
	
	@Test
	public void testParseInvalidIp() {
        assertNull(IPv4.parseIp(null));
		assertNull(IPv4.parseIp(""));
		assertNull(IPv4.parseIp("111.222.33"));
        assertNull(IPv4.parseIp("111.222.33."));
        assertNull(IPv4.parseIp("111.222.3.-"));
		assertNull(IPv4.parseIp("111.222.32.5.5"));
		assertNull(IPv4.parseIp("111.222"));
		assertNull(IPv4.parseIp("111.222.33.4."));
		assertNull(IPv4.parseIp("11."));
		assertNull(IPv4.parseIp("12"));
		assertNull(IPv4.parseIp("122.222.333"));
		assertNull(IPv4.parseIp("111.2.3.444"));
	}

    @Test(expected = NullPointerException.class)
    public void testReverse() {
        boolean reverse = IPv4.reverse(mIp);
        assertTrue(reverse);
        IPv4.reverse(null);
    }

    @Test
    public void testIpToString() {
        assertNotNull(IPv4.ipToString(mIp));
        assertEquals("", IPv4.ipToString(null));
    }

    @Test
    public void testIpToInt() {
        assertNotNull(IPv4.ipToInt(mIp));
        assertEquals(-1, IPv4.ipToInt(null));
    }

    @Test
    public void testIpToBytes() {
        assertNotNull(IPv4.ipToBytes(IPv4.ipToInt(mIp)));
    }

    @Test
    public void ntohl() {
        int value = IPv4.ntohl(0x12345678);
        int expected;
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            expected = 0x78563412;
        } else {
            expected = 0x12345678;
        }
        assertEquals(expected, value);
    }

    @Test
    public void byteOrder() {
        boolean expected;
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            expected = true;
        } else {
            expected = false;
        }
        assertEquals(expected, IPv4.lton_need_swap_byte_order);
    }

    @Test
    public void ipToString() {
        String s = IPv4.ipToString(-1471553414);
        if (IPv4.lton_need_swap_byte_order) {
            assertEquals("168.73.224.122", s);
        } else {
            assertEquals("122.224.73.168", s);
        }
    }
}
