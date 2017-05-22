package cn.wsds.gamemaster.ui.floatwindow;

import android.os.Message;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.UIUtils;

import com.subao.resutils.WeakReferenceHandler;

public class ReconnectObserver extends EventObserver {
	
	private static final int TIMEOUT_FOR_DESTROY_FLOATWND = 40 * 1000;

	private final MyHandler myHandler;

	public static final ReconnectObserver instance = new ReconnectObserver();
	
	private int uidEffect;			// 当前正在显示特效是哪一个游戏？
	private int taskIdEffect;		// 当前正在显示特效是游戏的哪一个连接？
	
	private int uidCurrentForegroundGame;	// 当前处于前台的游戏的UID

	private ReconnectObserver() {
		myHandler = new MyHandler(this);
	}

	@Override
	public void onReconnectResult(ReconnectResult result) {
		// 事件发生的游戏，是当前前台游戏吗？如果不是就直接返回
		if (uidCurrentForegroundGame != result.uid) {
			return;
		}
		
		// 如果当前正在显示某个游戏的特效，判断本次事件是不是这个游戏
		// 并且是不是正在显示的连接
		if (this.uidEffect >= 0) {
			if (result.uid != this.uidEffect) { return; }
			if (result.taskId != this.taskIdEffect) { return; }
		} else if (result.count == 1) {
			this.uidEffect = result.uid;
			this.taskIdEffect = result.taskId;
		}
		
		// 提交统计数据
		if (result.success || result.count >= GlobalDefines.MAX_COUNT_OF_CONNECTION_REPAIR) {
			Statistic.addEvent(AppMain.getContext(), Statistic.Event.NETWORK_REPAIR_CONNECTION_EFFECT_RESULT,
				String.format("%d,%s", result.count, result.success ? "成功" : "失败"));
		}
		
		// 特效已存在，说明“开始”动效已做过，直接Update
		if (FloatWindowInReconnect.exists()) {
			FloatWindowInReconnect.changeCurrentData(result.count, result.success);
			return;
		}
		
		// 特效不存在，如果不是第一次，而且成功，显示Toast
		if (result.count > 1 && result.success) {
			UIUtils.showToast(String.format("%s: 断线重连成功", AppMain.getContext().getString(R.string.app_name)));
			return;
		}
		
		// 如果是第一次，才显示特效
		if (result.count != 1) {
			return;
		}
//		RecentState recentState = RecentNetState.instance.getRecentState();
//		StatisticUtils.statisticConnectionCause(recentState);
		String desc = "网络异常";//FIXME 文本确认
		FloatWindowInReconnect wnd = FloatWindowInReconnect.createInstance(AppMain.getContext(), desc);
		wnd.setOnDestroyListener(new FloatWindow.OnDestroyListener() {
			@Override
			public void onFloatWindowDestroy(FloatWindow who) {
				myHandler.removeCallbacksAndMessages(null);
				uidEffect = taskIdEffect = -1;
			}
		});
		myHandler.removeCallbacksAndMessages(null);
		myHandler.sendEmptyMessageDelayed(MyHandler.MSG_DESTROY, TIMEOUT_FOR_DESTROY_FLOATWND);
		
		
		if (result.success) {
			// 刚创建就是“成功”状态，延时8秒
			myHandler.postDelayed(new DelayedSucceed(uidEffect, taskIdEffect), 8000);
		} else {
			// 刚创建是“失败”状态，可以Update
			FloatWindowInReconnect.changeCurrentData(result.count, result.success);
		}
	
	}
	
	private class DelayedSucceed implements Runnable {
		private final int uidEffect, taskIdEffect;

		public DelayedSucceed(int uidEffect, int taskIdEffect) {
			this.uidEffect = uidEffect;
			this.taskIdEffect = taskIdEffect;
		}
		
		@Override
		public void run() {
			if (this.uidEffect == ReconnectObserver.this.uidEffect && this.taskIdEffect == ReconnectObserver.this.taskIdEffect) {
				FloatWindowInReconnect.changeCurrentData(1, true);
			}
		}
	}

	@Override
	public void onTopTaskChange(GameInfo info) {
		int uid = info == null ? -1 : info.getUid();
		if (this.uidCurrentForegroundGame != uid) {
			this.uidCurrentForegroundGame = uid;
			this.uidEffect = this.taskIdEffect = -1;	// 前台应用切换过了，特效消失
			FloatWindowInReconnect.destroyInstance();
		}
	}

//	private void showToast(boolean success) {
//		String string = success ? "断线重连成功" : "断线重连失败";
//		Toast.makeText(AppMain.getContext(), string, Toast.LENGTH_LONG).show();
//	}

	private static final class MyHandler extends WeakReferenceHandler<ReconnectObserver> {
		private static final int MSG_DESTROY = 0;

		public MyHandler(ReconnectObserver ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(ReconnectObserver ref, Message msg) {
			switch (msg.what) {
			case MSG_DESTROY:
				FloatWindowInReconnect.changeCurrentData(GlobalDefines.MAX_COUNT_OF_CONNECTION_REPAIR, false);
				break;
			default:
				break;
			}
		}
	}


//	private class MyOnVisibilityChangedListener implements OnVisibilityChangedListener {
//
//		@Override
//		public void visibilityChanged(boolean visible) {
//			myHandler.removeCallbacksAndMessages(null);
//			if (visible) {
//				myHandler.sendEmptyMessageDelayed(MyHandler.MSG_DESTROY, TIMEOUT);
//			} else {
//				uidEffect = taskIdEffect = -1;
//			}
//		}
//	}

}
