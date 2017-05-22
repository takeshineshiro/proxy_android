package cn.wsds.gamemaster.ui.user;

import hr.client.appuser.RetrievePasswd.RetrievePasswordResponse;

import java.net.HttpURLConnection;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.net.HttpURLConnection;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.data.UserSession.LogoutReason;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.view.PhoneAndPassword.PasswordView;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 修改密码
 */
public class FragmentUserUpdatePassword extends FragmentUserAccount {
	
	private PasswordView oldPasswordView;
	private PasswordView newPasswordView;
	
	@Override
	public CharSequence getTitle() {
		return "修改密码";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_user_update_password, container, false);
		EditText oldEditPassword = (EditText) root.findViewById(R.id.edit_old_password);
		ImageView oldImgEye = (ImageView) root.findViewById(R.id.img_old_eye);
		oldPasswordView = new PasswordView(oldEditPassword , oldImgEye);
		EditText newEditPassword = (EditText) root.findViewById(R.id.edit_new_password);
		ImageView newImgEye = (ImageView) root.findViewById(R.id.img_new_eye);
		newPasswordView = new PasswordView(newEditPassword , newImgEye);
		//
		root.findViewById(R.id.button_update_password).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!oldPasswordView.checkPasswordLen()){
					return;
				}
				
				if(!newPasswordView.checkPasswordLen()){
					return;
				}
				SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
				Activity activity = getActivity();
				if(sessionInfo==null &&  activity!=null){
					// sessionInfo 为空，令牌失效 提醒用户重新登录
					UIUtils.showReloginDialog(activity);
					return;
				}
				String userId = sessionInfo.getUserId();
				String oldPassword = oldPasswordView.getPassword();
				String newPassWord = newPasswordView.getPassword();
				HttpApiService.requestUpdatePassword(oldPassword,newPassWord,userId,new UpdatePasswordCallback(activity));
			}
		});
		//
		return root;
	}


	private final class UpdatePasswordCallback extends ResponseHandler {
		
		public UpdatePasswordCallback(Activity activity) {
			super(activity, new ReLoginOnHttpUnauthorizedCallBack(activity));
		}

		@Override
		protected void onSuccess(Response response) {
			Activity activity = getActivity();
			if(HttpURLConnection.HTTP_BAD_REQUEST == response.code){
				UIUtils.showToast("密码填写错误");
				return;
			}else if(HttpURLConnection.HTTP_OK != response.code){
				onUpdateFail();
				return;
			}
			if (response.body != null) {
				try {
					RetrievePasswordResponse obj = RetrievePasswordResponse.parseFrom(response.body);
					if (obj.getResultCode() == 0) {
						UIUtils.showToast("修改成功，请重新登录");
						UserSession.logout(LogoutReason.OTHER_REASON);
						if (activity != null) {
							activity.setResult(Activity.RESULT_OK);
							activity.finish();
							ActivityUserAccount.open(null, ActivityUserAccount.FRAGMENT_TYPE_LOGIN);
						}
						return;
					}
				} catch (InvalidProtocolBufferException e) {}
			}
			onUpdateFail();
		}

		private void onUpdateFail() {
			UIUtils.showToast("密码修改失败");
		}
	}; 
}
