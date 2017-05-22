package cn.wsds.gamemaster.ui.memoryclean.animation;

import java.util.LinkedList;
import java.util.Queue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.ui.ActivityMemoryClean.ItemProgress;

public class AnimationManager {

	private final Queue<AnimationClean> queue = new LinkedList<AnimationClean>();
	private final OnAnimationEndListener onQueueAnimationEndListener = new OnAnimationEndListener() {
		
		@Override
		public void onAnimationEnd() {
			showNext();
		}
	};
	private OnAnimationEndListener onAnimationEndListener;
	private AnimationClean currentAnimation;
	private boolean isRunning;
	public static AnimationManager createInstance(ViewGroup gameGridView,TextView textLabel,ViewFlipper viewFlipper,View starGroup, ItemProgress itemMemoryUsage, ItemProgress itemCPUTemperature, ItemProgress itemBackgroundApplication,View viewGroup){
		AnimationManager animationManager = new AnimationManager();
		animationManager.queue.add(new Pause(gameGridView));
		animationManager.queue.add(new GridViewSmoothScroll(gameGridView,textLabel));
		animationManager.queue.add(new Flipper(viewFlipper));
		animationManager.queue.add(new StarFlicker(starGroup));
		animationManager.queue.add(new ProgressAnimation(itemMemoryUsage));
		animationManager.queue.add(new ProgressAnimation(itemCPUTemperature));
		animationManager.queue.add(new ProgressAnimation(itemBackgroundApplication));
		return animationManager;
	}
	
	public void execute(OnAnimationEndListener onAnimationEndListener){
		this.onAnimationEndListener = onAnimationEndListener;
		isRunning = true;
		showNext();
	}

	private void showNext() {
		if(showNextAnimation()){
			return;
		}
		isRunning = false;
		if(onAnimationEndListener!=null){
			onAnimationEndListener.onAnimationEnd();
		}
	}

	private boolean showNextAnimation() {
		currentAnimation = queue.poll();
		if(currentAnimation == null){
			return false;
		}
		currentAnimation.execute(onQueueAnimationEndListener);
		return true;
	}
	
	public void interrupt(){
		if(currentAnimation!=null){
			currentAnimation.interrupt();
		}
		queue.clear();
	}
	
	public boolean isRunning(){
		return this.isRunning;
	}
}
