package cn.wsds.gamemaster.ui;

import com.subao.common.data.SubaoIdManager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

/**
 * 反馈界面
 */
public class ActivityFeedback extends ActivityBase{

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feedback);
		setDisplayHomeArrow(R.string.main_menu_feedback);
		initView();
	}

	private void initView() {
		ViewClick viewClick = new ViewClick();
		TextView qq_group_2 = (TextView)findViewById(R.id.qq_group_2);
		qq_group_2.setText(qq_group_2.getText() + " （推荐添加）");
		qq_group_2.setOnClickListener(viewClick);
		//
		findViewById(R.id.qq_group_1).setOnClickListener(viewClick);
		findViewById(R.id.qq_group_king_glory).setOnClickListener(viewClick);
		findViewById(R.id.qq_customer_service).setOnClickListener(viewClick);
		findViewById(R.id.feedback).setOnClickListener(viewClick);
		initDeviceModelView();
	}
	
	private void initDeviceModelView() {
		TextView deviceModelText = (TextView)findViewById(R.id.device_model);
		String subaoId = SubaoIdManager.getInstance().getSubaoId();
		subaoId = subaoId == null ? "" : subaoId;
		deviceModelText.setText(String.format("设备标识：%s", subaoId));
		deviceModelText.setTag(subaoId);
		deviceModelText.setOnLongClickListener(new View.OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				Object tag = v.getTag();
				if (tag == null) {
					return false;
				}
				String subaoId = tag.toString();
				if (subaoId.length() == 0) {
					return false;
				}
				ClipboardManager clipboard = (ClipboardManager) ActivityFeedback.this.getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setPrimaryClip(ClipData.newPlainText(null, subaoId));
				UIUtils.showToast("设备标识已复制到剪贴板");
				return true;
			}
		});
	}
	/**
	 * 点击监听类
	 */
	private class ViewClick implements OnClickListener {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.qq_group_1:   //官方QQ一群
				UIUtils.openQQgroup(ActivityFeedback.this, UIUtils.KEY_QQ_GROUP_1);
				break;
			case R.id.qq_group_2:   //官方QQ二群
				UIUtils.openQQgroup(ActivityFeedback.this, UIUtils.KEY_QQ_GROUP_2);
				break;
			case R.id.qq_group_king_glory:
				UIUtils.openQQgroup(ActivityFeedback.this, UIUtils.KEY_QQ_GROUP_KING_GLORY);
				break;
			case R.id.qq_customer_service:	//客服QQ
				UIUtils.openServiceQQ(ActivityFeedback.this);
				break;
			case R.id.feedback:
				UIUtils.turnActivity(ActivityFeedback.this, ActivityNormalFeedback.class);
				break;
			}
		}
	}
}
