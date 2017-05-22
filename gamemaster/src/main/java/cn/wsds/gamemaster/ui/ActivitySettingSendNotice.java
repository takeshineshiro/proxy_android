package cn.wsds.gamemaster.ui;

import android.os.Bundle;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.ui.view.Switch;
import cn.wsds.gamemaster.ui.view.Switch.OnChangedListener;

/**
 * 设置子界面 --- 推送消息设置
 */
public class ActivitySettingSendNotice extends ActivityBase {
	
	private Switch accelResultSendSwitch;
	private OnChangedListener onChangedListener = new OnChangedListener() {
		
		@Override
		public void onCheckedChanged(Switch checkSwitch, boolean checked) {
			switch(checkSwitch.getId()){
			case R.id.check_accelresult_send:
				ConfigManager.getInstance().setSendNoticeAccelResult(checked);
//				if (!checked){
//					StatisticDefault.addEvent(ActivitySettingSendNotice.this, StatisticDefault.Event.SWITCH_NOTIFICATION_REPORT_GAME);
//				}
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(R.string.notice_accelresult_send);
		setContentView(R.layout.activity_setting_sendnotice);
		initSendNotice();
	}

	private void initSendNotice() {
		accelResultSendSwitch = (Switch) findViewById(R.id.check_accelresult_send);
		accelResultSendSwitch.setChecked(ConfigManager.getInstance().getSendNoticeAccelResult());
		accelResultSendSwitch.setOnChangedListener(onChangedListener);
	}
	

}
