package cn.wsds.gamemaster.ui.floatwindow.twosegment;

import android.content.Context;
import android.content.res.Resources;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.R;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

public abstract class NetDelayDrawer {
	
	protected final TextView textDelayValue;
	protected final ImageView delayProgress;
	protected final TextView textStateDesc;
	private final OnDelayErrorListenter delayErrorListener;
	protected final int colorGame11;
	protected final int colorGame16;
	protected final int colorGame17;
	
	public interface OnDelayErrorListenter {
		public void onDelayError();
	}
	
	public NetDelayDrawer(TextView textDelayValue, ImageView delayProgress,TextView textStateDesc,OnDelayErrorListenter delayErrorListener) {
		Resources resources = textDelayValue.getResources();
		this.colorGame11 = resources.getColor(R.color.color_game_11);
		this.colorGame16 = resources.getColor(R.color.color_game_16);
		this.colorGame17 = resources.getColor(R.color.color_game_17);
		this.textDelayValue = textDelayValue;
		this.delayProgress = delayProgress;
		this.textStateDesc = textStateDesc;
		this.delayErrorListener = delayErrorListener;
	}
	
	protected Context getContext() {
		return this.textDelayValue.getContext();
	}

	private State currentState;
	protected enum State{
		wait,error,timeout,normal
	}
	
	public void onDelayChange(long delay){
		State state = getDelayState(delay);
		if(state!=State.normal){
			if(currentState == state){
				return;
			}
		}
		currentState = state;
		switch (state) {
		case error:
			if(delayErrorListener!=null){
				delayErrorListener.onDelayError();
			}
			onDelayError();
			break;
		case normal:
			onDelayNormal(delay);
			break;
		case timeout:
			onDelayTimeOut();
			break;
		case wait:
			onDelayWait();
			break;
		default:
			break;
		}
	}
	
	public void setNormalDelay(long delayMilliseconds) {
		String delayValue = delayMilliseconds > 1000  ? String.format("%.2fs", delayMilliseconds * 0.001f) : delayMilliseconds + "ms";
		this.textDelayValue.setText(delayValue);
	}

	protected abstract void onDelayError();
	
	protected abstract void onDelayNormal(long delay);
	
	protected abstract void onDelayTimeOut();
	
	protected abstract void onDelayWait();

	protected static State getDelayState(long delay) {
		if (NetManager.getInstance().isDisconnected()) {
			return State.error;
		}
		if(NetTypeDetector.NetType.MOBILE_2G == NetManager.getInstance().getCurrentNetworkType()){
			return State.error;
		}
		if(GlobalDefines.NET_DELAY_TEST_FAILED == delay){
			return State.error;
		}else if(GlobalDefines.NET_DELAY_TEST_WAIT == delay){
			return State.wait;
		}else if(GlobalDefines.NET_DELAY_TIMEOUT <= delay){
			return State.timeout;
		}else{
			return State.normal;
		}
	}
	
	
}
