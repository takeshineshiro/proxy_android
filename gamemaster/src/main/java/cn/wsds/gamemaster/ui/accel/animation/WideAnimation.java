package cn.wsds.gamemaster.ui.accel.animation;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/**
 * 布局伸展动画
 */
public final class WideAnimation {

	private final View contentView;
	private final int delta;
	private final int origin;
	private final int duration;
	private long beginTime;		
	private final Runnable callbackExecute = new Runnable() {
		
		@Override
		public void run() {
			execute();
		}
	};
	private AccelAnimationListener accelAnimationListener;

	public WideAnimation(int duration,int delta, View contentView) {
		this(duration, contentView.getHeight(), delta, contentView,null);
	}
	
	public WideAnimation(int duration,int origin,int delta, View contentView, AccelAnimationListener accelAnimationListener) {
		this.duration = duration;
		this.contentView = contentView;
		this.delta = delta;
		this.origin = origin;
		this.accelAnimationListener = accelAnimationListener;
	}
	
	public void setAccelAnimationListener(AccelAnimationListener accelAnimationListener) {
		this.accelAnimationListener = accelAnimationListener;
	}
	
	public void execute() {
		LayoutParams layoutParams = contentView.getLayoutParams();
		if(beginTime == 0){
			beginTime = SystemClock.elapsedRealtime();
			layoutParams.height = origin; 
			contentView.setLayoutParams(layoutParams);
		}
		long elapsedTime = SystemClock.elapsedRealtime() - beginTime;
		if(elapsedTime < duration){
			// 根据当前时间计算应该增加的值
			int dis = (int)elapsedTime * delta / duration;
			contentView.postDelayed(callbackExecute, 50);
			int height = layoutParams.height;
			if(height<0){
				 layoutParams.height = contentView.getMeasuredHeight();
				 height = layoutParams.height;
			}
			layoutParams.height = origin + dis;
		}else{
			layoutParams.height = origin + delta; 
			if(accelAnimationListener!=null){
				accelAnimationListener.onAnimationEnd();
			}

		}
		contentView.setLayoutParams(layoutParams);
	}
	
	public void reset() {
		LayoutParams layoutParams = contentView.getLayoutParams();
		layoutParams.height = origin; 
		contentView.setLayoutParams(layoutParams);
	}
}
