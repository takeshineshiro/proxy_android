package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class InertiaListView extends ListView {

	public InertiaListView(Context context) {
		super(context);		
	}
	
	public InertiaListView(Context context, AttributeSet attrs) {
		super(context, attrs);		
	}
	
	
	public InertiaListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(ev.getAction() ==  MotionEvent.ACTION_MOVE){
			return true ;  //禁止滑动
		}
		
		return super.dispatchTouchEvent(ev);
	}
	
	

}
