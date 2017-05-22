package cn.wsds.gamemaster.ui.floatwindow;

import android.content.Context;
import android.graphics.Bitmap;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;
import com.subao.resutils.CommonUIResources;

/**
 * 悬浮窗系统所用到的一些公用定义
 */
public class FloatWindowCommon {

	/**
	 * 判断是否网络异常
	 * 
	 * @return true: 网络异常， false: 网络正常
	 */
	public static boolean isNetworkException() {
		return NetManager.getInstance().isDisconnected()
		//				|| NetManager.getInstance().isWapCurrent(AppMain.getContext())
			|| isDelayException();
	}

	private static boolean isDelayException() {
		if (FloatWindowCommon.isCurrent2G()) {
			return false;
		}
		long delay = GameManager.getInstance().getFirstSegmentNetDelay();
		return delay >= GlobalDefines.NET_DELAY_TIMEOUT || (delay < 0 && delay != GlobalDefines.NET_DELAY_TEST_WAIT);
	}

	public static boolean isNetDelayException(int value) {
		return value >= GlobalDefines.NET_DELAY_TIMEOUT || (value < 0 && value != GlobalDefines.NET_DELAY_TEST_WAIT);
	}

	/**
	 * 机器人贴图（不含“VPN未开启时的机器人贴图”）
	 */
	static class RobotList {
		private final Bitmap[] items;

		public RobotList(Context context ,boolean isNormalIcon) {
			if(isNormalIcon){
				items = new Bitmap[4];
				items[0] = FloatWindow.commonUIRes.getBitmap(context, R.drawable.suspension_robot_nor_fire_l_big);
				items[1] = FloatWindow.commonUIRes.getBitmap(context, R.drawable.suspension_robot_nor_fire_s_big);
				items[2] = FloatWindow.commonUIRes.getBitmap(context, R.drawable.suspension_robot_hard_fire_l);
				items[3] = FloatWindow.commonUIRes.getBitmap(context, R.drawable.suspension_robot_hard_fire_s);			
			}else{
				items = new Bitmap[1];
				items[0] = FloatWindow.commonUIRes.getBitmap(context, R.drawable.floating_window_abnormal_state_prompt_huge);
			}
		}
		
		/**
		 * 取“机器人图片”指定帧
		 * 
		 * @param index
		 *            第几帧。取值为负将被取反，越界值将被取模以保证不越界
		 * @return 图片
		 */
		public Bitmap get(int index) {
			if (index < 0) {
				index = -index;
			}
			return items[index % items.length];
		}

	}

	final static CommonUIResources uiRes = new CommonUIResources();

	public static boolean isCurrent2G() {
		return NetTypeDetector.NetType.MOBILE_2G == NetManager.getInstance().getCurrentNetworkType();
	}

	public enum OutColorType {
		Green, Yello, Red, Grey
	}

	public enum MobileNetType {
		Type_2G, Type_3G, Type_4G //, UNKNOWN,
	}

	public static boolean isSupportHunterSkin(GameInfo info) {
		return info != null && info.getAppLabel().contains("时空猎人");
	}
	
//	public static boolean isCurrenVisibleHunterSkin(GameInfo gameInfo) {
//		if (gameInfo == null) {
//			return false;
//		}
//		return ConfigManager.getInstance().isFloatwindowHunterSkin() && isSupportHunterSkin(gameInfo);
//	}
}
