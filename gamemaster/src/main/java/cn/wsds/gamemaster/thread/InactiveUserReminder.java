package cn.wsds.gamemaster.thread;

import java.util.Calendar;

import android.os.AsyncTask;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;



public class InactiveUserReminder {

	// XXX: 为方便测试，此值不是只读的
	public static long TIME_UNIT = 3600 * 1000; // 单位时间

	/** 当前Worker */
	private Worker currentWorker;


	public static final InactiveUserReminder instance = new InactiveUserReminder();

	private InactiveUserReminder() {
	}

	/**
	 * 返回当前时刻的毫秒数
	 * （实现可以方便地在System.currentTimeMillis()和SystemClock.elapsedRealtime()切换）
	 * 
	 * @return 当前时刻的毫秒数
	 */
	private static long now() {
		return System.currentTimeMillis();
	}

	/**
	 * 返回当前本地时间是几点（小时）
	 * 
	 * @return 0~23的小时
	 */
	private static int getHourNow() {
		return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * 定时检查是否已在运行中？
	 */
	public boolean isRunning() {
		return currentWorker != null;
	}

	/**
	 * 开始启动定时器
	 */
	public void restart() {
		stop(); // 先停止
		currentWorker = new Worker();
		currentWorker.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
	}

	/**
	 * 停止定时检查
	 */
	public void stop() {
		if (currentWorker != null) {
			currentWorker.cancel(true);
			currentWorker = null;
		}
	}

	/**
	 * 抽象的“处理”
	 */
	private static abstract class Checker {
		/**
		 * 在Worker线程里被调用，执行检查
		 * 
		 * @param now
		 *            当前时刻
		 * @return 各派生类自定义的参数，非null表示条件满足，null表示条件未满足
		 */
		public abstract Object check(long now);

		/**
		 * 在主线程里被调用，执行条件满足后的操作（比如推送通知）
		 * 
		 * @param context
		 *            由check()返回的上下文数据
		 */
		public abstract void action(Object context);
	}

	/**
	 * 处理器：检查是不是长时间未开启VPN
	 */
	private static class Checker_LongTimeVpnClosed extends Checker {

		// 如果VPN关闭超过此时间，推送条件满足（目前是24个单位时间）
		private final long TIMEOUT_OF_LONGTIME_VPN_CLOSED = TIME_UNIT * 24;

		// 两次通知推送的最小时间间隔（目前是48个单位时间）
		private final long MIN_DELTA_TIME_OF_NOTICE_SEND = TIME_UNIT * 48;

		// 最近一次推送通知的时刻
		// 注意：这个时刻是static的，不因Worker启停而重置
		private static long lastTimeOfNoticeSent;

		private final long timeOfLastVpnClose;

		public Checker_LongTimeVpnClosed(long timeOfLastVpnClose) {
			this.timeOfLastVpnClose = timeOfLastVpnClose;
		}

		@Override
		public Object check(long now) {
			// VPN开启，不推送
			if (AccelOpenManager.isStarted()){
				return null;
			}
			// VPN关闭时长未到阈值，不推送
			if (now - timeOfLastVpnClose < TIMEOUT_OF_LONGTIME_VPN_CLOSED) {
				return null;
			}
			// 上一次已经推送过，且间隔时长未达阈值，不推送
			if (now - lastTimeOfNoticeSent < MIN_DELTA_TIME_OF_NOTICE_SEND) {
				return null;
			}
			// 仅在12:00和18:00（一小时时段内）才推送
			int hour = getHourNow();
			if (hour != 12 && hour != 18) {
				return null;
			}
			return Boolean.TRUE;
		}

		@Override
		public void action(Object context) {
//			LogUtil.d("推送通知：48个时间单位内未开启VPN");
			lastTimeOfNoticeSent = now();
			AppNotificationManager.sendRemindInactiveUser();
			Statistic.addEvent(AppMain.getContext(), Statistic.Event.NOTIFICATION_AWAKE);
		}

	}


	private class Worker extends AsyncTask<Void, Object, Void> {

		private final long timeOfLastVpnClose; // 最近一次VPN关闭的时刻
		private final Checker[] checkerList;

		public Worker() {
			this.timeOfLastVpnClose = now(); // 当VPN关闭时，Worker才被创建，所以这个时刻就是关闭时刻
			this.checkerList = new Checker[1];
			this.checkerList[0] = new Checker_LongTimeVpnClosed(this.timeOfLastVpnClose);
		}
		
		@Override
		protected void onPreExecute() {
//			LogUtil.d("开始：长时间未开启VPN检测");
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled()) {
				long now = now();
				for (int i = 0; i < this.checkerList.length; ++i) {
					Checker checker = this.checkerList[i];
					Object context = checker.check(now);
					if (context != null) {
						publishProgress(checker, context);
					}
					if (isCancelled()) {
						return null;
					}
				}
				try {
					Thread.sleep(TIME_UNIT - (now % TIME_UNIT));
				} catch (InterruptedException e) {
				}
				if (isCancelled()) {
					return null;
				}

			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			Checker checker = (Checker) values[0];
			checker.action(values[1]);
		}

		@Override
		protected void onCancelled() {
			resetCurrentWorker();
		}

		@Override
		protected void onPostExecute(Void result) {
			resetCurrentWorker();
		}

		private void resetCurrentWorker() {
			if (currentWorker == this) {
				currentWorker = null;
			}
//			LogUtil.d("结束：长时间未开启VPN检测");
		}
	}

}