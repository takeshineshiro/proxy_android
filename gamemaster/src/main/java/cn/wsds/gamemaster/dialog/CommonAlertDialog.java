package cn.wsds.gamemaster.dialog;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import cn.wsds.gamemaster.R;

/**
 * 自定义普通对话框
 */
public class CommonAlertDialog extends CommonDialog {

	private Activity activity;
	private boolean cancelOnWindowLoseFocused;

	public CommonAlertDialog(Activity activity) {
		this(activity, R.style.AppDialogTheme);
	}
	
	protected Activity getActivity() {
		return activity;
	}

	private CommonAlertDialog(Activity activity, int theme) {
		super(activity, theme);
		this.activity = activity;
		this.setContentView(layout);

		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
		int width = displayMetrics.widthPixels > displayMetrics.heightPixels ? displayMetrics.heightPixels : displayMetrics.widthPixels;
		lp.width = (int) (width * 0.9);
		dialogWindow.setGravity(Gravity.CENTER);
		dialogWindow.setAttributes(lp);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (cancelOnWindowLoseFocused) {
			if (!hasFocus) {
				dismiss();
			}
		}
	}

	@Override
	public void show() {
		if (this.activity != null) {
			if (this.activity.isFinishing()) {
				this.activity = null;
			} else {
				super.show();
			}
		}
	}

}
