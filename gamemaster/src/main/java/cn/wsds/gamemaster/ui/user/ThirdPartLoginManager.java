package cn.wsds.gamemaster.ui.user;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import cn.wsds.gamemaster.social.AppId;
import cn.wsds.gamemaster.social.SOCIAL_MEDIA;
import cn.wsds.gamemaster.social.UserSocialBean;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.wxapi.NotInstalledException;
import cn.wsds.gamemaster.wxapi.WeixinUtils;

import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuth;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.LogoutAPI;
import com.sina.weibo.sdk.openapi.UsersAPI;
import com.sina.weibo.sdk.openapi.models.User;
import com.subao.common.SuBaoObservable;
import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

public class ThirdPartLoginManager extends SuBaoObservable<ThirdPartLoginManager.Observer> {

	public static final ThirdPartLoginManager instance = new ThirdPartLoginManager();

	private ThirdPartLoginManager() {}

	/**
	 * 通知所有观察者：登录成功
	 */
	public void notifyLoginSucceed(UserSocialBean bean) {
		List<Observer> observers = this.cloneAllObservers();
		if (observers != null) {
			for (Observer o : observers) {
				o.onLoginSucceed(bean);
			}
		}
	}

	/**
	 * 通知所有观察者：登录失败
	 */
	public void notifyLoginFail() {
		List<Observer> observers = this.cloneAllObservers();
		if (observers != null) {
			for (Observer o : observers) {
				o.onLoginFail();
			}
		}
	}
	
	private static void fail() {
		ThirdPartLoginManager.instance.notifyLoginFail();
	}

	/**
	 * 用户信息及会话信息观察者
	 */
	public interface Observer {

		public void onLoginSucceed(UserSocialBean bean);

		public void onLoginFail();
	}

	/**
	 * 第三方身份验证
	 */
	public interface Authenticator {
		/** 调用者Activity必须在其onActivity事件里，调用本方法 */
		public void onActivityResult(int requestCode, int resultCode, Intent data);
	}

	/**
	 * 微信登录
	 * 
	 * @param context
	 * @return
	 * @throws NotInstalledException
	 */
	public boolean loginWeixin(Context context) throws NotInstalledException {
		IWXAPI wxApi = WeixinUtils.createWXApi(context);
		SendAuth.Req req = new SendAuth.Req();
		req.scope = "snsapi_userinfo";
		if (req.checkArgs()) {
			return wxApi.sendReq(req);
		} else {
			return false;
		}
	}

	/**
	 * 登出微信
	 * 
	 * @param context
	 */
	public void logoutWeixin(Context context) {
		try {
			// FIXME: 登出
			IWXAPI wxApi = WeixinUtils.createWXApi(context.getApplicationContext());
			wxApi.unregisterApp();
		} catch (NotInstalledException e) {
		}
	}

	/**
	 * 登录微博
	 * 
	 * @param activity
	 *            调用者的Activity.
	 *            <p>
	 *            <b>注意：该Activity必须在其onActivityResult事件里，调用
	 *            {@link #onActivityResult(Activity, int, int, Intent)}方法</b>
	 *            </p>
	 */
	public Authenticator loginSinaWeibo(Activity activity) {
		AuthenticatorSinaWeibo auth = new AuthenticatorSinaWeibo();
		auth.login(activity);
		return auth;
	}
	
	/**
	 * 登出新浪微博
	 */
	public void logoutSinaWeibo() {
		AuthenticatorSinaWeibo.logout();
	}

	/**
	 * QQ登录
	 * 
	 * @param activity
	 *            调用者的Activity.
	 * @return {@link Authenticator}，调用者Activity必须在其onActivityResult事件里，调用
	 *         {@link Authenticator#onActivityResult(int, int, Intent)}
	 *         方法</b></p>
	 */
	public Authenticator loginQQ(Activity activity) {
		AuthenticatorQQ auth = new AuthenticatorQQ();
		auth.login(activity);
		return auth;
	}

	/**
	 * 登出QQ
	 */
	public void logoutQQ(Context context) {
		AuthenticatorQQ.logout(context.getApplicationContext());
	}

	/**
	 * QQ登录
	 */
	private static class AuthenticatorQQ implements Authenticator {

		/** QQ回调的基类，统一处理onCancel和onError事件 */
		private static abstract class Listener implements IUiListener {
			@Override
			public void onCancel() {
				ThirdPartLoginManager.instance.notifyLoginFail();
			}

			@Override
			public void onError(UiError paramUiError) {
				ThirdPartLoginManager.instance.notifyLoginFail();
			}
		}

		/** 取QQ用户信息的回调 */
		private static class GetUserInfoListener extends Listener {

			private final String openId, accessToken;

			GetUserInfoListener(String openId, String accessToken) {
				this.openId = openId;
				this.accessToken = accessToken;
			}

			@Override
			public void onComplete(Object obj) {
				// 取QQ用户的一些信息，然后通知ThirdPartLoginManager
				try {
					if (!(obj instanceof JSONObject)) {
						throw new JSONException("Is not json");
					}
					JSONObject json = (JSONObject) obj;
					UserSocialBean userSocialBean = new UserSocialBean(SOCIAL_MEDIA.QQ, openId, json.getString("nickname"),
						accessToken, json.getString("figureurl_qq_2"));
					ThirdPartLoginManager.instance.notifyLoginSucceed(userSocialBean);
				} catch (JSONException e) {
					fail();
				}
			}

		}

