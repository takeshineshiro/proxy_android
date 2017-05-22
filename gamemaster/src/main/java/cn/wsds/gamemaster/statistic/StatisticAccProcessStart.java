package cn.wsds.gamemaster.statistic;

public class StatisticAccProcessStart {
	
	private static final String TAG = "StatisticAccProcessStar";
	
	// 网络异常
	public static final String STEP_PROMPT_WIFI_AP_OPENED = "开启WiFi热点"; // 提示“WiFi热点开启”
	public static final String STEP_PROMPT_NETWORK_REJECT = "网络权限被禁"; // 提示“无网络权限”
	public static final String STEP_PROMPT_WAP = "提示WAP"; // 提示“WAP方式”
	//  root 流程标号
	public static final String STEP_ROOT_MISSING_MODULE = "ROOT模块缺失"; // 
	public static final String STEP_ROOT_IMPOWER = "获取ROOT授权"; // 授权
	public static final String STEP_ROOT_IMPOWER_ERROR = "ROOT授权失败"; // root 授权出错
	public static final String STEP_ROOT_IMPOWER_REJECT = "ROOT授权被拒"; // root 授权被拒绝
	public static final String STEP_ROOT_IMPOWER_SUCCEED = "ROOT授权成功"; // root 授权被拒绝
	public static final String STEP_ROOT_ACCEL_ERROR = "ROOT开启失败"; // Root 模式开启失败
	public static final String STEP_ROOT_ACCEL_SUCCEED = "ROOT开启成功"; // 授权成功
	public static final String STEP_ROOT_PROMPT_VPN = "提示使用VPN开启"; // 提示使用VPN开启
	// VPN 流程标号
	public static final String STEP_VPN_MISSING_MODULE = "VPN模块缺失"; 
	public static final String STEP_VPN_IMPOWER_REMIND = "VPN授权提示"; // 授权提示
	public static final String STEP_VPN_IMPOWER_REJECT = "VPN授权被拒"; // 授权被拒绝
	public static final String STEP_VPN_IMPOWER_ERROR = "VPN授权失败"; // 授权被拒绝
	public static final String STEP_VPN_IMPOWER_SUCCEED = "VPN授权成功"; // 授权被拒绝
	public static final String STEP_VPN_IMPOWER_REJECTED_PORMPT = "VPN授权被拒后再次提示授权对话框"; // 再次引导授权对话框取消
	public static final String STEP_VPN_ACCEL_SUCCEED = "VPN开启成功"; // 加速成功
	public static final String STEP_VPN_ACCEL_ERROR = "VPN开启失败"; // 加速失败
	public static final String STEP_VPN_PROMPT_ROOT = "提示使用ROOT开启"; // 提示使用VPN开启

	private final static StatisticAccProcessStart instance = new StatisticAccProcessStart();
	private StatisticAccProcessStart() {}

	public static StatisticAccProcessStart getInstance() {
		return instance;
	}

	public void addStep(Object step) {
		createFlow();

	}

	private void createFlow() {
	}

	public void end(){

	}
	
	public void end(Object... steps) {
		if (steps != null && steps.length != 0) {
			createFlow();
			for (Object step : steps) {

			}
		}
		end();
	}

}
