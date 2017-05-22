package cn.wsds.gamemaster.ui.floatwindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.os.SystemClock;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

import com.subao.common.utils.CalendarUtils;
import com.subao.net.NetManager;

public class ToastAccel {

//	private static final boolean LOG = false;
//	private static final String TAG = "ToastAccel";

	private static class Info {
		public long lastToastTime;			// 上一次Toast的时间
		public boolean alwaysForeground;	// 自上次Toast以来，一直在前台

		public Info(long lastToastTime, boolean alwaysForeground) {
			this.lastToastTime = lastToastTime;
			this.alwaysForeground = alwaysForeground;
		}
	}

	private static final long MIN_DELTE_TIME_TOAST = 3600 * 1000;

	private static HashMap<String, Info> infoList = new HashMap<String, Info>();
	private static MyObserver eventObserver;
	private static ToastEx.Strategy strategy;

	private static class MyObserver extends EventObserver {

		private String topTaskPackageName;
		private int uidOfTopTask = -1;

		@Override
		public void onTopTaskChange(GameInfo gameInfo) {
			if(NetManager.getInstance().isDisconnected()){
				return ;
			}
			
			if (gameInfo == null) {
				topTaskPackageName = null;
				uidOfTopTask = -1;
				// 顶层不是游戏，所有游戏的“一直在前台”字段置False
				for (Info info : infoList.values()) {
					info.alwaysForeground = false;
				}
			} else {
				topTaskPackageName = gameInfo.getPackageName();
				uidOfTopTask = gameInfo.getUid();
				// 顶层是游戏，其它游戏的“一直在前台”字段置False
				for (Entry<String, Info> e : infoList.entrySet()) {
					if (!topTaskPackageName.equals(e.getKey())) {
						e.getValue().alwaysForeground = false;
					}
				}
				//
				if (AccelOpenManager.isStarted()) {
					showToastWhenAccelEnabled(gameInfo);
					Statistic.addEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_GAME_ON_TOP,
						gameInfo.getAppLabel());
				} else {
					showToastWhenAccelDisabled();
				}
			}
		}

		private void showToastWhenAccelEnabled(final GameInfo gameInfo) {
			MainHandler.getInstance().postDelayed(new Runnable() {
				@Override
				public void run() {
					if (gameInfo.getUid() == uidOfTopTask) {
						beginShowAccelToast(gameInfo);
					}
				}
			}, 2000);

		}

		private void showToastWhenAccelDisabled() {
			int today = CalendarUtils.todayLocal();
			if (today != ConfigManager.getInstance().getLastDayToastGameWhenVpnClosed()) {
				ConfigManager.getInstance().setLastDayToastGameWhenVpnClosed(today);
				Context context = AppMain.getContext();
				UIUtils.showToast(context.getString(R.string.app_name) + "已经准备好为您加速");
			}
		}

		private void beginShowAccelToast(final GameInfo gameInfo) {
			if (gameInfo == null) {
				return;
			}
			// 正在显示Toast，返回
			if (strategy != null) {
				return;
			}
			// 服务未开，返回
			if (!AccelOpenManager.isStarted()) {
				return;
			}
			// 游戏不在前台
			if (topTaskPackageName == null) {
				return;
			}
			// 检测的不是前台游戏
			if (!topTaskPackageName.equals(gameInfo.getPackageName())) {
				return;
			}

			// 找Info
			long now = SystemClock.elapsedRealtime();
			Info info = infoList.get(gameInfo.getPackageName());
			if (info != null) {
				// 自上次提示以来，游戏一直在前台，返回
				if (info.alwaysForeground) {
					return;
				}
				// 距上一次提示不足1小时，返回
				if (info.lastToastTime >= 0 && now - info.lastToastTime < MIN_DELTE_TIME_TOAST) {
					return;
				}
				info.lastToastTime = now;
			} else {
				info = new Info(now, true);
				infoList.put(gameInfo.getPackageName(), info);
			}
			// 显示Toast
//			StatisticDefault.addEvent(AppMain.getContext(), StatisticDefault.Event.MOTION_GAME_START); // 从1.4.2起，此事件只计数了（以前参数是游戏名）
//			SupportGameInfo sgi = gameInfo.getInfo();
			List<String> textList = new ArrayList<String>(3);
			textList.add("正在加速……");
			if (gameInfo.isForeignGame()) {
				textList.add("使用海外专用专点");
			}
			textList.add(String.format("预计降低延迟%d%%", 45 + Math.round(Math.random() * (80 - 45))));
			strategy = new ToastEx.FixedTimeStrategy(textList,ToastEx.Effect.SUCCEED);
			ToastEx.show(AppMain.getContext(), strategy, new ToastEx.OnToastExOverListener() {
				@Override
				public void onToastExOver(int id) {
					strategy = null;
				}
			},true);
		}

		@Override
		public void onVPNClose() {
			infoList.clear();
		}

		@Override
		public void onAccelSwitchChanged(boolean state) {
			if (!state) {
				infoList.clear();
			}
		}
	}

	public static void init() {
		if (eventObserver == null) {
			eventObserver = new MyObserver();
			TriggerManager.getInstance().addObserver(eventObserver);
		}
	}

	private ToastAccel() {}
}
