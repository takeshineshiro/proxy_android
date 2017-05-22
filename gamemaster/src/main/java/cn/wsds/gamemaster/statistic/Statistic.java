package cn.wsds.gamemaster.statistic;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.text.TextUtils;
import android.util.JsonWriter;

import com.subao.utils.Misc;
import com.umeng.analytics.MobclickAgent;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.useraction.UserActionManager;

/**
 * Statistic 的缺省实现：友盟+内部事件系统
 */
public class Statistic {
	/**
	 * 事件枚举。<br />
	 * <b>* 名字一旦确定，则不可更改，也不可混淆（上报事件名使用枚举名的全小写串）</b><br />
	 * <b>* 顺序不可更改（上报UserAction的ID使用枚举序号）</b><br />
	 * <b>* 即使某项已不再使用，也绝不能删除，否则会影响其后的序号</b>
	 */
	public enum Event {
//		/** 1 - 本程序成功启动 (<b>此事件目前不上报</b>) */
//		@Deprecated
//		APP_START,
		/** 2 - 加速成功开启 */
		ACC_ALL_START_SUCCESS_NEW,
		/** 3 - 开启VPN流程 */
		ACC_PROCESS_VPNSTART,
		/** 4 - 激活 */
		BACKSTAGE_ACTIVATION,
//		/** 5 - 进入页面时加速状态 */
//		@Deprecated
//		STATUS_VPN,
//		/** 6 - 累计游戏时长 0=15M */
//		@Deprecated
//		STATUS_GAME_TIME_0,
//		/** 7 - 主页面点击返回 */
//		@Deprecated
//		CLICK_BACK,
//		/** 8 - 主页点击开启加速 (1.4.2废弃) */
//		@Deprecated
//		CLICK_START,
		/** 9 - 主页点击关闭加速 */
		ACC_HOMEPAGE_CLICK_STOP,
//		@Deprecated
//		/** 10 - 退出程序 */
//		CLICK_EXIT,
		/** 11 - 点击游戏列表 */
		INTERACTIVE_CLICK_GAME_LIST,
		/** 12 - 点击启动游戏 */
		ACC_HOMEPAGE_CLICK_GAME_START,
//		/** 13 - 同时运行的游戏数 */
//		@Deprecated
//		CLICK_GAME_SWITCH,
//		@Deprecated
//		/** 14 - 游戏加速开启 */
//		MOTION_GAME_START,
		/** 15 - 新游戏提醒 */
		NOTIFICATION_NEW_GAME,
//		@Deprecated
//		/** 16 - 新游戏提醒被点击 */
//		NOTIFICATION_NEW_GAME_READ,
//		/** 17 - 提醒开启加速 */
//		@Deprecated
//		TOAST_GAME_START_REQUEST,
//		/** 18 - 提醒加速失败 */
//		@Deprecated
//		TOAST_GAME_FAIL,
//		/** 19 - 退出界面时拦截　 */
//		DIALOG_EXIT_HOMEPAGE,
//		/** 20 - 游戏过程中断开加速　 */
//		@Deprecated
//		DIALOG_DISCONNECT_GAME,
//		/** 21 - 启动悬浮窗时加速状态　 */
//		@Deprecated
//		FLOATING_WINDOW_STATUS_VPN,
//		/** 22 - 点击“开启加速 （1.4.2废弃） */
//		@Deprecated
//		FLOATING_WINDOW_CLICK_ACC_START,
//		/** 23 - 关闭悬浮窗显示 */
//		@Deprecated
//		FLOATING_WINDOW_OFF,
//		/** 24 - 关闭悬浮窗显示设置时，累计游戏加速时长 */
//		@Deprecated
//		FLOATING_WINDOW_OFF_TIME,
//		/** 25 - 展开悬浮窗 （1.4.2废弃） */
//		@Deprecated
//		FLOATING_WINDOW_CLICK,
//		/** 26 - 加速失败　 */
//		@Deprecated
//		FLOATING_WINDOW_ACC_FAIL,
//		/** 27 - 异常状态　 */
//		@Deprecated
//		FLOATING_WINDOW_EXCEPTION,
		/** 28 - 分享　 */
		SHARE_HOMEPAGE_CLICK,
		/** 29 - 累计游戏时长 1=30M */
//		@Deprecated
//		STATUS_GAME_TIME_1,
//		/** 30 - 累计游戏时长 2=2h */
//		@Deprecated
//		STATUS_GAME_TIME_2,
//		/** 31 - 累计游戏时长 3=6h */
//		@Deprecated
//		STATUS_GAME_TIME_3,
//		/** 32 - 累计游戏时长 4=24h */
//		@Deprecated
//		STATUS_GAME_TIME_4,
//		/**
//		 * 33 - 测速结果
//		 * 
//		 * @deprecated Use SPEED_TEST_RESULT_xxx instead
//		 */
//		@Deprecated
//		SPEED_TEST_RESULT,
//		/** 34 - 测速结果分布 */
//		@Deprecated
//		SPEED_TEST_RESULT_3G,
//		/** 35 - 测速结果分布 */
//		@Deprecated
//		SPEED_TEST_RESULT_4G,
//		/** 36 - 测速结果分布 */
//		@Deprecated
//		SPEED_TEST_RESULT_WIFI,
//		/** 37 - 测速结果分布 */
//		@Deprecated
//		SPEED_TEST_NODE,
		/** 38 - 使用成就提醒 */
		NOTIFICATION_USE_REPORT,
//		@Deprecated
//		/** 39 - 使用成就提醒阅读 */
//		NOTIFICATION_REPORT_READ,
		/** 40 - 悬浮窗被临时隐藏 */
		FLOATING_WINDOW_HIDE,
		/** 41 - 大悬浮窗展开时的网络状态 */
		FLOATING_WINDOW_OPEN_STATUS,
//		@Deprecated
//		/** 42 - 大悬浮窗展开时长 */
//		FLOATING_WINDOW_UNFOLD_TIME,
//		/** 43 - 测速结果统计（网络情况） */
//		@Deprecated
//		SPEED_TEST_RESULT_DETAIL,
		/** 44 - 测速结果统计 3G */
		NETWORK_SPEED_TEST_RESULT_CAUSE_3,
		/** 45 - 测速结果统计 4G */
		NETWORK_SPEED_TEST_RESULT_CAUSE_4,
		/** 46 - 测速结果统计 WiFi */
		NETWORK_SPEED_TEST_RESULT_CAUSE_W,
		/** 47 - 测速结果统计 其它 */
		NETWORK_SPEED_TEST_RESULT_CAUSE_O,
		/** 48 - 上报root与否 */
		@Deprecated
		ROOTINFO,
		/** 49 - 网络异常诊断结果 */
		NETWORK_FLOATING_OPEN_CHECK,
//		/** 50 - 打开流量拦截开关（1.4.2废弃） */
//		@Deprecated
//		SWITCH_FIREWALL_ON,
//		/** 51 - 断线重连记录 */
//		@Deprecated
//		REPAIR_CONNECTION,
		/** 52 - 推送通知给非活跃用户 */
		NOTIFICATION_AWAKE,
		/** 53 - 用户阅读了提醒通知 */
		NOTIFICATION_AWAKE_READ,
		/** 54 - 游戏在前台运行超过15分钟推送通知 */
		NOTIFICATION_REPORT_GAME,
//		@Deprecated
//		/** 55 - 游戏在前台运行超过15分钟推送通知，用户点击阅读了该通知 */
//		NOTIFICATION_REPORT_GAME_READ,
		/** 56 - 游戏在前台运行超过15分钟，但是游戏进程并未结束 */
		NOTIFICATION_REPORT_GAME2,
//		@Deprecated
//		/** 57 - 使用成就 分享 */
//		NOTIFICATION_REPORT_SHARE,
//		@Deprecated
//		/** 58 - 防断线Toast */
//		TOAST_NETWORK_RECONNECTED,
//		@Deprecated
//		/** 59 - 关闭单游戏推送通知 */
//		SWITCH_NOTIFICATION_REPORT_GAME,
		/** 60 - MIUI/EMUI用户提示开启悬浮窗 */
		INTERACTIVE_PROMPT_DIALOG_UI_FLOATING,
		/** 61 - 点击去设置MIUI/EMUI悬浮窗 */
		INTERACTIVE_SETTING_CLICK_UI_FLOATING,
//		/** 62 - 当onTirmMemory()被调用时上传手机型号 */
//		@Deprecated
//		PROCESS_KILLED_PHONETYPE,
//		/** 63 - 当onTirmMemory()被调用时上传Level */
//		@Deprecated
//		PROCESS_KILLED_LEVEL,
		/** 64 - 开机启动统计事件 */
		DIALOG_AUTOSTART,
		/** 65 - 清理内存统计事件 */
		FLOATING_WINDOW_CLEAR_RAM,
//		@Deprecated
//		/** 66 - 开启VPN时连接失败 */
//		VPN_ESTABLISH_FAIL,
//		@Deprecated
//		/** 67 - 断线重连原因 */
//		REPAIR_CONNECTION_CAUSE,
//		/** 68 - 断线重连特效被用户关闭 （1.4.2废弃） */
//		@Deprecated
//		REPAIR_CONNECTION_EFFECT_CLOSED_BY_USER,
//		/** 69 - 快的分享弹窗 */
//		@Deprecated
//		DIALOG_KUAIDI_SHARE,
//		/** 70 - 快的弹窗内点击分享 */
//		@Deprecated
//		CLICK_KUAIDI_SHARE_DIALOG,
//		/** 71 - 快的朋友圈点击分享 */
//		@Deprecated
//		CLICK_KUAIDI_SHARE_FRIEND,
//		/** 72 - 快的活动领取成功 */
//		@Deprecated
//		DIALOG_KUAIDI_PACKET_SUCCESS,
//		/** 73 - 快的活动领取失败 */
//		@Deprecated
//		DIALOG_KUAIDI_PACKET_FAIL,
//		@Deprecated
//		/** 74 - 新的断线重连统计 */
//		REPAIR_CONNECTION2,
//		@Deprecated
//		/** 75 - 关闭root模式 */
//		SWITCH_ROOTMODE_OFF,
//		@Deprecated
//		/** 76 - 打开root模式 */
//		SWITCH_ROOTMODE_ON,
		/** 77 - root模式开启流程 */
		ACC_PROCESS_ROOTSTART,
		/** 78 - 断线重连特效显示的结果 */
		NETWORK_REPAIR_CONNECTION_EFFECT_RESULT,
//		@Deprecated
//		/** 79 - 页面Resume */
//		ACTIVITY_RESUME,
		/** 80 - 页面Pause */
		ACTIVITY_PAUSE,
		/** 81 - 开启加速 */
		ACC_ALL_START_NEW,
		/** 82 - 游戏到前台 */
		BACKSTAGE_GAME_ON_TOP,
//		@Deprecated
//		/** 83 - 心跳包 */
//		HELLO,
//		@Deprecated
//		/** 84 - 点击消息中心里的帮助消息跳转到帮助页面 */
//		MESSAGE_READ_APPDEMO,
		/** 85 - 悬浮窗接听事件 */
		FLOATING_WINDOW_CALL_IN,
//		@Deprecated
//		/** 86 - 关闭游戏来电提醒 */
//		SWITCH_CALLMANAGER_OFF,
		/** 87 - 接听是否成功事件 */
		FLOATING_WINDOW_CALL_CONTACTS_RESULT,
		/** 88 - 获取联系人信息 */
		GET_CONTACT_INFO,
		/** 89 - app被卸载了 */
		APP_UNINSTALLED,
		/** 90 - app被清理了 */
		APP_CLEANED,
		/** 91 - 统计用户在未开启加速的情况下打开悬浮窗后，点击“开启加速”和离开的比例 */
		FLOATING_WINDOW_OPEN_NO_ACC,
		/** 92 - 重新加速流程 */
		ACC_GAME_RESTART,
//		@Deprecated
//		/** 93 - 每次进入主页面时，统计曲线图展开与否*/
//		STATUS_GRAPH,
//		@Deprecated
//		/** 94 - 曲线图收起 */
//		CLICK_GAME_LIST_FOLD,
//		/** 95 - 网络请求被关闭时的关闭原因 */
//		@Deprecated
//		CLOSECONNECT,
//		@Deprecated
//		/** 96 - 设置里关闭悬浮窗 */
//		SWITCH_FLOATINGWINDOW_OFF,
//		/** 97 - JNI层传来：连接已建立 */
//		@Deprecated
//		CONNECT,
//		@Deprecated
//		/** 98 - 连续3次及以上root模式开启加速失败，对话框提示 */
//		DIALOG_ROOTFAILURE,
//		@Deprecated
//		/** 99 - process_rootguide*/
//		PROCESS_ROOTGUIDE,
//		@Deprecated
//		/** 100 - 出现流量警告Toast */
//		TOAST_BACKGROUND_TRAFFIC,
		/** 101 - 截屏事件流程 */
		FLOATING_WINDOW_PROCESS_SCREENSHOT,
//		@Deprecated
//		/** 102 - 截屏结果反馈 */
//		CLICK_SCREENSHOT,
//		@Deprecated
//		/** 103 - 截屏保存 */
//		CLICK_SCREENSHOT_SAVE,
//		@Deprecated
//		/** 104 - 截屏分享 */
//		CLICK_SCREENSHOT_SEND,
		/** 105 - 在游戏悬浮窗中点击“开启加速” */
		FLOATING_WINDOW_ACC_DIALOG_START,
//		@Deprecated
//		/** 106 - 开机直接开启加速 */
//		ACC_STARTUP,
//		@Deprecated
//		/** 107 - 悬浮窗样式- mini */
//		SWITCH_FLOATINGWINDOW_SIZE_MINI,
//		@Deprecated
//		/** 108 - 悬浮窗样式- normal */
//		SWITCH_FLOATINGWINDOW_SIZE_DEFAULT,
		/** 109 - 显示“游戏下载更新正在影响网络速度”的Toast */
		FLOATING_WINDOW_TOAST_ABNORMAL_TRAFFIC,
//		@Deprecated
//		/** 110 - 截屏完成后，用户点击了“取消” */
//		CLICK_SCREENSHOT_CANCEL,
		/** 111 - 设备信息 */
		DEVICE_INFO {
			@Override
			boolean doesReportUmeng() {
				return false;
			}
		},
//		@Deprecated
//		/** 112 - 阅读防清理消息 */
//		MESSAGE_READ_CLEANUP,
//		@Deprecated
//		/** 113 - 激活（点击或拖动）小悬浮窗 */
//		FLOATING_WINDOW_CALL_ACTIVATE,
		/** 114 - 是否在桌面添加快速启动工具 */
		DIALOG_SHORTCUT,
		/** 115 - 打开快捷方式 */
		INTERACTIVE_SHORTCUT_CLICK_OPEN,
		/** 116 - 快捷方式启动游戏 */
		ACC_SHORTCUT_CLICK_GAME_START,
		/** 117 - 创建快捷方式 */
		INTERACTIVE_SHORTCUT_CREATE,
//		//
//		// 1.4.2.1新增
//		//
//		/** 118 - 测速结果统计 3G （重测） */
//		@Deprecated
//		SPEED_TEST_RESULT2_CAUSE_3G,
//		/** 119 - 测速结果统计 4G （重测） */
//		@Deprecated
//		SPEED_TEST_RESULT2_CAUSE_4G,
//		/** 120 - 测速结果统计 WiFi （重测） */
//		@Deprecated
//		SPEED_TEST_RESULT2_CAUSE_WIFI,
//		/** 121 - 测速结果统计 其它 （重测） */
//		@Deprecated
//		SPEED_TEST_RESULT2_CAUSE_OTHER,
//		/** 122 - 测速后使用新节点 */
//		@Deprecated
//		SPEED_TEST_NEW_NODE,	// 1.4.7去掉
//		//
//		/**
//		 * 123 - 换肤按钮 2.0.0 废弃
//		 */
//		@Deprecated
//		FLOATING_WINDOW_CLICK_SKIN_SKLR,
//		/** 124 - 主页面创建时自动生成桌面快捷方式 */
//		@Deprecated
//		AUTO_CREATE_SHORTCUT,
//		/** 125 - DNS智能选择节点解析结果 */
//		@Deprecated
//		DNS_NODE,
		/** 126 - 来电提醒功能对话框 */
		DIALOG_INGAME_CALL,
//		/** 127 - 悬浮窗中，点击到详细信息页 */
//		@Deprecated
//		FLOATING_WINDOW_INFO,
//		/** 128 - 发现有嵌入了速宝SDK的游戏 */
//		@Deprecated
//		SDK_GAME,
		/** 129 - 测速失败的对话框提醒 */
		ACC_GAME_CLICK_START_FAIL,
//		@Deprecated
//		/** 130 - 点击“加速效果还可提升”的通知 */
//		NOTIFICATION_GAME_OPTIMIZE,
//		/**
//		 * 131 - 底层上报的onAccelState，非海外游戏、非80、443端口
//		 * 
//		 * @see #BACKSTAGE_ACCEL_STATE_FOREIGN_NEW
//		 * @see #BACKSTAGE_ACCEL_STATE_80_443_NEW
//		 */
//		BACKSTAGE_ACCEL_STATE_NEW,
		/** 132 - 推送“加速效果还可提升”的通知 */
		NOTIFICATION_PUSH_GAME_OPTIMIZE,
		/** 133 - 分享成功次数 */
		SHARE_HOMEPAGE_CLICK_SUCCESS,
		/** 134 - HOOK成功与否 */
		BACKSTAGE_HOOK_RESULT,
//		@Deprecated
//		/** 135 - 点击通知栏成就分享页面次数 */
//		CLICK_NOTIFICATION_BAR_SHARE_RESULT,
//		@Deprecated
//		/** 136 - 通知栏新成就分享成功次数 */
//		NOTIFICATION_BAR_SHARE_RESULT_SUCCESSFUL,
//		@Deprecated
//		/** 137 - 通知栏新成就分享关闭次数 */
//		NOTIFICATION_BAR_SHARE_RESULT_CLOSE,
//		@Deprecated
//		/** 138 - 主界面新成就分享弹出次数 */
//		INTERFACE_SHARE_RESULT_POPUP,
//		@Deprecated
//		/** 139 - 主界面新成就分享成功次数 */
//		INTERFACE_SHARE_RESULT_SUCCESSFUL,
//		@Deprecated
//		/** 140 - 主界面新成就分享关闭次数 */
//		INTERFACE_SHARE_RESULT_CLOSE,
//		/** 141 - 主页开户中加速动画呈现 (v1.5.2废弃) */
//		@Deprecated
//		ACC_ANIMATION,
//		/**
//		 * 142 - 海外游戏（非80、443）的加速状态
//		 * 
//		 * @see #BACKSTAGE_ACCEL_STATE_NEW
//		 * @see #BACKSTAGE_ACCEL_STATE_80_443_NEW
//		 */
//		BACKSTAGE_ACCEL_STATE_FOREIGN_NEW,

//		2016.7.25废弃 (2.2.4)		
//		/**
//		 * 143 - 80和443端口的加速状态
//		 * 
//		 * @see #BACKSTAGE_ACCEL_STATE_NEW
//		 * @see #BACKSTAGE_ACCEL_STATE_FOREIGN_NEW
//		 */
//		BACKSTAGE_ACCEL_STATE_80_443_NEW,
		
		
		/** 144 - 开启加速失败(任意形式) */
		ACC_ALL_START_FAIL_NEW,
		/** 145 - 修改root模式 */
		ACC_ALL_SWITCH_ROOTMODE,
		/** 146 - 用户加速模式(任意形式) */
		ACC_ALL_START_MODE_NEW,
		/** 147 - 开机自动启动开关（设置页） */
		ACC_POWER_SWITCH_STARTUP,
		/** 148 - 测速失败的对话框弹出原因 */
		ACC_GAME_CLICK_START_FAIL_REASON,
		/** 149 - 当天总游戏时长 */
		ACC_GAME_PLAY_TIME,
		/** 150 - 游戏来电提醒开关 */
		FLOATING_WINDOW_SWITCH_CALLMANAGER,
		/** 151 - 修改悬浮窗尺寸（点击） */
		FLOATING_WINDOW_SETTING_CLICK_SIZE,
		/** 152 - 悬浮窗大小（状态） */
		FLOATING_WINDOW_SETTING_SIZE,
		/** 153 - 开关悬浮窗点击（点击） */
		FLOATING_WINDOW_SETTING_SWITCH_DISPLAY,
		/** 154 - 悬浮窗开关（状态） */
		FLOATING_WINDOW_SETTING_DISPLAY,

//		/** 155 - 任意位置点击“开始诊断”按钮 */
//		@Deprecated
//		NETWORK_DIAGNOSIS_CLICK_START,
//		/** 156 - 任意形式中断网络诊断 */
//		@Deprecated
//		NETWORK_DIAGNOSIS_CLICK_CANCEL,
//		/** 157 - 网络诊断结束后点击“开启加速” */
//		@Deprecated
//		NETWORK_DIAGNOSIS_CLICK_ACC,
//		/** 158 - 用户网络诊断结果 */
//		@Deprecated
//		NETWORK_DIAGNOSIS_RESULT,
//		/** 159 - 网络设备处用户诊断结果区间 */
//		@Deprecated
//		NETWORK_DIAGNOSIS_RESULT_SECTION_DEVICE,
//		/** 160 - 运营商处用户诊断结果区间 */
//		@Deprecated
//		NETWORK_DIAGNOSIS_RESULT_SECTION_OPERATOR,

