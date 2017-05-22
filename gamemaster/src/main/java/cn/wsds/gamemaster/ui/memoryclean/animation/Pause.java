package cn.wsds.gamemaster.ui.memoryclean.animation;

import android.view.ViewGroup;

class Pause implements AnimationClean {

	private final int pauseTime = 1500;
	private final ViewGroup gameGridView;
	private final Runnable action = new Runnable() {
		
		@Override
		public void run() {
			interrupt();
			if(onAnimationEndListener!=null){
				onAnimationEndListener.onAnimationEnd();
			}
		}
	};
	private OnAnimationEndListener onAnimationEndListener;
	
	public Pause(ViewGroup gameGridView) {
		this.gameGridView = gameGridView; 
	}

	@Override
	public void execute(OnAnimationEndListener onAnimationEndListener) {
		this.onAnimationEndListener = onAnimationEndListener;
		gameGridView.postDelayed(action, pauseTime);
	}

	@Override
	public void interrupt() {
		gameGridView.removeCallbacks(action);
	}

}
