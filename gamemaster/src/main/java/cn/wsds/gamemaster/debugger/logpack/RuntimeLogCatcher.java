package cn.wsds.gamemaster.debugger.logpack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;

/**
 * 运行日志捕捉器
 */
class RuntimeLogCatcher {
	
	private AsyncCatcher logcatch = null;
	private final int pid;
	private OnLogUpdateListenerRef onLogUpdateListener = new OnLogUpdateListenerRef(null);

	public RuntimeLogCatcher(int pid) {
		this.pid = pid;
	}
	
	/**
	 * 设置日志更新监听器
	 * @param onLogUpdateListener 日志更新监听器
	 */
	public void setOnLogUpdateListener(OnLogUpdateListener onLogUpdateListener) {
		this.onLogUpdateListener = new OnLogUpdateListenerRef(onLogUpdateListener);
	}
	
	/**
	 * 容null listener处理
	 */
	private static final class OnLogUpdateListenerRef implements OnLogUpdateListener{

		private final OnLogUpdateListener listener;
		public OnLogUpdateListenerRef(OnLogUpdateListener onLogUpdateListener) {
			this.listener = onLogUpdateListener;
		}

		@Override
		public void onLogUpdate(String log) {
			if(listener != null){
				listener.onLogUpdate(log);
			}
		}
	}

	/**
	 * 开始
	 * @param onLogUpdateListener
	 */
	public void start(OnLogUpdateListener onLogUpdateListener) {
		if (logcatch == null) {
			setOnLogUpdateListener(onLogUpdateListener);
			logcatch = new AsyncCatcher(String.valueOf(pid));
			logcatch.execute();
		}
	}

	/**
	 * 关闭
	 */
	public void stop(){
		if (logcatch != null) {
			logcatch.stopLogs();
			setOnLogUpdateListener(null);
			logcatch = null;
		}
	}
	
	/**
	 * 现在是否正在捕获日志
	 * @return
	 */
	public boolean isCatching(){
		if (logcatch != null) {
			return logcatch.mRunning;
		}
		return false;
	}

	/**
	 * 日志更新时回调
	 */
	public interface OnLogUpdateListener {
		/**
		 * 当日志更新的时候
		 * @param log 读到的日志信息
		 */
		public void onLogUpdate(String log);
	}

	/** 
	 * 异步读取日志
	 */
	private class AsyncCatcher extends AsyncTask<String, String, Boolean> {
		private boolean mRunning;
		/** 命令内容 */
		private final String cmds = "logcat -v time";
		/** 应用PID字符串 */
		private final String appId;
		public AsyncCatcher(String pid) {
			appId = String.valueOf(pid);
		}
		public void stopLogs() {
			mRunning = false;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			for (String v : values) {
				onLogUpdateListener.onLogUpdate(v);
			}
		}

		@Override
		protected Boolean doInBackground(String... params) {
			mRunning = true;
			Process logcatProc = null;
			BufferedReader mReader = null;
			try {
				logcatProc = Runtime.getRuntime().exec(cmds);
				mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()));
				String log = null;
				while (mRunning && (log = mReader.readLine()) != null) {
					if (!mRunning) {
						break;
					}
					if (log.length() == 0) {
						continue;
					}
					if (log.contains(appId)) {
						publishProgress(log + "\n");
					}
				}
			} catch (IOException e) {
				return false;
			} finally {
				if (logcatProc != null) {
					logcatProc.destroy();
					logcatProc = null;
				}
				if (mReader != null) {
					try {
						mReader.close();
						mReader = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return true;
		}
	}
}