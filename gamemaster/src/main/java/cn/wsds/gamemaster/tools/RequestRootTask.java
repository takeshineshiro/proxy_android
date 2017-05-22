package cn.wsds.gamemaster.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.AsyncTask;
import android.util.Log;


public class RequestRootTask extends AsyncTask<Void, Void, Void> {
	private static final boolean LOG = false;
	private static final String TAG = "RequestRootTask";

	private RootUtil.RequestRootResult result = RootUtil.RequestRootResult.Succeed;
	private List<RootUtil.OnRequestRootListener> listeners = new ArrayList<RootUtil.OnRequestRootListener>();
	//
	private static RequestRootTask instance;
	private static AtomicBoolean instance_exists = new AtomicBoolean(false);
	
	
	/**
	 * 同一时刻，只有一个线程请求Root权限
	 * @param listener 回调请求结果
	 * @return
	 */
	public static boolean execute(RootUtil.OnRequestRootListener listener) {
		if (instance_exists.compareAndSet(false, true)) {
			instance = new RequestRootTask(listener);
			instance.execute();
			return true;
		} else {
			if (LOG) {
				Log.e(TAG, "已经有线程在请求Root权限，返回");
			}
			if (listener != null) {
				listener.onRequestRoot(RootUtil.RequestRootResult.Failed);
			}
			return false;
		}
	}

	public static void addRequestRootListener(RootUtil.OnRequestRootListener listener) {
		if (instance_exists.get() && instance != null) {
			synchronized (instance.listeners) {
				instance.listeners.add(listener);
			}
		}
	}
	
	private RequestRootTask(RootUtil.OnRequestRootListener listener) {
		if (listener != null) {
			this.listeners.add(listener);
		}		
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		try {
			result = RootUtil.RequestRootResult.Succeed;
			// 还没获取到root权限，或者su，inj文件有新版本了
			if (!RootUtil.isGotRootPermission() || RootUtil.isFileUpdated()) {
				int sdkVer = android.os.Build.VERSION.SDK_INT;
				if (sdkVer < RootUtil.SDK_VERSION_FOR_DAEMON) {
					result = RootUtil.copyRunMySU(false);
				} else { // 需要启动守护进程
					result = RootUtil.copyRunMySU(true);
				}
			}
			
			if (result == RootUtil.RequestRootResult.Succeed) { // 已获取root权限
				RootUtil.createSharedDir();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			//instance_exists.set(false);
		}
		
		return null;
	}

	protected void onPostExecute(Void ret) {
		instance_exists.set(false);
		instance = null;
		//boolean ret = result.booleanValue();
		
		synchronized (listeners) {
			for (RootUtil.OnRequestRootListener listener : listeners) {
				if (listener != null) {
					listener.onRequestRoot(result);
				}
			}
			listeners.clear();
		}				
	}
}
