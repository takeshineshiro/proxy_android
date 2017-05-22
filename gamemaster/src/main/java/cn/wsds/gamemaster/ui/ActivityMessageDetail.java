package cn.wsds.gamemaster.ui;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.message.FragmentMessage;
import cn.wsds.gamemaster.ui.message.FragmentMessage_PreventClean;

/**
 * 这个页面是一个Fragment的容器页面<br />
 * 用于显示各种消息的详情
 */
public class ActivityMessageDetail extends ActivityBase {
	
	private static final String EXTRA_NAME = "cn.wsds.gamemaster.fragment_container.which";

	/** 防清理 */
	public static final int TYPE_PREVENT_CLEAN = 1;
	
	/** 产品更名 */
	public static final int TYPE_RENAME = 2;
	
	public static void show(Context context, int type) {
		Intent intent = new Intent(context, ActivityMessageDetail.class);
		intent.putExtra(EXTRA_NAME, type);
		context.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message_detail);
		FragmentMessage fragment;
		switch (getIntent().getIntExtra(EXTRA_NAME, -1)) {
		case TYPE_PREVENT_CLEAN:
			fragment = new FragmentMessage_PreventClean();
			break;
		default:
			return;
		}
		this.setDisplayHomeArrow(fragment.getTitleResId());
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_container, fragment);
		transaction.commit();
	}
}