		/** 161 - 资源请求失败 */
		BACKSTAGE_RES_REQUEST_FAIL_NEW,
		/** 162 - 启动迅游手游 */
		INTERACTIVE_CLIENT_POWER,

		/** 163 - 内存清理（主页内） */
		INTERACTIVE_CLEARRAM_IN,
		/** 164 - 客户端内完成清理内存次数（不包括悬浮窗） */
		INTERACTIVE_CLEARRAM_OVER,
		/** 165 - 加速结果推送是否开启 */
		INTERACTIVE_SETTING_SWITCH_ACC_RESULT,
		/** 166 - 内存清理(手动/自动) */
		INTERACTIVE_SETTING_SWITCH_CLEARRAM,
		
//		//////// 以下事件2.2.4废弃 ////////
//		
//		/** 167 - 电信4G用户进入游戏前连续3个包均超过80ms */
//		DEBUG_BEFORE_GAME_THREE_OVER_80,
//		/** 168 - 电信4G用户进入游戏前连续3个包均超过100ms */
//		DEBUG_BEFORE_GAME_THREE_OVER_100,
//		/** 169 - 电信4G用户进入游戏前连续6个包均超过80ms */
//		DEBUG_BEFORE_GAME_SIX_OVER_80,
//		/** 170 - 电信4G用户进入游戏前连续6个包均超过100ms */
//		DEBUG_BEFORE_GAME_SIX_OVER_100,
//		/** 171 - 电信4G用户进入游戏前连续9个包均超过80ms */
//		DEBUG_BEFORE_GAME_NINE_OVER_80,
//		/** 172 - 电信4G用户进入游戏前连续9个包均超过100ms */
//		DEBUG_BEFORE_GAME_NINE_OVER_100,
//		/** 173 - 电信4G用户进入游戏前以上都没达成 */
//		DEBUG_BEFORE_GAME_ALL_OUT,
//		
//		/////////////////////////////////////////
		
