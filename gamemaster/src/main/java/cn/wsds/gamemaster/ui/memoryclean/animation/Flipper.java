package cn.wsds.gamemaster.ui.memoryclean.animation;

import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;

public class Flipper implements AnimationClean {
	
	private ViewFlipper viewFlipper;
	private final Animation inAnimation = AnimationUtils.loadAnimation(AppMain.getContext(), R.anim.fade_in_memory_clean);
	private OnAnimationEndListener onAnimationEndListener;
	public Flipper(ViewFlipper viewFlipper) {
		this.viewFlipper = viewFlipper;
		viewFlipper.setInAnimation(inAnimation);
		inAnimation.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if(onAnimationEndListener != null){
					onAnimationEndListener.onAnimationEnd();
				}
			}
		});
	}

	@Override
	public void execute(OnAnimationEndListener onAnimationEndListener) {
		this.onAnimationEndListener = onAnimationEndListener;
		viewFlipper.showNext();
	}

	@Override
	public void interrupt() {}

}
