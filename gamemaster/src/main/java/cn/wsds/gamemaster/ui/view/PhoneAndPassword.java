package cn.wsds.gamemaster.ui.view;

import hr.client.appuser.VerificationCode.GetVerificationCodeResponse;

import java.net.HttpURLConnection;

import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.http.DefaultNoUIResponseHandler;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.UIUtils;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 对控件组的简单封闭。<br />
 * 该控件组含：手机号码输入框、密码框、验证码框及按钮等
 * 
 */
public class PhoneAndPassword {

	private static final String STR_INVALID_PHONE_NUMBER_EMPTY = "请先填写手机号";
	private static final String STR_INVALID_PHONE_NUMBER = "手机号格式有误";
	private static final String STR_INVALID_PASSWORD_LEN = "密码最短6位，最长32位";
	private static final String STR_INVALID_VERIFY_CODE_LEN = "验证码错误";
	protected static final int REGET_VERIFYCODE_TIMEOUT = 60;

	private final EditText editPhoneNumber, editVerifyCode;
	private final View groupVerifyCode;
	private final PasswordView passwordView;

	private final ReGetVerifyCodeCountDown regetVerifyCodeCountDown;

	/**
	 * 令“获取验证码”按钮显示倒计时文字
	 */
	private static class ReGetVerifyCodeCountDown implements Runnable {
		private int timeout;
		private final TextView buttonGetVerifyCode;
		private final View groupGetVerifyCodeTimeout;
		private final TextView textTimeout;
		private boolean running;

		public ReGetVerifyCodeCountDown(TextView buttonGetVerifyCode, View groupGetVerifyCodeTimeout, TextView textTimeout) {
			this.buttonGetVerifyCode = buttonGetVerifyCode;
			this.groupGetVerifyCodeTimeout = groupGetVerifyCodeTimeout;
			this.textTimeout = textTimeout;
		}

		public void start(int timeout) {
			this.timeout = timeout;
			if (!running) {
				buttonGetVerifyCode.setEnabled(false);
				buttonGetVerifyCode.setText(null);
				groupGetVerifyCodeTimeout.setVisibility(View.VISIBLE);
				buttonGetVerifyCode.post(this);
				running = true;
			}
		}

		private void stop() {
			if (running) {
				buttonGetVerifyCode.removeCallbacks(this);
				buttonGetVerifyCode.setText("获取验证码");
				buttonGetVerifyCode.setEnabled(true);
				groupGetVerifyCodeTimeout.setVisibility(View.GONE);
				running = false;
			}
		}

		@Override
		public void run() {
			String strTimeout = String.format("(%ds)", timeout);
			textTimeout.setText(strTimeout);
			--timeout;
			if (timeout > 0) {
				buttonGetVerifyCode.postDelayed(this, 1000);
			} else {
				stop();
			}
		}
	};
	
