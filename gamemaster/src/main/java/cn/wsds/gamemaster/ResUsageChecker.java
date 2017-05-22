package cn.wsds.gamemaster;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import cn.wsds.gamemaster.data.AppProfile;
import cn.wsds.gamemaster.tools.SystemInfoUtil;

/**
 * Created by hujd on 15-10-26. 定时将内存占用、CPU占用和后台进程数进行上报
 */
public class ResUsageChecker {

	private static final ResUsageChecker instance = new ResUsageChecker();

	/**
	 * 内存、CPU占用和运行APP数据
	 */
	public static class ResUsage {
		/** CPU占用百分比 [0,100] */
		public final int cpuUsage;
		/** 内存占用百分比 [0,100] */
		public final int memoryUsage;
		/** 当前正在运行的APP列表 */
		public final List<AppProfile> runningAppList;

		public ResUsage(int cpuUsage, int memoryUsage, List<AppProfile> runningAppList) {
			this.cpuUsage = cpuUsage;
			this.memoryUsage = memoryUsage;
			this.runningAppList = runningAppList;
		}

		@Override
		public String toString() {
			return String.format("[cpu=%d%%, mem=%d%%, apps=%d]", cpuUsage, memoryUsage, runningAppList == null ? 0 : runningAppList.size());
		}
	}

	/**
	 * 观察者。当规定的时间点到达时，被通知。
	 */
	public static interface Observer {
		/**
		 * 被通知：当前的资源占用情况
		 */
		public void onResUsageCheckResult(ResUsage resUsage);
	}

	/**
	 * 使用者（包含两个要素：“观察者”和“间隔时长”）
	 */
	private static class User {
		public final Observer observer;
		public final long interval;

		public User(Observer observer, long interval) {
			this.observer = observer;
			this.interval = interval;
		}
	}

	/**
	 * 负责通知观察者，被Post到主线程里使用
	 */
	private static class ObserverNotifier implements Runnable {

		private final Observer observer;
		private final ResUsage resUsage;

		public ObserverNotifier(Observer observer, ResUsage resUsage) {
			this.observer = observer;
			this.resUsage = resUsage;
		}

		@Override
		public void run() {
			observer.onResUsageCheckResult(resUsage);
		}

	}

	/**
	 * 工作Handler，运行在“非主线程”里
	 */
	private class WorkerHandler extends Handler {

		private static final int MSG_CHECK = 1;

		private long timeOfCacheExpired;
		private ResUsage resUsageCache;

		public WorkerHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what != MSG_CHECK) {
				return;
			}
			synchronized (userList) {
				for (User user : userList) {
					if (user.observer == msg.obj) {
						MainHandler.getInstance().post(new ObserverNotifier(user.observer, getResUsage()));
						this.postCheckDelayed(user);
						break;
					}
				}
			}
		}

		/**
		 * 针对指定的{@link ResUsageChecker.User}，延时触发检查
		 */
		public void postCheckDelayed(User user) {
			this.sendMessageDelayed(this.obtainMessage(MSG_CHECK, user.observer), user.interval);
		}

		public void remove(Observer observer) {
			this.removeMessages(MSG_CHECK, observer);
		}

		/**
		 * 从缓存取（或当缓存过期时重新计算）资源占用数据
		 */
		private ResUsage getResUsage() {
			long now = SystemClock.elapsedRealtime();
			if (resUsageCache == null || now >= timeOfCacheExpired) {
				resUsageCache = buildResUsage();
				timeOfCacheExpired = now + 5000;	// 缓存5秒
			}
			return resUsageCache;
		}

		/**
		 * 重新计算资源占用
		 */
		private ResUsage buildResUsage() {
			List<AppProfile> runningAppList = SystemInfoUtil.getRunningAppList();
			if (debugUsage != null) {
				return new ResUsage(debugUsage.cpuUsage, debugUsage.memoryUsage, runningAppList);
			}
			int cpuUsage = SystemInfoUtil.getCpuUsage();
			int memoryUsage = SystemInfoUtil.getMemoryUsage(AppMain.getContext());
			return new ResUsage(cpuUsage, memoryUsage, runningAppList);
		}

	}

	private final List<User> userList = new ArrayList<User>(4);

	private WorkerHandler mHandler;

	private ResUsage debugUsage;

	/**
	 * 隐藏规则：这个函数第一次被调用应该在主线程里
	 */
	public static ResUsageChecker getInstance() {
		return instance;
	}

	private ResUsageChecker() {}

	/**
	 * 进入一个检查观察者
	 * 
	 * @param observer
	 *            观察者{@link Observer}
	 * @param interval
	 *            检查时间间隔。
	 */
	public void enter(Observer observer, long interval) {
		if (observer == null) {
			return;
		}
		synchronized (userList) {
			for (User user : userList) {
				if (observer == user.observer) {
					return;
				}
			}
			//
			if (mHandler == null) {
				HandlerThread handlerThread = new HandlerThread("res_usage_checker");
				handlerThread.setPriority(Thread.MIN_PRIORITY);
				handlerThread.start();
				mHandler = new WorkerHandler(handlerThread.getLooper());
			}
			//
			User user = new User(observer, interval);
			userList.add(user);
			mHandler.postCheckDelayed(user);
		}
	}

	/**
	 * 指定的观察都退出检查序列（比如，当ActivityMain没显示时，就不需要扫描）
	 * 
	 * @param observer
	 *            与调用{@link #enter(Observer, long)}时传递的值一样
	 * @see #enter(Observer, long)
	 */
	public synchronized void leave(Observer observer) {
		if (observer == null) {
			return;
		}
		synchronized (userList) {
			for (int i = userList.size() - 1; i >= 0; --i) {
				if (observer == userList.get(i).observer) {
					userList.remove(i);
					break;
				}
			}
			if (mHandler != null) {
				mHandler.remove(observer);
				if (userList.isEmpty()) {
					mHandler.getLooper().quit();
					mHandler = null;
				}
			}
		}
	}

	/** 测试使用 */
	public void setDebugData(ResUsage debugUsage) {
		this.debugUsage = debugUsage;
	}

	/** 测试使用 */
	public ResUsage getDebugUsage() {
		return debugUsage;
	}
}
