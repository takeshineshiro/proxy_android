package cn.wsds.gamemaster.ui;

import java.util.Iterator;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.netdelay.NetDelayDataManager;
import cn.wsds.gamemaster.netdelay.NetDelayDataManager.TimePoint;
import cn.wsds.gamemaster.ui.NetDelayChart.OnUpdateListener;

import com.subao.resutils.CommonUIResources;
import com.subao.utils.MetricsUtils;

/**
 * 网络延时图的折线部分
 */
public class NetDelayChartLine extends View {

	private static final int MAX_DELAY_DRAW = 400;			// 所能画出的最大延时
	private final float PIXELS_PER_DP;	// 一个DP大约等于几个像素？
	
	private final Rect rectReconnectHint = new Rect();

	private final Paint paintLineNormal = new Paint();		// 绘制常规线段的Paint
	private final Paint paintLineAbnormal = new Paint();	// 绘制异常线段的Paint
	private final Paint paintRangeMark = new Paint();		// 绘制范围标记的Paint
	
	private final static CommonUIResources commonUIRes = new CommonUIResources();
	private final Bitmap bitmapReconnectHintText, bitmapReconnectHintArrow;	// 断线重连Hint的图片

	private NetDelayDataManager.TimePointContainer dataSource;
	private int dataSeconds;				// 总共要画多少秒的数据？

	private int yMinValue, yMaxValue;	// 延时的最小值和最大值应该绘制在哪个Y坐标内
										// 注意：这两个值是屏幕坐标点，所以yMinValue是大于yMaxValue的
	
	// 为避免在onDraw里频繁创建对象，预先构造两个DrawParam
	private final DrawParam leftDrawParam = new DrawParam();
	private final DrawParam rightDrawParam = new DrawParam();
	
	// 本次绘制的起止时间范围
	private long beginTime = -1, endTime = -1;
	
	// 本次绘制数据的最大、最小和平均值
	private int maxDelay = -1, minDelay = -1, avgDelay = -1;
	
	// 本次绘制数据的超时百分比
	private int timeoutPercent;

	private NetDelayChart owner;
	private OnUpdateListener onUpdateListener;

	public final int heightOfReconnectFlag;	// 断线重线图标的高度

	// 是否显示范围标志
	private boolean rangeMarkVisible;

	// 范围标志线的高度
	private final float heightOfRangeMark;	

	/**
	 * 绘制参数
	 */
	private static class DrawParam {
		public float x, y;				// 坐标点
		public boolean isAbnormal;		// 是异常值吗？

		public void set(float x, float y, boolean isAbnormal) {
			this.x = x;
			this.y = y;
			this.isAbnormal = isAbnormal;
		}

		public void assignFrom(DrawParam other) {
			this.set(other.x, other.y, other.isAbnormal);
		}
	}
	
	public NetDelayChartLine(Context context, AttributeSet attrs) {
		super(context, attrs);
		Resources res = context.getResources();
		paintLineAbnormal.setColor(res.getColor(R.color.color_game_16));
		paintLineAbnormal.setStyle(Paint.Style.STROKE);
		paintLineAbnormal.setStrokeWidth(MetricsUtils.dp2px(context, 1));
		paintLineNormal.set(paintLineAbnormal);
		paintLineNormal.setColor(res.getColor(R.color.color_game_6));
		paintLineNormal.setFlags(Paint.ANTI_ALIAS_FLAG);
		paintRangeMark.set(paintLineAbnormal);
		paintRangeMark.setColor(res.getColor(R.color.color_game_32));
		//
		bitmapReconnectHintText = commonUIRes.getBitmap(context, R.drawable.list_reconnection_hint_text);
		bitmapReconnectHintArrow = commonUIRes.getBitmap(context, R.drawable.list_reconnection_hint_arrow);
		//
		this.heightOfReconnectFlag = ((bitmapReconnectHintText == null) ? 0 : bitmapReconnectHintText.getHeight())
			+ ((bitmapReconnectHintArrow == null) ? 0 : bitmapReconnectHintArrow.getHeight());
		this.heightOfRangeMark = MetricsUtils.dp2px(context, 4);
		this.PIXELS_PER_DP = MetricsUtils.dp2px(context, 1);
	}

