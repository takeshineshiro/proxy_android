package cn.wsds.gamemaster.netdelay;

import java.util.concurrent.atomic.AtomicBoolean;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import cn.wsds.gamemaster.AppMain;

class TrafficStatisticTask {
	private static final boolean LOG = false;
	private static final String TAG = "TrafficStatisticTask";

	private static AtomicBoolean instance_exists = new AtomicBoolean(false);
	private static TrafficStatisticTask instance;

	private ResultCallback callback;
	private int seconds; // 统计多少秒
	private long traffic; // 超过多少字节提醒
	private int excludeUid;	// 排除哪个UID？

	private Worker worker = new Worker();

	/**
	 * 检查结果回调
	 */
	static interface ResultCallback {
		/**
		 * 回调通知
		 * 
		 * @param trafficTooLarge
		 *            true表示检查到流量过大，已超过阈值
		 * @param uid
		 *            应用的UID
		 */
		public void onTrafficStatisticResult(boolean trafficTooLarge, int uid);
	}

	/**
	 * 在主线程调用，统计网络流量，找出最费流量的app
	 * 
	 * @param context
	 * @param seconds
	 *            统计多少秒
	 * @param traffic
	 *            流量阈值（单位字节），超过该值返回true
	 * @param excludeUid
	 *            排除哪个UID
	 * @param listener
	 *            回调
	 * @return 
	 *         true表示成功地启动了一个检查任务，检查结果将通过callback进行回调；false表示已经有一个检查任务在进行中，不再触发回调事件
	 */
	public static boolean start(int seconds, long traffic, int excludeUid, ResultCallback listener) {
		if (instance_exists.compareAndSet(false, true)) {
			instance = new TrafficStatisticTask(seconds, traffic, excludeUid, listener);
			instance.worker.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
			return true;
		} else {
			if (LOG) {
				Log.w(TAG, "Traffic statistic thread already exists");
			}
			return false;
		}
	}

	/**
	 * 中断
	 */
	public static void stop() {
		TrafficStatisticTask inst = instance;
		if (inst != null) {
			Worker worker = inst.worker;
			if (worker != null) {
				worker.cancel(true);
				inst.worker = null;
			}
		}
	}

	private TrafficStatisticTask(int seconds, long traffic, int excludeUid, ResultCallback callback) {
		this.seconds = seconds;
		this.traffic = traffic;
		this.excludeUid = excludeUid;
		this.callback = callback;
	}

	private class Worker extends AsyncTask<Void, Void, Boolean> {

		private AppStats found; // 最耗流量的app

		@Override
		protected Boolean doInBackground(Void... params) {
			if (LOG) {
				Log.d(TAG, "doInBackground()");
			}
			found = null;
			try {
				SparseArray<AppStats> appList = AppStats.getAppList(AppMain.getContext(), excludeUid);
				if (appList == null || appList.size() == 0) {
					if (LOG) {
						Log.w(TAG, "getAppList return empty");
					}
					return false;
				}

				// 将所有应用消耗的流量值记录
				for (int i = appList.size() - 1; i >= 0; --i) {
					AppStats app = appList.valueAt(i);
					app.recvBytes = app.getRecvBytes(app.uid);
					app.sendBytes = app.getSendBytes(app.uid);
					if (LOG) {
						Log.d(TAG,
							String.format("uid:%d, recvBytes:%d, sendBytes:%d", app.uid, app.recvBytes, app.sendBytes));
					}
					if (isCancelled()) {
						return false;
					}
				}

				long beginTime = SystemClock.elapsedRealtime();
				long expectEndTime = beginTime + seconds * 1000;
				long endTime;
				while (true) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException ie) {

					}
					if (isCancelled()) {
						return false;
					}
					endTime = SystemClock.elapsedRealtime();
					if (endTime >= expectEndTime) {
						break;
					}
				}

				// 第二次
				long total = 0; // 所有app总增量		
				long max = -1;
				for (int i = appList.size() - 1; i >= 0; --i) {
					AppStats app = appList.valueAt(i);
					long recvBytes = app.getRecvBytes(app.uid);
					app.recvBytes = recvBytes - app.recvBytes; // 接收增量					
					//
					long sendBytes = app.getSendBytes(app.uid);
					app.sendBytes = sendBytes - app.sendBytes; // 发送增量

					long totalBytes = app.recvBytes + app.sendBytes; // 总增量
					total += totalBytes;

					if (max < totalBytes) { // 找出最大值
						max = totalBytes;
						found = app;
					}

					if (LOG) {
						Log.d(TAG, String.format("uid:%d, recvBytes2:%d, sendBytes2:%d, 该app消耗流量:%d", app.uid,
							recvBytes, sendBytes, totalBytes));
					}
				}

				if (LOG) {
					if (found != null) {
						Log.d(TAG, String.format("All=%d, Max=(uid=%d, flow:%d)", total, found.uid, max));
					} else {
						Log.d(TAG, "Not found");
					}
				}

				return total > traffic; // 字节
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			end(result, found);
		}

		@Override
		protected void onCancelled(Boolean result) {
			end(false, null);
		}

		private void end(Boolean result, AppStats found) {
			if (instance == TrafficStatisticTask.this) {
				instance = null;
			}
			instance_exists.set(false);
			if (callback != null) {
				callback.onTrafficStatisticResult(result.booleanValue(), found == null ? -1 : found.uid);
				callback = null;
			}
		}
	}

}
