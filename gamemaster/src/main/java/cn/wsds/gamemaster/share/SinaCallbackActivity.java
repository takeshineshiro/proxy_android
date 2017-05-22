package cn.wsds.gamemaster.share;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import cn.wsds.gamemaster.social.AppId;

import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.constant.WBConstants;

public abstract class SinaCallbackActivity extends Activity implements IWeiboHandler.Response {

	private IWeiboShareAPI sinaAPI;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sinaAPI = WeiboShareSDK.createWeiboAPI(this, AppId.SINA_APP_ID);
		sinaAPI.handleWeiboResponse(getIntent(), this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (sinaAPI != null) {
			sinaAPI.handleWeiboResponse(intent, this);
		}
	}

	@Override
	public void onResponse(BaseResponse baseResp) {
		switch (baseResp.errCode) {
		case WBConstants.ErrorCode.ERR_OK:
			sinaResp(ShareObserver.CALLBACK_CODE_SUCCESS);
			break;
		case WBConstants.ErrorCode.ERR_CANCEL:
			sinaResp(ShareObserver.CALLBACK_CODE_CANCEL);
			break;
		case WBConstants.ErrorCode.ERR_FAIL:
			sinaResp(ShareObserver.CALLBACK_CODE_DENY);
			break;
		default:
			sinaResp(ShareObserver.CALLBACK_CODE_UNKNOWN);
			break;
		}
	}

	/**
	 * 新浪微博分享后的返回状态
	 * 
	 * @param respCode
	 *            状态有：CALLBACK_CODE_SUCCESS ...
	 */
	public abstract void sinaResp(int respCode);

}
