package cn.wsds.gamemaster.event;

import java.util.List;
import java.util.UUID;

import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.AppProfile;

import com.subao.airplane.SwitchState;
import com.subao.common.net.NetTypeDetector;

public abstract class EventObserver {

	/** 安装了新应用 */
	public void onAppInstalled(com.subao.data.InstalledAppInfo info) {}

	/** 应用卸载 */
	public void onAppRemoved(String packageName) {}

	//网络变化
	public void onNetChange(NetTypeDetector.NetType netType) {}

	public void onWifiEnableChanged(int state) {}

	//Wifi热点状态改变
	public void onAPStateChange(int state) {}

	//飞行模式状态发生改变
	public void onAirplaneModeChanged(SwitchState state) {}

	/** 加速状态变化 */
	public void onAccelSwitchChanged(boolean state) {}

	//屏幕解锁
	public void onScreenOn() {}

	//锁屏
	public void onScreenOff() {}

	//VPN相关
	public void onVpnServiceCreate() {}

	/** 当VPN成功开启时触发 */
	public void onVPNOpen() {}

	public void onVPNClose() {}

	public void onStartVPNFailed(boolean impowerCancel) {} // 尝试启动VPN失败

	//启动新游戏
	public void onStartNewGame(GameInfo info) {}

	//顶层任务发生变化，如果是游戏，GameInfo则设置成相应的值
	public void onTopTaskChange(GameInfo info) {}

	//有新的回复
	public void onNewFeedbackReply(List<UUID> newReplyUUIDList) {}

	/**
	 * 底层通知：节点检测完成（成功或失败）
	 * 
	 * @param code
	 *            底层自定义代码，用于上报统计事件
	 * @param uid
	 *            哪个游戏？
	 * @param succeed
	 *            是否成功
	 */
	public void onNodeDetectResult(int code, int uid, boolean succeed) {}

	/** 本地到节点（第一段）的时延值发生改变 */
	public void onFirstSegmentNetDelayChange(int delayMilliseconds) {}

	/**
	 * 第二段延时值发生变化
	 * 
	 * @param uid
	 *            是哪个游戏？
	 * @param secondDelay
	 *            延迟数据
	 */
	public void onSecondSegmentNetDelayChange(int uid, GameManager.SecondSegmentNetDelay secondDelay) {}

	/** 短连接游戏网络请求结束 */
	public void onShortConnGameNetRequestEnd() {}

	/**
	 * 支持的游戏有更新
	 */
	public void onSupportedGameUpdate() {}

	/**
	 * 本程序的网络权限被禁止了
	 */
	public void onNetRightsDisabled() {}

	/**
	 * 断线重连触发
	 */
	public void onReconnectResult(ReconnectResult result) {}

	public static class ReconnectResult {
		public final int uid;
		public final int taskId;
		public final int count;
		public final boolean success;

		public ReconnectResult(int uid, int taskId, int count, boolean success) {
			this.uid = uid;
			this.taskId = taskId;
			this.count = count;
			this.success = success;
		}
	}

	/**
	 * SD卡挂载成功
	 */
	public void onMediaMounted() {}
	
    /**
     * 自动清理内存
     */
    public void onAutoProcessClean(List<AppProfile> runningAppList) {}

	//TODO:------- 未添加触发的事件----------

	/**
	 * wifi 加速状态
	 */

	public void onGetWifiAccelState(boolean isEnable) {}
}
