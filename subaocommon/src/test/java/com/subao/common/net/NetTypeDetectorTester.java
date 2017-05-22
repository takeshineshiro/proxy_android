package com.subao.common.net;

import static org.junit.Assert.*;

import org.junit.Test;

import com.subao.common.net.NetTypeDetector.NetType;

public class NetTypeDetectorTester {

	@Test
	public void test() {
		assertEquals(6, NetType.values().length);
		assertEquals(-1, NetType.DISCONNECT.value);
		assertEquals(0, NetType.UNKNOWN.value);
		assertEquals(1, NetType.WIFI.value);
		assertEquals(2, NetType.MOBILE_2G.value);
		assertEquals(3, NetType.MOBILE_3G.value);
		assertEquals(4, NetType.MOBILE_4G.value);
	}

}
