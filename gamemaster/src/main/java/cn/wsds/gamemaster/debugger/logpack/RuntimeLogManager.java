package cn.wsds.gamemaster.debugger.logpack;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.subao.common.SuBaoObservable;
import com.subao.net.NetManager;
import com.subao.utils.Misc;

import java.io.IOException;
import java.util.List;

import cn.wsds.gamemaster.debugger.logpack.LogFileUtil.WriteRuntimeLog;
import cn.wsds.gamemaster.debugger.logpack.RuntimeLogCatcher.OnLogUpdateListener;
import cn.wsds.gamemaster.tools.DataUploader.OnUploadCompletedCallback;

/**
 * 管理运行时日志
 */
public class RuntimeLogManager implements OnLogUpdateListener{
 
	private static final String TAG = "RuntimeLogManager" ;
	private RuntimeLogCatcher runtimeLogCatcher;
	private WriteRuntimeLog writer;
	public static final RuntimeLogManager instance = new RuntimeLogManager();
	/** 倒计时时长  5 分钟 */
	private final int MAX_RUNTIME_DEFAULT =  5 * 60 * 1000;
	private RuntimeLogManager() {}
	/** 开启抓获日志时刻 */
	private long runtime;
	private int maxRuntime;
	private String desc;
	private ProgressHandler handler;
	private int currentProgress;
	
	private final OnUploadCompletedCallback onUploadCompletedCallback = new OnUploadCompletedCallback() {
		
		@Override
		public boolean onUploadCompleted(boolean succeeded, byte[] data) {
			if(succeeded){
				LogFileUtil.clearFile();
			}
			observers.progressChanged(100);
			observers.uploadCompleted(succeeded);
			return false;
		}
	};
	

	/**
	 * 开启抓获日志
	 * @param desc 
	 * @param maxRuntime 最长运行时间(单位毫秒)
	 */
	public void startCatch(int maxRuntime,String desc) throws IOException{
		if(isCatching()){
			return;
		}
		writer = new WriteRuntimeLog();
		this.maxRuntime = maxRuntime;
		this.desc = desc;
		runtime = getTime();
		handler = new ProgressHandler(maxRuntime);
		handler.start();
		// FIXME: 17-3-29 hujd
//		VPNUtils.sendSetLogLevel(VPNManager.LOG_LEVEL_DEBUG ,TAG);
		runtimeLogCatcher = new RuntimeLogCatcher(android.os.Process.myPid());
		runtimeLogCatcher.start(this);
	}
	
	public void startCatch() throws IOException{
		startCatch(MAX_RUNTIME_DEFAULT, desc);
	}
	
	/**
	 * 开启抓获日志
	 */
	public void startCatch(String desc) throws IOException{
		startCatch(MAX_RUNTIME_DEFAULT, desc);
	}

	private long getTime() {
		return SystemClock.elapsedRealtime();
	}
	
	/**
	 * 关闭抓获日志	
	 */
	public void closeCatch(){
		handler.stop();
		handler = null;
		// FIXME: 17-3-29 hujd
//		VPNUtils.sendSetLogLevel(VPNManager.LOG_LEVEL_WARNING,TAG);
		if(runtimeLogCatcher!=null){
			runtimeLogCatcher.stop();
			runtimeLogCatcher = null;
		}
		Misc.safeClose(writer);
		writer = null;
	}

	@Override
	public void onLogUpdate(String log) {
		if (writer != null) {
			writer.onLogUpdate(log);
		}
	}
	
	/**
	 * 是否正在抓获日志
	 * @return
	 */
	public static boolean isCatching(){
		if(instance.runtimeLogCatcher == null){
			return false;
		}else{
			return instance.runtimeLogCatcher.isCatching();
		}
	}
	
	public void registerObservers(Observer observer){
		observers.registerObserver(observer);
	}
	
	public void unregisterObserver(Observer observer) {
		observers.unregisterObserver(observer);
	}
	
	
	public interface Observer{
		/**
		 * 上传结束
		 * @param result 上传结果
		 */
		public void uploadCompleted(boolean result);
		
		/** 进度更新 */
		public void progressChanged(int progress);
	}
	
	private final Observers observers = new Observers();
	private static final class Observers extends SuBaoObservable<Observer> {

		public void uploadCompleted(boolean result) {
			List<Observer> list = this.cloneAllObservers();
			if (list != null) {
				for (Observer o : list) {
					o.uploadCompleted(result);
				}
			}
		}

		public void progressChanged(int progress) {
			List<Observer> list = this.cloneAllObservers();
			if (list != null) {
				for (Observer o : list) {
					o.progressChanged(progress);
				}
			}
		}
	};

	@SuppressLint("HandlerLeak")
	private final class ProgressHandler extends Handler {
		
		private final int intervalTime;
		private static final int PERCENT_COUNT = 99;
		private static final int MSG_PROGRESS_CHANGE = 0;
		private ProgressHandler(int totalDuration) {
			super();
			intervalTime = totalDuration / (PERCENT_COUNT);
			currentProgress = 0;
		}
		public void start(){
			sendEmptyMessageDelayed(MSG_PROGRESS_CHANGE, intervalTime);
		}
		public void stop(){
			removeMessages(MSG_PROGRESS_CHANGE);
		}
		public void handleMessage(Message msg) {
			if(MSG_PROGRESS_CHANGE == msg.what){
				onProgressChange();
			}
		}
		private void onProgressChange() {
			currentProgress ++;
			observers.progressChanged(currentProgress);
			if(currentProgress == PERCENT_COUNT || getElapsedTime()>=maxRuntime){
				catchEnd();
			}else{
				start();
			}
		}
	}
	
	private void catchEnd() {
		closeCatch();
		if (NetManager.getInstance().isDisconnected()) {
			onUploadCompletedCallback.onUploadCompleted(false, null);
			return;
		}
		new AsynDumpUpload(desc).upload(onUploadCompletedCallback);
	};
	
	public static long getElapsedTime() {
		return instance.getTime() - instance.runtime;
	}
	
	public static int getCurrentProgress(){
		return instance.currentProgress;
	}
}
