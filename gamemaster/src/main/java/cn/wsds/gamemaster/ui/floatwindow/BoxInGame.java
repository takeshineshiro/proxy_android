package cn.wsds.gamemaster.ui.floatwindow;

import java.util.Observable;
import java.util.Observer;

import com.subao.common.data.ParallelConfigDownloader;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.ThreadUtils;
import com.subao.net.NetManager;
import com.subao.utils.MetricsUtils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.drawable.ClipDrawable;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.FloatWindowMeasure;
import cn.wsds.gamemaster.dialog.CommonDesktopDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager.Checker.Result;
import cn.wsds.gamemaster.netdelay.NetDelayDataManager;
import cn.wsds.gamemaster.screenshot.ScreenshotManager;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.Statistic.Event;
import cn.wsds.gamemaster.statistic.StatisticUtils;
import cn.wsds.gamemaster.tools.FaultProcessor;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.ActivityFloatwindowOpenAccel;
import cn.wsds.gamemaster.ui.NetDelayChart;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenWorker;
import cn.wsds.gamemaster.ui.accel.AccelOpener;
import cn.wsds.gamemaster.ui.accel.NoticeOpenGameInside;
import cn.wsds.gamemaster.ui.accel.VpnOpener;
import cn.wsds.gamemaster.ui.doublelink.ActivityDoubleLink;
import cn.wsds.gamemaster.ui.floatwindow.skin.SkinResourceBoxInGame;
import cn.wsds.gamemaster.ui.floatwindow.skin.SkinResourceBoxInGameNormal;
import cn.wsds.gamemaster.ui.view.LoadingRing;
import cn.wsds.gamemaster.ui.view.LoadingRing.OnCompleteListener;
import cn.wsds.gamemaster.ui.view.SubaoProgressBar;
import cn.wsds.gamemaster.ui.view.Switch;

@SuppressLint("RtlHardcoded")
public class BoxInGame extends FloatWindow {

	//	private static final boolean LOG = false;
	//	private static final String TAG = "BoxInGame";

	private static final String UNKNOWN_NETWORK_PROBLEM = "未知网络异常";
	private static final String TAG = "BoxInGame" ;

	private static void checkMainThread() {
		if (GlobalDefines.CHECK_MAIN_THREAD) {
			if (!ThreadUtils.isInAndroidUIThread()) {
				MainHandler.getInstance().showDebugMessage("PopupWindowInGame. must be called in main thread");
			}
		}
	}

	private static BoxInGame instance;

	/** 出生时间，用于统计大悬浮窗生存时长 */
	private final long birthTime = SystemClock.elapsedRealtime();

	/** {@link GameInfo} 哪个游戏？ */
	private final GameInfo gameInfo;

	private final boolean anmiationEnter;
	private final SkinResourceBoxInGame currentSkinResource;

	/** 当前页 */
	private Page currentPage;

	private CommonDialog dialog;

	//------------- UI 控件 -------------------------

	/** 整个根容器 */
	private View wholeLayout;
	/** 整个主面板 （不含右边按钮的部分） */
	private View mainPanel;

	/** 右边的按钮组 */
	private ImageView buttonCurveRight, buttonSettingsRight, buttonDetailsRight, buttonLogUpload,
		buttonScreenShotRight;

	private ViewGroup containerOfPages;

	/** 开始加速的大按钮 */
	private ImageView buttonOpenVPN;
	/** 网络异常检查用的Loading效果 */
	private LoadingRing loadingRingForNetCheck;
	/** 详情（两段加速）示意控件 */
	private TwoSectionDelayDrawer twoSectionDelayDrawer;
	/** 异常描述文本 */
	private TextView exceptionDesc;

	/** 设置页面：大中小尺寸 */
	private RadioGroup settingRadio;
	//	private ViewGroup parentOfSwitchSkin;
	//	/** 设置页面：开关-使用皮肤 */
	//	private Switch switchSkin;
	//	/** 设置页面：文本-是否显示延迟 */
	//	private View textSwitchShowDelay;
	/** 设置页面：开关-是否显示延迟 */
	private Switch switchDelay;

	/** 日志上传页面：Flipper（含“上传按钮”和“进度条”） */
	private ViewFlipper logUploadFlipper;
	/** 日志上传页面：文字描述 */
	private TextView logUploadText;
	/** 日志上传页面：“上传”按钮 */
	private View logUploadButtonExecute;
	/** 日志上传页面：进度条 */
	private SubaoProgressBar logUploadProgress;

	/** 电池 */
	private ImageView batteryImageView;
	private ClipDrawable batteryClipDrawable;
	/** 电池百分比 */
	private TextView batteryPercent;

	/** 网络延迟曲线图 */
	private NetDelayChart netDelayChart;
	private final Observer netDelayDataObserver = new Observer() {

		@Override
		public void update(Observable observable, Object data) {
			if (netDelayChart != null) {
				netDelayChart.setDataSource((NetDelayDataManager.TimePointContainer) data, true);
			}
		}

	};

