package cn.wsds.gamemaster.tools;

import java.util.ArrayList;
import java.util.Collection;

import android.os.AsyncTask;
//import java.util.concurrent.atomic.AtomicBoolean;
//import android.util.Log;


public class ExecCommandTask extends AsyncTask<Void, Void, Integer> {
	//private static final boolean LOG = false;
	//private static final String TAG = "ExecCommandTask";

	private final RootUtil.OnExecCommandListener listener;

	//private static AtomicBoolean instance_exists = new AtomicBoolean(false);

	private final Iterable<String> commands;

	
	
	/**
	 * 以root权限，在另一个线程里执行命令
	 */
	public static void postExecuteInThread(Collection<String> commands, RootUtil.OnExecCommandListener listener) {
		new ExecCommandTask(commands, listener).executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
	}
	
	private ExecCommandTask(Collection<String> commands, RootUtil.OnExecCommandListener listener) {
		this.commands = new ArrayList<String>(commands);
		this.listener = listener;
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		int ret = -1;
		
		try {
			ret = RootUtil.execCommands(commands);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			//instance_exists.set(false);
		}
		
		return ret;
	}

	protected void onPostExecute(Integer result) {
		if (listener != null) {
			listener.onExecCommand(result.intValue());
		}
	}
}
