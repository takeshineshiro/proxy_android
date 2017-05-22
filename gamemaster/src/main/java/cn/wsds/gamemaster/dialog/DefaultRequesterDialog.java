package cn.wsds.gamemaster.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.view.LoadingRing;
import cn.wsds.gamemaster.ui.view.LoadingRing.OnCompleteListener;

/**
 * 一个公用的Loading效果的“对话框”
 */
public class DefaultRequesterDialog extends Dialog {

	private static DefaultRequesterDialog instance;

	private static int showCount = 0;

	/**
	 * 显示一个Loading效果。如果效果已存在，则引用计数递增。<br />
	 * 调用N次本函数，必须保证调用N次 {@link #decShow()} !!<br />
	 * 意即 {@link #incShow(Activity)}和 {@link #decShow()}
	 * <b>必须成对出现</b>
	 * 
	 * @see #decShow()
	 */
	public static void incShow(Activity activity) {
		showCount++;
		if (activity == null || activity.isFinishing()) {
			return;
		}
		if (instance == null) {
			instance = new DefaultRequesterDialog(activity);
			instance.show();
		}
	}

	/**
	 * 递减一次计数，如果计数减到0，则关闭Loading动效
	 * 
	 * @see #incShow(Activity)
	 */
	public static void decShow() {
		showCount--;
		if (showCount <= 0) {
			showCount = 0;
			if (instance != null) {
				try {
					instance.dismiss();
				} catch (RuntimeException e) {
					// 某设备上出现的
				} finally {
					instance = null;
				}
			}
		}
	}

	private final LoadingRing loadingRing;

	private DefaultRequesterDialog(Context context) {
		this(context, R.style.AppDialogTheme);
	}

	private DefaultRequesterDialog(Context context, int theme) {
		super(context, theme);
		loadingRing = new LoadingRing(context);
		loadingRing.setDuration(50000); // 这里把时间值设大一点，因为可能有多次计数，时间是累积的
		setContentView(loadingRing);
		//
		this.setCancelable(false);
		this.setCanceledOnTouchOutside(false);
	}

	@Override
	protected void onStart() {
		super.onStart();
		loadingRing.start(new OnCompleteListener() {

			@Override
			public void onComplete() {
				dismiss();
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		loadingRing.requestStop();
		if (instance == this) {
			instance = null;
		}
	}

}
