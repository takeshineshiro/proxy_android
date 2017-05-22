package cn.wsds.gamemaster.ui.floatwindow;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Message;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.PhoneEvent;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.Statistic.Event;
import cn.wsds.gamemaster.tools.ContactsUtils;

import com.subao.resutils.WeakReferenceHandler;

public class GameForgroundDetect extends EventObserver {
	public static final GameForgroundDetect instance = new GameForgroundDetect();

	private GameForgroundDetect() {}

	private MyObserver myObserver = new MyObserver();
	private int taskId = -1;

	private boolean ignore = false;
	private boolean success = false;
	private boolean hasCall = false;
	private GameInfo gameInfo;
	private MyHandler myHandler = new MyHandler(this);
	private boolean hasRing = false;
	private long startTime = -1;

	private static final long MIN_VALID_CALL_DURATION = 2000;

	public void setIgnore(boolean value) {
		ignore = value;
	}

	public void setHasCalled() {
		hasCall = true;
	}

	public GameInfo getCurrentGameInfo() {
		return gameInfo;
	}

	@Override
	public void onTopTaskChange(GameInfo info) {
		gameInfo = info;
		if (info != null) {
			PhoneEvent.getInstance().addObserver(myObserver);
			taskId = info.getTaskId();
			success = false;
		} else {
			if (!isInCallPage()) {
				CallInterceptWindow.destoryInstance();
				if (hasRing) {
					CallUtils.changeToDefaultAnswer();
				}
			}
			taskId = -1;
			if (hasCall) {
				Statistic.addEvent(AppMain.getContext(), Event.FLOATING_WINDOW_CALL_CONTACTS_RESULT, success ? "成功 " : "失败");
				hasCall = false;
			}
			success = false;
		}
	}

	@SuppressWarnings("deprecation")
	private String getTopActivityName() {
		ActivityManager activityManager = (ActivityManager) AppMain.getContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> list = activityManager.getRunningTasks(1);
		if (list != null && !list.isEmpty()) {
			RunningTaskInfo runningTaskInfo = list.get(0);
			if (runningTaskInfo != null) {
				ComponentName componentName = runningTaskInfo.topActivity;
				if (componentName != null) {
					return componentName.getPackageName();
				}
			}
		}
		return null;
	}

	private boolean isInCallPage() {
		String currentActivity = getTopActivityName();
		if (currentActivity == null) {
			return false;
		}
		return currentActivity.equals("com.android.phone") || currentActivity.equals("com.android.incallui");
	}

	private class MyObserver implements PhoneEvent.Observer {

		@Override
		public void onPhoneRinging(String incomingNumber) {
			if (gameInfo == null) {
				return;
			}
			ConfigManager config = ConfigManager.getInstance();
			config.setPhoneIncomingHappenedWhenGameForeground();
			if (ignore) {
				return;
			}
			if (!config.getCallRemindGamePlaying()) {
				return;
			}
			if (!config.getFloatWindowActivated()) {
				// 还没有激活（点击或拖动）过小悬浮窗。
				return;
			}
			hasRing = true;
			startTime = System.currentTimeMillis();
			String phoneNumber = ContactsUtils.getDisplayName(incomingNumber);
			phoneNumber = phoneNumber != null ? phoneNumber : "未知号码";
			CallInterceptWindow.createInstance(AppMain.getContext(), phoneNumber, taskId);
			ContactsUtils.destoryDialog();
		}

		@Override
		public void onPhoneOffhook() {
			if (gameInfo != null) {
				success = true;
			}
		}

		@Override
		public void onPhoneIdle() {
			CallInterceptWindow.hide();
			if (hasRing) {
				hasRing = false;
				if (startTime > 0 && System.currentTimeMillis() - startTime > MIN_VALID_CALL_DURATION) {
					myHandler.sendEmptyMessageDelayed(MyHandler.MSG_AUTHORIZE, 1000);
				}
			}
			ignore = false;
		}

	}

	private static class MyHandler extends WeakReferenceHandler<GameForgroundDetect> {
		private static final int MSG_AUTHORIZE = 0;

		public MyHandler(GameForgroundDetect ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(GameForgroundDetect ref, Message msg) {
			switch (msg.what) {
			case MSG_AUTHORIZE:
				ContactsUtils.query();
				break;

			default:
				break;
			}
		}

	}

}
