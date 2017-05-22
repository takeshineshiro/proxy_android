package cn.wsds.gamemaster.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.IntentExtraName;

public class ActivityMessageGraphicsTextMixed extends ActivityBase{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message_graphics_text_mixed);
		int recordId = getIntent().getIntExtra(IntentExtraName.NOTICE_INTENT_EXTRANAME_ID, 0);
		MessageMixedInfo messageMixedInfo = MessageMixedInfo.create(recordId);
		if(null == messageMixedInfo){
			finish();
			return;
		}
		initView(messageMixedInfo);
	}

	private void initView(MessageMixedInfo messageMixedInfo) {
		setDisplayHomeArrow(messageMixedInfo.actionBartitle);
		TextView textTitle = (TextView) findViewById(R.id.text_title);
		textTitle.setText(messageMixedInfo.title);
		TextView textDesc = (TextView) findViewById(R.id.text_desc);
		textDesc.setText(messageMixedInfo.desc);
		ImageView imageLegend = (ImageView) findViewById(R.id.image_legend);
		imageLegend.setImageResource(messageMixedInfo.legendResId);
	}

	private static final class MessageMixedInfo {
		public final String actionBartitle;
		public final String title;
		public final String desc;
		public final int legendResId;
		private MessageMixedInfo(String actionBaritle, String title,
				String desc, int legendResId) {
			this.actionBartitle = actionBaritle;
			this.title = title;
			this.desc = desc;
			this.legendResId = legendResId;
		}
		public static MessageMixedInfo create(int recordId){
			switch (recordId) {
//			case MessageManager.ID_GOTO_NOTIFY_SDKEMBEDGAME_SUPPORT:
//				return createNotifySdkembedGameSupport();
			default:
				return null;
			}
		}
//		private static MessageMixedInfo createNotifySdkembedGameSupport() {
//			String actionBartitle = "加速开放";
//			String title = "内嵌加速的游戏支持加速啦~";
//			String desc = "是不是还在苦恼内嵌加速却找不到加速开关？\n在你们的口水中，攻城狮们奋力加班，现在客户端终于支持加速啦~~\\(≧▽≦)/~！\n快去游戏列表内点击启动，感受迅游手游给你的助攻~！";
//			int legendResId = R.drawable.message_hit_sdk_pic;
//			return new MessageMixedInfo(actionBartitle,title, desc, legendResId);
//		}
	}
}

