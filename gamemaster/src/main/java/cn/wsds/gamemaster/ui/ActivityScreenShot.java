package cn.wsds.gamemaster.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.screenshot.ScreenshotManager;
import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;
import cn.wsds.gamemaster.share.QQShareManager;
import cn.wsds.gamemaster.share.WeixinShareManager;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowInGame;
import cn.wsds.gamemaster.wxapi.NotInstalledException;
import cn.wsds.gamemaster.wxapi.WeixinUtils;

import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

/**
 * 截屏管理页面
 */
public class ActivityScreenShot extends ActivityShare {

	private ViewFlipper viewFlipper;
	/** 复制到图库的路径 */
	private String filePathName = null;
	private View buttonShare;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
	}

	private void initView() {
		setContentView(R.layout.activity_screenshot);
		initViewFilpper();
//		initShareLayout();
		findViewById(R.id.button_finished)
				.setOnClickListener(viewOnClickListener);
		buttonShare = findViewById(R.id.button_share);
		buttonShare.setOnClickListener(viewOnClickListener);
		findViewById(R.id.share_qq).setOnClickListener(shareClick);
		findViewById(R.id.share_penyouquan).setOnClickListener(shareClick);
		findViewById(R.id.share_weixin).setOnClickListener(shareClick);
		findViewById(R.id.share_qqzone).setOnClickListener(shareClick);
//		findViewById(R.id.share_sina).setOnClickListener(shareClick);
	}

	private void initViewFilpper() {
		Context context = getApplicationContext();
		viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(context,
				R.anim.slide_in_bottom));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(context,
				R.anim.slide_out_top));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ScreenshotManager.changeFinishState();
		FloatWindowInGame.setInstanceVisibility(View.VISIBLE);
	}

	private final View.OnClickListener viewOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
//			case R.id.button_cancel:
//				finish();
//				FileUtils.deleteFileOrDirectory(FileUtils
//						.getDataFile(ScreenshotManager.SCREENSHOT_TMP_FILE));
//				StatisticDefault.addEvent(getApplicationContext(),
//						StatisticDefault.Event.CLICK_SCREENSHOT_CANCEL);
//				break;
			case R.id.button_share:
//				initShareLayout();
				buttonShare.setEnabled(false);
				filePathName = ScreenshotManager.saveToMediaStore();
				viewFlipper.showNext();
//				StatisticDefault.addEvent(getApplicationContext(),
//						StatisticDefault.Event.CLICK_SCREENSHOT_SEND);
				break;
			case R.id.button_finished:
				onFinished();
				break;
			}
		}

	};
	private void onFinished() {
		if(filePathName==null){
			filePathName = ScreenshotManager.saveToMediaStore();
		}
		boolean result = !TextUtils.isEmpty(filePathName);
		ScreenshotManager.onFinished(result);
		finish();
	}

