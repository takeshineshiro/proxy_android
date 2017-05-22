package cn.wsds.gamemaster.wxapi;

import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;
import cn.wsds.gamemaster.share.ShareCallBackObservable;
import cn.wsds.gamemaster.share.SinaCallbackActivity;

/**
 * 
 * @author Administrator
 * 新浪微博分享结果回馈
 */
public class SinaEntryActivity extends SinaCallbackActivity{

	@Override
	public void sinaResp(int respCode) {
		ShareCallBackObservable.getInstance().
				callbackShareResult(ShareType.ShareToSina, respCode);
		this.finish();
	}
}
