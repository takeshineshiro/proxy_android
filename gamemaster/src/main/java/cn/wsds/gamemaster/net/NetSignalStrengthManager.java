package cn.wsds.gamemaster.net;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;
import com.subao.resutils.WeakReferenceHandler;

/**
 * 网络信号管理
 */
public class NetSignalStrengthManager {

	/**
	 * 手机网络信号等级 默认值为 <b> -1 </b> 正常取值范围为（0，{@link #MAX_SIGNAL_NUMLEVELS}）
	 */
	private int mobileSignalLevel = -1;

	private final WifiManager wifiManager;
	private final TelephonyManager telephonyManager;
	private final WifiSignalUpdateHandler wifiSignalUpdateHandler;

	private MyPhoneStateListener myPhoneStateListener;
	private MyEventObserver eventObserver;

	/** 最近一次的信号强度级别，WiFi和Mobile公用 */
	private int currentSignalLevel = -1;

	private OnSignalLevelChangedListener signalLevelChangedListener;

	/**
	 * 信号强度级别改变监听
	 */
	public interface OnSignalLevelChangedListener {
		/**
		 * 当信号强度级别改变的时候
		 * 
		 * @param signaLevel
		 *            当前最新的信号级别
		 */
		public void onSignalLevelChanged(int signaLevel);
	}

	/**
	 * 信号强度等级最大等级
	 */
	public final static int MAX_SIGNAL_NUMLEVELS = 4;

	/**
	 * 当不在使用manager对象时务必调用cleanUp 去除手机信号监听
	 */
	public NetSignalStrengthManager() {
		Context context = AppMain.getContext();
		this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		this.wifiSignalUpdateHandler = new WifiSignalUpdateHandler(this);
	}

	/**
	 * 开始运行（不断地监测信号强度）
	 */
	public void start() {
		if (eventObserver == null) {
			eventObserver = new MyEventObserver();
			TriggerManager.getInstance().addObserver(eventObserver);
		}
		if (NetManager.getInstance().isWiFiConnected()) {
			wifiSignalUpdateHandler.start();
		} else {
			registerPhoneStateListener();
		}
	}

	/**
	 * 结束（停止监测信号的操作）
	 * <p>
	 * <b>重要：当不需要NetSignalStrengthManager的时候，一定要调用这个方法！！</b>
	 * </p>
	 */
	public void cleanUp() {
		if (eventObserver != null) {
			TriggerManager.getInstance().deleteObserver(eventObserver);
			eventObserver = null;
		}
		wifiSignalUpdateHandler.stop();
		unregisterPhoneStateListener();
	}

