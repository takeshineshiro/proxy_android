package com.subao.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import android.os.Handler;
import android.os.Looper;

import com.subao.common.utils.ThreadUtils;

public class MainThreadExecutorTest extends RoboBase {

	@Test
	public void test() {
		final Handler h = new Handler(Looper.getMainLooper());
		final RunnableHasResult<Integer> r = new RunnableHasResult<Integer>() {
		
			@Override
			public void run() {
				assertTrue(ThreadUtils.isInAndroidUIThread());
				setResult(123);
			}
		};

		int result = MainThreadExecutor.execute(h, r);
		assertEquals(123, result);
		//
		new Thread() {
			@Override
			public void run() {
				int result = MainThreadExecutor.execute(h, r);
				assertEquals(123, result);
			}
		}.start();
	}

	@Test
	public void testConstructor() {
		new MainThreadExecutor();
	}
}
