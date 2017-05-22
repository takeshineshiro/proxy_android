package cn.wsds.gamemaster.ui.floatwindow.twosegment;

import android.text.Spannable;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.floatwindow.DelayTextSpannable;
import cn.wsds.gamemaster.ui.floatwindow.DelayTextSpannable.OnTakeLocalDelayTextSpannableListener;
import cn.wsds.gamemaster.ui.floatwindow.WifiAccelState;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

public class FirstSegmentNetDelayDrawer extends NetDelayDrawer{

	public FirstSegmentNetDelayDrawer(TextView textDelayValue,ImageView delayProgress, TextView textStateDesc,OnDelayErrorListenter delayErrorListener) {
		super(textDelayValue, delayProgress, textStateDesc, delayErrorListener);
	}

	enum DelayNormalStateSection {
		bad, poor, normal, wifi_accel,
	}
	
	private final DelayTextSpannable delayTextSpannable = new DelayTextSpannable(new OnTakeLocalDelayTextSpannableListener() {
		
		@Override
		public void onTakeLocalDelayTextSpannable(Spannable localSpannable,Spannable remoteSpannable) {
			textStateDesc.setText(localSpannable);
		}
	});

	@Override
	protected void onDelayError() {
		textDelayValue.setText("");
		delayProgress.setImageResource(R.drawable.suspension_network_state_progress_bar_bad);

		if(WifiAccelState.getInstance().isAccelState()) {
			return;
		}
		Spannable spannable;
		if (NetTypeDetector.NetType.MOBILE_2G == NetManager.getInstance().getCurrentNetworkType()) {
			spannable = DelayTextSpannable.getSpecialInBefore("2G网络自身延迟较高", "，影响您的游戏体验，建议使用其他网络进行游戏。",
				colorGame16);
		} else {
			spannable = DelayTextSpannable.getSpecialInBefore("未知网络异常", "，建议尝试手动开关WiFi/飞行模式。",
				colorGame16);
			delayTextSpannable.toTakeLocalTextSpannable(getContext());
		}
		textStateDesc.setText(spannable);
	}

	private Spannable getLocalDelayPormptTextOnBad(NetTypeDetector.NetType netType) {
		switch (netType) {
		case MOBILE_2G:
			return DelayTextSpannable.getSpecialInBefore("2G网络自身延迟较高", "，影响您的游戏体验，建议使用其他网络进行游戏。", colorGame16);
		case MOBILE_3G:
		case MOBILE_4G:
		case UNKNOWN:
			return DelayTextSpannable.getSpecialInMid("“我的网络”", "延迟异常", "，建议尝试重启数据连接（开关飞行模式）/换到信号更优的地方。",
				colorGame16);
		case WIFI:
			return DelayTextSpannable.getSpecialInMid("“我的网络”", "延迟异常", "，建议重连WiFi/重启无线路由器/更换WiFi/使用数据连接。",
				colorGame16);
		default:
			return null;
		}
	}
	
	private Spannable getNormalSpannable() {
		return DelayTextSpannable.getSpecialInMid("“我的网络”是否畅通", "直接影响加速效果的好坏", "。",
				colorGame11);
	}
	

	@Override
	protected void onDelayTimeOut() {
		textDelayValue.setText(">2s");
		delayProgress.setImageResource(R.drawable.suspension_network_state_progress_bar_bad);
		if(WifiAccelState.getInstance().isAccelState()) {
			return;
		}

		Spannable localDelayPormptTextOnBad = getLocalDelayPormptTextOnBad(NetManager.getInstance().getCurrentNetworkType());
		textStateDesc.setText(localDelayPormptTextOnBad);
	}

	@Override
	protected void onDelayWait() {
		textDelayValue.setText("---");
		delayProgress.setImageResource(R.drawable.suspension_network_state_progress_bar_normal);
		if(WifiAccelState.getInstance().isAccelState()) {
			return;
		}
		textStateDesc.setText(getNormalSpannable());
	}


	@Override
	protected void onDelayNormal(long delayMilliseconds) {
		setNormalDelay(delayMilliseconds);
		NetTypeDetector.NetType netType = NetManager.getInstance().getCurrentNetworkType();
		DelayNormalStateSection setionDelay = createSection(netType,delayMilliseconds, WifiAccelState.getInstance().isAccelState());
		switch (setionDelay) {
		case normal:
			setValue(getNormalSpannable(),R.drawable.suspension_network_state_progress_bar_normal);
			break;
		case poor:
			setValue(DelayTextSpannable.getSpecialInMid("“我的网络”", "延迟较高", "，可能影响加速效果。", colorGame17),R.drawable.suspension_network_state_progress_bar_poor);
			break;
		case wifi_accel:
			//do nothing
			break;
		case bad:
		default:
			setValue(getLocalDelayPormptTextOnBad(netType),R.drawable.suspension_network_state_progress_bar_bad);
			break;
		}
	}

	private void setValue(CharSequence text,int resId) {
		textStateDesc.setText(text);
		delayProgress.setImageResource(resId);
	}

	
	private static DelayNormalStateSection createSection(NetTypeDetector.NetType netType, long currentLocalNetDelay, boolean isWifiAccel) {
		if (isWifiAccel) {
			return DelayNormalStateSection.wifi_accel;
		}

		switch (netType) {
		case MOBILE_3G:
			return getLocalNetDelayStateByDelay(currentLocalNetDelay, 300, 150);
		case MOBILE_4G:
			return getLocalNetDelayStateByDelay(currentLocalNetDelay, 200, 100);
		case WIFI:
			return getLocalNetDelayStateByDelay(currentLocalNetDelay, 150, 80);
		default:
			return DelayNormalStateSection.bad;
		}
	}
	
	private static DelayNormalStateSection getLocalNetDelayStateByDelay(long delay, long poor, long normal) {
		if (delay < normal) {
			return DelayNormalStateSection.normal;
		} else if (delay <= poor) {
			return DelayNormalStateSection.poor;
		} else {
			return DelayNormalStateSection.bad;
		}
	}
}
