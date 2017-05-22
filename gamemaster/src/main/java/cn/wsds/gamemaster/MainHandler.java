package cn.wsds.gamemaster;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.subao.common.LogTag;
import com.subao.common.data.Defines;
import com.subao.common.data.Defines.ModuleType;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.ThreadUtils;
import com.subao.net.NetManager;

import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.debugger.FakeConnectionRepairPrompt;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpener.OpenSource;
import cn.wsds.gamemaster.ui.floatwindow.ScreenShotMask;

/**
 * 在UI线程的公用Handler，用于处理各种需要在UI线程里做的杂事
 */
public class MainHandler extends Handler {

	private static final String TAG = LogTag.GAME;

	/** 延时关闭加速、并延时再开 */
	private static final int MSG_CLOSE_ACCEL_FOR_TESTER = 1;

	/** 测试人员延时关闭加速后、再延时开启 */
	private static final int MSG_OPEN_ACCEL_AFTER_TESTER_CLOSE = 2;

	/** 执行VersionChecker */
	private static final int MSG_ACCEL_DATA_DOWNLOAD = 3;

	/** 显示主线程检查异常 */
	private static final int MSG_SHOW_DEBUG_MESSAGE = 4;

	/** 显示一条Toast */
	private static final int MSG_TOAST = 5;

	/** 检查本地和服务器上的用户数据是否一致 */
	public static final int MSG_CHECK_USER_DATA = 6;

	public static final int MSG_SAVE_USER_DATA = 7;

	public static final int MSG_UPLOAD_USER_DATA = 8;

	/** 显示断线重连特效 */
	public static final int MSG_SHOW_EFFECT_CONNECTION_REPAIR = 11;

	/** 显示加速成功特效 */
	public static final int MSG_SHOW_EFFECT_ACCEL_SUCCEED = 12;

	/** 显示流量异常特效 */
	public static final int MSG_SHOW_EFFECT_FLOW_EXCEPTION = 13;

	/** SD卡挂载成功 */
	private static final int MSG_MEDIA_MOUNTED = 14;

	/** 执行“2G是否切换到3G或4G”的检查 */
	public static final int MSG_CHECK_2G_CHANGE = 15;

	private static final MainHandler instance = new MainHandler();

	private Context context;

	public static MainHandler getInstance() {
		return instance;
	}

	private MainHandler() {
		if (!ThreadUtils.isInAndroidUIThread()) {
			Log.e(TAG, "MainHandler must be called in main thread");
		}
	}

	public void init(Context context) {
		if (this.context == null) {
			this.context = context.getApplicationContext();
			tryUpdateAccelData(0);
		}
	}

	Context getContext() {
		return this.context;
	}

	/**
	 * 尝试执行一次加速数据（节点和游戏）的更新？
	 */
	public void tryUpdateAccelData(long delayed) {
		if (Defines.moduleType == ModuleType.UI) {
			this.removeMessages(MSG_ACCEL_DATA_DOWNLOAD);
			this.sendEmptyMessageDelayed(MSG_ACCEL_DATA_DOWNLOAD, delayed);
		}
	}

