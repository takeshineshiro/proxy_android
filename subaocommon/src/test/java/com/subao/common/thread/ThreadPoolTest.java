package com.subao.common.thread;

import static org.junit.Assert.*;

import org.junit.Test;

public class ThreadPoolTest {

	@Test
	public void test() {
		new ThreadPool();
		assertNotNull(ThreadPool.getExecutor());
	}

}
