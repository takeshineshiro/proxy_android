package cn.wsds.gamemaster.tools;

import java.util.concurrent.atomic.AtomicBoolean;

import android.os.AsyncTask;
import android.util.Log;


public class ClearRootTask extends AsyncTask<Void, Void, Boolean> {
	private static final boolean LOG = false;
	private static final String TAG = "ClearRootTask";

	private RootUtil.OnClearRootListener listener;
	private static ClearRootTask instance;
	private static AtomicBoolean instance_exists = new AtomicBoolean(false);

	
	/**
	 * 同一时刻，只有一个线程清除Root权限
	 * @param listener 回调请求结果
	 * @return
	 */
	public static void execute(RootUtil.OnClearRootListener listener) {
		if (instance_exists.compareAndSet(false, true)) {
			instance = new ClearRootTask(listener);
			instance.execute();
		} else {
			if (LOG) {
				Log.e(TAG, "已经有线程在清除Root权限，返回");
			}
			if (listener != null) {
				listener.onClearRoot(false);
			}
		}
	}

	
	private ClearRootTask(RootUtil.OnClearRootListener listener) {
		this.listener = listener;	
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {
		boolean ok = false;
		
		try {
			if (!RootUtil.isGotRootPermission()) { // 还没获取root权限
				//Log.e(TAG, "ClearRootTask:之前还没获取到root权限！！！");
				return true;
			}
			
			ok = RootUtil.clearRoot();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			//instance_exists.set(false);
		}
		
		return ok;
	}

	protected void onPostExecute(Boolean result) {
		instance_exists.set(false);
		instance = null;
		if (listener != null) {
			listener.onClearRoot(result.booleanValue());
		}			
	}
}
