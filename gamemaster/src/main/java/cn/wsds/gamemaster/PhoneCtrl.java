package cn.wsds.gamemaster;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.android.internal.telephony.ITelephony;

/**
 * 电话接听拒绝挂断等操作的控制
 */
public class PhoneCtrl {

	/**
	 * 拒接或挂断电话
	 */
	public static void rejectCall(Context context) {
		ITelephony iTel = getTelephony(context);
		if (iTel != null) {
			try {
				iTel.endCall();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 接听来电
	 */
	public static void acceptCall(Context context) {
		sendMediaKeyButtonUpDown(context, 79);
	}

	private static ITelephony getTelephony(Context context) {
		TelephonyManager telMag = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		Class<TelephonyManager> c = TelephonyManager.class;
		Method method = null;
		try {
			method = c.getDeclaredMethod("getITelephony");
			if (method == null) {
				return null;
			}
			method.setAccessible(true);
			return (ITelephony) method.invoke(telMag);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void sendMediaKeyButtonUpDown(Context context, int paramInt) {
		Intent localIntent1 = new Intent("android.intent.action.MEDIA_BUTTON");
		localIntent1.putExtra("android.intent.extra.KEY_EVENT", new KeyEvent(0, paramInt));
		context.sendOrderedBroadcast(localIntent1, "android.permission.CALL_PRIVILEGED");
		Intent localIntent2 = new Intent("android.intent.action.MEDIA_BUTTON");
		localIntent2.putExtra("android.intent.extra.KEY_EVENT", new KeyEvent(1, paramInt));
		context.sendOrderedBroadcast(localIntent2, "android.permission.CALL_PRIVILEGED");
	}
}