		/** 174 - 查看应用权限授权获得情况（安卓系统≥5.0），参数“授权成功”/“授权失败”。每 天第一次进入主界面上报 */
		INTERACTIVE_SETTING_AUTHORIZE_FLOATING,
		/** 175 - 成就分享页面弹出 */
		SHARE_ACHIEVEMENT_APPEAR,
		/** 176 - 点击分享成就页面（参数：分享成功/取消分享） */
		SHARE_ACHIEVEMENT_CLICK,
		/** 177 - 断开VPN原因，参数“程序内断开”和“小钥匙处断开” */
		NETWORK_VPN_STOP_SEASON,
		/** 178 - 开启流程 */
		ACC_PROCESS_START,
		/** 179 - 悬浮窗显示时长 */
		FLOATING_WINDOW_DISPLAY_TIME,
		/** 180 - 悬浮窗显示时长（小米和华为） */
		FLOATING_WINDOW_DISPLAY_TIME_MI,
		/** 181 - 悬浮窗拖动（小米和华为） */
		FLOATING_WINDOW_DRAG_MI,
		/** 182 - 用户注册结果 */
		USER_REGISTER_RESULT,
		/** 183 - 用户类型 (2.0.0版新增，但暂不统计) */
		USER_LOGIN_TYPE,
		/** 184 - 用户积分 */
		USER_INTEGRAL_SECTION,
		/** 185 - 用户进入用户中心 */
		INTERACTIVE_USER_CENTER_IN,
//		/** 186 - 兑换中心兑换结果 **/
//		@Deprecated
//		USER_EXCHANGE_RESULT,
		/** 187 - 兑换未完成原因 **/
		USER_EXCHANGE_FAIL_REASON,
		/** 188 - 单日签到用户数 */
		USER_SIGN_SAMEDAY,
		/** 189 - 连续签到用户数 */
		USER_SIGN_CONTINUITY,
		/** 190 - 程序启动时已连上无密码的WiFi */
		NETWORK_UNSAFE_WIFI_CONNECT,
		/** 191 - 每天上报一次的：手机是否ROOT过 */
		BACKSTAGE_ROOT_USER,
		/** 192 - 用户点击注册按钮 */
		USER_REGISTER_CLICK,
		/** 193 - 用户点击兑换中心按钮 */
		USER_EXCHANGE_CENTRE_CLICK,
		/** 194 - 用户点击兑换按钮 */
		USER_EXCHANGE_CENTRE_CLICK_CHANGE,
		/** 195 - 内存自动清理时长 */
		INTERACTIVE_SETTING_CLEARRAM_TIME,
		/** 196 - 首次点击下载游戏 */
		USER_FIRST_DOWNLOAD_GAME,
		/** 197 - 用户点击任务中心 */
		USER_TASK_CENTRE_CLICK,
		/** 198 - 用户进入兑换中心时积分情况 */
		USER_EXCHANGE_CENTRE_IN_SCORE,
		/** 199 - 兑换中心流量包兑换结果 */
		USER_EXCHANGE_FLOW_RESULT,
		/** 200 - 计数提示进入用户中心的通知推送 */
		NOTIFICATION_PROMPT_USER_CENTER,
		/** 201 - 用户阅读此注册有礼通知的数量 */
		NOTIFICATION_PROMPT_USER_CENTER_CLICK,
		/** 202 - 游戏礼包兑换结果 */
		USER_EXCHANGE_GAME_RESULT,
//		/** 203 - 代理层初始化失败错误代码 */
//		@Deprecated
//		ERROR_PROXY_INIT,
		/** 用户单次游戏中切换网络 */
		BACKSTAGE_USER_NETWORK_CHANGE,
//		/** 并网加速流量消耗 */
//		@Deprecated
//		BACKSTAGE_DUAL_NETWORK_USE_FLOW,
		/** 首页并网加速点击次数 */
		INTERACTIVE_DUAL_NETWORK_HOMEPAGE_CLICK,
//		/** 并网加速说明页点击“我知道了” */
//		@Deprecated
//		INTERACTIVE_DUAL_NETWORK_ILLUSTRATION_CLICK,
//		/** 点击并网加速开关(全局) */
//		@Deprecated
//		INTERACTIVE_DUAL_NETWORK_SWITCH_CLICK,
		/** 并网加速开关状态 */
		INTERACTIVE_DUAL_NETWORK_SWITCH_STATUS,
		/** 点击并网加速开关(悬浮窗) */
		FLOATING_WINDOW_DUAL_NETWORK_SWITCH_CLICK,
		/** 双路径功能使用次数 */
		ACC_DUAL_NETWORK_TRIGGER,
		/** 用户昨日游戏在前台的时长 */
		ACC_GAME_PLAY_TIME_TOTAL,
		/** 双链路用户机型 */
		BACKSTAGE_DUAL_NETWORK_USER_PHONE,
		/** 启动页崩溃事件，一生只报一次 */
		BACKSTAGE_START_PAGE_CRASH,
		/** 悬浮窗addView时异常 */
		BACKSTAGE_FLOATWINDOW_EXCEPTION_ADDVIEW,
		/** 悬浮窗removeView时异常 */
		BACKSTAGE_FLOATWINDOW_EXCEPTION_REMOVEVIEW,
		/** 程序初始化异常 */
		BACKSTAGE_APP_INIT_FAIL,
		/** 用户反馈 */
		INTERACTIVE_DUAL_NETWORK_INVEST,
		/** 双链路使用了数据流量累计>0 */
		BACKSTAGE_DUAL_NETWORK_USE_FLOW_NULL0,
		/** 双链路使用了数据流量累计>5M */
		BACKSTAGE_DUAL_NETWORK_USE_FLOW_5M,
		/** 双链路使用了数据流量累计>20 */
		BACKSTAGE_DUAL_NETWORK_USE_FLOW_20M,
		/** 双链路使用了数据流量累计>50 */
		BACKSTAGE_DUAL_NETWORK_USE_FLOW_50M,
		/** 双链路使用了数据流量累计>150M */
		BACKSTAGE_DUAL_NETWORK_USE_FLOW_150M,
		/** 点击并网加速关 */
		FLOATING_WINDOW_DUAL_NETWORK_SWITCH_OFF,
		/** 点击并网加速开 */
		FLOATING_WINDOW_DUAL_NETWORK_SWITCH_ON,
		/** 发现内嵌SDK的游戏 */
		SMOBA_QQ_COM_AWX {
			@Override
			boolean doesReportUmeng() {
				return false;
			}
		},
		/** 对腾讯域名的解析结果 */
		SMOBA_QQ_COM_APP {
			@Override
			boolean doesReportUmeng() {
				return false;
			}
		},
		/** 底层通知被禁用需断开小钥匙时，上报机型 */
		CLOSE_VPN_BY_PROXY_MODEL,
		/** 统计一下“当底层通知权限被禁需关闭VPN时”，Java层的检测结果 */
		BACKSTAGE_NET_RESULT_WHNE_PROXY_RIGHT_DISABLE,
		/** 网络权限被禁用，VPN被断开 */
		NETWORK_PERMISSION_DENY,
		/** 手机网络无法识别 */
		NETWORK_NOT_IDENTIFY,
		/** 进入时发现上次停止残留service */	
		SERVICE_LAST_LEFT,
		/** vivo必读点击*/	
		VIVO_README,
		/** 小米必读点击 */	
		XIAOMI_README,
		/** 小米神隐点击 */	
		XIAOMI_SHENYIN,
		/**WiFi优化累计使用流量 */
		BACKSTAGE_DUAL_NETWORK_USE_FLOW,
		/**WiFi优化累计使用流量（UDP） */
		BACKSTAGE_DUAL_NETWORK_USE_FLOW_UDP,
		/**WiFi优化累计使用流量（TCP） */
		BACKSTAGE_DUAL_NETWORK_USE_FLOW_TCP,
		/** 网络异常出现*/
		NETWORK_CHECK,
		/** 推送消息出现*/
		NOTIFICATION_PUSH_SHOWUP,
		/**推送消息点击*/
		NOTIFICATION_PUSH_CLICK,
		/** ROOT模式切换重启提示*/
		ACC_ROOTMODE_REBOOT_TIP_SHOWUP,
		/** 大悬浮窗首页icon点击*/
		FLOATING_WINDOW_ICON_CLICK,
		/** 消息中心点击进入*/
		INTERACTIVE_MESSAGE_IN,
		/** 消息中心消息点击*/
		INTERACTIVE_MESSAGE_CLICK,
		/** 消息中心消息删除*/
		INTERACTIVE_MESSAGE_DELETE,
		/** 应用内打开网页*/
		INTERACTIVE_MESSAGE_WEBPAGE_OPEN,
		/** 进入注册登录页*/
		ACCOUNT_LOGIN_IN,
		/** 注册登录页登录按钮点击*/
		ACCOUNT_LOGIN_CLICK,
		/** 登录成功*/
		ACCOUNT_LOGIN_SUCCESS,
		/** VIP鉴权失败*/
		ACCOUNT_RIGHTCERTIFICATION_FAILED,
		/** 第一次加速登录提示*/
		ACCOUNT_LOGIN_PROMPT_ACC_FIRSTTIME,
		/** 选择套餐登录提示*/
		ACCOUNT_LOGIN_PROMPT_VIP_CHOOSE,
		/** 会员中心进入*/
		VIPCENTER_IN,
		/** 套餐VIP季卡选项点击*/
		VIPCENTER_QUARTER_CLICK,
		/** 套餐VIP月卡选项点击*/
		VIPCENTER_MONTH_CLICK,
		/** 套餐VIP免费试用点击*/
		VIPCENTER_FREE_CLICK,
		/** 套餐VIP季卡购买成功*/
		VIPCENTER_QUARTER_SUCCESS,
		/** 套餐VIP月卡购买成功*/
		VIPCENTER_MONTH_SUCCESS,
		/** 套餐VIP免费试用成功*/
		VIPCENTER_FREE_SUCCESS,
		/** 新安装用户购买提示弹窗出现*/
		VIPCENTER_GUIDEPROMPT_NEWUSER,
		/** 升级用户购买提示弹窗出现*/
		VIPCENTER_GUIDEPROMPT_UPGRADEUSER,
		/** 支付方式微信点击*/
		VIPCENTER_WAY_WECHAT_CLICK,
		/** 支付方式支付宝点击*/
		VIPCENTER_WAY_ALIPAY_CLICK,
		/** 支付中关闭按钮点击*/
		VIPCENTER_PAYMENTS_CLOSE_CLICK,
		/** 支付成功*/
		VIPCENTER_PAYMENTS_SUCCESS,
		/** 支付失败*/
		VIPCENTER_PAYMENTS_FAILED,
		/**登陆方式 */
		ACCOUNT_LOGIN_WAY,
		/**绑定手机号*/
		ACCOUNT_THIRDPART_PHONE_BIND,
		/**WiFi优化每日累计消耗流量>0MB*/
		BACKSTAGE_DUAL_NETWORK_DAY_FLOW_OVER0,

