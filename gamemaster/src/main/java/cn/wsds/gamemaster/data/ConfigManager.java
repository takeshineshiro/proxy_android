package cn.wsds.gamemaster.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

import com.subao.common.Misc;
import com.subao.common.SuBaoObservable;
import com.subao.common.msg.Message_VersionInfo;
import com.subao.common.utils.CalendarUtils;
import com.subao.common.utils.ThreadUtils;
import com.subao.utils.UrlConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.ui.UIUtils;

public class ConfigManager extends SuBaoObservable<ConfigManager.Observer> {

	private static final String KEY_AUTO_CLEAN_PROCESS_INTERNAL = "auto_clean_process_internal";
	private static final String KEY_LAST_FLOW_OF_DOUBLE_ACCEL = "last_flow_double_accel";
	private static final String KEY_LAST_UDP_FLOW_OF_DOUBLE_ACCEL = "last_udp_flow_double_accel";
	private static final String KEY_LAST_TCP_FLOW_OF_DOUBLE_ACCEL = "last_tcp_flow_double_accel";
	private static final String KEY_LAST_DAY_OF_TCP_FLOW = "last_day_of_tcp_flow";
	private static final String KEY_LAST_DAY_OF_UDP_FLOW = "last_day_of_udp_flow";
	private static final String KEY_LAST_ACEEL_TCP_FLOW = "last_accel_tcp_flow";
	private static final String KEY_LAST_ACEEL_UDP_FLOW = "last_accel_udp_flow";

	private int autoCleanProcessInternal;
	/** 上一次上报并联加速流量值 */
	private long lastFlowOfDoubleAccel;

	private long lastTcpFlowOfDoubleAccel;

	private long lastUdpFlowOfDoubleAccel;
	/** 上一次上报tcp流量的天数*/
	private int lastDayOfTcpFlow;

	/** 上一次上报tcp流量的天数*/
	private int lastDayOfUdpFlow;
	private long lastAccelTcpFlow;
	private long lastAccelUdpFlow;

	/**
	 * Config的观察者，当某项配置发生改变时被通知
	 */
	public interface Observer {
		/**
		 * 当“在小悬浮窗里显示延迟值”开关发生改变时被调用
		 */
		void onShowDelayInFloatWindowChange(boolean show);

		/**
		 * 当“自动定时清理进程”开关发生改变时被调用
		 */
		void onAutoCleanProgressSwitchChange(boolean on);
		
		/**
		 * 当“是否显示悬浮窗”开关发生改变时被调用
		 */
		void onFloatWindowSwitchChange(boolean on);
	}

	/** 加速时长记录 */
	public static class AccelTimeRecord {

		/** 哪一天？ */
		public final int day;

		/** 截止到当天为止的累计加速时长 （秒） */
		public final int accelSeconds;

		/** 截止到当天为止的累计在前台时长（秒） */
		public final int foregroundSeconds;

		public AccelTimeRecord(int day, int accelSeconds, int foregroundSeconds) {
			this.day = day;
			this.accelSeconds = accelSeconds;
			this.foregroundSeconds = foregroundSeconds;
		}

		public String saveToString() {
			return String.format("%d,%d,%d", day, this.accelSeconds, this.foregroundSeconds);
		}

		public static AccelTimeRecord createFromString(String s) {
			if (!TextUtils.isEmpty(s)) {
				String[] fields = s.split(",");
				if (fields.length >= 2) {
					try {
						int day = Integer.parseInt(fields[0]);
						int accelSeconds = Integer.parseInt(fields[1]);
						int foregroundSeonds;
						// 2016.5.19日为v2.2.0版本新增了一个字段
						if (fields.length >= 3) {
							foregroundSeonds = Integer.parseInt(fields[2]);
						} else {
							foregroundSeonds = accelSeconds;
						}
						return new AccelTimeRecord(day, accelSeconds, foregroundSeonds);
					} catch (NumberFormatException e) {}
				}
			}
			return null;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (this == o) {
				return true;
			}
			if (!(o instanceof AccelTimeRecord)) {
				return false;
			}
			AccelTimeRecord rec = (AccelTimeRecord) o;
			return this.day == rec.day && this.accelSeconds == rec.accelSeconds && this.foregroundSeconds == rec.foregroundSeconds;
		}

		@Override
		public String toString() {
			return saveToString();
		}
	}

	public static class VersionInfoOperator {
		
		public static String saveToString(Message_VersionInfo versionInfo) {
			return String.format("%s,%s,%s,%s", 
				versionInfo.number, 
				versionInfo.channel, 
				versionInfo.osVersion, 
				versionInfo.androidVersion);
		}

		public static Message_VersionInfo createFromString(String s) {
			if (!TextUtils.isEmpty(s)) {
				String[] strings = s.split(",");
				if (strings.length == 4) {
					return Message_VersionInfo.create(strings[0], strings[1]);
				}
			}
			return null;
		}
	}

	private Message_VersionInfo versionInfo;
	//	private static final boolean LOG = false;
	private static final String TAG = "ConfigManager";

	private final Wrapper wrapper;

	private boolean showFloatWndInGame = true; 		// 是否显示游戏内悬浮窗
	private boolean firstLaunchApp = true;
	private boolean firstLaunchGame = true;
	private boolean blockBackKeyPrompt = true;	//VPN关闭时，按back键退出应用时，是否需要提示
	private boolean firstStartVpn = true;
	private UrlConfig.ServerType serverType;	// 使用正式服务还是测试服务器
	private int logLevel;						// 日志级别
	private boolean activation;					// 用户是否为已激活用户（是否已经上报过相关事件？）
	//	private boolean vpnOpened;					// VPN开关时，记录在本地，供下一次启动时判断是否自动开启
	private long accumulateAccelTimeSecond;//所有游戏累计加速时长
	private int lastDayOfToastGameWhenVpnClosed; // 上一次“在开启游戏时未开VPN弹TOAST”是哪天
	private int guidePageVersion;     //引导页当前版本号
	private int openFloatWindowHelpPageStatus;    //悬浮窗打开帮助界面显示状态
	private int lastRemindAccelTimeRange;//累计加速时长最后一次提醒区间\
	//	private boolean preventDisturb;//防清理开关状态
	//	private boolean reportRootinfo;//是否上报过root
	private boolean installedListReported; // 是否已上报过安装列表？
	private boolean neverShowNewFunctionPrompt; //是否提醒过新功能
	private boolean sendNoticeAccelResult;//单次游戏加速结果是否推送通知
	private boolean bootAutoAccel = true;//开机启动时开启加速并记住该选择
	//	private boolean openUserCenterPormpt;//是否打开用户中心引导

	/** 在登录状态下最近一次进入用户中心是哪天 */
	private int enterUserCenterDayOnLogin;
	/**
	 * Root模式加速 Root手机 默认值为开启 （2.0.0修改）
	 */
	private boolean rootMode;
	@Deprecated
	/** 是否免VPN模式提醒对话框 默认为需要 */
	private boolean needDialogDescRoot;
	//测试数据
	private int debugGameAccelTimeSenconds;//测试数据 游戏加速时长

	private boolean showCleanDrag; //展示拖入加速飞碟的气泡
	private boolean showCleanEnter; //展示拖入飞碟位置的气泡
	private boolean floatwindowButtonSettingGuide;//悬浮窗设置按钮显示引导
	private boolean showUsageStateHelpDialog;//显示“有权查看应用使用情况”引导
	private int debugPointHistoryRequest = -1;//测试数据 请求积分历史条数

	private int menuHelpClick;	// 帮助菜单被用户点击
	private int systemType; //系统类型

	private boolean callRemindGamePlaying;

	private int floatwindowMeasureType;//悬浮窗尺寸类型

	/** 为True表示小悬浮窗里要显示时延 */
	private boolean showDelayInFloatWindow;

	/** 小悬浮窗是否被点击或拖动过？ */
	private boolean floatWindowActivated;

	/** 当游戏在前台的时候，是否有过来电？ */
	private boolean phoneIncomingHappenedWhenGameForeground;

	private boolean hasClickedGameLaunchButton, hasCreatedShortcut, hasManuallyCreatedShortcut;

	private boolean hasOpenAccel;

	private boolean hasPromptReadContact;

	/** 上一次推送“在APP内开启游戏”通知的时刻 （UTC毫秒数） */
	private long timeOfNoticeOpenGameInside;

	/**
	 * 发送加速成就的通知的时间 （UTC毫秒数） 默认值 -1 不存储
	 */
	private long timeOfNoticeAccelAchieve = -1;
	/**
	 * 已经发送过加速成就的通知 默认值false
	 */
	private boolean alreadySendNoticeAccelAchieve = false;
	/**
	 * 已经打开过加速成就的通知 默认值false
	 */
	private boolean alreadyOpenNoticeAccelAchieve = false;
	/**
	 * 游戏使用成就超越的小伙伴百分比 默认值 -1
	 */
	private int gamePlayAchievePercent = -1;

	/**
	 * 主页帮助引导界面状态
	 */
	private int helpUIStatus;

	/**
	 * 公用标志字段
	 */
	private long commonFlag;

	/**
	 * 在主界面开启加速次数 默认值为0，不存储
	 */
	private int inMainOpenAccelCount = 0;

	/** 测试人员手工设置的测试节点 */
	private String debugNodeIP;

	/** 最近一次显示加速开启流程动画是哪一天？ */
	private int dayOfAccelProgressAni;
	
	/** 最近上报网络无法识别时间是哪一天？ */
	private int dayOfNetWorkUnknown;

	/**
	 * ver 40 1.5.2 激活 时间 (UTC毫秒值) 默认值为 -1
	 */
	private long timeInMillisOfActivateV40 = -1;
	/** 是否发送 过“查看应用使用情况” 模块 引导 通知 默认为false */
	private boolean isToSendUsageStateNotification = false;

	/** 用户选择的“悬浮窗模式” */
	private int floatWindowMode;

	/** 最近一次上报各个设置值是哪一天？ */
	private int dayOfSettingValueReport;

	/** 在主页面或快捷方式里启动游戏的次数 */
	private int startGameCount;

	/** 最近一天的累计加速时长 */
	private AccelTimeRecord accelTimeRecord;

	/** 最近一次上报Start消息的时刻（UTC毫秒数） v1.5.3 */
	private long lastTimeOfSubmitStartMessage;

	/** 最近一次上报悬浮窗被点击或拖动是哪一天？ */
	private int dayReportFloatWindowActivate;

	/** 最近一次申请手机验证码的时刻（UTC时间） */
	private long lastTimeRequestPhoneVerifyCode;

	/** 最近一次弹出“签到框”是哪一天？ */
	private int daySignDialogPopup;

	/** 不需保存 -- 加速模式是否自动切换过 */
	private boolean isAutoChangedAccelModel = false;

	private List<String> orderIds;

	/** 记录上次点击兑换时间毫秒 */
	private long lastTimestampMillisMarket;
	/** 发送兑换流量通知时刻 */
	private long sendNoticeExchangeFlowTimeInMillis;
	/** 重复发送兑换流量通知时刻 */
	private long sendNoticeRepeatExchangeFlowTimeInMillis;
	/** 进入兑换中心流量兑换界面的时间 */
	private long openExchangeFlowTimeInMillis;
	/** 新用户首次启动程序的时间 */
	private long newUserFristStartTime;

	private boolean useTestUmengKey;

