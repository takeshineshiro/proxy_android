package cn.wsds.gamemaster.ui.accel;

import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpener.Listener;
import cn.wsds.gamemaster.ui.accel.AccelOpener.OpenSource;

/**
 * 执行加速开启操作的辅助类
 */
public class AccelOpenManager {
	
	private static final String TAG = "AccelOpenManager";
	public static AccelOpener createOpener(Listener listener, OpenSource openSource) {
		return createOpener(listener, openSource, null);
	}
	
	/**
	 * 根据Config里的当前设置（ROOT模式还是VPN模式），创建{@link AccelOpener}对象实例
	 * 
	 * @param listener
	 *            {@link AccelOpener}，开启结果的监听器
	 * @param openSource
	 *            {@link AccelOpener.OpenSource} 开启加速动作来源
	 * @param tester
	 *            {@link AccelOpener} 测试接口
	 */
	public static AccelOpener createOpener(Listener listener, OpenSource openSource, AccelOpener.Tester tester) {
		return new VpnOpener(listener, openSource, tester);
	}
	
	/**
	 * 当前是否是root模式
	 *  包括加速和未加速 
	 */
	public static boolean isRootModel(){
		return  ConfigManager.getInstance().isRootMode();
	}

	/** 当前已开启了加速吗？ */
	public static boolean isStarted() {
		//return VPNManager.getInstance().getAccelStatus() != VPNManager.AccelStatus.STOPPED;
		// FIXME: 17-3-29 hujd
   		return (VPNUtils.getAccelStatus(TAG)!= 0);
	}

	/** 关闭加速  */
	public static void close(CloseReason reason) {
		AccelOpener opener;
		// FIXME: 17-3-29 hujd
		if(VPNUtils.getAccelStatus(TAG) == 1){
			opener = new VpnOpener();
		}else{
			return ;
		}

		opener.close(reason);
		
		TriggerManager instance = TriggerManager.getInstance();
		instance.raiseAccelSwitchChanged(false);
		GameManager.getInstance().clearAllAccelTime();
	}
}
