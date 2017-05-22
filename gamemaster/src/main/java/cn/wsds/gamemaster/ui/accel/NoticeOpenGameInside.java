package cn.wsds.gamemaster.ui.accel;

import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.tools.VPNUtils;

/**
 * 负责监视顶层任务，如果需要，推送“在APP内开启游戏”的通知
 */
public class NoticeOpenGameInside {

	private static final long MIN_TIME_BETWEEN_TWO_NOTICE = 1000 * 3600 * 8;

	private static final String TAG = "NoticeOpenGameInside" ;
	
	private static class Observer extends EventObserver {

		@Override
		public void onTopTaskChange(GameInfo info) {
			if (info == null) {
				return;
			}
			if (!AccelOpenManager.isStarted()) {
				return;
			}
	
			if(VPNUtils.isNodeAlreadyDetected(info.getUid(), TAG)){
				return ;
			}
			
			/*if (VPNManager.getInstance().isNodeAlreadyDetected(info.getUid())) {			 
				return;
			}*/
			if (GameManager.getInstance().isGameLaunchFromMe(info.getUid())) {
				return;
			}
			execute();
		}
	};

	private static Observer observer;

	public static void init() {
		if (observer == null) {
			observer = new Observer();
			TriggerManager.getInstance().addObserver(observer);
		}
	}

	/**
	 * 推送通知，除非距上一次推送时间不足8小时
	 * 
	 * @return true表示成功推送，false表示未推送（因为距上一次推送时间不足8小时）
	 */
	public static boolean execute() {
		long lastTime = ConfigManager.getInstance().getTimeOfNoticeOpenGameInside();
		long now = System.currentTimeMillis();
		if (now - lastTime >= MIN_TIME_BETWEEN_TWO_NOTICE || lastTime - now >= 1000 * 3600) {
			AppNotificationManager.sendOpenGameInside();
			ConfigManager.getInstance().setTimeOfNoticeOpenGameInside(now);
			return true;
		}
		return false;
	}
}
