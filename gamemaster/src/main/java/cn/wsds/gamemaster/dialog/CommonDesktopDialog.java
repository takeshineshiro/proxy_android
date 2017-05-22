package cn.wsds.gamemaster.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;

import com.subao.utils.MetricsUtils;

/**
 * 桌面对话框
 */
public class CommonDesktopDialog extends CommonDialog{
	
	private WindowManager mWindow;//桌面管理
	private LayoutParams params;//布局属性
	private boolean isShowing = false;//是否已经打开
	public OnDismissListener onDismissLisener;

	public CommonDesktopDialog() {
		this(AppMain.getContext(), R.style.AppDialogTheme);
	}

	private CommonDesktopDialog(Context context, int theme) {
		super(context,theme);
		mWindow = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		initPoint(context);
	}

	/**
	 * 设置左面布局属性
	 * @param context 上下文对象获取桌面布局属性
	 */
	private void initPoint(Context context) {
		Point point = MetricsUtils.getDevicesSizeByPixels(context);
		int width = point.x > point.y ? point.y : point.x;
		params = new LayoutParams();
		params.width = (int) (width * 0.9f);
		params.height = LayoutParams.WRAP_CONTENT;
		params.type = LayoutParams.TYPE_PRIORITY_PHONE;
		params.format = PixelFormat.RGBA_8888;
		params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
				| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
				| LayoutParams.FLAG_NOT_FOCUSABLE;
		params.gravity = Gravity.CENTER;
	}
	
	@Override
	public void cancel() {
		dismiss();
	}
	
	/**
     * Sets whether this dialog is canceled when touched outside the window's
     * bounds. If setting to true, the dialog is set to be cancelable if not
     * already set.
     * 
     * @param cancel Whether the dialog should be canceled when touched outside
     *            the window.
     */
	@Override
    public void setCanceledOnTouchOutside(boolean cancel) {
	    if(cancel){
	    	layout.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if(event.getAction() == MotionEvent.ACTION_OUTSIDE){
						dismiss();
					}
					return false;
				}
			});
	    }else{
	    	layout.setOnTouchListener(null);
	    }
	}
    
	/**
	 * Set a listener to be invoked when the dialog is dismissed.
	 * @param listener The {@link DialogInterface.OnDismissListener} to use.
	 */
	@Override
	public void setOnDismissListener(OnDismissListener onDismissLisener) {
		this.onDismissLisener = onDismissLisener;
	}

	@Override
	public void dismiss() {
		if(isShowing){
			mWindow.removeView(layout);
			isShowing = false;
			if (onDismissLisener != null) {
				onDismissLisener.onDismiss(this);
			}
		}
	}

	@Override
	public void show() {
		if(!isShowing){
			adjustButtonLayout();
			mWindow.addView(layout, params);
			isShowing = true;
		}
	}

	public boolean isShowing() {
		return isShowing;
	}
}