	/** 最近一次上报并联加速开关状态的时刻（UTC毫秒数） */
	private long lastTimeOfSubmitDoubleAccel;
	
	/**并联加速流量改变事件的标志*/
	private long doubleAccelFlowEventFlag ;

	/** 未登录用户上一次提示天数 */
	private int  lastDayOfUnlogin;

	/** 普通加速上一次提示天数 */
	private int  lastDayOfNormalAceel;

	/** 最近一次提示VIP服务即将过期是哪一天？ */
	private int dayOfRemindVIPWillBeExpired;

	private static final String KEY_SHOW_FLOAT_WND_IN_GAME = "show_float_wnd_in_game";
	private static final String KEY_FIRST_LAUNCH_APP = "first_launch_app";
	private static final String KEY_FIRST_LAUNCH_GAME = "first_launch_game";
	private static final String KEY_SERVER_TYPE = "server_type";
	private static final String KEY_LOG_LEVEL = "log_level_2";
	private static final String KEY_BLOCK_BACK_KEY_PROMPT = "block_back_key_prompt";
	private static final String KEY_FIRST_START_VPN = "first_start_vpn";
	private static final String KEY_ACTIVATION = "activation";
	private static final String KEY_ACCUMULATE_ACCEL_TIME_SECOND = "accumulate_accel_time_second";
	private static final String KEY_LAST_DAY_TOAST_GAME_WHEN_VPN_CLOSED = "last_day_toast_game_when_vpn_closed";
	private static final String KEY_GUIDE_PAGE_VERSION = "guid_page_version";
	private static final String KEY_OPEN_FLOAT_WINDOW_HELPPAGE_STATUS = "open_float_window_help_page_count";
	private static final String LAST_REMIND_ACCELTIME_RANGE = "last_remind_acceltime_range";
	private static final String KEY_INSTALLED_LIST_REPORTED = "installed_list_reported";
	private static final String KEY_NEVER_SHOW_NEW_FUNCTION_PROMPT = "never_show_new_function_prompt";
	private static final String KEY_SEND_NOTICE_ACCELR_ESULT = "key_send_notice_accelr_esult";//单次游戏加速推送通知
	private static final String KEY_SHOW_CLEAN_DRAG = "key_show_clean_drag";
	private static final String KEY_SHOW_CLEAN_ENTER = "key_show_clean_enter";
	private static final String KEY_ROOT_MODE = "key_root_mode";
	private static final String KEY_DIALOGDESC_ROOT = "KEY_DIALOGDESC_ROOT";
	private static final String KEY_MENU_HELP_CLICK = "menu_help_click";
	private static final String KEY_MOBILE_SYSTEM_TYPE = "mobile_system_type";
	private static final String KEY_CALL_REMIND_GAMEPLAYING = "callRemindGamePlaying";
	//	private static final String KEY_SHOW_SCREENSHOT_PORMPT = "showScreenshotPormpt";
	private static final String KEY_FLOATWINDOW_MEASURE_TYPE = "floatwindowMeasureType";
	private static final String KEY_BOOT_AUTO_ACCEL = "bootAutoAccel_2_1_2";
	private static final String KEY_FLOAT_WINDOW_ACTIVATED = "float_window_activated";
	private static final String KEY_PHONE_INCOMING_HAPPENED = "phone_incoming_happened";
	private static final String KEY_FLOATWINDOW_BUTTON_SETTING_GUIDE = "floatwindow_buttonsetting_Guide";
	private static final String KEY_HAS_CLICKED_GAME_LAUNCH = "has_clicked_game_launch_button";
	private static final String KEY_START_GAME_IN_MAIN_PAGE_COUNT = "start_game_num";
	private static final String KEY_HAS_CREATED_SHORTCUT = "has_created_shortcut";
	private static final String KEY_HAS_MANUALLY_CREATE_SHORTCUT = "has_manually_created_shortcut";
	private static final String KEY_HAS_OPEN_ACCEL = "has_open_accel";
	private static final String KEY_HAS_PROMPT_READ_CONTACTS = "has_prompt_read_contacts";
	//	private static final String KEY_ALREADY_DIALOG_PROMPT_WHEN_FIRST_ACCEL = "already_dialog_prompt_when_first_accel";
	private static final String KEY_HELP_UI_STATUS = "help_ui";
	private static final String KEY_NOTICE_OPEN_GAME_INSIDE_TIME = "notice_open_game_insidie_time";
	private static final String KEY_FLOATWINDOW_SWITCH_DELAY = "floatwindow_switch_delay";
	private static final String KEY_ALREADY_SEND_NOTICE_ACCEL_ACHIEVE = "already_send_notice_accel_achieve";
	private static final String KEY_ALREADY_OPEN_NOTICE_ACCEL_ACHIEVE = "already_open_notice_accel_achieve";
	private static final String KEY_GAME_PLAY_ACHIEVE_PERCENT = "game_play_achieve_percent";
	private static final String KEY_DEBUG_NODE_IP = "debug_node_ip";
	private static final String KEY_DAY_ACCEL_PROGRESS_ANI = "day_accel_progress_ani";
	private static final String KEY_SHOW_USAGE_STATE_HELP_DIALOG = "showUsageStateHelpDialog";
	private static final String KEY_TIME_INMILLIS_OF_ACTIVATE_V40 = "timeInMillisOfActivateV40";
	private static final String KEY_TO_SEND_USAGE_STATE_NOTIFICATION = "toSendUsageStateNotification";
	private static final String KEY_FLOAT_WINDOW_MODE = "floatwindow_mode";
	private static final String KEY_DAY_OF_SETTING_VALUE_REPORT_DAY = "day_setting_value_report";
	private static final String KEY_ACCEL_TIME_RECORD = "acceL-time_record";
	private static final String KEY_VERSION_INFO = "version_info";
	private static final String KEY_LAST_TIME_OF_SUBMIT_START_MESSAGE = "last_time_submit_start_message";
	private static final String KEY_DAY_REPORT_FLOAT_WINDOW_ACTIVATE = "day_report_float_window_activate";
	private static final String KEY_DAY_SIGN_DIALOG_POPUP = "day_sign_dlg";
	private static final String KEY_DAY_ENTER_USER_CENTER_ON_LOGIN = "day_enter_user_center_on_login";

	private static final String KEY_LAST_TIME_REQUEST_PHONE_VERIFY_CODE = "ltrpvc";
	private static final String KEY_LAST_TIME_STAMP_MARKET = "timeMarket";
	private static final String KEY_ORDER_IDS = "key_order_ids";
	private static final String KEY_SEND_NOTICE_EXCHANGE_FLOW_TIMEINMILLIS = "send_notice_exchange_flow_timeinmillis";
	private static final String KEY_SEND_NOTICE_REPEAT_EXCHANGE_FLOW_TIMEINMILLIS = "send_notice_repeat_exchange_flow_timeinmillis";
	private static final String KEY_OPEN_EXCHANGE_FLOW_TIMEINMILLIS = "open_exchange_flow_timeinmillis";

	private static final String KEY_NEW_USER_FIRST_START_TIME = "first_start_time";

	private static final String KEY_USE_TEST_UMENG_KEY = "use_test_umkey";

	private static final String KEY_LAST_TIME_OF_SUBMIT_DOUBLE_ACCEL = "last_time_submit_double_accel";
	/** 公用标志字段的KEY */
	private static final String KEY_COMMON_FLAG = "common_flag";
	/** 并联加速流量改变事件标志的KEY */
	private static final String KEY_DOUBLE_ACCEL_FLOW_EVENT_FLAG = "double_accel_flow_event_flag";
	/** 上报网络无法识别事件的日期 */
	private static final String KEY_DAY_NETWORK_UNKNOWN = "day_network_unknown";

	private static final String KEY_LAST_DAY_OF_UNLOGIN = "LAST_DAY_OF_UNLOGIN";

	private static final String KEY_LAST_DAY_OF_NORMAL_ACCEL = "LAST_DAY_OF_NORMAL_ACCEL";

	private static final String KEY_DAY_OF_REMIND_VIP_WILL_EXPIRED = "day_of_remind_vip_will_expired";

	/** 公用标志字段的位掩码：1.4.8版的“常见问题”菜单是否已点击过 */
	private static final long COMMON_FLAG_MASK_MENU_QA_CLICKED_1_4_8 = 1;

	/** 公用标志字段的位掩码：1.4.8版的游戏内大悬浮窗的设置按钮是否已点击过 */
	private static final long COMMON_FLAG_MASK_BOXINGAME_SETTING_CLICKED_1_4_8 = 2;

	//	/** 公用标志字段的位掩码：是否在TaskManager里强制使用Ver5的策略 
	//	 * v1.5.2废弃 */
	//	@SuppressWarnings("unused")
	//	@Deprecated
	//	private static final long COMMON_FLAG_MASK_USE_VER5_STRATEGY = 4;

	/** 公用标志字段的位掩码：是否在TaskManager里强制使用Ver5的策略 */
	private static final long COMMON_FLAG_MASK_SEND_NOTICE_AUTO_CLEAN = 8;

	private static final long COMMON_FLAG_MASK_SET_PROCESS_CLEAN = 1 << 4;

	private static final long COMMON_FLAG_MASK_SET_QUESTION_SUVERY = 1 << 5;

	/**
	 * 公用标志字段的位掩码：是否问卷调查已经通知
	 * <p>
	 * <b>请保留下面的声明，提醒后面写代码的不要忘记(1<<6)这个位已占用</b>
	 * </p>
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private static final long COMMON_FLAG_MASK_SET_QUESTION_SUVERY_NOTIFY = 1 << 6;

	/** 该标志位记录是否向新用户发送过通知 */
	//private static final long COMMON_FLAG_HAS_SEND_NOTICE_TO_NEW_USER = 1 << 7;

	/** 该标志位记录用户是否进入过用户中心 */
	//private static final long COMMON_FLAG_HAS_TURNED_USER_CNETER = 1 << 8;
	/** 该标志位是否开启并联加速 */
	private static final long COMMON_FLAG_HAS_DOUBLE_ACCEL = 1 << 9;

	/** 该标志位记录是否为JPush设置过标签 */
	private static final long COMMON_FLAG_HAS_SET_TAT_FOR_JPUSH = 1 << 10;

	/** 是否首次进入并联加速页 */
	private static final long COMMON_FLAG_HAS_DOUBLE_LINK_FIRST = 1 << 11;

	/** 是否首次进入并联加速页 */
	private static final long COMMON_FLAG_MASK_BOXINGAME_SETTING_DOUBLE_ACCEL = 1 << 12;

	/** 极光sdk是否询问过相关权限 */
	private static final long COMMON_FLAG_IS_REQUEST_SDK23W_PERMISSION_FOR_JPUSH = 1 << 13;

	/** debug并联加速机型判断 */
	private static final long COMMON_FLAG_DEBUG_DOUBLE_LINK_MODEL = 1 << 14;

    /** 该标志位是否首次开启并联加速 */
    private static final long COMMON_FLAG_HAS_ONLY_ONE_DOUBLE_ACCEL = 1 << 15;

    /** 该标志位是否统计过并联加速用户反馈 */
    private static final long COMMON_FLAG_HAS_USER_FEEDBACK_DOUBLE_ACCEL = 1 << 16;
    
    /** 该标志位表示是否显示过注册或充值的提示页面*/
    private static final long COMMON_FLAG_HAS_REMIND_REGIST_RECHARGE = 1 << 17;
    
