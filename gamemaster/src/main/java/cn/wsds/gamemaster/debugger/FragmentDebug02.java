package cn.wsds.gamemaster.debugger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import cn.wsds.gamemaster.AppInitializer;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.ErrorReportor;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.netdelay.NetDelayExceptionWatcher;
import cn.wsds.gamemaster.tools.ScreenCapUtils;
import cn.wsds.gamemaster.ui.ActivityErrorLog;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowInGame;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowInReconnect;
import cn.wsds.gamemaster.ui.floatwindow.ToastEx;
import cn.wsds.gamemaster.ui.floatwindow.ToastFlowWarning;

import com.subao.utils.FileUtils;

public class FragmentDebug02 extends FragmentDebug {

	// 游戏悬浮窗相关
	private EditText editNetDelayFloatInGame;
	private Button buttonNetDelayFloatInGame, buttonNetRequestFloatInGame;

	// 流量警告相关
	private EditText editToastFlowWarningParams;
	private CheckBox checkboxFlowWarningGameSelf;

	
	private EditText editNextInitResult;
	
	private View.OnClickListener viewClick = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.debug_button_toast_flow_warning:
				ToastFlowWarning.show(AppMain.getContext(), "一二三四五六七", checkboxFlowWarningGameSelf.isChecked());
				break;
			case R.id.debug_button_all_effect:
				turnSecondDebugPage(ActivityDebuggerSecondPage.VALUE_DEBUG_TOAST_TEST);
				break;
			case R.id.debug_button_toastex:
				List<String> messages = new ArrayList<String>(2);
				messages.add("迅游手游开始为您加速");
				messages.add("延迟降低%d%%");
				ToastEx.show(v.getContext(), messages, null,true);
				break;
			case R.id.debug_button_toast_app:
				UIUtils.showToast("这是用Application的Toast");
				break;
			case R.id.debug_button_toast_activity:
				UIUtils.showToast("这是用Activity的Toast");
				break;
			case R.id.debug_button_java_crash:
				ErrorReportor.testJavaCrash();
				break;
			case R.id.debug_button_native_crash:
				ErrorReportor.testNativeCrash();
				break;
			case R.id.debug_button_anr_crash:
				ErrorReportor.testANRCrash();
				break;
			case R.id.debug_button_report_crash:
				ErrorReportor.postCatchedException(new RuntimeException("This is a test exception"));
				UIUtils.showToast("Crash 已上传");
				break;
			case R.id.debug_link_view_error_log:
				UIUtils.turnActivity(getActivity(), ActivityErrorLog.class);
				break;
			case R.id.debug_button_init_exit:
				CommonAlertDialog clearExitDialog = createAlertDialog("完全退出", "你确定要清空数据完全退出app");
				clearExitDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 删除文件
						FileUtils.deleteFileOrDirectory(FileUtils.getDataDirectory());
						// 清空配置
						ConfigManager.getInstance().clearConfig();
						exitApp();
					}
				});
				clearExitDialog.show();
				break;
			}
		}
	};

	@Override
	protected int getRootLayoutResId() {
		return R.layout.fragment_debug_02;
	}
	
	@Override
	protected void initView(View root) {
		initToastFlowWarning(root);
		initFloatInGame(root);
		initErroLog(root);
		initExitApp(root);
		root.findViewById(R.id.debug_button_all_effect).setOnClickListener(viewClick);
		//
		// 截屏总是失败
		CheckBox switchScreenShotAlwaysFail = (CheckBox) root.findViewById(R.id.debug_screenshot_always_fail);
		switchScreenShotAlwaysFail.setChecked(ScreenCapUtils.alwaysFail);
		switchScreenShotAlwaysFail.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				ScreenCapUtils.alwaysFail = isChecked;
			}
		});
		// 开加速总是失败
		CheckBox switchOpenAccelAlwaysFail = (CheckBox)root.findViewById(R.id.debug_open_accel_always_fail);
		switchOpenAccelAlwaysFail.setChecked(DebugParams.getOpenAccelAlwaysFail());
		switchOpenAccelAlwaysFail.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				DebugParams.setOpenAccelAlwaysFail(isChecked);
			}
		});
		//
		editNextInitResult = (EditText)root.findViewById(R.id.debug_edit_next_init_result);
		root.findViewById(R.id.debug_button_next_init_result).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					int code = Integer.parseInt(editNextInitResult.getText().toString());
					AppInitializer.writeInitErrorTestFile(code);
					exitApp();
				} catch (NumberFormatException e) {
					UIUtils.showToast("请填写正确的初始化错误码");
				}
			}
		});
	}

	private void initToastFlowWarning(View root) {
		Button buttonShowToastFlowWarning = (Button) root.findViewById(R.id.debug_button_toast_flow_warning);
		this.checkboxFlowWarningGameSelf = (CheckBox) root.findViewById(R.id.debug_checkbox_toast_flow_warning_gameself);
		Button buttonToastFlowWarningSetParams = (Button) root.findViewById(R.id.debug_button_toast_flow_warning_set_params);
		this.editToastFlowWarningParams = (EditText) root.findViewById(R.id.debug_edit_toast_flow_warning);

		NetDelayExceptionWatcher.Params params = NetDelayExceptionWatcher.getParams();
		if (params != null) {
			this.editToastFlowWarningParams.setText(String.format("%d,%d,%d,%d,%d",
				params.thresholdExceptionDelayValue, params.thresholdExceptionDelayCount, params.secondsOfFlowCheck,
				params.thresholdForFlowCheck, params.intervalPrompt / 1000));
		}
		//
		buttonShowToastFlowWarning.setOnClickListener(viewClick);
		buttonToastFlowWarningSetParams.setOnClickListener(new View.OnClickListener() {

			private NetDelayExceptionWatcher.Params makeParams(String text) {
				if (TextUtils.isEmpty(text)) {
					return null;
				}
				String[] fields = text.split(",");
				if (fields == null || fields.length != 5) {
					return null;
				}
				try {
					return new NetDelayExceptionWatcher.Params(Long.parseLong(fields[0]), Integer.parseInt(fields[1]),
						Integer.parseInt(fields[2]), Long.parseLong(fields[3]), Long.parseLong(fields[4]) * 1000);
				} catch (Exception e) {
					return null;
				}
			}

			@Override
			public void onClick(View v) {
				NetDelayExceptionWatcher.Params params = makeParams(editToastFlowWarningParams.getText().toString());
				if (params == null) {
					UIUtils.showToast("无效的参数格式");
					return;
				}
				if (NetDelayExceptionWatcher.reset(params)) {
					UIUtils.showToast("设置成功");
				} else {
					UIUtils.showToast("设置失败");
				}
			}
		});

	}

	// 游戏悬浮窗相关
	private void initFloatInGame(View root) {
		editNetDelayFloatInGame = (EditText) root.findViewById(R.id.debug_edit_float_net_delay);
		buttonNetDelayFloatInGame = (Button) root.findViewById(R.id.debug_button_float_net_delay);
		buttonNetDelayFloatInGame.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String text = editNetDelayFloatInGame.getText().toString().trim();
				int delay = -10000;
				if (!text.isEmpty()) {
					try {
						delay = Integer.parseInt(text);
					} catch (NumberFormatException e) {
						UIUtils.showErrorDialog(getActivity(), "无效的延迟值");
					}
				}

				TriggerManager.getInstance().raiseFirstSegmentNetDelayChange(delay);
			}
		});
		//
		buttonNetRequestFloatInGame = (Button) root.findViewById(R.id.debug_button_float_net_request);
		buttonNetRequestFloatInGame.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TriggerManager.getInstance().raiseShortConnGameNetRequestEnd();
			}
		});
		//
		Switch switchFloatReconn = (Switch) root.findViewById(R.id.debug_switch_float_reconn_window);
		switchFloatReconn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					FakeConnectionRepairPrompt.execute(buttonView.getContext());
				} else {
					FloatWindowInReconnect.destroyInstance();
				}
			}
		});
		//
		Switch switchFloatInGame = (Switch) root.findViewById(R.id.debug_switch_float_in_game);
		switchFloatInGame.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					FloatWindowInGame.createInstance(getActivity(), GameManager.getInstance().getRandomGame(), -1, -1, false);
				} else {
					FloatWindowInGame.destroyInstance();
				}
				setFloatInGameControllersEnabled(isChecked);
			}
		});
		//
		FloatWindowInGame.destroyInstance();
		//
		root.findViewById(R.id.debug_button_toastex).setOnClickListener(viewClick);
		root.findViewById(R.id.debug_button_toast_app).setOnClickListener(viewClick);
		root.findViewById(R.id.debug_button_toast_activity).setOnClickListener(viewClick);
	}

	/**
	 * 初始化和错误日志相关的界面
	 */
	private void initErroLog(View root) {
		root.findViewById(R.id.debug_button_java_crash).setOnClickListener(viewClick);
		root.findViewById(R.id.debug_button_native_crash).setOnClickListener(viewClick);
		root.findViewById(R.id.debug_button_anr_crash).setOnClickListener(viewClick);
		root.findViewById(R.id.debug_button_report_crash).setOnClickListener(viewClick);
		root.findViewById(R.id.debug_link_view_error_log).setOnClickListener(viewClick);
		root.findViewById(R.id.debug_link_del_error_log).setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle("警告").setMessage("确定要删除错误日志文件吗？");
				DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (DialogInterface.BUTTON_NEGATIVE == which) {
							File file = ErrorReportor.getErrorLogFile();
							if (file.exists() && file.isFile()) {
								if (file.delete()) {
									UIUtils.showToast("删除成功");
								} else {
									UIUtils.showToast("删除失败");
								}
							} else {
								UIUtils.showToast("日志文件不存在");
							}
						}
						dialog.dismiss();
					}
				};
				builder.setPositiveButton("不删除", l);
				builder.setNegativeButton("删除！", l);
				builder.setCancelable(true);
				builder.show();
				return false;
			}
		});
	}

	/**
	 * 完全退出
	 */
	private void initExitApp(View root) {
		root.findViewById(R.id.debug_button_init_exit).setOnClickListener(viewClick);
	}

	private CommonAlertDialog createAlertDialog(String title, String message) {
		CommonAlertDialog clearErrorDialog = new CommonAlertDialog(getActivity());
		clearErrorDialog.setTitle(title);
		clearErrorDialog.setMessage(message);
		return clearErrorDialog;
	}

	private void setFloatInGameControllersEnabled(boolean enabled) {
		editNetDelayFloatInGame.setEnabled(enabled);
		buttonNetDelayFloatInGame.setEnabled(enabled);
		buttonNetRequestFloatInGame.setEnabled(enabled);
	}

	private void exitApp() {
		getActivity().finish();
		AppMain.exit(false);
	}

}
