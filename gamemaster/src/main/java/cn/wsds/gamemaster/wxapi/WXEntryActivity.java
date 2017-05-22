package cn.wsds.gamemaster.wxapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;
import cn.wsds.gamemaster.share.ShareCallBackObservable;
import cn.wsds.gamemaster.share.ShareObserver;
import cn.wsds.gamemaster.share.WeixinShareManager;
import cn.wsds.gamemaster.social.AppId;
import cn.wsds.gamemaster.social.SOCIAL_MEDIA;
import cn.wsds.gamemaster.social.UserSocialBean;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.user.ThirdPartLoginManager;

import com.subao.common.Misc;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

	private void handleIntent(Intent intent) {
		IWXAPI wxApi;
		try {
			wxApi = WeixinUtils.createWXApi(this);
			wxApi.handleIntent(intent, this);
		} catch (NotInstalledException e) {}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		handleIntent(intent);
	}

	@Override
	public void onResp(BaseResp resp) {
		if (resp instanceof SendAuth.Resp) {
			if (BaseResp.ErrCode.ERR_OK == resp.errCode) {
				new UsersocialRequestor(((SendAuth.Resp) resp).code).executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
			} else {
				UIUtils.showToast("微信账号登录失败，错误代码 " + resp.errCode);
			}
		} else {
			onShareResp(resp);
		}
		this.finish();
	}

	@Override
	public void onReq(BaseReq req) {}

	private void onShareResp(BaseResp resp) {
		int result = getShareResult(resp);
		ShareType type = getShareType(resp);
		if (type != null) {
			ShareCallBackObservable.getInstance().callbackShareResult(type, result);
		}
	}

	private int getShareResult(BaseResp resp) {
		int result = ShareObserver.CALLBACK_CODE_UNKNOWN;
		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_OK:
			result = ShareObserver.CALLBACK_CODE_SUCCESS;
			break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
			result = ShareObserver.CALLBACK_CODE_CANCEL;
			break;
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
			result = ShareObserver.CALLBACK_CODE_DENY;
			break;
		default:
			result = ShareObserver.CALLBACK_CODE_UNKNOWN;
			break;
		}
		return result;
	}

	private static ShareType getShareType(BaseResp resp) {
		int shareType = 0;
		if (resp.transaction != null) {
			try {
				String str = resp.transaction.split("_")[0];
				shareType = Integer.parseInt(str);
			} catch (NumberFormatException e) {}
		}

		if (WeixinShareManager.WEIXIN_SHARE_TYPE_FRENDS == shareType) {
			return ShareType.ShareToFriends;
		}
		if (WeixinShareManager.WEIXIN_SHARE_TYPE_TALK == shareType) {
			return ShareType.ShareToWeixin;
		}
		return null;
	}

	private static class UsersocialRequestor extends AsyncTask<Void, Void, UserSocialBean> {

		private final String code;

		public UsersocialRequestor(String code) {
			this.code = code;
		}

		@Override
		protected UserSocialBean doInBackground(Void... params) {
			if (TextUtils.isEmpty(code)) {
				return null;
			}
			String getTokenSpec = String.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
				AppId.WEIXIN_APP_ID, AppId.WEIXIN_APP_SECRET, code);
			String jsonAccessToken = getWeixinMessage(getTokenSpec);
			if (jsonAccessToken == null) {
				return null;
			}
			try {
				JSONObject joAccessToken = new JSONObject(jsonAccessToken);
				String accessToken = joAccessToken.getString("access_token");
				String openid = joAccessToken.getString("openid");
				String getUserInfoToken = String.format("https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s", accessToken, openid);
				String jsonUserInfo = getWeixinMessage(getUserInfoToken);
				JSONObject joUserInfo = new JSONObject(jsonUserInfo);
				return new UserSocialBean(SOCIAL_MEDIA.WEIXIN,
					joUserInfo.getString("openid"),
					joUserInfo.getString("nickname"),
					accessToken,
					joUserInfo.getString("headimgurl"));
			} catch (Exception e) {
				// 注意，这里不要只Catch JSONException
				// 因为在某个设备(联想)上发现了会抛别的异常
				e.printStackTrace();
				return null;
			}

		}

		private String getWeixinMessage(String spec) {
			URL url;
			try {
				url = new URL(spec);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
			HttpURLConnection conn = null;
			try {
				conn = (HttpURLConnection) url.openConnection();
				InputStream in = null;
				try {
					in = conn.getInputStream();
					byte[] data = Misc.readStreamToByteArray(in);
					if (data != null) {
						return new String(data);
					}
				} catch (IOException e) {} finally {
					Misc.close(in);
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(UserSocialBean result) {
			//TODO 处理social
			if (result == null) {
				ThirdPartLoginManager.instance.notifyLoginFail();
			} else {
				ThirdPartLoginManager.instance.notifyLoginSucceed(result);
			}
		}
	}

}
