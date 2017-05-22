package cn.wsds.gamemaster.ui.user;

import hr.client.appuser.RetrievePasswd.RetrievePasswordResponse;

import java.net.HttpURLConnection;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.view.PhoneAndPassword;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 忘记密码
 */
public class FragmentUserResetPassword extends FragmentUserAccount {
	
	private PhoneAndPassword phoneAndPassword;
	
	@Override
	public CharSequence getTitle() {
		return "找回密码";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_user_reset_password, container, false);
		phoneAndPassword = new PhoneAndPassword(root.findViewById(R.id.phone_and_password));
		boolean hasUserId = hasUserId();
		if(hasUserId){
			phoneAndPassword.hidePhoneNumberEditText();
		}
		EditText editPassword = phoneAndPassword.getEditPassword();
		editPassword.setHint("请输入新的密码");
		//
		root.findViewById(R.id.button_reset_password).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean hasUserId = hasUserId();
				if(!hasUserId){
					if (!phoneAndPassword.checkPhoneNumber()) {
						return;
					}
				}
				if (!phoneAndPassword.checkPasswordLen()) {
					return;
				}
				if (!phoneAndPassword.checkVerifyCodeLen()) {
					return;
				}
				if(hasUserId){
					HttpApiService.requestResetPassword(
							null,
							phoneAndPassword.getPassword(),
							phoneAndPassword.getVerifyCode(),
							UserSession.getInstance().getSessionInfo().getUserId(), 
							new ResetPasswordCallback());
				}else{
					HttpApiService.requestResetPassword(
							phoneAndPassword.getPhoneNumber(),
							phoneAndPassword.getPassword(),
							phoneAndPassword.getVerifyCode(),
							null, 
							new ResetPasswordCallback());
				}
			}
		});
		//
		return root;
	}

	private boolean hasUserId() {
		SessionInfo sessioInfo = UserSession.getInstance().getSessionInfo();
		boolean hasUserId = sessioInfo !=null && sessioInfo.getUserId() != null;
		return hasUserId;
	}


	private final class ResetPasswordCallback extends ResponseHandler {
		
		public ResetPasswordCallback() {
			super(getActivity());
		}

		@Override
		protected void onSuccess(Response response) {
			if(HttpURLConnection.HTTP_BAD_REQUEST == response.code){
				UIUtils.showToast("验证码错误或超时");
			}else if(HttpURLConnection.HTTP_FORBIDDEN == response.code){
				UIUtils.showToast("用户不存在或帐号被冻结");
			}else if(HttpURLConnection.HTTP_ACCEPTED == response.code ){
				parseFromData(response.body);
			}else {
				UIUtils.showToast("密码重置失败");
			}
		}

		private void parseFromData(byte[] body) {
			if (body != null) {
				try {
					RetrievePasswordResponse obj = RetrievePasswordResponse.parseFrom(body);
					int resultCode = obj.getResultCode();
					if (resultCode == 0) {
						UIUtils.showToast("密码重置成功");
						UserSession.getInstance().updateUserInfoByServerProto(obj.getUserInfo());
						UserSession.getInstance().updateSessionInfoByServerProto(obj.getSessionInfo());
						Activity activity = getActivity();
						if (activity != null) {
							activity.finish();
						}
						return;
					}
				} catch (InvalidProtocolBufferException e) {}
			}
			UIUtils.showToast("密码重置失败");
		}
	}; 
}
