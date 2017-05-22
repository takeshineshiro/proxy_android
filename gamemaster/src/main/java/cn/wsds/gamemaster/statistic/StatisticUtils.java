package cn.wsds.gamemaster.statistic;

import android.content.Context;
import android.text.TextUtils;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.DeviceInfo;
import cn.wsds.gamemaster.data.FloatWindowMeasure;
import cn.wsds.gamemaster.data.PersistConfig;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;
import cn.wsds.gamemaster.statistic.Statistic.Event;

import com.subao.common.net.NetTypeDetector;

/**
 * 统计相关的工具类
 */
public class StatisticUtils {
	

	
	// 以下代码废弃于：2015-3-16
//	/**
//	 * 统计主页面按返回键的事件
//	 * @param gameSize 游戏数量
//	 */
//	public static void statisticMainKeybackEvent(int gameSize) {
//		String value = null;
//		if(gameSize<=0){
//			value = "none";
//		}else{
//			if(AccelOpenManager.isStarted()){
//				boolean hasRunGame = GameManager.getInstance().getSupportedAndRunningGames().size() > 0;
//				value = hasRunGame ? "game" : "on";
//			}else{
//				value = "off";
//			}
//		}
//		StatisticDefault.addEvent(AppMain.getContext(), StatisticDefault.Event.CLICK_BACK, value);
//	}
	
//    /**
//     * 统计 同时运行的游戏数 在主界面点击左右箭头时使用
//     * @param runSize 运行游戏的数量
//     */
//	public static void statisticUmGameSwitch(int runSize) {
//		StatisticDefault.addEvent(AppMain.getContext(), StatisticDefault.Event.CLICK_GAME_SWITCH, String.valueOf(runSize));
//	}
		
	/**
	 * 统计初始化事件
	 *  激活（首次使用app）、root
	 */
	public static void statisticInitializer(Context context, int gameSize) {
		//统计激活事件
		boolean activationReported = 
			ConfigManager.getInstance().isActivation()
			|| PersistConfig.getInstance().getActivationReported();
		if (!activationReported) {
			Statistic.addEvent(context, Statistic.Event.BACKSTAGE_ACTIVATION, Integer.toString(gameSize));
		}
		ConfigManager.getInstance().setActivation(true);
		PersistConfig.getInstance().setActivationReported();
		// 设备详情
		if (!PersistConfig.getInstance().getDeviceInfoReported()) {
			Statistic.addEvent(context, Statistic.Event.DEVICE_INFO, DeviceInfo.get(context));
			PersistConfig.getInstance().setDeviceInfoReported();
		}

		// 以下代码废弃于：2015-3-16
//		//统计root事件
//		if(!ConfigManager.getInstance().isReportRootinfo()){
//			String value = RootUtil.isRoot() ? "1" : "2";// 1 = root | 2 = not root
//			StatisticDefault.addEvent(AppMain.getContext(), StatisticDefault.Event.ROOTINFO, value);
//			ConfigManager.getInstance().setReportRootinfo(true);
//		}
		
	}

//	// 累计游戏时长
//	public static void statisticAccelTime(int accelTimeSeconds) {
//		// 以秒为单位时间值
//		int[] range = new int[] { 15 * 60 , 30 * 60 , 2 * 60 * 60 , 6 * 60 * 60 ,
//				24 * 60 * 60 };
//
//		StatisticDefault.Event[] eventId = new StatisticDefault.Event[] {
//				StatisticDefault.Event.STATUS_GAME_TIME_0,
//				StatisticDefault.Event.STATUS_GAME_TIME_1,
//				StatisticDefault.Event.STATUS_GAME_TIME_2,
//				StatisticDefault.Event.STATUS_GAME_TIME_3,
//				StatisticDefault.Event.STATUS_GAME_TIME_4 };
//
//		long lastAccelTime = ConfigManager.getInstance()
//				.getAccumulateAccelTimeSecond();
//
//		int index = 0;
//		for (int r : range) {
//			if (accelTimeSeconds >= r && lastAccelTime < r) {// 不需要break,需要全部统计
//				StatisticDefault.addEvent(AppMain.getContext(),
//						eventId[index]);
//			}
//			index++;
//		}
//		ConfigManager.getInstance().setAccumulateAccelTimeSecond(accelTimeSeconds);
//	}

//	/**
//	 * 统计进入页面时加速状态
//	 */
//	public static void statisticAccelState(int gameSize, int runSize) {
//		if (gameSize <= 0) {// 没有游戏
//			statisticVpnState("none");
//			return;
//		}
//
//		if (AccelOpenManager.isStarted()) {
//			// vpn 开启状态下判断运行游戏数量
//			String value = runSize > 0 ? "game" : "on";
//			statisticVpnState(value);
//		}else{// 加速未开启
//			statisticVpnState("off");
//		}
//	}

//	private static void statisticVpnState(String value) {
//		StatisticDefault.addEvent(AppMain.getContext(),
//				StatisticDefault.Event.STATUS_VPN, value);
//	}
	
//	/**
//	 * 测速结果统计
//	 * @param rc
//	 */
//	public static void statisticSpeedTestResultCause(int rc) {
//		StatisticDefault.Event causeEventId;
//		Context context = AppMain.getContext();
//		switch (NetManager.getInstance().getCurrentNetworkType(context)) {
//		case NetTypeDetector.NETWORK_CLASS_3G:
//			causeEventId = StatisticDefault.Event.NETWORK_SPEED_TEST_RESULT_CAUSE_3;
//			break;
//		case NetTypeDetector.NETWORK_CLASS_4G:
//			causeEventId = StatisticDefault.Event.NETWORK_SPEED_TEST_RESULT_CAUSE_4;
//			break;
//		case NetTypeDetector.NETWORK_CLASS_WIFI:
//			causeEventId = StatisticDefault.Event.NETWORK_SPEED_TEST_RESULT_CAUSE_W;
//			break;
//		default:
//			causeEventId = StatisticDefault.Event.NETWORK_SPEED_TEST_RESULT_CAUSE_O;
//			break;
//		}
//		StatisticDefault.addEvent(context, causeEventId, String.valueOf(rc));
//	
//	}
	
