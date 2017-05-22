package cn.wsds.gamemaster.ui.memoryclean.animation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class SmoothGridView extends ViewGroup {

	public SmoothGridView(Context context) {
		super(context);
	}

	public SmoothGridView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public SmoothGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (!changed) {
			return;
		}
		int top = 0;
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			child.layout(0, top, r, top += child.getMeasuredHeight());
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = 0;
		int height = 0;
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			measureChild(child, widthMeasureSpec, heightMeasureSpec);
			width = child.getMeasuredWidth();
			height += child.getMeasuredHeight();
		}
		setMeasuredDimension(resolveSize(width, widthMeasureSpec),height);
	}
}
