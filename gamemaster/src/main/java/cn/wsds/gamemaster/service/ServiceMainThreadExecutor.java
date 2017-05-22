package cn.wsds.gamemaster.service;

import android.os.Handler;
import android.os.Looper;

public class ServiceMainThreadExecutor {

	private static class MyHandler extends Handler {
		
		public MyHandler() {
			super(Looper.getMainLooper());
		}
		
	}
	
	private static final MyHandler handler = new MyHandler();
	
	public static void runInMainThread(Runnable r) {
		handler.post(r);
	}
}
