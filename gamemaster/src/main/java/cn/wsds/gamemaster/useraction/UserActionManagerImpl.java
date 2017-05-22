package cn.wsds.gamemaster.useraction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.net.Http;
import com.subao.net.NetManager;
import com.subao.utils.FileUtils;

class UserActionManagerImpl extends UserActionManager {

	private final static String TAG = LogTag.MESSAGE;

	private static HandlerThread createThreadAndStart() {
		HandlerThread ht = new HandlerThread("UserActionManager");
		ht.start();
		return ht;
	}

	@SuppressLint("HandlerLeak")
	private class MyHandler extends Handler {

		/** 存储UserAction的本地文件名 */
		private static final String FILE_NAME = ".ua2.data";

		/** 是否已经执行过Load操作？ */
		private boolean already_loaded;

		/** 上一次向服务器发送数据的时间 */
		private long last_post_time;

		public MyHandler() {
			super(createThreadAndStart().getLooper());
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TRY_POST:
				postToServer();
				saveToFileIfNeed();
				this.sendEmptyMessageDelayed(MSG_TRY_POST, TIME_PERIOD_TRY_POST);
				break;
			case MSG_STOP:
				Logger.d(TAG, "Try to stop ...");
				saveToFileIfNeed();
				this.getLooper().quit();
				Logger.d(TAG, "Work thread exit.");
				break;
			case MSG_LOAD_FROM_FILE:
				loadFromFile();
				break;
			}
		}

