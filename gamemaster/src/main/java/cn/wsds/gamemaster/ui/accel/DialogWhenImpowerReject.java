package cn.wsds.gamemaster.ui.accel;

import android.app.Activity;
import android.content.DialogInterface;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.CommonDesktopDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;

public class DialogWhenImpowerReject {

	private static DialogWhenImpowerReject instance;

	public static void showInstance(Activity activity, DialogInterface.OnClickListener dlgClickListener,
		final DialogInterface.OnCancelListener dlgCancelListener) {
		destroyInstance();
		instance = new DialogWhenImpowerReject(activity, dlgClickListener, dlgCancelListener);
	}

	public static void destroyInstance() {
		if (instance != null) {
			instance.destroy();
			instance = null;
		}
	}

	private static class MyEventObserver extends EventObserver {

		@Override
		public void onAccelSwitchChanged(boolean state) {
			if (state) {
				destroyInstance();
			}
		};
	};

	private final DialogInterface.OnCancelListener onDialogCancelListener;
	private final ClickListenerWrapper clickListenerWrapper;

	private CommonDialog dialog;
	private EventObserver eventObserver;

	private static class ClickListenerWrapper implements DialogInterface.OnClickListener {
		private DialogInterface.OnClickListener rawListener;
		private int clickedButton;

		public ClickListenerWrapper(DialogInterface.OnClickListener listener) {
			this.rawListener = listener;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			this.clickedButton = which;
			if (rawListener != null) {
				rawListener.onClick(dialog, which);
			}
		}

		public int getClickedButton() {
			return this.clickedButton;
		}
	}

	private DialogWhenImpowerReject(Activity activity, DialogInterface.OnClickListener dlgClickListener, DialogInterface.OnCancelListener dlgCancelListener) {
		this.dialog = (activity == null) ? new CommonDesktopDialog() : new CommonAlertDialog(activity);
		this.clickListenerWrapper = new ClickListenerWrapper(dlgClickListener);
		this.onDialogCancelListener = dlgCancelListener;
		//
		eventObserver = new MyEventObserver();
		TriggerManager.getInstance().addObserver(eventObserver);
		//
		dialog.setMessage("不予授权无法使用加速功能快人一步哟~");
		dialog.setPositiveButton("授权", clickListenerWrapper);
		dialog.setNegativeButton("取消", clickListenerWrapper);

		dialog.setCanceledOnTouchOutside(false);
		dialog.setOnCancelListener(dlgCancelListener);
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (clickListenerWrapper.getClickedButton() != DialogInterface.BUTTON_POSITIVE) {
					if (onDialogCancelListener != null) {
						onDialogCancelListener.onCancel(dialog);
					}
				}
				destroyInstance();
			}
		});
		dialog.show();
	}

	void destroy() {
		if (dialog != null) {
			try {
				dialog.dismiss();
			} catch (RuntimeException e) {}
			dialog = null;
		}
		if (eventObserver != null) {
			TriggerManager.getInstance().deleteObserver(eventObserver);
			eventObserver = null;
		}
	}
}
