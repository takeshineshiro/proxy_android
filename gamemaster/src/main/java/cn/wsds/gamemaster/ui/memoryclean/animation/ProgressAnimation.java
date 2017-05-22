package cn.wsds.gamemaster.ui.memoryclean.animation;

import android.os.SystemClock;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.ActivityMemoryClean.ItemProgress;
import cn.wsds.gamemaster.ui.view.CircleProgres;

public class ProgressAnimation implements AnimationClean{
	
	private final CircleProgres circleProgres;
	private final TextView textValue;
	private final int maxProgress;
	private final int minValue;
	private static final int PER_TIME = 10;
	private static final int DURATION = 300;
	private final float perProgress;
	private final float perValue;
	private int pristineValue;
	private long beginTime;
	private Runnable runnable = new Runnable() {
		
		@Override
		public void run() {
			proRun();
		}
	};
	private OnAnimationEndListener onAnimationEndListener;
	

	ProgressAnimation(ItemProgress itemProgress) {
		this.circleProgres = itemProgress.circleProgres;
		this.textValue = itemProgress.textValue;
		this.maxProgress = itemProgress.maxProgress;
		this.minValue = itemProgress.minValue;
		this.pristineValue = circleProgres.getProgress();
		
		int amount = DURATION / PER_TIME;
		float deltaProgress = maxProgress - pristineValue;
		perProgress = deltaProgress / amount;
		perValue = (pristineValue - minValue) / amount;
	}

	@Override
	public void execute(OnAnimationEndListener onAnimationEndListener) {
		this.onAnimationEndListener = onAnimationEndListener;
		proRun();
	}

	@Override
	public void interrupt() {
		circleProgres.removeCallbacks(runnable);
	}
	
	public void proRun(){
		if(beginTime == 0){
			beginTime = SystemClock.elapsedRealtime();
		}
		long deltaTime = SystemClock.elapsedRealtime() - beginTime;
		if(deltaTime >= DURATION){
			textValue.setText(String.valueOf(minValue));
			circleProgres.setProgress(maxProgress);
			end();
			return;
		}
		long p = deltaTime / PER_TIME;
		int progress = (int) (pristineValue + p * perProgress);
		circleProgres.setProgress(progress);
		int value = (int) (pristineValue - p * perValue);
		textValue.setText(String.valueOf(value));
		
		circleProgres.postDelayed(runnable, PER_TIME);
	}

	private void end() {
		circleProgres.setRingColor(circleProgres.getResources().getColor(R.color.memory_clean_progress_color_noraml));
		if(onAnimationEndListener!=null){
			onAnimationEndListener.onAnimationEnd();
		}
	}
}
