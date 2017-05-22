package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R;

/**
 * 自定义圆形进度条
 */
public class CircleProgres extends View {

	/** 外圆的直径 */
	private final int outDiameter;
	/** 圆环的宽度 */
	private final int ringWidth;
	/** 起始角度 */
	private final int startAngle;
	/** 圆环背景色 */
	private final int ringBackgroundColor;
	/** 进度条进度颜色 */
	private int ringColor;
	/** 圆环进度 画笔 */
	private final Paint ringProgresPaint = new Paint();
	/** 圆环背景色画笔 */
	private final Paint ringBackgroundPaint = new Paint();
	private final RectF mRect = new RectF();

	/**　最大进度 */
	public static final int DEFAULT_MAX_PROGRESS = 100;
	/** 当前进度 0- DEFAULT_MAX_PROGRESS */
	private int mProgress;

	private int maxProgress = DEFAULT_MAX_PROGRESS;
	private int minProgress = 0;

	public CircleProgres(Context context) {
		this(context, null);
	}
	public CircleProgres(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public CircleProgres(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleProgres, defStyle, 0);
		startAngle = ta.getInt(R.styleable.CircleProgres_start_angle, -90);
		ringColor = ta.getColor(R.styleable.CircleProgres_ring_progres_color, Color.RED);
		ringWidth = ta.getDimensionPixelSize(R.styleable.CircleProgres_ring_width,4);
		outDiameter = ta.getDimensionPixelSize(R.styleable.CircleProgres_outer_diameter, 68);
		ringBackgroundColor = ta.getColor(R.styleable.CircleProgres_ring_background_color, Color.BLACK);
		ta.recycle();
		init();
	}
	
	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}
	public int getMaxProgress() {
		return maxProgress;
	}
	
	public void setMinProgress(int minProgress) {
		this.minProgress = minProgress;
	}
	
	private void init() {
		ringProgresPaint.setStyle(Paint.Style.STROKE);
		ringProgresPaint.setAntiAlias(true);
		ringProgresPaint.setStrokeWidth(ringWidth);
		ringProgresPaint.setStrokeCap(Cap.ROUND);
		ringProgresPaint.setColor(ringColor);
		
		ringBackgroundPaint.set(ringProgresPaint);
		ringBackgroundPaint.setColor(ringBackgroundColor);

		int delta = outDiameter - ringWidth;
		mRect.set(new RectF(ringWidth, ringWidth, delta, delta));
		mProgress = 0;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawArc(mRect, 0, 360, false, ringBackgroundPaint);
		if (mProgress != 0) {
			//FIXME
			float degree = mProgress * 360.0f / (maxProgress - minProgress);
			canvas.drawArc(mRect, startAngle, degree, false, ringProgresPaint);
		}
	}

	@Override
	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(outDiameter, outDiameter);
	}

	public boolean setProgress(int progress) {
		if(progress < minProgress){
			progress = minProgress;
		}else if(progress > maxProgress){
			progress = maxProgress;
		}
		mProgress = progress;
		invalidate();
		return true;
	}

	public void postProgress(final int progress) {
		post(new Runnable() {
			@Override
			public void run() {
				setProgress(progress);
			}
		});
	}

	public void setRingColor(int arcColor) {
		this.ringColor = arcColor;
		ringProgresPaint.setColor(ringColor);
		invalidate();
	}
	public int getProgress() {
		return mProgress;
	}
	
	

}
