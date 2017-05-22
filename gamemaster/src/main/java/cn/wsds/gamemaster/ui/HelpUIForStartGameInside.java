package cn.wsds.gamemaster.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;

public class HelpUIForStartGameInside extends Dialog {

	private static HelpUIForStartGameInside instance;

	private final View helpContent;
	private final View maskTop;
	private final View maskBottom;

	private final Rect rectMaskTop;
	private final Rect rectMaskBottom;

	private final int xAnchor, yAnchor;

	private Runnable autoClose;

	/**
	 * 如果没有正在显示的UI实例，创建并显示一个新实例并返回
	 * 
	 * @param context
	 * @param rectTopMask
	 *            上部遮罩区
	 * @param rectBottomMask
	 *            下部遮罩区
	 * @param xAnchor
	 *            锚点X
	 * @param yAnchor
	 *            锚点Y
	 * @param delayAutoClose
	 *            如果大于0，表示延时给定毫秒数后自动关闭
	 * @return 如果新创建了一个UI，返回它的实例。如果已有存在的UI实例，返回null;
	 */
	public static HelpUIForStartGameInside showInstance(Activity activity, Rect rectTopMask, Rect rectBottomMask, int xAnchor, int yAnchor, long delayAutoClose) {
		if (instance != null) {
			return null;
		}
		if (activity == null || activity.isFinishing()) {
			return null;
		}
		instance = new HelpUIForStartGameInside(activity, rectTopMask, rectBottomMask, xAnchor, yAnchor, delayAutoClose);
		instance.setCanceledOnTouchOutside(true);
		instance.show();
		ConfigManager.getInstance().setMaskHelpUIStatus(ConfigManager.HELP_UI_STATUS_START_GAME_INSIDE);
		return instance;
	}
	
	public static boolean isInstanceExists() {
		return instance != null;
	}

	private HelpUIForStartGameInside(Context context, Rect rectTopMask, Rect rectBottomMask, int xAnchor, int yAnchor, long delayAutoClose) {
		super(context, R.style.MainHelpUIDialog);
		this.rectMaskTop = rectTopMask;
		this.rectMaskBottom = rectBottomMask;
		this.xAnchor = xAnchor;
		this.yAnchor = yAnchor;
		//
		this.setContentView(R.layout.hint_start_game_inside);
		this.helpContent = findViewById(R.id.help_content);
		this.maskTop = findViewById(R.id.mask_top);
		this.maskBottom = findViewById(R.id.mask_bottom);
		((View) this.helpContent.getParent()).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		//
		this.helpContent.post(new Runnable() {
			@Override
			public void run() {
				adjustPosition();
			}
		});
		//
		if (delayAutoClose > 0) {
			autoClose = new Runnable() {
				@Override
				public void run() {
					dismiss();
				}
			};

			this.helpContent.postDelayed(autoClose, delayAutoClose);
		}
	}

	private static void adjustMask(View mask, Rect rect) {
		int[] location = new int[2];
		mask.getLocationOnScreen(location);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, rect.height());
		lp.setMargins(rect.left - location[0], rect.top - location[1], 0, 0);
		mask.setLayoutParams(lp);
	}

	private void adjustPosition() {
		adjustMask(this.maskTop, this.rectMaskTop);
		adjustMask(this.maskBottom, this.rectMaskBottom);
		//
		int[] location = new int[2];
		helpContent.getLocationOnScreen(location);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		int x = xAnchor - helpContent.getMeasuredWidth() - location[0];
		int y = yAnchor - helpContent.getMeasuredHeight() - location[1];
		lp.setMargins(x, y, 0, 0);
		helpContent.setLayoutParams(lp);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (autoClose != null) {
			helpContent.removeCallbacks(autoClose);
			autoClose = null;
		}
		if (instance == this) {
			instance = null;
		}
	}

	public static void close() {
		if (instance != null) {
			instance.dismiss();
			instance = null;
		}
	}
}
