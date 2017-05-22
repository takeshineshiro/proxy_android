package com.subao.common.thread;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ThreadPool {

	private static Executor executor;
	
	public static synchronized Executor getExecutor() {
		if (executor == null) {
			executor = Executors.newCachedThreadPool();
		}
		return executor;
	}
}