    /** 该标志位表示是否需要弹窗提示绑定手机号*/
    private static final long COMMON_FLAG_NEED_REMIND_BOUND_PHONE = 1 << 18;
    
    /** 该标志位表示是否需要弹窗提示注册或登录*/
    private static final long COMMON_FLAG_NEED_REMIND_LOGIN_REGIST = 1 << 19;
    
    /** 该标志位表示是否需要弹窗提示续费*/
    private static final long COMMON_FLAG_NEED_REMIND_RENEW = 1 << 20;

	/** 该标志位表示是否第一次开始加速*/
	private static final long COMMON_FLAG_FIRST_START_ACCEL = 1 << 21;

	/** 该标志位表示是否第一次开始加速*/
	private static final long COMMON_FLAG_GUIDE_PAGE = 1 << 22;

	/** 该标志位表示是否第一次开始加速*/
	private static final long COMMON_FLAG_FIRST_PROMPT_LOGIN = 1 << 23;
    
    /** 标志位，是否上报了事件双链路使用了数据流量累计>0M */
    public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_NULL0 = 1 ;
    /** 标志位，是否上报了事件双链路使用了数据流量累计>5M */
    public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_5M = 2;
    /** 标志位，是否上报了事件双链路使用了数据流量累计>20M */
    public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_20M = 4;
    /** 标志位，是否上报了事件双链路使用了数据流量累计>50M */
    public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_50M = 8;
    /** 标志位，是否上报了事件双链路使用了数据流量累计>150M */
    public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_150M = 1 << 4;

	/** 标志位，是否上报了事件双链路使用了 tcp 数据流量累计>0M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_NULL0 = 1 << 5;

	/** 标志位，是否上报了事件双链路使用了 tcp 数据流量累计>5M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_5M = 1 << 6;
	/** 标志位，是否上报了事件双链路使用了tcp数据流量累计>20M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_20M = 1 << 7;

	/** 标志位，是否上报了事件双链路使用了tcp数据流量累计>50M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_50M = 1 << 8;

	/** 标志位，是否上报了事件双链路使用了tcp数据流量累计>150M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_150M = 1 << 9;

	/** 标志位，是否上报了事件双链路使用了udp数据流量累计>0M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_NULL0 = 1 << 10;

	/** 标志位，是否上报了事件双链路使用了udp数据流量累计>5M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_5M = 1 << 11;

	/** 标志位，是否上报了事件双链路使用了udp数据流量累计>20M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_20M = 1 << 12;

	/** 标志位，是否上报了事件双链路使用了udp数据流量累计>50M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_50M = 1 << 13;

	/** 标志位，是否上报了事件双链路使用了udp数据流量累计>150M */
	public static final long DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_150M = 1 << 14;

	private static ConfigManager instance;

	public static ConfigManager createInstance(Context context) {
		if (instance == null) {
			instance = new ConfigManager(context);
		}
		return instance;
	}

	public static ConfigManager getInstance() {
		if (GlobalDefines.CHECK_MAIN_THREAD) {
			long tid = Thread.currentThread().getId();
			if (tid != ThreadUtils.getAndroidUIThreadId()) {
				Log.e(TAG, String.format("call ConfigManager.getInstance() in thread:%d %s error !!!!!!!!!!!!!!", tid, Thread.currentThread().getName()));
			}
		}
		return instance;
	}

	/**
	 * 因为老版本Config文件里可能与新版本Config文件里数据类型不一致，比如老版本某字段是int型，但新版本的程序代码改成long型去读，
	 * 会出现异常。 故设计此包装类，简化读写时的判断，增加容错性。
	 */
	private static class Wrapper {
		private static final String STORE_NAME = "settings";
		private final SharedPreferences sharedPreferences;

		public Wrapper(Context context) {
			this.sharedPreferences = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE);
		}

		public synchronized boolean contains(String key) {
			return this.sharedPreferences.contains(key);
		}

		public synchronized void clear() {
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.clear();
			editor.apply();
		}

		public synchronized boolean getBoolean(String key, boolean defValue) {
			try {
				return sharedPreferences.getBoolean(key, defValue);
			} catch (ClassCastException e) {
				return defValue;
			}
		}

		public synchronized int getInt(String key, int defValue) {
			int result = defValue;
			try {
				result = sharedPreferences.getInt(key, defValue);
			} catch (ClassCastException e1) {
				try {
					result = (int) sharedPreferences.getLong(key, defValue);
				} catch (ClassCastException e2) {}
			}
			return result;
		}

		public synchronized long getLong(String key, long defValue) {
			long result = defValue;
			try {
				result = sharedPreferences.getLong(key, defValue);
			} catch (ClassCastException e1) {
				try {
					result = sharedPreferences.getInt(key, (int) defValue);
				} catch (ClassCastException e2) {}
			}
			return result;
		}

		public synchronized String getString(String key, String defValue) {
			try {
				return sharedPreferences.getString(key, defValue);
			} catch (ClassCastException e) {
				return defValue;
			}
		}

		///////////////////////////////////////////////

