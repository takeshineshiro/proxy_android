package com.subao.common.utils;

import android.os.Looper;

public class ThreadUtils {

	private static long androidUIThreadId = -1;
	
	/**
	 * 返回主线程的ID
	 * @return 主线程ID
	 */
	public static long getAndroidUIThreadId() {
		if (androidUIThreadId < 0) {
			androidUIThreadId = Looper.getMainLooper().getThread().getId();
		}
		return androidUIThreadId;
	}
	
	/**
	 * 判断当前线程是否为主线程
	 * @return True表示当前线程是主线程，False表示当前线程不是主线程
	 */
	public static boolean isInAndroidUIThread() {
		return Thread.currentThread().getId() == getAndroidUIThreadId();
	}
	

}
