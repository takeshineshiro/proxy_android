package cn.wsds.gamemaster.ui.floatwindow;

/**
 * 加速状态类
 * Created by hujd on 16-11-29.
 */
public class WifiAccelState {
	private final static WifiAccelState INSTANCE = new WifiAccelState();

	private boolean accelState;


	public static WifiAccelState getInstance() {
		return INSTANCE;
	}

	/**
	 * getter accel state
	 * @return
	 */
	public boolean isAccelState() {
		return accelState;
	}


	/***
	 * setter accel state
	 * @param accelState
	 */
	public void setAccelState(boolean accelState) {
		this.accelState = accelState;
	}
}