        // ==== 下面四个是产品妹纸起的名字，有问题不要找研发撕 ====
        /** 启动游戏时设备仅开启WiFi */
        ACC_GAME_ONTOP_NET_WIFI {
            @Override
            boolean doesReportSubao() {
                return false;
            }
        },
        /** 启动游戏时设备仅开启移动数据 */
        ACC_GAME_ONTOP_NET_FLOW {
            @Override
            boolean doesReportSubao() {
                return false;
            }
        },
        /** 启动游戏时设备同时开启WiFi与移动数据 */
        ACC_GAME_ONTOP_NET_WIFI_AND_FLOW {
            @Override
            boolean doesReportSubao() {
                return false;
            }
        },
        /** 启动游戏时设备WiFi或移动数据均未开启 */
        ACC_GAME_ONTOP_NET_NULL {
            @Override
            boolean doesReportSubao() {
                return false;
            }
        },
        /** 启动游戏时开着WiFi但取Mobile开关状态失败 */
        ACC_GAME_ONTOP_WIFI_AND_MOBILE_UNKNOWN {
            @Override
            boolean doesReportSubao() {
                return false;
            }
        },
        ;

		private final static String[] eventNames;

		static {
			// 初始化ID到字串的映射表
			Event[] values = Event.values();
			eventNames = new String[values.length];
			Locale locale = Locale.US;
			for (int i = values.length - 1; i >= 0; --i) {
				Event e = values[i];
				if (e.ordinal() != i) {
					throw new RuntimeException("Invalid StatisticDefault.Event Defines: " + e);
				}
				eventNames[i] = e.name().toLowerCase(locale);
			}
		}

