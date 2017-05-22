package cn.wsds.gamemaster.ui;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.StringUtils;
import com.subao.net.NetManager;
import com.subao.utils.MetricsUtils;
import com.subao.utils.UrlConfig;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.NetDelayDetector;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ResUsageChecker;
import cn.wsds.gamemaster.ResUsageChecker.ResUsage;
import cn.wsds.gamemaster.SelfUpgrade;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.data.ProcessCleanRecords;
import cn.wsds.gamemaster.data.UserFeedback;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.UserCenterPormpt;
import cn.wsds.gamemaster.dialog.UserSign;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager.Checker.Result;
import cn.wsds.gamemaster.service.aidl.IGameVpnService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.StatisticPerDay;
import cn.wsds.gamemaster.statistic.StatisticUtils;
import cn.wsds.gamemaster.tools.JPushUtils;
import cn.wsds.gamemaster.tools.ProcessKiller;
import cn.wsds.gamemaster.tools.SystemInfoUtil;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenWorker;
import cn.wsds.gamemaster.ui.accel.AccelOpener;
import cn.wsds.gamemaster.ui.accel.AccelOpener.OpenSource;
import cn.wsds.gamemaster.ui.accel.DialogWhenImpowerReject;
import cn.wsds.gamemaster.ui.accel.animation.AccelAnimationListener;
import cn.wsds.gamemaster.ui.accel.animation.AnimationAccelOn;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailProcesser;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailType;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenFloatwindowHelp;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenHelpManager;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenHelpManager.OnFinshListener;
import cn.wsds.gamemaster.ui.mainfragment.FragmentList;
import cn.wsds.gamemaster.ui.mainfragment.FragmentNetState;
import cn.wsds.gamemaster.ui.mainfragment.PieceAccelOff;

public class ActivityMain extends ActivityVpnOpener {
	
	public static final NetDelayDetector.Type NET_DELAY_DETECT_TYPE = NetDelayDetector.Type.UDP;

	private MainContainer mainContainer;

	//// 界面引用fragment
	private FragmentNetState fragmentNetState;
	private FragmentList fragmentList;

	////
	private AnimationAccelOn animationAccelOn;
	private NetDelayDetector.Observer tcpNetDelayObserver;
	private FloatwindowHelper floatwindowHelper;
	//// 依然加速开启状态显示界面
	 
	private final MenuIconMarker menuIconMarker = new MenuIconMarker_Message() {

		@Override
		protected int getResIdNormal() {
			if (UserSession.isLogined()) {// 用户未登录
				return R.drawable.title_user_login;
			} else {
				return R.drawable.title_user_unknow;
			}
			//return R.drawable.title_user;
		};

		@Override
		protected int getResIdStriking() {
			if (UserSession.isLogined()) {// 用户未登录
				return R.drawable.title_user_login_red;
			} else {
				return R.drawable.title_user_unknow;
			}
			//return R.drawable.home_page_network_state_red;
		};

		@Override
		protected void recheckState() {
			super.recheckState(); // 这一行很必要，不要去掉！
			//TODO: 下面添加“检查是否有未完成的当日任务（比如签到）”
		};

		@Override
		protected boolean isStateStriking() {
			if (super.isStateStriking()) {
				return true;
			}
			if (!UserSession.isLogined()) {// 用户未登录
				return false;
			}
			if (!ConfigManager.getInstance().isEnterUserCenterTodayOnLogin() && !UserSign.getInstance().isUserSignToday(UserSession.getInstance().getUserId())) {
				// 用户今天未签到 并且 用户今天并没有在登录状态下进入用户中心
				return true;
			}
			if (ActivityUser.needAlertScoreEnough()) {//积分已购并需要提示
				return true;
			}
			return false;
		};
	};

	/** 上方红色条，显示网络异常 */
	private TextView netExceptionText;

