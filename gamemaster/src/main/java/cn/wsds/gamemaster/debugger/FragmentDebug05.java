package cn.wsds.gamemaster.debugger;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager.Checker.Result;
import cn.wsds.gamemaster.thread.GameRunningTimeObserver;
import cn.wsds.gamemaster.thread.InactiveUserReminder;
import cn.wsds.gamemaster.ui.ActivityGuider;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

public class FragmentDebug05 extends FragmentDebug {

	private static final String TEST_GAME_PACKAGE_NAME = "com.tencent.clover";

	// 节省时长相关
	private EditText editSpareTime;
	private TextView textSpareTime;

	// 长时间未开加速推通知
	private EditText editUnitTime, editSendTime;

	// 显示3页还是1页引导页？
	private RadioGroup radioGroupGuidePages;

	@Override
	protected int getRootLayoutResId() {
		return R.layout.fragment_debug_05;
	}

	@Override
	protected void initView(View root) {
		initSpareTime(root);
		initNetCheck(root);
		initNotificationLongTimeCloseAccel(root);
		initGuidePageTest(root);
	}

	private void initGuidePageTest(View root) {
		radioGroupGuidePages = (RadioGroup) root.findViewById(R.id.debug_radio_group_guide_page);
		root.findViewById(R.id.debug_button_guide_show).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ActivityGuider.show(getActivity(), radioGroupGuidePages.getCheckedRadioButtonId() == R.id.debug_radio_guide_single_page);
			}
		});
	}

	private void initNotificationLongTimeCloseAccel(View root) {
		editUnitTime = (EditText) root.findViewById(R.id.debug_time_unit_value);
		editSendTime = (EditText) root.findViewById(R.id.debug_send_time_value);
		root.findViewById(R.id.debug_time_unit_confirm).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				long timeUnit = 1;
				try {
					timeUnit = parseIntFromEdit(editUnitTime);
					UIUtils.showToast(String.format("时间单位设置成%ds", timeUnit));
					InactiveUserReminder.TIME_UNIT = timeUnit * 1000;
					if (!AccelOpenManager.isStarted()) {
						InactiveUserReminder.instance.restart();
					}
				} catch (Exception e) {}
			}
		});
		root.findViewById(R.id.debug_send_time_confirm).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					long sendTime = parseIntFromEdit(editSendTime);
					UIUtils.showToast(String.format("推送时间设置成%ds", sendTime));
					GameRunningTimeObserver.NOTIFICATION_DURATION = sendTime * 1000;
				} catch (Exception e) {}
			}
		});
	}

	private void initNetCheck(View root) {
		root.findViewById(R.id.debug_button_start_check).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				NetworkCheckManager.start(getActivity(), new NetworkCheckManager.Observer() {
					
					@Override
					public void onNetworkCheckResult(Result result) {
						String str = "网络检查结果：" + result;
						Log.d("NetworkCheck", str);
						UIUtils.showToast(str, Toast.LENGTH_SHORT);
					}
				});
			}
		});
		root.findViewById(R.id.debug_button_stop_check).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				NetworkCheckManager.postStopRequest();
			}
		});
	}

	private void initSpareTime(View root) {
		root.findViewById(R.id.debug_button_spare_time).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setSpareTime();
			}
		});
		textSpareTime = (TextView) root.findViewById(R.id.debug_text_spare_time);
		editSpareTime = (EditText) root.findViewById(R.id.debug_edit_spare_time);
		GameInfo gi = GameManager.getInstance().getGameInfo(TEST_GAME_PACKAGE_NAME);
		if (gi != null) {
			onSpareTimeChange(gi.getAccumulateShortenWaitTimeMilliseconds());
		} else {
			onSpareTimeChange(0);
		}
	}

	private void setSpareTime() {
		int value;
		try {
			value = Integer.parseInt(editSpareTime.getText().toString().trim());
		} catch (NumberFormatException e) {
			UIUtils.showToast("无效的值");
			return;
		}
		GameInfo gi = GameManager.getInstance().getGameInfo(TEST_GAME_PACKAGE_NAME);
		if (gi != null) {
			int old = gi.getAccumulateShortenWaitTimeMilliseconds();
			gi.setShortenTimeMillisecond(value - old, 0);
		}
		onSpareTimeChange(value);
	}

	private void onSpareTimeChange(int milliseconds) {
		editSpareTime.setText(Integer.toString(milliseconds));
		textSpareTime.setText(UIUtils.formatTotalSparedTime(milliseconds));
	}

	private static int parseIntFromEdit(EditText edit) throws Exception {
		try {
			return Integer.parseInt(edit.getText().toString());
		} catch (NumberFormatException e) {
			UIUtils.showToast("请输入有效的整数值");
			throw new Exception();
		}
	}
}
