package cn.wsds.gamemaster.ui.adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.subao.collection.FixedCapacityQueue;

public class AdapterConsole extends BaseAdapter {

	public interface Listener {
		public void onStop(boolean isCancelled);
	}

	private class Worker extends AsyncTask<String, String, Void> {

		@Override
		protected void onPreExecute() {
			lines.clear();
		}

		@Override
		protected Void doInBackground(String... params) {
			BufferedReader reader = null;
			Process process = null;
			try {
				process = new ProcessBuilder(params).start();
				reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				while (!isCancelled()) {
					String line = reader.readLine();
					if (line == null) {
						break;
					}
					publishProgress(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {}
				}
				if (process != null) {
					process.destroy();
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			lines.offer(values[0]);
			notifyDataSetChanged();
			if (autoScroll) {
				listView.smoothScrollToPosition(lines.size());
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			cleanup();
			if (listener != null) {
				listener.onStop(false);
			}
		}

		@Override
		protected void onCancelled(Void result) {
			cleanup();
			if (listener != null) {
				listener.onStop(true);
			}
		}

		private void cleanup() {
			if (worker == this) {
				worker = null;
			}
		}

	}

	private final Context context;
	private final ListView listView;
	private final Listener listener;
	private final FixedCapacityQueue<String> lines;
	private final boolean autoScroll;
	private Worker worker;

	public AdapterConsole(Context context, ListView listView, int maxLine, boolean autoScroll, Listener listener) {
		this.context = context;
		this.listView = listView;
		this.autoScroll = autoScroll;
		this.listener = listener;
		this.lines = new FixedCapacityQueue<String>(maxLine);
	}

	public void execute(String... commands) {
		if (worker != null) {
			return;
		}
		worker = new Worker();
		worker.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor(), commands);
	}

	public void cleanup() {
		if (worker != null && !worker.isCancelled()) {
			worker.cancel(true);
		}
	}

	@Override
	public int getCount() {
		return lines.size();
	}

	@Override
	public Object getItem(int position) {
		return lines.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String line = lines.get(position);
		TextView tv;
		if (convertView == null) {
			tv = new TextView(context);
			convertView = tv;
		} else {
			tv = (TextView) convertView;
		}
		tv.setText(line);
		return tv;
	}
}
