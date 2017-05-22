package cn.wsds.gamemaster.ui;

import android.app.Activity;
import android.app.Dialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.statistic.NetSwitcherStatistic;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.LaunchGameProgress.OnFinishedListener;
import cn.wsds.gamemaster.ui.user.UserTaskManager;

public class StartNodeDetectUI extends Dialog {

	private static final Boolean LOG = false;
	private static final String TAG = "StartNodeDetectUI";

	private static StartNodeDetectUI instance;

	private final GameInfo gameInfo;
	private EventObserver eventObserver;
	private LaunchGameProgress launchGameProgress;
	private final ViewFlipper viewFlipper;

	//	private static final String ERROR_MESS_2G = "2G网络自身延迟较高，建议更换网络。是否继续启动游戏？";
	//	private static final String ERROR_MESS_NET_BREAK = "网络连接已断开，是否继续启动游戏？";
	
	
	public interface Owner {
		Activity getActivity();
		boolean isCurrentNetworkOk();
	}

	private final Owner owner;

	private enum ErrorType {
		/** 断网 */
		netBreak("断网", "网络连接已断开，是否继续启动游戏？"),
		/** 2G */
		net2G("2G失败", "2G网络自身延迟较高，建议更换网络。是否继续启动游戏？"),
		/** TCP测速失败 */
		tcp("TCP测速失败", "当前网络异常，是否继续启动游戏？");
		public final String label;
		public final String desc;

		ErrorType(String label, String desc) {
			this.label = label;
			this.desc = desc;
		}
	}

	private class MyEventObserver extends EventObserver {
		@Override
		public void onNodeDetectResult(int code, int uid, boolean succeed) {
			if (uid != gameInfo.getUid()) {
				return;
			}
			launchGameProgress.endImmediately();
			if (succeed) {
				if (LOG) {
					Log.d(TAG, "节点优选成功");
				}
			} else {

				if (LOG) {
					Log.d(TAG, "节点优选失败");
				}
				//				long value = TcpNetDelay.getValue();
				//				if(value>0 && value<GlobalDefines.NET_DELAY_TIMEOUT){
				//					return;
				//				}
				//				String mess = getErrorMess();
				//				if(mess == null){
				//					mess = "当前网络异常，是否继续启动游戏？";
				//				}
				//				showErrorMessage(mess);
			}
		}

	};

	private ErrorType getNetErrorType() {
		if (NetManager.getInstance().isDisconnected()) {
			return ErrorType.netBreak;
		}
		boolean net2G = NetTypeDetector.NetType.MOBILE_2G == NetManager.getInstance().getCurrentNetworkType();
		if (net2G) {
			return ErrorType.net2G;
		}
		return null;
	};

	/**
	 * 显示一个“开始检测节点”的UI
	 * 
	 * @param owner
	 *            {@link Owner}
	 * @param gameInfo
	 *            哪个游戏？
	 * @param onDismissListener
	 *            监听界面关闭
	 * @return 返回true表示成功创建，false表示创建失败或者已有一个实例存在了
	 */
	public static boolean createInstanceIfNotExists(Owner owner, GameInfo gameInfo, OnDismissListener onDismissListener) {
		if (owner == null) {
			throw new NullPointerException();
		}
		Activity activity = owner.getActivity();
		if (activity == null || activity.isFinishing()) {
			return false;
		}
        // 在启动游戏的时候，增加一条当前网络开关的统计
        NetSwitcherStatistic.execute(activity, NetManager.getInstance());
        //
		if (instance == null) {
			instance = new StartNodeDetectUI(owner, gameInfo);
			try {
				instance.show();
				instance.setOnDismissListener(onDismissListener);
				return true;
			} catch (RuntimeException e) {
				// 在个别机型上，偶发WindowManager.BadTokenException异常
				// 原因一直查不出来，所以只好Catch一下了
				instance = null;
			}
		}
		return false;
	}

	public static void destroyInstance(Activity owner) {
		if (instance != null) {
			if (instance.owner == owner) {
				instance.dismiss();
				instance = null;
			}
		}
	}

