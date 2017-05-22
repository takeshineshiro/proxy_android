package cn.wsds.gamemaster.ui.floatwindow;

import android.content.Context;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.FloatWindowMeasure;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowCommon.MobileNetType;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowCommon.OutColorType;

import com.subao.utils.MetricsUtils;

/**
 * 小悬浮窗配置参数
 */
class FloatwindowInGameConfig {

	// 宽高 单位像素
	private int widthPx, heightPx;
	// 延时 相关
	private int delayValueTextSizeSp;
	private int delayUnitTextSizeSp;
	private int delayViewHeightPx;

	// 资源相关

	private int ringLayoutResource;
	private int earLayoutResource;
	private int claimResource;
	private int accelResource;
	
	/** 等待状态：转动的外环 */
	private int waitRingResource;
	
	/** 火箭动画图片 */
	private int rocketResourceAnimation;
	
	/** 火箭静止图片 */
	private int rocketResourceStatic;
	
	private int boxSignalResource;
	private int wifiSignalResource;
	private int ringResourceGreen;
	private int ringResourceYello;
	private int ringResourceRed;
	private int ringResourceGrey;
	private int boxRectResourceGreen;
	private int boxRectResourceYello;
	private int boxRectResourceRed;
	private int boxRectResourceGrey;
	private int boxNetTypeResource2G;
	private int boxNetTypeResource3G;
	private int boxNetTypeResource4G;
	
	private int pxEarNetSignalLeft, pxEarNetTypeLeft, pxEarWifiLeft;
	private int pxEarWidth,pxEarHeight;

	private FloatwindowInGameConfig() {}

	/**
	 * 设置宽度
	 * 
	 * @param widthdp
	 *            宽度的dp值
	 */
	private void setWidthdp(int widthdp) {
		this.widthPx = MetricsUtils.dp2px(AppMain.getContext(), widthdp);
	}

	private void setDelayViewHeightPx(int delayViewHeightDp) {
		this.delayViewHeightPx = MetricsUtils.dp2px(AppMain.getContext(),
				delayViewHeightDp);
	}
	
