package cn.wsds.gamemaster.debugger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Debug;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.message.MessageManager;
import cn.wsds.gamemaster.message.MessageManager.Record;
import cn.wsds.gamemaster.tools.onlineconfig.OnlineConfigAgent;
import cn.wsds.gamemaster.ui.ActivityInstalled;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.view.OpenFloatWindowHelpDialog;

public class FragmentDebug06 extends FragmentDebug {

	private EditText editPointHistoryNumber;

	private TextView textMemoryInfo;

	private EditText editAccelTime;

	@Override
	protected int getRootLayoutResId() {
		return R.layout.fragment_debug_06;
	}

	@Override
	protected void initView(View root) {
		initMessageTest(root);
		initPointHistory(root);
		initMemoryInfo(root);
		root.findViewById(R.id.debug_button_miui_help).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OpenFloatWindowHelpDialog.open(getActivity(), null, null);
			}
		});
		root.findViewById(R.id.debug_button_installed_info).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				UIUtils.turnActivity(getActivity(), ActivityInstalled.class);
			}
		});
		root.findViewById(R.id.debug_text_user_center_global_config).setOnLongClickListener(new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				((TextView) v).setText(
					OnlineConfigAgent.getInstance().getGlobalConfig().toString() 
					// + "\n" + OnlineConfigAgent.getInstance().getTaskListResponse()
					);
				return true;
			}
		});
		//
		editAccelTime = (EditText) root.findViewById(R.id.debug_value_accel_time);
		root.findViewById(R.id.debug_button_confirm_accel_time).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String valueString = editAccelTime.getText().toString();
					GameManager.getInstance().setAccelTimeSecondsAmount(Integer.valueOf(valueString));
				} catch (Exception e) {
					Toast.makeText(AppMain.getContext(), "请输入整数", Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	private void initMemoryInfo(View root) {
		textMemoryInfo = (TextView) root.findViewById(R.id.debug_text_show_memory);
		textMemoryInfo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				refreshMemoryInfo(v.getContext());
			}
		});
		refreshMemoryInfo(root.getContext());
	}

	private void initPointHistory(View root) {
		editPointHistoryNumber = (EditText) root.findViewById(R.id.debug_point_histroy_number);
		root.findViewById(R.id.debug_point_histroy_number_confirm).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String historyNumber = editPointHistoryNumber.getText().toString();
				if (TextUtils.isEmpty(historyNumber)) {
					ConfigManager.getInstance().setDefaultDebugPointHistoryRequest();
					UIUtils.showToast("积分历史请求条数设置为默认");
				} else {
					try {
						ConfigManager.getInstance().setDebugPointHistoryRequest(Integer.valueOf(historyNumber));
						UIUtils.showToast("积分历史请求条数设置成功");
					} catch (NumberFormatException e) {
						e.printStackTrace();
						UIUtils.showToast("积分历史请求条数设置为默认");
						ConfigManager.getInstance().setDefaultDebugPointHistoryRequest();
					}
				}
			}
		});

		int debugPointHistoryRequest = ConfigManager.getInstance().getDebugPointHistoryRequest();
		if (debugPointHistoryRequest >= 0) {
			editPointHistoryNumber.setText(String.valueOf(debugPointHistoryRequest));
		}
	}

	private void initMessageTest(View root) {
		root.findViewById(R.id.message_center_createtext).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				MessageManager.getInstance().addDebugCreateMessage("测试文本消息", "这是一个测试消息", Record.TYPE_HTML);
				UIUtils.showToast("已创建，请到消息中心查看");

			}
		});
		root.findViewById(R.id.message_center_createurl).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				MessageManager.getInstance().addDebugCreateMessage("测试链接消息", "http://www.baidu.com", Record.TYPE_URL);
				UIUtils.showToast("已创建，请到消息中心查看");
			}
		});
	}

	private void refreshMemoryInfo(Context context) {
		String memoryInfo = getMemoryInfo(context);
		textMemoryInfo.setText("内存情况（点击刷新）：\n" + memoryInfo);
	}

	private static String getMemoryInfo(Context context) {
		StringBuilder sb = new StringBuilder();
		String string;
		String str1 = "/proc/meminfo";
		String str2;
		String[] arrayOfString;
		long initial_memory = 0;
		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
			str2 = localBufferedReader.readLine();
			arrayOfString = str2.split("\\s+");
			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;
			localBufferedReader.close();
			string = Formatter.formatFileSize(context, initial_memory);
			sb.append("System total memory: ").append(string);
		} catch (IOException e) {}

		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo memoryInfo = new MemoryInfo();
		am.getMemoryInfo(memoryInfo);
		string = Formatter.formatFileSize(context, initial_memory - memoryInfo.availMem);
		sb.append("\nSystem used memory: ").append(string);

		string = Formatter.formatFileSize(context, memoryInfo.availMem);
		sb.append("\nSystem free memory: ").append(string);

		List<RunningAppProcessInfo> lRunningAppProcessInfos = am.getRunningAppProcesses();
		for (RunningAppProcessInfo ra : lRunningAppProcessInfos) {
			if (ra.processName.equals(context.getPackageName())) {
				int[] pids = new int[] {
					ra.pid
				};
				Debug.MemoryInfo dm = am.getProcessMemoryInfo(pids)[0];
				string = Formatter.formatFileSize(context, dm.getTotalPss() * 1024);
				sb.append("\nApp used memory: ").append(string);
				break;
			}
		}
		return sb.toString();
	}

}
