package cn.wsds.gamemaster;

import com.subao.net.NetManager;

import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.event.TaskManager;
import cn.wsds.gamemaster.net.NetTypeDetectorImpl;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.floatwindow.WifiAccelState;

public class FirstNetDelayManager {

	private static final FirstNetDelayManager instance = new FirstNetDelayManager();
	private static final String TAG = "FirstNetDelayManager" ;

	private boolean udpDetectStarted;

	public static FirstNetDelayManager getInstance() {
		return instance;
	}

	private FirstNetDelayManager() {}

	private int countExceptionOfUDP;
	private NetDelayDetector.Observer tcpNetDelayObserver;

	private void stopTcpNetDelayDetect() {
		if (tcpNetDelayObserver != null) {
			NetDelayDetector.removeObserver(tcpNetDelayObserver, NetDelayDetector.Type.TCP);
			tcpNetDelayObserver = null;
		}
	}

	private void startTcpNetDelayDetect() {
		if (tcpNetDelayObserver == null) {
			tcpNetDelayObserver = new NetDelayDetector.Observer() {
				@Override
				public void onNetDelayChange(int value, NetDelayDetector.Type type) { }
			};
			NetDelayDetector.addObserver(tcpNetDelayObserver, NetDelayDetector.Type.TCP);
		}
	}

	private int adjustUDPResult(int milliseconds) {
		// 值正常，或者是断网或2G状态，停止TCP测速，返回UDP值
		if (!isDelayValueException(milliseconds) || !isNetWiFiOr3G4G()) {
			countExceptionOfUDP = 0;
			stopTcpNetDelayDetect();
			return milliseconds;
		}
		// 值异常，且当前是WiFi或3G或4G
		// 如果TCP测速正在进行中，直接返回TCP结果
		if (NetDelayDetector.isRunning(NetDelayDetector.Type.TCP)) {
			return NetDelayDetector.getDelayValue(NetDelayDetector.Type.TCP);
		}
		// 还没有启动TCP测速，看看是不是已连续异常3次了？
		++countExceptionOfUDP;
		if (countExceptionOfUDP >= 3) {
			// 连续3次在非断网和2G下面异常了
			// 启动TCP测速
			countExceptionOfUDP = 0;
			startTcpNetDelayDetect();
			return NetDelayDetector.getDelayValue(NetDelayDetector.Type.TCP);
		}
		return milliseconds;
	}
	/**
	 * 开启测速
	 */
	public void startDetect() {
		if (!udpDetectStarted) {
			udpDetectStarted = true;
			//VPNManager.getInstance().sendStartGameDelayDetect();
			VPNUtils.sendStartGameDelayDetect(TAG);
		}
	}

	/**
	 * 停止测速
	 */
	public void stopDetect() {
		if (udpDetectStarted) {
			udpDetectStarted = false;
			//VPNManager.getInstance().sendStopGameDelayDetect();
			VPNUtils.sendStopGameDelayDetect(TAG);
		}
		stopTcpNetDelayDetect();
	}

	/**
	 * JNI回调通知：UDP测速结果
	 * <p>
	 * <b>本函数必须被JNI回调首先调用！！</b>
	 * </p>
	 * @return 
	 */
	public int onUDPResult(int rawUDPDelayValue) {
		//
		// 修正底层上传的值，使之符合上层逻辑
		// （上层认为-1表示网络有问题，-2表示测速未完成，但JNI层-2表示测速失败）
		if (rawUDPDelayValue == GlobalDefines.NET_DELAY_TEST_WAIT) {
			if (NetManager.getInstance().isDisconnected()) {
				rawUDPDelayValue = GlobalDefines.NET_DELAY_TEST_FAILED;
			}
		}
		//
		int delay = adjustUDPResult(rawUDPDelayValue);
		return GameManager.getInstance().onFirstSegmentNetDelayChange(delay, rawUDPDelayValue, NetTypeDetectorImpl.getInstance(), TaskManager.getInstance(), WifiAccelState.getInstance().isAccelState());
	}

	/**
	 * 判断给定的延迟值是否异常？
	 * 
	 * @param milliseconds
	 *            给定的延迟值，单位毫秒
	 * @return true，表示给定的延迟值是一个表示异常状态的值
	 */
	private static boolean isDelayValueException(int milliseconds) {
		return milliseconds < 0 || milliseconds >= GlobalDefines.NET_DELAY_TIMEOUT;
	}

	/**
	 * 当前网络是WiFi、3G或4G吗？
	 */
	private static boolean isNetWiFiOr3G4G() {
		switch (NetManager.getInstance().getCurrentNetworkType()) {
		case WIFI:
		case MOBILE_3G:
		case MOBILE_4G:
		case UNKNOWN:
			return true;
		default:
			return false;
		}
	}

}
