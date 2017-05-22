package cn.wsds.gamemaster.netdelay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;

import android.text.TextUtils;
import android.util.Log;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

import com.subao.utils.FileUtils;
import com.subao.utils.Misc;

/**
 * 保存每个游戏运行时的网络时延数据
 */
public class NetDelayDataManager extends Observable {

	private static final boolean LOG = false;
	private static final String TAG = "NetDelayDataManager";

	private static final String DIR_NAME = "nd_data";

	/**
	 * 时间点
	 */
	public static class TimePoint {

		public static final int EVENT_NONE = 0;					// 啥事件也没有
		public static final int EVENT_RECONNECT_SUCCEED = 1;	// 断线重连成功
		public static final int EVENT_RECONNECT_FAIL = 2;		// 断线重连失败
		public static final int EVENT_RECONNECT_BEGIN = 4;		// 断线重连开始
		public static final int EVENT_BACKGROUND = 8;			// 游戏切到后台

		public final long time;		// 时刻，UTC毫秒数
		public final int value;		// 延时值
		public final int event;		// 额外的一些事件

		public TimePoint(long time, int value, int event) {
			this.time = time;
			this.value = value;
			this.event = event;
		}

		public boolean hasEvent(int event) {
			return (this.event & event) != 0;
		}

	}

	public static class TimePointContainer {
		private static final int MAX_COUNT_OF_TIME_POINT = (5 * 60 + 10) / 2; // 最多保留多少个时间点？（5分钟，每2秒一个，所以是150个，还要预留10秒的冗余）

		private int howManyRecordsNeedSave;		// 自上次存盘以来，又新增了多少条记录需要存盘？	

		private final Deque<TimePoint> queue = new LinkedList<NetDelayDataManager.TimePoint>();

		/**
		 * 获取一个正向迭代器：从较早时刻到较晚时刻
		 */
		public Iterator<TimePoint> iterator() {
			return queue.iterator();
		}

		/** 获取一个反向迭代器：从较晚时刻到较早时刻 */
		public Iterator<TimePoint> descendingIterator() {
			return queue.descendingIterator();
		}

		/** 本容器里有多少个TimePoint */
		public int getCount() {
			return queue.size();
		}

		/**
		 * 取本容器里数据的最早时刻
		 * 
		 * @return UTC毫秒数。如果容器为空，则返回-1
		 */
		public long getBeginTime() {
			return getTime(queue.peekFirst());
		}

		/**
		 * 取本容器里数据的最晚时刻
		 * 
		 * @return UTC毫秒数。如果容器为空，则返回-1
		 */
		public long getEndTime() {
			return getTime(queue.peekLast());
		}

		private long getTime(TimePoint tp) {
			return tp == null ? -1 : tp.time;
		}

		/** 在队列里加入新的时间点 */
		private void offer(TimePoint tp) {
			if (queue.size() >= MAX_COUNT_OF_TIME_POINT) {
				queue.poll();
			}
			queue.offer(tp);
			++howManyRecordsNeedSave;
		}

		private void save(ByteBuffer buf) {
			for (TimePoint tp : queue) {
				buf.putLong(tp.time);
				buf.putShort((short) tp.value);
				buf.putShort((short) tp.event);
				buf.putInt(0);
			}
		}

		private static TimePointContainer load(ByteBuffer buf) {
			TimePointContainer container = new TimePointContainer();
			container.queue.clear();
			while (true) {
				try {
					long time = buf.getLong();	// 时刻，单位“秒”
					int value = buf.getShort();	// 延时值
					int event = buf.getShort();	// 事件
					buf.getInt();				// 保留未用的4字节
					container.queue.add(new TimePoint(time, value, event));
				} catch (BufferUnderflowException b) {
					break;
				}
			}
			return container;
		}

