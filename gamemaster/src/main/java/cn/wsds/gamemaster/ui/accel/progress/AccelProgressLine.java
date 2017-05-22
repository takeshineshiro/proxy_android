package cn.wsds.gamemaster.ui.accel.progress;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R;

public class AccelProgressLine extends View {

	/** 用于绘制线条的{@link Drawable} */
	private final Drawable drawable;

	/** 线条的宽度，单位像素 */
	private final int lineWidth;

	/** 当线条下部的圆形图标膨胀到最大时，线条下端需要向上收缩多少个像素 */
	private final float maxBottomDeflate;

	/** 本View的宽度（像素） */
	private int width;

	/** 本View的高度（像素） */
	private int height;

	/** 当前需要绘制线条的百分比 */
	private float percent = 1f;

	/** 下端需要收缩多少{@link AccelProgressLine#maxBottomDeflate}百分比的像素 */
	private float percentOfBottomDeflate;
	
	/** 当前的{@link ValueAnimator} */
	private ValueAnimator valueAnimator;

	private boolean abortFlag;

	@SuppressWarnings("deprecation")
	public AccelProgressLine(Context context, AttributeSet attrs) {
		super(context, attrs);
		drawable = context.getResources().getDrawable(R.drawable.accel_progress_line);
		lineWidth = context.getResources().getDimensionPixelSize(R.dimen.accel_progress_line_width);
		maxBottomDeflate = context.getResources().getDimension(R.dimen.space_size_6);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		width = this.getMeasuredWidth();
		height = this.getMeasuredHeight();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int left = (width - lineWidth) >> 1;
		int top = 0;
		int right = left + lineWidth;
		int bottom = (int) (percent * height - percentOfBottomDeflate * maxBottomDeflate);
		drawable.setBounds(left, top, right, bottom);
		drawable.draw(canvas);
	}

	/**
	 * 开始动画
	 * 
	 * @param listener
	 */
	public void startAni(final AniListener listener) {
		if (abortFlag || valueAnimator != null || this.getVisibility() == VISIBLE) {
			return;
		}
		this.setVisibility(VISIBLE);
		valueAnimator = ValueAnimator.ofFloat(0f, 1f);
		valueAnimator.setDuration(150);
		valueAnimator.setInterpolator(null);
		valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				percent = (Float) animation.getAnimatedValue();
				invalidate();
			}
		});
		valueAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (listener != null) {
					listener.onAniEnd(AccelProgressLine.this);
				}
			}
			@Override
			public void onAnimationCancel(Animator animation) {
				if (listener != null) {
					listener.onAniAbort(AccelProgressLine.this);
				}
			}
		});
		valueAnimator.start();
	}

	public void abortAni() {
		if (!abortFlag) {
			abortFlag = true;
			if (valueAnimator != null) {
				valueAnimator.cancel();
				valueAnimator = null;
			}
		}
	}

	/**
	 * 向上收缩线条的下端
	 * 
	 * @param percent
	 *            百分比
	 */
	public void deflateBottom(float percent) {
		if (!abortFlag) {
			percentOfBottomDeflate = percent;
			invalidate();
		}
	}
}
