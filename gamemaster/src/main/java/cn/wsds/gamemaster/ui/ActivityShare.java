package cn.wsds.gamemaster.ui;

import android.content.Intent;

import com.tencent.tauth.Tencent;

public abstract class ActivityShare extends ActivityBase {

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Tencent.onActivityResultData(requestCode, resultCode, data, null);
	}
}
