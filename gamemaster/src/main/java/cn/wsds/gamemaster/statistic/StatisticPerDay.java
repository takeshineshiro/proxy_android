package cn.wsds.gamemaster.statistic;

import android.content.Context;
import android.util.Log;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.FloatWindowMeasure;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.tools.AppsWithUsageAccess;
import cn.wsds.gamemaster.tools.RootUtil;
import cn.wsds.gamemaster.ui.UIUtils;

import com.subao.common.utils.CalendarUtils;

/**
 * 每天上传一次的各数据
 */
public class StatisticPerDay {
	
	private static final boolean LOG = false;
	private static final String TAG = "StatisticPerDay";
	
	/**
	 * 如果今天没有上报过，那么就上报一次
	 */
	public static void reportIfNeed(Context context) {
		int today = CalendarUtils.todayLocal();
		ConfigManager config = ConfigManager.getInstance();
		if (config.getDayOfSettingValueReport() == today) {
			if (LOG) {
				Log.d(TAG, "Day equals, do not need report");
			}
			return;
		}
		if (LOG) {
			Log.d(TAG, "Report settings");
		}
		if (UIUtils.isCallRemindSupportCurrentRom()) {
			report(context, Statistic.Event.FLOATING_WINDOW_SWITCH_CALLMANAGER, config.getCallRemindGamePlaying());
		}
		report(context, Statistic.Event.FLOATING_WINDOW_SETTING_SIZE, floatWindowSizeToString(config.getFloatwindowMeasureType()));
		report(context, Statistic.Event.FLOATING_WINDOW_SETTING_DISPLAY, config.getShowFloatWindowInGame());
		report(context, Statistic.Event.ACC_ALL_START_MODE_NEW, accelStateDesc());
		report(context, Statistic.Event.ACC_POWER_SWITCH_STARTUP, ConfigManager.getInstance().getBootAutoAccel());
		report(context, Statistic.Event.INTERACTIVE_SETTING_SWITCH_CLEARRAM, ConfigManager.getInstance().getAutoProcessClean() ? "自动" : "手动");
		report(context, Statistic.Event.INTERACTIVE_SETTING_SWITCH_ACC_RESULT, ConfigManager.getInstance().getSendNoticeAccelResult() ? "开启推送" : "未开启");
		
		statisticClearRam(context);
		if (RootUtil.isRoot()) {
			Statistic.addEvent(context, Statistic.Event.BACKSTAGE_ROOT_USER);
		}
//		report(context, StatisticDefault.Event.FLOATING_WINDOW_CLICK_SKIN_SKLR, ConfigManager.getInstance().isFloatwindowHunterSkin());
		if (android.os.Build.VERSION_CODES.LOLLIPOP <= android.os.Build.VERSION.SDK_INT) {
			report(context, Statistic.Event.INTERACTIVE_SETTING_AUTHORIZE_FLOATING, AppsWithUsageAccess.hasEnable() ? "授权成功" : "授权失败");
		}

		reportAccelTime(context);
		
		if(UserSession.isLogined()){
			int score = UserSession.getInstance().getUserInfo().getScore();
			StatisticUtils.statisticUserScore(context, score);
		}
		//
		// 在CONFIG里记一下：今天已经报过了
		config.setDayOfSettingValueReport(today);
	}

	private static void statisticClearRam(Context context) {
		int param;
		if(ConfigManager.getInstance().getAutoProcessClean()){
			int index = ConfigManager.getInstance().getAutoCleanProcessInternal();
			switch (index) {
			case 2:
				param = 10;
				break;
			case 3:
				param = 15;
				break;
			default:
				param = 5;
				break;
			}
		}else{
			param = 0;
		}
		report(context, Statistic.Event.INTERACTIVE_SETTING_CLEARRAM_TIME, String.valueOf(param));
	}

	private static void reportAccelTime(Context context) {
		// 到今天为止的累计加速时长
		ConfigManager.AccelTimeRecord recToday = new ConfigManager.AccelTimeRecord(
			CalendarUtils.todayLocal(),
			GameManager.getInstance().getAccelTimeSecondsAmount(),
			GameManager.getInstance().getGameForegroundTimeAmount());
		ConfigManager.AccelTimeRecord recLastDay = ConfigManager.getInstance().getAccelTimeRecord();
		if (recLastDay != null) {
			// 仅当以前记录过时，才计算差值并上报。
			String param = secondsToAccelTimeStatisticParam(recToday.accelSeconds - recLastDay.accelSeconds);
			Statistic.addEvent(context, Statistic.Event.ACC_GAME_PLAY_TIME, param);
			param = secondsToAccelTimeStatisticParam(recToday.foregroundSeconds - recLastDay.foregroundSeconds);
			Statistic.addEvent(context, Statistic.Event.ACC_GAME_PLAY_TIME_TOTAL, param);
		}
		ConfigManager.getInstance().setAccelTimeRecord(recToday);
	}

	private static void report(Context context, Statistic.Event event, boolean onOrOff) {
		report(context, event, onOrOff ? "开" : "关");
	}

	private static void report(Context context, Statistic.Event event, String str) {
		Statistic.addEvent(context, event, str);
	}

	private static String floatWindowSizeToString(int configValue) {
		if (configValue == FloatWindowMeasure.Type.MINI.ordinal()) {
			return "小";
		}
		if (configValue == FloatWindowMeasure.Type.LARGE.ordinal()) {
			return "大";
		}
		return "中";
	}
	
	private static String accelStateDesc(){
		 if(RootUtil.isRoot()){
			 if(ConfigManager.getInstance().isRootMode()){
				 return "RR";
			 }else{
				 return "RV";
			 }
		 }else{
			 return "VV";
		 }
	}

	/**
	 * 将加速时长秒数，转换成统计数据参数。（精确到半小时）
	 */
	public static String secondsToAccelTimeStatisticParam(int seconds) {
		int half_hours = (seconds + 900) / 1800;
		if (half_hours <= 0) {
			return "0小时";
		}
		if (half_hours >= 48) {
			return "24小时";
		}
		int h = half_hours >> 1;
		int r = half_hours & 1;
		StringBuffer sb = new StringBuffer(16);
		sb.append(h);
		if (r != 0) {
			sb.append('.');
			sb.append('5');
		}
		sb.append("小时");
		return sb.toString();
	}
}
