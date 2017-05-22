package com.subao.common.thread;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SerialExecutorTest {
	
	private static int currentId;

	private static class R implements Runnable {
		
		private final int id;
		
		public R(int id) {
			this.id = id;
		}
		
		@Override
		public void run() {
			assertEquals(currentId, id);
			++currentId;
		}
	}

	@Test
	public void test() {
		SerialExecutor se = new SerialExecutor();
		currentId = 0;
		for (int i = 0; i < 100; ++i) {
			Runnable r = new R(i);
			se.execute(r);
		}
	}

}