	private void unregisterPhoneStateListener() {
		if (myPhoneStateListener != null) {
			telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_NONE);
			myPhoneStateListener = null;
		}
	}

	private void registerPhoneStateListener() {
		if (myPhoneStateListener == null) {
			myPhoneStateListener = new MyPhoneStateListener(this);
			telephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		}
	}

	/**
	 * 设置信号强度改变监听
	 * 
	 * @param listener
	 */
	public void setSignalLevelChangedListener(OnSignalLevelChangedListener listener) {
		this.signalLevelChangedListener = listener;
	}

	/** wifi 信号强度刷新 handler */
	private static final class WifiSignalUpdateHandler extends WeakReferenceHandler<NetSignalStrengthManager> {

		private static final long DELAY_MILLIS = 2000;

		public WifiSignalUpdateHandler(NetSignalStrengthManager ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(NetSignalStrengthManager ref, Message msg) {
			int wifiSignalLevel = ref.getWifiSignalLevel();
			ref.updateSignalLevel(wifiSignalLevel, true);
			sendEmptyMessageDelayed(0, DELAY_MILLIS);
		}

		public void start() {
			this.stop();
			this.sendEmptyMessage(0);
		}

		public void stop() {
			this.removeCallbacksAndMessages(null);
		}
	}

	/**
	 * 获得手机网络 信号等级
	 * 
	 * @return {@link #mobileSignalLevel}
	 */
	public int getMobileSignalLevel() {
		return mobileSignalLevel;
	}

	/** 更新当前信息强度级别 */
	private void updateSignalLevel(int newValue, boolean wifi) {
		if (currentSignalLevel == newValue) {
			return;
		}
		if (wifi != NetManager.getInstance().isWiFiConnected()) {
			return;
		}
		currentSignalLevel = newValue;
		if (signalLevelChangedListener != null) {
			signalLevelChangedListener.onSignalLevelChanged(newValue);
		}
	}

	/**
	 * 获得wifi 信号的等级
	 * 
	 * @return wifi 信号的等级 值范围为（0，{@link #MAX_SIGNAL_NUMLEVELS}） ， 0 包括异常情况及正常情况
	 */
	public int getWifiSignalLevel() {
		// 已知至少一个机型（LenovoA828t）上面，getConnectionInfo()会抛异常，汗
		WifiInfo wifiInfo;
		try {
			wifiInfo = wifiManager.getConnectionInfo();
		} catch (Exception e) {
			return 0;
		}
		if (wifiInfo == null || wifiInfo.getBSSID() == null) {
			return 0;
		}
		int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), MAX_SIGNAL_NUMLEVELS + 1);
		if (level < 0) {
			level = 0;
		} else if (level > MAX_SIGNAL_NUMLEVELS) {
			level = MAX_SIGNAL_NUMLEVELS;
		}
		return level;
	}

	private final class MyEventObserver extends EventObserver {
		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			if (NetTypeDetector.NetType.WIFI == state) {
				unregisterPhoneStateListener();
				wifiSignalUpdateHandler.start();
			} else {
				wifiSignalUpdateHandler.stop();
				registerPhoneStateListener();
			}
		}
	}

	/**
	 * 监听手机信号改变
	 */
	// !!! 重要：必须是static内部类，不能有隐含的对外部类实例的强引用。 !!!
	// （原因：PhoneStateListener构造的时候会创建Handler，该Handler基于主线程）
	private final static class MyPhoneStateListener extends PhoneStateListener {

		// 重要：弱引用父对象，否则会有内存泄露
		private final WeakReference<NetSignalStrengthManager> owner;

		private MyPhoneStateListener(NetSignalStrengthManager owner) {
			this.owner = new WeakReference<NetSignalStrengthManager>(owner);
		}

		private static int invokeMethod(android.telephony.SignalStrength signalStrength, String name) {
			try {
				Method m = signalStrength.getClass().getMethod(name);
				if (m != null) {
					Object obj = m.invoke(signalStrength);
					if (obj != null && (obj instanceof Integer)) {
						return (Integer) obj;
					}
				}
			} catch (Exception e) {}
			return -1;
		}

		private static int calcLevelFromDBM(int dbm) {
			if (dbm >= -70) {
				return 4;
			} else if (dbm >= -85) {
				return 3;
			} else if (dbm >= -95) {
				return 2;
			} else if (dbm >= -100) {
				return 1;
			} else {
				return 0;
			}
		}

		private static int calcLevelFromECIO(int ecio) {
			// Ec/Io are in dB*10
			if (ecio >= -90) {
				return 4;
			} else if (ecio >= -110) {
				return 3;
			} else if (ecio >= -130) {
				return 2;
			} else if (ecio >= -150) {
				return 1;
			} else {
				return 0;
			}
		}

		private static int getSignalStrengthLevel(TelephonyManager telephonyManager,
			android.telephony.SignalStrength signalStrength) {
			int level = invokeMethod(signalStrength, "getLevel");
			if (level >= 0) {
				return Math.min(level, MAX_SIGNAL_NUMLEVELS);
			}
			// GSM
			if (signalStrength.isGsm()) {
				int value = signalStrength.getGsmSignalStrength();
				if (value != 99) {
					return calcLevelFromDBM(-113 + 2 * value);
				}
			}
			// 4G
			if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
				int lteLevel = invokeMethod(signalStrength, "getLteLevel");
				if (lteLevel >= 0) {
					return Math.min(lteLevel, MAX_SIGNAL_NUMLEVELS);
				}
			}
			//
			int snr = signalStrength.getEvdoSnr();
			if (snr < 0) {
				int levelDbm = calcLevelFromDBM(signalStrength.getCdmaDbm());
				int levelEcio = calcLevelFromECIO(signalStrength.getCdmaEcio());
				return (levelDbm < levelEcio) ? levelDbm : levelEcio;
			}

			if (snr >= 7) {
				return 4;
			} else if (snr >= 5) {
				return 3;
			} else if (snr >= 3) {
				return 2;
			} else if (snr >= 1) {
				return 1;
			} else {
				return 0;
			}
		}

		@Override
		public void onSignalStrengthsChanged(android.telephony.SignalStrength signalStrength) {
			NetSignalStrengthManager owner = this.owner.get();
			if (owner != null) {
				int currentSignalLevel = getSignalStrengthLevel(owner.telephonyManager, signalStrength);
				owner.mobileSignalLevel = currentSignalLevel;
				owner.updateSignalLevel(currentSignalLevel, false);
			}
		}
	}
}
