package cn.wsds.gamemaster.ui.accel.progress;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R;

public class AccelProgressIcon extends View {

	/**
	 * 动画缩放的监听器
	 */
	public interface IconAniListener extends AniListener {
		/**
		 * 当动画缩放的时候被调用
		 * 
		 * @param sender
		 *            哪个{@link AccelProgressIcon} ?
		 * @param scale
		 *            缩放值，取值在0到1之间的浮点值。0表示原始大小，1表示扩张到最大
		 */
		public void onAniZoom(AccelProgressIcon sender, float scale);

	}

	private Drawable imgIcon;
	private final Drawable imgBkgnd;
	private final Drawable imgRing;

	private final Drawable imgRotateRing;
	private final int cxRotateRing, cyRotateRing;

	private Ani currentAni;
	
	private boolean abortFlag;

	public AccelProgressIcon(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AccelProgressIcon);
		imgIcon = ta.getDrawable(R.styleable.AccelProgressIcon_icon);
		ta.recycle();
		//
		imgBkgnd = loadDrawable(context, R.drawable.home_page_network_state_background_n);
		imgRing = loadDrawable(context, R.drawable.home_page_network_state_outer_ring);
		imgRotateRing = loadDrawable(context, R.drawable.home_page_network_state_outer_ring_big);
		//
		cxRotateRing = imgRotateRing.getIntrinsicWidth();
		cyRotateRing = imgRotateRing.getIntrinsicHeight();
		imgRotateRing.setBounds(0, 0, cxRotateRing, cyRotateRing);
	}

	@SuppressWarnings("deprecation")
	private Drawable loadDrawable(Context context, int imgResId) {
		return context.getResources().getDrawable(imgResId);
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		return this.cxRotateRing;
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		return this.cyRotateRing;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
		int w = (widthSpecMode == MeasureSpec.EXACTLY) ? MeasureSpec.getSize(widthMeasureSpec) : getSuggestedMinimumWidth();
		int h = (heightSpecMode == MeasureSpec.EXACTLY) ? MeasureSpec.getSize(heightMeasureSpec) : getSuggestedMinimumHeight();
		if (w < getSuggestedMinimumWidth()) {
			w |= View.MEASURED_STATE_TOO_SMALL;
		}
		if (h < getSuggestedMinimumHeight()) {
			h |= View.MEASURED_STATE_TOO_SMALL;
		}
		setMeasuredDimension(w, h);
	}

	private void drawImage(Canvas canvas, Drawable d, float scale) {
		if (d == null) {
			return;
		}
		int left, top, right, bottom;
		if (scale <= 0) {
			left = (cxRotateRing - d.getIntrinsicWidth()) >> 1;
			top = (cyRotateRing - d.getIntrinsicHeight()) >> 1;
			right = left + d.getIntrinsicWidth();
			bottom = top + d.getIntrinsicHeight();
		} else if (scale >= 1f) {
			left = top = 0;
			right = cxRotateRing;
			bottom = cyRotateRing;
		} else {
			float dx = (cxRotateRing - d.getIntrinsicWidth());
			float dy = (cyRotateRing - d.getIntrinsicHeight());
			int w = d.getIntrinsicWidth() + (int) (dx * scale);
			int h = d.getIntrinsicHeight() + (int) (dy * scale);
			left = (cxRotateRing - w) >> 1;
			top = (cyRotateRing - h) >> 1;
			right = left + w;
			bottom = top + h;
		}
		d.setBounds(left, top, right, bottom);
		d.draw(canvas);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (currentAni == null || this.isInEditMode()) {
			drawBaseImages(canvas, -1);
		} else {
			currentAni.draw(canvas);
		}
	}

	private void drawBaseImages(Canvas canvas, float scale) {
		drawImage(canvas, imgBkgnd, scale);
		drawImage(canvas, imgIcon, scale);
	}

	private void changeAni(Ani ani) {
		if (ani != currentAni) {
			if (ani == null) {
				if (currentAni.listener != null) {
					currentAni.listener.onAniEnd(AccelProgressIcon.this);
				}
			}
			currentAni = ani;
			if (currentAni != null) {
				currentAni.start();
			}
		}
	}

	public void setIcon(Drawable d) {
		if (this.imgIcon != d) {
			this.imgIcon = d;
			invalidate();
		}
	}

	/**
	 * 开启动画
	 */
	public void startAni(IconAniListener listener) {
		if (currentAni == null) {
			currentAni = new Ani_Inflate(listener);
			currentAni.start();
		}
	}
	
	/**
	 * 中断动画
	 */
	public void abortAni() {
		if (!abortFlag) {
			abortFlag = true;
			if (currentAni != null) {
				currentAni.abort();
			}
		}
	}
	
	//////////////////////////////////////////

	/**
	 * 动画抽象其类
	 */
	private abstract class Ani extends AnimatorListenerAdapter implements AnimatorUpdateListener {

		private ValueAnimator valueAnimator;
		
		private float percent;

		protected final IconAniListener listener;

		public Ani(IconAniListener listener) {
			this.listener = listener;
		}

		public void start() {
			if (valueAnimator == null) {
				valueAnimator = ValueAnimator.ofFloat(0f, 1f);
				valueAnimator.setDuration(getDuration());
				valueAnimator.setInterpolator(null);
				valueAnimator.addListener(this);
				valueAnimator.addUpdateListener(this);
				valueAnimator.start();
			}
		}
		
		public void abort() {
			if (valueAnimator != null) {
				valueAnimator.cancel();
				valueAnimator = null;
				if (listener != null) {
					listener.onAniAbort(AccelProgressIcon.this);
				}
			}
		}
		
		protected final float getPercent() {
			return percent;
		}

		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			percent = (Float)animation.getAnimatedValue();
			invalidate();
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			valueAnimator = null;
			changeAni(createNextAni());
		}

		@Override
		public void onAnimationCancel(Animator animation) {
			valueAnimator = null;
		}

		public abstract long getDuration();

		public abstract void draw(Canvas canvas);

		public abstract Ani createNextAni();

	}

	/**
	 * 缩放动画的基类
	 */
	private abstract class Ani_Scale extends Ani {

		public Ani_Scale(IconAniListener listener) {
			super(listener);
		}

		@Override
		public long getDuration() {
			return 100;
		}

		@Override
		public void draw(Canvas canvas) {
			float scale = calcScale(getPercent());
			if (listener != null) {
				listener.onAniZoom(AccelProgressIcon.this, scale);
			}
			drawBaseImages(canvas, scale);
			drawImage(canvas, imgRing, scale);
		}

		protected abstract float calcScale(float percent);
	}

	/**
	 * 扩大
	 */
	private class Ani_Inflate extends Ani_Scale {

		public Ani_Inflate(IconAniListener listener) {
			super(listener);
		}

		@Override
		protected float calcScale(float percent) {
			return percent;
		}

		@Override
		public Ani createNextAni() {
			return new Ani_Rotate(listener);
		}
	}

	/**
	 * 缩小
	 */
	private class Ani_Deflate extends Ani_Scale {

		public Ani_Deflate(IconAniListener listener) {
			super(listener);
		}

		@Override
		protected float calcScale(float percent) {
			return 1f - percent;
		}

		@Override
		public Ani createNextAni() {
			return null;
		}
	}

	private class Ani_Rotate extends Ani {

		public Ani_Rotate(IconAniListener listener) {
			super(listener);
		}

		@Override
		public long getDuration() {
			return 300;
		}

		@Override
		public void draw(Canvas canvas) {
			drawBaseImages(canvas, 2f);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate(120 * getPercent(), cxRotateRing * 0.5f, cyRotateRing * 0.5f);
			imgRotateRing.draw(canvas);
			canvas.restore();
		}

		@Override
		public Ani createNextAni() {
			return new Ani_Deflate(listener);
		}
	}
}