	// 控制成员变量
	private CommonAlertDialog feedbackDialog;// 反馈提醒对话框
	private GameManager.Observer gameManagerObserver = new GameManager.Observer() {

		@Override
		public void onGameListUpdate() {
			invalidateOptionsMenu();
			if (isAnimationRunning()) {
				return;
			}
			fragmentList.refreshGameList();
			JPushUtils.setTagsForJPush(ActivityMain.this);
		}

        @Override
        public void onDoubleAccelTimeChanged(String packageName, int seconds) {

        }

        @Override
		public void onAccelTimeChanged(int seconds) {
			// do nothing
		}
	};

	private AccelProcessHelper accelOpenAssistant;

	private final NetworkCheckManager.Observer networkCheckObserver = new NetworkCheckManager.Observer() {

		/**
		 * 根据 message 是否有效判断是否显示网络异常提示
		 * 
		 * @param message
		 *            网络异常提醒内容
		 */
		private void showNetErrorMessage(String message) {
			if (TextUtils.isEmpty(message)) {
				netExceptionText.setVisibility(View.GONE);
			} else {
				netExceptionText.setVisibility(View.VISIBLE);
				netExceptionText.setText(message);
			}
		}

		@Override
		public void onNetworkCheckResult(Result event) {
			String msg;
			switch (event) {
			case OK:
				msg = null;
				break;
			case AIRPLANE_MODE:
				msg = "飞行模式开启";
				break;
			case MOBILE_UNAVAILABLE:
			case WIFI_UNAVAILABLE:
				msg = NetworkCheckManager.getBadNetStateMsg(
						NetManager.getInstance().getCurrentNetworkType(), true);//"移动网络异常"; //"WiFi网络异常";
				if(TextUtils.isEmpty(msg)){
					msg = event.description;
				}
				break;
			case WIFI_MOBILE_CLOSED:
				msg = "网络开关未开启";
				break;
			case NETWORK_DISCONNECT:
				msg = "网络连接已断开";
				break;
			case WIFI_FAIL_RETRIEVE_ADDRESS:
				msg = "WiFi无法获取IP地址";
				break;
			case WIFI_SHOULD_AUTHORIZE:
				msg = "WiFi需要认证";
				break;
			case IP_ADDR_ASSIGN_PENDING:
				msg = "DHCP地址分配中";
				break;
			case WAP_POINT:
				msg = "正在使用WAP接入点";
				break;
			case NETWORK_AUTHORIZATION_FORBIDDED:
				msg = "网络权限被禁用";
				break;
			default:
				msg = "当前网络异常";
				break;
			}
			showNetErrorMessage(msg);
		}
	};

	/**
	 * 系统资源占用（cpu、内存）观察者
	 */
	private static class ResUsageCheckerObserver implements ResUsageChecker.Observer {

		private final WeakReference<ActivityMain> owner;

		public ResUsageCheckerObserver(ActivityMain owner) {
			this.owner = new WeakReference<ActivityMain>(owner);
		}

		@Override
		public void onResUsageCheckResult(ResUsage resUsage) {
			ActivityMain owner = this.owner.get();
			if (owner == null || owner.isFinishing()) {
				return;
			}
			owner.fragmentList.onResUsageAccelOn(resUsage);
		}
	}

	private ResUsageChecker.Observer resUsageCheckObserver;

