package cn.wsds.gamemaster.ui;

import java.util.LinkedList;
import java.util.Queue;

import android.graphics.drawable.Drawable;
import android.os.Message;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.widget.ImageView;
import android.widget.TextView;

import com.subao.resutils.WeakReferenceHandler;

public class LaunchGameProgress {

	private final ProgressBar progress;
	private final MyHandler myHandler = new MyHandler(this);
	private OnFinishedListener onFinishedListener;
	private long startTime,currentTime;
	// 时间值 毫秒值
	/** 更新进度条运行最大时长 */
	private static final long MAX_TIME = 10000;
	/** 快速更新进度条运行最大时长*/
	private static final long FAST_TIME = 1000;
	/** 立即完成时长*/
	private static final long IMMEDIAT_TIME = 500;
	/** 刷新频率间隔 */
	private static final int EACH_TIME = 15;

	/**最大level */
	private static final int MAX_LEVEL = 10000;
	/** 更新进度数据队列 */
	private final Queue<DeltaProgress> deltaLevel = new LinkedList<DeltaProgress>();
	
	private DeltaProgress currentDeltaProgress;

	/**
	 * 更新完成监听
	 */
	public interface OnFinishedListener {
		/** 当更新完成时 */
		public void onFinished(boolean isTimeOut);
	}

	public LaunchGameProgress(final ImageView imageProgress,TextView textProgress,OnFinishedListener onFinishedListener) {
		progress = new ProgressBar(imageProgress, textProgress);
		this.onFinishedListener = onFinishedListener;
		progress.setLevel(0);
	}
	
	private static final class ProgressBar {
		private final Drawable drawableProgress;
		private final ImageView imageProgress;
		private final TextView textProgress;
		private  float progressDistance;
		public static final int LEVEL_TO_PROGRESS = 100;
//		private float centerXProgressText;
		
		private ProgressBar(final ImageView imageProgress,TextView textProgress) {
			this.imageProgress = imageProgress;
			imageProgress.post(new Runnable() {
				
				@Override
				public void run() {
					progressDistance = imageProgress.getMeasuredWidth();
				}
			});
			Drawable drawableProgress = imageProgress.getDrawable();
			this.drawableProgress = drawableProgress;
			this.textProgress = textProgress;
		}
		
		public void setLevel(final int progress) {
			if(progress < 0 || progress > MAX_LEVEL) {
				return;
			}
			setProgressPercent(progress/LEVEL_TO_PROGRESS);
			if(progressDistance <= 0 ){
				textProgress.post(new Runnable() {
					
					@Override
					public void run() {
						progressDistance = imageProgress.getMeasuredWidth();
//						centerXProgressText = textProgress.getWidth() / 2;
						setTextX(progress);
					}
				});
			}else{
				setTextX(progress);
			}
			drawableProgress.setLevel(progress);
		}
		
		private void setProgressPercent(int percent) {
			String percentMess = String.valueOf(percent);
			String unit = "%";
			SpannableStringBuilder spannable = new SpannableStringBuilder();
			spannable.append(percentMess);
			spannable.append(unit);
			int start = percentMess.length();
			int end = start + unit.length();
			spannable.setSpan(new AbsoluteSizeSpan(9,true), start , end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			textProgress.setText(spannable);
		}

		public int getLevel() {
			return drawableProgress.getLevel();
		}
		
		private void setTextX(int progress) {
			if(progress==0&&textProgress.getX()==0){
				return;
			}
			float delta = progressDistance * (progress * 1.0f / MAX_LEVEL);
			float x = /*centerXProgressText +*/ delta;
			textProgress.setX(x);
		}
	}
	
	
	private static final class MyHandler extends WeakReferenceHandler<LaunchGameProgress> {
		
		public static final int MSG_REFRESH_PROGRESS = 0;

		public MyHandler(LaunchGameProgress ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(LaunchGameProgress ref, Message msg) {
			switch (msg.what) {
			case MSG_REFRESH_PROGRESS:
				ref.update();
				break;
			}
		}
	}
	
	
	public void endImmediately(){
		deltaLevel.clear();
		deltaLevel.add(new DeltaProgress(IMMEDIAT_TIME,IMMEDIAT_TIME,MAX_LEVEL - progress.getLevel(),MAX_LEVEL));
		myHandler.removeMessages(MyHandler.MSG_REFRESH_PROGRESS);
		start();
	}
	
	/**
	 * 正常开启进度条 
	 */
	public void startNormalModel(){
		deltaLevel.add(new DeltaProgress(2000,2000,5000,5000));
		deltaLevel.add(new DeltaProgress(6000,4000,4900,9900));
		deltaLevel.add(new DeltaProgress(MAX_TIME,4000,1,MAX_LEVEL));
		start();
	}
	
	/** 快速开启进度条 */
	public void startFastModel() {
		deltaLevel.add(new DeltaProgress(FAST_TIME,FAST_TIME,MAX_LEVEL,MAX_LEVEL));
		start();
	}

	/** 更新进度条 */
	private void update() {
		// 进度更新完成
		if(progress.getLevel() >= MAX_LEVEL){
			onFinish(false);
			return;
		}
		// 进度更新差值数据为空
		if(currentDeltaProgress==null){
			onFinish(false);
			return;
		}
		
		// 超时
		long elapsedTime = SystemClock.elapsedRealtime() - startTime;
		if(MAX_TIME<=elapsedTime){
			onFinish(true);
			return;
		}
		
		int level;
		if(currentDeltaProgress.time<elapsedTime){
			level = currentDeltaProgress.goal - progress.getLevel(); 
			currentDeltaProgress = deltaLevel.poll();
		}else{
			if(elapsedTime == 0 || SystemClock.elapsedRealtime() - currentTime == 0){
				level = 0;
			}else{
				level = (int) (currentDeltaProgress.bulk * (SystemClock.elapsedRealtime() - currentTime) / currentDeltaProgress.deltaTime);
			}
		}
		currentTime = SystemClock.elapsedRealtime();
		int currentLevelValue = progress.getLevel() + level;
		progress.setLevel(currentLevelValue);
		myHandler.sendEmptyMessageDelayed(MyHandler.MSG_REFRESH_PROGRESS, EACH_TIME);
	}

	private void start() {
		startTime = SystemClock.elapsedRealtime();
		currentTime = SystemClock.elapsedRealtime();
		currentDeltaProgress = deltaLevel.poll();
		update();
	}
	
	private void onFinish(boolean isTimeOut){
		myHandler.removeMessages(MyHandler.MSG_REFRESH_PROGRESS);
		if(onFinishedListener!=null){
			onFinishedListener.onFinished(isTimeOut);
		}
	}
	
	/**
	 * 进度更新差值数据
	 */
	private final class DeltaProgress {
		/** 目标level */
		private final int goal;
		/** 需要移动的 level*/
		private final int bulk;
		/** 当前进度时间点*/
		private final long time;
		/** 当前进度更新所需时间*/
		private long deltaTime;
		private DeltaProgress(long time, long deltaTime,int bulk,int goal) {
			this.time = time;
			this.goal = goal;
			this.bulk = bulk;
			this.deltaTime = deltaTime;
		}
	}
}
