package cn.wsds.gamemaster.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.DoubleLinkUseRecords;
import cn.wsds.gamemaster.event.EventObserver.ReconnectResult;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.ui.UIUtils;

import com.subao.resutils.WeakReferenceHandler;

public class FragmentDebug07 extends FragmentDebug {

//	private EditText editGameCount;
//	private TextView textShowTime;
	private Button buttonLaunch;
	private final EditText[] editReconnectTimeDelayValueList = new EditText[3];
	private final EditText[] editReconnectTimeDelayTrueValueList = new EditText[3];
    private final EditText[] editDoubelAccelDetail = new EditText[2];

	@Override
	protected int getRootLayoutResId() {
		return R.layout.fragment_debug_07;
	}

	@Override
	protected void initView(View root) {
//		editGameCount = (EditText) root.findViewById(R.id.debug_text_game_count);
//		textShowTime = (TextView) root.findViewById(R.id.debug_text_show_time);
		//
		editReconnectTimeDelayValueList[0] = (EditText) root.findViewById(R.id.reconnect_time_delay_value);
		editReconnectTimeDelayValueList[1] = (EditText) root.findViewById(R.id.reconnect_time_delay_value2);
		editReconnectTimeDelayValueList[2] = (EditText) root.findViewById(R.id.reconnect_time_delay_value3);
		editReconnectTimeDelayTrueValueList[0] = (EditText) root.findViewById(R.id.reconnect_time_delay_true_value);
		editReconnectTimeDelayTrueValueList[1] = (EditText) root.findViewById(R.id.reconnect_time_delay_true_value2);
		editReconnectTimeDelayTrueValueList[2] = (EditText) root.findViewById(R.id.reconnect_time_delay_true_value3);

        editDoubelAccelDetail[0] = (EditText) root.findViewById(R.id.edit_double_accel_id);
        editDoubelAccelDetail[1] = (EditText) root.findViewById(R.id.edit_double_accel_flow);
		//
		View.OnClickListener viewClick = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				/* 有问题，暂时注掉，以后来改 (By YHB 2016.3.10.)
				case R.id.debug_button_calculate_time:
					try {
						String string = editGameCount.getText().toString();
						int count = Integer.parseInt(string);
						long startTime = SystemClock.elapsedRealtime();
						matchGames(count);
						long endTime = SystemClock.elapsedRealtime();
						long spentTime = endTime - startTime;
						textShowTime.setText(String.valueOf(spentTime) + "ms");
					} catch (NumberFormatException e) {
						UIUtils.showToast("请设置合适的时间，只接受数字");
					}
					break;
				*/
				case R.id.reconnect_time_delay_button:
					setReconnectDelay(0);
					break;
				case R.id.reconnect_time_delay_button2:
					setReconnectDelay(1);
					break;
				case R.id.reconnect_time_delay_button3:
					setReconnectDelay(2);
					break;
				case R.id.reconnect_time_delay_launch:
					launchGame();
					break;
                    case R.id.btn_double_accel:
                        setDoubleAccelDetail();
                        break;
                }

			}
		};
		//
