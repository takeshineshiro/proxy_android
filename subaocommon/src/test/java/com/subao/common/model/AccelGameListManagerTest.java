package com.subao.common.model;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by hujd on 17-3-30.
 */
public class AccelGameListManagerTest {

	@Test
	public void testInstance() {
		final CountDownLatch latch = new CountDownLatch(1);
		int threadCount = 50;

		for (int i = 0; i < threadCount; i++) {
			new Thread() {

				@Override
				public void run() {
					try {
						latch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					System.out.println(AccelGameListManager.getInstance().hashCode());
				}
			}.start();
		}

		latch.countDown();
	}
	@Test
	public void testSetAccelGameList() throws Exception {
		AccelGameListManager listManager = new AccelGameListManager();
		listManager.setAccelGameList(null);
		assertEquals(null, listManager.getAccelGameList());
	}
}