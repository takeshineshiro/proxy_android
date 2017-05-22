package cn.wsds.gamemaster.service;


public class VPNGlobalDefines {

	public enum CloseReason {
		/** 用户点击主菜单里的“关闭” */
		SETTING_CLOSE("主页关闭加速"),
		/** 用户在桌面快捷方式里关闭 */
		DESKTOP_SHORTCUT_CLOSE("快捷方式关闭加速"),
		/** Proxy层通知暂停 */
		BY_PROXY("后台通知"),
		/** 程序正常退出 */
		APP_EXIT("主页退出"),
		/** VpnService destroy */
		VPN_SERVICE_DESTROY("服务终止"),
		/** VPN授权被吊销 */
		VPN_REVOKE("小钥匙处断开"),
		/** 测试关闭 */
		DEBUG("测试");

		public final String desc;

		CloseReason(String desc) {
			this.desc = desc;
		}
		
		public static CloseReason fromOrdinal(int ordinal) {
			if (ordinal < 0) {
				return null;
			}
			CloseReason[] all = CloseReason.values();
			if (ordinal >= all.length) {
				return null;
			}
			return all[ordinal];
		}
	}

	/** vpn broadcast action */
	/** broadcast receiver action 标识： vpn service 被创建 */
	public static final String ACTION_VPN_SERVICE_CREATED = "cn.wsds.vpn.action.SERVICE_CREATED";
	/** broadcast receiver action 标识 ： 开启vpn加速服务 */
	public static final String ACTION_VPN_OPEN = "cn.wsds.vpn.action.OPEN";
	/** broadcast receiver action 标识 ： 开启vpn加速服务失败 */
	public static final String ACTION_VPN_START_FAILED = "cn.wsds.vpn.action.START_FAILED";
	/** broadcast receiver action 标识 ： 关闭vpn加速服务 */
	public static final String ACTION_VPN_CLOSE = "cn.wsds.vpn.action.CLOSE";
	/** broadcast receiver action 标识 ： 关闭vpn，触发AccelManager的相应处理 */
	public static final String ACTION_VPN_ACCEL_MANAGER_CLOSE = "cn.wsds.vpn.action.ACCEL_MANAGER_CLOSE";
	/** broadcast receiver action 标识 ： 上报vpn服务相关事件 */
	public static final String ACTION_VPN_ADD_EVENT = "cn.wsds.vpn.action.ADD_EVENT";
	/** broadcast receiver action ， Key ： vpn服务关闭原因 */
	public static final String KEY_ACTION_VPN_CLOSE_REASON = "close_reason";
	/** broadcast receiver action ，Key ：上报事件id */
	public static final String KEY_VPN_EVENT_ID = "event_id";
	/** broadcast receiver action ，Key ：上报事件的参数 */
	public static final String KEY_ACTION_VPN_EVENT_PARAM = "event_param";

	public enum JniMethod {
		/** updateState(int state) */
		METHOD_UPDATE_STATE,

		/** onGameDelayDetectResult(final int delayMilliseconds) */
		METHOD_ON_GAME_DELAY_DETECT_ERESULT,

		/** saveToFile(String filename, String data) */
		METHOD_SAVE_TO_FILE,

		/** onGameConnected(final int uid, final int connTime) */
		METHOD_ON_GAME_CONNECTED,

		/** onGameLog(final String log) */
		METHOD_ON_GAME_LOG,

		/** onLinkMessage(final String json) */
		METHOD_ON_LINK_MESSAGE,
		
//		METHOD_ON_LINK_MESSAGE_BEGIN,
		
		METHOD_ON_LINK_END,

		/** onRepairConnection(int uid, int taskId, boolean succ, int reconnCount) */
		METHOD_ON_REPAIR_CONNECTION,

		/** onCloseConnect(final int errorCode) */
		METHOD_ON_CLOSE_CONNECT,

		/** onNodeDetect(int code, int uid, boolean succeed) */
		METHOD_ON_NODE_DETECT,

		/** onCreateConnect(final int errCode, final boolean transparent) */
		METHOD_ON_CREATE_CONNECT,

		/** onNode2GameServerDelay(final int uid, final int delay) */
		METHOD_ON_NODE2_GAME_SERVER_DELAY,

		/** onDirectTrans(final int uid, final int port, final int delay) */
		METHOD_ON_DIRECT_TRANS,
		
		/** openQosAccel(int id, String node ,String srcIp, String accessToken ,int srcPort, String 
		dstIp, int dstPort, String protocol, int timeSeconds */
		METHOD_OPEN_QOS_ACCEL,

		/** modifyQosAccel(int id, String node, String accessToken, int timeSeconds) */
		METHOD_MODIFY_QOS_ACCEL,

		/** closeQosAccel(int id, String node, String accessToken) */
		METHOD_CLOSE_QOS_ACCEL,

		/** onQPPVicePathFlow(final int uid, final int flowBytes, final int totalTime, final int enableTime) */
		METHOD_QPP_VICE_PATH_FLOW,

		/** onTencentSGameDelayInfo(int arg0, int arg1, float arg2) */
		METHOD_ON_TENCENT_SGAME_DELAY_INFO,

		/** onQosMessage(String jsonFromJNI) */
		METHOD_ON_QOS_MESSAGE,
		 
		/** onNetMeasureMessage(String jsonFromJNI) */
		METHOD_ON_NET_MEASURE_MESSAGE,
		
		/** onMessageUserIdUpdate() */
		METHOD_ON_MESSAGE_USER_ID_UPDATE,

		/** onGetJWTTokenResult(String jwtToken, long expires, String shortId,
		 int userStatus, String expiredTime, boolean result,int code) */
		METHOD_ON_GET_JWT_TOKEN_RESULT,

		/** onGetTokenResult(String ip, byte[] token, int length, int expires,
		 boolean result, int code) */
		METHOD_ON_GET_TOKEN_RESULT,

		/** onGetUserAccelStatusResult(String shortId, int status, String expiredTime,
		 boolean result, int code) */
		METHOD_ON_GET_USER_ACCEL_STATUS_RESULT,

		/** onGetUserConfigResult(String config, int code, boolean result) */
		METHOD_ON_GET_USER_CONFIG_RESULT,

		/** onWifiAccelStateChange */
		METHOD_ON_WIFI_ACCEL_STATE_CHANGE;
		
		public static JniMethod fromOrdinal(int ordinal) {
			if (ordinal < 0) {
				return null;
			}
			JniMethod[] all = JniMethod.values();
			if (ordinal >= all.length) {
				return null;
			}
			return all[ordinal];
		}
	}

	public enum VPNEvent {
		NETWORK_VPN_STOP_SEASON,
		CLOSE_VPN_BY_PROXY_MODEL
	}
	
}
