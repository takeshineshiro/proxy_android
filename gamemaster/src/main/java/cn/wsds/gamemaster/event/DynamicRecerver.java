package cn.wsds.gamemaster.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.app.GameManager;

import com.subao.airplane.AirplaneMode;
import com.subao.common.net.NetTypeDetector;
import com.subao.data.InstalledAppInfo;
import com.subao.net.NetManager;

public class DynamicRecerver extends BroadcastReceiver {
	private static final DynamicRecerver instance = new DynamicRecerver();

	private boolean init_completed;
	
	private NetTypeDetector.NetType lastNetType;

	private DynamicRecerver() {}

	public static void init(Context context) {
		if (instance.init_completed)
			return;
		instance.init_completed = true;

		// 动态注册BroadcastReceiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		context.registerReceiver(instance, filter);

		filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(instance, filter);
		instance.lastNetType = NetManager.getInstance().getCurrentNetworkType();

		filter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
		context.registerReceiver(instance, filter);

		filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		context.registerReceiver(instance, filter);

		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		context.registerReceiver(instance, filter);

		filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
		context.registerReceiver(instance, filter);

		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addDataScheme("file");
		context.registerReceiver(instance, filter);
		
//		filter = new IntentFilter();
//		filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//		context.registerReceiver(instance, filter);
		
	}

	private void onAppInstalled(String data, Context context) {
		if (TextUtils.isEmpty(data))
			return;
		String[] strs = data.split(":");
		if (strs.length < 2)
			return;

		String packagename = strs[1];
		PackageManager pm = context.getPackageManager();
		try {
			ApplicationInfo app = pm.getApplicationInfo(packagename, 0);
			InstalledAppInfo info = new InstalledAppInfo(app, app.loadLabel(pm).toString(),
				InstalledAppInfo.hasSuBaoSDKPermission(pm, packagename));
			//GameManager的更新要在trigger之前，避免其它地方取列表，错误
			GameManager.getInstance().onAppInstalled(info);
			TriggerManager.getInstance().raiseAppInstalled(info);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void onAppRemove(String data) {
		if (TextUtils.isEmpty(data))
			return;

		String[] strs = data.split(":");
		if (strs.length < 2)
			return;

		String packagename = strs[1];
		GameManager.getInstance().onAppRemoved(packagename);
		TriggerManager.getInstance().raiseAppRemoved(packagename);
	}

	private void onWifiEnableChanged(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager == null)
			return;

		TriggerManager.getInstance().raiseWifiEnableChanged(wifiManager.getWifiState());	
	}

	private void onWifiAPStateChange(int state) {
		// Fix for Android 4
		if (state > 10)
			state -= 10;
		TriggerManager.getInstance().raiseAPStateChange(state);
	}

	private void onNetChange() {
		NetTypeDetector.NetType current = NetManager.getInstance().refreshNetState();
		if (current != lastNetType) {
			lastNetType = current;
			TriggerManager.getInstance().raiseNetChange(current);
			MainHandler.getInstance().removeMessages(MainHandler.MSG_CHECK_2G_CHANGE);
			if (current == NetTypeDetector.NetType.MOBILE_2G) {
				MainHandler.getInstance().sendEmptyMessageDelayed(MainHandler.MSG_CHECK_2G_CHANGE, 2999);
			}
		}
	}

	private void onAirplaneModeChange(Context context) {
		TriggerManager.getInstance().raiseAirplaneModeChanged(AirplaneMode.getState(context));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		// 其它事件
		if (Intent.ACTION_PACKAGE_ADDED.equals(action))
			this.onAppInstalled(intent.getDataString(), context);
		else if (Intent.ACTION_PACKAGE_REMOVED.equals(action))
			this.onAppRemove(intent.getDataString());
		else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
//			Log.e("TTT", "Connectivity Action: " + !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
			this.onNetChange();
		} else if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
			int state = intent.getIntExtra("wifi_state", -1);
			this.onWifiAPStateChange(state);
		} else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
			this.onAirplaneModeChange(context);
		} else if (Intent.ACTION_SCREEN_ON.equals(action)) {
			TriggerManager.getInstance().raiseScreenOn();
		} else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
			TriggerManager.getInstance().raiseScreenOff();
		} else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
			postNetChanged();
			this.onWifiEnableChanged(context);
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
			MainHandler.getInstance().sendMediaMountedDelayed(5000);
//		} else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)){
//			long myDwonloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
//			onDownloadComplete(context, myDwonloadID);  
		}
	}
	
	//此个方法是为了解决一款机型中出现的bug：（HUAWEI-H60 , Android6.0 ）
	//游戏在前台时，断网后重新联网成功，但悬浮窗仍显示红色叹号。
	//原因是这种情况下，只收到了wifi状态改变的广播，因此需要加此处理，
	//由于网络状态的调整需要一些时间，因此采用postDelayed方式
	private void postNetChanged(){
		MainHandler.getInstance().postDelayed(new Runnable() {
			@Override
			public void run() {
				DynamicRecerver.this.onNetChange();
			}
		}, 5000);
	}
	
	

//	private void onDownloadComplete(Context context,long myDwonloadID) {
//		//TODO 安装下载的Apk游戏
//		DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);  
//		Uri downloadFileUri = downloadManager.getUriForDownloadedFile(myDwonloadID);
//		Intent intent = new Intent(Intent.ACTION_VIEW);
//		intent.setDataAndType(downloadFileUri,"application/vnd.android.package-archive"); 
//		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		try {
//			context.startActivity(intent);
//		} catch (ActivityNotFoundException e) {
//			e.printStackTrace();
//		}
//	}

}
