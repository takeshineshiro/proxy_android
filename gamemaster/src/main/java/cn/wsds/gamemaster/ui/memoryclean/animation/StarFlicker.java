package cn.wsds.gamemaster.ui.memoryclean.animation;

import java.util.ArrayList;
import java.util.List;

import android.os.SystemClock;
import android.view.View;
import cn.wsds.gamemaster.R;

class StarFlicker implements AnimationClean{
	private final List<View> firstGroups = new ArrayList<View>();
	private final List<View> sencondGroups = new ArrayList<View>();
	private final View starGroup;
	private static final int DELTA = 100;
	private final Runnable runnable = new Runnable() {
		
		@Override
		public void run() {
			starRun();
		}
	};
	
	private long startTime;
	private OnAnimationEndListener onAnimationEndListener;
	public StarFlicker(View starGroup) {
		this.starGroup = starGroup;
		firstGroups.add(starGroup.findViewById(R.id.image_first_star));
		firstGroups.add(starGroup.findViewById(R.id.image_second_star));
		sencondGroups.add(starGroup.findViewById(R.id.image_third_star));
	}
	
	private void starRun() {
		long elapsed = SystemClock.elapsedRealtime() - startTime;
		int step = elapsed == 0 ? 0 : (int) elapsed/ DELTA;
		switch (step) {
		case 0:
		case 2:
			starDrak(firstGroups);
			starLight(sencondGroups);
			break;
		case 1:
		case 3:
			starLight(firstGroups);
			starDrak(sencondGroups);
			break;
		case 4:
		default:
			starLight(sencondGroups);
			end();
			return;
		}
		starGroup.postDelayed(runnable, DELTA);
	}

	private void end() {
		interrupt();
		if(onAnimationEndListener!=null){
			onAnimationEndListener.onAnimationEnd();
		}
	}

	private void starLight(List<View> groups){
		for (View view : groups) {
			view.setAlpha(1.0f);
		}
	}
	
	private void starDrak(List<View> groups){
		for (View view : groups) {
			view.setAlpha(0.5f);
		}
	}

	@Override
	public void execute(OnAnimationEndListener onAnimationEndListener) {
		this.onAnimationEndListener = onAnimationEndListener;
		startTime = SystemClock.elapsedRealtime();
		starRun();
	}

	@Override
	public void interrupt() {
		starGroup.removeCallbacks(runnable);
	}
	
}