package cn.wsds.gamemaster.thread;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

public class GameRunningTimeObserver extends EventObserver {
	public static long NOTIFICATION_DURATION = 15 * 60 * 1000;
	
	private final ActivityManager am;
	private final MyHandler myHandler = new MyHandler();
	
	private GameInfo lastRunningGameInfo;
	private long lastLaunchedTime;
	private Container gameContainer;
	
	private static class Container{
		private final GameInfo gameInfo;
		private final long runningTime;
		private int tryCount;
		
		public Container(GameInfo gameInfo, long runningTime){
			this.gameInfo = gameInfo;
			this.runningTime = runningTime;
		}
	}
	
	public static final GameRunningTimeObserver instance = new GameRunningTimeObserver();

	private GameRunningTimeObserver() {
		am = (ActivityManager)AppMain.getContext().getSystemService(Context.ACTIVITY_SERVICE);
	}
	
	// 注：由于GameRunningTimeObserver是一个永久生存的单例，所以不存在Handler Leak问题
	@SuppressLint("HandlerLeak")
	private class MyHandler extends Handler {
		private static final int MSG_CHECK = 0;
		private static final int CHECK_DURATION = 5 * 1000;

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_CHECK:
				if (gameContainer == null){
					return;
				}
				if (!checkGameRunningStatus(gameContainer.gameInfo)){
					int delay = getPercentDelayDecrease(gameContainer.gameInfo);
					AppNotificationManager.sendGameAccelResult((int)gameContainer.runningTime / 1000, delay, gameContainer.gameInfo);
					Statistic.addEvent(AppMain.getContext(), Statistic.Event.NOTIFICATION_REPORT_GAME);
					gameContainer = null;
				} else if (gameContainer.tryCount >= 2){
					Statistic.addEvent(AppMain.getContext(), Statistic.Event.NOTIFICATION_REPORT_GAME2);
					gameContainer = null;
				} else{
					++ gameContainer.tryCount;
					myHandler.sendEmptyMessageDelayed(MSG_CHECK, CHECK_DURATION);
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * 找到GameInfo里最近一次加速的“延迟降低百分比”
	 */
	private int getPercentDelayDecrease(GameInfo gameInfo){
		int result = gameInfo.getPercentDelayDecreaseLastAccel();
		if (result <= 0) {
			return 50;
		} else {
			return result;
		}
	}

	@Override
	public void onTopTaskChange(GameInfo gameInfo) {
		if (lastRunningGameInfo == null) {
			if (gameInfo != null) {
				lastRunningGameInfo = gameInfo;
				if (AccelOpenManager.isStarted()){
					lastLaunchedTime = SystemClock.elapsedRealtime();
				} else {
					lastLaunchedTime = Long.MAX_VALUE;
				}
			}
		} else {
			long runningTime = SystemClock.elapsedRealtime() - lastLaunchedTime;
			if (runningTime >= NOTIFICATION_DURATION && ConfigManager.getInstance().getSendNoticeAccelResult()){
				gameContainer = new Container(lastRunningGameInfo, runningTime);
				myHandler.removeCallbacksAndMessages(null);
				myHandler.sendEmptyMessage(MyHandler.MSG_CHECK);
			}
			lastRunningGameInfo = gameInfo;
			lastLaunchedTime = SystemClock.elapsedRealtime();
		}
	}
	
	@Override
	public void onVPNOpen() {
		lastLaunchedTime = SystemClock.elapsedRealtime();
	}
	
	@Override
	public void onVPNClose() {
		lastLaunchedTime = Long.MAX_VALUE;
	}
	
	@Override
 	public void onScreenOn() {
		lastLaunchedTime = SystemClock.elapsedRealtime();
	}
	
	@Override
 	public void onScreenOff() {
		lastLaunchedTime = Long.MAX_VALUE;
	}
	
	private boolean checkGameRunningStatus(GameInfo gameInfo){
		if (gameInfo == null) { return false; }
		String packageName = gameInfo.getPackageName();
		if (TextUtils.isEmpty(packageName)) { return false; }
		List<RunningAppProcessInfo> lr = am.getRunningAppProcesses();
		if (lr == null) { return false; }
		for (RunningAppProcessInfo ap : lr){
			if (ap.processName != null && ap.processName.equals(gameInfo.getPackageName())){
				return true;
			}
		}
		return false;
	}

}
