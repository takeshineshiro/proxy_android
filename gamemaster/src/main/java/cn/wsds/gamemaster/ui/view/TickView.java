package cn.wsds.gamemaster.ui.view;

/**
 * Created by hujd on 17-2-17.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import cn.wsds.gamemaster.R;

/**
 * @author hujd
 */
public class TickView extends View {

	protected Paint tickPaint = new Paint();
	/**
	 * 画笔宽度
	 */
	private int strokeWidth = 5;

	/**
	 * 测量打钩
	 */
	protected PathMeasure tickPathMeasure;

	public TickView(Context context) {
		super(context);
		init(context);
	}

	public TickView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public TickView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		float radius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 4 - strokeWidth;

		//初始化打钩路径
		Path tickPath = new Path();
		float startX = getMeasuredWidth() / 2.0f;
		float startY = getMeasuredHeight() / 2.0f;
		tickPath.moveTo(startX - (0.5f * radius +strokeWidth), startY);
		tickPath.lineTo(startX, startY + (0.3f * radius +strokeWidth));
		tickPath.lineTo(startX + radius + strokeWidth, startY -  radius);
		tickPathMeasure = new PathMeasure(tickPath,false);

		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}

	private void init(Context context){
		setTickPaint(context);
		setTickAni();
	}

	protected void setTickAni() {

	}

	private void setTickPaint(Context context) {
		tickPaint.setColor(ContextCompat.getColor(context, R.color.color_game_10));
		tickPaint.setAntiAlias(true);
		tickPaint.setStrokeWidth(strokeWidth);
		tickPaint.setStyle(Paint.Style.STROKE);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawTick(canvas);
	}

	/**
	 * 绘制打钩
	 * @param canvas canvas
	 */
	protected void drawTick(Canvas canvas) {
		Path path = new Path();
        /*
         * On KITKAT and earlier releases, the resulting path may not display on a hardware-accelerated Canvas.
         * A simple workaround is to add a single operation to this path, such as dst.rLineTo(0, 0).
         */
		tickPathMeasure.getSegment(0, tickPathMeasure.getLength(), path, true);
		path.rLineTo(0, 0);
		canvas.drawPath(path, tickPaint);
	}
}