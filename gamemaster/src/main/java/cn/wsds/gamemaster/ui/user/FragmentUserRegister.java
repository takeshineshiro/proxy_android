package cn.wsds.gamemaster.ui.user;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.net.HttpURLConnection;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.StatisticUtils;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.view.PhoneAndPassword;
import hr.client.appuser.Regist.RegisteAppUserResponse;

/**
 * 用户注册
 */
public class FragmentUserRegister extends FragmentUserAccount {
	
	private PhoneAndPassword phoneAndPassword;
	
	private static enum Status {
		/** 未发送过注册请求 */
		NO_REQUEST,
		/** 已发送过注册请求，但注册并未成功 */
		REQUEST_EXECUTED,
		/** 注册已成功 */
		REGISTER_SUCCEEDED,
	}
	private Status status = Status.NO_REQUEST;
	
	@Override
	public CharSequence getTitle() {
		return "新用户注册";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_user_register, container, false);
		phoneAndPassword = new PhoneAndPassword(root.findViewById(R.id.phone_and_password));
		//
		Button buttonConfirm = (Button) root.findViewById(R.id.button_confirm);
		buttonConfirm.setText(R.string.register_button_text);
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
				HttpApiService.requestRegister(
						phoneAndPassword.getPhoneNumber(),
						phoneAndPassword.getPassword(),
						phoneAndPassword.getVerifyCode(), 
						new RegisterCallBack(getActivity()));
				status = Status.REQUEST_EXECUTED;
			}
		});
		TextView textAwardDesc = (TextView) root.findViewById(R.id.text_award_desc);
		textAwardDesc.setText(R.string.register_award_desc);
		Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACCOUNT_LOGIN_IN);
		//
		return root;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (status == Status.REQUEST_EXECUTED) {
			StatisticUtils.statisticUserRegisterResult(getActivity(), false);
		}
	}

	private final class RegisterCallBack extends ResponseHandler {
		

		public RegisterCallBack(Activity activity) {
			super(activity);
		}
		
		private void onRegisterSuccess(RegisteAppUserResponse obj) {
			status = Status.REGISTER_SUCCEEDED;
			StatisticUtils.statisticUserRegisterResult(getActivity(), true);
			UIUtils.showToast("注册成功");
			UserSession.getInstance().updateUserInfoByServerProto(obj.getUserInfo());
			UserSession.getInstance().updateSessionInfoByServerProto(obj.getSessionInfo());
			Activity activity = getActivity();
			if(activity!=null){
				activity.finish();
			}
			Identify.defaultStartCheck();
		}

		private void onRegisterError() {
			UIUtils.showToast("注册失败");
		}
		
		@Override
		protected void onSuccess(Response response) {
			if(response== null){
				onRegisterError();
				return;
			}else if(HttpURLConnection.HTTP_BAD_REQUEST == response.code){
				UIUtils.showToast("验证码错误或超时");
				return;
			}else if(HttpURLConnection.HTTP_CONFLICT == response.code){
				UIUtils.showToast("手机号已被注册");
				return;
			}else if(HttpURLConnection.HTTP_CREATED != response.code){
				onRegisterError();
				return;
			}
			if (response.body != null) {
				try {
					RegisteAppUserResponse obj = RegisteAppUserResponse.parseFrom(response.body);
					int resultCode = obj.getResultCode();
					if (0 == resultCode) {
						onRegisterSuccess(obj);
						return;
					}
					if (131 == resultCode) {
						UIUtils.showToast("验证码错误");
						return;
					}
				} catch (InvalidProtocolBufferException e) {}
			}
			onRegisterError();
		}
		
	}
}
