package cn.wsds.gamemaster.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;

import cn.wsds.gamemaster.pay.weixin.WXPay;

/**
 * Created by hujd on 16-8-3.
 */
public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(WXPay.getInstance() != null) {
			WXPay.getInstance().getWXApi().handleIntent(getIntent(), this);
		} else {
			finish();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		if(WXPay.getInstance() != null) {
			WXPay.getInstance().getWXApi().handleIntent(intent, this);
		}
	}

	@Override
	public void onReq(BaseReq baseReq) {

	}

	@Override
	public void onResp(BaseResp baseResp) {
		if(baseResp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX) {
			if(WXPay.getInstance() != null) {
				if(baseResp.errStr != null) {
					Log.e("wxpay", "errstr=" + baseResp.errStr);
				}

				Logger.d(LogTag.PAY, "onResp dopay errCode: " + baseResp.errCode);
				WXPay.getInstance().onResp(baseResp.errCode);
				finish();
			}
		}
	}
}