		private void setLastTimePointEvent(int event) {
			TimePoint tp = queue.pollLast();
			if (tp != null) {
				TimePoint tpNew = new TimePoint(tp.time, tp.value, tp.event | event);
				queue.offer(tpNew);
				++this.howManyRecordsNeedSave;
			}
		}

		/** 调整数据：从最近时间点前推，如果未满足“在前台达10秒”这个条件，则这部分数据弃掉 */
		private void deleteIfTimeRangeTooShort(long gameEnterTime) {
			if (queue.isEmpty()) {
				return;
			}
			TimePoint tp1 = null;
			TimePoint tp2 = null;
			Iterator<TimePoint> it = queue.descendingIterator();
			while (it.hasNext()) {
				TimePoint tp = it.next();
				if (tp.time < gameEnterTime) {
					break;
				}
				if (tp2 == null) {
					tp2 = tp;
				} else {
					tp1 = tp;
				}
			}
			if (tp2 == null) {
				return;
			}
			if (tp1 == null) {
				// 只需要删除TP2
				queue.pollLast();
				return;
			}
			if (tp2.time - tp1.time >= 10 * 1000) {
				return;
			}
			while (true) {
				TimePoint tp = queue.pollLast();
				if (tp == null || tp == tp1) {
					break;
				}
			}
		}

	}

	private static final NetDelayDataManager instance = new NetDelayDataManager();

	private final HashMap<String, TimePointContainer> container = new HashMap<String, NetDelayDataManager.TimePointContainer>();

	private GameInfo currentGame;
	private TimePointContainer currentTPC;
	private long currentGameBeginTime;

	public static NetDelayDataManager getInstance() {
		return instance;
	}

	private NetDelayDataManager() {
		TriggerManager.getInstance().addObserver(new EventObserver() {

			@Override
			public void onFirstSegmentNetDelayChange(int delayMilliseconds) {
				if (currentTPC != null && delayMilliseconds != GlobalDefines.NET_DELAY_TEST_WAIT && AccelOpenManager.isStarted()) {
					addNetDelay(currentTPC, currentGame.getPackageName(), System.currentTimeMillis(), delayMilliseconds);
				}
			}

			@Override
			public void onTopTaskChange(GameInfo info) {
				if (currentGame == info) {
					return;
				}
				// 如果当前游戏不为空，处理
				if (currentTPC != null) {
					currentTPC.deleteIfTimeRangeTooShort(currentGameBeginTime);
					currentTPC.setLastTimePointEvent(TimePoint.EVENT_BACKGROUND);
					saveTimePointContainerToFile(currentTPC, currentGame.getPackageName(), null);
				}
				// 当前游戏等于新游戏
				currentGame = info;
				if (currentGame == null) {
					currentTPC = null;
				} else {
					// 为currentTPC赋值
					currentGameBeginTime = System.currentTimeMillis();
					String packageName = currentGame.getPackageName();
					currentTPC = container.get(packageName);
					if (currentTPC == null) {
						currentTPC = createTimePointContainerFromFile(packageName);
						if (currentTPC == null) {
							currentTPC = new TimePointContainer();
						}
						container.put(packageName, currentTPC);
					}
				}
			}

			@Override
			public void onReconnectResult(ReconnectResult result) {
				if (currentGame != null && currentGame.getUid() == result.uid) {
					if (result.success || result.count == GlobalDefines.MAX_COUNT_OF_CONNECTION_REPAIR || result.count == 1) {
						TimePointContainer tpc = container.get(currentGame.getPackageName());
						if (tpc != null) {
							int event;
							if (result.success) {
								event = TimePoint.EVENT_RECONNECT_SUCCEED;
							} else if (result.count == 1) {
								event = TimePoint.EVENT_RECONNECT_BEGIN;
							} else {
								event = TimePoint.EVENT_RECONNECT_FAIL;
							}
							tpc.setLastTimePointEvent(event);
						}
					}
				}
			}
		});
	}

