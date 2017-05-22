package cn.wsds.gamemaster.ui.floatwindow.twosegment;

import android.text.Spannable;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager.Checker.Result;
import cn.wsds.gamemaster.ui.floatwindow.DelayTextSpannable;

public class SecondSegmentNetDelayDrawer extends NetDelayDrawer{
	
	private final int remoteNormalDelay;
	private final boolean isForeignGame;
	private final Spannable delayOverTopSpannable;

	public SecondSegmentNetDelayDrawer(TextView textDelayValue,ImageView delayProgress, TextView textStateDesc,OnDelayErrorListenter delayErrorListener,boolean isForeignGame) {
		super(textDelayValue, delayProgress, textStateDesc, delayErrorListener);
		this.isForeignGame = isForeignGame;
		String specialText = this.isForeignGame ? "游戏服务器（海外）延迟较高" : "游戏服务器延迟较高";
		this.delayOverTopSpannable = DelayTextSpannable.getSpecialInMid("当前", specialText, "，建议重新加速。", colorGame16);
		this.remoteNormalDelay = isForeignGame ? 400 : 50;
	}

	private enum DelayNormalStateSection {
		bad, normal
	}
	
	@Override
	protected void onDelayError() {
		textDelayValue.setText("");
		delayProgress.setImageResource(R.drawable.suspension_network_state_progress_bar_bad);
		if(State.error == getDelayState(GameManager.getInstance().getFirstSegmentNetDelay())){
			textStateDesc.setText(DelayTextSpannable.getSpecialInBefore("“我的网络”异常", "，“加速网络”无法正常工作。", colorGame16));
			NetworkCheckManager.start(getContext(), new NetworkCheckManager.Observer() {
				
				@Override
				public void onNetworkCheckResult(Result result) {
					textStateDesc.setText(DelayTextSpannable.getRemoteDelayTextSpannable(result));
				}
			});
		}else{
			textStateDesc.setText("“加速网络”异常，请在客户端重开加速解决此问题。");
		}
	}

	@Override
	protected void onDelayTimeOut() {
		textDelayValue.setText(">2s");
		delayProgress.setImageResource(R.drawable.suspension_network_state_progress_bar_bad);
		textStateDesc.setText(this.delayOverTopSpannable);
	}

	@Override
	protected void onDelayWait() {
		textDelayValue.setText("---");
		delayProgress.setImageResource(R.drawable.suspension_network_state_progress_bar_normal);
		textStateDesc.setText(getNormalSpannable());
	}


	private CharSequence getNormalSpannable() {
		return DelayTextSpannable.getSpecialInBehind("此段网络是“我的网络”之外到游戏服务器的加速网络，", "最高可提速80%。", colorGame11);
	}

	@Override
	protected void onDelayNormal(long delayMilliseconds) {
		if(GlobalDefines.NET_DELAY_TEST_FAILED == GameManager.getInstance().getFirstSegmentNetDelay()){
			onDelayWait();
			return;
		}
		setNormalDelay(delayMilliseconds);
		DelayNormalStateSection setionDelay = createSection(delayMilliseconds);
		switch (setionDelay) {
		case normal:
			setValue(getNormalSpannable(),R.drawable.suspension_network_state_progress_bar_normal);
			break;
		case bad:
		default:
			setValue(this.delayOverTopSpannable,R.drawable.suspension_network_state_progress_bar_bad);
			break;
		}
	}
	private void setValue(CharSequence text,int resId) {
		textStateDesc.setText(text);
		delayProgress.setImageResource(resId);
	}

	
	private DelayNormalStateSection createSection(long currentRemoteNetDelay) {
		return currentRemoteNetDelay <= remoteNormalDelay ? DelayNormalStateSection.normal : DelayNormalStateSection.bad;
	}
	
}
