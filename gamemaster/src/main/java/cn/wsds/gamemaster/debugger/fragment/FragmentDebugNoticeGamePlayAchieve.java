package cn.wsds.gamemaster.debugger.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.AccelTimeChangedListener;


public class FragmentDebugNoticeGamePlayAchieve extends Fragment {
	
	OnClickListener ViewClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.debug_button_gameachieve:
				onGameAchieveClick();
				break;

			case R.id.debug_button_acceltime:
				onAccelTimeClick();
				break;
			case  R.id.debug_button_sendnotice:
				AppNotificationManager.sendGamePlayAchievements(ConfigManager.getInstance().getGamePlayAchievePercent());
				break;
			}
		}

	};
	private EditText editGameAchieve,editAccelTime;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_debugger_notice_gameachieve, container, false);
		initGameplayAchieve(view);
		initAccelTime(view);
		view.findViewById(R.id.debug_button_sendnotice).setOnClickListener(ViewClickListener);
		return view;
	}
	
	
	/** 测试使用成就 
	 * @param view */
	private void initGameplayAchieve(View view) {
		editGameAchieve = (EditText) view.findViewById(R.id.debug_edit_gameachieve);
		if(AccelTimeChangedListener.unitbaseDebug > 0){
			editGameAchieve.setText(String.valueOf(AccelTimeChangedListener.unitbaseDebug));
		}
		view.findViewById(R.id.debug_button_gameachieve).setOnClickListener(ViewClickListener);
	}
	/** 测试加速时长 
	 * @param view */
	private void initAccelTime(View view) {
		editAccelTime = (EditText) view.findViewById(R.id.debug_edit_acceltime);
		int debugGameAccelTimeSenconds = ConfigManager.getInstance().getDebugGameAccelTimeSenconds();
		if(debugGameAccelTimeSenconds > 0){
			editAccelTime.setText(String.valueOf(debugGameAccelTimeSenconds));
		}
		view.findViewById(R.id.debug_button_acceltime).setOnClickListener(ViewClickListener);
	}
	
	private void onGameAchieveClick() {
		String strGameAchieve = editGameAchieve.getText().toString();
		if(!TextUtils.isEmpty(strGameAchieve)){
			Integer unitBase = Integer.valueOf(strGameAchieve);
			AccelTimeChangedListener.unitbaseDebug = unitBase;
		}else{
			AccelTimeChangedListener.unitbaseDebug = -1;
		}
	}
	
	private int getValue(String context) {
		return TextUtils.isEmpty(context) ? -1 : Integer.valueOf(context);
	}


	protected void onAccelTimeClick() {
		String context = editAccelTime.getText().toString();
		int accelTime = getValue(context);
		ConfigManager.getInstance().setDebugGameAccelTimeSenconds(accelTime);
	}


}
