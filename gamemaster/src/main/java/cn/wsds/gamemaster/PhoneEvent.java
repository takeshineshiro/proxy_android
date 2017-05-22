package cn.wsds.gamemaster;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class PhoneEvent {

	public interface Observer {
		/** 有来电 */
		public void onPhoneRinging(String incomingNumber);

		/** 摘机（接通） */
		public void onPhoneOffhook();

		/** 无来电（空闲）或已挂断 */
		public void onPhoneIdle();
	}

	private class MyPhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				for (Observer o : observers) {
					o.onPhoneIdle();
				}
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				for (Observer o : observers) {
					o.onPhoneRinging(incomingNumber);
				}
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				for (Observer o : observers) {
					o.onPhoneOffhook();
				}
				break;
			}
		}
	}

	private static final PhoneEvent instance = new PhoneEvent();

	private final List<Observer> observers = new ArrayList<Observer>();

	private MyPhoneStateListener phoneStateListener;

	public static PhoneEvent getInstance() {
		return instance;
	}

	private PhoneEvent() {

	}

	private TelephonyManager GetTelephonyManager() {
		Context context = AppMain.getContext();
		return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	}

	private void registerPhoneListener() {
		if (phoneStateListener == null) {
			phoneStateListener = new MyPhoneStateListener();
			try {
				TelephonyManager tm = GetTelephonyManager();
				tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
			} catch (RuntimeException re) {
				// 可能会有SecurityException异常
				phoneStateListener = null;
			}
		}
	}

	private void unregisterPhoneListener() {
		if (phoneStateListener != null) {
			TelephonyManager tm = GetTelephonyManager();
			tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
			phoneStateListener = null;
		}
	}

	/**
	 * 添加一个观察者。非线程安全，只能在UI线程中调用
	 */
	public void addObserver(Observer o) {
		if (observers.indexOf(o) < 0) {
			observers.add(o);
			registerPhoneListener();
		}
	}

	/**
	 * 移除一个观察者。非线程安全，只能在UI线程中调用
	 * 
	 * @param o
	 */
	public void removeObserver(Observer o) {
		observers.remove(o);
		if (observers.isEmpty()) {
			unregisterPhoneListener();
		}
	}
}
