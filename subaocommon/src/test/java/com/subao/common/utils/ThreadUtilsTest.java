package com.subao.common.utils;

import android.os.Looper;

import com.subao.common.RoboBase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThreadUtilsTest extends RoboBase {
	
	@Test
	public void testConstructor() {
		new ThreadUtils();
	}

	@Test
	public void testGetAndroidUIThreadId() {
		assertEquals(Looper.getMainLooper().getThread().getId(), ThreadUtils.getAndroidUIThreadId());
	}
	
	@Test
	public void testIsInAndroidUIThread() {
		assertTrue(ThreadUtils.isInAndroidUIThread());
		//
		Thread t = new Thread() {
			public void run() {
				assertFalse(ThreadUtils.isInAndroidUIThread());
			}
		};
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
		}
	}

}
