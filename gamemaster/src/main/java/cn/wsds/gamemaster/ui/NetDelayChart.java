package cn.wsds.gamemaster.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.netdelay.NetDelayDataManager;

import com.subao.utils.MetricsUtils;

/**
 * 网络时延折线图的栅格文字部分，并且还是{@link NetDelayChartLine}的容器
 */
public class NetDelayChart extends ViewGroup {

	public static class Info {
		/** 起始时间，UTC毫秒数 */
		public final long beginTime;
		/** 结束时间，UTC毫秒数 */
		public final long endTime;
		/** 最小时延值 */
		public final int minDelay;
		/** 最大时延值 */
		public final int maxDelay;
		/** 平均时延值 */
		public final int avgDelay;
		/** 超时百分比 */
		public final int timeoutPercent;

		public Info(long beginTime, long endTime, int minDelay, int maxDelay, int avgDelay, int timeoutPercent) {
			this.beginTime = beginTime;
			this.endTime = endTime;
			this.minDelay = minDelay;
			this.maxDelay = maxDelay;
			this.avgDelay = avgDelay;
			this.timeoutPercent = timeoutPercent;
		}
	}

	public interface OnUpdateListener {
		public void onNetDelayChartUpdate(NetDelayChart sender);
	}

	private final NetDelayChartLine chartLine;

	/////////// 绘制相关  ////////////////////

	private static final String[] PANEL_TEXT = new String[] { "400ms", "300ms", "200ms", "100ms", "0ms" };

	private final float widthPanelText;	// PANEL_TEXT里最宽的文字的宽度
	private final float PIXELS_PER_DP;	// 一个DP大约等于几个像素？

	private final Rect rectEntire = new Rect();		// 整个控件的大小
	private final Rect rectPanel = new Rect();		// 整个面板的位置
	private final Paint paintLine = new Paint();	// 画下边和右边实线
	private final Paint paintDashed = new Paint();	//
	private final Path path = new Path();			// 画虚线
	private final Paint paintText = new Paint();	// 画文本

	private boolean rightVertLineVisible;			// 是否绘制右边竖线

