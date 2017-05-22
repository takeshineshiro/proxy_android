package cn.wsds.gamemaster.netcheck;

import java.io.IOException;

import com.subao.airplane.AirplaneMode;
import com.subao.airplane.SwitchState;
import com.subao.common.RunnableHasResult;
import com.subao.common.net.Http.HttpResponseCodeGetter;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.ThreadUtils;
import com.subao.net.NetManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.tools.VPNUtils;

class NetworkStateChecker implements NetworkCheckManager.Checker {

	private static final String URL_FOR_DETECT = "http://www.baidu.com";

	private static final String TAG = "NetworkStateChecker" ;
	
	private final Context context;

	public NetworkStateChecker(Context context) {
		this.context = context.getApplicationContext();
	}

	@Override
	public Result run() {
		NetTypeDetector.NetType nt = NetManager.getInstance().getCurrentNetworkType();
		switch (nt) {
		case WIFI:
			return checkWiFi();
		case DISCONNECT:
			return checkDisconnected();
		default:
			return checkMobile(nt);
		}
	}

	/** 检查WiFi */
	private Result checkWiFi() {
		// 判断有无IP
		RunnableHasResult<Result> r = new RunnableHasResult<Result>() {
			@Override
			public void run() {
				setResult(hasIpOfWiFi() ? null : Result.WIFI_FAIL_RETRIEVE_ADDRESS);
			}

			private boolean hasIpOfWiFi() {
				WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				if (wm == null) {
					return false;
				}
				WifiInfo info;
				try {
					info = wm.getConnectionInfo();
				} catch (Exception e) {
					// 已知一个奇葩手机，getConnectionInfo有可能抛NullPointer异常
					return false;
				}
				if (info == null) {
					return false;
				}
				return 0 != info.getIpAddress();
			}
		};
		if (ThreadUtils.isInAndroidUIThread()) {
			r.run();
		} else {
			MainHandler.getInstance().post(r);
		}
		Result result = r.waitResult();
		if (result != null) {
			return result;
		}
		//
		// 检查是否网络权限被禁止了
		if (isNetForbidden()) {
			return Result.NETWORK_AUTHORIZATION_FORBIDDED;
		}
		//
		// 检查是否需要web认证
		int status_code;
		try {
			status_code = HttpResponseCodeGetter.execute("www.baidu.com", 80, 3000, 3000);
		} catch (IOException e) {
			return Result.WIFI_UNAVAILABLE;
		} catch (RuntimeException e) {
			return Result.WIFI_UNAVAILABLE;
		}
		if (status_code >= 300 && status_code < 400) {
			return Result.WIFI_SHOULD_AUTHORIZE;
		}
		return null;
	}

	/** 检查无线连接 */
	private static Result checkMobile(NetTypeDetector.NetType mobileType) {
		if (isNetForbidden()) {
			return Result.NETWORK_AUTHORIZATION_FORBIDDED;
		}
		RunnableHasResult<Boolean> r = new RunnableHasResult<Boolean>() {

			@Override
			public void run() {
				setResult(NetManager.getInstance().isWapCurrent());
			}

		};
		if (ThreadUtils.isInAndroidUIThread()) {
			r.run();
		} else {
			MainHandler.getInstance().post(r);
		}
		if (r.waitResult()) {
			return Result.WAP_POINT;
		}
		//
		try {
			com.subao.utils.SubaoHttp.createHttp().getHttpResponseCode(URL_FOR_DETECT);
		} catch (IOException e) {
			return Result.MOBILE_UNAVAILABLE;
		} catch (RuntimeException e) {
			return Result.MOBILE_UNAVAILABLE;
		}
		return null;
	}

	/** 检查网络类型未知的情况 （断网） */
	private Result checkDisconnected() {
		RunnableHasResult<Result> r = new RunnableHasResult<Result>() {

			@Override
			public void run() {
				Result event;
				while (true) {
					if (AirplaneMode.getState(context) == SwitchState.On) {
						event = Result.AIRPLANE_MODE;
						break;
					}
					// 检查DHCP
					event = checkDHCP(context);
					if (event != null) {
						break;
					}
					// 检查是否WIFI和移动网络都关闭着
					if (!NetManager.getInstance().getWiFiDataSwitch()
						&& NetManager.getInstance().getMobileDataSwitch() == SwitchState.Off) {
						event = Result.WIFI_MOBILE_CLOSED;
					} else {
						event = Result.NETWORK_DISCONNECT;
					}
					break;
				}
				setResult(event);
			}
		};
		if (ThreadUtils.isInAndroidUIThread()) {
			r.run();
		} else {
			MainHandler.getInstance().post(r);
		}
		return r.waitResult();
	}

	/**
	 * 检查是否分配到IP地址 必须在主线程里调用
	 */
	private static Result checkDHCP(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
			.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (info != null && info.getDetailedState() == DetailedState.OBTAINING_IPADDR) {
			return Result.IP_ADDR_ASSIGN_PENDING;
		} else {
			return null;
		}
	}

	/**
	 * 检查是否网络权限被禁止了
	 * <p>
	 * <b>本函数线程安全</b>
	 * </p>
	 */
	private static boolean isNetForbidden() {
		return false;
		// FIXME: 17-3-29 hujd
//		return VPNJni.SOCKET_STATE_FIREWALL == VPNUtils.checkSocketState(TAG);
	}


}
