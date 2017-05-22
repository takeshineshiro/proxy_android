package com.subao.common.msg;

import com.subao.common.RoboBase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MessageSessionIdTest extends RoboBase {

	@Test
	public void testConstructor() {
		MessageSessionId msg = new MessageSessionId("123");
		assertEquals("123", msg.id);
	}
	
	@Test
	public void testEquals() {
		MessageSessionId msg = new MessageSessionId("123");
		assertTrue(msg.equals(msg));
		assertFalse(msg.equals(null));
		assertFalse(msg.equals(this));
		assertTrue(msg.equals(new MessageSessionId("123")));
		assertFalse(msg.equals(new MessageSessionId("456")));

	}

}