	/**
	 * 事件观察者
	 */
	private final EventObserver eventObserver = new EventObserver() {
		public void onNewFeedbackReply(List<UUID> newReplyUUIDList) {
			selectFeeddbackReply();
		};

		@Override
		public void onAccelSwitchChanged(boolean state) {
			if (state) {
				ConfigManager.getInstance().addInMainOpenAccelCount();
			} else {
				if (isAnimationRunning()) {
					animationAccelOn.abort();
				}
				mainContainer.switchPiece(false);
			}
		}

		@Override
		public void onSupportedGameUpdate() {
			if (!isAnimationRunning()) {
				fragmentList.refreshGameList();
				JPushUtils.setTagsForJPush(ActivityMain.this);
			}
		}

		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			fragmentNetState.onNetChange(state);
			startNetChecker(NetDelayDetector.getDelayValue(NET_DELAY_DETECT_TYPE));
		}
	};

	private final AccelOpenWorker accelOpenWorker = new AccelOpenWorker() {

		@Override
		public void openAccel() {
			if (accelOpenAssistant == null || AccelOpenManager.isStarted() || isAnimationRunning()) {
				return;
			}
			accelOpenAssistant.openAccel(this);
		}
	};

	private ViewFlipper accelOnContentFlipper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//临时方案，保证主页显示时，iVpnService可用
		IGameVpnService iVpnService= VPNUtils.getIGameVpnService();	
		if(iVpnService == null){
			UIUtils.turnActivity(this, ActivityStart.class);
			finish();
			return;
		}
	
		setDisplayHomeArrow(R.string.app_name);
		//
		setContentView(R.layout.activity_main);
		//
		this.resUsageCheckObserver = new ResUsageCheckerObserver(this);

		netExceptionText = (TextView) findViewById(R.id.net_exception_text);
		accelOnContentFlipper = (ViewFlipper) findViewById(R.id.accelOnContentFlipper);

		initFragment();
		floatwindowHelper = new FloatwindowHelper(findViewById(R.id.float_window_helper));
		this.mainContainer = new MainContainer();
		initAccelAnimation();

		StatisticUtils.statisticInitializer(this, fragmentList.getGameInfos().size());
		//
		String warningMessage = StringUtils.EMPTY;
		if (UrlConfig.instance.getServerType() != UrlConfig.ServerType.NORMAL) {
			warningMessage = "当前使用的是测试服";
		}
		String debugNodeIP = ConfigManager.getInstance().getDebugNodeIP();
		if (!TextUtils.isEmpty(debugNodeIP)) {
			if (warningMessage.length() > 0) {
				warningMessage += "\r\n";
			}
			warningMessage = "当前使用的是测试节点 " + debugNodeIP;
		}
		if (ConfigManager.getInstance().getUseTestUmengKey()) {
			if (warningMessage.length() > 0) {
				warningMessage += "\r\n";
			}
			warningMessage = "当前使用的是测试友盟KEY";
		}
		if (warningMessage.length() > 0) {
			UIUtils.showToast(warningMessage, Toast.LENGTH_LONG);
		}

		GameManager.getInstance().registerObserver(gameManagerObserver);
		TriggerManager.getInstance().addObserver(eventObserver);

		accelOpenAssistant = new AccelProcessHelper(this, OpenSource.Main, new AccelOpenerListener());

		tryCreateShortcut();
		ConfigManager configManager = ConfigManager.getInstance();
		if (!configManager.getSendNoticeAutoClean()) {
			AppNotificationManager.sendMemoryAutoClean();
			configManager.setSendNoticeAutoClean();
		}
		//

		menuIconMarker.attachActivity(this);
		//
		netExceptionText.post(new Runnable() {
			
			@Override
			public void run() {
				SelfUpgrade.getInstance().showDialogWhenNeed(ActivityMain.this, false, true);
			}
		});
	}

	private void tryCreateShortcut() {
		ConfigManager configManager = ConfigManager.getInstance();
		if (configManager.isClickGameLaunchButton() && !configManager.isManuallyCreatedShortcut()
			&& !configManager.isCreateShortcut()) {
			if (ShortcutCreateHelper.Result.OK == ShortcutCreateHelper.createShortcut(this)) {
				configManager.setCreateShortcut();
			}
			Statistic.addEvent(this, Statistic.Event.INTERACTIVE_SHORTCUT_CREATE, "自动创建");
		}
	}

	private void initFragment() {

		FragmentManager fragmentManager = getFragmentManager();
		fragmentNetState = (FragmentNetState) fragmentManager.findFragmentById(R.id.fragment_netstate);
		fragmentList = (FragmentList) fragmentManager.findFragmentById(R.id.fragment_list);
		fragmentList.setAccelOpenWorker(accelOpenWorker);

	}

	// 打开使用成就相关逻辑
	private void openGamePlayAchieve() {
		if (ConfigManager.getInstance().isAlreadyOpenNoticeAccelAchieve()
			|| !ConfigManager.getInstance().isAlreadySendNoticeAccelAchieve()) {
			return;
		}
		long timeOfNoticeAccelAchieve = ConfigManager.getInstance().getTimeOfNoticeAccelAchieve();
		if (timeOfNoticeAccelAchieve > 0) {
			if (System.currentTimeMillis() - timeOfNoticeAccelAchieve > 3 * 3600 * 1000) {
				delayOpenGamePlayAchieve();
			}
			return;
		}
		if (2 == ConfigManager.getInstance().getInMainOpenAccelCount()) {
			delayOpenGamePlayAchieve();
		}
	}

	private void delayOpenGamePlayAchieve() {
		MainHandler.getInstance().postDelayed(new Runnable() {

			@Override
			public void run() {
				if (isFinishing()) {
					return;
				}
				Intent intent = new Intent(ActivityMain.this, ActivityNewGamePlayAchieve.class);
				//				intent.putExtra(IntentExtraName.INTENT_EXTRANAME_AVHIEVE_OPENAT_MAIN, true);
				startActivity(intent);
			}
		}, 2000);
	}

	private void initAccelAnimation() {
		animationAccelOn = new AnimationAccelOn(mainContainer);
		animationAccelOn.setAccelAnimationListener(new AccelAnimationListener() {

			@Override
			public void onAnimationEnd() {
				if (AccelOpenManager.isStarted()) {
					startNetChecker(NetDelayDetector.getDelayValue(NET_DELAY_DETECT_TYPE));
					HelpUIController.showHelpUIWhenNeed(ActivityMain.this);
					if (mainContainer.isCurrentPieceAccelOn()) {
						floatwindowHelper.reset();
					} else {
						mainContainer.switchPiece(true);
					}
					openGamePlayAchieve();

					//动画运行前开启硬件极速，避免在手机上展开列表绘制曲线时出现页面卡顿
					mainContainer.pieceAccelOn.setLayerType(View.LAYER_TYPE_HARDWARE, null);
				}
			}

			@Override
			public void onAnimationStart() {
				netExceptionText.setVisibility(View.GONE);
				
				//动画运行前关闭硬件极速，避免在某些手机上因为硬件加速而崩溃 
				mainContainer.pieceAccelOn.setLayerType(View.LAYER_TYPE_SOFTWARE, null);				 
			}

		});

	}

	private void startNetChecker(long delayMilliseconds) {
		boolean delayError = delayMilliseconds >= GlobalDefines.NET_DELAY_TIMEOUT
			|| delayMilliseconds == GlobalDefines.NET_DELAY_TEST_FAILED;
		boolean delayWait = (delayMilliseconds == GlobalDefines.NET_DELAY_TEST_WAIT);
		if ((delayError || delayWait) && !isAnimationRunning()) {
			NetworkCheckManager.start(this, networkCheckObserver);
		} else {
			netExceptionText.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		StartNodeDetectUI.destroyInstance(this);
		menuIconMarker.detachActivity();
		MainHelp.close();
		TriggerManager.getInstance().deleteObserver(eventObserver);
		//
		if (this.gameManagerObserver != null) {
			GameManager.getInstance().unregisterObserver(this.gameManagerObserver);
			this.gameManagerObserver = null;
		}
		this.resUsageCheckObserver = null;
	}

	@Override
	public void onStart() {
		super.onStart();
		// 主页创建，看看要不要上传那一大堆Setting的值
		StatisticPerDay.reportIfNeed(this);
		//
		selectFeeddbackReply();
		invalidateOptionsMenu();
		boolean animationRunning = isAnimationRunning();
		if (!animationRunning) {
			HelpUIController.showHelpUIWhenNeed(ActivityMain.this);
		}
		
//		 //6.0以上版本的极光询问设置，只调用一次
		if(android.os.Build.VERSION.SDK_INT>=23){
			if(!ConfigManager.getInstance().isHasRequestSDK23WPermisionForJPush()){
				 //在 Android 6.0 及以上的系统上，需要去请求一些用到的权限，
				 //JPush SDK 用到的一些需要请求一些权限，因为需要这些权限使统计更加精准，
				 //功能更加丰富，极光推荐调用。	
				JPushUtils.requestPermission(this);
				ConfigManager.getInstance().setHasRequestSDK23WPermisionForJpush();
			}
		}
		
		if(!ConfigManager.getInstance().isHasSetTagForJPush()){	  
			JPushUtils.setTagsForJPush(this);	 
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (!isAnimationRunning()) {
			mainContainer.switchPiece(AccelOpenManager.isStarted());
		}
	}

	private boolean isAnimationRunning() {
		return animationAccelOn != null && animationAccelOn.isRunning();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (feedbackDialog != null && feedbackDialog.isShowing()) {
			feedbackDialog.dismiss();
			feedbackDialog = null;
		}
		DialogWhenImpowerReject.destroyInstance();
	}

	@Override
	protected void onResume() {
		super.onResume();
		floatwindowHelper.reset();

		if (AccelOpenManager.isStarted() && !isAnimationRunning()) {
			//注意，已知某些手机上会出现未调用onActivityResult()的问题
			new ResUsageGetter().executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
		}

		statisticAwakeRead();
		//
		if (AccelOpenManager.isStarted() && getIntent().getBooleanExtra(IntentExtraName.START_FROM_OPEN_GAME_INSIDE, false)) {
			getIntent().removeExtra(IntentExtraName.START_FROM_OPEN_GAME_INSIDE);
			if (HelpUIController.canShowHelpUIForStartGameInside(this, false)) {
				showHelpUIForStartGameInside(5000);
			}
		}

		// 开启时延测速
		if (tcpNetDelayObserver == null) {
			tcpNetDelayObserver = new NetDelayDetector.Observer() {
				@Override
				public void onNetDelayChange(int value, NetDelayDetector.Type type) {
					if (!animationAccelOn.isRunning()) {
						fragmentNetState.onGeneralNetDelayChange(value);
					}
					startNetChecker(value);
				}
			};
			NetDelayDetector.addObserver(tcpNetDelayObserver, NET_DELAY_DETECT_TYPE);
		}

		ResUsageChecker.getInstance().enter(resUsageCheckObserver, 10 * 1000);

        if (!animationAccelOn.isRunning()) {
            fragmentNetState.refreshDoubleIcon();
        }
    }

	@Override
	public void onPause() {
		super.onPause();
		if (tcpNetDelayObserver != null) {
			NetDelayDetector.removeObserver(tcpNetDelayObserver, NET_DELAY_DETECT_TYPE);
			tcpNetDelayObserver = null;
		}

		ResUsageChecker.getInstance().leave(resUsageCheckObserver);
	}

	//	@Override
	//	public boolean onKeyDown(int keyCode, KeyEvent event) {
	//		fragmentAccelOff.onUserOperateApp(true);
	//		return super.onKeyDown(keyCode, event);
	//	}

	@Override
	public void onBackPressed() {
		if (isAnimationRunning()) {
			try {
				this.moveTaskToBack(false);
				return;
			} catch (RuntimeException e) {}
		}
		super.onBackPressed();
	}

	/**
	 * 加速开启监听
	 */
	private final class AccelOpenerListener implements AccelOpener.Listener {

		@Override
		public void onStartFail(AccelOpener accelOpener, FailType type) {
			if (accelOpener != null) {
				clearAccelOpener();
				FailProcesser failProcesser = accelOpener.getFailProcesser();
				failProcesser.process(accelOpener, type, ActivityMain.this);
			}
		}

		@Override
		public void onStartSucceed() {
			if (!isFinishing()) {
				clearAccelOpener();
				animationAccelOn.start(cleanProcess());
			}
		}

		private void clearAccelOpener() {
			if (accelOpenAssistant != null) {
				accelOpenAssistant.clearAccelOpener();
			}
		}
	};

	/**
	 * 清理应用并返回清理数量
	 * 
	 * @return 清理应用个数
	 */
	private int cleanProcess() {
		Set<String> packageNameList = ProcessCleanRecords.getInstance().getCleanRecord(null);
		if (packageNameList.isEmpty()) {
			return 0;
		}
		boolean result = ProcessKiller.execute(getApplicationContext(), packageNameList);
		if (result) {
			return packageNameList.size();
		} else {
			return 0;
		}
	}

	/**
	 * 获得当前的系统资源占用情况 包括 调试用的占用情况 或者最近的占用情况
	 * <p>
	 * 由于会耗约半秒时间，所以采用异步方式
	 * </p>
	 */
	private class ResUsageGetter extends AsyncTask<Void, Void, ResUsage> {

		@Override
		protected ResUsage doInBackground(Void... params) {
			ResUsage debugUsage = ResUsageChecker.getInstance().getDebugUsage();
			if (debugUsage != null) {
				return new ResUsage(debugUsage.cpuUsage, debugUsage.memoryUsage, SystemInfoUtil.getRunningAppList());
			} else {
				int cpuUsage = SystemInfoUtil.getCpuUsage();
				int memoryUsage = SystemInfoUtil.getMemoryUsage(AppMain.getContext());
				return new ResUsage(cpuUsage, memoryUsage, SystemInfoUtil.getRunningAppList());
			}
		}

		@Override
		protected void onPostExecute(ResUsage result) {
			if (!isFinishing()) {
				fragmentList.onResUsageAccelOn(result);
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case GlobalDefines.START_ACTIVITY_REQUEST_CODE_MEMORY_CLEAN:
			onMemoryCleanActivityResult(resultCode);
			break;
		default:
			if (accelOpenAssistant != null) {
				AccelOpener accelOpener = accelOpenAssistant.getAccelOpener();
				if (accelOpener != null) {
					accelOpener.checkResult(requestCode, resultCode, data);
				}
			}
		}
	}

	/**
	 * 清理内存界面返回对结果进行处理
	 * 
	 * @param resultCode
	 */
	private void onMemoryCleanActivityResult(int resultCode) {
		if (RESULT_OK == resultCode) {
			fragmentList.setMemoryCleanPromptVisible(false, 0);
		}
	}

	@Override
	protected boolean isMainActivity() {
		return true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		this.setIntent(intent);
	}

	private void statisticAwakeRead() {
		Intent intent = getIntent();
		if (intent.getBooleanExtra(IntentExtraName.START_FROM_NOTIFICATION, false)) {
			Statistic.addEvent(this, Statistic.Event.NOTIFICATION_AWAKE_READ);
			intent.removeExtra(IntentExtraName.START_FROM_NOTIFICATION);
		}
	}

	private void selectFeeddbackReply() {
		if (UserFeedback.History.instance.hasUnread()) {
			if (feedbackDialog == null) {
				feedbackDialog = new CommonAlertDialog(this);
				feedbackDialog.setMessage(R.string.hasunread_feedback_mess);
				feedbackDialog.setCanceledOnTouchOutside(true);
				feedbackDialog.setPositiveButton("立即查看", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dlg, int which) {
						UserFeedback.History.instance.setAllRead();
						UIUtils.turnActivity(ActivityMain.this, ActivityMessage.class);
					}
				});
			}
			if (!isFinishing() && !feedbackDialog.isShowing()) {
				feedbackDialog.show();
			}
		}
	}

	/**
	 * 显示帮助界面，引导用户在APP内开启游戏
	 */
	private class HelpUIForStartGameInsideShower implements Runnable {
		private final long delayAutoClose;
		private int step;

		private HelpUIForStartGameInsideShower(long delayAutoClose) {
			this.delayAutoClose = delayAutoClose;
		}

		@Override
		public void run() {
			switch (step) {
			case 0:
				if (!fragmentList.collapseAll()) {
					gray();    
					step = 1;
				}
				break;
			case 1:
				gray();
				break;
			case 2:
				showArrowAndMask();
				return;
			}
			++step;
			show();
		}

		public void show() {
			ExpandableListView accelListView = fragmentList.getAccelListView();
			if (accelListView != null) {
				accelListView.post(this);
			}
		}

		private void gray() {
			AdapterGameList listAdapter = fragmentList.getListAdapter();
			if (listAdapter != null) {
				listAdapter.setGrayExceptStartButton(true);
			}
		}

		private View findFirstStartButton() {
			ExpandableListView accelListView = fragmentList.getAccelListView();
			if (accelListView == null) {
				return null;
			}
			int i = accelListView.getFirstVisiblePosition();
			if (i >= 0 && i < accelListView.getCount()) {
				AdapterGameList listAdapter = fragmentList.getListAdapter();
				if (listAdapter != null) {
					return listAdapter.getStartButton(i);
				}
			}
			return null;
		}

		private void showArrowAndMask() {
			ExpandableListView accelListView = fragmentList.getAccelListView();
			if (accelListView == null) {
				return;
			}
			Point screenSize = MetricsUtils.getDevicesSizeByPixels(ActivityMain.this);
			// 找锚点
			int[] location = new int[2];
			accelListView.getLocationOnScreen(location);
			final int yAnchor = location[1];
			int xAnchor;
			View startButton = findFirstStartButton();
			if (startButton == null) {
				xAnchor = location[0] + accelListView.getMeasuredWidth() - MetricsUtils.dp2px(ActivityMain.this, 64);
			} else {
				startButton.getLocationOnScreen(location);
				xAnchor = location[0] + (startButton.getMeasuredWidth() >> 1);
			}
			// 上部分的Mask
			Rect rectMaskTop = new Rect(0, 0, screenSize.x, yAnchor);
			// 下部分的Mask
			Object lastVisibleObject = null; // 列表里最后一个可以看见的项
			int lastVisiblePos = accelListView.getLastVisiblePosition();
			if (lastVisiblePos >= 0 && lastVisiblePos < accelListView.getCount()) {
				lastVisibleObject = accelListView.getItemAtPosition(lastVisiblePos);
			}
			if (lastVisibleObject != null || !fragmentList.getMemoryCleanPromptLocationOnScreen(location)) {
				location[1] = screenSize.y;
			}
			Rect rectMaskBottom = new Rect(0, location[1], screenSize.x, screenSize.y);
			HelpUIForStartGameInside dlg = HelpUIForStartGameInside.showInstance(ActivityMain.this, rectMaskTop, rectMaskBottom,
				xAnchor, yAnchor, delayAutoClose);
			if (dlg != null) {
				dlg.setOnDismissListener(new OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						AdapterGameList listAdapter = fragmentList.getListAdapter();
						if (listAdapter != null) {
							listAdapter.setGrayExceptStartButton(false);
						}

					}
				});
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_user:
			UIUtils.turnActivity(this, ActivityUser.class);
			break;
		}
		//		fragmentAccelOff.onUserOperateApp(true);
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menuIconMarker.resetMenuIcon(menu.findItem(R.id.action_user));
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * 悬浮窗引导开启控件及对话框相关逻辑
	 */
	private final class FloatwindowHelper {
		private final View helpView;
		private OpenFloatwindowHelp helper;

		public FloatwindowHelper(View helpView) {
			this.helpView = helpView;
			helpView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (helper != null) {
						helper.doOpenHelp(ActivityMain.this, new OnFinshListener() {

							@Override
							public void onFinish() {
								reset();
							}
						});
					}
				}
			});
		}

		void reset() {
			int visibility;
			if (HelpUIController.isGameInfosEmpty(ActivityMain.this) || !AccelOpenManager.isStarted()) {
				visibility = View.GONE;
				this.helper = null;
			} else {
				this.helper = OpenHelpManager.createHelper();
				visibility = this.helper == null ? View.GONE : View.VISIBLE;
			}
			this.helpView.setVisibility(visibility);
		}
	}

	private void showHelpUIForStartGameInside(final long delayAutoClose) {
		new HelpUIForStartGameInsideShower(delayAutoClose).show();
	}

	private static class HelpUIController {

		/** 如果需要，显示帮助引导UI */
		public static void showHelpUIWhenNeed(final ActivityMain owner) {
			ExpandableListView accelListView = owner.fragmentList.getAccelListView();
			if (accelListView != null) {
				accelListView.postDelayed(new Runnable() {

					@Override
					public void run() {
						if (owner.isAnimationRunning()) {
							return;
						}
						if (showHelpUIForUserCenterPormpt(owner)) {
							return;
						}
						tryShowHelpUIForStartGameInside(owner, 0);
					}
				}, 1500);
			}
		}

		private static boolean showHelpUIForUserCenterPormpt(final ActivityMain owner) {
			if (!AccelOpenManager.isStarted()) {
				return false;
			}
			boolean canShowHelpUI = MainHelp.canShowHelpUI(MainHelp.HelpType.OPEN_USER_CENTER, owner, !isGameInfosEmpty(owner), true);
			if (canShowHelpUI) {
				View menuView = owner.findViewById(R.id.action_user);
				UserCenterPormpt.open(menuView, new Runnable() {

					@Override
					public void run() {
						tryShowHelpUIForStartGameInside(owner, 0);
					}
				});
			}
			return canShowHelpUI;
		}

		/** 如果需要，显示帮助引导UI */
		private static void tryShowHelpUIForStartGameInside(ActivityMain owner, final long delayAutoClose) {
			if (canShowHelpUIForStartGameInside(owner, true)) {
				owner.showHelpUIForStartGameInside(delayAutoClose);
			}
		}

		private static boolean isGameInfosEmpty(ActivityMain owner) {
			FragmentList fragmentList = owner.fragmentList;
			if (fragmentList != null) {
				return fragmentList.getGameInfos().isEmpty();
			}
			return false;
		}

		private static boolean canShowHelpUIForStartGameInside(ActivityMain owner, boolean checkSelfHistoryShown) {
			if (!AccelOpenManager.isStarted()) {
				return false;
			}

			if (MainHelp.canShowHelpUI(MainHelp.HelpType.OPEN_GAME_INSIDE, owner, !isGameInfosEmpty(owner), checkSelfHistoryShown)) {
				return true;
			}
			return false;
		}
	}

	 

	 

	private class MainContainer implements AnimationAccelOn.Controller {
		private final ViewGroup container;
		private final View pieceAccelOn;
		private PieceAccelOff pieceAccelOff;

		private boolean currentAccelState;

		MainContainer() {
			this.pieceAccelOn = findViewById(R.id.piece_accel_on);
			this.container = (ViewGroup) pieceAccelOn.getParent();
			this.currentAccelState = !AccelOpenManager.isStarted();
			this.switchPiece(!currentAccelState);
		}

		@Override
		public void switchPiece(boolean accelOn) {
			if (currentAccelState == accelOn) {
				return;
			}
			currentAccelState = accelOn;
			if (accelOn) {
				pieceAccelOn.setVisibility(View.VISIBLE);
				fragmentNetState.setStateAccelOn();
				fragmentList.refreshGameList();
				fragmentList.setLaunchGamePormptVisible();
				new ResUsageGetter().executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
				if (!isAnimationRunning()) {
					int endIndex = accelOnContentFlipper.getChildCount() - 1;
					if (endIndex > 0 && accelOnContentFlipper.getDisplayedChild() != endIndex) {
						accelOnContentFlipper.setDisplayedChild(endIndex);
					}
					floatwindowHelper.reset();
				}
				if (pieceAccelOff != null) {
					pieceAccelOff.dispose();
					pieceAccelOff = null;
				}
			} else {
				HelpUIForStartGameInside.close();
				pieceAccelOn.setVisibility(View.GONE);
				if (this.pieceAccelOff == null) {
					this.pieceAccelOff = new PieceAccelOff(accelOpenWorker, container);
				}
				this.pieceAccelOff.setTopBotoomShadeHeight(0);
				this.pieceAccelOff.setPormptTextVisibility(View.VISIBLE);
			}
		}

		@Override
		public View getPieceAccelOn() {
			return this.pieceAccelOn;
		}

		@Override
		public PieceAccelOff getPieceAccelOff() {
			return this.pieceAccelOff;
		}

		@Override
		public boolean isCurrentPieceAccelOn() {
			return this.currentAccelState;
		}
	}
	
}
