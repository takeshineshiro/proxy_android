package cn.wsds.gamemaster.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.SuBaoObservable;
import com.subao.common.data.Address;
import com.subao.common.data.Address.EndPoint;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.msg.Message_VersionInfo;
import com.subao.common.net.Http;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.NetUtils;
import com.subao.common.utils.InfoUtils;
import com.subao.data.InstalledAppInfo;
import com.subao.net.NetManager;
import com.subao.utils.SubaoHttp;
import com.subao.utils.UrlConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.messageuploader.MessageUploaderManager;
import grpc.client.AppOuterClass.App;
import grpc.client.AppOuterClass.AppType;
import grpc.client.Device.DeviceInfo;
import grpc.client.Fault;
import grpc.client.Id.SubaoId;
import grpc.client.NetworkOuterClass.Network;
import grpc.client.NetworkOuterClass.NetworkQuality;
import grpc.client.NetworkOuterClass.NetworkType;
import grpc.client.Version.VersionInfo;

/**
 * 智能排障，搜集信息并上报 Created by qinyanjun on 16-05-13.
 */

public class FaultProcessor {

	private static final FaultProcessor instance = new FaultProcessor();
	private static final int PERCENT_COUNT = 99;

	//进度条时间控制，5秒
	private static final int TOTAL_DURATION = 5 * 1000;

	private Worker worker;

	private int currentProgress = 0;
	private ProgressHandler progressHandler;

	private static final class ExcuteParams {
		public final String description;
		public final Context context;
		public final GameInfo gameInfo;

		public ExcuteParams(String descrpition, Context context, GameInfo gameInfo) {
			this.description = descrpition;
			this.context = context.getApplicationContext();
			this.gameInfo = gameInfo;
		}
	}

	public static FaultProcessor getInstance() {
		return instance;
	}

	@SuppressLint("HandlerLeak")
	private final class ProgressHandler extends Handler {

		private final int intervalTime;

		private static final int MSG_PROGRESS_CHANGE = 0;

		private ProgressHandler() {
			super();
			intervalTime = TOTAL_DURATION / (PERCENT_COUNT);
			currentProgress = 0;
		}

		private void start() {
			sendEmptyMessageDelayed(MSG_PROGRESS_CHANGE, intervalTime);
		}

		private void stop() {
			removeMessages(MSG_PROGRESS_CHANGE);
		}

		@Override
		public void handleMessage(Message msg) {
			if (MSG_PROGRESS_CHANGE == msg.what) {
				onProgressChange();
			}
		}

		private void onProgressChange() {
			currentProgress++;
			observers.progressChanged(currentProgress);

			if (currentProgress == PERCENT_COUNT) {
				if(NetManager.getInstance().isDisconnected()){
					observers.uploadCompleted(false);
					return ;
				}
				currentProgress = 100;
				if (worker.isCompleted()) {
					boolean result = worker.getResult();
					if (result) {
						observers.progressChanged(100);
					}
					observers.uploadCompleted(result);
				}
			} else {
				sendEmptyMessageDelayed(MSG_PROGRESS_CHANGE, intervalTime);
			}
		}
	}

	public void start(String description, Context context, GameInfo gameInfo) {
		if ((context == null)||(NetManager.getInstance().isDisconnected())) {
			observers.uploadCompleted(false);
			return;
		}
		
		if (worker == null) {
			currentProgress = 0;
			EndPoint server;
			if (UrlConfig.instance.getServerType() == UrlConfig.ServerType.TEST) {
				server = new EndPoint(Address.HostName.TEST_SERVER, 501);
			} else {
				server = new EndPoint("node-ddns.wsds.cn", 501);

			}
			worker = new Worker(server);
			progressHandler = new ProgressHandler();
			ExcuteParams params = new ExcuteParams(description, context, gameInfo);
			worker.execute(params);
			progressHandler.start();
		}
	}

	public void stop() {
		currentProgress = 0 ;
		
		if (worker != null) {
			worker.cancel(true);
			worker = null;

			if (progressHandler != null) {
				progressHandler.stop();
				progressHandler = null;
			}
		}
	}

	public boolean isRunning() {
		return worker != null;
	}

	public int getProgress() {
		return currentProgress;
	}

	public boolean getResult() {
		if (worker != null) {
			return worker.getResult();
		} else {
			return false;
		}
	}

	public interface Observer {
		/**
		 * 上传结束
		 * 
		 * @param result
		 *            上传结果
		 */
		void uploadCompleted(boolean result);

		/** 进度更新 */
		void progressChanged(int progress);
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

	public void registerObservers(Observer observer) {
		observers.registerObserver(observer);
	}

	public void unregisterObserver(Observer observer) {
		observers.unregisterObserver(observer);
	}

	private class Worker extends AsyncTask<ExcuteParams, Integer, Boolean> {
		private static final String TAG = "FaultProcessor";

		private final EndPoint server;
		private boolean isCompleted = false;
		private boolean result = false;

		/**
		 * 判断是否完成
		 */
		private boolean isCompleted() {
			return isCompleted;
		}

		/**
		 * 判断是否上报成功
		 */
		private boolean getResult() {
			return result;
		}

		public Worker(EndPoint server) {
			this.server = server;
		}

