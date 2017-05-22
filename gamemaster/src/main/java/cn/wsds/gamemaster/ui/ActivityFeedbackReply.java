package cn.wsds.gamemaster.ui;

import java.util.UUID;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.data.UserFeedback;


public class ActivityFeedbackReply extends ActivityBase {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feedback_reply);
		setDisplayHomeArrow("反馈回复");
		initHelp();
		
		String extra = getIntent().getStringExtra(IntentExtraName.NOTICE_INTENT_EXTRANAME_EXTRA_DATA);
		if(TextUtils.isEmpty(extra)){
			return;
		}
		UUID uuid = UUID.fromString(extra);
		UserFeedback userFeedback = UserFeedback.searchFeedbackReplyByUUID(uuid);
		TextView textReplyLabel = (TextView)findViewById(R.id.text_reply_label);
		textReplyLabel.setText(String.format("由%d号客服回复如下：", userFeedback.getServiceId()));

		String feedbackContent = userFeedback.getFeedbackContent();
		String replyContent = userFeedback.content;
		TextView textFeedbackContent = (TextView)findViewById(R.id.feedback_content);
		textFeedbackContent.setText(feedbackContent);
		
		TextView textReplyContent = (TextView)findViewById(R.id.reply_content);
		textReplyContent.setText(replyContent);
	}

	private void initHelp() {
		TextView textHelp = (TextView) findViewById(R.id.text_help);
		textHelp.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
		textHelp.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				UIUtils.openQQgroup(ActivityFeedbackReply.this, UIUtils.KEY_QQ_GROUP_DEFAULT);
			}
		});
	}
	
}
