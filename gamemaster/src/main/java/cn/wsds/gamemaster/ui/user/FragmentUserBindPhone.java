package cn.wsds.gamemaster.ui.user;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.statistic.Statistic;
import hr.client.appuser.ModifyUser.ModifyUserInfoResponse;

import java.net.HttpURLConnection;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.view.PhoneAndPassword;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 绑定手机号
 */
public class FragmentUserBindPhone extends FragmentUserAccount {
	
	private PhoneAndPassword phoneAndPassword;
	
	@Override
	public CharSequence getTitle() {
		return "绑定手机";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_user_register, container, false);
		phoneAndPassword = new PhoneAndPassword(root.findViewById(R.id.phone_and_password));
		//
		Button buttonConfirm = (Button) root.findViewById(R.id.button_confirm);
		buttonConfirm.setText(R.string.bind_phone_button_text);
		buttonConfirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!phoneAndPassword.checkPhoneNumber()) {
					return;
				}
				if (!phoneAndPassword.checkPasswordLen()) {
					return;
				}
				if (!phoneAndPassword.checkVerifyCodeLen()) {
					return;
				}
				
				Activity activity = getActivity();
				if(activity==null){
					return;
				}
				if(activity.isFinishing()){
					return;
				}
				SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
				if(sessionInfo==null){
					// sessionInfo 为空，令牌失效 提醒用户重新登录
					UIUtils.showReloginDialog(activity);
					return;
				}
				HttpApiService.requestBindPhone(
						phoneAndPassword.getPhoneNumber(),
						phoneAndPassword.getPassword(),
						phoneAndPassword.getVerifyCode(), 
						sessionInfo.getSessionId(),
						sessionInfo.getUserId(),
						new BindPhoneRequestorCallback(activity));
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACCOUNT_THIRDPART_PHONE_BIND);
			}
		});
		TextView textAwardDesc = (TextView) root.findViewById(R.id.text_award_desc);
		textAwardDesc.setText(R.string.bind_phone_award_desc);
		//
		return root;
	}


	private final class BindPhoneRequestorCallback extends ResponseHandler {
		
		public BindPhoneRequestorCallback(Activity activity) {
			super(activity, new ReLoginOnHttpUnauthorizedCallBack(activity));
		}

		private void onRequestSucceed(Response response) {
			if (response.body != null) {
				try {
					ModifyUserInfoResponse obj = ModifyUserInfoResponse.parseFrom(response.body);
					int resultCode = obj.getResultCode();
					if (resultCode == 0) {
						onBindPhoneSucceed(obj.getGotPoints());
						//FIXME 
						UserInfo userInfo = UserSession.getInstance().getUserInfo();
						if (userInfo == null) {
							UserSession.getInstance().updateUserInfoByServerProto(obj.getUserInfo());
						} else {
							UserSession.getInstance().updateUserInfoByServerProto(obj.getUserInfo(), userInfo.getThirdPartNickName(),
								userInfo.getSocial_MEDIA(), userInfo.getDrawableAvatar());
						}
						return;
					}
				} catch (InvalidProtocolBufferException e) {}
			}
			UIUtils.showToast("绑定失败");
		}

		private void onBindPhoneSucceed(int points) {
			UIUtils.showToast(String.format("绑定成功，积分+%d", points));
			Activity activity = getActivity();
			if(activity!=null){
				activity.finish();
			}
		}

		@Override
		protected void onSuccess(Response response) {
			if(HttpURLConnection.HTTP_OK == response.code) {
				onRequestSucceed(response);
			}else if(HttpURLConnection.HTTP_CONFLICT == response.code) {
				UIUtils.showToast("该账号已绑定或手机号已注册");
			}else if (HttpURLConnection.HTTP_BAD_REQUEST == response.code) {
				UIUtils.showToast("验证码错误或超时");
			} else {
				UIUtils.showToast("绑定失败");
			}
		}
	}
}
