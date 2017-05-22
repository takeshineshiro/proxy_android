package cn.wsds.gamemaster.ui.view;

import java.util.Calendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

public class TimeTextView extends TextView {

	public final static String DEFAULT_FORMAT = "kk:mm:ss";

	/** 显示当前时刻 */
	public final static int TYPE_CLOCK = 0;

	/** 显示流逝的时间 */
	public final static int TYPE_ELAPSED = 1;

	private int type;
	private String clockFormat;
	private Handler handler;

	private final Calendar calendar = Calendar.getInstance();
	private int elapsedSeconds;

	public TimeTextView(Context context) {
		this(context, null);
	}

	public TimeTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TimeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TimeTextView);
		this.type = ta.getInt(R.styleable.TimeTextView_type, TYPE_CLOCK);
		setClockFormat(ta.getString(R.styleable.TimeTextView_clockFormat));
		this.elapsedSeconds = ta.getInt(R.styleable.TimeTextView_elapsedSeconds, 0);
		ta.recycle();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (handler == null) {
			handler = new Handler();
			Runnable r = new Runnable() {
				@Override
				public void run() {
					if (elapsedSeconds >= 0) {
						++elapsedSeconds;
					}
					changeText();
					invalidate();
					long now = SystemClock.uptimeMillis();
					long next = now + (1000 - now % 1000);
					handler.postAtTime(this, next);
				}
			};
			r.run();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
			handler = null;
		}
	}

	/**
	 * 设置时钟显示格式
	 * 
	 * @param fmt
	 *            格式字串，例如 "k:mm:ss"
	 */
	public void setClockFormat(String fmt) {
		clockFormat = fmt;
		if (TextUtils.isEmpty(clockFormat)) {
			clockFormat = DEFAULT_FORMAT;
		}
	}

	/** 获取当前的时钟显示格式 */
	public String getClockFormat() {
		return clockFormat;
	}

	/**
	 * 获取当前控件类型
	 * 
	 * @return {@link #TYPE_CLOCK} or {@link #TYPE_ELAPSED}
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * 设置当前控件类型
	 * 
	 * @param type
	 *            {@link #TYPE_CLOCK} or {@link #TYPE_ELAPSED}
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * 设置当前流逝的秒数
	 * 
	 * @param value
	 *            流逝的秒数。如果为负，则显示"--:--:--"
	 */
	public void setElapsedSeconds(int value) {
		this.elapsedSeconds = value;
	}

	public int getElapsedSeconds() {
		return this.elapsedSeconds;
	}

	private void changeText() {
		if (this.type == TYPE_CLOCK) {
			calendar.setTimeInMillis(System.currentTimeMillis());
			setText(DateFormat.format(clockFormat, calendar));
		} else {
			if (elapsedSeconds < 0) {
				setText("--:--:--");
			} else {
				int remain = elapsedSeconds;
				int hours = remain / 3600;
				remain -= hours * 3600;
				int minutes = remain / 60;
				int seconds = remain - minutes * 60;
				setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
			}
		}
	}

}