//		root.findViewById(R.id.debug_button_calculate_time).setOnClickListener(viewClick);
		root.findViewById(R.id.reconnect_time_delay_button).setOnClickListener(viewClick);
		root.findViewById(R.id.reconnect_time_delay_button2).setOnClickListener(viewClick);
		root.findViewById(R.id.reconnect_time_delay_button3).setOnClickListener(viewClick);
		buttonLaunch = (Button) root.findViewById(R.id.reconnect_time_delay_launch);
		buttonLaunch.setOnClickListener(viewClick);

        root.findViewById(R.id.btn_double_accel).setOnClickListener(viewClick);

        initDoubelAccelSwicth(root);
	}

    private void initDoubelAccelSwicth(View root) {
        CheckBox checkBox = (CheckBox) root.findViewById(R.id.check_model);
        boolean debugDoueblAcellSwitch = ConfigManager.getInstance().getDebugDoubleAccelSwitch();
        checkBox.setChecked(debugDoueblAcellSwitch);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ConfigManager.getInstance().setDebugDoubleAccelSwitch(isChecked);
            }
        });
    }

    private void setDoubleAccelDetail() {
        String strUid = editDoubelAccelDetail[0].getText().toString().trim();
        String strFlow = editDoubelAccelDetail[1].getText().toString().trim();

        try {
            int uid = Integer.parseInt(strUid);
            float flow = Float.valueOf(strFlow);
            if(uid != 0 && flow != 0) {
                DoubleLinkUseRecords.getInstance().createRecords(uid, "tcp", (long)(flow * 1024 * 1024), 50);
            }
        } catch (NumberFormatException e) {

        }
    }

    private void launchGame() {
		Object uid = buttonLaunch.getTag();
		if (uid == null || !(uid instanceof Integer)) {
			return;
		}
		GameInfo gameInfo = GameManager.getInstance().getGameInfoByUID((Integer) uid);
		if (gameInfo != null) {
			PackageManager pm = getActivity().getPackageManager();
			Intent intent = pm.getLaunchIntentForPackage(gameInfo.getPackageName());
			getActivity().startActivity(intent);
		}
	}

	private void setReconnectDelay(int index) {
		List<GameInfo> gameList = GameManager.getInstance().getSupportedAndReallyInstalledGames();
		if (gameList == null || gameList.isEmpty()) {
			return;
		}
		int delay;
		try {
			delay = Integer.parseInt(editReconnectTimeDelayValueList[index].getText().toString());
		} catch (NumberFormatException e) {
			UIUtils.showToast("请设置合适的时间，只接受数字");
			return;
		}
		String valueStr = editReconnectTimeDelayTrueValueList[index].getText().toString();
		List<Boolean> list = new ArrayList<Boolean>(valueStr.length());
		for (int i = 0; i < valueStr.length(); i++) {
			if (valueStr.charAt(i) == '0') {
				list.add(false);
			} else {
				list.add(true);
			}
		}
		myHandler.removeMessages(index);
		GameInfo gameInfo = gameList.get(0);
		int uid = gameInfo.getUid();
		myHandler.sendMessageDelayed(myHandler.obtainMessage(index, uid, 0, list), delay * 1000);
		buttonLaunch.setTag(uid);
		UIUtils.showToast(String.format("%d秒之后断线重连，游戏是：%s", delay, gameInfo.getAppLabel()));
	}

	HashMap<String, GameInfo> games = new HashMap<String, GameInfo>();

	/*
	private boolean matchGames(int count) {
		BaseDataManager bdManager = BaseDataManager.getInstance();
		if (bdManager.getGameCount() <= 0) {
			return false;
		}

		// 获取已安装，并支持的游戏，放到curGames列表中
		InstalledAppInfo[] instList = this.getInstalledApps(count);
		List<GameInfo> curGames = new ArrayList<GameInfo>();
		for (InstalledAppInfo inst : instList) {
			SupportGameInfo si = bdManager.findSupportGameInfo(inst);
			if (si == null) {
				continue;
			}
			GameInfo g = new GameInfo(inst.getUid(), inst.getPackageName(), inst.getAppLabel(), si,
				inst.hasSuBaoSDKPermission());
			g.setAppIcon(inst.getAppIcon(AppMain.getContext()));
			curGames.add(g);
		}

		// 跟旧数据合并
		HashMap<String, GameInfo> oldGames = this.games;
		this.games = new HashMap<String, GameInfo>();

		// 不在（安装并支持列表）中的，自然就丢弃了
		for (GameInfo g : curGames) {
			GameInfo o = oldGames.get(g.getPackageName());
			if (o != null) {
				// 如果以前有，沿用以前的扩展数据，更新现有的基础数据
				o.update(g);
				this.games.put(o.getPackageName(), o);
			} else {
				this.games.put(g.getPackageName(), g);
			}
		}
		return true;
	}

	private InstalledAppInfo[] getInstalledApps(int count) {
		InstalledAppInfo[] installedAppInfos = new InstalledAppInfo[count];
		for (int i = 0; i < count; i++) {
			installedAppInfos[i] = new InstalledAppInfo(null, "cn.wsds.gamamaster.test." + String.valueOf(i), false);
		}
		return installedAppInfos;
	}
*/
	private final MyHandler myHandler = new MyHandler(this);

	private static final class MyHandler extends WeakReferenceHandler<FragmentDebug07> {
		private final int[] countList = new int[] {
			1,
			1,
			1
		};

		public MyHandler(FragmentDebug07 ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(FragmentDebug07 ref, Message msg) {
			int index = msg.what;
			if (index < 0 || index >= countList.length) {
				return;
			}
			@SuppressWarnings("unchecked")
			List<Boolean> list = (List<Boolean>) msg.obj;
			int uid = msg.arg1;
			int count = countList[index];
			TriggerManager.getInstance().raiseReconnectResult(new ReconnectResult(uid, 1, count, list.get(count - 1)));
			if (count < list.size()) {
				count++;
				sendEmptyMessageDelayed(index, (index == 0) ? 12000 : 8000);
			} else {
				count = 1;
			}
			countList[index] = count;
		}

	}
}
