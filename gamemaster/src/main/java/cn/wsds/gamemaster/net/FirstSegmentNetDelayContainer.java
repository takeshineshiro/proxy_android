package cn.wsds.gamemaster.net;

import cn.wsds.gamemaster.GlobalDefines;

import com.subao.collection.FixedCapacityQueue;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.NetTypeDetector.NetType;

/**
 * 第一段（手机至节点）网络延迟
 * 
 * see SecondSegmentNetDelayContainer SecondSegmentNetDelay, 第二段（节点到游戏服务器）的网络延迟
 */
public class FirstSegmentNetDelayContainer {

	/**
	 * 最近一次的，未经加工的原始延迟值
	 */
	private int raw;

	/**
	 * 根据产品提供的算法进行修饰后的值
	 */
	private int adjusted;

	/**
	 * 历史记录
	 */
	private final History history = new History();

	/**
	 * 延迟值
	 */
	private static class TheDelay {

		private final short delayValue;

		/**
		 * 产生的时刻
		 */
		public final long time;

		/**
		 * 是否是质量差的延迟值?
		 * 
		 * @see FirstSegmentNetDelayContainer(int, NetTypeDetector);
		 */
		public boolean isBad() {
			return this.delayValue < 0;
		}

		public TheDelay(long time, int delayValue, boolean bad) {
			this.time = time;
			this.delayValue = (short) (bad ? -delayValue : delayValue);
		}

		public int getDelayValue() {
			return delayValue < 0 ? -delayValue : delayValue;
		}
	}

	/** 历史延迟值的队列 */
	private static class History {
		private final FixedCapacityQueue<TheDelay> queue = new FixedCapacityQueue<TheDelay>(150);

		/** 队列"all"里有多少个异常值？ */
		private int countOfBadValue;

		/** 所有非Bad值的和 */
		private int sumOfGoodValue;

		/**
		 * 清除所有时刻早于给定时刻的项
		 * 
		 * @param time
		 *            给定的时刻。容器中所以早于此时刻的项将被清除
		 */
		public void removeAllExpired(long time) {
			while (!queue.isEmpty()) {
				TheDelay bd = queue.peek();
				if (bd.time <= time) {
					queue.poll();
				} else {
					break;
				}
			}
		}

		public void offer(long now, int delayValue, boolean isBad) {
			if (queue.isFull()) {
				TheDelay head = queue.poll();
				if (head.isBad()) {
					--countOfBadValue;
				} else {
					sumOfGoodValue -= head.getDelayValue();
				}
			}
			TheDelay theDelay = new TheDelay(now, delayValue, isBad);
			queue.offer(theDelay);
			if (isBad) {
				++countOfBadValue;
			} else {
				sumOfGoodValue += theDelay.getDelayValue();
			}
		}

		public int getBadPercent() {
			int total = queue.size();
			return total == 0 ? 0 : countOfBadValue * 100 / total;
		}

		/**
		 * 返回所有“Good”值的平均值
		 * @return 所有“Good延迟值”的平均值，如果没有符合条件的延迟值，返回-1
		 */
		public int getAverageOfGoodValue() {
			int count = queue.size() - countOfBadValue;
			return count <= 0 ? -1 : sumOfGoodValue / count;
		}
	}

	public FirstSegmentNetDelayContainer() {
		reset(GlobalDefines.NET_DELAY_TEST_WAIT);
	}

	public void reset(int value) {
		this.raw = this.adjusted = value;
	}

	public void onGameForeground(long now) {
		// 清除队列里时间超过半小时的数据
		long time = now - 1000 * 60 * 30;
		history.removeAllExpired(time);
	}

	/**
	 * 根据给定的延迟值和当前网络类型，判断给定的延迟值是否属于“质量较差”的延迟值
	 * 
	 * @param value
	 *            给定的延迟值
	 * @param netType
	 *            当前网络类型 ({@link NetTypeDetector.NetType}
	 * @return true=质量较差
	 */
	public static boolean isBadDelay(int value, NetType netType) {
		if (value < 0 || value >= 150) {
			return true;
		}
		switch (netType) {
		case WIFI:
			return value >= 80;
		case MOBILE_4G:
			return value >= 100;
		case MOBILE_3G:
			return value >= 150;
		default:
			return false;
		}
	}

	/**
	 * 收到网络延迟值
	 */
	public void offerDelayValue(int rawUDPDelay, int delayValue, NetType netType, long now) {
		history.offer(now, rawUDPDelay, isBadDelay(rawUDPDelay, netType));
		//
		// 如果是异常值（负值或超时），当上一次真实值不是异常时
		// 需要缓存异常值，返回调整值（上一次真实值）
		if (isDelayValueException(delayValue) && !isDelayValueException(this.raw)) {
			this.adjusted = this.raw;
			this.raw = delayValue;
		} else {
			this.raw = this.adjusted = delayValue;
		}
	}

	/**
	 * 返回最近的、至多150条延迟记录里，异常值所占百分比
	 * 
	 * @return 0~100的值
	 */
	public int getBadPercent() {
		return history.getBadPercent();
	}

	public int getAdjusted() {
		return this.adjusted;
	}

	/**
	 * 返回所有“Good”值的平均值
	 * @return 所有“Good延迟值”的平均值，如果没有符合条件的延迟值，返回-1
	 */
	public int getAverageOfGoodValue() {
		return this.history.getAverageOfGoodValue();
	}

	//	public int getRaw() {
	//		return this.raw;
	//	}

	/**
	 * 判断给定的延迟值是否异常？
	 * 
	 * @param milliseconds
	 *            给定的延迟值，单位毫秒
	 * @return true，表示给定的延迟值是一个表示异常状态的值
	 */
	private static boolean isDelayValueException(int milliseconds) {
		return milliseconds < 0 || milliseconds >= GlobalDefines.NET_DELAY_TIMEOUT;
	}

	public void adjustWithWiFiAccel(int value) {
		this.adjusted = value;
	}
}
