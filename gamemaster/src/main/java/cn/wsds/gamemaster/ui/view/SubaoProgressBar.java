package cn.wsds.gamemaster.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import cn.wsds.gamemaster.R;

@SuppressLint("RtlHardcoded")
public class SubaoProgressBar extends View {

	private final Paint paintText = new Paint();

	private final Drawable bar;

	private int percent;
	private String text = "0%";
	private final FontMetrics fontMetrics = new FontMetrics();

	private boolean showText = true;

	public SubaoProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		paintText.setAntiAlias(true);
		paintText.setTextAlign(Paint.Align.CENTER);
		//
		Drawable bar = null;
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SubaoProgressBar);
		try {
			for (int i = ta.getIndexCount() - 1; i >= 0; --i) {
				int attr = ta.getIndex(i);
				switch (attr) {
				case R.styleable.SubaoProgressBar_textSize:
					paintText.setTextSize(ta.getDimensionPixelSize(attr, 15));
					break;
				case R.styleable.SubaoProgressBar_textColor:
					paintText.setColor(ta.getColor(attr, 0xffffffff));
					break;
				case R.styleable.SubaoProgressBar_bar:
					bar = ta.getDrawable(attr);
					break;
				case R.styleable.SubaoProgressBar_percent:
					setPercent(ta.getInt(attr, 0));
					break;
				case R.styleable.SubaoProgressBar_showText:
					this.showText = ta.getBoolean(attr, true);
					break;
				}
			}
		} finally {
			ta.recycle();
		}
		//
		if (bar == null) {
			this.bar = null;
		} else {
			this.bar = new ClipDrawable(bar, Gravity.LEFT, ClipDrawable.HORIZONTAL);
		}
		//
		paintText.getFontMetrics(this.fontMetrics);
	}

	public void setPercent(int percent) {
		if (percent > 100) {
			percent = 100;
		} else if (percent < 0) {
			percent = 0;
		}
		if (percent != this.percent) {
			this.percent = percent;
			this.text = Integer.toString(percent) + "%";
			this.invalidate();
		}
	}

	public void setShowText(boolean value) {
		if (this.showText != value) {
			this.showText = value;
			this.invalidate();
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (percent > 0 && bar != null) {
			bar.setLevel(percent * 100);
			bar.setBounds(0, 0, getWidth(), getHeight());
			bar.draw(canvas);
		}
		if (this.showText) {
			float x = this.getWidth() >> 1;
			float y = (this.getHeight() - fontMetrics.bottom + fontMetrics.top) * 0.5f - fontMetrics.top;
			canvas.drawText(text, x, y, paintText);
		}

	}
}