	public static FloatwindowInGameConfig createConfig(FloatWindowMeasure.Type type) {
		return createNormalConfig(type);
	}
	
//	/**
//	 * 时空猎人皮肤
//	 * @param type
//	 * @return
//	 */
//	public static FloatwindowInGameConfig createHunterConfig(FloatWindowMeasure.Type type) {
//		switch (type) {
//		case LARGE:
//			return createHunterLargeConfig();
//		case MINI:
//			return createHunterMiniConfig();
//		case NORMAL:
//		default:
//			return createHunterConfig();
//		}
//	}
//	
//	private static FloatwindowInGameConfig createHunterConfig() {
//		FloatwindowInGameConfig config = new FloatwindowInGameConfig();
//		config.setWidthdp(38);
//		config.heightPx = config.widthPx;
//		config.delayValueTextSizeSp = 12;
//		config.delayUnitTextSizeSp = 9;
//		config.setDelayViewHeightPx(22);
//		
//		config.ringLayoutResource = R.layout.small_float_window_ring_hunter;
//		config.earLayoutResource = R.layout.small_float_window_box_hunter;
//		config.claimResource = R.drawable.floating_window_abnormal_state_prompt_hunter;
//		config.accelResource = R.drawable.floating_window_not_open_time_delay_open_text_hunter;
//		config.waitRingResource = R.drawable.suspension_circle_loading_hunter;
//		config.rocketResourceAnimation = R.drawable.rocket_animation;
//		config.rocketResourceStatic = R.drawable.suspension_robot_nor_fire_l_hunter;
//		config.boxSignalResource = R.drawable.floating_window_open_state_full_signal_hunter;
//		config.wifiSignalResource = R.drawable.floating_window_open_state_wifi_signal_three_hunter;
//		
//		config.ringResourceGreen = R.drawable.floating_window_open_state_hunter;
//		config.ringResourceYello = R.drawable.floating_window_delay_state_hunter;
//		config.ringResourceRed = R.drawable.floating_window_abnormal_state_hunter;
//		config.ringResourceGrey = R.drawable.floating_window_not_open_state_hunter;
//		
//		config.boxRectResourceGreen = R.drawable.floating_window_open_time_delay_right_hunter;
//		config.boxRectResourceYello = R.drawable.floating_window_open_time_delay_right_hunter;
//		config.boxRectResourceRed = R.drawable.floating_window_open_time_delay_right_hunter;
//		config.boxRectResourceGrey = R.drawable.floating_window_open_time_delay_right_hunter;
//		
//		config.boxNetTypeResource2G = R.drawable.floating_window_open_state_2g_hunter;
//		config.boxNetTypeResource3G = R.drawable.floating_window_open_state_3g_hunter;
//		config.boxNetTypeResource4G = R.drawable.floating_window_open_state_4g_hunter;
//		
//		Context context = AppMain.getContext();
//		config.pxEarNetSignalLeft = MetricsUtils.dp2px(context, 20);
//		config.pxEarNetTypeLeft = MetricsUtils.dp2px(context, 4);
//		config.pxEarWifiLeft = MetricsUtils.dp2px(context, 32);
//		config.pxEarWidth = MetricsUtils.dp2px(context, 65);
//		config.pxEarHeight = MetricsUtils.dp2px(context, 38);
//		return config;
//	}
//
//	private static FloatwindowInGameConfig createHunterLargeConfig() {
//		FloatwindowInGameConfig config = new FloatwindowInGameConfig();
//		config.setWidthdp(46);
//		config.heightPx = config.widthPx;
//		config.delayValueTextSizeSp = 18;
//		config.delayUnitTextSizeSp = 12;
//		config.setDelayViewHeightPx(30);
//		
//		config.ringLayoutResource = R.layout.small_float_window_ring_hunter;
//		config.earLayoutResource = R.layout.small_float_window_box_hunter;
//		config.claimResource = R.drawable.floating_window_abnormal_state_prompt_huge_hunter;
//		config.accelResource = R.drawable.floating_window_not_open_time_delay_open_text_huge_hunter;
//		config.waitRingResource = R.drawable.suspension_circle_loading_huge_hunter;
//		config.rocketResourceAnimation = R.drawable.rocket_animation_huge;
//		config.rocketResourceStatic = R.drawable.suspension_robot_nor_fire_l_huge;
//		config.boxSignalResource = R.drawable.floating_window_open_state_full_signal_huge_hunter;
//		config.wifiSignalResource = R.drawable.floating_window_open_state_wifi_signal_three_huge_hunter;
//		
//		config.ringResourceGreen = R.drawable.floating_window_open_state_huge_hunter;
//		config.ringResourceYello = R.drawable.floating_window_delay_state_huge_hunter;
//		config.ringResourceRed = R.drawable.floating_window_abnormal_state_huge_hunter;
//		config.ringResourceGrey = R.drawable.floating_window_not_open_state_huge_hunter;
//		
//		config.boxRectResourceGreen = R.drawable.floating_window_open_time_delay_right_huge_hunter;
//		config.boxRectResourceYello = R.drawable.floating_window_open_time_delay_right_huge_hunter;
//		config.boxRectResourceRed = R.drawable.floating_window_open_time_delay_right_huge_hunter;
//		config.boxRectResourceGrey = R.drawable.floating_window_open_time_delay_right_huge_hunter;
//		
//		config.boxNetTypeResource2G = R.drawable.floating_window_open_state_2g_huge_hunter;
//		config.boxNetTypeResource3G = R.drawable.floating_window_open_state_3g_huge_hunter;
//		config.boxNetTypeResource4G = R.drawable.floating_window_open_state_4g_huge_hunter;
//		
//		Context context = AppMain.getContext();
//		config.pxEarNetSignalLeft = MetricsUtils.dp2px(context, 25);
//		config.pxEarNetTypeLeft = MetricsUtils.dp2px(context, 5);
//		config.pxEarWifiLeft = MetricsUtils.dp2px(context, 40);
//		config.pxEarWidth = MetricsUtils.dp2px(context, 86);
//		config.pxEarHeight = MetricsUtils.dp2px(context, 48);
//		return config;
//	}
//
//	public static FloatwindowInGameConfig createHunterMiniConfig() {
//		FloatwindowInGameConfig config = new FloatwindowInGameConfig();
//		config.setWidthdp(30);
//		config.heightPx = config.widthPx;
//		config.delayValueTextSizeSp = 8;
//		config.delayUnitTextSizeSp = 6;
//		config.setDelayViewHeightPx(16);
//		
//		config.ringLayoutResource = R.layout.small_float_window_ring_hunter;
//		config.earLayoutResource = R.layout.small_float_window_box_hunter;
//		config.claimResource = R.drawable.floating_window_abnormal_state_prompt_mini_hunter;
//		config.accelResource = R.drawable.floating_window_not_open_time_delay_open_text_mini_hunter;
//		config.waitRingResource = R.drawable.suspension_circle_loading_mini_hunter;
//		config.rocketResourceAnimation = R.drawable.rocket_animation_mini;
//		config.rocketResourceStatic = R.drawable.suspension_robot_nor_fire_l_mini_hunter;
//		config.boxSignalResource = R.drawable.floating_window_open_state_full_signal_mini_hunter;
//		config.wifiSignalResource = R.drawable.floating_window_open_state_wifi_signal_three_mini_hunter;
//		
//		config.ringResourceGreen = R.drawable.floating_window_open_state_mini_hunter;
//		config.ringResourceYello = R.drawable.floating_window_delay_state_mini_hunter;
//		config.ringResourceRed = R.drawable.floating_window_abnormal_state_mini_hunter;
//		config.ringResourceGrey = R.drawable.floating_window_not_open_state_mini_hunter;
//		
//		config.boxRectResourceGreen = R.drawable.floating_window_open_time_delay_right_mini_hunter;
//		config.boxRectResourceYello = R.drawable.floating_window_open_time_delay_right_mini_hunter;
//		config.boxRectResourceRed = R.drawable.floating_window_open_time_delay_right_mini_hunter;
//		config.boxRectResourceGrey = R.drawable.floating_window_open_time_delay_right_mini_hunter;
//		
//		config.boxNetTypeResource2G = R.drawable.floating_window_open_state_2g_mini_hunter;
//		config.boxNetTypeResource3G = R.drawable.floating_window_open_state_3g_mini_hunter;
//		config.boxNetTypeResource4G = R.drawable.floating_window_open_state_4g_mini_hunter;
//		
//		Context context = AppMain.getContext();
//		config.pxEarNetSignalLeft = MetricsUtils.dp2px(context, 15);
//		config.pxEarNetTypeLeft = MetricsUtils.dp2px(context, 3);
//		config.pxEarWifiLeft = MetricsUtils.dp2px(context, 24);
//		config.pxEarWidth = MetricsUtils.dp2px(context, 52);
//		config.pxEarHeight = MetricsUtils.dp2px(context, 30);
//		return config;
//	}

