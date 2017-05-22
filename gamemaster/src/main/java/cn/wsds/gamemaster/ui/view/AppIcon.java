package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R;

public class AppIcon extends View {
	
	private final int iconWidth, iconHeight;
	private final int iconMarginRight;
	private final Paint paint;
	
	private Bitmap mark;
	private Rect rectMarkSrc, rectMarkDst;
	private boolean markVisible;
	
	private Bitmap icon;
	private Rect rectIconSrc, rectIconDst;
	
	private boolean drawParamIsValid;

	public AppIcon(Context context, AttributeSet attrs) {
		super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppIcon);
        this.iconWidth = a.getDimensionPixelSize(R.styleable.AppIcon_icon_width, 96);
        this.iconHeight = a.getDimensionPixelSize(R.styleable.AppIcon_icon_height, 96);
        this.iconMarginRight = a.getDimensionPixelSize(R.styleable.AppIcon_icon_marginRight, 0);
        this.setIcon(a.getDrawable(R.styleable.AppIcon_icon_image), false);
        //
        this.setMark(a.getDrawable(R.styleable.AppIcon_mark_image), false);
        this.setMarkVisible(a.getBoolean(R.styleable.AppIcon_mark_visible, false), false);
        a.recycle();
        //
        this.paint = new Paint();
	}
	
	public void setMarkVisible(boolean visible, boolean redraw) {
		if (this.markVisible != visible) {
			this.markVisible = visible;
			if (redraw) {
				this.invalidate();
			}
		}
	}
	
	public void setMark(Drawable d, boolean redraw) {
        if (d != null && (d instanceof BitmapDrawable)) {
        	this.mark = ((BitmapDrawable)d).getBitmap();
        } else {
        	this.mark = null;
        }
        this.drawParamIsValid = false;
        if (redraw) {
        	this.invalidate();
        }
	}
	
	public void setIcon(Drawable icon, boolean redraw) {
		if (icon != null && (icon instanceof BitmapDrawable)) {
			this.icon = ((BitmapDrawable)icon).getBitmap();
		} else {
			this.icon = null;
		}
		this.drawParamIsValid = false;
		if (redraw) {
			this.invalidate();
		}
	}
	
	private int calcMeasureSize(int measureSpec, int normalSize) {
		int mode = MeasureSpec.getMode(measureSpec);
		int size = MeasureSpec.getSize(measureSpec);
		switch (mode) {
		case MeasureSpec.EXACTLY:
			return size;
		case MeasureSpec.AT_MOST:
			return Math.min(size, normalSize);
		default:
			return normalSize;
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int cx = calcMeasureSize(widthMeasureSpec, getSuggestedMinimumWidth());
		int cy = calcMeasureSize(heightMeasureSpec, getSuggestedMinimumHeight());
		this.setMeasuredDimension(cx, cy);
	}
	
	@Override
	protected int getSuggestedMinimumWidth() {
		int w = Math.max(iconWidth, mark == null ? 0 : mark.getWidth());
		return w + getPaddingLeft() + getPaddingRight();
	}
	
	@Override
	protected int getSuggestedMinimumHeight() {
		int h = Math.max(iconHeight, mark == null ? 0 : mark.getHeight());
		return h + getPaddingTop() + getPaddingBottom();			
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (!this.drawParamIsValid) {
			recalcDrawParams();
		}
		if (icon != null) {
			canvas.drawBitmap(icon, rectIconSrc, rectIconDst, paint);
		}
		if (this.markVisible && mark != null) {
			canvas.drawBitmap(mark, rectMarkSrc, rectMarkDst, paint);
		}
	}

	private void recalcDrawParams() {
		int cx = this.getWidth();
		int cy = this.getHeight();
		//
		if (this.mark != null) {
			this.rectMarkSrc = new Rect(0, 0, mark.getWidth(), mark.getHeight());
			int x = (cx - mark.getWidth()) / 2;
			this.rectMarkDst = new Rect(x, getPaddingTop(), x + mark.getWidth(), getPaddingTop() + mark.getHeight());
		}
		//
		if (this.icon != null) {
			this.rectIconSrc = new Rect(0, 0, icon.getWidth(), icon.getHeight());
			int x = cx - iconWidth - getPaddingRight() - this.iconMarginRight;
			x /= 2;
			int y = (cy - iconHeight) / 2;
			this.rectIconDst = new Rect(x, y, x + iconWidth, y + iconHeight);
		}
		this.drawParamIsValid = true;
	}
}
