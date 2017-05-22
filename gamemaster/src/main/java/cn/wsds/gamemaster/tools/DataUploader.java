package cn.wsds.gamemaster.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.GZIPOutputStream;

import android.os.AsyncTask;
import android.text.TextUtils;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.pb.Proto;
import cn.wsds.gamemaster.statistic.Statistic;

import com.google.protobuf.ByteString;
import com.subao.common.net.Http;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.ThreadUtils;
import com.subao.net.NetManager;
import com.subao.utils.SubaoHttp;
import com.subao.utils.UrlConfig;

/**
 * 数据上传
 */
public class DataUploader {

	/**
	 * 执行HTTP上传成功或失败的回调
	 */
	public static interface OnUploadCompletedCallback {
		/**
		 * HTTP POST完成后，由DataUplodaer在UI线程里回调此方法
		 * 
		 * @param succeeded
		 *            true=成功，false=失败
		 * @param data
		 *            要上传的数据
		 * @return 返回一个boolean值，指明当上传失败后，DataUploader应该如何继续下一步。<br />
		 *         true表示由DataUploader负责重试上传，<br />
		 *         false表示 DataUplodaer不要再重试了，上层自己会处理<br />
		 *         如果上传成功，此返回值无意义。
		 */
		boolean onUploadCompleted(boolean succeeded, byte[] data);
	}

	private static DataUploader instance;

	public static DataUploader getInstance() {
		if (GlobalDefines.CHECK_MAIN_THREAD) {
			if (!ThreadUtils.isInAndroidUIThread()) {
				MainHandler.getInstance().showDebugMessage("DataUploader.getInstance() invoked by non-main thread");
			}
		}
		if (instance == null) {
			instance = new DataUploader();
		}
		return instance;
	}

	private static class Entry {
		public final byte[] data;
		public final int type;
		public final OnUploadCompletedCallback callback;
		public final String desc;
		public final long delayed;

		public Entry(long delayed, byte[] data, int type, OnUploadCompletedCallback callback, String desc) {
			this.delayed = delayed;
			this.data = data;
			this.type = type;
			this.callback = callback;
			this.desc = desc;
		}

//		public Entry(long delayed, byte[] data, int type, OnUploadCompletedCallback callback) {
//			this(delayed, data, type, callback, null);
//		}
	}

	//private static final int TYPE_ERROR_LOG = 0;
	//private static final int TYPE_INSTALLED_LIST = 1;
	//	@Deprecated
	//	private static final int TYPE_GAME_LOG = 2;
	//	@Deprecated
	//	private static final int TYPE_NET_DELAY = 3;
	private static final int TYPE_DEBUG_DUMP = 4;
	//	private static final int TYPE_SPEED_TEST = 5;		// 测速结果
	//private static final int TYPE_QUESTION_SUVERY = 7;		//6已经被占用上传问卷调查数据

	private final Queue<Entry> queue = new LinkedList<Entry>();

	private DataUploader() {
		TriggerManager.getInstance().addObserver(new EventObserver() {
			@Override
			public void onNetChange(NetTypeDetector.NetType state) {
				if (NetManager.getInstance().isConnected()) {
					processNextEntry();
				}
			}
		});
	}

	private void processNextEntry() {
		if (queue.isEmpty()) {
			return;
		}
		if (NetManager.getInstance().isDisconnected()) {
			return;
		}
		Entry entry = queue.poll();
		if (entry == null) {
			return;
		}
		Task task = new Task(this, entry);
		task.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
	}

//	private void addEntry(long delayed, byte[] data, int type, OnUploadCompletedCallback callback) {
//		addEntry(new Entry(delayed, data, type, callback));
//	}

	private void addEntry(Entry entry) {
		queue.add(entry);
		processNextEntry();
	}

//	/**
//	 * 添加一个上传数据，内容是安装列表
//	 * 
//	 * @param data
//	 *            数据
//	 * @param callback
//	 *            回调
//	 */
//	public void addInstalledList(byte[] data, OnUploadCompletedCallback callback) {
//		addEntry(5000, data, TYPE_INSTALLED_LIST, callback);
//	}

//	/**
//	 * 添加一个上传数据，内容是错误日志
//	 * 
//	 * @param data
//	 *            数据
//	 * @param callback
//	 *            回调
//	 */
//	public void addErrorLog(byte[] data, OnUploadCompletedCallback callback) {
//		addEntry(1000, data, TYPE_ERROR_LOG, callback);
//	}

