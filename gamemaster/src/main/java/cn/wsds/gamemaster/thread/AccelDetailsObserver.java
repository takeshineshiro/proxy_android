package cn.wsds.gamemaster.thread;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.app.AccelDetails;
import cn.wsds.gamemaster.app.AccelDetails.NetKind;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

public class AccelDetailsObserver {
	
	private static final boolean LOG = false;
	private static final String TAG = "AccelDetailsObserver";

	private static final AccelDetailsObserver instance = new AccelDetailsObserver();

	private static final int MIN_VALID_THRESHOLD = 10 * 1000;

	private static final int MSG_SET_GAMEINFO_NEWLAUNCH = 1;
	private static final int MSG_REMOVE = 0;

	/**
	 * 一个快速检索容器，Key为游戏的UID，Value为{@link Info}
	 */
	private final SparseArray<Info> gameListByUID = new SparseArray<Info>();

	// 隐含强引用到单例对象，生存期为进程生存期，所以勿需用弱引用的Handler
	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_REMOVE:
				gameListByUID.delete(msg.arg1);
				break;
			case MSG_SET_GAMEINFO_NEWLAUNCH:
				if (lastGameInfo != null) {
					lastGameInfo.setNewLaunch(true);
				}
				break;
			}
		};
	};

	private GameInfo lastGameInfo;
	private long launchedTime;
	private long duration;
	private NetKind netState;
	private int percentDelayDecrease;
	private long flow;
	private boolean needAccumulate;
	private long tempTime;

	private static class PercentAdjust {
		/** 上一次的网络时延是正常值吗？ */
		private boolean lastLocalDelayNormal = true;
		/** 因为网络异常减掉的加速百分比，已经减了几次了？ */
		private int countAdjustByLocalDelay;
		/** 因为断线重连成功而增加的加速百分比，已经增加了几次了？ */
		private int countAdjustByReconnect;
		
		public void reset() {
			lastLocalDelayNormal = true;
			countAdjustByLocalDelay = countAdjustByReconnect = 0;
		}
	}
	private final PercentAdjust percentAdjust = new PercentAdjust();	
	
	/**
	 * 保存 {@link AccelDetails} 和离开前台时刻的容器
	 */
	private static class Info {
		public final AccelDetails accelDetails;
		public final long leavingTime;

		public Info(AccelDetails accelDetails, long leavingTime) {
			this.accelDetails = accelDetails;
			this.leavingTime = leavingTime;
		}
	}
	

	public static AccelDetailsObserver getInstance() {
		return instance;
	}

	private AccelDetailsObserver() {
		TriggerManager.getInstance().addObserver(eventObserver);
	}

	private final EventObserver eventObserver = new EventObserver() {

		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			NetKind current = getCurrentNetKind(state);
			if (current == netState || current == NetKind.UNKNOWN) {
				return;
			}
			if (netState == NetKind.UNKNOWN) {
				netState = current;
			} else {
				netState = NetKind.BOTH;
			}
		}

		@Override
		public void onVPNOpen() {
			//只有当处于游戏中的时间才起作用
			startTiming();
		}

		@Override
		public void onVPNClose() {
			startAccumulate(true);
		}

		@Override
		public void onScreenOn() {
			startTiming();
		}

		@Override
		public void onScreenOff() {
			startAccumulate(AccelOpenManager.isStarted());
		}
		
		@Override
		public void onFirstSegmentNetDelayChange(int delayMilliseconds) {
			if (lastGameInfo == null) {
				return;
			}
			boolean isDelayNormal = delayMilliseconds >= 0 && delayMilliseconds < GlobalDefines.NET_DELAY_TIMEOUT;
			if (isDelayNormal == percentAdjust.lastLocalDelayNormal) {
				// 延迟的异常状态并未发生改变，直接返回
				return;
			}
			percentAdjust.lastLocalDelayNormal = isDelayNormal;
			if (isDelayNormal) {
				// 延迟从异常变到正常，直接返回
				return;
			}
			if (percentAdjust.countAdjustByLocalDelay >= 10) {
				// 已调整过10次了，不再调整
				return;
			}
			if (percentDelayDecrease <= 0) {
				// 已经是0了，不能再调整
				return;
			}
			// 调整
			++percentAdjust.countAdjustByLocalDelay;
			percentDelayDecrease -= 1;
			//
			if (LOG) {
				Log.d(TAG, String.format("时延异常，第%d次调整，降低时延百分比：%d", percentAdjust.countAdjustByLocalDelay, percentDelayDecrease));
			}
		};
		
		@Override
		public void onReconnectResult(ReconnectResult result) {
			if (lastGameInfo == null) {
				return;
			}
			if (!result.success) {
				// 断线重连失败，不调整
				return;
			}
			if (percentAdjust.countAdjustByReconnect >= 3) {
				// 已调整过多次了，返回
				return;
			}
			if (result.uid != lastGameInfo.getUid()) {
				// 不是这个游戏，返回
				return;
			}
			// 调整
			++percentAdjust.countAdjustByReconnect;
			percentDelayDecrease += 1;
			//
			if (LOG) {
				Log.d(TAG, String.format("断线重连，第%d次调整，降低时延百分比：%d", percentAdjust.countAdjustByReconnect, percentDelayDecrease));
			}
		};

		@Override
		public void onTopTaskChange(GameInfo gameInfo) {
			if (lastGameInfo == null) {
				onTopTaskChange_LastGameIsNull(gameInfo);
			} else {
				onTopTaskChange_LastGameExists(gameInfo);
			}
		}

		/**
		 * 顶层Task改变，如果上次lastGameInfo !=
		 * null，说明之前处于游戏中，需要判断是否需要将该次加速记录到对应的gameInfo中
		 */
		private void onTopTaskChange_LastGameExists(GameInfo gameInfo) {
			lastGameInfo.setLeavingForegroundTime(System.currentTimeMillis());
			//如果加速的时间超过10s，那么需要将该次加速信息放到gameInfo中
			if (needAccumulate) {
				duration += Math.max(System.currentTimeMillis() - tempTime, 0);
			}
			if (duration > MIN_VALID_THRESHOLD) {
				AccelDetails accelDetails = new AccelDetails(launchedTime, duration, netState, percentDelayDecrease,
					flow);
				lastGameInfo.updateAccelDetails(accelDetails);
				//将当前离开前台的时间记下来，如果一会再次进入前台的时间不超过10s，那么需要跟本次的结果拼接起来
				long leavingTime = System.currentTimeMillis();
				gameListByUID.put(lastGameInfo.getUid(), new Info(accelDetails, leavingTime));
				handler.sendMessageDelayed(handler.obtainMessage(MSG_REMOVE, lastGameInfo.getUid(), 0, null),
					MIN_VALID_THRESHOLD);
			}
			lastGameInfo = gameInfo;
			handler.removeMessages(MSG_SET_GAMEINFO_NEWLAUNCH);
		}

		/**
		 * 顶层Task改变，且lastGameInfo为空（之前未处在游戏中）
		 */
		private void onTopTaskChange_LastGameIsNull(GameInfo gameInfo) {
			if (gameInfo == null) {
				// 切换到非游戏页面，直接返回。
				return;
			}
			// 说明进入了游戏页面
			TriggerManager.getInstance().raiseStartNewGame(gameInfo);
			//判定进入的游戏是否为新游戏
			Info container = gameListByUID.get(gameInfo.getUid());
			//如果是新游戏
			if (container == null) {
				reset(gameInfo);
				return;
			}
			//如果是之前进过的游戏，判定上次离开前台的时间是否超过10s
			//如果上次离开前台的时间超过10s
			long enteringTime = System.currentTimeMillis();
			if (enteringTime - container.leavingTime > MIN_VALID_THRESHOLD) {
				reset(gameInfo);
			}
			//如果上次离开前台的时间<=10s,与上次的结果拼接起来
			else {
				lastGameInfo = gameInfo;
				launchedTime = container.accelDetails.getLaunchedTime();
				tempTime = System.currentTimeMillis();
				duration = container.accelDetails.getDuration();
				NetKind currentNetKind = getCurrentNetKind();
				if (currentNetKind == NetKind.UNKNOWN || currentNetKind == container.accelDetails.getNetState()) {
					netState = container.accelDetails.getNetState();
				} else if (container.accelDetails.getNetState() == NetKind.UNKNOWN) {
					netState = currentNetKind;
				} else {
					netState = NetKind.BOTH;
				}
				percentDelayDecrease = container.accelDetails.getPercentDelayDecrease();
				flow = container.accelDetails.getFlow();
				gameListByUID.remove(gameInfo.getUid());

				needAccumulate = AccelOpenManager.isStarted() ? true : false;

				handler.sendEmptyMessageDelayed(MSG_SET_GAMEINFO_NEWLAUNCH, MIN_VALID_THRESHOLD - duration < 0 ? 0
					: MIN_VALID_THRESHOLD - duration);

			}
		}
	};

	public void onFlowProduce(GameInfo gameInfo, long mobile) {
		//如果本来就在游戏里，数据来了，那么直接添加
		if (gameInfo == lastGameInfo) {
			if (netState == NetKind.BOTH || netState == NetKind.MOBILE) {
				flow += mobile;
			}
			return;
		}

		//如果在刚刚退出游戏的时间，数据来了
		if (gameListByUID == null || gameListByUID.size() == 0) {
			return;
		}
		Info container = gameListByUID.get(gameInfo.getUid());
		if (container != null) {
			AccelDetails accelDetails = container.accelDetails;
			NetKind state = accelDetails.getNetState();
			if (state == NetKind.BOTH || state == NetKind.MOBILE) {
				accelDetails.addFlow(mobile);
			}
		}
	}

	private void reset(GameInfo gameInfo) {
		lastGameInfo = gameInfo;
		tempTime = launchedTime = System.currentTimeMillis();
		duration = 0;
		netState = getCurrentNetKind();
		percentDelayDecrease = buildPercentDelayDecrease(getCurrentNetType());
		flow = 0;
		needAccumulate = AccelOpenManager.isStarted() ? true : false;
		percentAdjust.reset();
		//
		handler.sendEmptyMessageDelayed(MSG_SET_GAMEINFO_NEWLAUNCH, MIN_VALID_THRESHOLD);
		//
		if (LOG) {
			Log.d(TAG, "降低时延百分比：" + percentDelayDecrease);
		}
	}

	private void startTiming() {
		//只有当处于游戏中的时间才起作用
		if (lastGameInfo != null) {
			needAccumulate = true;
			tempTime = System.currentTimeMillis();
		}
	}

	private void startAccumulate(boolean extra) {
		//只有当处于游戏中的时间才起作用
		if (lastGameInfo != null && needAccumulate && extra) {
			long temp = Math.max(System.currentTimeMillis() - tempTime, 0);
			duration += temp;
			tempTime = Long.MAX_VALUE;
		}
		needAccumulate = false;
	}

	private static int buildPercentDelayDecrease(NetTypeDetector.NetType netType) {
		long r = SystemClock.elapsedRealtime() % 1000;
		int delta = (int)r * 20 / 1000;
		switch (netType) {
		case WIFI:
			return 60 + delta;
		case MOBILE_3G:
			return 40 + delta;
		case MOBILE_4G:
			return 50 + delta;
		default:
			return 0;
		}
	}

	private static AccelDetails.NetKind getCurrentNetKind(NetTypeDetector.NetType netType) {
		switch (netType) {
		case WIFI:
			return AccelDetails.NetKind.WIFI;
		case MOBILE_2G:
		case MOBILE_3G:
		case MOBILE_4G:
			return AccelDetails.NetKind.MOBILE;
		default:
			return AccelDetails.NetKind.UNKNOWN;
		}
	}

	private static AccelDetails.NetKind getCurrentNetKind() {
		return getCurrentNetKind(getCurrentNetType());
	}

	private static NetTypeDetector.NetType getCurrentNetType() {
		return NetManager.getInstance().getCurrentNetworkType();
	}
}