		public synchronized void setValue(String key, Object value) {
			Editor editor = this.sharedPreferences.edit();
			if (value == null) {
				editor.remove(key);
			} else if (value instanceof Boolean) {
				editor.putBoolean(key, ((Boolean) value).booleanValue());
			} else if (value instanceof Integer) {
				editor.putInt(key, ((Integer) value).intValue());
			} else if (value instanceof String) {
				editor.putString(key, value.toString());
			} else if (value instanceof Long) {
				editor.putLong(key, ((Long) value).longValue());
			} else {
				throw new RuntimeException(String.format("setValue(\"%s\", %s) not implemented", key, value.getClass().getName()));
			}
			editor.apply();
		}
	}

	private ConfigManager(Context context) {
		wrapper = new Wrapper(context.getApplicationContext());
		showFloatWndInGame = wrapper.getBoolean(KEY_SHOW_FLOAT_WND_IN_GAME, true);
		firstLaunchApp = wrapper.getBoolean(KEY_FIRST_LAUNCH_APP, true);
		firstLaunchGame = wrapper.getBoolean(KEY_FIRST_LAUNCH_GAME, true);
		serverType = (wrapper.getInt(KEY_SERVER_TYPE, 0) == 0) ? UrlConfig.ServerType.NORMAL : UrlConfig.ServerType.TEST;
		logLevel = wrapper.getInt(KEY_LOG_LEVEL, 0); //// FIXME: 17-3-29 hujd
		blockBackKeyPrompt = wrapper.getBoolean(KEY_BLOCK_BACK_KEY_PROMPT, true);
		firstStartVpn = wrapper.getBoolean(KEY_FIRST_START_VPN, true);
		activation = wrapper.getBoolean(KEY_ACTIVATION, false);
		accumulateAccelTimeSecond = wrapper.getLong(KEY_ACCUMULATE_ACCEL_TIME_SECOND, 0);
		lastDayOfToastGameWhenVpnClosed = wrapper.getInt(KEY_LAST_DAY_TOAST_GAME_WHEN_VPN_CLOSED, -1);
		guidePageVersion = wrapper.getInt(KEY_GUIDE_PAGE_VERSION, 0);
		openFloatWindowHelpPageStatus = wrapper.getInt(KEY_OPEN_FLOAT_WINDOW_HELPPAGE_STATUS, 0);
		lastRemindAccelTimeRange = wrapper.getInt(LAST_REMIND_ACCELTIME_RANGE, 0);
		installedListReported = wrapper.getBoolean(KEY_INSTALLED_LIST_REPORTED, false);
		neverShowNewFunctionPrompt = wrapper.getBoolean(KEY_NEVER_SHOW_NEW_FUNCTION_PROMPT, true);
		sendNoticeAccelResult = wrapper.getBoolean(KEY_SEND_NOTICE_ACCELR_ESULT, true);//默认为true
		showCleanDrag = wrapper.getBoolean(KEY_SHOW_CLEAN_DRAG, true);
		showCleanEnter = wrapper.getBoolean(KEY_SHOW_CLEAN_ENTER, true);
		/*if (wrapper.contains(KEY_ROOT_MODE)) {
			rootMode = wrapper.getBoolean(KEY_ROOT_MODE, false);
		} else {
		    rootMode = new RootOpener().hasModel();
		}*/
		needDialogDescRoot = wrapper.getBoolean(KEY_DIALOGDESC_ROOT, true);
		this.menuHelpClick = wrapper.getInt(KEY_MENU_HELP_CLICK, 0);
		systemType = wrapper.getInt(KEY_MOBILE_SYSTEM_TYPE, -1);
		floatwindowMeasureType = wrapper.getInt(KEY_FLOATWINDOW_MEASURE_TYPE, FloatWindowMeasure.Type.NORMAL.ordinal());
		this.bootAutoAccel = wrapper.getBoolean(KEY_BOOT_AUTO_ACCEL, true);
		this.floatwindowButtonSettingGuide = wrapper.getBoolean(KEY_FLOATWINDOW_BUTTON_SETTING_GUIDE, true);
		if (UIUtils.isCallRemindSupportCurrentRom()) {
			this.callRemindGamePlaying = wrapper.getBoolean(KEY_CALL_REMIND_GAMEPLAYING, true);
		}
		this.floatWindowActivated = wrapper.getBoolean(KEY_FLOAT_WINDOW_ACTIVATED, false);
		this.phoneIncomingHappenedWhenGameForeground = wrapper.getBoolean(KEY_PHONE_INCOMING_HAPPENED, false);
		this.hasClickedGameLaunchButton = wrapper.getBoolean(KEY_HAS_CLICKED_GAME_LAUNCH, false);
		this.startGameCount = wrapper.getInt(KEY_START_GAME_IN_MAIN_PAGE_COUNT, 0);
		this.hasCreatedShortcut = wrapper.getBoolean(KEY_HAS_CREATED_SHORTCUT, false);
		this.hasManuallyCreatedShortcut = wrapper.getBoolean(KEY_HAS_MANUALLY_CREATE_SHORTCUT, false);
		this.hasOpenAccel = wrapper.getBoolean(KEY_HAS_OPEN_ACCEL, false);
		this.hasPromptReadContact = wrapper.getBoolean(KEY_HAS_PROMPT_READ_CONTACTS, false);
		this.helpUIStatus = wrapper.getInt(KEY_HELP_UI_STATUS, 0);
		this.timeOfNoticeOpenGameInside = wrapper.getLong(KEY_NOTICE_OPEN_GAME_INSIDE_TIME, 0);
		this.showDelayInFloatWindow = wrapper.getBoolean(KEY_FLOATWINDOW_SWITCH_DELAY, true);
		this.commonFlag = wrapper.getLong(KEY_COMMON_FLAG, COMMON_FLAG_HAS_DOUBLE_ACCEL);	// 并联加速开关默认开
		this.alreadySendNoticeAccelAchieve = wrapper.getBoolean(KEY_ALREADY_SEND_NOTICE_ACCEL_ACHIEVE, alreadySendNoticeAccelAchieve);
		this.alreadyOpenNoticeAccelAchieve = wrapper.getBoolean(KEY_ALREADY_OPEN_NOTICE_ACCEL_ACHIEVE, alreadyOpenNoticeAccelAchieve);
		this.gamePlayAchievePercent = wrapper.getInt(KEY_GAME_PLAY_ACHIEVE_PERCENT, -1);
		this.debugNodeIP = wrapper.getString(KEY_DEBUG_NODE_IP, null);
		this.dayOfAccelProgressAni = wrapper.getInt(KEY_DAY_ACCEL_PROGRESS_ANI, 0);
		this.showUsageStateHelpDialog = wrapper.getBoolean(KEY_SHOW_USAGE_STATE_HELP_DIALOG, true);
		this.timeInMillisOfActivateV40 = wrapper.getLong(KEY_TIME_INMILLIS_OF_ACTIVATE_V40, -1);
		this.isToSendUsageStateNotification = wrapper.getBoolean(KEY_TO_SEND_USAGE_STATE_NOTIFICATION, isToSendUsageStateNotification);
		this.floatWindowMode = wrapper.getInt(KEY_FLOAT_WINDOW_MODE, 0);
		this.dayOfSettingValueReport = wrapper.getInt(KEY_DAY_OF_SETTING_VALUE_REPORT_DAY, 0);
		this.accelTimeRecord = AccelTimeRecord.createFromString(wrapper.getString(KEY_ACCEL_TIME_RECORD, null));
		this.versionInfo = VersionInfoOperator.createFromString(wrapper.getString(KEY_VERSION_INFO, null));
		this.lastTimeOfSubmitStartMessage = wrapper.getLong(KEY_LAST_TIME_OF_SUBMIT_START_MESSAGE, 0);
		this.dayReportFloatWindowActivate = wrapper.getInt(KEY_DAY_REPORT_FLOAT_WINDOW_ACTIVATE, 0);
		this.lastTimeRequestPhoneVerifyCode = wrapper.getLong(KEY_LAST_TIME_REQUEST_PHONE_VERIFY_CODE, -1);
		this.daySignDialogPopup = wrapper.getInt(KEY_DAY_SIGN_DIALOG_POPUP, 0);
		this.orderIds = cn.wsds.gamemaster.tools.StringUtils.split(wrapper.getString(KEY_ORDER_IDS, null));
		this.lastTimestampMillisMarket = wrapper.getLong(KEY_LAST_TIME_STAMP_MARKET, 0);
		this.enterUserCenterDayOnLogin = wrapper.getInt(KEY_DAY_ENTER_USER_CENTER_ON_LOGIN, -1);
		this.autoCleanProcessInternal = wrapper.getInt(KEY_AUTO_CLEAN_PROCESS_INTERNAL, 0);
		this.sendNoticeExchangeFlowTimeInMillis = wrapper.getLong(KEY_SEND_NOTICE_EXCHANGE_FLOW_TIMEINMILLIS, 0);
		this.sendNoticeRepeatExchangeFlowTimeInMillis = wrapper.getLong(KEY_SEND_NOTICE_REPEAT_EXCHANGE_FLOW_TIMEINMILLIS, 0);
		this.openExchangeFlowTimeInMillis = wrapper.getLong(KEY_OPEN_EXCHANGE_FLOW_TIMEINMILLIS, 0);
		this.newUserFristStartTime = wrapper.getLong(KEY_NEW_USER_FIRST_START_TIME, 0);
		this.useTestUmengKey = wrapper.getBoolean(KEY_USE_TEST_UMENG_KEY, false);
		this.lastTimeOfSubmitDoubleAccel = wrapper.getLong(KEY_LAST_TIME_OF_SUBMIT_DOUBLE_ACCEL, 0);
		this.lastFlowOfDoubleAccel = wrapper.getLong(KEY_LAST_FLOW_OF_DOUBLE_ACCEL, 0);
		this.lastUdpFlowOfDoubleAccel = wrapper.getLong(KEY_LAST_UDP_FLOW_OF_DOUBLE_ACCEL, 0);
		this.lastTcpFlowOfDoubleAccel = wrapper.getLong(KEY_LAST_TCP_FLOW_OF_DOUBLE_ACCEL, 0);
		this.doubleAccelFlowEventFlag = wrapper.getLong(KEY_DOUBLE_ACCEL_FLOW_EVENT_FLAG, 0);
		this.dayOfNetWorkUnknown = wrapper.getInt(KEY_DAY_NETWORK_UNKNOWN, 0);
		setCommonFlagBit(COMMON_FLAG_NEED_REMIND_RENEW, true);
		setCommonFlagBit(COMMON_FLAG_NEED_REMIND_LOGIN_REGIST, true);
		setCommonFlagBit(COMMON_FLAG_NEED_REMIND_BOUND_PHONE, true);
		setFirstStrtAccel(true);
		this.lastDayOfUnlogin = wrapper.getInt(KEY_LAST_DAY_OF_UNLOGIN, 0);
		this.lastDayOfNormalAceel = wrapper.getInt(KEY_LAST_DAY_OF_NORMAL_ACCEL, 0);
		this.dayOfRemindVIPWillBeExpired = wrapper.getInt(KEY_DAY_OF_REMIND_VIP_WILL_EXPIRED, 0);
		setFirstPromptLogin(true);
		this.lastDayOfTcpFlow = wrapper.getInt(KEY_LAST_DAY_OF_TCP_FLOW, 0);
		this.lastDayOfUdpFlow = wrapper.getInt(KEY_LAST_DAY_OF_TCP_FLOW, 0);
		this.lastAccelTcpFlow = wrapper.getLong(KEY_LAST_ACEEL_TCP_FLOW, 0);
		this.lastAccelUdpFlow = wrapper.getLong(KEY_LAST_ACEEL_UDP_FLOW, 0);
	}

	/**
	 * 初始化设置（配置）
	 */
	public void clearConfig() {
		this.wrapper.clear();
	}

	/** 设置：是否显示游戏内悬浮窗 */
	public void setShowFloatWindowInGame(boolean show) {
		if (this.showFloatWndInGame != show) {
			showFloatWndInGame = show;
			this.wrapper.setValue(KEY_SHOW_FLOAT_WND_IN_GAME, show);
			//
			List<Observer> observers = this.cloneAllObservers();
			if (observers != null) {
				for (Observer o : observers) {
					o.onFloatWindowSwitchChange(show);
				}
			}
		}
	}

	/** 获取设置：是否显示游戏内悬浮窗 */
	public boolean getShowFloatWindowInGame() {
		return this.showFloatWndInGame;
	}

	/** 设置：是否首次启动App */
	public void setFirstLaunchApp(boolean value) {
		if (this.firstLaunchApp != value) {
			firstLaunchApp = value;
			this.wrapper.setValue(KEY_FIRST_LAUNCH_APP, value);
		}
	}

	/** 获取：是否首次启动APP */
	public boolean getFirstLaunchApp() {
		return this.firstLaunchApp;
	}

	/** 设置：是否首次在应用内启动一个游戏 */
	public void setFirstLaunchGame(boolean value) {
		if (this.firstLaunchGame != value) {
			firstLaunchGame = value;
			this.wrapper.setValue(KEY_FIRST_LAUNCH_GAME, value);
		}
	}

	/** 获取：是否首次在应用内启动一个游戏 */
	public boolean getFirstLaunchGame() {
		return this.firstLaunchGame;
	}

	/** 设置：用户按Back键退出主页面时如果未开VPN，是否弹框提示？ */
	public void setBlockBackKeyPrompt(boolean value) {
		if (this.blockBackKeyPrompt != value) {
			blockBackKeyPrompt = value;
			this.wrapper.setValue(KEY_BLOCK_BACK_KEY_PROMPT, value);
		}
	}

	/** 获取：用户按Back键退出主页面时如果未开VPN，是否弹框提示？ */
	public boolean getBlockBackKeyPrompt() {
		return this.blockBackKeyPrompt;
	}

	/** 设置：是每一次开启VPN吗？ */
	public void setFirstStartVpn(boolean value) {
		if (this.firstStartVpn != value) {
			firstStartVpn = value;
			this.wrapper.setValue(KEY_FIRST_START_VPN, value);
		}
	}

	/** 获取：是每一次开启VPN吗？ */
	public boolean getFirstStartVpn() {
		return firstStartVpn;
	}

	/** 设置：使用正式服还是测试服 （Debug页面） */
	public void setServerType(UrlConfig.ServerType type) {
		if (this.serverType != type) {
			this.serverType = type;
			this.wrapper.setValue(KEY_SERVER_TYPE, (type == UrlConfig.ServerType.NORMAL) ? 0 : 1);
		}
	}

	/** 获取：使用正式服还是测试服 */
	public UrlConfig.ServerType getServerType() {
		return this.serverType;
	}

	/** 获取：用户已经是激活用户了吗（是否已经上报过相关事件？） */
	public boolean isActivation() {
		return activation;
	}

	/** 设置：用户是否已经是激活用户了（是否已经上报过相关事件？） */
	public void setActivation(boolean activation) {
		if (this.activation != activation) {
			this.activation = activation;
			this.wrapper.setValue(KEY_ACTIVATION, activation);
		}
	}

	/** 设置日志级别 */
	public void setLogLevel(int level) {
		if (this.logLevel != level) {
			this.logLevel = level;
			this.wrapper.setValue(KEY_LOG_LEVEL, level);
		}
	}

	/** 取当前日志级别 */
	public int getLogLevel() {
		return this.logLevel;
	}

	/**
	 * 获得所有游戏累计加速时长总和
	 */
	public long getAccumulateAccelTimeSecond() {
		return accumulateAccelTimeSecond;
	}

	/**
	 * 所有游戏累计加速时长
	 * 
	 * @param accumulateAccelTimeSecond
	 *            单位秒
	 */
	public void setAccumulateAccelTimeSecond(long accumulateAccelTimeSecond) {
		if (this.accumulateAccelTimeSecond != accumulateAccelTimeSecond) {
			this.accumulateAccelTimeSecond = accumulateAccelTimeSecond;
			this.wrapper.setValue(KEY_ACCUMULATE_ACCEL_TIME_SECOND, accumulateAccelTimeSecond);
		}
	}

	/**
	 * 获取记录的当前天数
	 * 
	 * @return
	 */
	public int getLastDayToastGameWhenVpnClosed() {
		return lastDayOfToastGameWhenVpnClosed;
	}

	/**
	 * 设置当前时间
	 */
	public void setLastDayToastGameWhenVpnClosed(int day) {
		if (this.lastDayOfToastGameWhenVpnClosed != day) {
			this.lastDayOfToastGameWhenVpnClosed = day;
			this.wrapper.setValue(KEY_LAST_DAY_TOAST_GAME_WHEN_VPN_CLOSED, day);
		}
	}

	/**
	 * 获取引导页版本号
	 */
	public int getGuidePageVersion() {
		return guidePageVersion;
	}

	/**
	 * 设置引导页版本号
	 */
	public void setGuidePageVersion(int version) {
		if (guidePageVersion != version) {
			guidePageVersion = version;
			this.wrapper.setValue(KEY_GUIDE_PAGE_VERSION, version);
		}
	}

	/**
	 * 获取悬浮窗帮助界面(启动次数)计数
	 */
	public int getOpenFloatWindowHelpPageCount() {
		return openFloatWindowHelpPageStatus;
	}

	/**
	 * 设置悬浮窗帮助界面的状态
	 * 
	 * @param status
	 *            0 等待一次 1 启用 2 不启用
	 */
	public void setOpenFloatWindowHelpPageCount(int status) {
		if (openFloatWindowHelpPageStatus != status) {
			openFloatWindowHelpPageStatus = status;
			this.wrapper.setValue(KEY_OPEN_FLOAT_WINDOW_HELPPAGE_STATUS, status);
		}
	}

	public int getLastRemindAccelTimeRange() {
		return lastRemindAccelTimeRange;
	}

	public void setLastRemindAccelTimeRange(int range) {
		if (this.lastRemindAccelTimeRange != range) {
			this.lastRemindAccelTimeRange = range;
			this.wrapper.setValue(LAST_REMIND_ACCELTIME_RANGE, range);
		}
	}

	/**
	 * 是否已经上报过安装列表？
	 */
	public boolean getInstalledListReported() {
		return this.installedListReported;
	}

	/**
	 * 设置：安装列表是否已上报过
	 * 
	 * @param value
	 */
	public void setInstalledListReported(boolean value) {
		if (this.installedListReported != value) {
			this.installedListReported = value;
			this.wrapper.setValue(KEY_INSTALLED_LIST_REPORTED, value);
		}
	}

	public void setNeverShowNewFunctionPrompt(boolean value) {
		if (this.neverShowNewFunctionPrompt != value) {
			this.neverShowNewFunctionPrompt = value;
			this.wrapper.setValue(KEY_NEVER_SHOW_NEW_FUNCTION_PROMPT, value);
		}
	}

	public boolean isNeverShowNewFunctionPrompt() {
		return this.neverShowNewFunctionPrompt;
	}

	/**
	 * 测试数据 游戏加速时长
	 * 
	 * @return 默认为0
	 */
	public int getDebugGameAccelTimeSenconds() {
		return debugGameAccelTimeSenconds;
	}

	public void setDebugGameAccelTimeSenconds(int debugGameAccelTimeSenconds) {
		this.debugGameAccelTimeSenconds = debugGameAccelTimeSenconds;
	}

	/**
	 * 设置单次游戏加速结果是否推送通知
	 * 
	 * @param checked
	 */
	public void setSendNoticeAccelResult(boolean checked) {
		if (this.sendNoticeAccelResult != checked) {
			this.sendNoticeAccelResult = checked;
			this.wrapper.setValue(KEY_SEND_NOTICE_ACCELR_ESULT, checked);
		}
	}

	/**
	 * 单次游戏加速结果是否推送通知
	 * 
	 * @return
	 */
	public boolean getSendNoticeAccelResult() {
		return this.sendNoticeAccelResult;
	}

	@Deprecated
	public void setShowCleanDrag(boolean value) {
		if (this.showCleanDrag != value) {
			this.showCleanDrag = value;
			this.wrapper.setValue(KEY_SHOW_CLEAN_DRAG, value);
		}
	}

	@Deprecated
	public boolean getShowCleanDrag() {
		return this.showCleanDrag;
	}

	public void setShowCleanEnter(boolean value) {
		if (this.showCleanEnter != value) {
			this.showCleanEnter = value;
			this.wrapper.setValue(KEY_SHOW_CLEAN_ENTER, value);
		}
	}

	public boolean getShowCleanEnter() {
		return this.showCleanEnter;
	}

	public void setRootMode(boolean rootMode) {
		if (this.rootMode == rootMode)
			return;
		//		StatisticDefault.addEvent(AppMain.getContext(), rootMode ? Event.SWITCH_ROOTMODE_ON : Event.SWITCH_ROOTMODE_OFF);
		this.rootMode = rootMode;
		this.wrapper.setValue(KEY_ROOT_MODE, rootMode);
	}

	public boolean isRootMode() {
		return rootMode;
	}

	/**
	 * root模式提醒对话框
	 * 
	 * @return 默认为 true 需要提醒 true 需要提醒，false 不需要提醒
	 */
	@Deprecated
	public boolean isNeedDialogDescRoot() {
		return needDialogDescRoot;
	}

	/**
	 * 记录不再需要打开免VPN提醒对话框
	 */
	@Deprecated
	public void setNotNeedDialogDescRoot() {
		if (!this.needDialogDescRoot) {
			return;
		}
		this.needDialogDescRoot = false;
		this.wrapper.setValue(KEY_DIALOGDESC_ROOT, false);
	}

	/**
	 * 获取帮助菜单被点击次数
	 */
	public int getMenuHelpClick() {
		return menuHelpClick;
	}

	/**
	 * 递增帮助菜单被点击次数
	 */
	public void incMenuHelpClick() {
		if (this.menuHelpClick == 0) {
			this.wrapper.setValue(KEY_MENU_HELP_CLICK, ++this.menuHelpClick);
		}
	}

	/**
	 * 获取系统类型
	 * 
	 * @return
	 */
	public int getSystemType() {
		return systemType;
	}

	/**
	 * 设置系统类型
	 * 
	 * @param type
	 */
	public void setSystemType(int type) {
		if (this.systemType == type)
			return;
		systemType = type;
		this.wrapper.setValue(KEY_MOBILE_SYSTEM_TYPE, type);
	}

	/**
	 * 设置：来电拦截开关 开或关
	 * 
	 * @param on
	 *            true表示开关打开（允许拦截）
	 */
	public void setCallRemindGamePlaying(boolean on) {
		if (this.callRemindGamePlaying == on)
			return;
		this.callRemindGamePlaying = on;
		this.wrapper.setValue(KEY_CALL_REMIND_GAMEPLAYING, on);
	}

	/**
	 * 返回设置：来电拦截开关是否开启？
	 * 
	 * @return true = 开启, false = 关闭
	 */
	public boolean getCallRemindGamePlaying() {
		return callRemindGamePlaying;
	}

	/**
	 * 设置悬浮窗尺寸（大中小）
	 * 
	 * @param type
	 * @return
	 */
	public boolean setFloatwindowMeasureType(int type) {
		if (this.floatwindowMeasureType == type) {
			return false;
		}
		this.floatwindowMeasureType = type;
		this.wrapper.setValue(KEY_FLOATWINDOW_MEASURE_TYPE, type);
		return true;
	}

	/**
	 * 获取悬浮窗尺寸（大中小）
	 * 
	 * @return {@link FloatWindowMeasure.Type} 的order值
	 */
	public int getFloatwindowMeasureType() {
		return this.floatwindowMeasureType;
	}

	public boolean getBootAutoAccel() {
		return this.bootAutoAccel;
	}

	public void setBootAutoAccel(boolean bootAutoAccel) {
		if (this.bootAutoAccel == bootAutoAccel)
			return;
		this.bootAutoAccel = bootAutoAccel;
		this.wrapper.setValue(KEY_BOOT_AUTO_ACCEL, bootAutoAccel);
	}

	/** 返回保存的值：小悬浮窗是否曾经被激活（点击或拖动）过？ */
	public boolean getFloatWindowActivated() {
		return this.floatWindowActivated;
	}

	/**
	 * 设置“小悬浮窗已经被激活过了”并保存
	 * 
	 * @return true表示前值为假，本次操作改变了前值并执行了保存
	 */
	public boolean setFloatWindowActivated() {
		if (!this.floatWindowActivated) {
			this.floatWindowActivated = true;
			this.wrapper.setValue(KEY_FLOAT_WINDOW_ACTIVATED, true);
			return true;
		}
		return false;
	}

	/** 获取保存的值：是否有来电呼入发生过 */
	public boolean getPhoneIncomingHappenedWhenGameForeground() {
		return this.phoneIncomingHappenedWhenGameForeground;
	}

	/** 设置“历史上有过来电呼入”并保存 */
	public void setPhoneIncomingHappenedWhenGameForeground() {
		if (!this.phoneIncomingHappenedWhenGameForeground) {
			this.phoneIncomingHappenedWhenGameForeground = true;
			this.wrapper.setValue(KEY_PHONE_INCOMING_HAPPENED, true);
		}
	}

	public void setNoFloatwindowButtonSettingGuide() {
		if (!this.floatwindowButtonSettingGuide) {
			return;
		}
		this.floatwindowButtonSettingGuide = false;
		this.wrapper.setValue(KEY_FLOATWINDOW_BUTTON_SETTING_GUIDE, false);
	}

	public boolean isFloatwindowButtonSettingGuide() {
		return floatwindowButtonSettingGuide;
	}

	/**
	 * 取：在主页面或快捷方式里点击“开始”启动游戏的次数
	 */
	public int getStartGameCount() {
		return this.startGameCount;
	}

	/**
	 * 设置：在主页面或快捷方式里点击“开始”启动游戏的次数
	 */
	public void setStartGameCount(int count) {
		if (count != this.startGameCount) {
			this.startGameCount = count;
			this.wrapper.setValue(KEY_START_GAME_IN_MAIN_PAGE_COUNT, count);
		}
	}

	public boolean isClickGameLaunchButton() {
		return this.hasClickedGameLaunchButton;
	}

	public void setClickGameLaunchButton() {
		if (!this.hasClickedGameLaunchButton) {
			this.hasClickedGameLaunchButton = true;
			this.wrapper.setValue(KEY_HAS_CLICKED_GAME_LAUNCH, true);
		}
	}

	public boolean isCreateShortcut() {
		return this.hasCreatedShortcut;
	}

	public void setCreateShortcut() {
		if (!this.hasCreatedShortcut) {
			this.hasCreatedShortcut = true;
			this.wrapper.setValue(KEY_HAS_CREATED_SHORTCUT, true);
		}
	}

	public boolean isManuallyCreatedShortcut() {
		return hasManuallyCreatedShortcut;
	}

	public void setManuallyCreatedShortcut() {
		if (!this.hasManuallyCreatedShortcut) {
			this.hasManuallyCreatedShortcut = true;
			this.wrapper.setValue(KEY_HAS_MANUALLY_CREATE_SHORTCUT, true);
		}
	}

	public boolean isOpenAccelBefore() {
		return this.hasOpenAccel;
	}

	public void setOpenAccel() {
		if (!this.hasOpenAccel) {
			this.hasOpenAccel = true;
			this.wrapper.setValue(KEY_HAS_OPEN_ACCEL, true);
		}
	}

	public boolean isPromptReadContacts() {
		return this.hasPromptReadContact;
	}

	public void setPormptReadContacts() {
		if (!this.hasPromptReadContact) {
			this.hasPromptReadContact = true;
			this.wrapper.setValue(KEY_HAS_PROMPT_READ_CONTACTS, true);
		}
	}

	/** 两段延迟的帮助引导显示过了（这个引导在1.4.7里取消了） */
	public static final int HELP_UI_STATUS_TWO_DELAY_SHOWN = 1;

	/** “在APK内启动游戏”引导显示过了 */
	public static final int HELP_UI_STATUS_START_GAME_INSIDE = 2;

	/** “用户中心”提示引导显示过了 */
	public static final int HELP_UI_STATUS_USER_CENTER_PORMPT = 1 << 2;

	/** “并联加速”提示引导显示过了 */
	public static final int HELP_UI_STATUS_DOUBLE_ACCEl_PORMPT = 1 << 3;

	/**
	 * 获取：帮助UI显示状态
	 */
	public int getHelpUIStatus() {
		return this.helpUIStatus;
	}

	/**
	 * 设置：帮助UI显示状态
	 */
	public void setMaskHelpUIStatus(int mask) {
		int v = helpUIStatus | mask;
		if (v != helpUIStatus) {
			this.helpUIStatus = v;
			this.wrapper.setValue(KEY_HELP_UI_STATUS, v);
		}
	}

	/**
	 * 返回最近一次显示“APP内开启游戏”通知的时刻（UTC毫秒数）
	 */
	public long getTimeOfNoticeOpenGameInside() {
		return this.timeOfNoticeOpenGameInside;
	}

	/**
	 * 设置最近一次显示“APP内开启游戏”通知的时刻（UTC毫秒数）
	 * 
	 * @param utcMilliseconds
	 */
	public void setTimeOfNoticeOpenGameInside(long utcMilliseconds) {
		if (this.timeOfNoticeOpenGameInside != utcMilliseconds) {
			this.timeOfNoticeOpenGameInside = utcMilliseconds;
			this.wrapper.setValue(KEY_NOTICE_OPEN_GAME_INSIDE_TIME, utcMilliseconds);
		}
	}

	public void setFloatwindowSwitchDelay(boolean showDelayInFloatWindow) {
		if (this.showDelayInFloatWindow == showDelayInFloatWindow) {
			return;
		}
		this.showDelayInFloatWindow = showDelayInFloatWindow;
		this.wrapper.setValue(KEY_FLOATWINDOW_SWITCH_DELAY, showDelayInFloatWindow);
		//
		List<Observer> observers = this.cloneAllObservers();
		if (observers != null) {
			for (Observer o : observers) {
				o.onShowDelayInFloatWindowChange(showDelayInFloatWindow);
			}
		}
	}

	public boolean getFloatwindowSwitchDelay() {
		return showDelayInFloatWindow;
	}

	private synchronized boolean setCommonFlagBit(long mask, boolean set) {	
		long value = setFlagBit(KEY_COMMON_FLAG,this.commonFlag,mask,set);	
		if(this.commonFlag!=value){
			this.commonFlag = value;
			return true ;
		}
		
		return false ;
	}
	
	private synchronized boolean setDoubleAccelFlowEventFlagBit(long mask, boolean set){		
		long value = setFlagBit(KEY_DOUBLE_ACCEL_FLOW_EVENT_FLAG,this.doubleAccelFlowEventFlag,mask,set);
		if(this.doubleAccelFlowEventFlag!=value){
			this.doubleAccelFlowEventFlag = value;
			return true ;
		}
		
		return false ;
	}
		
	private long setFlagBit(String key , long flagValue,long mask, boolean set) {
		long oldValue = flagValue;
		
		if (set) {
			flagValue |= mask;
		} else {
			flagValue &= ~mask;
		}
	
		if (oldValue != flagValue) {	
			this.wrapper.setValue(key, flagValue);			 
		}
		
		return flagValue;
	}

	/**
	 * 1.4.8里的菜单项“常见问题”是否被点击过
	 * 
	 * @return true表示点击过
	 */
	public boolean getMenuQAClicked_1_4_8() {
		return 0 != (this.commonFlag & COMMON_FLAG_MASK_MENU_QA_CLICKED_1_4_8);
	}

	/**
	 * 设置1.4.8里的“常见问题”已被点击过
	 */
	public void setMenuQAClicked_1_4_8() {
		setCommonFlagBit(COMMON_FLAG_MASK_MENU_QA_CLICKED_1_4_8, true);
	}

	/**
	 * 游戏内大悬浮窗的设置按钮是否已经点击过？
	 */
	public boolean getBoxInGameSettingClicked() {
		return 0 != (this.commonFlag & COMMON_FLAG_MASK_BOXINGAME_SETTING_CLICKED_1_4_8);
	}

	/**
	 * 设置“大悬浮窗里设置按钮”已被点击过
	 */
	public void setBoxInGameSettingClicked() {
		setCommonFlagBit(COMMON_FLAG_MASK_BOXINGAME_SETTING_CLICKED_1_4_8, true);
	}

	/**
	 * 是否向用户发送了 提醒自动清理通知
	 */
	public boolean getSendNoticeAutoClean() {
		return 0 != (this.commonFlag & COMMON_FLAG_MASK_SEND_NOTICE_AUTO_CLEAN);
	}

	/**
	 * 设置：向用户发送了 提醒自动清理通知
	 * 
	 * @param
	 */
	public void setSendNoticeAutoClean() {
		setCommonFlagBit(COMMON_FLAG_MASK_SEND_NOTICE_AUTO_CLEAN, true);
	}

	public boolean getAutoProcessClean() {
		return 0 != (this.commonFlag & COMMON_FLAG_MASK_SET_PROCESS_CLEAN);
	}

	/**
	 * 设置自动内存清理标记
	 */
	public void setAutoProcessClean(boolean on) {
		if (setCommonFlagBit(COMMON_FLAG_MASK_SET_PROCESS_CLEAN, on)) {
			List<Observer> observers = this.cloneAllObservers();
			if (observers != null) {
				for (Observer o : observers) {
					o.onAutoCleanProgressSwitchChange(on);
				}
			}
		}
	}

	public boolean getQuestionSuverySucess() {
		return 0 != (this.commonFlag & COMMON_FLAG_MASK_SET_QUESTION_SUVERY);
	}

	/**
	 * 设置问卷调查成功字段
	 */
	public void setQuestionSuverySucess() {
		setCommonFlagBit(COMMON_FLAG_MASK_SET_QUESTION_SUVERY, true);
	}

	//    /**
	//     * 设置问卷调查为已经通知
	//     * 2.0.0去除该功能
	//     */
	//    @Deprecated
	//    public void setQuestionSuveryNotify() {
	//        setCommonFlagBit(COMMON_FLAG_MASK_SET_QUESTION_SUVERY_NOTIFY, true);
	//    }
	//
	//    @Deprecated
	//    public boolean getQuestionSuveryNotify() {
	//        return 0 != (this.commonFlag & COMMON_FLAG_MASK_SET_QUESTION_SUVERY_NOTIFY);
	//    }
	public long getTimeOfNoticeAccelAchieve() {
		return timeOfNoticeAccelAchieve;
	}

	public void setTimeOfNoticeAccelAchieve(long timeOfNoticeAccelAchieve) {
		this.timeOfNoticeAccelAchieve = timeOfNoticeAccelAchieve;
	}

	public boolean isAlreadySendNoticeAccelAchieve() {
		return alreadySendNoticeAccelAchieve;
	}

	public void setAlreadySendNoticeAccelAchieve() {
		if (this.alreadySendNoticeAccelAchieve) {
			return;
		}
		this.alreadySendNoticeAccelAchieve = true;
		this.wrapper.setValue(KEY_ALREADY_SEND_NOTICE_ACCEL_ACHIEVE, this.alreadySendNoticeAccelAchieve);
	}

	public boolean isAlreadyOpenNoticeAccelAchieve() {
		return alreadyOpenNoticeAccelAchieve;
	}

	public void setAlreadyOpenNoticeAccelAchieve() {
		if (this.alreadyOpenNoticeAccelAchieve) {
			return;
		}
		this.alreadyOpenNoticeAccelAchieve = true;
		this.wrapper.setValue(KEY_ALREADY_OPEN_NOTICE_ACCEL_ACHIEVE, this.alreadyOpenNoticeAccelAchieve);
	}

	public int getGamePlayAchievePercent() {
		return this.gamePlayAchievePercent;
	}

	public void setGamePlayAchievePercent(int achieve) {
		if (this.gamePlayAchievePercent == achieve) {
			return;
		}
		this.gamePlayAchievePercent = achieve;
		this.wrapper.setValue(KEY_GAME_PLAY_ACHIEVE_PERCENT, this.gamePlayAchievePercent);
	}

	/**
	 * 在主界面开启加速次数
	 * 
	 * @return
	 */
	public int getInMainOpenAccelCount() {
		return this.inMainOpenAccelCount;
	}

	public void addInMainOpenAccelCount() {
		this.inMainOpenAccelCount++;
	}

	public String getDebugNodeIP() {
		return this.debugNodeIP;
	}

	public boolean setDebugNodeIP(String value) {
		if (com.subao.common.utils.StringUtils.isStringSame(value, this.debugNodeIP)) {
			return false;
		}
		this.debugNodeIP = value;
		this.wrapper.setValue(KEY_DEBUG_NODE_IP, this.debugNodeIP);
		return true;
	}

	/** 最近一次显示加速开启流程动画是哪一天？ */
	public int getDayOfAccelProgressAni() {
		return this.dayOfAccelProgressAni;
	}

	/** 设置：最近一次显示加速开启流程动画是哪一天？ */
	public void setDayOfAccelProgressAni(int day) {
		if (this.dayOfAccelProgressAni != day) {
			this.dayOfAccelProgressAni = day;
			this.wrapper.setValue(KEY_DAY_ACCEL_PROGRESS_ANI, day);
		}
	}

	public boolean getShowUsageStateHelpDialog() {
		return this.showUsageStateHelpDialog;
	}

	public void setShowUsageStateHelpDialog(boolean showUsageStateHelpDialog) {
		if (this.showUsageStateHelpDialog != showUsageStateHelpDialog) {
			this.showUsageStateHelpDialog = showUsageStateHelpDialog;
			this.wrapper.setValue(KEY_SHOW_USAGE_STATE_HELP_DIALOG, showUsageStateHelpDialog);
		}
	}

	public long getTimeInOfMillisActivateV40() {
		return this.timeInMillisOfActivateV40;
	}

	public void setTimeInMillisOfActivateV40() {
		if (timeInMillisOfActivateV40 > -1) {
			return;
		}
		timeInMillisOfActivateV40 = System.currentTimeMillis();
		this.wrapper.setValue(KEY_TIME_INMILLIS_OF_ACTIVATE_V40, timeInMillisOfActivateV40);
	}

	public boolean isToSendUsageStateNotification() {
		return this.isToSendUsageStateNotification;
	}

	public void setToSendUsageStateNotification() {
		if (this.isToSendUsageStateNotification) {
			return;
		}
		this.isToSendUsageStateNotification = true;
		this.wrapper.setValue(KEY_TO_SEND_USAGE_STATE_NOTIFICATION, this.isToSendUsageStateNotification);
	}

	/**
	 * 取用户设置的悬浮窗模式（模式4.0、4.4和5.0）
	 */
	public int getFloatWindowMode() {
		return this.floatWindowMode;
	}

	/**
	 * 设置悬浮窗模式（检测策略）
	 * 
	 * @return true表示改变过了，false表示没改变（和原来的值一样）
	 */
	public boolean setFloatWindowMode(int mode) {
		if (this.floatWindowMode != mode) {
			this.floatWindowMode = mode;
			this.wrapper.setValue(KEY_FLOAT_WINDOW_MODE, mode);
			return true;
		}
		return false;
	}

	/**
	 * 取加速时长记录
	 * 
	 * @return 加速时长记录，或null
	 */
	public AccelTimeRecord getAccelTimeRecord() {
		return this.accelTimeRecord;
	}

	public void setAccelTimeRecord(AccelTimeRecord rec) {
		if (rec == null) {
			if (this.accelTimeRecord == null) {
				return;
			}
		} else if (rec.equals(this.accelTimeRecord)) {
			return;
		}
		this.accelTimeRecord = rec;
		if (this.accelTimeRecord == null) {
			this.wrapper.setValue(KEY_ACCEL_TIME_RECORD, null);
		} else {
			this.wrapper.setValue(KEY_ACCEL_TIME_RECORD, this.accelTimeRecord.saveToString());
		}
	}

	/**
	 * 取“最近一次上报各设置值”是哪一天
	 * 
	 * @return 是哪天上报的？
	 */
	public int getDayOfSettingValueReport() {
		return this.dayOfSettingValueReport;
	}

	public void setDayOfSettingValueReport(int day) {
		if (dayOfSettingValueReport != day) {
			dayOfSettingValueReport = day;
			this.wrapper.setValue(KEY_DAY_OF_SETTING_VALUE_REPORT_DAY, day);
		}
	}

	public Message_VersionInfo getVersionInfo() {
		return this.versionInfo;
	}

	public void setVersionInfo(Message_VersionInfo info) {
		if (!Misc.isEquals(info, this.versionInfo)) {
			this.versionInfo = info;
			if (info == null) {
				this.wrapper.setValue(KEY_VERSION_INFO, null);
			} else {
				this.wrapper.setValue(KEY_VERSION_INFO, VersionInfoOperator.saveToString(info));
			}
		}
	}

	/**
	 * 返回：最近一次上报Start消息的时刻（UTC毫秒数）
	 */
	public long getLastTimeOfSubmitStartMessage() {
		return this.lastTimeOfSubmitStartMessage;
	}

	/**
	 * 设置：最近一次上报Start消息的时刻
	 * 
	 * @param millisecondsUTC
	 *            UTC毫秒数
	 */
	public void setLastTimeOfSubmitStartMessage(long millisecondsUTC) {
		if (lastTimeOfSubmitStartMessage != millisecondsUTC) {
			lastTimeOfSubmitStartMessage = millisecondsUTC;
			this.wrapper.setValue(KEY_LAST_TIME_OF_SUBMIT_START_MESSAGE, lastTimeOfSubmitStartMessage);
		}
	}

	/**
	 * 返回：最近一次上报“悬浮窗被点击或拖动”是哪一天？
	 */
	public int getDayReportFloatWindowActivate() {
		return this.dayReportFloatWindowActivate;
	}

	/**
	 * 设置：最近一次上报“悬浮窗被点击或拖动”是哪一天？
	 */
	public void setDayReportFloatWindowActivate(int day) {
		if (this.dayReportFloatWindowActivate != day) {
			this.dayReportFloatWindowActivate = day;
			this.wrapper.setValue(KEY_DAY_REPORT_FLOAT_WINDOW_ACTIVATE, day);
		}
	}

	public boolean isAutoChangedAccelModel() {
		return this.isAutoChangedAccelModel;
	}

	public void setAutoChangedAccelModel() {
		this.isAutoChangedAccelModel = true;
	}

	public void resetAutoChangedAccelModel() {
		this.isAutoChangedAccelModel = false;
	}

	/**
	 * 取上一次获取手机验证码的UTC时间
	 */
	public long getLastTimeRequestPhoneVerifyCode() {
		return this.lastTimeRequestPhoneVerifyCode;
	}

	/**
	 * 设置上一次获取手机验证码的UTC时间
	 */
	public void setLastTimeReuestPhoneVerifyCode(long time) {
		if (this.lastTimeRequestPhoneVerifyCode != time) {
			this.lastTimeRequestPhoneVerifyCode = time;
			this.wrapper.setValue(KEY_LAST_TIME_REQUEST_PHONE_VERIFY_CODE, time);
		}
	}

	/**
	 * 获取上一次点击兑换时间（毫秒）
	 * 
	 * @return
	 */
	public long getLastTimestampMillisMarket() {
		return this.lastTimestampMillisMarket;
	}

	/**
	 * 设置上一次点击兑换时间（毫秒）
	 */
	public void setLastTimestampMillisMarket(long time) {
		if (this.lastTimestampMillisMarket != time) {
			this.lastTimestampMillisMarket = time;
			this.wrapper.setValue(KEY_LAST_TIME_STAMP_MARKET, time);
		}
	}

	/**
	 * 取得所有保存的Order ID列表
	 * 
	 * @return 保存的Order ID列表或null
	 */
	public List<String> getAllOrderId() {
		return this.orderIds;
	}

	/** 将orderIds存储 */
	private void saveOrderIDs() {
		this.wrapper.setValue(KEY_ORDER_IDS, cn.wsds.gamemaster.tools.StringUtils.make(this.orderIds));
	}

	/**
	 * 添加一个Order ID
	 */
	public void addOrderId(String orderId) {
		if (this.orderIds == null) {
			orderIds = new ArrayList<String>();
		}
		if (!orderIds.contains(orderId)) {
			orderIds.add(orderId);
			saveOrderIDs();
		}
	}

	/**
	 * 移除一个Order ID
	 */
	public void removeOrderId(String needRemoveId) {
		if (orderIds == null || orderIds.isEmpty()) {
			return;
		}
		boolean modified = false;
		for (int i = orderIds.size(); i >= 0; --i) {
			if (com.subao.common.utils.StringUtils.isStringEqual(orderIds.get(i), needRemoveId)) {
				orderIds.remove(i);
				modified = true;
			}
		}
		if (modified) {
			saveOrderIDs();
		}
	}

	/**
	 * 移除指定的一些Order ID
	 */
	public void removeOrderId(Collection<String> needRemoveIDs) {
		if (orderIds != null && needRemoveIDs != null && !needRemoveIDs.isEmpty()) {
			if (orderIds.removeAll(needRemoveIDs)) {
				saveOrderIDs();
			}
		}
	}

	/** 获取：最近一次弹签到框是哪一天？ */
	public int getDaySignDialogPopup() {
		return this.daySignDialogPopup;
	}

	/** 设置：最近一次弹签到框是哪一天？ */
	public void setDaySignDialogPopup(int day) {
		if (this.daySignDialogPopup != day) {
			this.daySignDialogPopup = day;
			this.wrapper.setValue(KEY_DAY_SIGN_DIALOG_POPUP, day);
		}
	}

	/**
	 * 测试数据获取积分历史请求条数
	 * 
	 * @return -1 为无效值
	 */
	public int getDebugPointHistoryRequest() {
		return debugPointHistoryRequest;
	}

	public void setDebugPointHistoryRequest(int debugPointHistoryRequest) {
		this.debugPointHistoryRequest = debugPointHistoryRequest;
	}

	public void setDefaultDebugPointHistoryRequest() {
		this.debugPointHistoryRequest = -1;
	}

	/**
	 * 在登录状态下，最近一次进入用户中心是今天吗？（天是指北京时间的“天”）
	 */
	public boolean isEnterUserCenterTodayOnLogin() {
		return CalendarUtils.todayCST() == this.enterUserCenterDayOnLogin;
	}

	/**
	 * 设置：在登录状态下，最近一次查入用户中心是今天（天是指北京时间的“天”）
	 */
	public void setToadyEnterUserCenterOnLogin() {
		int todayCST = CalendarUtils.todayCST();
		if (todayCST == this.enterUserCenterDayOnLogin) {
			return;
		}
		this.enterUserCenterDayOnLogin = todayCST;
		this.wrapper.setValue(KEY_DAY_ENTER_USER_CENTER_ON_LOGIN, enterUserCenterDayOnLogin);
	}

	public int getAutoCleanProcessInternal() {
		return this.autoCleanProcessInternal;
	}

	public void setAutoCleanProcessInternal(int index) {
		if (index != autoCleanProcessInternal) {
			this.autoCleanProcessInternal = index;
			this.wrapper.setValue(KEY_AUTO_CLEAN_PROCESS_INTERNAL, index);
		}
	}

	public long getNewUserFirstSartTime() {
		return newUserFristStartTime;
	}

	public void setNewUserFirstStartTime(long time) {
		if (this.newUserFristStartTime != time) {
			newUserFristStartTime = time;
			this.wrapper.setValue(KEY_NEW_USER_FIRST_START_TIME, newUserFristStartTime);
		}
	}

	/** 取：是否使用测试友盟Key？ */
	public boolean getUseTestUmengKey() {
		return this.useTestUmengKey;
	}

	/**
	 * 设置：是否使用测试友盟Key
	 * 
	 * @return true表示要设置的值与当前值不同，已改变为新值；false表示要设置的值与当前值相同，啥都不做
	 */
	public boolean setUseTestUmengKey(boolean value) {
		if (this.useTestUmengKey != value) {
			this.useTestUmengKey = value;
			this.wrapper.setValue(KEY_USE_TEST_UMENG_KEY, value);
			return true;
		}
		return false;
	}

	/**
	 * 是否开启并联加速
	 * 
	 * @return
	 */
	public boolean isEnableDoubleAccel() {
		return 0 != (this.commonFlag & COMMON_FLAG_HAS_DOUBLE_ACCEL);
	}

	/**
	 * 设置并联加速的状态
	 * 
	 * @param isEnable
	 *            true：开启 false：关闭
	 */
	public void setDoubleAccelStatus(boolean isEnable) {
		setCommonFlagBit(COMMON_FLAG_HAS_DOUBLE_ACCEL, isEnable);
	}

	/**
	 * 是否已设置极光推送标签
	 */
	public boolean isHasSetTagForJPush() {
		return 0 != (this.commonFlag & COMMON_FLAG_HAS_SET_TAT_FOR_JPUSH);
	}

	/**
	 * 设置极光推送标签已被设置的标志位
	 */
	public void setHasSetTagForJPush() {
		setCommonFlagBit(COMMON_FLAG_HAS_SET_TAT_FOR_JPUSH, true);
	}

	/**
	 * 是否首次进入并联加速
	 * 
	 * @return
	 */
	public boolean isDoubleAccelFirst() {
		return 0 == (this.commonFlag & COMMON_FLAG_HAS_DOUBLE_LINK_FIRST);
	}

	/**
	 * 设置并联加速是否首次进入
	 * 
	 * @param isEnable
	 *            true：不是首次 false：首次
	 */
	public void setDoubleAccelEnter(boolean isEnable) {
		setCommonFlagBit(COMMON_FLAG_HAS_DOUBLE_LINK_FIRST, isEnable);
	}

	/**
	 * 是否首次进入并联加速
	 * 
	 * @return
	 */
	public boolean getBoxInGameSettingDoubleAccelEntered() {
		return 0 != (this.commonFlag & COMMON_FLAG_MASK_BOXINGAME_SETTING_DOUBLE_ACCEL);
	}

	/**
	 * 设置并联加速是否首次进入
	 */
	public void setBoxInGameSettingDoubleAccelEnter() {
		setCommonFlagBit(COMMON_FLAG_MASK_BOXINGAME_SETTING_DOUBLE_ACCEL, true);
	}

	/**
	 * 返回：最近一次上报并联加速开关状态的时刻（UTC毫秒数）
	 */
	public long getLastTimeOfSubmitDoubleAccel() {
		return this.lastTimeOfSubmitDoubleAccel;
	}

	/**
	 * 设置：最近一次上报的并联加速开关状态的时刻
	 * 
	 * @param millisecondsUTC
	 *            UTC毫秒数
	 */
	public void setLastTimeOfSubmitDoubleAccel(long millisecondsUTC) {
		if (lastTimeOfSubmitDoubleAccel != millisecondsUTC) {
			lastTimeOfSubmitDoubleAccel = millisecondsUTC;
			this.wrapper.setValue(KEY_LAST_TIME_OF_SUBMIT_DOUBLE_ACCEL, lastTimeOfSubmitDoubleAccel);
		}
	}

	/**
	 * 返回：最近一次上报并联加速流量
	 */
	public long getLastFlowOfDoubleAccel() {
		return this.lastFlowOfDoubleAccel;
	}

	/**
	 * 设置：最近一次上报的并联加速流量
	 * 
	 * @param usedFlow
	 *            消耗流量数 （单位字节）
	 */
	public void setLastFlowOfDoubleAccel(long usedFlow) {
		if (lastFlowOfDoubleAccel != usedFlow) {
			lastFlowOfDoubleAccel = usedFlow;
			this.wrapper.setValue(KEY_LAST_FLOW_OF_DOUBLE_ACCEL, lastFlowOfDoubleAccel);
		}
	}

	/**
	 * 返回：最近一次上报并联加速流量
	 */
	public long getLastTcpFlowOfDoubleAccel() {
		return this.lastTcpFlowOfDoubleAccel;
	}

	/**
	 * 设置：最近一次上报的并联加速流量
	 *
	 * @param usedFlow
	 *            消耗流量数 （单位字节）
	 */
	public void setLastTcpFlowOfDoubleAccel(long usedFlow) {
		if (lastTcpFlowOfDoubleAccel != usedFlow) {
			lastTcpFlowOfDoubleAccel = usedFlow;
			this.wrapper.setValue(KEY_LAST_TCP_FLOW_OF_DOUBLE_ACCEL, lastTcpFlowOfDoubleAccel);
		}
	}

	/**
	 * 返回：最近一次上报并联加速流量
	 */
	public long getLastUdpFlowOfDoubleAccel() {
		return this.lastUdpFlowOfDoubleAccel;
	}

	/**
	 * 设置：最近一次上报的并联加速流量
	 *
	 * @param usedFlow
	 *            消耗流量数 （单位字节）
	 */
	public void setLastUdpFlowOfDoubleAccel(long usedFlow) {
		if (lastUdpFlowOfDoubleAccel != usedFlow) {
			lastUdpFlowOfDoubleAccel = usedFlow;
			this.wrapper.setValue(KEY_LAST_UDP_FLOW_OF_DOUBLE_ACCEL, lastUdpFlowOfDoubleAccel);
		}
	}

	/**
	 * 极光是否对6.0以上版本询问过运行时权限
	 */
	public boolean isHasRequestSDK23WPermisionForJPush() {
		return 0 != (this.commonFlag & COMMON_FLAG_IS_REQUEST_SDK23W_PERMISSION_FOR_JPUSH);
	}

	/**
	 * 设置极光对6.0以上版本询问过运行时权限的标志位
	 */
	public void setHasRequestSDK23WPermisionForJpush() {
		setCommonFlagBit(COMMON_FLAG_IS_REQUEST_SDK23W_PERMISSION_FOR_JPUSH, true);
	}

	/**
	 * debug并联加速机型预判开关
	 */
	public boolean getDebugDoubleAccelSwitch() {
		return 0 != (this.commonFlag & COMMON_FLAG_DEBUG_DOUBLE_LINK_MODEL);
	}

	/**
	 * debug并联加速机型预判开关
	 */
	public void setDebugDoubleAccelSwitch(boolean isChecked) {
		setCommonFlagBit(COMMON_FLAG_DEBUG_DOUBLE_LINK_MODEL, isChecked);
	}

    /**
     * 是否首次开启并联加速开关
     */
    public boolean isFirstStartDoubleAccel() {
        return 0 == (this.commonFlag & COMMON_FLAG_HAS_ONLY_ONE_DOUBLE_ACCEL);
    }

    /**
     * 设置非首次开启并联加速开关
     */
    public void setNotFirstStartDoubleAccel() {
        setCommonFlagBit(COMMON_FLAG_HAS_ONLY_ONE_DOUBLE_ACCEL, true);
    }

    /**
     * 是否上报并联加速用户反馈
     * @return
     */
    public boolean isUserFeedBackDoubleAccel() {
        return 0 != (this.commonFlag & COMMON_FLAG_HAS_USER_FEEDBACK_DOUBLE_ACCEL);
    }

    /**
     * 设置已经上报过并联加速用户反馈
     */
    public void setUserFeedBackDoubleAccel() {
        setCommonFlagBit(COMMON_FLAG_HAS_USER_FEEDBACK_DOUBLE_ACCEL, true);
    }


	/**
	 * 是否上报过事件：并联加速使用流量
	 */
	public boolean isDoubleAccelFlagHasReportFlow(long bit) {
		return 0 != (this.doubleAccelFlowEventFlag
				& bit);
	}

	/**
	 * 设置开关：并联加速使用流量，标志位置1
	 */
	public void setDoubleAccelFlagHasReportFlow(long bit){
		setDoubleAccelFlowEventFlagBit(
				bit, true);
	}

	/** 最近一次上报网络无法识别事件是哪一天？ */
	public int getDayOfNetWorkUnknown() {
		return this.dayOfNetWorkUnknown;
	}

	/** 设置：最近一次上报网络无法识别事件是哪一天？ */
	public void setDayOfNetWorkUnknown(int day) {
		if (this.dayOfNetWorkUnknown != day) {
			this.dayOfNetWorkUnknown = day;
			this.wrapper.setValue(KEY_DAY_NETWORK_UNKNOWN, day);
		}
	}
	
	 /** 是否显示过注册或充值的提示页面*/
	public boolean isHasShowRemindGuideActivity(){	
		 return 0 != (this.commonFlag & COMMON_FLAG_HAS_REMIND_REGIST_RECHARGE);
	}
	
	 /** 设置显示过注册或充值的提示页面*/
	public void setHasShowRemindGuideActivity(){
		 setCommonFlagBit(COMMON_FLAG_HAS_REMIND_REGIST_RECHARGE, true);
	}
	
	/** 是否需要弹窗提示绑定手机号*/
	public boolean isNeedRemindBoundPhoneNumber(){	
		 return 0 != (this.commonFlag & COMMON_FLAG_NEED_REMIND_BOUND_PHONE);
	}
		
	/** 设置需要弹窗提示绑定手机号*/
	public void setNeedRemindBoundPhoneNumber(boolean checked){
		setCommonFlagBit(COMMON_FLAG_NEED_REMIND_BOUND_PHONE, checked);
	}

	/** 是否需要弹窗提示注册或登录*/
	public boolean isNeedRemindRegistLogin(){	
		return 0 != (this.commonFlag & COMMON_FLAG_NEED_REMIND_LOGIN_REGIST);
	}
		
	/** 设置需要弹窗提示注册或登录*/
	public void setNeedRemindRegistLogin(boolean checked){
		setCommonFlagBit(COMMON_FLAG_NEED_REMIND_LOGIN_REGIST, checked);
	}
		
	/** 是否需要弹窗提示续费*/
	public boolean isNeedRemindRenew(){	
		return 0 != (this.commonFlag & COMMON_FLAG_NEED_REMIND_RENEW);
	}
		
	/** 设置需要弹窗提示续费*/
	public void setNeedRemindRenew(boolean checked){
		setCommonFlagBit(COMMON_FLAG_NEED_REMIND_RENEW, checked);
	}

	/** 是否需要弹窗提示续费*/
	public boolean getFirstStartAccel(){
		return 0 != (this.commonFlag & COMMON_FLAG_FIRST_START_ACCEL);
	}

	/** 设置需要弹窗提示续费*/
	public void setFirstStrtAccel(boolean checked){
		setCommonFlagBit(COMMON_FLAG_FIRST_START_ACCEL, checked);
	}

	/** 是否进入过引导页*/
	public boolean getGuidePage(){
		return 0 != (this.commonFlag & COMMON_FLAG_GUIDE_PAGE);
	}

	/** 是否进入过引导页*/
	public void setGuidePage(boolean checked){
		setCommonFlagBit(COMMON_FLAG_GUIDE_PAGE, checked);
	}

	/** 未登录状态提示的天数  */
	public int getDayOfUnLogin() {
		return lastDayOfUnlogin;
	}

	/** 设置未登录状态提示的天数  */
	public void setDayOfUnLogin(int day) {
		if (this.lastDayOfUnlogin != day) {
			this.lastDayOfUnlogin = day;
			this.wrapper.setValue(KEY_LAST_DAY_OF_UNLOGIN, day);
		}
	}

	/** 未登录状态提示的天数  */
	public int getLastDayOfNormalAceel() {
		return lastDayOfNormalAceel;
	}

	/** 设置未登录状态提示的天数  */
	public void setLastDayOfNormalAceel(int day) {
		if (this.lastDayOfNormalAceel != day) {
			this.lastDayOfNormalAceel = day;
			this.wrapper.setValue(KEY_LAST_DAY_OF_NORMAL_ACCEL, day);
		}
	}


	/** 最近一次提示VIP服务即将过期是哪一天？ */
	public int getDayOfRemindVIPWillBeExpired() {
		return this.dayOfRemindVIPWillBeExpired;
	}

	/** 设置：最近一次显示提示VIP服务过期是哪一天？ */
	public void setDayOfRemindVIPWillBeExpired(int day) {
		if (this.dayOfRemindVIPWillBeExpired != day) {
			this.dayOfRemindVIPWillBeExpired = day;
			this.wrapper.setValue(KEY_DAY_OF_REMIND_VIP_WILL_EXPIRED, day);
		}
	}

	/** 是否需要弹窗提示续费*/
	public boolean getFirstPromptLogin(){
		return 0 != (this.commonFlag & COMMON_FLAG_FIRST_PROMPT_LOGIN);
	}

	/** 设置需要弹窗提示续费*/
	public void setFirstPromptLogin(boolean checked){
		setCommonFlagBit(COMMON_FLAG_FIRST_PROMPT_LOGIN, checked);
	}

	/** 获取最后一次tcp流量的天数  */
	public int getDayOfTcpFlow() {
		return lastDayOfTcpFlow;
	}

	/** 设置最后一次tcp流量的天数  */
	public void setDayOfTcpFlow(int day) {
		if (this.lastDayOfTcpFlow != day) {
			this.lastDayOfTcpFlow = day;
			this.wrapper.setValue(KEY_LAST_DAY_OF_TCP_FLOW, day);
		}
	}

	/** 获取最后一次udp流量的天数 */
	public int getDayOfUdpFlow() {
		return lastDayOfUdpFlow;
	}

	/** 设置最后一次udp流量的天数  */
	public void setDayOfUdpFlow(int day) {
		if (this.lastDayOfUdpFlow != day) {
			this.lastDayOfUdpFlow = day;
			this.wrapper.setValue(KEY_LAST_DAY_OF_UDP_FLOW, day);
		}
	}

	/** 获取最近一次tcp流量的 */
	public long getLastAccelTcpFlow() {
		return lastAccelTcpFlow;
	}

	/** 设置最后一次tcp流量的 */
	public void setLastAccelTcpFlow(long lastAccelTcpFlow) {
		if (this.lastAccelTcpFlow != lastAccelTcpFlow) {
			this.lastAccelTcpFlow = lastAccelTcpFlow;
			this.wrapper.setValue(KEY_LAST_ACEEL_TCP_FLOW, lastAccelTcpFlow);
		}
	}

	/** 获取最近一次udp流量的 */
	public long getLastAccelUdpFlow() {
		return lastAccelUdpFlow;
	}

	/** 设置最后一次tcp流量的 */
	public void setLastAccelUdpFlow(long lastAccelUdpFlow) {
		if (this.lastAccelUdpFlow != lastAccelUdpFlow) {
			this.lastAccelUdpFlow = lastAccelUdpFlow;
			this.wrapper.setValue(KEY_LAST_ACEEL_UDP_FLOW, lastAccelUdpFlow);
		}
	}
}