	public NetDelayChart(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.NetDelayChart);
		try {
			rightVertLineVisible = ta.getBoolean(R.styleable.NetDelayChart_right_vert_line, false);
			boolean rangeMarkVisible = ta.getBoolean(R.styleable.NetDelayChart_range_mark, false);

			//
			this.setWillNotDraw(false);
			//
			this.chartLine = new NetDelayChartLine(context, null);
			this.addView(chartLine);
			this.chartLine.setRangeMarkVisible(rangeMarkVisible);
			//
			Resources res = context.getResources();
			PIXELS_PER_DP = MetricsUtils.dp2px(context, 1f);
			//
			paintText.setFlags(Paint.ANTI_ALIAS_FLAG);
			paintText.setColor(ta.getColor(R.styleable.NetDelayChart_text_color, res.getColor(R.color.color_game_6)));
			paintText.setTextSize(ta.getDimensionPixelSize(R.styleable.NetDelayChart_text_size, (int)res.getDimension(R.dimen.text_size_10)));
			paintText.setTextAlign(Paint.Align.RIGHT);
			widthPanelText = measurePanelText(paintText);
			//
			paintLine.setColor(ta.getColor(R.styleable.NetDelayChart_line_color, res.getColor(R.color.color_game_4)));
			paintLine.setStyle(Style.STROKE);
			paintLine.setStrokeWidth(MetricsUtils.dp2px(context, 1));
			paintDashed.set(paintLine);
			paintDashed.setStrokeWidth(paintLine.getStrokeWidth() * 0.5f);
			paintDashed.setPathEffect(new DashPathEffect(new float[] { 3, 3 }, 0));
		} finally {
			ta.recycle();
		}
	}

	/**
	 * 设置是否显示范围标记
	 */
	public void setRangeMarkVisible(boolean visible) {
		this.chartLine.setRangeMarkVisible(visible);
	}

	/** 返回当前是否显示范围标记 */
	public boolean getRangeMarkVisible() {
		return this.chartLine.getRangeMarkVisible();
	}

	/** 设置：是否显示右边竖线 */
	public void setRightVertLineVisible(boolean visible) {
		if (this.rightVertLineVisible != visible) {
			this.rightVertLineVisible = visible;
			this.invalidate();
		}
	}

	/** 返回：当前是否显示右边竖线 */
	public boolean getRightVertLineVisible() {
		return this.rightVertLineVisible;
	}

	/** 图表的座标线颜色 */
	public int getLineColor() {
		return this.paintLine.getColor();
	}
	
	/** 设置图表的座标线颜色 */
	public void setLineColor(int color) {
		int old = getLineColor();
		if (old != color) {
			this.paintLine.setColor(color);
			this.invalidate();
		}
	}
	
	/**
	 * 设置最多显示多少秒的数据
	 */
	public void setDataSeconds(int seconds) {
		this.chartLine.setDataSeconds(seconds);
	}

	/**
	 * 设置数据源
	 */
	public void setDataSource(NetDelayDataManager.TimePointContainer dataSource, boolean alwaysRedraw) {
		this.chartLine.setDataSource(dataSource, alwaysRedraw);
	}

	private static float measurePanelText(Paint paintText) {
		float max = 0f;
		for (String text : PANEL_TEXT) {
			float w = paintText.measureText(text);
			if (w > max) {
				max = w;
			}
		}
		return max;
	}

	private int calcXOfCharLine() {
		return (int) (widthPanelText + 2 * PIXELS_PER_DP);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		this.rectEntire.set(0, 0, this.getMeasuredWidth(), this.getMeasuredHeight());
		this.rectPanel.set(rectEntire);
		rectPanel.inset((int) (PIXELS_PER_DP * 4), (int) (PIXELS_PER_DP * 4));
		//
		int width = MeasureSpec.makeMeasureSpec(rectPanel.width() - calcXOfCharLine(), MeasureSpec.EXACTLY);
		int height = MeasureSpec.makeMeasureSpec(rectPanel.height(), MeasureSpec.EXACTLY);
		chartLine.measure(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		chartLine.layout(rectPanel.left + calcXOfCharLine(), rectPanel.top, rectPanel.right, rectPanel.bottom);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Drawable background = this.getBackground();
		if (background != null) {
			background.draw(canvas);
		}
		drawGrid(canvas);
	}

	/** 绘制背景线条栅格以及文本 */
	private void drawGrid(Canvas canvas) {
		if (this.rightVertLineVisible) {
			canvas.drawLine(rectPanel.right, rectPanel.top, rectPanel.right, rectPanel.bottom, paintLine);
		}
		int cy = rectPanel.bottom - rectPanel.top - this.chartLine.heightOfReconnectFlag;
		int heightRow = cy >> 2;
		float y = rectPanel.bottom;
		float x = rectPanel.left + widthPanelText;
		for (int i = PANEL_TEXT.length - 1; i >= 0; --i) {
			if (i == PANEL_TEXT.length - 1) {
				canvas.drawLine(rectPanel.left, y, rectPanel.right, y, paintLine);
			} else {
				path.moveTo(rectPanel.left, y);
				path.lineTo(rectPanel.right, y);
				canvas.drawPath(path, paintDashed);
			}
			// 文字
			String text = PANEL_TEXT[i];
			canvas.drawText(text, x, y - PIXELS_PER_DP * 4, paintText);
			y -= heightRow;
		}
	}

	/**
	 * 取本次绘制的数据的时间范围 （如果TimeRange里的任何一个字段为负数，表示时间段为空）
	 */
	public Info getStatistic() {
		return this.chartLine.getInfo();
	}

	public void setOnUpdateListener(OnUpdateListener listener) {
		this.chartLine.setOnUpdateListener(this, listener);
	}

}
