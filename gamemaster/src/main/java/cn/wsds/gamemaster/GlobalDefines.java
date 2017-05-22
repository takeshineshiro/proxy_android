package cn.wsds.gamemaster;

public class GlobalDefines {
	
	public static final boolean CHECK_MAIN_THREAD = false;
	
	/**
	 * 此值为true时，客户端按照正常流程进行必要的预判断（比如客户端判断出今日不能签到，则“签到”按钮灰掉不可点）。<br />
	 * 此值为false时，客户端完全不进行预判断，方便测试服务端问题
	 */
	public static boolean CLIENT_PRE_CHECK = true;

	/** 网络延迟大于等于此值表示超时 */
	public static final int NET_DELAY_TIMEOUT = 2000;

	/** 网络延迟等于此值，表示测速失败 */
	public static final int NET_DELAY_TEST_FAILED = -1;

	/** 网络延迟等于此值，表示等待测速中 */
	public static final int NET_DELAY_TEST_WAIT = -2;

	/** 断线重连最多尝试多少次？ */
	public static final int MAX_COUNT_OF_CONNECTION_REPAIR = 5;

	/**
	 * 这个值用于进行HTTP请求时，在URL上的“app=xxxx” 参数
	 */
	public static final String APP_NAME_FOR_HTTP_REQUEST = "game";

	/**
	 * startActivityForResult的request_code：VPN授权框
	 */
	public static final int START_ACTIVITY_REQUEST_CODE_VPN_IMPOWER = 0xaa01;

	/**
	 * startActivityForResult的request_code：内存清理
	 */
	public static final int START_ACTIVITY_REQUEST_CODE_MEMORY_CLEAN = 0xaa02;
	
	/**
	 * 进入某页面事件参数：从app页面进入
	 */
	public static final String EVENT_PARAM_FROM_APP = "from app activity" ;
	
	/**
	 * 进入某页面事件参数：从推送消息进入
	 */
	public static final String EVENT_PARAM_FROM_JPUSH = "from JPush message";

	//	/**
	//	 * startActivityForResult的request_code：网络诊断
	//	 */
	//	public static final int START_ACTIVITY_REQUEST_CODE_NET_DIAGNOSE = 0xaa03;
	public static final String TESTTAG = "MissIPTest" ;
	
	public enum ConfigName {
		/**
		 * 悬浮窗显示开关
		 */
		FLOAT_WINDOW_SWITCH,
	}
	
}