		@Override
		protected Boolean doInBackground(ExcuteParams... params) {
			Fault.FaultMsg.Builder builder = Fault.FaultMsg.newBuilder();
			if (!collectInfos(params[0], builder)) {
				return false;
			}
			try {
				URL url = new URL("http", server.host, server.port, "/v1/report/client/fault");
				return doPost(url, builder.build().toByteArray());
			} catch (IOException e) {
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			this.result = result;
			this.isCompleted = true;
			if (currentProgress >= PERCENT_COUNT) {
				currentProgress = 100;
				if (result) {
					observers.progressChanged(100);
				}
				observers.uploadCompleted(result);
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			observers.progressChanged(values[0]);
		}

		private boolean collectInfos(ExcuteParams params, Fault.FaultMsg.Builder builder) {

			boolean result = true;
			try {
				String id = SubaoIdManager.getInstance().getSubaoId();

				String desc = params.description;
				if (desc == null) {
					desc = "";
				}

				String logs = VPNUtils.getAppLogCache(TAG);
				int count = logs.length() - 1024 * 1024;
				if (count > 0) {
					logs = logs.substring(count);
				}
				SubaoId.Builder idBuilder = SubaoId.newBuilder();
				idBuilder.setId(id);
				builder.setId(idBuilder.build());
				builder.setTime(System.currentTimeMillis() / 1000);
				builder.setType(AppType.ANDROID_APP);
				builder.setDesc(desc);
				builder.setLog(logs);

				if (params.gameInfo != null) {
					App.Builder gameInfoBuilder = App.newBuilder();
					gameInfoBuilder.setAppLabel(params.gameInfo.getAppLabel());
					gameInfoBuilder.setPkgName(params.gameInfo.getPackageName());
					builder.setGame(gameInfoBuilder.build());
				}

				collectVersionInfo(builder);
				collectDeviceInfo(params.context, builder);
				collectInstalledAppInfo(builder);
				collectNetWorkInfo(params.context, builder);

			} catch (Exception e) {
				result = false;
				Logger.e(TAG, e.toString());
			}

			return result;
		}

		private void collectVersionInfo(Fault.FaultMsg.Builder builder) {
			Message_VersionInfo versionInfo = MessageUploaderManager.getInstance().getVersionInfo();
			VersionInfo.Builder versionBuilder = VersionInfo.newBuilder();
			versionBuilder.setAndroidVersion(versionInfo.androidVersion);
			versionBuilder.setChannel(versionInfo.channel);
			versionBuilder.setNumber(versionInfo.number);
			versionBuilder.setOsVersion(versionInfo.osVersion);
			builder.setVersion(versionBuilder.build());
		}

		private void collectDeviceInfo(Context context, Fault.FaultMsg.Builder builder) {

			DeviceInfo.Builder deviceBuilder = DeviceInfo.newBuilder();

			deviceBuilder.setCpuCore(InfoUtils.CPU.getCores());
			deviceBuilder.setCpuSpeed((int) InfoUtils.CPU.getMaxFreqKHz());
			if (context == null) {
				deviceBuilder.setMemory(-1);
			} else {
				deviceBuilder.setMemory((int) (InfoUtils.getTotalMemory(context) / (1024 * 1024)));
			}

			deviceBuilder.setModel(Build.MODEL);

			builder.setDevice(deviceBuilder.build());
		}

		private void collectInstalledAppInfo(Fault.FaultMsg.Builder builder) {

			InstalledAppInfo[] appInfos = GameManager.getInstance().getInstalledApps();
			if (appInfos.length == 0) {
				return;
			}

			App.Builder appbuilder = App.newBuilder();
			for (int i = 0; i < appInfos.length; i++) {
				appbuilder.clear();

				appbuilder.setAppLabel(appInfos[i].getAppLabel());
				appbuilder.setPkgName(appInfos[i].getPackageName());

				builder.addAppList(appbuilder.build());
			}
		}

		private void collectNetWorkInfo(Context context, Fault.FaultMsg.Builder builder) {
			NetManager netManager = NetManager.getInstance();
			Network.Builder netWorkbuilder = Network.newBuilder();
			if (netManager.isConnected()) {
				String netName = NetUtils.getCurrentNetName(context, netManager);
				NetTypeDetector.NetType nt = netManager.getCurrentNetworkType();
				switch (nt) {
				case WIFI:
					netWorkbuilder.setType(NetworkType.WIFI);
					break;
				case MOBILE_2G:
					netWorkbuilder.setType(NetworkType.MOBILE_2G);
					break;
				case MOBILE_3G:
					netWorkbuilder.setType(NetworkType.MOBILE_3G);
					break;
				case MOBILE_4G:
					netWorkbuilder.setType(NetworkType.MOBILE_4G);
					break;
				default:
					netWorkbuilder.setType(NetworkType.UNKNOWN_NETWORKTYPE);
					break;
				}
				if (netName != null) {
					netWorkbuilder.setDetail(netName);
				}
			}
			NetworkQuality.Builder qBuilder = NetworkQuality.newBuilder();
			netWorkbuilder.setQuality(qBuilder.build());
			builder.setNetwork(netWorkbuilder.build());
		}

		private boolean doPost(URL url, byte[] postData) {
			try {
				Http http = SubaoHttp.createHttp();
				HttpURLConnection conn = http.createHttpUrlConnection(url, Http.Method.POST, Http.ContentType.PROTOBUF.str);
				com.subao.common.net.Http.Response response = Http.doPost(conn, postData);
				return response.code == 201;
			} catch (IOException e) {
				Logger.w(LogTag.MESSAGE, e.toString());
				return false;
			}
		}
	}
}