	/**
	 * 统计测速原因
	 * @param submitter 事件提交接口，为null则取StatisticDefault.DEFAULT_SUBMITTER
	 * @param netType 网络类型
	 * @param code 代码
	 */
	public static void statisticSpeedTestCause(Context context, Statistic.Submitter submitter, NetTypeDetector.NetType netType, int code) {
		if (submitter == null) {
			submitter = Statistic.DEFAULT_SUBMITTER;
		}
//		String param = getSpeedTestCauseDesc(code) + getSpeedTestResult(code);
		submitter.execute(context, getSpeedTestCauseId(netType),String.valueOf(code));
	}
	
//	private static String getSpeedTestCauseDesc(int code){
//		// case 中数值来源底层，因为只在该处使用，所以没有声明常量
//	    int cause = code % 10;
//	    switch (cause) {
//		case 1:
//			return "启动游戏测速";
//		case 2:
//			return "改变网络测速";
//		default:
//			return "原因异常";
//		}
//	}
//	
//	private static String getSpeedTestResult(int code){
//		// case 中数值来源底层，因为只在该处使用，所以没有声明常量
//		int result = code / 10;
//		switch (result) {
//		case 10:
//			return "测速成功";
//		case 11:
//			return "快速测速成功";
//		case 12:
//			return "部分测速成功";
//		case 20:
//			return "测速错误";
//		case 21:
//			return "网络不通";
//		case 22:
//			return "测速超时";
//		case 23:
//			return "网络错误";
//		default:
//			return "结果异常";
//		}
//	}
	

