package cn.wsds.gamemaster.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R.styleable;

/**
 * 主页有个需求是将ImageView降低透明度，但是ImageView会直接设置Drawable的Alpha值，
 * 从而使桌面快捷方式里的相同引用的Drawable也降低了Alpha值。
 * 所以特意做此控件
 */
public class ImageViewAlpha extends View {
	
	private static abstract class Strategy {
		abstract int getDrawableAlpha(Drawable d);
	}
	
	private static class StrategyHighVersion extends Strategy {
		
		@TargetApi(Build.VERSION_CODES.KITKAT)
		@Override
		int getDrawableAlpha(Drawable d) {
			try {
				return d.getAlpha();
			} catch (NoSuchMethodError e) {
				// 注意，在有些坑爹的手机上面，4.4及以上版本
				// 居然也报找不到方法的异常 ！！！
				// 所以这里要捕捉
				// 【每一个做Android的程序员上辈子都是折翼的天使】
				return 255;
			}
		}
	}
	
	private static class StrategyLowVersion extends Strategy {

		@Override
		int getDrawableAlpha(Drawable d) {
			return 255;
		}
		
	}
	
	private Drawable image;
	private int drawAlpha;
	private final Strategy strategy;
	private final Rect bounds = new Rect();
	private final Rect saveBounds = new Rect();

	public ImageViewAlpha(Context context, AttributeSet attrs) {
		super(context, attrs);
    	this.strategy = createStrategy();
		TypedArray ta = context.obtainStyledAttributes(attrs, styleable.ImageViewAlpha);
		try {
			image = ta.getDrawable(styleable.ImageViewAlpha_image);
			setDrawAlpha( ta.getInt(styleable.ImageViewAlpha_drawAlpha, 255), false );
		} finally {
			ta.recycle();
		}
	}
	
	private static Strategy createStrategy() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			return new StrategyHighVersion();
		} else {
			return new StrategyLowVersion();
		}
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		return image == null ? 0 : image.getIntrinsicWidth();
	}
	
	@Override
	protected int getSuggestedMinimumHeight() {
		return image == null ? 0 : image.getIntrinsicHeight();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(calcSize(getSuggestedMinimumWidth(), widthMeasureSpec),
        	calcSize(getSuggestedMinimumHeight(), heightMeasureSpec));
        bounds.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
	}
	
    private static int calcSize(int size, int measureSpec) {
        switch (MeasureSpec.getMode(measureSpec)) {
        case MeasureSpec.EXACTLY:
            return MeasureSpec.getSize(measureSpec);
        default:
        	return size;
        }
    }
    
    public void setImageDrawable(Drawable d) {
    	if (this.image != d) {
    		this.image = d;
    		this.invalidate();
    	}
    }
    
    public void setDrawAlpha(int alpha) {
    	setDrawAlpha(alpha, true);
    }
    private void setDrawAlpha(int alpha, boolean redraw) {
    	if (alpha < 0) {
    		alpha = 0;
    	} else if (alpha > 255) {
    		alpha = 255;
    	}
    	if (alpha != this.drawAlpha) {
    		this.drawAlpha = alpha;
    		if (redraw) {
    			this.invalidate();
    		}
    	}
    }
    
	@Override
    public void draw(Canvas canvas) {
    	if (this.image == null) {
    		return;
    	}
    	if (this.drawAlpha <= 0) {
    		return;
    	}
    	int saveAlpha = strategy.getDrawableAlpha(this.image);
    	//
    	saveBounds.set(image.getBounds());    	
    	image.setBounds(bounds);
    	image.setAlpha(drawAlpha);
    	image.draw(canvas);
    	image.setAlpha(saveAlpha);
    	image.setBounds(saveBounds);
    }
}
