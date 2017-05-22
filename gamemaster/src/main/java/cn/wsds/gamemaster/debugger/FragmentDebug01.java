package cn.wsds.gamemaster.debugger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.StringUtils;
import com.subao.net.NetManager;
import com.subao.utils.UrlConfig;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.tools.VPNUtils;

public class FragmentDebug01 extends FragmentDebug {

	private class NetChangeObserver extends EventObserver {

		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			refreshTextNetworkType(state);
		}

		public void refreshTextNetworkType(NetTypeDetector.NetType type) {
			textNetworkType.setText(String.format("当前网络：%d", type.value));
		}

	}

	private NetChangeObserver netChangeObserver;

	/** 用于显示当前网络类型的TextView */
	private TextView textNetworkType;

	private RadioButton radioNormalServer, radioTestServer;
	private EditText editNodeChoose;

	@Override
	protected int getRootLayoutResId() {
		return R.layout.fragment_debug_01;
	}

	@Override
	protected void initView(View root) {
		initNetworkType(root);
		initServerChoose(root);
		initTestUMengKey(root);
		initLogLevel(root);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (netChangeObserver != null) {
			TriggerManager.getInstance().deleteObserver(netChangeObserver);
			netChangeObserver = null;
		}
	}

	private void initNetworkType(View root) {
		textNetworkType = (TextView) root.findViewById(R.id.debug_text_network_type);
		if (netChangeObserver == null) {
			netChangeObserver = new NetChangeObserver();
			TriggerManager.getInstance().addObserver(netChangeObserver);
		}
		netChangeObserver.refreshTextNetworkType(NetManager.getInstance().getCurrentNetworkType());
	}

	// 选择服务器相关
	private void initServerChoose(View root) {
		this.radioNormalServer = (RadioButton) root.findViewById(R.id.debug_radio_normalserver);
		this.radioTestServer = (RadioButton) root.findViewById(R.id.debug_radio_testserver);
		switch (UrlConfig.instance.getServerType()) {
		case NORMAL:
			radioNormalServer.setChecked(true);
			break;
		case TEST:
			radioTestServer.setChecked(true);
			break;
		}
		root.findViewById(R.id.debug_button_choose_server).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				UrlConfig.ServerType expected;
				if (radioNormalServer.isChecked()) {
					expected = UrlConfig.ServerType.NORMAL;
				} else if (radioTestServer.isChecked()) {
					expected = UrlConfig.ServerType.TEST;
				} else {
					return;
				}
				if (expected == UrlConfig.instance.getServerType()) {
					return;
				}
				UrlConfig.instance.setServerType(expected);
				ConfigManager.getInstance().setServerType(expected);
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage("改变所用服务器后须重启程序").setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						AppMain.exit(true);
					}
				});
				builder.show();
			}
		});
		//
		editNodeChoose = (EditText) root.findViewById(R.id.debug_edit_node_choose);
		editNodeChoose.setText(ConfigManager.getInstance().getDebugNodeIP());
		root.findViewById(R.id.debug_button_node_choose).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String ip = editNodeChoose.getText().toString().trim();
				if (ConfigManager.getInstance().setDebugNodeIP(ip)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
					builder.setMessage("测试节点IP已改变，须重启程序生效");
					builder.setCancelable(true);
					builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							AppMain.exit(true);
						}
					});
					builder.show();
				}
			}
		});
	}

	private void initLogLevel(View root) {
		Spinner spinnerLogLevel = (Spinner) root.findViewById(R.id.debug_spinner_log_level);
		int selectedIndex = ConfigManager.getInstance().getLogLevel() - 1;
		selectedIndex = Math.max(0, Math.min(selectedIndex, spinnerLogLevel.getAdapter().getCount() - 1));
		spinnerLogLevel.setSelection(selectedIndex, false);
		spinnerLogLevel.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int logLevel = position + 1;
				ConfigManager.getInstance().setLogLevel(logLevel);
				VPNUtils.sendSetLogLevel(logLevel, "Debug");
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// do nothing
			}

		});
	}

	private void initTestUMengKey(View root) {
		CheckBox check = (CheckBox) root.findViewById(R.id.debug_check_test_umeng_key);
		check.setChecked(ConfigManager.getInstance().getUseTestUmengKey());
		check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (ConfigManager.getInstance().setUseTestUmengKey(isChecked)) {
					AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
					adb.setTitle(R.string.app_name);
					adb.setMessage(String.format("设置已改变，将%s使用测试友盟KEY！\n是否立刻退出程序然后手工重启？", isChecked ? StringUtils.EMPTY : "不"));
					DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == DialogInterface.BUTTON_POSITIVE) {
								getActivity().finish();
								AppMain.exit(false);
							}
							dialog.dismiss();
						}
					};
					adb.setPositiveButton("退出本程序", l);
					adb.setNegativeButton("不退出", l);
					adb.show();
				}
			}
		});
	}



}