		/** QQ登录的回调 */
		private static class LoginListener extends Listener {

			private final Context context;

			LoginListener(Context context) {
				this.context = context.getApplicationContext();
			}

			@Override
			public void onComplete(Object obj) {
				try {
					if (!(obj instanceof JSONObject)) {
						throw new RuntimeException();
					}
					JSONObject joUserInfo = (JSONObject) obj;
					String openId = joUserInfo.getString(Constants.PARAM_OPEN_ID);
					String accessToken = joUserInfo.getString(Constants.PARAM_ACCESS_TOKEN);
					String expiresIn = joUserInfo.getString(Constants.PARAM_EXPIRES_IN);
					Tencent tencent = createTencentObject(context);
					tencent.setOpenId(openId);
					tencent.setAccessToken(accessToken, expiresIn);
					UserInfo info = new UserInfo(context, tencent.getQQToken());
					info.getUserInfo(new GetUserInfoListener(openId, accessToken));
				} catch (JSONException e) {
					fail();
				} catch (RuntimeException e) {
					fail();
				}
			}
		};

		private Listener loginCallback;

		private static Tencent createTencentObject(Context context) {
			return Tencent.createInstance(AppId.QQ_APP_ID, context.getApplicationContext());
		}

		void login(Activity activity) {
			Context context = activity.getApplicationContext();
			Tencent tencent = createTencentObject(context);
			if (tencent != null) {
				if (tencent.isSessionValid()) {
					tencent.logout(context);
				}
				this.loginCallback = new LoginListener(context);
				tencent.login(activity, "", this.loginCallback);
			} else {
				fail();
			}
		}

		static void logout(Context context) {
			Tencent tencent = createTencentObject(context);
			if (tencent != null) {
				tencent.logout(context);
			}
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			if (this.loginCallback != null) {
				Tencent.onActivityResultData(requestCode, resultCode, data, this.loginCallback);
			}
		}
	}

	private static class AuthenticatorSinaWeibo implements Authenticator {

		private static final String REDIRECT_URL = "http://sns.whalecloud.com/sina2/callback";
		
		private static Oauth2AccessToken accessToken;

		private static class AuthListener implements WeiboAuthListener {

			@Override
			public void onComplete(Bundle values) {
				accessToken = Oauth2AccessToken.parseAccessToken(values);
				if (accessToken != null && accessToken.isSessionValid()) {
					try {
						long uid = Long.parseLong(accessToken.getUid());
						UsersAPI api = new UsersAPI(accessToken);
						new AsyncTask_RequestUserInfo(uid, accessToken.getToken()).executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor(), api);
						return;
					} catch (NumberFormatException e) {
						
					}
				}
				fail();
			}

			@Override
			public void onWeiboException(WeiboException e) {
				fail();
			}

			@Override
			public void onCancel() {
				fail();
			}

		};

		private static class AsyncTask_RequestUserInfo extends AsyncTask<UsersAPI, Void, User> {
			private final long uid;
			private final String token;
			
			public AsyncTask_RequestUserInfo(long uid, String token) {
				this.uid = uid;
				this.token = token;
			}

			@Override
			protected User doInBackground(UsersAPI... params) {
				String str = params[0].showSync(uid);
				if (TextUtils.isEmpty(str)) {
					return null;
				} else {
					return com.sina.weibo.sdk.openapi.models.User.parse(str);
				}
			}
			
			@Override
			protected void onPostExecute(com.sina.weibo.sdk.openapi.models.User user) {
				if (user == null) {
					fail();
				} else {
					UserSocialBean usb = new UserSocialBean(SOCIAL_MEDIA.WEIBO, user.idstr, user.screen_name, token, user.avatar_large);
					ThirdPartLoginManager.instance.notifyLoginSucceed(usb);
				}
			}

		}

		private SsoHandler mSsoHandler;

		void login(Activity activity) {
			logout();
			try {
				if (mSsoHandler == null) {
					WeiboAuth authInfo = new WeiboAuth(activity, AppId.SINA_APP_ID, REDIRECT_URL, "");
					mSsoHandler = new SsoHandler(activity, authInfo);
				}
				mSsoHandler.authorize(new AuthListener());
			} catch (RuntimeException e) {
				// 在Android 5.0及以上机型，当未安装新浪微博客户端时，可能会抛个
				// Service Intent must be explicit 异常（IllegalArgumentException）
				// （如果TargetVersion设到21的话）
				UIUtils.showToast("未检测到新浪微博客户端");
			}
		}
		
		static void logout() {
			if (accessToken != null) {
				LogoutAPI api = new LogoutAPI(accessToken);
				api.logout(new RequestListener() {
					
					@Override
					public void onWeiboException(WeiboException paramWeiboException) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onComplete(String paramString) {
						// TODO Auto-generated method stub
						
					}
				});
				accessToken = null;
			}
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			if (mSsoHandler != null) {
				mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
			}
		}
	}



}