	private void showErrorMessage(String mess) {
		boolean isErrorMessageShowing = isErrorMessageShowing();
		if (isErrorMessageShowing) {
			return;
		}
		setCancelable(true);
		viewFlipper.addView(View.inflate(getContext(), R.layout.dialog_launchgame_error, null));
		viewFlipper.showNext();

		TextView textMess = (TextView) findViewById(R.id.text_mess);
		textMess.setText(mess);
		Button buttonContinue = (Button) findViewById(R.id.button_continue);
		buttonContinue.setText("继续启动");
		Button buttonNotStart = (Button) findViewById(R.id.button_notstart);
		buttonNotStart.setText("暂不启动");
		View.OnClickListener onClickListener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String eventParam;
				switch (v.getId()) {
				case R.id.button_continue:
					eventParam = "继续启动游戏";
					launchGame();
					break;
				case R.id.button_notstart:
					eventParam = "暂不启动游戏";
					break;
				default:
					return;
				}
				Statistic.addEvent(getContext(), Statistic.Event.ACC_GAME_CLICK_START_FAIL, eventParam);
				dismiss();
			}
		};
		buttonContinue.setOnClickListener(onClickListener);
		buttonNotStart.setOnClickListener(onClickListener);
	}

	private boolean isErrorMessageShowing() {
		return R.id.dialog_error_group == viewFlipper.getCurrentView().getId();
	}

	private StartNodeDetectUI(Owner owner, GameInfo gameInfo) {
		super(owner.getActivity(), R.style.AppDialogTheme);
		this.owner = owner;
		this.gameInfo = gameInfo;
		this.setContentView(R.layout.dialog_launch_game);
		viewFlipper = (ViewFlipper) (findViewById(R.id.launch_game_group));

		Window dialogWindow = getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		DisplayMetrics displayMetrics = owner.getActivity().getResources().getDisplayMetrics();
		int width = displayMetrics.widthPixels > displayMetrics.heightPixels ? displayMetrics.heightPixels
			: displayMetrics.widthPixels;
		lp.width = (int) (width * 0.9);
		dialogWindow.setGravity(Gravity.CENTER);
		dialogWindow.setAttributes(lp);

		setCanceledOnTouchOutside(false);
		ErrorType errorType = getNetErrorType();
		if (null != errorType) {
			Statistic.addEvent(getContext(), Statistic.Event.ACC_GAME_CLICK_START_FAIL_REASON, errorType.label);
			showErrorMessage(errorType.desc);
		} else {
			setCancelable(false);
			launchGameProgress = createLaunchGameProgress(gameInfo);
			startGameProgressBar(gameInfo);
		}
	}

	private void startGameProgressBar(final GameInfo gameInfo) {
		//VPNManager.getInstance().isNodeAlreadyDetected(gameInfo.getUid());
		if (VPNUtils.isNodeAlreadyDetected(gameInfo.getUid(), TAG)) {
			if (LOG) {
				Log.d(TAG, "已有最优节点");
			}
			launchGameProgress.startFastModel();
		} else {
			if (LOG) {
				Log.d(TAG, "需进行节点优选");
			}
			if (eventObserver == null) {
				eventObserver = new MyEventObserver();
				TriggerManager.getInstance().addObserver(eventObserver);
			}
			//VPNManager.getInstance().startNodeDetect(gameInfo.getUid(), true); // 开始找节点
			VPNUtils.startNodeDetect(gameInfo.getUid(), true, TAG);
			launchGameProgress.startNormalModel();
		}
	}

	private LaunchGameProgress createLaunchGameProgress(GameInfo gameInfo) {
		TextView textProgress = (TextView) findViewById(R.id.progress_text);
		View title = findViewById(R.id.foreign_server_progress_title);
		boolean foreignGame = gameInfo.isForeignGame();
		if (foreignGame) {
			textProgress.setBackgroundResource(R.drawable.foreign_server_progress_pointer);
			title.setVisibility(View.VISIBLE);
		} else {
			textProgress.setBackgroundResource(R.drawable.foreign_server_progress_pointer_ordinary);
			title.setVisibility(View.GONE);
		}
		return new LaunchGameProgress((ImageView) findViewById(R.id.image_progress),
			textProgress, createProgressFinsishedListener(owner, gameInfo));
	}

	private OnFinishedListener createProgressFinsishedListener(final Owner owner, final GameInfo gameInfo) {
		return new OnFinishedListener() {

			@Override
			public void onFinished(boolean isTimeout) {
				
				//VPNManager.getInstance().isNodeAlreadyDetected(gameInfo.getUid())			
				if (VPNUtils.isNodeAlreadyDetected(gameInfo.getUid(), TAG)) {
					nodeDetectSuccessed();
				} else {
					if (owner.isCurrentNetworkOk()) {
						nodeDetectSuccessed();
					} else {
						ErrorType errorType = getNetErrorType();
						if (null == errorType) {
							errorType = ErrorType.tcp;
						}
						showErrorMessage(errorType.desc);
						Statistic.addEvent(getContext(), Statistic.Event.ACC_GAME_CLICK_START_FAIL_REASON, errorType.label);
					}
				}
			}

			private void nodeDetectSuccessed() {
				((TextView) findViewById(R.id.launch_progress_desc)).setText("正在启动游戏");
				if (!isErrorMessageShowing()) {
					launchGame();
					try {
						dismiss();
					} catch (Exception e) {
						//						e.printStackTrace();
					}
				}
			}
		};
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (eventObserver != null) {
			TriggerManager.getInstance().deleteObserver(eventObserver);
			eventObserver = null;
		}
		if (instance == this) {
			instance = null;
		}
	}

	private void launchGame() {
		UserTaskManager.reportStartGameInside(gameInfo.getPackageName(), gameInfo.getAppLabel());
		GameManager.getInstance().launchGame(getContext(), gameInfo);
	}

	public static boolean doesInstanceExists() {
		return null != instance;
	}

}
