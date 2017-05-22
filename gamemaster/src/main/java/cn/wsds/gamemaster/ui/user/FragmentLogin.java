package cn.wsds.gamemaster.ui.user;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.protobuf.InvalidProtocolBufferException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.dialog.DefaultRequesterDialog;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.social.UserSocialBean;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.JPushUtils;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.BitmapUtil;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.user.ThirdPartLoginManager.Authenticator;
import cn.wsds.gamemaster.ui.user.ThirdPartLoginManager.Observer;
import cn.wsds.gamemaster.ui.view.PhoneAndPassword;
import cn.wsds.gamemaster.wxapi.NotInstalledException;
import hr.client.appuser.LoginUsePhoneNum;
import hr.client.appuser.ThirdPartLogin.ReportThirdPartAuthResultResponse;

public class FragmentLogin extends FragmentUserAccount {

	private PhoneAndPassword phoneAndPassword;
	private TextView textForgetPassword;
	
	private Authenticator authenticator;
	
	@Override
	public CharSequence getTitle() {
		return "登录";
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_user_login, container, false);
		phoneAndPassword = new PhoneAndPassword(root.findViewById(R.id.phone_and_password));
		phoneAndPassword.setPasswordVisible(false);
		phoneAndPassword.setPasswordEyeIconVisibility(View.GONE);
		phoneAndPassword.setVerifyCodeCtrlsVisibility(View.GONE);
		//
		textForgetPassword = (TextView) root.findViewById(R.id.text_forget_password);
		textForgetPassword.setOnClickListener(onClickListener);
		textForgetPassword.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		
		root.findViewById(R.id.textbutton_login).setOnClickListener(getLoginClickListener());
		//
		root.findViewById(R.id.textbutton_register).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				changeFragment(ActivityUserAccount.FRAGMENT_TYPE_REGUSTER,true);
				Statistic.addEvent(getActivity(), Statistic.Event.USER_REGISTER_CLICK);
			}
		});
		//
		root.findViewById(R.id.button_login_weibo).setOnClickListener(onClickListener);
		root.findViewById(R.id.button_login_weixin).setOnClickListener(onClickListener);
		root.findViewById(R.id.button_login_qq).setOnClickListener(onClickListener);
		//
		return root;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ThirdPartLoginManager.instance.registerObserver(thirdPartLoginObserver);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		ThirdPartLoginManager.instance.unregisterObserver(thirdPartLoginObserver);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (this.authenticator != null) {
			this.authenticator.onActivityResult(requestCode, resultCode, data);
		}
	}

	private OnClickListener getLoginClickListener() {
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!phoneAndPassword.checkPhoneNumber()) {
					return;
				}
				if (!phoneAndPassword.checkPasswordLen()) {
					return;
				}
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACCOUNT_LOGIN_CLICK);
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACCOUNT_LOGIN_WAY, "手机号");
				HttpApiService.requestLogin(phoneAndPassword.getPhoneNumber(), phoneAndPassword.getPassword(),new LoginCallBack(getActivity()));

			}
		};
	}
	
	private final Observer thirdPartLoginObserver = new ThirdPartLoginManager.Observer() {
		
		@Override
		public void onLoginSucceed(UserSocialBean bean) {
			ReportThirdPartAuthCallBack callback = new ReportThirdPartAuthCallBack(getActivity(),bean);
			HttpApiService.requestReportThirdPartAuthResult(bean, callback);
		}

		@Override
		public void onLoginFail() {
			UIUtils.showToast("登录失败");
		}
	};
	
	
    private final OnClickListener onClickListener = new OnClickListener() {

        @Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_login_weibo:
				authenticator = ThirdPartLoginManager.instance.loginSinaWeibo(getActivity());
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACCOUNT_LOGIN_WAY, "微博");
				break;
			case R.id.button_login_weixin:
				try {
					ThirdPartLoginManager.instance.loginWeixin(getActivity());
				} catch (NotInstalledException e) {
					UIUtils.showToast(e.getMessage());
				}
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACCOUNT_LOGIN_WAY, "微信");
				break;
			case R.id.button_login_qq:
				authenticator = ThirdPartLoginManager.instance.loginQQ(getActivity());
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACCOUNT_LOGIN_WAY, "QQ");
				break;
			case R.id.text_forget_password:
				changeFragment(ActivityUserAccount.FRAGMENT_TYPE_RESET_PASSWORD, true);
				break;
			default:
				break;
			}
		}
	};

    private final class LoginCallBack extends ResponseHandler {

		public LoginCallBack(Activity activity) {
			super(activity);
		}

		@Override
		protected void onSuccess(Response response) {
			if(response== null){
				UIUtils.showToast("登录失败");
			}else if(HttpURLConnection.HTTP_UNAUTHORIZED == response.code){
				UIUtils.showToast("账号或密码错误");
			}else if(HttpURLConnection.HTTP_FORBIDDEN == response.code){
				UIUtils.showToast("密码多次输入错误，账号已被冻结半小时");
			}else if(HttpURLConnection.HTTP_ACCEPTED == response.code || 0 == response.code){
				parse(response);				
			}else{
				UIUtils.showToast("登录失败");
			}
		}

		private void parse(Response response) {
			if (response.body != null) {
				try {
					LoginUsePhoneNum.AppUserLoginResponse userLoginResponse = LoginUsePhoneNum.AppUserLoginResponse.parseFrom(response.body);
					if (0 == userLoginResponse.getResultCode()) {
						UserSession.getInstance().updateUserInfoByServerProto(userLoginResponse.getUserInfo());
						UserSession.getInstance().updateSessionInfoByServerProto(userLoginResponse.getSessionInfo());
						UIUtils.showToast("欢迎回来o(^▽^)o");
						Activity ref = getActivity();
						
						//JPush setTag为覆盖型 ，所以login时重新设置Tag
						JPushUtils.setTagsForJPush(ref);
						if (ref != null) {
							ref.finish();
						}

						// FIXME: 17-2-22
						Identify.defaultStartCheck();
						return;
					}
					UIUtils.showToast("账号或密码错误");
				} catch (InvalidProtocolBufferException e) {}
			}
			UIUtils.showToast("登录失败");
		}
    }
    
    private static final class ReportThirdPartAuthCallBack extends ResponseHandler {
    	
		private WeakReference<Activity> activity;
		private final UserSocialBean bean;
		private ReportThirdPartAuthResultResponse reportThirdPartAuthResultResponse;

		public ReportThirdPartAuthCallBack(Activity context,UserSocialBean bean) {
			super(context);
			this.bean = bean;
			this.activity = new WeakReference<Activity>(context);
		}
		
		@Override
		protected void onSuccess(Response response) {
			if (HttpURLConnection.HTTP_FORBIDDEN == response.code) {
//				super.onFinish();
				UIUtils.showToast("密码多次输入错误，账号已被冻结半小时");
				return;
			}
			if (HttpURLConnection.HTTP_ACCEPTED != response.code) {
				onLoginFail();
				return;
			}

			if (response.body != null) {
				try {
					ReportThirdPartAuthResultResponse obj = ReportThirdPartAuthResultResponse.parseFrom(response.body);
					int resultCode = obj.getResultCode();
					if (0 == resultCode) {
						// 取用户头像
						new GetAvatar(this).executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor(), bean.avatarUrl);
						reportThirdPartAuthResultResponse = obj;
						// FIXME: 17-2-22 发起鉴权
						Identify.defaultStartCheck();
						return;
					}
				} catch (InvalidProtocolBufferException e) {}
			}
			onLoginFail();
		}

		private void onLoginFail() {
			UIUtils.showToast("登录失败");
		}
    	
		void onGetAvatarSucceed(Bitmap drawableAvatar) {
			UIUtils.showToast("欢迎回来o(^▽^)o");
			Activity ref = this.activity.get();			
			UserSession.getInstance().updateUserInfoByServerProto(
				reportThirdPartAuthResultResponse.getUserInfo(), 
				bean.name, bean.socailMedia, drawableAvatar);
			UserSession.getInstance().updateSessionInfoByServerProto(reportThirdPartAuthResultResponse.getSessionInfo());
			JPushUtils.setTagsForJPush(ref);
			if (ref != null) {
				ref.finish();
			}
		}
		
    }
    
    /**
     * 取用户头像
     */
    private static final class GetAvatar extends AsyncTask<String, Void,Bitmap> {

    	private final ReportThirdPartAuthCallBack reportThirdPartAuthCallBack;
    	
		public GetAvatar(ReportThirdPartAuthCallBack reportThirdPartAuthCallBack) {
			DefaultRequesterDialog.incShow(reportThirdPartAuthCallBack.activity.get());	// 准备开始，需要Loadin动效
			this.reportThirdPartAuthCallBack = reportThirdPartAuthCallBack;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			if (params == null) {
				return null;
			}
			String spec = params[0];
			if(TextUtils.isEmpty(spec)){
				return null;
			}
			InputStream inputStream = null;
			try {
				URL url = new URL(spec);
				HttpURLConnection urlConn = (HttpURLConnection) url
						.openConnection();
				inputStream = urlConn.getInputStream();
//				int available = inputStream.available();
//				byte[] buffer = new byte[available];
//				inputStream.read(buffer, 0, available);
				Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
				return bitmap;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			Bitmap drawableAvatar = null;
			if(bitmap!=null){
				drawableAvatar = BitmapUtil.toRoundBitmap(bitmap);
			}
			if(reportThirdPartAuthCallBack!=null){
				reportThirdPartAuthCallBack.onGetAvatarSucceed(drawableAvatar);
			}
			DefaultRequesterDialog.decShow();	// 开始的时候显示了Loading动效，这里该关闭它了
		}
	}

}
