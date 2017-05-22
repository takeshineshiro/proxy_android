package cn.wsds.gamemaster.ui.view;

/**
 * Created by hujd on 17-2-17.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import cn.wsds.gamemaster.R;

/**
 * @author hujd
 */
public class TickViewAni extends TickView {

	private ValueAnimator tickAnimation;

	private float tickPercent;

	public TickViewAni(Context context) {
		super(context);
	}

	public TickViewAni(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TickViewAni(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		startAnimation();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		stopAnimation();

	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}

	/**
	 * 打钩动画
	 */
	@Override
	protected void setTickAni() {
		tickAnimation = ValueAnimator.ofFloat(0f, 1f);
		tickAnimation.setStartDelay(1000);
		tickAnimation.setDuration(500);
		tickAnimation.setInterpolator(new AccelerateInterpolator());
		tickAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				tickPercent = (float) animation.getAnimatedValue();
				invalidate();
			}
		});
		tickAnimation.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				super.onAnimationStart(animation);
			}
		});
	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawTick(canvas);
	}

	/**
	 * 绘制打钩
	 * @param canvas canvas
	 */
	@Override
	protected void drawTick(Canvas canvas) {
		Path path = new Path();
        /*
         * On KITKAT and earlier releases, the resulting path may not display on a hardware-accelerated Canvas.
         * A simple workaround is to add a single operation to this path, such as dst.rLineTo(0, 0).
         */
		tickPathMeasure.getSegment(0, tickPercent * tickPathMeasure.getLength(), path, true);
		path.rLineTo(0, 0);
		canvas.drawPath(path, tickPaint);
	}

	public void startAnimation(){
		post(new Runnable() {
			@Override
			public void run() {
				tickAnimation.start();
			}
		});
	}

	public void stopAnimation(){
		post(new Runnable() {
			@Override
			public void run() {
				tickAnimation.cancel();
			}
		});
	}
}