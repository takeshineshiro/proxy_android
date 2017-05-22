package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R;

/**
 * 表示百分比的圆环
 */
public class PercentRing extends View {

	private Drawable ringForeground;
	private final Drawable ringBackground;
	private final int suggestedMinimumWidth, suggestedMinimumHeight;
	private final RectF bounds = new RectF();
	private Paint paintOfRingDraw;	// 如果使用drawPath()绘制前景，此为画笔
	private Path path;
	private int width, height;
	private int percent;

	public PercentRing(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PercentRing);
		try {
			ringBackground = ta.getDrawable(R.styleable.PercentRing_backgroundRing);
			ringForeground = ta.getDrawable(R.styleable.PercentRing_foregroundRing);
			setPercent(ta.getInt(R.styleable.PercentRing_percent, 0), false);
		} finally {
			ta.recycle();
		}
		this.suggestedMinimumWidth = getDrawableWidth(ringBackground);
		this.suggestedMinimumHeight = getDrawableHeight(ringBackground);
		//
		// 因为用了clipPath()，所以关闭硬件加速，否则在某些低版本手机下会崩溃
		this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	private static int getDrawableWidth(Drawable d) {
		return d == null ? 0 : d.getIntrinsicWidth();
	}

	private static int getDrawableHeight(Drawable d) {
		return d == null ? 0 : d.getIntrinsicHeight();
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		return suggestedMinimumWidth;
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		return suggestedMinimumHeight;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(calcSize(getSuggestedMinimumWidth(), widthMeasureSpec),
			calcSize(getSuggestedMinimumHeight(), heightMeasureSpec));
		this.width = getMeasuredWidth();
		this.height = getMeasuredHeight();
		this.bounds.set(0, 0, width, height);
	}

	private static int calcSize(int size, int measureSpec) {
		switch (MeasureSpec.getMode(measureSpec)) {
		case MeasureSpec.UNSPECIFIED:
		case MeasureSpec.AT_MOST:
			return size;
		default:
			return MeasureSpec.getSize(measureSpec);
		}
	}

	/**
	 * 设置前景圆环Drawable
	 * 
	 * @param resId
	 *            资源ID
	 */
	@SuppressWarnings("deprecation")
	public void setForegroundRing(int resId) {
		ringForeground = getContext().getResources().getDrawable(resId);
		paintOfRingDraw = null;
		invalidate();
	}

	/**
	 * 设置百分比
	 * 
	 * @param percent
	 *            [0,100]区间内的值
	 */
	public void setPercent(int percent) {
		setPercent(percent, true);
	}

	private void setPercent(int percent, boolean redraw) {
		if (percent < 0) {
			percent = 0;
		} else if (percent > 100) {
			percent = 100;
		}
		if (this.percent != percent) {
			this.percent = percent;
			if (redraw) {
				this.invalidate();
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawBackground(canvas);
		drawForeground(canvas);
	}

	private void setDrawableBounds(Drawable d) {
		int x = (this.width - d.getIntrinsicWidth()) >> 1;
		int y = (this.height - d.getIntrinsicHeight()) >> 1;
		d.setBounds(x, y, x + d.getIntrinsicWidth(), y + d.getIntrinsicHeight());
	}

	private void drawBackground(Canvas canvas) {
		if (percent != 100 && ringBackground != null) {
			setDrawableBounds(ringBackground);
			ringBackground.draw(canvas);
		}
	}

	private void drawForeground(Canvas canvas) {
		if (ringForeground == null || percent == 0) {
			return;
		}
		setDrawableBounds(ringForeground);
		if (percent >= 100) {
			ringForeground.draw(canvas);
			return;
		}
		//
		canvas.save();
		try {
			canvas.clipPath(calcPath());
			ringForeground.draw(canvas);
			return;
		} catch (UnsupportedOperationException e) {
		} finally {
			canvas.restore();
		}
		// clipPath()失败了，尝试用drawPath()绘制
		if (!drawByShader(canvas)) {
			ringForeground.draw(canvas);
		}
		
	}

	private boolean drawByShader(Canvas canvas) {
		if (ringForeground instanceof BitmapDrawable) {
			if (paintOfRingDraw == null) {
				paintOfRingDraw = new Paint();
				Shader shader = new BitmapShader(((BitmapDrawable) ringForeground).getBitmap(), TileMode.CLAMP, TileMode.CLAMP);
				paintOfRingDraw.setShader(shader);
			}
			canvas.drawPath(calcPath(), paintOfRingDraw);
			return true;
		} else {
			return false;
		}
	}

	private Path calcPath() {
		if (path == null) {
			path = new Path();
		}
		// 生成扇形Path
		float xCenter = width * 0.5f;
		float yCenter = height * 0.5f;
		path.moveTo(xCenter, yCenter);
		// 注意path API的0度是从3点钟方向开始的，但
		// 我们业务逻辑里的0度是从12点方向开始
		path.arcTo(bounds, -90f, percent * 3.6f);
		return path;
	}

}