	/**
	 * 添加一个上传数据，内容是游戏日志
	 * 
	 * @param data
	 *            数据
	 * @param callback
	 *            回调
	 */
	//	@Deprecated
	//	public void addGameLog(byte[] data, OnUploadCompletedCallback callback) {
	//		addEntry(data, TYPE_GAME_LOG, callback);
	//	}

	/**
	 * 添加一个上传数据，内容是延时数据
	 * 
	 * @param data
	 *            数据
	 * @param callback
	 *            回调
	 */
	//	@Deprecated
	//	public void addNetDelayLog(byte[] data, OnUploadCompletedCallback callback) {
	//		addEntry(data, TYPE_NET_DELAY, callback);
	//	}

	//    /**
	//     * 添加一个上传数据，问卷调查数据
	//     *
	//     * @param data
	//     *            数据
	//     * @param callback
	//     *            回调
	//     */
	//    public void addQuestionSuveryData(byte[] data, OnUploadCompletedCallback callback) {
	//        addEntry(data, TYPE_QUESTION_SUVERY, callback);
	//    }
	/**
	 * 添加一个上传数据，内容是DEBUG信息（已压缩成ZIP）
	 * 
	 * @param data
	 * @param desc
	 * @param callback
	 */
	public void addDebugDump(byte[] data, String desc, OnUploadCompletedCallback callback) {
		addEntry(new Entry(1000, data, TYPE_DEBUG_DUMP, callback, desc));
	}

	//	/**
	//	 * 添加一个上传数据，内容是网速测试
	//	 * @param data
	//	 * @param callback
	//	 */
	//	public void addSpeedTest(byte[] data, OnUploadCompletedCallback callback) {
	//		addEntry(data, TYPE_SPEED_TEST, callback);
	//	}

	/**
	 * 负责执行上传操作的后台线程
	 */
	private static class Task extends AsyncTask<Void, Void, Boolean> {

		private DataUploader owner;
		private Entry entry;

		public Task(DataUploader owner, Entry entry) {
			this.owner = owner;
			this.entry = entry;
		}

		private byte[] encodeEntryWithProto() {
			Proto.UploadData.Builder builder = Proto.UploadData.newBuilder();
			builder.setUserId(Statistic.getDeviceId());
			builder.setModel(android.os.Build.MODEL);
			builder.setSdk(android.os.Build.VERSION.SDK_INT);
			builder.setOsVersion(android.os.Build.VERSION.RELEASE);
			builder.setData(ByteString.copyFrom(entry.data));
			builder.setType(entry.type);
			if (!TextUtils.isEmpty(entry.desc)) {
				builder.setDescribe(entry.desc);
			}
			return builder.build().toByteArray();
		}

		private byte[] compress(byte[] data) {
			try {
				ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
				GZIPOutputStream gzip = new GZIPOutputStream(os);
				try {
					gzip.write(data);
				} finally {
					gzip.close();
				}
				return os.toByteArray();
			} catch (Exception ex) {
				return null;
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (entry.delayed > 0) {
				try {
					Thread.sleep(entry.delayed);
				} catch (InterruptedException e) {}
			}
			byte[] data = compress(encodeEntryWithProto());
			com.subao.common.net.Http.Response response;
			try {
				URL url = SubaoHttp.createURL(SubaoHttp.InterfaceType.HAS_TIMESTAMP_KEY, GlobalDefines.APP_NAME_FOR_HTTP_REQUEST, "1", UrlConfig.instance.getDomainOfLogSubmit() + "/uploadLogs", null);
				response = SubaoHttp.createHttp().doPost(url, data, Http.ContentType.ANY.str);
				return response.code == 200;
			} catch (IOException e) {
				return false;
			}
		}

		protected void onPostExecute(Boolean result) {
			OnUploadCompletedCallback callback = entry.callback;
			boolean autoRetryWhenFail = true;
			if (callback != null) {
				autoRetryWhenFail = callback.onUploadCompleted(result, this.entry.data);
			}
			if (!result && autoRetryWhenFail) {
				owner.queue.offer(this.entry);
			}
			// 由于Task可能被置入线程池，所以这里及时解除引用非常必要！！
			entry = null;
			DataUploader owner = this.owner;
			this.owner = null;
			owner.processNextEntry();
		}
	}

}
