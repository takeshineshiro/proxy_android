package com.subao.common;

import android.util.Log;

public class Logger {

	private static LoggableChecker loggableChecker = new DefaultLoggableChecker();

	public static synchronized LoggableChecker setLoggableChecker(LoggableChecker loggableChecker) {
		LoggableChecker old = Logger.loggableChecker;
        Logger.loggableChecker = (loggableChecker == null)
			? new DefaultLoggableChecker()
			: loggableChecker;
        return old;
	}

	public static LoggableChecker getLoggableChecker() {
		return loggableChecker;
	}

	public static void println(String tag, int level, String msg) {
		if (msg != null) {
			if (isLoggable(tag, level)) {
				Log.println(level, tag, msg);
			}
		}
	}

	public static boolean isLoggable(String tag, int level) {
		return loggableChecker.isLoggable(tag, level);
	}

	public static boolean isLoggableDebug(String tag) {
		return isLoggable(tag, Log.DEBUG);
	}

	public static void v(String tag, String msg) {
		println(tag, Log.VERBOSE, msg);
	}

	public static void d(String tag, String msg) {
		println(tag, Log.DEBUG, msg);
	}

	public static void i(String tag, String msg) {
		println(tag, Log.INFO, msg);
	}

	public static void w(String tag, String msg) {
		println(tag, Log.WARN, msg);
	}

	public static void w(String tag, String msg, Throwable t) {
		if (isLoggable(tag, Log.WARN)) {
			Log.w(tag, msg, t);
		}
	}

	public static void e(String tag, String msg) {
		println(tag, Log.ERROR, msg);
	}

	public interface LoggableChecker {
		boolean isLoggable(String tag, int level);
	}

	private static class DefaultLoggableChecker implements LoggableChecker {

		@Override
		public boolean isLoggable(String tag, int level) {
			return Log.isLoggable(tag, level);
		}
	}

}