//	public void initShareLayout() {
//		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//			viewFlipper.removeView(findViewById(R.id.share_group_portait));
//		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//			viewFlipper.removeView(findViewById(R.id.share_group_landscape));
//		}
//	}

	private final View.OnClickListener shareClick = new View.OnClickListener() {
		private final IUiListener iUiListener = new IUiListener() {

			@Override
			public void onError(UiError arg0) {
				onShareEnd(false);
			}

			@Override
			public void onComplete(Object arg0) {
//				if(UserSession.isLogined()) {
//	                UserSign.getInstance().userShareScore(null);
//	            } else {
//	                UIUtils.showToast(R.string.thank_your_share);
//	            }
				onShareEnd(true);
			}

			@Override
			public void onCancel() { }
		};

		@Override
		public void onClick(View v) {
			String path = filePathName;
			v.setEnabled(false);
			switch (v.getId()) {
			case R.id.share_qq:
				shareQQ(v, ShareType.ShareToQQ, path);
				break;
			case R.id.share_qqzone:
				shareQQ(v, ShareType.ShareToZone, path);
				break;
			case R.id.share_penyouquan:
				shareWeixin(path, WeixinShareManager.WEIXIN_SHARE_TYPE_FRENDS,
						v);
				break;
			case R.id.share_weixin:
				shareWeixin(path, WeixinShareManager.WEIXIN_SHARE_TYPE_TALK, v);
				break;
//			case R.id.share_sina:
//				boolean result = SinaShareManager.shareLocalPic(
//						ActivityScreenShot.this, path);
//				onShareEnd(result);
//				v.setEnabled(true);
//				break;
			}
		}

		private void shareQQ(View v, ShareType type, String path) {
			Tencent mTencent = QQShareManager
					.createTencent(ActivityScreenShot.this);
			if (type == ShareType.ShareToQQ) {
				QQShareManager.shareLocalImgToQQ(ActivityScreenShot.this,
						mTencent, path, iUiListener);
			} else {
				QQShareManager.shareLocalImgToQzone(ActivityScreenShot.this,
						mTencent, path, iUiListener);
			}
			v.setEnabled(true);
		}

		private void shareWeixin(String path, int shareScene, View v) {
			boolean ok;
			try {
//                ShareCallBackObservable.getInstance().registShareObserver(new MyShareObserver());
				ok = WeixinShareManager.shareLocalPicture(
					WeixinUtils.createWXApi(ActivityScreenShot.this),
					ActivityScreenShot.this, shareScene, path);
			} catch (NotInstalledException e) {
				ok = false;
			}
			if (!ok) {
				v.setEnabled(true);
			}
			onShareEnd(ok);
		}

		private void onShareEnd(boolean result) {
			if (result) {
			    finish();
			}else{    
				UIUtils.showToast("分享失败");
			}
		}
	};
	

//    private static final class MyShareObserver extends FragmentShareMenu.DefaultShareObserver {
//        @Override
//        public void callbackResult(ShareType shareType, int resultCode) {
//        	ShareCallBackObservable.getInstance().removeShareObserver(this);
//            if(ShareObserver.CALLBACK_CODE_SUCCESS == resultCode && UserSession.isLogined()) {
//                UserSign.getInstance().userShareScore(null);
//            } else {
//                UIUtils.showToast(R.string.thank_your_share);
//            }
//        }
//    }
//	/** 保存到本地图库 */
//	private boolean saveToMediaStore() {
//		if (filePathName != null) {
//			return true;
//		}
//		try {
//			File fileSource = FileUtils
//					.getDataFile(ScreenshotManager.SCREENSHOT_TMP_FILE);
//			Calendar calendar = Calendar.getInstance();
//			String name = String.format(
//					"迅游手游截图_%d-%02d-%02d-%02d-%02d-%02d.jpg",
//					calendar.get(Calendar.YEAR),
//					calendar.get(Calendar.MONTH) + 1,
//					calendar.get(Calendar.DAY_OF_MONTH),
//					calendar.get(Calendar.HOUR_OF_DAY),
//					calendar.get(Calendar.MINUTE),
//					calendar.get(Calendar.SECOND));
//			// 目标文件
//			File path = Environment
//					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//			path = new File(path, "Screenshots");
//			path.mkdirs();
//			File fileDest = new File(path, name);
//			if (!FileUtils.copyFile(fileSource, fileDest, false)) {
//				filePathName = null;
//				return false;
//			}
//			filePathName = fileDest.getPath();
//			// 通知Media Scanner
//			MediaScannerConnection.scanFile(AppMain.getContext(),
//					new String[] { fileDest.getAbsolutePath() }, null, null);
//			return true;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return false;
//	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(filePathName==null){
			onFinished();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(R.id.share_group_landscape == viewFlipper.getCurrentView().getId()){
				viewFlipper.showPrevious();
				buttonShare.setEnabled(true);	
				return true;
			}else{
				onFinished();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}
