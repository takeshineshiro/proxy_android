package cn.wsds.gamemaster.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.share.AchieveShareManager;
import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;
import cn.wsds.gamemaster.share.ShareObserver;
import cn.wsds.gamemaster.share.ui.ShareLayout;
import cn.wsds.gamemaster.share.ui.ShareLayout.DefaultShareObserver;
import cn.wsds.gamemaster.share.ui.ShareLayout.OnShareClickListener;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.Statistic.Event;
import cn.wsds.gamemaster.statistic.StatisticUtils;


/**
 * 主界面分享对话框
 */
public class AchieveShareDialog extends Dialog {
	
	private final Activity activity;
	
	public static void showDialog(Activity activity) {
		if (!activity.isFinishing()) {
			new AchieveShareDialog(activity).show();
		}
	}
	
	private AchieveShareDialog(Activity activity) {
		super(activity, R.style.AppDialogTheme);
		this.activity = activity;
		initDialog(activity);
	}

	private void initDialog(Activity activity) {
		setContentView(ShareLayout.createView(activity, onShareClickListener));
		setWindow(getWindow());
		setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				Statistic.addEvent(AppMain.getContext(), Event.SHARE_ACHIEVEMENT_CLICK,"取消分享");	
			}
		});
		setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
			}
		});
	}
	
	private void setWindow(Window dialogWindow) {
	    DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
		int width = displayMetrics.widthPixels > displayMetrics.heightPixels ? displayMetrics.heightPixels : displayMetrics.widthPixels;
		width = (int) (width * 0.9);
	    dialogWindow.setGravity(Gravity.CENTER);
	    dialogWindow.setLayout(width, LayoutParams.WRAP_CONTENT);
	}
	
	private final OnShareClickListener onShareClickListener = new OnShareClickListener() {
		
		@Override
		public void onClick(ShareType type) {
			//分享相关的点击事件
			AchieveShareManager.share(type, activity,new MyShareObserver());
			dismiss();
		}
	};
	
	private final class MyShareObserver extends DefaultShareObserver {


		@Override
		public void callbackResult(ShareType shareType, int resultCode) {
			super.callbackResult(shareType, resultCode);
			if(ShareObserver.CALLBACK_CODE_SUCCESS == resultCode){
				String param = "分享成功" + StatisticUtils.getShareTypeDesc(shareType);
				Statistic.addEvent(AppMain.getContext(), Event.SHARE_ACHIEVEMENT_CLICK,param);
			}else if(ShareObserver.CALLBACK_CODE_CANCEL == resultCode){
				Statistic.addEvent(AppMain.getContext(), Event.SHARE_ACHIEVEMENT_CLICK,"取消分享");
			}
		}

		@Override
		public Activity getActivity() {
			return activity;
		}
	}
}