	private static Statistic.Event getSpeedTestCauseId(NetTypeDetector.NetType netType) {
		Statistic.Event causeEventId;
		switch (netType) {
		case MOBILE_3G:
			causeEventId = Statistic.Event.NETWORK_SPEED_TEST_RESULT_CAUSE_3;
			break;
		case MOBILE_4G:
			causeEventId = Statistic.Event.NETWORK_SPEED_TEST_RESULT_CAUSE_4;
			break;
		case WIFI:
			causeEventId = Statistic.Event.NETWORK_SPEED_TEST_RESULT_CAUSE_W;
			break;
		default:
			causeEventId = Statistic.Event.NETWORK_SPEED_TEST_RESULT_CAUSE_O;
			break;
		}
		return causeEventId;
	}


//	/**
//	 * 统计测速结果
//	 * @param submitter 事件提交接口，为null则取StatisticDefault.DEFAULT_SUBMITTER
//	 * @param netType 网络类型
//	 * @param delay 时延
//	 */
//	public static void statisticSpeedTestResult(StatisticDefault.Submitter submitter, int netType, int delay) {
//		String range;
//		String netTypeName;
//		StatisticDefault.Event resultEventId;
//		Context context = AppMain.getContext();
//		switch (netType) {
//		case NetTypeDetector.NETWORK_CLASS_3G:
//			range = getDelayRangeThird(delay);
//			netTypeName = "3G";
//			resultEventId = StatisticDefault.Event.SPEED_TEST_RESULT_3G;
//			break;
//		case NetTypeDetector.NETWORK_CLASS_4G:
//			range = getDelayRangeWiFi(delay);
//			netTypeName = "4G";
//			resultEventId = StatisticDefault.Event.SPEED_TEST_RESULT_4G;
//			break;
//		case NetTypeDetector.NETWORK_CLASS_WIFI:
//			range = getDelayRangeWiFi(delay);
//			netTypeName = "WiFi";
//			resultEventId = StatisticDefault.Event.SPEED_TEST_RESULT_WIFI;
//			break;
//		default:
//			return;
//		}
//		if (submitter == null) {
//			submitter = StatisticDefault.DEFAULT_SUBMITTER;
//		}
//		String value = new StringBuffer(netTypeName).append("-").append(range).toString();
//		submitter.execute(context, resultEventId, value);
//	}

//	/**
//	 * 获取延时区间
//	 * 
//	 * @param delay
//	 *            延时
//	 * @return wifi和4g的延时区间
//	 */
//	private static String getDelayRangeWiFi(int delay) {
//		if (delay >= 2000) {
//			return ">=2000";
//		}
//		if (delay >= 500) {
//			return "[500-2000)";
//		}
//		if (delay >= 200) {
//			return "[200-500)";
//		}
//		if (delay >= 100) {
//			return "[100-200)";
//		}
//		if (delay >= 50) {
//			return "[50-100)";
//		}
//		if (delay >= 20) {
//			return "[20-50)";
//		}
//		if (delay >= 0) {
//			return "[0-20)";
//		}
//		return null;
//	}

//	/**
//	 * 获取延时区间
//	 * 
//	 * @param delay
//	 *            延时
//	 * @return 3g的延时区间
//	 */
//	private static String getDelayRangeThird(int delay) {
//		if (delay >= 5000) {
//			return ">=5000";
//		}
//		if (delay >= 2000) {
//			return "[2000-5000)";
//		}
//		if (delay >= 500) {
//			return "[500-2000)";
//		}
//		if (delay >= 200) {
//			return "[200-500)";
//		}
//		if (delay >= 100) {
//			return "[100-200)";
//		}
//		if (delay >= 50) {
//			return "[50-100)";
//		}
//		if (delay >= 0) {
//			return "[0-50)";
//		}
//		return null;
//	}
	
//	/**
//	 * 统计断线重连原因
//	 * @param recentState
//	 */
//	public static void statisticConnectionCause(RecentState recentState){
//		StatisticDefault.addEvent(AppMain.getContext(),StatisticDefault.Event.REPAIR_CONNECTION_CAUSE,recentState.name());
//	}
	
//	/**
//	 * 统计root模式 开关
//	 * @param context
//	 * @param flow
//	 */
//	public static void statisticSwitchRootmodeOn(String flow){
//		StatisticDefault.addEvent(AppMain.getContext(),StatisticDefault.Event.SWITCH_ROOTMODE_ON, flow);
//	}
	
//	/**
//	 * 统计“开启加速成功”
//	 */
//	public static void statisticAccStart() {
//		String param;
//		switch (VPNManager.getInstance().getAccelStatus()) {
//		case STARTED_WITH_ROOT:
//			param = "root";
//			break;
//		case STARTED_WITH_VPN:
//			param = "vpn";
//			break;
//		default:
//			return;
//		}
//		StatisticDefault.addEvent(AppMain.getContext(), StatisticDefault.Event.ACC_ALL_START_SUCCESS);
//	}
	
//	public static void statisticAccStartUpEvent(boolean isRootModel, boolean result) {
//		String modle = isRootModel ? "root" : "vpn";
//		String value = result ? "1" : "2";
//		String param = String.format("%s.%s", modle,value);
//		StatisticDefault.addEvent(AppMain.getContext(), StatisticDefault.Event.ACC_STARTUP,param);
//	}

	public static void statisticShareComplete(Context context, Event evnet,ShareType shareType) {
		if(shareType==null){
			return;
		}
		String value = getShareTypeDesc(shareType);
		Statistic.addEvent(context, evnet,value);
	}

	public static String getShareTypeDesc(ShareType shareType) {
		switch (shareType) {
		case ShareToFriends:
			return "weixin2";
		case ShareToQQ:
			return "qq";
		case ShareToSina:
			return "weibo";
		case ShareToWeixin:
			return "weixin";
		case ShareToZone:
			return "qzone";
		default:
			return null;
		}
	}

