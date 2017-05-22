package cn.wsds.gamemaster.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

public class ActivityJpushMessage extends ActivityBase{

	private static final String TITLE = "cn.wsds.gamemaster.jpush.message.title" ;
	private static final String CONTENT = "cn.wsds.gamemaster.jpush.message.content";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jpush_message);
		
		Intent intent = getIntent();
		String title = intent.getStringExtra(TITLE);
		if (TextUtils.isEmpty(title)) {
			title = "通知消息查看";
		}
		setDisplayHomeArrow(title);
	
		String content = intent.getStringExtra(CONTENT);
		((TextView)findViewById(R.id.text_message)).setText(content);
	}

	public static void show(Context context ,String title , String content){
		Intent intent = new Intent(context,ActivityJpushMessage.class);
		if(!TextUtils.isEmpty(title)){
			intent.putExtra(TITLE, title);
		}
		
		if(!TextUtils.isEmpty(content)){
			intent.putExtra(CONTENT, content);
		}
		
		context.startActivity(intent);
	}
}