	/**
	 * 给测试人员用的：延时关闭加速、并延时再开
	 * 
	 * @param delayCloseMillis
	 *            延时多少毫秒关闭加速？
	 * @param delayReopenMillis
	 *            如果大于0，表示关闭加速后再延时多少毫秒开启
	 */
	public void closeAccelDelayed(long delayCloseMillis, long delayReopenMillis) {
		removeMessages(MSG_CLOSE_ACCEL_FOR_TESTER);
		removeMessages(MSG_OPEN_ACCEL_AFTER_TESTER_CLOSE);
		sendEmptyMessageDelayed(MSG_CLOSE_ACCEL_FOR_TESTER, delayCloseMillis);
		if (delayReopenMillis >= 0) {
			sendEmptyMessageDelayed(MSG_OPEN_ACCEL_AFTER_TESTER_CLOSE, delayCloseMillis + delayReopenMillis);
		}
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_CLOSE_ACCEL_FOR_TESTER:
			AccelOpenManager.close(CloseReason.DEBUG);
			break;
		case MSG_OPEN_ACCEL_AFTER_TESTER_CLOSE:
			if (!AccelOpenManager.isStarted()) {
				AccelOpenManager.createOpener(null, OpenSource.Main);
			}
			break;
		case MSG_ACCEL_DATA_DOWNLOAD:
			tryUpdateAccelData(3600 * 1000 * 8 + 977);
			break;
		case MSG_SHOW_DEBUG_MESSAGE:
			AppNotificationManager.sendDebugNotice(msg.obj.toString());
			break;
		case MSG_TOAST:
			Toast.makeText(this.context, msg.obj.toString(), msg.arg1).show();
			break;
		case MSG_SHOW_EFFECT_CONNECTION_REPAIR:
			FakeConnectionRepairPrompt.execute(context);
			break;
		case MSG_SHOW_EFFECT_ACCEL_SUCCEED:
			break;
		case MSG_SHOW_EFFECT_FLOW_EXCEPTION:
			break;
		case MSG_MEDIA_MOUNTED:
			GameManager.getInstance().onMediaMounted();
			TriggerManager.getInstance().raiseMediaMounted();
			break;
		case MSG_CHECK_2G_CHANGE:
			check2GChange();
			break;
		}
	}

	/** 2G无缝切换到3G或4G的时候，系统不会触发NetChange事件，所以这里主动触发一下 */
	private void check2GChange() {
		NetManager nm = NetManager.getInstance();
		if (nm.isDisconnected()) {
			return;
		}
		if (!nm.isMobileConnected()) {
			return;
		}
		NetTypeDetector.NetType type = nm.getCurrentNetworkType();
		if (type == NetTypeDetector.NetType.MOBILE_2G) {
			sendEmptyMessageDelayed(MSG_CHECK_2G_CHANGE, 2999);
		} else {
			TriggerManager.getInstance().raiseNetChange(type);
		}
	}

	/** 显示一条调试信息 */
	public void showDebugMessage(String msg) {
		sendMessage(obtainMessage(MSG_SHOW_DEBUG_MESSAGE, msg));
	}

	public void showToast(CharSequence msg, int duration) {
		sendMessage(obtainMessage(MSG_TOAST, duration, 0, msg));
	}

	/** 延时显示截屏遮罩 */
	public void showScreenShotMask(long delayed) {
		this.postDelayed(new Runnable() {
			@Override
			public void run() {
				ScreenShotMask.createInstance(context);
			}
		}, delayed);

	}

	public void sendMediaMountedDelayed(long delayMillis) {
		this.removeMessages(MSG_MEDIA_MOUNTED);
		this.sendEmptyMessageDelayed(MSG_MEDIA_MOUNTED, delayMillis);
	}

	/**
	 * 代理层通知：网络权限可能被禁用了
	 */
	public void onProxyRightsDisabled() {
		this.post(new ProxyRightsDisabledProcessor(this, this.context));
	}

	private static class ProxyRightsDisabledProcessor implements NetDelayDetector.Observer, Runnable {

		private static ProxyRightsDisabledProcessor instance;

		private final Handler ownerHandler;
		private final Context context;

		private boolean firstTime = true;
		private int tcpCount, udpCount;
		private int result;

		public ProxyRightsDisabledProcessor(Handler ownerHandler, Context context) {
			this.ownerHandler = ownerHandler;
			this.context = context;
		}

		private void start() {
			NetDelayDetector.addObserver(this, NetDelayDetector.Type.UDP);
			NetDelayDetector.addObserver(this, NetDelayDetector.Type.TCP);
		}

		private void stop() {
			ownerHandler.removeCallbacks(this);
			if (instance == this) {
				instance = null;
			}
			// 
			NetDelayDetector.removeObserver(this, NetDelayDetector.Type.UDP);
			NetDelayDetector.removeObserver(this, NetDelayDetector.Type.TCP);
			//
			Statistic.addEvent(context, Statistic.Event.BACKSTAGE_NET_RESULT_WHNE_PROXY_RIGHT_DISABLE, Integer.toString(this.result));
			//
			if (this.result != 3) {
				UIUtils.showToast(context.getString(R.string.app_name) + "网络权限被禁用，加速终止。", Toast.LENGTH_LONG);
				AccelOpenManager.close(CloseReason.BY_PROXY);
			}
		}

		@Override
		public void onNetDelayChange(int value, NetDelayDetector.Type type) {
			boolean ok = (value >= 0 && value < GlobalDefines.NET_DELAY_TIMEOUT);
			Log.d(TAG, String.format("Check net delay: %s, %d", type == NetDelayDetector.Type.TCP ? "tcp" : "udp", value));
			switch (type) {
			case TCP:
				++tcpCount;
				if (ok && tcpCount == 2) {
					result |= 1;
				}
				break;
			case UDP:
				++udpCount;
				if (ok && udpCount == 2) {
					result |= 2;
				}
				break;
			}
			if (tcpCount >= 2 && udpCount >= 2) {
				stop();
			}
		}

		@Override
		public void run() {
			if (firstTime) {
				firstTime = false;
				if (null == instance) {
					instance = this;
					this.start();
					ownerHandler.postDelayed(this, 10 * 1000);
				}
			} else {
				// 第2次运行了，说明是10秒以后的再次运行
				stop();
			}
		}
	}

}
