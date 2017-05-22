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
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;

public class FragmentDebugNoticeAccelResult extends Fragment {

	private EditText editAcceltime;
	private OnClickListener listener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.debug_button_send_notice:
				sendNotice();
				break;
			}
		}
	};
	
	private int getValue(String context) {
		return TextUtils.isEmpty(context) ? -1 : Integer.valueOf(context);
	}

	protected void sendNotice() {
		GameInfo gameInfo = GameManager.getInstance().getRandomGame();
		if (gameInfo != null) {
			String strAccelTime = editAcceltime.getText().toString();
			int accelTime = getValue(strAccelTime);
			AppNotificationManager.sendGameAccelResult(accelTime, 45, gameInfo);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_debugger_notice_accel_result, container, false);
		initView(view);
		return view;
	}

	/**
	 * 初始化部分view
	 * @param view
	 */
	private void initView(View view) {
		editAcceltime = (EditText) view.findViewById(R.id.debug_edit_acceltime);
		view.findViewById(R.id.debug_button_send_notice).setOnClickListener(listener);
	}
	
}
