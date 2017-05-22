package cn.wsds.gamemaster.event;

import java.util.Random;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.statistic.Statistic;


/**
 * 游戏累计加速时长改变监听器
 */
public class AccelTimeChangedListener implements cn.wsds.gamemaster.app.GameManager.Observer {

//	/**
//	 * 提醒范围  单位 h
//	 * 新增加数据必须倒序放 
//	 *   满足大范围时一定已经满足小范围，所以判断已经满足大范围时就不需要再去判断小范围
//	 */
//	private final int[] remindRange = new int[]{
//			12,9,6,3
//	}; 
//	
	/** 以秒为基数的单位 */
	private static final int UNITBASE = 3600;
	/** 测试字段 */
	public static int unitbaseDebug = -1;
	
	@Override
	public void onAccelTimeChanged(int seconds) {
		judgeNeedRemind(seconds);
//		StatisticUtils.statisticAccelTime(seconds);
	}
	
	@Override
	public void onGameListUpdate() {
		// do nothing		
	}

    @Override
    public void onDoubleAccelTimeChanged(String packageName, int seconds) {
        // do nothing
    }

    /**
	 * 判断是否需要提醒加速时长
	 * @param seconds
	 */
	private void judgeNeedRemind(int seconds) {
		if(ConfigManager.getInstance().isAlreadySendNoticeAccelAchieve()){
			return;
		}
		int unit = unitbaseDebug > 0 ? unitbaseDebug :UNITBASE;
		if(6 * unit > seconds){
			return;
		}
		int achieve = 50 +new Random().nextInt(10);
		ConfigManager.getInstance().setGamePlayAchievePercent(achieve);
		AppNotificationManager.sendGamePlayAchievements(achieve);
		ConfigManager.getInstance().setAlreadySendNoticeAccelAchieve();
		ConfigManager.getInstance().setTimeOfNoticeAccelAchieve(System.currentTimeMillis());
		Statistic.addEvent(AppMain.getContext(),Statistic.Event.NOTIFICATION_USE_REPORT);
		
//		int lastRemindAccelTimeRange = ConfigManager.getInstance()
//				.getLastRemindAccelTimeRange();
//		// 遍历可提醒区间范围，满足该区间条件时进行提醒
//		for (int range : remindRange) {
//			if (lastRemindAccelTimeRange == range) {
//				// 已经遍历到上次提醒的范围没必要再去检查判断
//				break;
//			}
//			// 为满足测试添加debug变量
//			int rangeValue = range * (unitbaseDebug > 0 ? unitbaseDebug :UNITBASE);
//			if (rangeValue < seconds) {
//				// 当前加速时长大于加速区间范围可进行提醒
//				ConfigManager.getInstance().setLastRemindAccelTimeRange(range);
//				AppNotificationManager.sendGamePlayAchievements(range);
//				StatisticDefault.addEvent(AppMain.getContext(),StatisticDefault.Event.NOTIFICATION_REPORT);
//				break;
//			}
//		}
	}

}
