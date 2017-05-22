package com.subao.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RunnableHasResultTest {

	private static class Foo extends RunnableHasResult<Integer> {

		private final int value;

		public Foo(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		@Override
		public void run() {
			setResult(this.value + 1);
		}
	}

	@Test
	public void testInterrupt() {
		final Foo foo = new Foo(1);
		Thread t = new Thread() {
			@Override
			public void run() {
				foo.waitResult();
			}
		};
        t.interrupt();
		t.start();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}
        t.interrupt();
        foo.run();
		try {
			t.join();
		} catch (InterruptedException e) {
		}
		assertEquals(2, (int)foo.waitResult());
	}

	@Test
	public void test() {
		doTest(1000, 0);
		doTest(0, 1000);
	}

	private static void doSleep(long time) {
		if (time > 0) {
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {}
		}
	}

	private static void doTest(long sleepCaller, final long sleepRunner) {
		final Foo foo = new Foo(1);
		assertEquals(1, foo.getValue());
		new Thread() {
			public void run() {
				doSleep(sleepRunner);
				foo.run();
			}
		}.start();
		doSleep(sleepCaller);
		assertEquals(2, (int)foo.waitResult());
	}
}
