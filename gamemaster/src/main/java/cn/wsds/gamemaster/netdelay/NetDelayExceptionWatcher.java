package cn.wsds.gamemaster.netdelay;

import android.content.Context;
import android.os.SystemClock;
import cn.wsds.gamemaster.data.InstalledAppManager;
import cn.wsds.gamemaster.ui.floatwindow.ToastFlowWarning;

public class NetDelayExceptionWatcher {

	public static interface Provider {
		public Context getContext();

		public boolean isDisconnectOr2G();
	}

	private class Recorder {

		/** 记录已有多少个连续的异常值了 */
		private int exceptionDelayCount;

		/**
		 * 有时延值传来
		 * 
		 * @param provider
		 * @param delayValue
		 *            时延值
		 * @return true=满足了异常条件，false=未满足
		 */
		public boolean onNetDelayData(Provider provider, long delayValue) {
			if (delayValue < params.thresholdExceptionDelayValue || provider.isDisconnectOr2G()) {
				exceptionDelayCount = 0;
				return false;
			}
			++exceptionDelayCount;
			if (exceptionDelayCount >= params.thresholdExceptionDelayCount) {
				exceptionDelayCount = 0;
				return true;
			}
			return false;
		}

		public void reset() {
			this.exceptionDelayCount = 0;
		}
	}

	private static NetDelayExceptionWatcher lastInstance;

	public static Params getParams() {
		return lastInstance == null ? null : lastInstance.params;
	}

	public static boolean reset(Params params) {
		if (lastInstance != null) {
			lastInstance.params = params;
			lastInstance.recorder.reset();
			lastInstance.lastTimeOfException = 0;
			return true;
		} else {
			return false;
		}
	}

	private final Provider provider;
	private final Recorder recorder;
	private Params params;

	private int currentGameUid;

	private long lastTimeOfException;

	private TrafficStatisticTask.ResultCallback callback = new TrafficStatisticTask.ResultCallback() {

		@Override
		public void onTrafficStatisticResult(boolean trafficTooLarge, int uid) {
			if (trafficTooLarge && currentGameUid > 0) {
				lastTimeOfException = SystemClock.elapsedRealtime();
				String appLabel = getAppLabel(uid);
				ToastFlowWarning.show(provider.getContext(), appLabel, (uid == currentGameUid));
			}
		}

		private String getAppLabel(int uid) {
			InstalledAppManager.Entry entry = InstalledAppManager.getInstance().find(uid);
			if (entry != null) {
				return entry.appLabel;
			}
			return Integer.toString(uid);
		}
	};

	public static class Params {
		/** 当时延值大于等于此值时，视为异常值 */
		public final long thresholdExceptionDelayValue;
		/** 当异常时延值连续达到多少个时，视为异常发生 */
		public final int thresholdExceptionDelayCount;
		/** 进行流量检查的时候，需要检查多少秒？ */
		public final int secondsOfFlowCheck;
		/** 流量检查的流量阈值 */
		public final long thresholdForFlowCheck;
		/** 两次提醒的最小时间间隔（毫秒） */
		public final long intervalPrompt;

		/**
		 * @param thresholdExceptionDelayValue
		 *            当时延值大于等于此值时，视为异常值
		 * @param thresholdExceptionDelayCount
		 *            当异常时延值连续达到多少个时，视为异常发生
		 * @param secondsOfFlowCheck
		 *            进行流量检查的时候，需要检查多少秒？
		 * @param thresholdForFlowCheck
		 *            流量检查的流量阈值
		 * @param intervalPrompt
		 *            两次提醒的最小时间间隔
		 */
		public Params(long thresholdExceptionDelayValue, int thresholdExceptionDelayCount, int secondsOfFlowCheck, long thresholdForFlowCheck,
			long intervalPrompt) {
			this.thresholdExceptionDelayValue = thresholdExceptionDelayValue;
			this.thresholdExceptionDelayCount = thresholdExceptionDelayCount;
			this.secondsOfFlowCheck = secondsOfFlowCheck;
			this.thresholdForFlowCheck = thresholdForFlowCheck;
			this.intervalPrompt = intervalPrompt;
		}

		public static Params createDefault() {
			return new Params(
				400, // 时延大于此值视为异常
				3,		// 连续异常次数达到此值，视为异常
				5,		// 检查多少秒？
				500 * 1024,	// 流量阈值
				1000 * 3600	// 两次提醒的最小时间间隔
			);
		}
	}

	/**
	 * 构造
	 * 
	 * @param provider
	 *            {@link NetDelayExceptionWatcher#Provider}
	 */
	public NetDelayExceptionWatcher(Provider provider, Params params) {
		this.provider = provider;
		this.params = params;
		this.recorder = new Recorder();
		lastInstance = this;
		InstalledAppManager.init(provider.getContext());
	}

	/**
	 * 有时延值传来
	 * 
	 * @param delayValue
	 *            时延值
	 * @return true=满足了异常条件，false=未满足
	 */
	public boolean onNetDelayData(long delayValue) {
		if (currentGameUid <= 0) {
			return false;
		}
		// 如果距上次提醒间隔过短，就别检测了
		if (lastTimeOfException > 0 && SystemClock.elapsedRealtime() - lastTimeOfException < params.intervalPrompt) {
			recorder.reset();
			return false;
		}
		boolean r = this.recorder.onNetDelayData(provider, delayValue);
		if (r) {
			TrafficStatisticTask.start(params.secondsOfFlowCheck, params.thresholdForFlowCheck,
				InstalledAppManager.getInstance().getUidOfWSDS(),
				callback);
		}
		return r;
	}

	/** 开始监测工作 */
	public void start(int currentGameUid) {
		this.currentGameUid = currentGameUid;
	}

	/** 停止监测工作 */
	public void stop() {
		this.currentGameUid = -1;
		TrafficStatisticTask.stop();
		recorder.reset();
	}
}