	/**
	 * 正常皮肤
	 * @param type
	 * @return
	 */
	public static FloatwindowInGameConfig createNormalConfig(
			FloatWindowMeasure.Type type) {
		switch (type) {
		case LARGE:
			return createNormalLargeConfig();
		case MINI:
			return createNormalMiniConfig();
		case NORMAL:
		default:
			return createNormalConfig();
		}
	}

	private static FloatwindowInGameConfig createNormalConfig() {
		FloatwindowInGameConfig config = new FloatwindowInGameConfig();
		config.setWidthdp(38);
		config.heightPx = config.widthPx;
		config.delayValueTextSizeSp = 14;
		config.delayUnitTextSizeSp = 10;
		config.setDelayViewHeightPx(25);
		
		config.ringLayoutResource = R.layout.small_float_window_ring;
		config.earLayoutResource = R.layout.small_float_window_box;
		config.claimResource = R.drawable.floating_window_abnormal_state_prompt;
		config.accelResource = R.drawable.floating_window_not_open_time_delay_open_text;
		config.waitRingResource = R.drawable.suspension_circle_loading;
		config.rocketResourceAnimation = R.drawable.rocket_animation;
		config.rocketResourceStatic = R.drawable.suspension_robot_nor_fire_l;
		config.boxSignalResource = R.drawable.floating_window_open_state_full_signal;
		config.wifiSignalResource = R.drawable.floating_window_open_state_wifi_signal_three;
		
		config.ringResourceGreen = R.drawable.floating_window_open_state;
		config.ringResourceYello = R.drawable.floating_window_delay_state;
		config.ringResourceRed = R.drawable.floating_window_abnormal_state;
		config.ringResourceGrey = R.drawable.floating_window_not_open_state;
		
		config.boxRectResourceGreen = R.drawable.floating_window_open_time_delay_right;
		config.boxRectResourceYello = R.drawable.floating_window_delay_time_delay_right;
		config.boxRectResourceRed = R.drawable.floating_window_abnormal_time_delay_right;
		config.boxRectResourceGrey = R.drawable.floating_window_not_open_time_delay_right;
		
		config.boxNetTypeResource2G = R.drawable.floating_window_open_state_2g;
		config.boxNetTypeResource3G = R.drawable.floating_window_open_state_3g;
		config.boxNetTypeResource4G = R.drawable.floating_window_open_state_4g;
		
		Context context = AppMain.getContext();
		config.pxEarNetSignalLeft = MetricsUtils.dp2px(context, 24);
		config.pxEarNetTypeLeft = MetricsUtils.dp2px(context, 4);
		config.pxEarWifiLeft = MetricsUtils.dp2px(context, 32);
		config.pxEarWidth = MetricsUtils.dp2px(context, 65);
		config.pxEarHeight = MetricsUtils.dp2px(context, 38);
		return config;
	}