		/**
		 * 从文件中加载历史数据
		 */
		private void loadFromFile() {
			if (this.already_loaded) {
				return;
			}
			this.already_loaded = true;
			byte[] bytes = FileUtils.read(FileUtils.getDataFile(FILE_NAME));
			if (bytes != null) {
				try {
					Collection<UserActionList> data = serializer.unserializeList(bytes, true);
					if (data != null && !data.isEmpty()) {
						Logger.d(TAG, String.format("Load %d item(s)", data.size()));
						queue.offerFirst(data);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				Logger.d(TAG, "No user action data loaded, maybe first run.");
			}
			sendMessage_TryPost();
		}

		/**
		 * 如果需要，将数据保存到文件中
		 */
		private void saveToFileIfNeed() {
			if (need_save.get() <= 0) {
				return;
			}
			if (!already_loaded) {
				this.loadFromFile();
				if (!already_loaded) {
					Logger.w(TAG, "Try to save, but load from file failed");
					return;
				}
			}
			try {
				byte[] bytes = queue.serialize(serializer);
				File file = FileUtils.getDataFile(FILE_NAME);
				if (FileUtils.write(file, bytes)) {
					if (Logger.isLoggableDebug(TAG)) {
						Logger.d(TAG, String.format("Save %d bytes ok", bytes.length));
					}
					need_save.set(0);
				} else {
					Logger.w(TAG, "Save failed");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		/**
		 * 尝试将数据Post到Server
		 */
		private void postToServer() {
			if (queue.isEmpty()) {
				return;
			}
			NetManager netManager = NetManager.getInstance();
			if (!netManager.isConnected()) {
				return;
			}
			long now = System.currentTimeMillis();
			if (netManager.isMobileConnected()) {
				// 3G环境
				if (now - last_post_time < TIME_PERIOD_POST_IN_3G) {
					return;
				}
			} else if (!netManager.isWiFiConnected()) {
				return;
			}
			//
			Http http = new Http(10 * 1000, 10 * 1000);
			while (true) {
				UserActionList ual = queue.poll();
				if (ual == null) {
					// 取到null，说明已取完，跳出循环
					break;
				}
				if (ual.isEmpty()) {
					Logger.w(TAG, "UserActionList is empty ??? !!!");
					continue;
				}
				if (this.doPost(http, ual)) {
					this.last_post_time = now;
					need_save.incrementAndGet();	// 队列发生了改变，需要存盘
				} else {
					Logger.d(TAG, "Post fail, try again next time");
					queue.offerFirst(ual); // 发送失败，把取出来的放回去
					break; // 没必要再试了，下次再说
				}
			}
		}

		private boolean doPost(Http http, UserActionList ual) {
			byte[] data = serializer.serializeSingle(ual);
			try {
				Http.Response response = http.doPost(postUrl, data, Http.ContentType.PROTOBUF.str);
				boolean ok = (response.code >= 200 && response.code < 300);
				if (Logger.isLoggableDebug(TAG)) {
					Logger.d(TAG, String.format("Try to post %d bytes %s, response code = %d", data.length, ok ? "ok" : "fail", response.code));
				}
				return ok;
			} catch (IOException e) {
				return false;
			} catch (RuntimeException e) {
				// 在某些设备上，可能会抛RuntimeException（比如权限被禁用之类的）
				return false;
			}
		}

	}

	private final Serializer serializer;
	private final URL postUrl;

	private final UserActionListQueue queue;
	private final Handler handler;

	/** 是否发生了数据改变，需要存盘？ */
	private final AtomicInteger need_save = new AtomicInteger();

	private final static int MSG_TRY_POST = 1;					// 一个定时消息，尝试Post
	private final static int MSG_STOP = 2;						// 停止
	private final static int MSG_LOAD_FROM_FILE = 3;			// 从设备存储加载

	private final static long TIME_PERIOD_TRY_POST = (1 * 60 * 1000);		// 每分钟尝试一下Post
	private final static long TIME_PERIOD_POST_IN_3G = (10 * 60 * 1000);	// 如果一直在3G环境下，超过此时间，也试下Post

	UserActionManagerImpl(VersionInfo versionInfo, Serializer serializer, URL postUrl) {
		this.queue = new UserActionListQueue(versionInfo);
		this.serializer = serializer;
		this.postUrl = postUrl;
		Logger.d(TAG, "Post URL: " + postUrl);
		this.handler = new MyHandler();
		this.handler.sendEmptyMessage(MSG_LOAD_FROM_FILE);
	}

	@Override
	public void updateSubaoId(String subaoId) {
		this.queue.updateSubaoId(subaoId);
	}
	
	@Override
	public void udpateUserId(String userId) {
		this.queue.updateUserId(userId);		
	}

	@Override
	public void onWiFiActivated() {
		sendMessage_TryPost();
	}

	private void sendMessage_TryPost() {
		handler.removeMessages(MSG_TRY_POST);
		handler.sendEmptyMessage(MSG_TRY_POST);
	}

	@Override
	public void stopAndWait(long milliseconds) {
		this.handler.removeCallbacksAndMessages(null);
		this.handler.sendEmptyMessage(MSG_STOP);
		try {
			Thread t = this.handler.getLooper().getThread();
			if (t != null) {
				t.join(milliseconds);
			}
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void addAction(long timeUTCSeconds, String actionName, String param) {
		UserAction ua = new UserAction(timeUTCSeconds, actionName, param);
		if (this.queue.offer(ua)) {
			if (this.need_save.incrementAndGet() > 8) {
				sendMessage_TryPost();
			}
		}
	}

	/**
	 * UserActionList队列
	 * 
	 */
	private static class UserActionListQueue {

		/** 在queue里最多只保留这么多条，免得过大 */
		private final static int MAX_SIZE_OF_QUEUE = 10000;

		/** SubaoId。如果没有SubaoId，则丢弃要添加的UserAction */
		private String subaoId;
		
		private String userId;

		private final VersionInfo versionInfo;

		/** UersAction的队列 */
		private final LinkedList<UserActionList> queue = new LinkedList<UserActionList>();

		/** 队列里的最后一项 */
		private UserActionList tail;

		UserActionListQueue(VersionInfo versionInfo) {
			this.versionInfo = versionInfo;
			Logger.d(TAG, "VersionInfo: " + versionInfo);
		}

		/**
		 * SubaoId发生改变
		 */
		public synchronized void updateSubaoId(String subaoId) {
			this.subaoId = subaoId;
			this.tail = null; // SubaoId更新了，以后的UserAction加到新的UserActionList里，所以tail要置空
			mergeTail();
		}
		
		public synchronized void updateUserId(String userId) {
			this.userId = userId;
			if (Logger.isLoggableDebug(TAG)) {
				Log.d(TAG, String.format("UserActionManager, userId = \"%s\"", userId));
			}
		}

		/**
		 * 从队列里弹出一个{@link UserActionList}
		 * 
		 * @return null表示队列已空
		 */
		public synchronized UserActionList poll() {
			UserActionList result = this.queue.poll();
			if (result == tail) {
				tail = null; // 最后一项已经弹出，tail置空
			}
			return result;
		}

		/**
		 * 添加{@link UserAction}
		 */
		public synchronized boolean offer(UserAction userAction) {
			if (null == subaoId) {
				Logger.d(TAG, "UserAction ignored: " + userAction.name);
				return false;
			}
			if (tail == null) {
				// tail为空，需要新建一项
				if (this.queue.size() >= MAX_SIZE_OF_QUEUE) {
					this.queue.poll();
				}
				tail = new UserActionList(subaoId, userId, versionInfo);
				this.queue.offer(tail);
			}
			tail.offer(userAction);
			if (tail.size() >= UserActionList.MAX_CAPACITY) {
				// 队列里最后一项已达到推荐的上限值，tail置空（下次再来数据的时候会新建UserActionList）
				tail = null;
			}
			//
//			outputDebugLog();
			return true;
		}

		private void mergeTail() {
			if (tail != null || this.subaoId == null) {
				return;
			}
			UserActionList ualLast = this.queue.peekLast();
			if (ualLast == null || ualLast.size() >= UserActionList.MAX_CAPACITY) {
				return;
			}
			if (!com.subao.common.utils.StringUtils.isStringEqual(ualLast.subaoId, this.subaoId)) {
				return;
			}
			if (!this.versionInfo.equals(ualLast.versionInfo)) {
				return;
			}
			tail = ualLast;
//			outputDebugLog();
		}

		/**
		 * 将指定的UserActionList插入到队列头
		 */
		public synchronized void offerFirst(UserActionList ual) {
			if (ual == null || ual.isEmpty()) {
				return;
			}
			if (this.queue.size() < MAX_SIZE_OF_QUEUE) {
				this.queue.offerFirst(ual);
				mergeTail();
			}
		}

		/**
		 * 将指定的UserActionList容器插入到队列头
		 */
		public void offerFirst(Collection<UserActionList> c) {
			if (c == null || c.isEmpty()) {
				return;
			}
			synchronized (this) {
				if (this.queue.size() < MAX_SIZE_OF_QUEUE) {
					this.queue.addAll(0, c);
					mergeTail();
				}
			}
		}

//		private void outputDebugLog() {
//			int count = 0;
//			for (UserActionList ual : queue) {
//				count += ual.size();
//			}
//			Logger.d(TAG,
//				"Queue count = %d, Tail = %s, Count of UserAction = %d",
//				this.queue.size(),
//				tail == null ? "null" : Integer.toString(tail.size()),
//				count);
//		}

		/**
		 * 将自己序列化
		 */
		public synchronized byte[] serialize(Serializer serializer) {
			return serializer.serializeList(this.queue);
		}

		public synchronized boolean isEmpty() {
			return this.queue.isEmpty();
		}

	}

}