	private final EventObserver eventObserver = new EventObserver() {

		@Override
		public void onFirstSegmentNetDelayChange(int delayMilliseconds) {
			currentPage.refreshTitle();
			currentPage.onNetDelayChanged(delayMilliseconds);
		};

		@Override
		public void onAccelSwitchChanged(boolean state) {
			// 加速开或关，直接销毁BoxInGame
			destroyInstance();
		};

		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			currentPage.refreshTitle();
		}
	};

	private BatteryReceiver batteryReceiver;

	/** 右边按钮组里的按钮点击监听 */
	private final View.OnClickListener onButtonClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.box_in_game_button_curve_right:
				currentPage.gotoPageCurve();
				break;

			case R.id.box_in_game_button_details_right:
				currentPage.gotoPageDetails();
				break;

			case R.id.box_in_game_button_setting_right:
				currentPage.gotoPageSettings();
				if (isDisplayBoxInGameSettingRedPoint()) {
					ConfigManager.getInstance().setBoxInGameSettingClicked();
					ConfigManager.getInstance().setBoxInGameSettingDoubleAccelEnter();
					View view = getView();
					if (view != null) {
						view = view.findViewById(R.id.red_point);
						if (view != null) {
							view.setVisibility(View.GONE);
						}
					}
				}
				changeSettingButton();
				break;
			case R.id.box_in_game_button_log_upload:
				currentPage.gotoPageLogUpload();
				break;