	public PhoneAndPassword(final View root) {
		this.editPhoneNumber = (EditText) root.findViewById(R.id.edit_phone_number);
		EditText editPassword = (EditText) root.findViewById(R.id.edit_password);
		ImageView imgEye = (ImageView) root.findViewById(R.id.img_eye);
		this.passwordView = new PasswordView(editPassword,imgEye);
		//
		this.groupVerifyCode = root.findViewById(R.id.group_verifycode);
		this.editVerifyCode = (EditText) root.findViewById(R.id.edit_verify_code);
		//
		TextView buttonGetVerifyCode = (TextView) root.findViewById(R.id.textbutton_get_verifycode);
		
		buttonGetVerifyCode.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!checkPhoneNumber()) {
					return;
				}
				String phoneNumber = getPhoneNumber();
				UserInfo userInfo = UserSession.getInstance().getUserInfo();
				String userId = TextUtils.isEmpty(phoneNumber) ? (userInfo == null ? null : userInfo.getUserId()) : null;
				boolean requestVerificationCode = HttpApiService.requestVerificationCode(
					phoneNumber,
					userId,new DefaultNoUIResponseHandler() {
						
						@Override
						protected CharSequence getToastText_RequestFail() {
							return DEFAULT_MSG_NET_PROBLEM;
						}
						
						@Override
						public void onNetworkUnavailable() {
							UIUtils.showToast(DEFAULT_MSG_NET_PROBLEM);
						}
						
						@Override
						protected void onSuccess(Response response) {
							if(HttpURLConnection.HTTP_FORBIDDEN == response.code){
								parseFrom(response.body);
								setRequestVerifyCodeLimit(0);
							}else if(HttpURLConnection.HTTP_OK == response.code
									|| HttpURLConnection.HTTP_ACCEPTED == response.code){
								UIUtils.showToast("验证码已发送，请注意查收");
							}else{
								UIUtils.showToast("验证码获取失败，请重新获取");
								setRequestVerifyCodeLimit(0);
							}
						}

						private void parseFrom(byte[] data) {
							if (data != null) {
								try {
									GetVerificationCodeResponse parse = GetVerificationCodeResponse.parseFrom(data);
									switch (parse.getResultCode()) {
									case 101:
										UIUtils.showToast("今日已接收过多个验证码，请明日再试");
										break;
									case 102:
										UIUtils.showToast("验证码已发送，请勿重复提交");
										break;
									case 103:
										UIUtils.showToast("提交频繁，请半小时后再试");
										break;
									default:
										UIUtils.showToast(DEFAULT_MSG_NET_PROBLEM);
										break;
									}
									return;
								} catch (InvalidProtocolBufferException e) {}
							}
							UIUtils.showToast(DEFAULT_MSG_NET_PROBLEM);
						}
					});
				if (requestVerificationCode) {
					setRequestVerifyCodeLimit(REGET_VERIFYCODE_TIMEOUT);
				}
			}
		});
		this.regetVerifyCodeCountDown = new ReGetVerifyCodeCountDown(
			buttonGetVerifyCode,
			root.findViewById(R.id.group_get_verifycode_timeout),
			(TextView) root.findViewById(R.id.text_timeout));

	}
	
	public void hidePhoneNumberEditText(){
		if(editPhoneNumber!=null){
			editPhoneNumber.setVisibility(View.GONE);
		}
	}


	/**
	 * 设置验证码输入框及获取验证码按钮控件的可见性
	 */
	public void setVerifyCodeCtrlsVisibility(int v) {
		groupVerifyCode.setVisibility(v);
	}


	/**
	 * 取得用户输入的手机号码
	 */
	public String getPhoneNumber() {
		return editPhoneNumber.getText().toString();
	}

	/**
	 * 取得用户输入的短信验证码
	 */
	public String getVerifyCode() {
		return editVerifyCode.getText().toString();
	}
	
	/** 判断手机号码是否合法？（是不是11个数字且以“1”开头） */
	private static boolean isPhoneNumberValid(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() != 11 || phoneNumber.charAt(0) != '1') {
			return false;
		}
		for (int i = 1; i < 11; ++i) {
			char ch = phoneNumber.charAt(i);
			if (ch < '0' || ch > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查用户输入的手机号码是否合法
	 */
	public boolean checkPhoneNumber() {
		String phoneNumber = getPhoneNumber();
		if (TextUtils.isEmpty(phoneNumber)) {
			onPhoneNumberError(STR_INVALID_PHONE_NUMBER_EMPTY);
			return false;
		}
		if (!isPhoneNumberValid(phoneNumber)) {

			onPhoneNumberError(STR_INVALID_PHONE_NUMBER);
			return false;
		}
		return true;
	}

	private void onPhoneNumberError(String errorMess) {
		editPhoneNumber.setError(errorMess);
		showErrorMessage(errorMess);
	}

	/**
	 * 检查验证码长度是否有效
	 */
	public boolean checkVerifyCodeLen() {
		String code = getVerifyCode();
		boolean valid;
		if (code.length() == 4) {
			valid = true;
			for (int i = 0; i < code.length(); ++i) {
				char ch = code.charAt(i);
				if (ch < '0' || ch > '9') {
					valid = false;
					break;
				}
			}
		} else {
			valid = false;
		}
		if (!valid) {
			editVerifyCode.setError(STR_INVALID_VERIFY_CODE_LEN);
			showErrorMessage(STR_INVALID_VERIFY_CODE_LEN);
			return false;
		}
		return true;
	}

	/**
	 * 用户申请验证码的时间记录在案，60秒内不能再次申请 本函数设置限制的超时时长（按钮变灰且倒计时）
	 * 
	 * @param timeoutSeconds
	 *            到期秒数
	 */
	public void setRequestVerifyCodeLimit(int timeoutSeconds) {
		if (regetVerifyCodeCountDown != null) {
			regetVerifyCodeCountDown.start(timeoutSeconds);
		}
	}

	private static void showErrorMessage(String text) {
		UIUtils.showToast(text);
	}
	
	public static final class PasswordView {
		private final EditText editPassword;
		private final ImageView imgEye;
		/** 密码是否以明文显示 */
		private boolean passwordVisible = true;
		public PasswordView(EditText editPassword, ImageView imgEye) {
			this.editPassword = editPassword;
			this.imgEye = imgEye;
			this.imgEye.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					setPasswordVisible(!passwordVisible);
				}
			});
		}
		
		/**
		 * 设置密码是否可见
		 * 
		 * @param visible
		 */
		public void setPasswordVisible(boolean visible) {
			if (this.passwordVisible != visible) {
				this.passwordVisible = visible;
				imgEye.setImageResource(passwordVisible ? R.drawable.login_btn_password_open : R.drawable.login_btn_password_closed);
				editPassword.setInputType(passwordVisible
					? (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
					: (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD));
			}
		}
		
		/**
		 * 设置密码框右边的眼睛（密码是否可见）图标是否可见
		 */
		public void setPasswordEyeIconVisibility(int visibility) {
			imgEye.setVisibility(visibility);
		}
		
		/**
		 * 检查用户输入的密码长度是否有效
		 */
		public boolean checkPasswordLen() {
			String password = getPassword();
			int len = password.length();
			if (len < 6 || len > 32) {
				editPassword.setError(STR_INVALID_PASSWORD_LEN);
				showErrorMessage(STR_INVALID_PASSWORD_LEN);
				return false;
			}
			return true;
		}
		
		/**
		 * 取得用户输入的密码
		 */
		public String getPassword() {
			return editPassword.getText().toString();
		}
	}

	public void setPasswordVisible(boolean b) {
		passwordView.setPasswordVisible(b);
	}

	/**
	 * 设置密码框右边的眼睛（密码是否可见）图标是否可见
	 */
	public void setPasswordEyeIconVisibility(int visibility) {
		passwordView.setPasswordEyeIconVisibility(visibility);
	}

	public boolean checkPasswordLen() {
		return passwordView.checkPasswordLen();
	}

	public String getPassword() {
		return passwordView.getPassword();
	}
	
	public EditText getEditPassword(){
		return passwordView.editPassword;
	}

}
