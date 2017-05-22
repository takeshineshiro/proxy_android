package cn.wsds.gamemaster.debugger;

import android.content.Context;
import android.os.AsyncTask;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowInReconnect;

public class FakeConnectionRepairPrompt {
	
	public static void execute(Context context) {
		FloatWindowInReconnect.createInstance(context, "测试特效，开始断线重连");
		Worker worker = new Worker();
		worker.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
	}
	
	private static class Worker extends AsyncTask<Void, Integer, Void> {
		
		private static void sleep(long milliseconds) {
			try {
				Thread.sleep(milliseconds);
			} catch (InterruptedException e) { }
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 1; i <= GlobalDefines.MAX_COUNT_OF_CONNECTION_REPAIR; ++i) {
				publishProgress(i);
				sleep(8000);
			}
			return null;
		}
	
		@Override
		protected void onProgressUpdate(Integer... values) {
			FloatWindowInReconnect.changeCurrentData(values[0], false);
		}

	}
}