		boolean doesReportUmeng() {
			return true;
		}

		boolean doesReportSubao() {
			return true;
		}

		public final String getEventName() {
			int idx = this.ordinal();
			return eventNames[idx];
		}
	}

	public static interface Submitter {
		public void execute(Context context, Statistic.Event e);

		public void execute(Context context, Statistic.Event e, String params);
	}

	private static class DefaultSubmitter implements Submitter {

		@Override
		public void execute(Context context, Event e) {
			Statistic.addEvent(context, e);
		}

		@Override
		public void execute(Context context, Event e, String params) {
			Statistic.addEvent(context, e, params);
		}

	}

	public static final Submitter DEFAULT_SUBMITTER = new DefaultSubmitter();
	
	private static String deviceId;
	
	public static void init(Context context) {
		deviceId = DeviceId.make(context); 
	}

	public static void onActivityResume(Context context) {
		MobclickAgent.onResume(context);
	}

	public static void onActivityPause(Context context) {
		MobclickAgent.onPause(context);
	}

	public static void addEvent(Context context, Event event) {
		addEvent(context, event, null);
	}
	
	public static void addEvent(Context context, Event event, boolean isFromJPush) {
		String param = GlobalDefines.EVENT_PARAM_FROM_APP ;
		
		if(isFromJPush){
			param = GlobalDefines.EVENT_PARAM_FROM_JPUSH ;
		} 
		
		addEvent(context, event, param); 
	}

