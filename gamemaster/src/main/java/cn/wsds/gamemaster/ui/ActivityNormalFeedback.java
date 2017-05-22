package cn.wsds.gamemaster.ui;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserFeedback;
import cn.wsds.gamemaster.tools.ReportFeedback;
import cn.wsds.gamemaster.ui.UIUtils.ProgressAlertDialog;

import com.subao.common.utils.InfoUtils;
import com.subao.net.NetManager;

/**
 * 普通反馈界面
 */
public class ActivityNormalFeedback extends ActivityBase{

	private ProgressAlertDialog progressAlertDialog;
	private EditText mContact;
	private EditText feedbackContent;
	private View submit;
	private final SendClickListener viewClick = new SendClickListener();
	private EditText phoneModel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_normal_feedback);
		setDisplayHomeArrow("普通反馈");
		initView();
	}

	private void initView() {
		mContact = (EditText) findViewById(R.id.feedback_contact);
		feedbackContent = (EditText) findViewById(R.id.feedback_msg);
		phoneModel = (EditText) findViewById(R.id.feedback_phone_model);
		phoneModel.setText(Build.MODEL);
		
		EditText versionName = (EditText) findViewById(R.id.feedback_version_name);
		versionName.setText(InfoUtils.getVersionName(getApplicationContext()));
		
		submit = findViewById(R.id.feedback_submit);
		submit.setOnClickListener(viewClick);
		
	}
	
	/**
	 * 点击监听类
	 */
	private class SendClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			String content = feedbackContent.getText().toString().trim();
			if (TextUtils.isEmpty(content)) {
				feedbackContent.setText(content);
				UIUtils.showToast("您还没有发表意见哦");
				return;
			}
			if (!NetManager.getInstance().isConnected()) {
				UIUtils.showToast("当前网络不可用，请稍后再试");
				return;
			}
			
			String contact = mContact.getText().toString().trim();
			new FeedbackTask().send(content, contact,phoneModel.getText().toString().trim());
		}
	}
	
	private final class FeedbackTask {
		
		private final ReportFeedback.Callback reportFeedbackCallback = new ReportFeedback.Callback() {
			
			@Override
			public void onEnd(boolean result) {
				submit.setEnabled(true);
				if(progressAlertDialog!=null)
					progressAlertDialog.dimiss();
	
				// 处理返回结果
				if (result) {
					sendSuccessed();
				} else {
					UIUtils.showToast("发送失败，请稍后重试");
				}
			}
		};
		private UserFeedback uf;
		
		public void send(String content, String contact,String phoneModel){
			uf = UserFeedback.createFeedback(content, contact);
			submit.setEnabled(false);
			if(progressAlertDialog==null){
				progressAlertDialog = new UIUtils.ProgressAlertDialog(ActivityNormalFeedback.this);
			}
			progressAlertDialog.show();
			
			
			ReportFeedback reportFeedback = new ReportFeedback(
				ReportFeedback.buildProtoHaveDeviceInfo(uf, getApplicationContext(),phoneModel),
				reportFeedbackCallback);
			reportFeedback.execute(com.subao.common.thread.ThreadPool.getExecutor());
		}
	
		/**
		 * 上传成功
		 */
		private void sendSuccessed() {
			clearForm();
			UserFeedback.History.instance.add(uf);
			UIUtils.showToast("提交成功，我们会尽快回复~");
		}
		
		/**
		 * 清除表单数据
		 */
		private void clearForm() {
			mContact.setText("");
			feedbackContent.setText("");
		}
	}
	
	
}
