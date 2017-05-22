package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.ConditionVariable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class SubaoAni extends SurfaceView {

	public enum ScaleType {
		RAW(0),
		FILL(1),
		FILL_X(2),
		FILL_Y(3),
		ZOOM_BY_X(4),
		ZOOM_BY_Y(5);

		ScaleType(int v) {
			this.value = v;
		}

		public static ScaleType fromValue(int value) {
			if (FILL.value == value) {
				return FILL;
			} else if (FILL_X.value == value) {
				return FILL_X;
			} else if (FILL_Y.value == value) {
				return FILL_Y;
			} else if (ZOOM_BY_X.value == value) {
				return ZOOM_BY_X;
			} else if (ZOOM_BY_Y.value == value) {
				return ZOOM_BY_Y;
			} else {
				return RAW;
			}
		}

		final int value;
	}

	/**
	 * 绘制的每帧之间的间隔（毫秒）
	 * <p>
	 * 【<b>注意</b>】此值如果过小，在某些机型上会造成死锁现象，经试验，100是一个比较合适的值。 (by YHB 2016.5)
	 * </p>
	 */
	private static final int SLEEP_TIME_BETWEEN_FRAME = 100;

	/** 动画开始的时刻 */
	private long timeWhenAniStart;

	private int width, height;

	private ScaleType scaleType;

	private Drawer drawer;

	protected SubaoAni(Context context) {
		super(context);
	}

	public SubaoAni(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setScaleType(ScaleType type) {
		this.scaleType = type;
	}

	public ScaleType getScaleType() {
		return this.scaleType;
	}

	/**
	 * 在绘制第一帧之前被调用
	 * 
	 * @param width
	 *            整个View的宽度
	 * @param height
	 *            整个View的高度
	 */
	protected abstract void beforeFrame(int width, int height);

	/**
	 * 绘制背景
	 * 
	 * @param canvas
	 *            画布
	 * @param width
	 *            整个View的宽度
	 * @param height
	 *            整个View的高度
	 * @param elapsedTime
	 *            逝去的时间
	 */
	protected abstract void drawForeground(Canvas canvas, int width, int height, long elapsedTime);

	/**
	 * 绘制前景
	 * 
	 * @param canvas
	 *            画布
	 * @param width
	 *            整个View的宽度
	 * @param height
	 *            整个View的高度
	 * @param elapsedTime
	 *            逝去的时间
	 */
	protected abstract void drawBackground(Canvas canvas, int width, int height, long elapsedTime);

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		this.startAni();
	}

	@Override
	protected void onDetachedFromWindow() {
		this.stopAni();
		super.onDetachedFromWindow();
	}

	@Override
	public void layout(int l, int t, int r, int b) {
		super.layout(l, t, r, b);
		this.width = r - l;
		this.height = b - t;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		this.width = w;
		this.height = h;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (this.isInEditMode()) {
			drawBackground(canvas, width, height, 0);
			drawForeground(canvas, width, height, 0);
		}
	}

	/**
	 * 开始动画
	 */
	private void startAni() {
		if (timeWhenAniStart <= 0) {
			timeWhenAniStart = now();
			drawer = new Drawer();
			this.getHolder().addCallback(drawer);
			drawer.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
		}
	}

	/**
	 * 停止动画的绘制
	 */
	public void stopAni() {
		if (drawer != null) {
			drawer.cancel(true);
			drawer.waitForStop();
			this.getHolder().removeCallback(drawer);
			drawer = null;
		}
	}

	protected final boolean doScale(Canvas canvas, int aniWidth, int aniHeight, int viewWidth, int viewHeight) {
		if (this.scaleType == null || this.scaleType == ScaleType.RAW) {
			return false;
		}
		if (aniWidth == 0 || aniHeight == 0) {
			return false;
		}
		canvas.save();
		float sx = (float) viewWidth / aniWidth;
		float sy = (float) viewHeight / aniHeight;
		switch (this.scaleType) {
		case FILL:
			canvas.scale(sx, sy);
			break;
		case FILL_X:
			canvas.scale(sx, 1);
			break;
		case FILL_Y:
			canvas.scale(1f, sy);
			break;
		case ZOOM_BY_X:
			canvas.scale(sx, sx);
			break;
		case ZOOM_BY_Y:
			canvas.scale(sy, sy);
			break;
		default:
			break;
		}
		return true;
	}

	private class Drawer extends AsyncTask<Void, Void, Void> implements SurfaceHolder.Callback {

		private final ConditionVariable stopped = new ConditionVariable();
		private SurfaceHolder holder;

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			this.holder = holder;
			holder.setFormat(PixelFormat.RGBA_8888);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			this.holder = holder;
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			holder = null;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				draw();
			} catch (RuntimeException re) {
				re.printStackTrace();
			} finally {
				stopped.open();
			}
			return null;
		}

		private void draw() {
			beforeFrame(width, height);
			long startTime = now();
			while (!isCancelled()) {
				SurfaceHolder sh = this.holder;
				if (sh != null) {
					Canvas canvas = sh.lockCanvas();
					if (canvas != null) {
						try {
							long elapsedTime = now() - startTime;
							drawBackground(canvas, width, height, elapsedTime);
							drawForeground(canvas, width, height, elapsedTime);
						} catch (Exception e) {
							break;
						} finally {
							try {
								sh.unlockCanvasAndPost(canvas);
							} catch (Exception e) {
								// 在6.0的某台设备上出现过IllegalArgument异常
								break;
							}
						}
					}
				}
				sleep(SLEEP_TIME_BETWEEN_FRAME);
			}
		}

		void waitForStop() {
			stopped.block();
		}
	}

	private static void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {

		}
	}

	private static long now() {
		return SystemClock.uptimeMillis();
	}
}
