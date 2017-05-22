package cn.wsds.gamemaster.ui;

import android.widget.TextView;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

/**
 * 稳定性的一些逻辑 
 */
public class StabilityDrawer {
	
	private final TextView textStability;
	private StatbilityStateProperty goodProperty;
	private StatbilityStateProperty normalProperty;
	private StatbilityStateProperty badProperty;
	private StatbilityStateProperty offProperty;
	
	public StabilityDrawer(TextView textStability,StatbilityStateProperty goodProperty,StatbilityStateProperty normalProperty,
			StatbilityStateProperty badProperty,StatbilityStateProperty offProperty) {
		this.textStability = textStability;
		this.goodProperty = goodProperty;
		this.normalProperty = normalProperty;
		this.badProperty = badProperty;
		this.offProperty = offProperty;
	}

	/**
	 * 根据信号强度等级设置稳定性
	 * @param signalLevel 网络信号强度 （异常为小于的0的数）
	 */
	public void setStabilityDesc(int signaLevel) {
		StatbilityStateProperty property;
		if(AccelOpenManager.isStarted()){
			NetTypeDetector.NetType netType = NetManager.getInstance().getCurrentNetworkType();
			property = getStabilityDescAccelOn(netType == NetTypeDetector.NetType.DISCONNECT ? -1 :signaLevel); 
		}else{
			property = offProperty;
		}
		setValue(property);
	}

	private void setValue(StatbilityStateProperty property) {
		textStability.setText(property.stateDesc);
		textStability.setTextColor(property.color);
	}
	
	/**
	 * 获得加速开启时本地网络的稳定性根据信号强度
	 * @param signalLevel 信号强度
	 * @return
	 */
	private StatbilityStateProperty getStabilityDescAccelOn(int signalLevel){
		switch (signalLevel) {
		case 0:
		case 1:
			return badProperty;
		case 2:
			return normalProperty;
		case 3:
		case 4:
			return goodProperty;
		default:
			return badProperty;
		}
	}
	
	public static final class StatbilityStateProperty {
		public final int color;
		public final String stateDesc;
		public StatbilityStateProperty(int color, String stateDesc) {
			this.color = color;
			this.stateDesc = stateDesc;
		}
	}

	public void setStateOff() {	
		setValue(offProperty);
	}
}
