package com.subao.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class RangeTest {

	@Test
	public void test() {
		Range<Integer> r = new Range<Integer>(123, 456);
		assertEquals(123, (int)r.start);
		assertEquals(456, (int)r.end);
	}

}
