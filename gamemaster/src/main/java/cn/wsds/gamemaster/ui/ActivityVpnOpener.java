package cn.wsds.gamemaster.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowVpnImpowerHelp;
import cn.wsds.gamemaster.ui.view.OpenFloatWindowHelpDialog;

/**
 * 如果需要开启VPN（弹出VPN授权框），并显示一个类Toast的提示条，则从此类继承
 */
public abstract class ActivityVpnOpener extends ActivityBase {

	private static boolean isOpenVpnRequest(int requestCode) {
		return GlobalDefines.START_ACTIVITY_REQUEST_CODE_VPN_IMPOWER == requestCode;
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		try {
			super.startActivityForResult(intent, requestCode);
			showHelpBarIfNeed(requestCode);
		} catch (RuntimeException e) {
			if (isOpenVpnRequest(requestCode)) {
				showToastWhenFailed();
			}else{
				processOtherException(e,intent);
			}
		}
	}

	private void showToastWhenFailed() {
		UIUtils.showToast("开启VPN失败");
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
		try {
			super.startActivityForResult(intent, requestCode, options);
			showHelpBarIfNeed(requestCode);
		} catch (RuntimeException e) {
			if (isOpenVpnRequest(requestCode)) {
				showToastWhenFailed();
			}else{
				processOtherException(e,intent);
			}
		}
	}

	private void showHelpBarIfNeed(int requestCode) {
		if (isOpenVpnRequest(requestCode)) {
			FloatWindowVpnImpowerHelp.createInstance(this);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (isOpenVpnRequest(requestCode)) {
			FloatWindowVpnImpowerHelp.destroyInstance();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		FloatWindowVpnImpowerHelp.destroyInstance();
	}
	
	private void processOtherException(RuntimeException e , Intent intent){
		if((e==null)||(intent==null)){
			return ;
		}
		
		if(e instanceof ActivityNotFoundException){
			boolean callFromFloatwindowDialog = intent.getBooleanExtra(IntentExtraName.CALL_FROM_FLOATWIDOW_DIALOG, false);
			if(callFromFloatwindowDialog){
				OpenFloatWindowHelpDialog.gotoAppSetting(this);
			}
		}
	}
}