	public static void statisticHookResult(Context context, int resultCode) {
		// case 中数值来源底层，因为只在该处使用，所以没有声明常量
		String result;
		switch (resultCode) {
		case 0:
			result = "成功";
			break;
		case 2:
			result = "已HOOK";
			break;
		default:
			result = "失败";
			break;
		}
		Statistic.addEvent(context, Statistic.Event.BACKSTAGE_HOOK_RESULT, result);
	}
	
//	private static String getAccelStateParam(int code){
//		// case 中数值来源底层，因为只在该处使用，所以没有声明常量
//		switch(code){
//		case 10:
//			return "节点端到端小于50ms（使用节点）";
//		case 11:
//			return "节点端到端快于透传（使用节点）";
//		case 12:
//			return "透传快于节点端到端但不超过10ms（使用节点）";
//		case 13:
//			return "节点端到端连通透传未连通（使用节点）";
//		case 14:
//			return "节点端到端连通透传连接失败（使用节点）";
//		case 20:
//			return "游戏连接成功节点失败或透传快于节点端到端";
//		case 30:
//			return "网段未命中";
//		case 40:
//			return "透传节点都连接失败";
//		case 50:
//			return "测速失败";
//		case 51:
//			return "正在测速";
//		case 52:
//			return "2G或断网";
//		default://TODO 
//			return "网段找到但没有测速结果";
//		}
//	}
	
	/**
	 * 网络诊断所用的统计事件参数生成
	 */
	public static class ParamBuilder_NetDiagnose {
		
		private static final int[] DELAY_RANGE = {
			5, 10, 15, 20, 25, 30, 40, 50, 60, 80, 100, 150, 200
		};

		public static String delayToRange(int delay) {
			if (delay >= 0) {
				for (int i = 0; i < DELAY_RANGE.length; ++i) {
					int value = DELAY_RANGE[i];
					if (delay <= value) {
						int prev_value = (i == 0) ? 0 : DELAY_RANGE[i - 1] + 1;
						return String.format("%d-%d", prev_value, value);
					}
				}
			}
			return ">" + DELAY_RANGE[DELAY_RANGE.length - 1];
		}
	}

	/**
	 * 统计用户积分区间
	 * @param score
	 */
	public static void statisticUserScore(Context context, int score) {
		String scoreRegion = getScoreRegion(score);
		if(TextUtils.isEmpty(scoreRegion)){
			return;
		}
		Statistic.addEvent(context, Statistic.Event.USER_INTEGRAL_SECTION, scoreRegion);
	}

	private static String getScoreRegion(int score) {
		if(score < 0){
			return null;
		}else if(score <= 20){
			return "0-20";
		}else if(score <= 50){
			return "21-50";
		}else if(score <= 100){
			return "51-100";
		}else if(score <= 200){
			return "101-200";
		}else if(score <= 400){
			return "201-400";
		}else if(score <= 600){
			return "401-600";
		}else if(score <= 800){
			return "601-800";
		}else if(score <= 1000){
			return "801-1000";
		}else if(score <= 1200){
			return "1001-1200";
		}else if(score <= 1500){
			return "1201-1500";
		}else{
			return ">1500";
		}
	}
	
	public static void statisticUserRegisterResult(Context context, boolean result){
		String strParam = result ? "成功": "失败";
		Statistic.addEvent(context, Statistic.Event.USER_REGISTER_RESULT, strParam);
	}

	public static void statisticFloatwindowType(Context context, FloatWindowMeasure.Type newType) {
		  String str;
          switch (newType) {
              case MINI:
                  str = "小";
                  break;
              case LARGE:
                  str = "大";
                  break;
              case NORMAL:
              default:
                  str = "中";
                  break;
          }
          Statistic.addEvent(context, Statistic.Event.FLOATING_WINDOW_SETTING_CLICK_SIZE, str);		
	}

	/**
	 * 用户进入兑换中心时积分情况
	 * @param context 
	 */
	public static void statisticUserExchangeCenterInScore(Context context) {
		UserInfo userInfo = UserSession.getInstance().getUserInfo();
		String params;
		if(userInfo == null){
			params = "-1";
		}else if(userInfo.getScore() < 50){
			params = "<50";
		}else if(userInfo.getScore() < 100){
			params = "50-100";
		}else if(userInfo.getScore() < 200){
			params = "100-200";
		}else{
			params = ">200";
		}
		Statistic.addEvent(context, Statistic.Event.USER_EXCHANGE_CENTRE_IN_SCORE,params);
	}
}