	/**
	 * 设置是否显示范围标记
	 */
	public void setRangeMarkVisible(boolean visible) {
		if (this.rangeMarkVisible != visible) {
			this.rangeMarkVisible = visible;
			this.invalidate();
		}
	}

	/** 返回当前是否显示范围标记 */
	public boolean getRangeMarkVisible() {
		return this.rangeMarkVisible;
	}
	
	/**
	 * 设置数据源
	 */
	public void setDataSource(NetDelayDataManager.TimePointContainer dataSource, boolean alwaysRedraw) {
		if (!alwaysRedraw) {
			alwaysRedraw = (this.dataSource != dataSource);
		}
		this.dataSource = dataSource;
		if (alwaysRedraw) {
			this.invalidate();
		}
	}

	/**
	 * 设置“最多画多少秒的数据”？
	 */
	public void setDataSeconds(int seconds) {
		if (this.dataSeconds != seconds) {
			this.dataSeconds = seconds;
			this.invalidate();
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		this.yMinValue = bottom - top;
		this.yMaxValue = this.heightOfReconnectFlag;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		doDraw(canvas);
		if (this.onUpdateListener != null) {
			this.onUpdateListener.onNetDelayChartUpdate(this.owner);
		}
	}
	
	private void doDraw(Canvas canvas) {
		this.beginTime = this.endTime = -1;
		this.minDelay = 100000;
		this.maxDelay = this.avgDelay = 0;
		this.timeoutPercent = 0;
		if (this.dataSource == null || this.dataSeconds <= 0 || this.dataSource.getCount() <= 0) {
			return;
		}
		float xEnd = -1;	// 范围的结束位置X坐标
		int idxTimePointOfReconnect = -1;
		final float cxGrid = (getWidth() << 1) / (float)this.dataSeconds;	// 每一格的像素差
		TimePoint rightTimePoint = null;		// 较右边的TimePoint（时刻更大，更接近于现在）
		int idx = 0;
		int delayCalcCount = 0;
		int delayCalcTotal = 0;
		final long timeRange = this.dataSeconds * 1000;
		int howManyPoint = 0;
		int timeoutPoint = 0;
		// 自右向左画点，较晚（较大、更接近现在）的时刻在右边，较早（较小、更接近过去）的时刻在左边
		Iterator<NetDelayDataManager.TimePoint> it = this.dataSource.descendingIterator();
		while (it.hasNext()) {
			++howManyPoint;
			NetDelayDataManager.TimePoint leftTimePoint = it.next();	// 较左（时刻值更小、更接近于过去）的TimePoint
			//
			int delay = leftTimePoint.value;
			if (delay >= 0 && delay < GlobalDefines.NET_DELAY_TIMEOUT) {
				++delayCalcCount;
				delayCalcTotal += delay;
				if (delay < minDelay) {
					minDelay = delay;
				}
				if (delay > maxDelay) {
					maxDelay = delay;
				}
			} else {
				++timeoutPoint;
			}
			if (rightTimePoint == null) {
				this.endTime = this.beginTime = leftTimePoint.time;
			}
			if (endTime - leftTimePoint.time > timeRange) {
				break;
			}
			this.beginTime = leftTimePoint.time;
			//
			calcDrawParam(leftTimePoint, rightTimePoint, cxGrid, leftDrawParam, rightDrawParam);
			//
			if (rightTimePoint != null && !leftTimePoint.hasEvent(TimePoint.EVENT_BACKGROUND)) {
				// 仅当较近的时间点不为空，且本点不是“切换到后台点”时，才画连线
				Paint paint;
				if (rightDrawParam.isAbnormal && leftDrawParam.isAbnormal) {
					paint = paintLineAbnormal;
				} else {
					paint = paintLineNormal;
				}
				canvas.drawLine(leftDrawParam.x, leftDrawParam.y, rightDrawParam.x, rightDrawParam.y, paint);
			}
			//
			// 绘制断线重连的Hint
			if (leftTimePoint.hasEvent(TimePoint.EVENT_RECONNECT_BEGIN)) {
				if (idxTimePointOfReconnect < 0 || (idx - idxTimePointOfReconnect) > 30) {
					if (drawReconnectHint(canvas, leftDrawParam.x)) {
						idxTimePointOfReconnect = idx;
					}
				}
			}
			rightDrawParam.assignFrom(leftDrawParam);
			rightTimePoint = leftTimePoint;
			xEnd = leftDrawParam.x;
			++idx;
		}
		//
		if (this.rangeMarkVisible) {
			drawRangeMark(canvas, xEnd < 0 ? 0 : xEnd);
			drawRangeMark(canvas, this.getWidth() - this.PIXELS_PER_DP);
		}
		//
		if (delayCalcCount == 0) {
			this.avgDelay = this.maxDelay = this.minDelay = -1;
		} else {
			this.avgDelay = delayCalcTotal / delayCalcCount;
		}
		//
		if (howManyPoint != 0) {
			this.timeoutPercent = timeoutPoint * 100 / howManyPoint;
		}
		if (endTime - beginTime > timeRange) {
			beginTime = endTime - timeRange;
		}

	}

	private void drawRangeMark(Canvas canvas, float x) {
		canvas.drawLine(x, this.yMinValue, x, this.yMinValue - this.heightOfRangeMark, paintRangeMark);
	}
	
	/**
	 * 计算坐标点
	 * 
	 * @param leftTimePoint
	 *            左边（较早）点的逻辑数据
	 * @param rightTimePoint
	 *            右边（较晚）点的逻辑数据，可以为null
	 * @param cxGrid
	 *            每一“格”的像素宽度
	 * @param outLeftDrawParam
	 *            用于返回绘制左边点的参数，由本函数填充值
	 * @param rightDrawParam
	 *            右边点的绘制参数
	 */
	private void calcDrawParam(TimePoint leftTimePoint, TimePoint rightTimePoint, float cxGrid, DrawParam outLeftDrawParam, DrawParam rightDrawParam) {
		// 确定X位置
		float x;
		if (rightTimePoint == null) {
			x = getWidth() - PIXELS_PER_DP;	// 第一个点，X=控件的宽度
		} else {
			long delta = (rightTimePoint.time - leftTimePoint.time) / 2000;
			if (delta > 0) {
				x = rightDrawParam.x - delta * cxGrid;
			} else {
				x = rightDrawParam.x; // - cxGrid;
			}
		}
		// 填充绘制本点的各参数	
		if (leftTimePoint.value < 0 || leftTimePoint.value >= MAX_DELAY_DRAW) {
			outLeftDrawParam.set(x, yMaxValue, true);
		} else if (leftTimePoint.value == 0) {
			outLeftDrawParam.set(x, yMinValue, false);
		} else {
			int y = yMinValue - leftTimePoint.value * (yMinValue - yMaxValue) / MAX_DELAY_DRAW;
			outLeftDrawParam.set(x, y, false);
		}
	}

	private boolean drawReconnectHint(Canvas canvas, float x) {
		if (x <= 0) {
			return false;
		}
		if (bitmapReconnectHintText == null || bitmapReconnectHintArrow == null) {
			return false;
		}
		//
		final int cxText = bitmapReconnectHintText.getWidth();
		final int cyText = bitmapReconnectHintText.getHeight();
		final int cxArrow = bitmapReconnectHintArrow.getWidth();
		final int cyArrow = bitmapReconnectHintArrow.getHeight();
		final int maxX = this.getWidth();
		// 文字框
		int left = (int)x - (cxText >> 1);
		if (left < 0) {
			left = 0;
		} else if (left + cxText > maxX) {
			left = maxX - cxText;
		}
		int top = yMaxValue - cyText - cyArrow;
		rectReconnectHint.set(left, top, left + cxText, top + cyText);
		canvas.drawBitmap(bitmapReconnectHintText, null, rectReconnectHint, null);
		// 箭头
		left = (int)x - (cxArrow >> 1);
		top = yMaxValue - cyArrow;
		rectReconnectHint.set(left, top, left + cxArrow, top + cyArrow);
		canvas.drawBitmap(bitmapReconnectHintArrow, null, rectReconnectHint, null);
		return true;
	}

	/**
	 * 取本次绘制的数据的时间范围
	 */
	public NetDelayChart.Info getInfo() {
		return new NetDelayChart.Info(this.beginTime, this.endTime, this.minDelay, this.maxDelay, this.avgDelay, this.timeoutPercent);
	}

	public void setOnUpdateListener(NetDelayChart owner, OnUpdateListener listener) {
		this.owner = owner;
		this.onUpdateListener = listener;		
	}
}