	/**
	 * 添加一条延时记录
	 * 
	 * @param tpc
	 *            时间点容器
	 * @param time
	 *            时刻
	 * @param delay
	 *            延时
	 */
	private void addNetDelay(TimePointContainer tpc, String packageName, long time, int delay) {
		time = ((time + 1000) / 2000) * 2000;	// 对齐到2秒边界
		tpc.offer(new TimePoint(time, delay, TimePoint.EVENT_NONE));
		if (tpc.howManyRecordsNeedSave >= 150) {
			saveTimePointContainerToFile(tpc, packageName, null);
		}
		this.setChanged();
		this.notifyObservers(tpc);
	}

	private static TimePointContainer createTimePointContainerFromFile(String packageName) {
		File file = createDataFile(packageName, false);
		if (file == null || !file.exists() || !file.isFile()) {
			if (LOG) {
				Log.d(TAG, "File not found: " + packageName);
			}
			return null;
		}
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
			ByteBuffer buf = allocateByteBuffer();
			int bytes = input.read(buf.array());
			if (bytes <= 0) {
				return null;
			}
			buf.rewind();
			buf.limit(bytes);
			TimePointContainer tpc = TimePointContainer.load(buf);
			if (LOG) {
				Log.d(TAG, String.format("Load %d time points from file: %s", tpc.getCount(), file.getAbsolutePath()));
			}
			return tpc;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Misc.safeClose(input);
		}
		return null;
	}

	public TimePointContainer getTimePoints(String packageName) {
		// 如果包名为空，表明是取当前的，直接返回currentTPC
		if (TextUtils.isEmpty(packageName)) {
			return currentTPC;
		}
		// 想取的这个游戏的数据，正好是当前游戏吗？
		if (currentGame != null && packageName.equals(currentGame.getPackageName())) {
			currentTPC.deleteIfTimeRangeTooShort(currentGameBeginTime);
			return currentTPC;
		}
		// 从容器里取
		TimePointContainer tpc = container.get(packageName);
		if (tpc == null) {
			// 尝试从文件加载
			tpc = createTimePointContainerFromFile(packageName);
			if (tpc == null) {
				tpc = new TimePointContainer();
				container.put(packageName, tpc);
			}
		}
		return tpc.getCount() > 0 ? tpc : null;
	}

	/**
	 * 将网络延时数据存盘
	 */
	public void save() {
		File dir = FileUtils.createDirectoryUnderData(DIR_NAME);
		if (dir == null) {
			return;
		}
		ByteBuffer buf = allocateByteBuffer();
		Iterator<Map.Entry<String, TimePointContainer>> it = container.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, TimePointContainer> item = it.next();
			TimePointContainer tpc = item.getValue();
			saveTimePointContainerToFile(tpc, item.getKey(), buf);
		}
	}

	private static void saveTimePointContainerToFile(TimePointContainer tpc, String packageName, ByteBuffer buf) {
		if (tpc.howManyRecordsNeedSave <= 0) {
			return;
		}
		if (buf == null) {
			buf = allocateByteBuffer();
		} else {
			buf.rewind();
		}
		tpc.save(buf);
		File file = createDataFile(packageName, true);
		if (file != null) {
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(file, false);
				output.write(buf.array(), 0, buf.position());
				tpc.howManyRecordsNeedSave = 0;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				Misc.safeClose(output);
			}
		}
	}

	private static ByteBuffer allocateByteBuffer() {
		return ByteBuffer.allocate(16 * TimePointContainer.MAX_COUNT_OF_TIME_POINT);
	}

	private static File createDataFile(String packageName, boolean createDirectoryIfNotExists) {
		File dir;
		if (createDirectoryIfNotExists) {
			dir = FileUtils.createDirectoryUnderData(DIR_NAME);
			if (dir == null) {
				return null;
			}
		} else {
			dir = new File(FileUtils.getDataDirectory(), DIR_NAME);
			if (!dir.exists() || !dir.isDirectory()) {
				return null;
			}
		}
		return new File(dir, packageName);
	}
}
