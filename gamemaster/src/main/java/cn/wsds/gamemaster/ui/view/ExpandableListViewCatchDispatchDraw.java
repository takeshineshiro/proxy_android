package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ExpandableListView;
import cn.wsds.gamemaster.ErrorReportor;

public class ExpandableListViewCatchDispatchDraw extends ExpandableListView {

	public ExpandableListViewCatchDispatchDraw(Context context) {
		super(context);
	}

	public ExpandableListViewCatchDispatchDraw(Context context,
		AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public ExpandableListViewCatchDispatchDraw(Context context,
		AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		try {
			super.dispatchDraw(canvas);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReportor.postCatchedException(e);
		}
	}

}
