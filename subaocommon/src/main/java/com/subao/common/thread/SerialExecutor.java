package com.subao.common.thread;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

/**
 * 串行执行的Executor
 */
public class SerialExecutor implements Executor {
	private final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
	private Runnable mActive;

	@Override
	public void execute(final Runnable r) {
		synchronized (this) {
			mTasks.offer(new Runnable() {
				public void run() {
					try {
						r.run();
					} finally {
						scheduleNext();
					}
				}
			});
			if (mActive == null) {
				scheduleNext();
			}
		}
	}

	private void scheduleNext() {
		synchronized (this) {
			if ((mActive = mTasks.poll()) != null) {
				ThreadPool.getExecutor().execute(mActive);
			}
		}
	}
}
