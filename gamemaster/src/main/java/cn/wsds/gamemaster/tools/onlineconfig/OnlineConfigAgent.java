package cn.wsds.gamemaster.tools.onlineconfig;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.data.ServiceConfig;
import com.subao.common.data.ServiceLocation;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.InfoUtils;
import com.subao.utils.FileUtils;

import java.io.File;
import java.io.IOException;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.net.NetworkStateChecker;
import cn.wsds.gamemaster.net.http.DefaultNoUIResponseHandler;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.user.UserTaskManager;
import hr.client.appuser.GlobalConfig;

/**
 * Created by lidahe on 15/12/21.
 */

public class OnlineConfigAgent {
	
	private static final String TAG = LogTag.DATA;

	public static final String DEFAULT_OPTION_PARAS = "defaultPoints";

	private static final String ONLINE_GLOBAL_DATA_FILE = "online.data.global";

	private static final long DELAY_NEXT_REQUEST_DOWNLOAD = 29 * 60 * 1000;

	private static final OnlineConfigAgent instance = new OnlineConfigAgent();

	private GlobalConfig.GetGlobalConfigResponse globalConfigResponse;

	/** globalConfigResponse成员是不是已成功从网上更新过了？ */
	private boolean isGlobalConfigOk;

	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			Logger.d(TAG, "OnlineConfig: Loop, try download");
			if (!isGlobalConfigOk) {
				asyncDownloadIfNeed();
				sendEmptyMessageDelayed(1, DELAY_NEXT_REQUEST_DOWNLOAD);
			} else {
				if (eventObserver != null) {
					TriggerManager.getInstance().deleteObserver(eventObserver);
					eventObserver = null;
				}
				Logger.d(TAG, "OnlineConfig: Stop loop");
			}
		};
	};
	
	private EventObserver eventObserver = new EventObserver() {
		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			if (NetworkStateChecker.defaultInstance.isNetworkAvail()) {
				Logger.d(TAG, "OnlineConfig: Net change, try download");
				handler.removeCallbacksAndMessages(null);
				handler.sendEmptyMessage(1);
			}
		}
	};
	
	public static synchronized OnlineConfigAgent getInstance() {
		return instance;
	}

	private OnlineConfigAgent() {
		globalConfigResponse = loadGlobalConfigResponseFromFile();
		if (globalConfigResponse == null) {
			globalConfigResponse = buildDefaultGlobalConfigResponse();
		} else {
			Logger.d(TAG, "OnlineConfig: Load Global Config from file ok");
		}
		// 在线更新
		handler.sendEmptyMessage(1);
		TriggerManager.getInstance().addObserver(eventObserver);
	}

	public GlobalConfig.GetGlobalConfigResponse getGlobalConfig() {
		return globalConfigResponse;
	}

	public String getBaseApiUrl() {
		if(globalConfigResponse!=null){
			return globalConfigResponse.getBaseUrl();
		}
		return null;
	}
	
	private static GlobalConfig.GetGlobalConfigResponse buildDefaultGlobalConfigResponse() {
		GlobalConfig.GetGlobalConfigResponse.Builder builder = GlobalConfig.GetGlobalConfigResponse.newBuilder();
		builder.setBaseUrl("http://api.wsds.cn:2100");
		builder.setCouponUrl("http://game.wsds.cn/shop/v0.2.3/index.html");
		builder.setInterfaceVersion("v1");
		builder.setTaskUrl("/api/app/tasks/v1.1");
		return builder.build();
	}


	private static GlobalConfig.GetGlobalConfigResponse loadGlobalConfigResponseFromFile() {
		try {
			File globalFile = FileUtils.getDataFile(ONLINE_GLOBAL_DATA_FILE);
			byte[] data = FileUtils.read(globalFile);
			if (data == null) {
				Logger.w(TAG, "OnlineConfig: load ONLINE_GLOBAL_DATA_FILE data file error, maybe first load?");
				return null;
			}
			return GlobalConfig.GetGlobalConfigResponse.parseFrom(data);
		} catch (IOException e) {
			return null;
		}
	}


	/**
	 * 如果本次启动App还没有更新过，就尝试请求一下
	 */
	private void asyncDownloadIfNeed() {
		if (isGlobalConfigOk) {
			return;
		}
		Logger.d(TAG, "OnlineConfig: Request global config");
		HttpApiService.requestGlobalConfig(
			getDownloadGlobalConfigUrl(),
			new DefaultNoUIResponseHandler() {
				@Override
				protected void onSuccess(Response response) {
					try {
						if (response.code != 200) {
							Logger.w(TAG, "OnlineConfig: requestGlobalConfig response code expect 200, but is " + response);
							return;
						}
						if (response.body == null) {
							Logger.w(TAG, "OnlineConfig: requestGlobalConfig response code is 200, but body is null");
							return;
						}
						globalConfigResponse = GlobalConfig.GetGlobalConfigResponse.parseFrom(response.body);
						new SaveExecutor().executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
						if(UserSession.isLogined()){
							UserTaskManager.getInstance().asyncDownloadTaskList();
						}
						isGlobalConfigOk = true;
					} catch (IOException e) {
						Logger.w(TAG, e.getMessage());
					}
				}
			}
		);
	}

	/**
	 * 负责将在线配置数据缓存到本地的线程
	 */
	private class SaveExecutor extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			FileUtils.write(FileUtils.getDataFile(ONLINE_GLOBAL_DATA_FILE), globalConfigResponse.toByteArray());
			return null;
		}

	}

	/**
	 * 在线配置数据从哪里取？
	 * <p>
	 * 根据Debug页面里设置用正式服还是测试服决定
	 * </p>
	 */
	private static String getDownloadGlobalConfigUrl() {
		StringBuilder sb = new StringBuilder(512);
		ServiceConfig serviceConfig = new ServiceConfig();
		serviceConfig.loadFromFile(null, false);
		ServiceLocation hrServiceLocation = serviceConfig.getHrServiceLocation();
		String host;
		if (hrServiceLocation == null || TextUtils.isEmpty(hrServiceLocation.host))   {
			host = "api.xunyou.mobi";
		} else{
			host = hrServiceLocation.host;
		}
		sb.append(host);                                            //"http://test.api.wsds.cn"
		sb.append("/api/app/config?appVersion=");     //:2100

		String version = InfoUtils.getVersionName(AppMain.getContext());
		if (version != null) {
			sb.append(version);
		}
		return sb.toString();
	}



}
