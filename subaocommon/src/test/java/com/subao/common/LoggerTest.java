package com.subao.common;

import android.util.Log;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoggerTest extends RoboBase {


	@Test
	public void testFunc() {
		String tag = "tag";
		String msg = "msg";
		Logger.v(tag, msg);
		Logger.d(tag, msg);
		Logger.i(tag, msg);
		Logger.w(tag, msg);
		Logger.e(tag, msg);
		//
		Throwable t = new Throwable();
		Logger.setLoggableChecker(new Logger.LoggableChecker() {
			@Override
			public boolean isLoggable(String tag, int level) {
				return true;
			}
		});
		Logger.w(tag, msg, t);
		Logger.setLoggableChecker(null);
	}

	@Test
	public void testConstructor() {
		new Logger();
	}

	@Test
	public void setLoggableChecker() {
		LoggerCheckerImpl newChecker = new LoggerCheckerImpl();
		Logger.setLoggableChecker(newChecker);
		try {
			assertEquals(Logger.getLoggableChecker(), newChecker);
			String tag = "TRUE";
			assertTrue(Logger.isLoggableDebug(tag));
			assertEquals(newChecker.tag, tag);
			assertEquals(newChecker.level, Log.DEBUG);
			//
			tag = "AAA";
			assertFalse(Logger.isLoggable(tag, 1));
			assertEquals(newChecker.tag, tag);
			assertEquals(newChecker.level, 1);
		} finally {
			Logger.setLoggableChecker(null);
		}
	}

	private static class LoggerCheckerImpl implements Logger.LoggableChecker {

		public String tag;
		public int level;

		@Override
		public boolean isLoggable(String tag, int level) {
			this.tag = tag;
			this.level = level;
			return "TRUE".equals(tag);
		}
	}
}