//			case R.id.box_in_game_button_screenshot_right:
//				if (android.os.Build.VERSION.SDK_INT >= 21) {
//					Toast.makeText(v.getContext(), "安卓5.0及以上版本暂不支持该功能！", Toast.LENGTH_SHORT).show();
//				} else {
//					currentPage.doScreenShot();
//				}
//				break;
			}

		}
	};

	private boolean isDisplayBoxInGameSettingRedPoint() {
		return !ConfigManager.getInstance().getBoxInGameSettingClicked() ||
			!ConfigManager.getInstance().getBoxInGameSettingDoubleAccelEntered();
	}

	/**
	 * 创建窗口实例
	 * 
	 * @param context
	 *            {@link Context}
	 * @param xAnchor
	 *            锚点（小悬浮窗的中心点）的X坐标
	 * @param yAnchor
	 *            锚点（小悬浮窗的中心点）的Y坐标
	 * @param gameInfo
	 *            {@link GameInfo} 不能为null
	 * @return {@link BoxInGame}实例
	 * @see {{@link #createInstance(Context, int, int, GameInfo, boolean)}
	 */
	//	public static BoxInGame createInstance(Context context, int xAnchor, int yAnchor, GameInfo gameInfo) {
	//		return createInstance(context, xAnchor, yAnchor, gameInfo, false);
	//	}

	/**
	 * 创建窗口实例
	 * 
	 * @param context
	 *            {@link Context}
	 * @param xAnchor
	 *            锚点（小悬浮窗的中心点）的X坐标
	 * @param yAnchor
	 *            锚点（小悬浮窗的中心点）的Y坐标
	 * @param gameInfo
	 *            {@link GameInfo} 不能为null
	 * @param anmiation
	 *            显示过程是否使用动画？
	 * @return {@link BoxInGame}实例
	 */
	public static BoxInGame createInstance(Context context, int xAnchor, int yAnchor, GameInfo gameInfo,
		boolean anmiation) {
		//
		checkMainThread();
		if (gameInfo != null && instance == null) {
			instance = new BoxInGame(context, xAnchor, yAnchor, gameInfo, anmiation);
			View view = LayoutInflater.from(context).inflate(instance.currentSkinResource.getLayoutResource(), null);
			instance.addView(Type.DIALOG, view, xAnchor, yAnchor);
		}
		return instance;
	}

	public static void destroyInstance() {
		checkMainThread();
		if (instance != null) {
			instance.doStatisticLiveTime();
			TriggerManager.getInstance().deleteObserver(instance.eventObserver);
			BoxInGame box = instance;
			instance = null;
			box.destroy();
			FloatWindowInGame.setInstanceStrikingOn(true);
		}
	}

	public static boolean exists() {
		return instance != null;
	}

	/////////////////////////////////////////////////

	/**
	 * 窗口内容分为几页，同一时刻只显示其中一页
	 * <p>
	 * Page是页面的抽象基类
	 * </p>
	 */
	private abstract class Page {
		/** 在几组页面里的第几页？ */
		public abstract int getPageIndex();

		/** 当本Page显示时，进行一些初始化操作 */
		public abstract void init();

		/** 当本Page离开时，进行一些清理操作 */
		public void cleanup() {}

		/** 本页应该让哪个按钮高亮？ */
		public abstract HighlightButton getHighlightButton();

		/** 当延迟值发生改变时被调用 */
		public void onNetDelayChanged(int valueFirstSegment) {}

		/** 转到详情页 */
		public void gotoPageDetails() {
			if (AccelOpenManager.isStarted()) {
				changePage(new PageTwoSectionDelay());
			} else {
				changePage(new PageAccelStopped(HighlightButton.DETAILS));
			}
		}

		/** 转到设置页 */
		public void gotoPageSettings() {
			changePage(new PageSettings());
		}

		/** 转到曲线图页 */
		public void gotoPageCurve() {
			if (AccelOpenManager.isStarted()) {
				changePage(new PageChart());
			} else {
				changePage(new PageAccelStopped(HighlightButton.CURVE));
			}
		}

		/** 转到日志上传页 */
		public void gotoPageLogUpload() {
			if (AccelOpenManager.isStarted()) {
				changePage(new PageLogUpload());
			} else {
				changePage(new PageAccelStopped(HighlightButton.LOG_UPLOAD));
			}
		}

		/** 截图 */
		public final void doScreenShot() {
			ScreenshotManager.onScreenShot();
		}

		public final void refreshTitle() {
			int firstDelay = GameManager.getInstance().getFirstSegmentNetDelay();
			int secondDelay = GameManager.getInstance().getSecondSegmentNetDelay(gameInfo.getUid()).getDelayValue();
			if (firstDelay >= GlobalDefines.NET_DELAY_TIMEOUT) {
				if (secondDelay == GlobalDefines.NET_DELAY_TEST_WAIT
					|| (secondDelay >= 0 && secondDelay < GlobalDefines.NET_DELAY_TIMEOUT)) {
					hideBoxTitle();
					return;
				}
			}
			if (firstDelay >= 0 && firstDelay < GlobalDefines.NET_DELAY_TIMEOUT) {
				if (secondDelay == GlobalDefines.NET_DELAY_TEST_FAILED) {
					setBoxTitle("“加速网络”异常", true);
					return;
				}
			}
			// 如果有异常，显示异常信息
			if (FloatWindowCommon.isNetworkException()) {
				setBoxTitle(getNetExceptionDesc(networkCheckResult), true);
				return;
			}
			if (!AccelOpenManager.isStarted()) {
				// 没开加速
				hideBoxTitle();
				return;
			}
			if (GameManager.getInstance().getFirstSegmentNetDelay() == GlobalDefines.NET_DELAY_TEST_WAIT
				&& !FloatWindowCommon.isCurrent2G()) {
				setBoxTitle("等待测速中，请稍候", false);
				return;
			}
			hideBoxTitle();
		}

		private void hideBoxTitle() {
			UIUtils.setViewVisibility(exceptionDesc, View.INVISIBLE);
		}

		private void setBoxTitle(CharSequence text, boolean isError) {
			UIUtils.setViewVisibility(exceptionDesc, View.VISIBLE);
			UIUtils.setViewText(exceptionDesc, text);
			UIUtils.setViewTextColor(exceptionDesc, isError ? colorTextError : colorTextNormal);
		}

	}

	private final ColorStateList colorTextError, colorTextNormal;

	public static final class FloatwindowAccelOpenWorker implements AccelOpenWorker {

		@Override
		public void openAccel() {
			if (instance == null) {
				return;
			}

			DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (instance == null) {
						return;
					}
					Context context = instance.getContext();
					String eventValue;
					if (DialogInterface.BUTTON_POSITIVE == which) {
						eventValue = "开启";
						onStart(context);
					} else {
						eventValue = "放弃";
					}
					Statistic.addEvent(context, Statistic.Event.FLOATING_WINDOW_ACC_DIALOG_START, eventValue);
				}

				private void onStart(Context context) {
					if (instance == null) {
						return;
					}

					if (AccelOpenManager.isStarted()) {
						instance.changePage(instance.new PageTwoSectionDelay());
					} else {
						instance.clickVpn = 1;
						destroyInstance();
						ActivityFloatwindowOpenAccel.startAccel(context);
					}
					UIUtils.showToast("建议重新启动游戏获得最佳加速效果~");
				}
			};
			instance.destroyDialog();
			CommonDesktopDialog dialog = new CommonDesktopDialog();
			dialog.setMessage("注意：游戏中开启加速会中断当前游戏");
			dialog.setPositiveButton("开启", dialogListener);
			dialog.setNegativeButton("取消", dialogListener);
			dialog.show();
			instance.dialog = dialog;
		}
	}


	/** VPN关闭状态时显示的页面 */
	private class PageAccelStopped extends Page {

		private HighlightButton highlightButton;

		private PageAccelStopped(HighlightButton highlightButton) {
			this.highlightButton = highlightButton;
		}

		@Override
		public int getPageIndex() {
			return 4;
		}

		@Override
		public void init() {
			// 开启加速按钮
			if (buttonOpenVPN == null) {
				buttonOpenVPN = (ImageView) containerOfPages.findViewById(R.id.box_in_game_button_open_vpn);
				buttonOpenVPN.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						new FloatwindowAccelOpenWorker().openAccel();
					}
				});
			}
		}

		private void setHighlightButton(HighlightButton btn) {
			if (highlightButton != btn) {
				highlightButton(highlightButton, false);
				highlightButton = btn;
				highlightButton(highlightButton, true);
			}
		}

		@Override
		public void gotoPageDetails() {
			setHighlightButton(HighlightButton.DETAILS);
		}

		@Override
		public void gotoPageCurve() {
			setHighlightButton(HighlightButton.CURVE);
		}

		@Override
		public void gotoPageLogUpload() {
			setHighlightButton(HighlightButton.LOG_UPLOAD);
		}

		@Override
		public HighlightButton getHighlightButton() {
			return highlightButton;
		}

	}

	/** 详情页（两段加速示意图） */
	private class PageTwoSectionDelay extends Page {

		@Override
		public int getPageIndex() {
			return 0;
		}

		@Override
		public void init() {
			if (twoSectionDelayDrawer == null) {
				twoSectionDelayDrawer = new TwoSectionDelayDrawer(
					containerOfPages.findViewById(R.id.two_section_delay), gameInfo, currentSkinResource);
			}
			twoSectionDelayDrawer.start();
		}

		@Override
		public void cleanup() {
			if (twoSectionDelayDrawer != null) {
				twoSectionDelayDrawer.cleanUp();
			}
		}

		@Override
		public void gotoPageDetails() {
			// 已经在详情页了，啥也别做
		}

		@Override
		public HighlightButton getHighlightButton() {
			return HighlightButton.DETAILS;
		}

	}

	/** 设置页，展示设置按钮 */
	private class PageSettings extends Page {

		private Switch switchDoubleLink;

		@Override
		public int getPageIndex() {
			return 2;
		}

		//		private void resetLayoutByHunterSkin() {
		//			if (!FloatWindowCommon.isSupportHunterSkin(gameInfo)) {
		//				if (parentOfSwitchSkin.getVisibility() == View.VISIBLE) {
		//					parentOfSwitchSkin.setVisibility(View.GONE);
		//					// 将左边的开关调整为左文字右开关
		//					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
		//						LayoutParams.WRAP_CONTENT);
		//					lp.addRule(RelativeLayout.RIGHT_OF, textSwitchShowDelay.getId());
		//					lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		//					lp.setMargins(MetricsUtils.dp2px(switchDelay.getContext(), 16), 0, 0, 0);
		//					switchDelay.setLayoutParams(lp);
		//					//
		//					lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		//					lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		//					textSwitchShowDelay.setLayoutParams(lp);
		//				}
		//			}
		//		}

		@Override
		public void init() {
			ViewGroup container = (ViewGroup) containerOfPages.getChildAt(this.getPageIndex());

			//			if (textSwitchShowDelay == null) {
			//				textSwitchShowDelay = container.findViewById(R.id.text_visible_delay);
			//			}
			//
			if (switchDelay == null) {
				switchDelay = (Switch) container.findViewById(R.id.check_visible_delay);
				switchDelay.setOnChangedListener(onSwitchChangeListener);
			}
			switchDelay.setChecked(ConfigManager.getInstance().getFloatwindowSwitchDelay());

			initDoubleAccel(container);
			//
			//			if (parentOfSwitchSkin == null) {
			//				parentOfSwitchSkin = (ViewGroup) container.findViewById(R.id.container_skin);
			//			}
			//			if (switchSkin == null) {
			//				switchSkin = (Switch) container.findViewById(R.id.switch_skin);
			//				switchSkin.setOnChangedListener(onSwitchChangeListener);
			//			}
			//			switchSkin.setChecked(ConfigManager.getInstance().isFloatwindowHunterSkin());
			if (settingRadio == null) {
				settingRadio = (RadioGroup) container.findViewById(R.id.setting_radiogroup);
				switch (FloatWindowMeasure.getCurrentType()) {
				case MINI:
					settingRadio.check(R.id.radio_mini);
					break;
				case LARGE:
					settingRadio.check(R.id.radio_large);
					break;
				default:
					settingRadio.check(R.id.radio_normal);
					break;
				}
				settingRadio.setOnCheckedChangeListener(onRadioCheckedChangeListener);
			}
			//			resetLayoutByHunterSkin();
		}

		private void initDoubleAccel(ViewGroup container) {

			if (ParallelConfigDownloader.isPhoneParallelSupported()) {
				if (switchDoubleLink == null) {
					switchDoubleLink = (Switch) container.findViewById(R.id.check_double_link);
					switchDoubleLink.setOnChangedListener(onSwitchChangeListener);
				}
				switchDoubleLink.setChecked(ConfigManager.getInstance().isEnableDoubleAccel());
			}
		}

		@Override
		public void gotoPageSettings() {
			// 已经在设置页了
		}

		@Override
		public HighlightButton getHighlightButton() {
			return HighlightButton.SETTINGS;
		}
	}

	private Result networkCheckResult;

	/** 网络异常检查页 */
	private class PageCheckingNetwork extends Page {

		private boolean isShowChecking = false;

		@Override
		public int getPageIndex() {
			return 5;
		}

		@Override
		public void init() {
			setAllButtonEnabled(false);
			if (isShowChecking) {
				return;
			}
			isShowChecking = true;
			exceptionDesc.setVisibility(View.VISIBLE);
			exceptionDesc.setText("正在检查网络...");

			if (loadingRingForNetCheck == null) {
				loadingRingForNetCheck = (LoadingRing) containerOfPages.findViewById(R.id.view_checking_status);
			}
			loadingRingForNetCheck.start(new OnCompleteListener() {
				@Override
				public void onComplete() {
					NetworkCheckManager.postStopRequest();
					showNetworkCheckResult();
				}
			});

			NetworkCheckManager.start(BoxInGame.this.getContext(), new NetworkCheckManager.Observer() {
				@Override
				public void onNetworkCheckResult(Result event) {
					networkCheckResult = event;
					NetworkCheckManager.postStopRequest();
					loadingRingForNetCheck.requestStop();
				}
			});
		}

		private void showNetworkCheckResult() {
			if (networkCheckResult != null) {
				String netExceptionDesc = getNetExceptionDesc(networkCheckResult);
				exceptionDesc.setText(netExceptionDesc);
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.NETWORK_FLOATING_OPEN_CHECK, networkCheckResult.description);
			}
			if (!AccelOpenManager.isStarted()) {
				changePage(new PageAccelStopped(HighlightButton.DETAILS));
			} else {
				changePage(new PageTwoSectionDelay());
			}
		}

		@Override
		public void gotoPageSettings() {
			// 不许去
		}

		@Override
		public void gotoPageDetails() {
			// 不许去
		}

		@Override
		public void gotoPageCurve() {
			// 不许去
		}

		@Override
		public void gotoPageLogUpload() {
			// 不许去
		}

		@Override
		public HighlightButton getHighlightButton() {
			return null;
		}

	}

	private String getNetExceptionDesc(Result networkCheckResult) {
		if (null != networkCheckResult) {
			switch (networkCheckResult) {
			case MOBILE_UNAVAILABLE:
			case WIFI_UNAVAILABLE:
				String text =  NetworkCheckManager.getBadNetStateMsg(
						NetManager.getInstance().getCurrentNetworkType(), false);
				
				if(!TextUtils.isEmpty(text)){
					return text ;
				}else{
					return networkCheckResult.description;		
				}						 
			case WIFI_MOBILE_CLOSED:
				return "网络连接已断开";			 
			case OK:
				break;
			default:
				return networkCheckResult.description;
			}
		}
		return UNKNOWN_NETWORK_PROBLEM;
	}

	/**
	 * 曲线图页面
	 */
	private class PageChart extends Page {

		@Override
		public int getPageIndex() {
			return 1;
		}

		@Override
		public void init() {
			if (netDelayChart == null) {
				netDelayChart = (NetDelayChart) containerOfPages.findViewById(R.id.net_delay_chart);
				netDelayChart.setDataSeconds(60);
				netDelayChart.setDataSource(NetDelayDataManager.getInstance().getTimePoints(null), true);
			}
		}

		@Override
		public void gotoPageCurve() {
			// 已在曲线页了
		}

		@Override
		public HighlightButton getHighlightButton() {
			return HighlightButton.CURVE;
		}

	}

	/** 日志上传页 */
	private class PageLogUpload extends Page {

		private final FaultProcessor.Observer observer = new FaultProcessor.Observer() {

			@Override
			public void uploadCompleted(boolean result) {
				if (result) {
					logUploadText.setText("上传成功");
				} else {			 
					logUploadText.setText("网络原因上传失败");					
					setlogUploadTextFaildColor();
				}

				FaultProcessor.getInstance().stop();
			}

			@Override
			public void progressChanged(int progress) {
				logUploadProgress.setPercent(progress);
			}
		};

		@Override
		public int getPageIndex() {
			return 3;
		}

		@Override
		public void init() {
			ViewGroup parent = (ViewGroup) containerOfPages.getChildAt(this.getPageIndex());
			if (logUploadFlipper == null) {
				logUploadFlipper = (ViewFlipper) parent.findViewById(R.id.log_upload_flipper);
			}
			if (logUploadText == null) {
				logUploadText = (TextView) parent.findViewById(R.id.log_upload_text);
			}
			if (logUploadButtonExecute == null) {
				logUploadButtonExecute = parent.findViewById(R.id.log_upload_button_execute);
			}
			logUploadButtonExecute.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(NetManager.getInstance().isDisconnected()){
						logUploadText.setText("网络原因无法上传");		
						logUploadText.setGravity(Gravity.CENTER);
						setlogUploadTextFaildColor();
						return ;
					}
					tryToUpload();
				}
			});
			if (logUploadProgress == null) {
				logUploadProgress = (SubaoProgressBar) parent.findViewById(R.id.log_upload_progress);
			}

			logUploadButtonExecute.setEnabled(true);
			logUploadButtonExecute.setAlpha(1);
			logUploadText.setTextColor(getContext().getResources().getColor(R.color.color_game_6));
			if (FaultProcessor.getInstance().isRunning()) {
				startStyle();
				if (FaultProcessor.getInstance().getProgress() == 100) {
					observer.uploadCompleted(FaultProcessor.getInstance().getResult());
				}

			} else {
				logUploadFlipper.setDisplayedChild(0);
				logUploadText.setText("上传网络日志可以让我们帮助您分析您所遇到的加速问题，提高服务质量。该功能需要消耗少量流量。");
				logUploadText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			}
		}

		private void setlogUploadTextFaildColor(){
			logUploadText.setTextColor(getContext().getResources().getColor(R.color.color_game_16));
		}
		
		private void startStyle() {
			logUploadFlipper.setDisplayedChild(1);
			FaultProcessor.getInstance().registerObservers(observer);
			logUploadProgress.setPercent(FaultProcessor.getInstance().getProgress());
			logUploadText.setText("正在分析网络日志…\n该过程需要几分钟，放置运行即可。");
			logUploadText.setGravity(Gravity.CENTER);
		}

		@Override
		public void cleanup() {
			super.cleanup();
			logUploadButtonExecute.setOnClickListener(null);
			FaultProcessor.getInstance().unregisterObserver(observer);
		}

		@Override
		public HighlightButton getHighlightButton() {
			return HighlightButton.LOG_UPLOAD;
		}

		private void tryToUpload() {

			boolean isCanStart = GameManager.getInstance().getBadPercentOfFirstSegmentNetDelay() >= 20;
			if (isCanStart) {
				try {
					FaultProcessor.getInstance().start(null, instance.getContext(), gameInfo);
					startStyle();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				notStartStyle();
			}
		}

		private void notStartStyle() {
			logUploadButtonExecute.setEnabled(false);
			logUploadButtonExecute.setAlpha(0.5f);
			logUploadText.setText("没有可以上传的网络日志");
			logUploadText.setGravity(Gravity.CENTER);
		}

	}

	private void destroyDialog() {
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
	}

	private BoxInGame(Context context, int xAnchor, int yAnchor, GameInfo gameInfo, boolean anmiationEnter) {
		super(context);
		this.gameInfo = gameInfo;
		this.currentSkinResource = createSkinResuorce(gameInfo);
		this.anmiationEnter = anmiationEnter;
		//
		this.colorTextError = ColorStateList.valueOf(context.getResources().getColor(R.color.color_game_16));
		this.colorTextNormal = ColorStateList.valueOf(context.getResources().getColor(R.color.color_game_7));
	}

	@Override
	protected boolean canDrag() {
		return false;
	}

	/**
	 * 改变背景
	 * 
	 * @param oldType
	 *            原来是哪个尺寸？
	 * @param newType
	 *            转换到哪个尺寸？
	 */
	private void updateBackground(FloatWindowMeasure.Type oldType, FloatWindowMeasure.Type newType) {
		boolean isSmallToLarge;
		switch (oldType) {
		case MINI:
			isSmallToLarge = true;
			break;
		case LARGE:
			isSmallToLarge = false;
			break;
		default:
			isSmallToLarge = (newType == FloatWindowMeasure.Type.LARGE);
			break;
		}
		if (isSmallToLarge) {
			// 从小到大，需要post执行，否则会有个缺口闪一下
			View v = getView();
			if (v != null) {
				v.post(new Runnable() {
					@Override
					public void run() {
						setBackground();
					}
				});
			}
		} else {
			setBackground();
		}
	}

	private void setBackground() {
		if (currentSkinResource != null) {
			int resource = currentSkinResource.getBackgroundResource();
			if (resource > 0) {
				mainPanel.setBackgroundResource(resource);
			}
		}
	}

	@Override
	protected void onViewAdded(View view) {
		wholeLayout = view.findViewById(R.id.box_in_game_whole_layout);
		mainPanel = view.findViewById(R.id.panel_basemap);
		containerOfPages = (ViewGroup) view.findViewById(R.id.box_in_game_page_container);
		modifyViewFlowWindowSetting(view);
		//
		// 右边的按钮组
		buttonCurveRight = (ImageView) view.findViewById(R.id.box_in_game_button_curve_right);
		buttonSettingsRight = (ImageView) view.findViewById(R.id.box_in_game_button_setting_right);
		buttonDetailsRight = (ImageView) view.findViewById(R.id.box_in_game_button_details_right);
		buttonLogUpload = (ImageView) view.findViewById(R.id.box_in_game_button_log_upload);
		buttonScreenShotRight = (ImageView) view.findViewById(R.id.box_in_game_button_screenshot_right);
		buttonCurveRight.setOnClickListener(onButtonClickListener);
		buttonSettingsRight.setOnClickListener(onButtonClickListener);
		buttonDetailsRight.setOnClickListener(onButtonClickListener);
		buttonLogUpload.setOnClickListener(onButtonClickListener);

		buttonScreenShotRight.setVisibility(View.GONE);

		//
		// 要不要在设置按钮上显示个小红点？
		if (isDisplayBoxInGameSettingRedPoint()) {
			ViewStub remind = (ViewStub) view.findViewById(R.id.box_in_game_button_setting_right_icon_remind);
			remind.inflate();
		}

		if (anmiationEnter) {
			enterAnimation();
		}

		//异常状态显示
		exceptionDesc = (TextView) view.findViewById(R.id.exception_desc_text);

		//电量、时间显示
		batteryImageView = (ImageView) view.findViewById(R.id.battery_capacity);
		batteryClipDrawable = (ClipDrawable) batteryImageView.getDrawable();
		batteryPercent = (TextView) view.findViewById(R.id.battery_percent);
		if (batteryReceiver == null) {
			batteryReceiver = new BatteryReceiver();
			try {
				Intent intent = AppMain.getContext().registerReceiver(batteryReceiver,
					new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
				batteryReceiver.updateBatteryView(intent);
			} catch (RuntimeException re) {
				UIUtils.setViewVisibility(batteryPercent, View.INVISIBLE);
				UIUtils.setViewVisibility(batteryImageView, View.INVISIBLE);
				batteryReceiver = null;
			}
		}

		clickVpn = -1;
		if (FloatWindowCommon.isNetworkException()) {
			changePage(new PageCheckingNetwork());
		} else if (!AccelOpenManager.isStarted()) {
			changePage(new PageAccelStopped(HighlightButton.DETAILS));
			clickVpn = 0;
		} else {
			changePage(new PageTwoSectionDelay());
		}
		//
		adjustSelfLayout();
		adjustAnchorPosition();
		//
		setBackground();
		//
		String param = getStatisticStatusParamWithCurrentState(view.getContext());
		Statistic.addEvent(view.getContext(), Statistic.Event.FLOATING_WINDOW_OPEN_STATUS, param);

		TriggerManager.getInstance().addObserver(eventObserver);
		NetDelayDataManager.getInstance().addObserver(this.netDelayDataObserver);

	}

	private void modifyViewFlowWindowSetting(View view) {
		if (ParallelConfigDownloader.isPhoneParallelSupported()) {
			ViewStub viewStub = (ViewStub) view.findViewById(R.id.floatwindow_setting);
			viewStub.setLayoutResource(R.layout.floatwindow_setting_double_accel);
			viewStub.inflate();
		}
	}

	private Switch.OnChangedListener onSwitchChangeListener = new Switch.OnChangedListener() {

		@Override
		public void onCheckedChanged(Switch checkSwitch, boolean checked) {
			switch (checkSwitch.getId()) {
			//			case R.id.switch_skin:
			////				StatisticDefault.addEvent(getContext(), StatisticDefault.Event.FLOATING_WINDOW_CLICK_SKIN_SKLR, checked ? "1" : "2");
			//				ConfigManager.getInstance().setFloatwindowHunterSkin(checked);
			//				redisplay();
			//				break;
			case R.id.check_visible_delay:
				ConfigManager.getInstance().setFloatwindowSwitchDelay(checked);
				break;
			case R.id.check_double_link:
				ConfigManager.getInstance().setDoubleAccelStatus(checked);
				//VPNManager.getInstance().sendUnionAccelSwitch(checked);
				VPNUtils.sendUnionAccelSwitch(checked, TAG);
				String status = checked ? "开" : "关";
                ActivityDoubleLink.uploadDoubleAccelSwithEvent(checked);
				//StatisticDefault.addEvent(getContext(), StatisticDefault.Event.INTERACTIVE_DUAL_NETWORK_SWITCH_CLICK, status);
				Statistic.addEvent(getContext(), Statistic.Event.FLOATING_WINDOW_DUAL_NETWORK_SWITCH_CLICK, status);
                ActivityDoubleLink.uploadFirstStartDoubleAccel(checked);
			}
		}
	};

	private RadioGroup.OnCheckedChangeListener onRadioCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (group.getId()) {
			case R.id.setting_radiogroup:
				FloatWindowMeasure.Type oldType = FloatWindowMeasure.getCurrentType();
				FloatWindowMeasure.Type newType = getFloatWindowMeasureTypeFromCheckedRadio(checkedId);
				if (oldType != newType && FloatWindowMeasure.setCurrentType(newType)) {
					FloatWindowInGame.adjustInstanceSize(getX(), getY());
					updateBackground(oldType, newType);
					StatisticUtils.statisticFloatwindowType(getContext(), newType);
				}
				break;
			}
		}

		private FloatWindowMeasure.Type getFloatWindowMeasureTypeFromCheckedRadio(int checkedId) {
			switch (checkedId) {
			case R.id.radio_mini:
				return FloatWindowMeasure.Type.MINI;
			case R.id.radio_large:
				return FloatWindowMeasure.Type.LARGE;
			default:
				return FloatWindowMeasure.Type.NORMAL;
			}
		}
	};

	private int clickVpn = -1; // -1代表vpn本身就是开启的；0代表vpn没有开启，也没有点击开启按钮；1代表vpn没有开启，但是点击了开启按钮。

	@Override
	protected void destroy() {
		if (currentPage != null) {
			currentPage.cleanup();
		}
		if (twoSectionDelayDrawer != null) {
			twoSectionDelayDrawer.cleanUp();
		}
		NetDelayDataManager.getInstance().deleteObserver(this.netDelayDataObserver);
		//
		if (batteryReceiver != null) {
			getContext().unregisterReceiver(batteryReceiver);
			batteryReceiver = null;
		}
		if (clickVpn == 0) {
			Statistic.addEvent(getContext(), Event.FLOATING_WINDOW_OPEN_NO_ACC, "未开启加速");
		} else if (clickVpn == 1) {
			Statistic.addEvent(getContext(), Event.FLOATING_WINDOW_OPEN_NO_ACC, "点击开启加速");
		}
		destroyDialog();
		restoreFloatInWindowPosition();

		if (FaultProcessor.getInstance().isRunning()) {
			FaultProcessor.getInstance().stop();
			UIUtils.showToast("您已取消上传日志");
		}

		super.destroy();
	}

	/** 根据当前状态返回 统计事件所需的参数字串 */
	private String getStatisticStatusParamWithCurrentState(Context context) {
		if (!AccelOpenManager.isStarted()) {
			return "VPN未开启";
		}
		if (NetManager.getInstance().isDisconnected()) {
			return "离线";
		}
		if (NetManager.getInstance().getCurrentNetworkType() == NetTypeDetector.NetType.MOBILE_2G) {
			return "2G";
		}
		long netDelay = GameManager.getInstance().getFirstSegmentNetDelay();
		if (netDelay == GlobalDefines.NET_DELAY_TEST_FAILED) {
			return "测速失败-1";
		} else if (netDelay == GlobalDefines.NET_DELAY_TEST_WAIT) {
			return "等待测速-2";
		} else if (netDelay < 0) {
			return "无时延值上报";
		} else if (netDelay < GlobalDefines.NET_DELAY_TIMEOUT) {
			return "正常值";
		} else {
			return "超时";
		}
	}

	private void restoreFloatInWindowPosition() {
		FloatWindowInGame floatWindowInGame = FloatWindowInGame.getInstance();
		if (floatWindowInGame != null) {
			floatWindowInGame.restorePosition();
		}
	}

	/** 将自己调整到合适的位置 */
	private void adjustSelfLayout() {
		Point screenSize = MetricsUtils.getDevicesSizeByPixels(getContext());
		int xCenter = screenSize.x / 2;
		int yCenter = screenSize.y / 2;
		setCenterPosition(xCenter, yCenter);
	}

	/** 调整锚点（小悬浮窗）的位置 */
	private void adjustAnchorPosition() {
		// 将小悬浮窗调整到合适的位置
		FloatWindowInGame floatWindowInGame = FloatWindowInGame.getInstance();
		if (floatWindowInGame == null) {
			return;
		}
		floatWindowInGame.savePosition();
		floatWindowInGame.setPosition(getX(), getY());
	}

	@Override
	protected void onTouchOutside() {
		super.onTouchOutside();
		destroyInstance();
	}

	private boolean changePage(Page page) {
		if (page == null || page == currentPage) {
			return false;
		}
		//		
		setAllButtonEnabled(true);
		if (currentPage != null) {
			currentPage.cleanup();
			HighlightButton hb = currentPage.getHighlightButton();
			if (hb != null) {
				highlightButton(hb, false);
			}
		}
		//
		currentPage = page;
		HighlightButton hb = page.getHighlightButton();
		if (hb != null) {
			highlightButton(hb, true);
		}
		//
		int pageIndex = currentPage.getPageIndex();
		//
		for (int i = 0; i < containerOfPages.getChildCount(); ++i) {
			View child = containerOfPages.getChildAt(i);
			if (pageIndex == i) {
				UIUtils.setViewVisibility(child, View.VISIBLE);
			} else if (child.getVisibility() == View.VISIBLE) {
				UIUtils.setViewVisibility(child, View.GONE);
			}
		}
		currentPage.init();

		return true;
	}

	private void setAllButtonEnabled(boolean enable) {
		buttonCurveRight.setEnabled(enable);
		buttonDetailsRight.setEnabled(enable);
		buttonSettingsRight.setEnabled(enable);
		buttonLogUpload.setEnabled(enable);
		int backgroundResource = enable ? currentSkinResource.getButtonEnableBackgroundRecource() : currentSkinResource
			.getButtonUnEnableBackgroundRecource();
		buttonCurveRight.setBackgroundResource(backgroundResource);
		buttonDetailsRight.setBackgroundResource(backgroundResource);
		buttonSettingsRight.setBackgroundResource(backgroundResource);
		buttonLogUpload.setBackgroundResource(backgroundResource);
		buttonScreenShotRight.setBackgroundResource(backgroundResource);
	}

	/**
	 * 统计页面生存时间
	 */
	private void doStatisticLiveTime() {
		int seconds = (int) ((SystemClock.elapsedRealtime() - birthTime) / 1000);
		if (seconds > 20) {
			seconds = 20;
		}
		//		StatisticDefault.addEvent(this.getContext(), StatisticDefault.Event.FLOATING_WINDOW_UNFOLD_TIME,
		//			Integer.toString(seconds));
	}

	private void changeSettingButton() {
		if (!ConfigManager.getInstance().isFloatwindowButtonSettingGuide())
			return;
		if (!FloatWindowCommon.isSupportHunterSkin(gameInfo))
			return;
		buttonSettingsRight.setImageResource(currentSkinResource.getButtonSettingResourceChecked());
		ConfigManager.getInstance().setNoFloatwindowButtonSettingGuide();
	}

	/** 高亮按钮 */
	private static enum HighlightButton {
		/** 折线图 */
		CURVE,
		/** 详情 */
		DETAILS,
		/** 设置 */
		SETTINGS,
		/** 日志上传 */
		LOG_UPLOAD,
		/** 截屏 */
		SCREENSHOT,
	}

	private void highlightButton(HighlightButton hb, boolean active) {
		ImageView button;
		int checkRes, uncheckRes;
		switch (hb) {
		case CURVE:
			button = buttonCurveRight;
			checkRes = currentSkinResource.getButtonCurveResourceChecked();
			uncheckRes = currentSkinResource.getButtonCurveResourceUnChecked();
			break;
		case DETAILS:
			button = buttonDetailsRight;
			checkRes = currentSkinResource.getButtonDetailsResourceChecked();
			uncheckRes = currentSkinResource.getButtonDetailsResourceUnChecked();
			break;
		case SETTINGS:
			button = buttonSettingsRight;
			checkRes = currentSkinResource.getButtonSettingResourceChecked();
			uncheckRes = currentSkinResource.getButtonSettingResourceUnChecked();
			break;
		case LOG_UPLOAD:
			button = buttonLogUpload;
			checkRes = currentSkinResource.getButtonLogUploadResourceChecked();
			uncheckRes = currentSkinResource.getButtonLogUploadResourceUnChecked();
			break;
		case SCREENSHOT:
			button = buttonScreenShotRight;
			checkRes = currentSkinResource.getButtonScreenShotResourceChecked();
			uncheckRes = currentSkinResource.getButtonScreenShotResourceUnChecked();
			break;
		default:
			return;
		}
		if (active) {
			button.setImageResource(checkRes);
			button.setBackgroundResource(currentSkinResource.getButtonBackgroundResourceOnSelected());
			button.setScaleType(ScaleType.CENTER);
		} else {
			button.setImageResource(uncheckRes);
			button.setBackgroundResource(currentSkinResource.getButtonEnableBackgroundRecource());
			button.setScaleType(ScaleType.FIT_START);
		}
	}

	class BatteryReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
				updateBatteryView(intent);
			}
		}

		void updateBatteryView(Intent intent) {
			if (intent != null) {
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
				int percent = (scale == 0) ? level : level * 100 / scale;
				batteryClipDrawable.setLevel(percent * 100);
				batteryPercent.setText(percent + "%");
			}
		}
	}

	public void openSettingPage() {
		changePage(new PageSettings());
	}

	//	private void redisplay() {
	//		View view = getView();
	//		if (view == null)
	//			return;
	//		Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.floatwindow_exit);
	//		animation.setAnimationListener(new AnimationListener() {
	//
	//			@Override
	//			public void onAnimationStart(Animation animation) {}
	//
	//			@Override
	//			public void onAnimationRepeat(Animation animation) {}
	//
	//			@Override
	//			public void onAnimationEnd(Animation animation) {
	//				wholeLayout.setVisibility(View.GONE);
	//			}
	//		});
	//		wholeLayout.startAnimation(animation);
	//		FloatWindowInGame.getInstance().redisplay();
	//	}

	private void enterAnimation() {
		Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.floatwindow_enter);
		wholeLayout.startAnimation(animation);
	}

	private static SkinResourceBoxInGame createSkinResuorce(GameInfo info) {
		return new SkinResourceBoxInGameNormal();
	}
}
