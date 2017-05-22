package cn.wsds.gamemaster.ui.floatwindow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import cn.wsds.gamemaster.R;

import com.subao.utils.MetricsUtils;

@SuppressLint("InflateParams")
public class ScreenShotMask extends FloatWindow {

	private static ScreenShotMask instance;

	public static void createInstance(Context context) {
		if (instance == null) {
			instance = new ScreenShotMask(context.getApplicationContext());
		}
	}

	private ScreenShotMask(Context context) {
		super(context);
		View view = LayoutInflater.from(context).inflate(R.layout.floatwindow_screenshot_mask, null);
		Point size = MetricsUtils.getDevicesSizeByPixels(context);
		this.addView(Type.TOAST, view, 0, 0, size.x, size.y);
	}

	@Override
	protected void onViewAdded(View view) {

	}

	@Override
	protected boolean canDrag() {
		return false;
	}
	
	@Override
	protected void destroy() {
		super.destroy();
		if (instance == this) {
			instance = null;
		}
	}

	public static void destroyInstance() {
		if (instance != null) {
			instance.destroy();
			instance = null;
		}
	}

}