	private static FloatwindowInGameConfig createNormalLargeConfig() {
		FloatwindowInGameConfig config = new FloatwindowInGameConfig();
		config.setWidthdp(48);
		config.heightPx = config.widthPx;
		config.delayValueTextSizeSp = 18;
		config.delayUnitTextSizeSp = 12;
		config.setDelayViewHeightPx(30);
		
		config.ringLayoutResource = R.layout.small_float_window_ring;
		config.earLayoutResource = R.layout.small_float_window_box;
		config.claimResource = R.drawable.floating_window_abnormal_state_prompt_huge;
		config.accelResource = R.drawable.floating_window_not_open_time_delay_open_text_huge;
		config.waitRingResource = R.drawable.suspension_circle_loading_huge;
		config.rocketResourceAnimation = R.drawable.rocket_animation_huge;
		config.rocketResourceStatic = R.drawable.suspension_robot_nor_fire_l_huge;
		config.boxSignalResource = R.drawable.floating_window_open_state_full_signal_huge;
		config.wifiSignalResource = R.drawable.floating_window_open_state_wifi_signal_three_huge;
		
		config.ringResourceGreen = R.drawable.floating_window_open_state_huge;
		config.ringResourceYello = R.drawable.floating_window_delay_state_huge;
		config.ringResourceRed = R.drawable.floating_window_abnormal_state_huge;
		config.ringResourceGrey = R.drawable.floating_window_not_open_state_huge;
		
		config.boxRectResourceGreen = R.drawable.floating_window_open_time_delay_right_huge;
		config.boxRectResourceYello = R.drawable.floating_window_delay_time_delay_right_huge;
		config.boxRectResourceRed = R.drawable.floating_window_abnormal_time_delay_right_huge;
		config.boxRectResourceGrey = R.drawable.floating_window_not_open_time_delay_right_huge;
		
		config.boxNetTypeResource2G = R.drawable.floating_window_open_state_2g_huge;
		config.boxNetTypeResource3G = R.drawable.floating_window_open_state_3g_huge;
		config.boxNetTypeResource4G = R.drawable.floating_window_open_state_4g_huge;
		
		Context context = AppMain.getContext();
		config.pxEarNetSignalLeft = MetricsUtils.dp2px(context, 30);
		config.pxEarNetTypeLeft = MetricsUtils.dp2px(context, 5);
		config.pxEarWifiLeft = MetricsUtils.dp2px(context, 40);
		config.pxEarWidth = MetricsUtils.dp2px(context, 86);
		config.pxEarHeight = MetricsUtils.dp2px(context, 48);
		return config;
	}

