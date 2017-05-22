package cn.wsds.gamemaster.event;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.os.Handler;

/**
 * 断线重连事件管理器
 */
public class ReconnectEventManager {
	
	private final static int MSG_BEGIN = 1;
	
	private static final ReconnectEventManager instance = new ReconnectEventManager();
	
	public static ReconnectEventManager getInstance() {
		return instance;
	}
	
	/** 每一种（UID+TASKID）事件的队列 */
	private static class Entry {
		public final int uid;
		public final int taskId;
		public final List<EventObserver.ReconnectResult> queue = new ArrayList<EventObserver.ReconnectResult>();
		public Entry(int uid, int taskId) {
			this.uid = uid;
			this.taskId = taskId;
		}
	}
	
	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what != MSG_BEGIN) {
				return;
			}
			// 在所有缓存的事件里找UID和TASKID相同的
			int idx = findEntryIndex(msg.arg1, msg.arg2);
			if (idx < 0) {
				return;
			}
			// 找到了，从容器里移除，然后针对每一个事件进行触发
			Entry e = container.get(idx);
			container.remove(idx);
			for (EventObserver.ReconnectResult rr : e.queue) {
				TriggerManager.getInstance().raiseReconnectResult(rr);
			}
		}
	};
	
	private final List<Entry> container = new ArrayList<Entry>();
	
	private ReconnectEventManager() { }
	
	private int findEntryIndex(int uid, int taskId) {
		for (int i = 0; i < container.size(); ++i) {
			Entry e = container.get(i);
			if (e.uid == uid && e.taskId == taskId) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * 收到底层传来的断线重连事件时，调用此方法
	 * @param rr 相关参数
	 */
	public void addEvent(EventObserver.ReconnectResult rr) {
		int idx = findEntryIndex(rr.uid, rr.taskId);
		if (rr.count < 1) {
			if (idx < 0) {
				container.add(new Entry(rr.uid, rr.taskId));
				handler.sendMessageDelayed(handler.obtainMessage(MSG_BEGIN, rr.uid, rr.taskId), 2000);
			}
			return;
		}
		// 不是第0次
		if (idx >= 0) {
			Entry e = container.get(idx);
			e.queue.add(rr);
		} else {
			TriggerManager.getInstance().raiseReconnectResult(rr);
		}
	}
}