package cn.wsds.gamemaster.wxapi;

import android.content.Context;
import cn.wsds.gamemaster.social.AppId;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class WeixinUtils {

	// 测试用
	private static final String APP_ID_DEBUG = "wxed02f35724127c03";
	//	private static final String APP_SECRET_DEBUG = "9c0c1e64a31166533585f1bffca23c6f";

	private static final boolean CHECK_SIGNATURE = true;
	private static final boolean DEBUG = false;

	public static IWXAPI createWXApi(Context context) throws NotInstalledException{
		String appId;
		if (DEBUG) {
			appId = APP_ID_DEBUG;
		} else {
			appId = AppId.WEIXIN_APP_ID;
		}

		IWXAPI api = WXAPIFactory.createWXAPI(context, appId, CHECK_SIGNATURE);
		if (api.isWXAppInstalled()) {
			api.registerApp(appId);
			return api;
		}

		throw new NotInstalledException.Weixin();
	}

}