	public static FloatwindowInGameConfig createNormalMiniConfig() {
		FloatwindowInGameConfig config = new FloatwindowInGameConfig();
		config.setWidthdp(30);
		config.heightPx = config.widthPx;
		config.delayValueTextSizeSp = 10;
		config.delayUnitTextSizeSp = 8;
		config.setDelayViewHeightPx(20);
		
		config.ringLayoutResource = R.layout.small_float_window_ring;
		config.earLayoutResource = R.layout.small_float_window_box;
		config.claimResource = R.drawable.floating_window_abnormal_state_prompt_mini;
		config.accelResource = R.drawable.floating_window_not_open_time_delay_open_text_mini;
		config.waitRingResource = R.drawable.suspension_circle_loading_mini;
		config.rocketResourceAnimation = R.drawable.rocket_animation_mini;
		config.rocketResourceStatic = R.drawable.suspension_robot_nor_fire_l_mini;
		config.boxSignalResource = R.drawable.floating_window_open_state_full_signal_mini;
		config.wifiSignalResource = R.drawable.floating_window_open_state_wifi_signal_three_mini;
		
		config.ringResourceGreen = R.drawable.floating_window_open_state_mini;
		config.ringResourceYello = R.drawable.floating_window_delay_state_mini;
		config.ringResourceRed = R.drawable.floating_window_abnormal_state_mini;
		config.ringResourceGrey = R.drawable.floating_window_not_open_state_mini;
		
		config.boxRectResourceGreen = R.drawable.floating_window_open_time_delay_right_mini;
		config.boxRectResourceYello = R.drawable.floating_window_delay_time_delay_right_mini;
		config.boxRectResourceRed = R.drawable.floating_window_abnormal_time_delay_right_mini;
		config.boxRectResourceGrey = R.drawable.floating_window_not_open_time_delay_right_mini;
		
		config.boxNetTypeResource2G = R.drawable.floating_window_open_state_2g_mini;
		config.boxNetTypeResource3G = R.drawable.floating_window_open_state_3g_mini;
		config.boxNetTypeResource4G = R.drawable.floating_window_open_state_4g_mini;
		
		Context context = AppMain.getContext();
		config.pxEarNetSignalLeft = MetricsUtils.dp2px(context, 18);
		config.pxEarNetTypeLeft = MetricsUtils.dp2px(context, 3);
		config.pxEarWifiLeft = MetricsUtils.dp2px(context, 24);
		config.pxEarWidth = MetricsUtils.dp2px(context, 52);
		config.pxEarHeight = MetricsUtils.dp2px(context, 30);
		return config;
	}

	public int getWidthPx() {
		return this.widthPx;
	}

	public int getHeightPx() {
		return this.heightPx;
	}

	public int getDelayValueTextSizeSp() {
		return delayValueTextSizeSp;
	}

	public int getDelayUnitTextSizeSp() {
		return delayUnitTextSizeSp;
	}

	public int getDelayViewHeightPx() {
		return delayViewHeightPx;
	}

	public int getRingResource(OutColorType outColorType) {
		switch (outColorType) {
		case Green:
			return this.ringResourceGreen;
		case Grey:
			return this.ringResourceGrey;
		case Yello:
			return this.ringResourceYello;
		case Red:
		default:
			return this.ringResourceRed;
		}
	}

	public int getClaimResource() {
		return this.claimResource;
	}

	public int getAccelResource() {
		return this.accelResource;
	}

	/**
	 * 等待测速状态：转动的外环
	 */
	public int getWaitRingResource() {
		return this.waitRingResource;
	}

	/**
	 * 火箭的资源ID
	 * @param animation 为True时表示返回动画资源
	 */
	public int getRocketResource(boolean animation) {
		return animation ? this.rocketResourceAnimation : this.rocketResourceStatic;
	}

	public int getBoxRectResource(OutColorType outColorType) {
		switch (outColorType) {
		case Green:
			return this.boxRectResourceGreen;
		case Grey:
			return this.boxRectResourceGrey;
		case Yello:
			return this.boxRectResourceYello;
		case Red:
		default:
			return this.boxRectResourceRed;
		}
	}

	public int getBoxSignalResource() {
		return this.boxSignalResource;
	}

	public int getBoxNetTypeResource(MobileNetType mobileNetType) {
		switch (mobileNetType) {
		case Type_2G:
			return this.boxNetTypeResource2G;
		case Type_3G:
			return this.boxNetTypeResource3G;
		case Type_4G:
			return this.boxNetTypeResource4G;
		default:
			return this.boxNetTypeResource2G;
		}
	}

	public int getWifiSignalResource() {
		return this.wifiSignalResource;
	}

	public int getRingLayoutResource() {
		return this.ringLayoutResource;
	}

	public int getEarLayoutResource() {
		return this.earLayoutResource;
	}

	public int getPxEarNetSignalLeft() {
		return pxEarNetSignalLeft;
	}

	public int getPxEarNetTypeLeft() {
		return pxEarNetTypeLeft;
	}

	public int getPxEarWifiLeft() {
		return pxEarWifiLeft;
	}

	public int getPxEarWidth() {
		return pxEarWidth;
	}

	public int getPxEarHeight() {
		return pxEarHeight;
	}

}
