package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import cn.wsds.gamemaster.R;

public class LoadingRing extends RelativeLayout {

	public interface OnCompleteListener {
		public void onComplete();
	}

	private enum State {
		NOT_STARTED, RUNNING, WAIT_FOR_STOP,
	}

	private long duration;
	private final long min_duration;

	private final AnimationDrawable rocket;
	private final ImageView ring;

	private long startTime;
	private State state = State.NOT_STARTED;

	private OnCompleteListener onCompleteListener;
	
	public LoadingRing(Context context) {
		this(context, null);
	}

	public LoadingRing(Context context, AttributeSet attrs) {
		super(context, attrs);
		if(attrs!=null){
			TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingRing);
			min_duration = Math.max(0, typedArray.getInteger(R.styleable.LoadingRing_min_duration, 1000));
			setDuration(typedArray.getInteger(R.styleable.LoadingRing_duration, (int)Math.max(1000, min_duration)));
			typedArray.recycle();
		}else{
			min_duration = 0;
			setDuration(min_duration);
		}
		//
		Drawable d = createImageView(context, R.drawable.loading_rocket).getDrawable();
		if (d != null && (d instanceof AnimationDrawable)) {
			rocket = (AnimationDrawable) d;
			rocket.stop();
		} else {
			rocket = null;
		}
		ring = createImageView(context, R.drawable.loading_edge_ring);
	}

	private ImageView createImageView(Context context, int imgResId) {
		ImageView iv = new ImageView(context);
		iv.setImageResource(imgResId);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
			LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		this.addView(iv, lp);
		return iv;
	}
	
	public void setDuration(long value) {
		this.duration = Math.max(min_duration, value);
	}

	public void start(OnCompleteListener listener) {
		if (state != State.NOT_STARTED) {
			return;
		}
		state = State.RUNNING;
		this.onCompleteListener = listener;
		this.startTime = now();
		//
		if (rocket != null) {
			rocket.start();
		}
		//
		RotateAnimation ani = new RotateAnimation(0f, duration * 270 / 1000, Animation.RELATIVE_TO_SELF, 0.5f,
			Animation.RELATIVE_TO_SELF, 0.5f);
		ani.setInterpolator(AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear));
		ani.setRepeatCount(0);
		ani.setDuration(duration);
		ani.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if (rocket != null) {
					rocket.stop();
				}
				cancelAnimation();
				if (onCompleteListener != null) {
					onCompleteListener.onComplete();
				}
			}

		});
		ring.startAnimation(ani);
	}

	/**
	 * 提交一个Stop请求。
	 * 如果特效时间未达到min_duration，将保证至少运行min_duration才停止
	 */
	public void requestStop() {
		if (state != State.RUNNING || state == State.WAIT_FOR_STOP) {
			return;
		}
		state = State.WAIT_FOR_STOP;
		this.postDelayed(new Runnable() {
			@Override
			public void run() {
				cancelAnimation();
			}
		}, startTime + min_duration - now());
	}
	
	private void cancelAnimation() {
		state = State.NOT_STARTED;
		ring.clearAnimation();
//		Animation ani = ring.getAnimation();
//		ring.setAnimation(null);
//		if (ani != null) {
//			ani.cancel();
//		}
	}

	private static long now() {
		return SystemClock.elapsedRealtime();
	}

}
