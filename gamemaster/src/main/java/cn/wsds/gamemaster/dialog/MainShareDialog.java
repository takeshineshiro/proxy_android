package cn.wsds.gamemaster.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.share.GameMasterShareManager;
import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;
import cn.wsds.gamemaster.share.ShareCallBackObservable;
import cn.wsds.gamemaster.share.ShareObserver;
import cn.wsds.gamemaster.share.ui.ShareLayout;
import cn.wsds.gamemaster.share.ui.ShareLayout.DefaultShareObserver;
import cn.wsds.gamemaster.share.ui.ShareLayout.OnShareClickListener;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.StatisticUtils;

/**
 * 主界面分享对话框
 */
public class MainShareDialog extends Dialog {

    private final Activity activity;
    
	public MainShareDialog(Activity activity) {
		super(activity, R.style.AppDialogTheme);
        this.activity = activity;
		initDialog();
	}

	private void initDialog() {
		View view = ShareLayout.createView(activity, new OnShareClickListener() {

            @Override
            public void onClick(ShareType type) {
                ShareCallBackObservable.getInstance().registShareObserver(new MyShareObserver());
                shareMenuClickAction(type, activity);
            }

        });
		setContentView(view);
		setWindow(getWindow());
	}
	
	private void setWindow(Window dialogWindow) {
	    DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
		int width = displayMetrics.widthPixels > displayMetrics.heightPixels ? displayMetrics.heightPixels : displayMetrics.widthPixels;
		width = (int) (width * 0.9);
	    dialogWindow.setGravity(Gravity.CENTER);
	    dialogWindow.setLayout(width, LayoutParams.WRAP_CONTENT);
	}
	
	private final class MyShareObserver extends DefaultShareObserver {

		@Override
		public void callbackResult(ShareType shareType, int resultCode) {
			super.callbackResult(shareType, resultCode);
			if(ShareObserver.CALLBACK_CODE_SUCCESS == resultCode){
				StatisticUtils.statisticShareComplete(activity, Statistic.Event.SHARE_HOMEPAGE_CLICK_SUCCESS,shareType);
			}
		}

		@Override
		public Activity getActivity() {
			return activity;
		}
	}

	/**
	 * 分享菜单事件处理
	 * @param position
	 */
	private void shareMenuClickAction(GameMasterShareManager.ShareType shareType,Activity activity) {
		this.dismiss();
		GameMasterShareManager gmsManager = GameMasterShareManager.getInstance();
		gmsManager.mainPageShare(shareType, activity);
//		if (ShareType.ShareToFriends != shareType) {
//			gmsManager.mainPageShare(shareType, activity);
//			return;
//		}
//		if (Misc.isAppInstalled(activity, GameMasterShareManager.WEIXIN_PACKAGE_NAME)) {
//			gmsManager.mainPageShare(shareType, activity);
//		} else {
//			UIUtils.showToast("请先安装微信客户端");
//		}
	}

}