	public static void addEvent(Context context, Event event, String strParam) {
		if (TextUtils.isEmpty(strParam)) {
			if (event.doesReportUmeng()) {
				MobclickAgent.onEvent(context, event.getEventName());
			}
		} else {
			if (event.doesReportUmeng()) {
				MobclickAgent.onEvent(context, event.getEventName(), strParam);
			}
		}
		if (event.doesReportSubao()) {
			UserActionManager.getInstance().addAction(System.currentTimeMillis() / 1000, event.getEventName(), strParam);
		}
	}
	
	public static String getDeviceId() {
		return deviceId;
	}

	public static class DeviceId {

		private static boolean checkPermission(Context context, String permission) {
			PackageManager pm = context.getPackageManager();
			if (pm != null && pm.checkPermission(permission, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
				return true;
			} else {
				return false;
			}
		}

		static String make(Context context) {
			try {
				String device_id = null;
				if (checkPermission(context, permission.READ_PHONE_STATE)) {
					try {
						android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
						if (tm != null) {
							device_id = tm.getDeviceId();
						}
					} catch (SecurityException se) {}
				}
				String mac = null;
				if (checkPermission(context, permission.ACCESS_WIFI_STATE)) {
					try {
						android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);
						if (wifi != null) {
							WifiInfo info = wifi.getConnectionInfo();
							if (info != null) {
								mac = info.getMacAddress();
							}
						}
					} catch (SecurityException se) {}
				}
				if (TextUtils.isEmpty(device_id)) {
					device_id = mac;
				}
				if (TextUtils.isEmpty(device_id)) {
					device_id = android.provider.Settings.Secure.getString(context.getContentResolver(),
						android.provider.Settings.Secure.ANDROID_ID);
				}
				return make(device_id, mac);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "{}";
		}

		public static String make(String deviceId, String mac) {
			StringWriter sw = new StringWriter(128);
			JsonWriter writer = null;
			try {
				writer = new JsonWriter(sw);

				writer.beginObject();
				if (null != deviceId) {
					writer.name("device_id").value(deviceId);
				}
				if (null != mac) {
					writer.name("mac").value(mac);
				}
				writer.endObject();
				return sw.toString();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				Misc.safeClose(writer);
			}
			return "{}";
		}
	}

}
