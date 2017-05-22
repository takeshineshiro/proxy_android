package cn.wsds.gamemaster.ui.floatwindow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.app.GameManager.SecondSegmentNetDelay;
import cn.wsds.gamemaster.data.AppProfile;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.FloatWindowMeasure;
import cn.wsds.gamemaster.data.ProcessCleanRecords;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager;
import cn.wsds.gamemaster.screenshot.ScreenshotManager;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil;
import cn.wsds.gamemaster.tools.ProcessKiller;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowCommon.OutColorType;

import com.subao.collection.Ref;
import com.subao.common.Logger;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.Protocol;
import com.subao.common.utils.CalendarUtils;
import com.subao.net.NetManager;
import com.subao.resutils.WeakReferenceHandler;
import com.subao.utils.FileUtils;

/**
 * 游戏里的小悬浮窗
 */
@SuppressLint("DefaultLocale")
public class FloatWindowInGame extends FloatWindow {

	private static final boolean LOG = false;
	private static final String TAG = "SubaoFloatWindow";

	private static FloatWindowInGame instance;
	private static ToastEx.Strategy strategy ;

	private final GameInfo gameInfo;

	private MyHandler handler;
	private Runnable delayShowCleanMemoryManual;	
	
	/** 这个堆栈负责保存自身的坐标 */
	private final Stack<Point> posStack = new Stack<Point>();

	private final ConfigManager.Observer onConfigChangeObserver = new ConfigManager.Observer() {

		@Override
		public void onShowDelayInFloatWindowChange(boolean show) {
			if (stateNormal != null) {
				stateNormal.changeDelayVisible();
			}
		}
		
		@Override
		public void onAutoCleanProgressSwitchChange(boolean on) {}
		
		@Override
		public void onFloatWindowSwitchChange(boolean on) {}
	};

	private final class OnMainViewClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (instance == null) {
				return;
			}
			if (BoxInGame.exists()) {
				BoxInGame.destroyInstance();
				return;
			}
			showBoxIngame(v, false);
		}
	}

	private final class OutRing {
		private final ImageView imageView;
		private int imgResId;

		public OutRing(ImageView imageView) {
			this.imageView = imageView;
		}

		public void setImageResource(int resId) {
			if (resId != imgResId) {
				imgResId = resId;
				imageView.setImageResource(resId);
			}
		}

		public void setAlpha(float alphaStriking) {
			imageView.setAlpha(alphaStriking);
		}

		public void setVisibility(int visibility) {
			UIUtils.setViewVisibility(imageView, visibility);
		}
	}

	private BoxInGame showBoxIngame(View v, boolean animation) {
		View view = getView();
		if (view == null || view.getVisibility() != View.VISIBLE) {
			return null;
		}
		if (gameInfo == null) {
			return null;
		}
		FloatWindowInGame fw = FloatWindowInGame.this;
		int x = fw.getCenterX();
		int y = fw.getCenterY();
		FloatWindowInGameEar.destoryInstance();
		return BoxInGame.createInstance(v.getContext(), x, y, gameInfo, animation);
	}

	/**
	 * 创建游戏内小悬浮窗单一实例
	 * 
	 * @param context
	 *            Context
	 * @param gameInfo
	 *            哪个游戏
	 * @param x
	 *            初始X位置（如果x和y都取值为负，表示使用缺省位置）
	 * @param y
	 *            初始Y位置（如果x和y都取值为负，表示使用缺省位置）
	 * @param recreateBySkinChange
	 *            是否因为皮肤切换而重建？
	 * @return
	 */
	public static FloatWindowInGame createInstance(Context context, GameInfo gameInfo, int x, int y, boolean recreateBySkinChange) {
		if (gameInfo == null) {
			return null;
		}
		if (LOG) {
			Log.d(TAG, "FloatWindowInGame.createInstance(): " + gameInfo.getPackageName());
		}
		if (instance == null) {
			instance = new FloatWindowInGame(context, gameInfo, recreateBySkinChange);
			View view = LayoutInflater.from(context).inflate(instance.config.getRingLayoutResource(), null);
			if (ScreenshotManager.isScreenshoting()) {
				view.setVisibility(View.INVISIBLE);
			}
			instance.addView(FloatWindow.Type.DRAGGED, view, x, y);
			if (ScreenshotManager.isScreenshoting()) {
				instance.setVisibility(View.GONE);
			}
		}
		return instance;
	}

	/**
	 * 销毁实例，返回坐标（可能为NULL）
	 */
	public static Point destroyInstance() {
		FloatWindowInGame inst = instance;
		instance = null;
		strategy = null ;
		if (inst != null) {
			inst.destroy();
			return new Point(inst.getX(), inst.getY());
		} else {
			return null;
		}
	}

	public static FloatWindowInGame getInstance() {
		return instance;
	}

	/**
	 * 显示或隐藏小悬浮窗实例的View
	 * 
	 * @param visible
	 *            与 View.setVisibily()函数参数相同的值
	 */

	private int visibleCount = 0;
//	final boolean isCurrenVisibleHunterSkin;
	private final boolean recreateBySkinChange;

	@Override
	public void setVisibility(int visibility) {
		if (visibility == View.GONE || visibility == View.INVISIBLE) {
			visibleCount -= 1;
		} else {
			visibleCount += 1;
		}
		int currentVisibility;
		if (visibleCount >= 0) {
			currentVisibility = View.VISIBLE;
		} else {
			currentVisibility = View.GONE;
			FloatWindowInGameEar.destoryInstance();
		}
		super.setVisibility(currentVisibility);
	}

	public static void setInstanceVisibility(int visibility) {
		if (BoxInGame.exists()) {
			BoxInGame.destroyInstance();
		}
		if (instance != null) {
			instance.setVisibility(visibility);
		}
	}


	private FloatWindowInGame(Context context, GameInfo gameInfo, boolean recreateBySkinChange) {
		super(context);
		ConfigManager.getInstance().registerObserver(onConfigChangeObserver);
		this.gameInfo = gameInfo;
		this.recreateBySkinChange = recreateBySkinChange;
//		this.isCurrenVisibleHunterSkin = FloatWindowCommon.isCurrenVisibleHunterSkin(gameInfo);
		config = FloatwindowInGameConfig.createConfig(FloatWindowMeasure.getCurrentType());
		handler = new MyHandler(this);
	}

	@Override
	protected boolean canDrag() {
		return !BoxInGame.exists();
	}

	/** 根容器 */
	private ViewGroup root;

	/** 外圈 */
	private OutRing outRing;

	/** 内部内容 */
	private View inner;

	/** 当前网络类型 */
	private NetTypeDetector.NetType currentNetType;

	/** 外圈颜色 */
	private OutColorType outColorType;
	
	private FloatwindowInGameConfig config;

	public FloatwindowInGameConfig getConfig() {
		return config;
	}

	/**
	 * 重置外观（外圈的图片），以及{@link currentNetType}和{@link outColorType}的值
	 */
	private void resetAppear() {
		outColorType = getOutColorType(this.gameInfo, currentNetType);
		outRing.setImageResource(config.getRingResource(outColorType));
//		if (!isCurrenVisibleHunterSkin) {
			FloatWindowInGameEar.setInstanceRectColor(outColorType);
//		}
	}

	/**
	 * 根据当前情况，决定小悬浮窗的外圈颜色
	 * 
	 * @param gameInfo
	 *            当前是哪个游戏？
	 * @param netState
	 *            网络类型
	 * @return {@link OutColorType}
	 */
	private static OutColorType getOutColorType(GameInfo gameInfo, NetTypeDetector.NetType netState) {
		// 断网：红色
		if (NetManager.getInstance().isDisconnected()) {
			return OutColorType.Red;
		}
		// gameInfo 为空：灰色
		if (gameInfo == null) {
			return OutColorType.Grey;
		}
		// 未开加速：灰色
		if (!AccelOpenManager.isStarted()) {
			return OutColorType.Grey;
		}
		// 2G：绿色
		if (netState == NetTypeDetector.NetType.MOBILE_2G) {
			return OutColorType.Green;
		}
		// 第一段延迟如果异常，红色
		int first = GameManager.getInstance().getFirstSegmentNetDelay();
		if (FloatWindowCommon.isNetDelayException(first)) {
			return OutColorType.Red;
		}
		// 第二段延迟如果异常，红色
		int second = GameManager.getInstance().getSecondSegmentNetDelay(gameInfo.getUid()).getDelayValue();
		if (FloatWindowCommon.isNetDelayException(second)) {
			return OutColorType.Red;
		}
		// 如果第一段延迟是“-2（等待测速）”，则转圈，绿色
		if (first == GlobalDefines.NET_DELAY_TEST_WAIT) {
			return OutColorType.Green;
		}
		// 和值
		int sum = first;
		if (second >= 0) {
			sum += second;
		}
		if (sum >= GlobalDefines.NET_DELAY_TIMEOUT) {
			return OutColorType.Red;
		}
		// 正常的和值，根据不同的网络环境决定范围
		int bad, normal;
		if (gameInfo.isForeignGame()) {
			if (netState == NetTypeDetector.NetType.WIFI) {
				bad = 550;
				normal = 480;
			} else {
				bad = 700;
				normal = 550;
			}
		} else {
			if (netState == NetTypeDetector.NetType.WIFI) {
				bad = 200;
				normal = 130;
			} else {
				bad = 350;
				normal = 200;
			}
		}
		return getOutColorTypeByDelaySection(sum, bad, normal);
	}

	private static OutColorType getOutColorTypeByDelaySection(long delay, int bad, int normal) {
		if (delay > bad) {
			return OutColorType.Red;
		}
		if (delay > normal) {
			return OutColorType.Yello;
		}
		return OutColorType.Green;
	}

	/**
	 * 网络发生改变的时候，要显示一下“耳朵”
	 */
	private void showNetChanged(NetTypeDetector.NetType oldType, NetTypeDetector.NetType newType) {
		boolean changed = false;
		if (isMobileNet(oldType)) {
			if (newType == NetTypeDetector.NetType.WIFI) {
				changed = true;
			}
		} else if (oldType == NetTypeDetector.NetType.WIFI) {
			if (isMobileNet(newType)) {
				changed = true;
			}
		}
		resetAppear();
		if(CleanMemoryManual.isSaucerInForeground()){
			return;
		}
		if (changed && !BoxInGame.exists() && visibleCount >= 0) {
			if (FloatWindowInGameEar.exists()) {
				FloatWindowInGameEar.destoryInstance();
			}
			FloatWindowInGameEar.createInstance(getContext(), getCenterX(), getY(), newType, outColorType);
		}
	}

	private boolean isMobileNet(NetTypeDetector.NetType netType) {
		switch (netType) {
		case MOBILE_2G:
		case MOBILE_3G:
		case MOBILE_4G:
		case UNKNOWN:
			return true;
		default:
			return false;
		}
	}

	//	private void adjustTextValueAndSpaceSize(int normalValueSize, int normalUnitSize, int miniValueSize,
	//		int miniUnitSize, float normalLp, float miniLp) {
	//		int lpHeight;
	//		switch (FloatWindowMeasure.getCurrentType()) {
	//		case MINI:
	//			adjustText(miniValueSize, miniUnitSize);
	//			lpHeight = MetricsUtils.dp2px(getContext(), miniLp);
	//			break;
	//		default:
	//			adjustText(normalValueSize, normalUnitSize);
	//			lpHeight = MetricsUtils.dp2px(getContext(), normalLp);
	//			break;
	//		}
	//		//设置控件高度
	//		LayoutParams lp = delayCenterAnchorNormal.getLayoutParams();
	//		lp.height = lpHeight;
	//		delayCenterAnchorNormal.setLayoutParams(lp);
	//	}

	/**
	 * 根据当前的尺寸设置调整大小
	 * 
	 */
	private void adjustSize(int x, int y) {
		config = FloatwindowInGameConfig.createConfig(FloatWindowMeasure.getCurrentType());
		outRing.setImageResource(config.getRingResource(outColorType));
		this.reLayout(x, y, config.getWidthPx(), config.getHeightPx());
		if (currentState != null) {
			currentState.resize();
		}
	}

	public static void adjustInstanceSize(int x, int y) {
		if (instance != null) {
			instance.adjustSize(x, y);
		}
	}

	@Override
	protected void onViewAdded(View v) {
		currentNetType = NetManager.getInstance().getCurrentNetworkType();
		root = (ViewGroup) v.findViewById(R.id.view_group);
		outRing = new OutRing((ImageView) v.findViewById(R.id.float_window_out_ring));
		inner = v.findViewById(R.id.float_window_inner);
		//
		if (recreateBySkinChange) {
			enterAnimation();
		}

		v.setOnClickListener(new OnMainViewClickListener());

		int x = this.getX();
		int y = this.getY();
		if (x < 0 && y < 0) {
			// 如果XY都是负数，表示用缺省位置
			DisplayMetrics dm = new DisplayMetrics();
			windowManager.getDefaultDisplay().getMetrics(dm);
			if (dm.widthPixels < dm.heightPixels) {
				x = dm.widthPixels - this.getWidth();
				y = dm.heightPixels / 5;
			} else {
				x = 0;
				y = dm.heightPixels * 2 / 3;
			}
			y -= (this.getHeight() >> 2);
		}

		resetAppear();
		adjustSize(x, y);
		setStrikingOn(true);

		stateNormal = new StateNormal();
		changeStateIfNeed();

		if (myEventObserver == null) {
			myEventObserver = new MyEventObserver();
			TriggerManager.getInstance().addObserver(myEventObserver);
		}
		
		LiveTime.instance.onCreate(); // LiveTime开始计时
		//
		handler.sendEmptyMessageDelayed(MyHandler.MSG_FAKE_SECOND_DELAY, 7777);
	}


	
	@Override
	protected void destroy() {
		LiveTime.instance.onDestroy(); // LiveTime停止计时
		ConfigManager.getInstance().unregisterObserver(onConfigChangeObserver);
		if (handler != null) {
			handler.removeCallbacksAndMessages(null);
			handler = null;
		}
		if (myEventObserver != null) {
			TriggerManager.getInstance().deleteObserver(myEventObserver);
			myEventObserver = null;
		}
		// 记录位置
		saveLastPosition();
		CleanMemoryManual.destoryInstance();

		if (currentState != null) {
			currentState.cleanup();
			currentState = null;
		}

		BoxInGame.destroyInstance();
		FloatWindowInGameEar.destoryInstance();
		super.destroy();
	}

	private void saveLastPosition() {
		int x, y;
		if (BoxInGame.exists() && !posStack.isEmpty()) {
			Point pos = posStack.pop();
			x = pos.x;
			y = pos.y;
		} else {
			x = getX();
			y = getY();
		}

		if (gameInfo != null) {
			gameInfo.setWindowX(x);
			gameInfo.setWindowY(y);
		}
	}

	private void recordActivated() {
		ConfigManager config = ConfigManager.getInstance();
		config.setFloatWindowActivated(); // 悬浮窗已激活。一些功能（如来电拦截）受此标志影响
		//
		String param;
		switch (MobileSystemTypeUtil.getSystemType()) {
		case MIUI:
			param = "M";
			break;
		case EMUI:
			param = "H";
			break;
		default:
			return;
		}
		int lastDay = config.getDayReportFloatWindowActivate();
		int today = CalendarUtils.todayLocal();
		if (today != lastDay) {
			Statistic.addEvent(getContext(), Statistic.Event.FLOATING_WINDOW_DRAG_MI, param);
			config.setDayReportFloatWindowActivate(today);
		}
	}

	@Override
	protected void onTouchDown() {
		super.onTouchDown();
		setStrikingOn(false);
		recordActivated();
		if (delayShowCleanMemoryManual == null) {
			View v = getView();
			if (v != null) {
				delayShowCleanMemoryManual = new Runnable() {

					@Override
					public void run() {
						showCleanMemoryManual();
					}
				};
				v.postDelayed(delayShowCleanMemoryManual, 300);
			}
		}
	}
	
	private void showCleanMemoryManual(){
		if(!BoxInGame.exists()){
			savePosition();
			CleanMemoryManual.createInstance(getContext(), getX(), getY());
		}
	}

	@Override
	protected void onTouchUp() {
		super.onTouchUp();
		onFingerLevel();
		cleanMemory();
	}

	private void onFingerLevel() {
		setStrikingOn(true);
		if(delayShowCleanMemoryManual!=null){
			View v = getView();
			if (v != null) {
				v.removeCallbacks(delayShowCleanMemoryManual);
			}
			delayShowCleanMemoryManual = null;
		}
	}

	private void cleanMemory() {
		CleanMemoryManual cleanMemoryManual = CleanMemoryManual.getInstance();
		if(cleanMemoryManual != null){
			if(cleanMemoryManual.canClean(getCenterX(),getCenterY())){
				Set<String> packageNameList = ProcessCleanRecords.getInstance().getCleanRecord(null);
				String showContent = killProcess(packageNameList);
				Statistic.addEvent(getContext(), Statistic.Event.FLOATING_WINDOW_CLEAR_RAM);
				cleanMemoryManual.clean(showContent);
			}else{
				CleanMemoryManual.destoryInstance();
			}
			cleanMemoryManual = null;
		}
	}

	@Override
	protected void onTouchCancel() {
		super.onTouchCancel();
		onFingerLevel();
		CleanMemoryManual.destoryInstance();
	}

	@Override
	protected void onDragBegin() {
		if (LOG) {
			Log.d(TAG, "onDragBegin()");
		}
		recordActivated();
		FloatWindowInGameEar.destoryInstance();

		if (delayShowCleanMemoryManual != null) {
			View v = getView();
			if (v != null) {
				v.removeCallbacks(delayShowCleanMemoryManual);
			}
			delayShowCleanMemoryManual = null;
			showCleanMemoryManual();
			cleanMemoryOpenTheDoor();
		} else {
			cleanMemoryOpenTheDoor();
		}
	}

	private void cleanMemoryOpenTheDoor() {
		CleanMemoryManual cleanMemoryManual = CleanMemoryManual.getInstance();
		if(cleanMemoryManual!=null){
			cleanMemoryManual.openTheDoor();
		}
	}

	private static final long MIN_KILLING_INTERVAL = 2 * 60 * 1000;
	private static long lastcleanTime = 0;

	@SuppressLint("DefaultLocale")
	private String killProcess(Set<String> packageNameList) {
		long now = System.currentTimeMillis();
		if (now - lastcleanTime <= MIN_KILLING_INTERVAL || packageNameList.isEmpty()) {
			return "当前内存已是最优状态";
		}
		lastcleanTime = now;
		if (!ProcessKiller.execute(AppMain.getContext(), packageNameList)) {
			return "请检查迅游手游是否被禁用了相关权限";
		}
		int count = packageNameList.size();
		int percent = UIUtils.getAccelPercent(count);
		if (count > 0) {
			return String.format("清理了%d个进程，手机加速%d%%", count, percent);
		}
		return String.format("手机加速%d%%", percent);
	}


	public void savePosition() {
		posStack.push(new Point(this.getX(), this.getY()));
	}

	public void restorePosition() {
		if (!posStack.empty()) {
			Point pos = posStack.pop();
			this.setPosition(pos.x, pos.y);
		}
	}

	private static class MyHandler extends WeakReferenceHandler<FloatWindowInGame> {
		private static final int MSG_STRIKING_OFF = 1;
		private static final int MSG_FAKE_SECOND_DELAY = 2;
		
		public MyHandler(FloatWindowInGame ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(FloatWindowInGame ref, Message msg) {
			switch (msg.what) {
			case MSG_STRIKING_OFF:
				if (!FloatWindowCommon.isNetworkException()) {
					ref.changeStrikingState(false);
				}
				break;
			case MSG_FAKE_SECOND_DELAY:
				fakeSecondDelay(ref);
				break;
			default:
				break;
			}
		}
		
		private static boolean needFake(GameInfo gameInfo) {
			if (gameInfo == null) {
				return false;
			}
			if (!AccelOpenManager.isStarted()) {
				return false;
			}
			if (gameInfo.isAccelFake()) {
				// 假加速游戏，需要人造数据
				return true;
			}
			if (Protocol.TCP == gameInfo.getProtocolType()) {
				// 只加速UDP的游戏，需要人造数据
				return true;
			}
			// 长时间没有真实数据的
			long elapsed = GameManager.getInstance().getElapsedTimeSinceLastSecondDelayUpdate(gameInfo.getUid());
			if (elapsed >= GameManager.SecondSegmentNetDelay.TIMEOUT_TRUE_DATA) {
				return true;
			}
			return false;
		}
		
		private void fakeSecondDelay(FloatWindowInGame ref) {
			GameInfo gameInfo = ref.gameInfo;
			if (gameInfo == null || !needFake(gameInfo)) {
				return;
			}
			//
			int low, high;
			if (gameInfo.isForeignGame()) {
				low = 150;
				high = 300;
			} else {
				low = 15;
				high = 35;
			}
			GameManager.getInstance().onSecondSegmentNetDelayChange(gameInfo.getUid(), low + (int)Math.round(Math.random() * (high - low)), false);
			this.sendEmptyMessageDelayed(MSG_FAKE_SECOND_DELAY, GameManager.SecondSegmentNetDelay.TIMEOUT_TRUE_DATA);
		}
	}

	/** 根据两段延迟值计算最终小悬浮窗里要显示的值 */
	private static int mergeTwoSectionDelayValue(int uid, Ref<Boolean> firstOnly) {
		int first = GameManager.getInstance().getFirstSegmentNetDelay();
		if (first < 0 || first >= GlobalDefines.NET_DELAY_TIMEOUT) {
			if (firstOnly != null) {
				firstOnly.set(true);
			}
			return first;
		}
		GameManager.SecondSegmentNetDelay secondDelay = GameManager.getInstance().getSecondSegmentNetDelay(uid);
		int second = secondDelay.getDelayValue();
		if (second > 0) {
			if (firstOnly != null) {
				firstOnly.set(false);
			}
			return first + second;
		}
		if (firstOnly != null) {
			firstOnly.set(true);
		}
		return first;
	}

	private boolean setStrikingOn(boolean autoOff) {
		if (handler == null) {
			return false;
		}
		handler.removeMessages(MyHandler.MSG_STRIKING_OFF);
		if (autoOff) {
			handler.sendEmptyMessageDelayed(MyHandler.MSG_STRIKING_OFF, 3000);	// 3秒后无操作转半透明
		}
		return changeStrikingState(true);
	}

	public static void setInstanceStrikingOn(boolean autoOff) {
		if (instance != null) {
			instance.setStrikingOn(autoOff);
		}
	}

	private static final float ALPHA_STRIKING = 1.0f;
	private static final float ALPHA_UN_STRIKING = 0.8f;

	private boolean changeStrikingState(boolean on) {
		View v = getView();
		if (on || BoxInGame.exists() || FloatWindowInGameEar.exists()) {
			if (v != null && Math.abs(v.getAlpha() - ALPHA_STRIKING) > 0.01f) {
				v.setAlpha(ALPHA_STRIKING);
				outRing.setAlpha(ALPHA_STRIKING);
				inner.setAlpha(ALPHA_STRIKING);
				return true;
			}
		} else {
			if (handler != null) {
				handler.removeMessages(MyHandler.MSG_STRIKING_OFF);
			}
			if (v != null && Math.abs(v.getAlpha() - ALPHA_STRIKING) < 0.01f) {
				v.setAlpha(ALPHA_UN_STRIKING);
				outRing.setAlpha(ALPHA_UN_STRIKING);
				inner.setAlpha(ALPHA_UN_STRIKING);
				return true;
			}
		}
		return false;
	}

	//	private final WifiManager wifiManager;
	//	private final TelephonyManager telephonyManager;
	//	private MyPhoneStateListener myPhoneStateListener;
	private MyEventObserver myEventObserver;

	private class MyEventObserver extends EventObserver {

		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			if (currentNetType != state) {
				NetTypeDetector.NetType oldType = currentNetType;
				currentNetType = state;
				showNetChanged(oldType, state);
				currentState.onNetChange();
			}
		}
		
		@Override
		public void onAutoProcessClean(List<AppProfile> runningAppList) {
			Misc.cleanMemory(getContext(), runningAppList, true, getX(), getY());
		}

		@Override
		public void onFirstSegmentNetDelayChange(int delayMilliseconds) {
			currentState.onNetDelayChange();
		}

		@Override
		public void onSecondSegmentNetDelayChange(int uid, SecondSegmentNetDelay secondDelay) {
			if (gameInfo != null && uid == gameInfo.getUid()) {
				currentState.onNetDelayChange();
			}
		}

		@Override
		public void onAccelSwitchChanged(boolean on) {
			currentState.onAccelSwitchChanged(on);
		}
	};

	private State currentState;
	private StateNormal stateNormal;

	private void changeState(State state) {
		if (currentState != null) {
			currentState.cleanup();
		}
		currentState = state;
		if (currentState != null) {
			if (currentState.isContentDynamic()) {
				UIUtils.setViewVisibility(inner, View.INVISIBLE);
			} else {
				UIUtils.setViewVisibility(inner, View.VISIBLE);
			}
			currentState.init();
		}
		resetAppear();
	}
	
	private Class<? extends State> whichStateWillPresent() {
		// 断网，优先级最高
		if (NetManager.getInstance().isDisconnected()) {			
			return StateException.class;
		}
		// 未开加速
		if (!AccelOpenManager.isStarted()) {		
			return StateAccelOff.class;
		}
		// 悬浮窗刚创建，且不是因为皮肤切换才创建的，转到首次动效状态（转圈1分钟）
		if (currentState == null && !recreateBySkinChange) {			
			return StateFirstEffect.class;
		}
		// 2G
		if (FloatWindowCommon.isCurrent2G()) {		
			return State2G.class;
		}
		// 第一段延迟 = -1？
		int first = GameManager.getInstance().getFirstSegmentNetDelay();
		if (first == GlobalDefines.NET_DELAY_TEST_FAILED) {			
			return StateException.class;
		}		
		// 第二段延迟 = -1？
		int second = GameManager.getInstance().getSecondSegmentNetDelay(gameInfo == null ? -1 : gameInfo.getUid()).getDelayValue();
		if (second == GlobalDefines.NET_DELAY_TEST_FAILED) {			
			return StateException.class;
		}
		// 第一段2000
		if (first >= GlobalDefines.NET_DELAY_TIMEOUT) {
			if (second >= GlobalDefines.NET_DELAY_TIMEOUT) {				
				return StateException.class;
			} else {				 
				return StateTimeout.class;
			}
		}
		// 第二段2000
		if (second >= GlobalDefines.NET_DELAY_TIMEOUT) {
			return StateTimeout.class;
		}
		// 第一段是-2
		if (first == GlobalDefines.NET_DELAY_TEST_WAIT) {
			return StateWait.class;
		}
		// 余下的情况		
		strategy = null ;
		return StateNormal.class;
	}
		
		
	private boolean changeStateIfNeed() {
		Class<? extends State> cls = whichStateWillPresent();
		if (currentState != null && cls.equals(currentState.getClass())) {
			return false;
		}
		//
		// 考虑到兼容性，不用反射机制来创建新类
		if (cls.equals(StateNormal.class)) {
			changeState(stateNormal);
		} else if (cls.equals(StateException.class)) {
			changeState(new StateException());
		} else if (cls.equals(StateWait.class)) {
			changeState(new StateWait());
		} else if (cls.equals(State2G.class)) {
			changeState(new State2G());
		} else if (cls.equals(StateAccelOff.class)) {
			changeState(new StateAccelOff());
		} else if (cls.equals(StateFirstEffect.class)) {
			changeState(new StateFirstEffect());
		} else if (cls.equals(StateTimeout.class)) {
			changeState(new StateTimeout());
		} else {
			changeState(stateNormal);
		}
		return true;
	}

	/**
	 * 状态的抽象基类
	 */
	private abstract class State {

		/**
		 * 本状态的内容是否动态创建的？（未包含在XML布局文件里）
		 * <p>
		 * 如果是，则inner这个层级的View将隐藏
		 * </p>
		 */
		public abstract boolean isContentDynamic();

		/** 初始化。每次changeState()时，新的State.init()将被调用 */
		public abstract void init();

		/** 清理。每次changeState()时，老的State.cleanup()将被调用 */
		public abstract void cleanup();

		/**
		 * 当网络类型发生改变时被调用
		 * 
		 * @return true表示已切换到另一个状态了
		 */
		public boolean onNetChange() {
			return changeStateIfNeed();
		}

		/**
		 * 当第一段网络延迟发生改变时被调用
		 * 
		 * @return true表示已切换到另一个状态了
		 */
		public boolean onNetDelayChange() {
			return changeStateIfNeed();
		}

		/**
		 * 当加速开或关的时候被调用
		 * 
		 * @return true表示已切换到另一个状态了
		 */
		public boolean onAccelSwitchChanged(boolean on) {
			return changeStateIfNeed();
		}

		/**
		 * 重新调整大小
		 */
		public abstract void resize();
	}

	private class StateNormal extends State {

		private final TextView delayValueNormal;
		private final TextView delayUnitNormal;
		private final View delayCenterAnchorNormal;
		private ImageView rocket;
		private int rocketResId;

		private final ColorStateList colorUseTwoSectionDelay;
		private final ColorStateList colorUseFirstSectionDelay;

		private final Ref<Boolean> refBoolean = new Ref<Boolean>();

		public StateNormal() {
			View v = getView();
			delayValueNormal = (TextView) v.findViewById(R.id.float_window_delay_value0);
			delayUnitNormal = (TextView) v.findViewById(R.id.float_window_delay_unit0);
			delayCenterAnchorNormal = v.findViewById(R.id.float_window_delay_center0);
			//
			colorUseTwoSectionDelay = delayValueNormal.getTextColors();
			colorUseFirstSectionDelay = ColorStateList.valueOf(0xff9ab6cc);
		}

		@Override
		public boolean isContentDynamic() {
			return false;
		}

		@Override
		public void resize() {
			delayValueNormal.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.getDelayValueTextSizeSp());
			delayUnitNormal.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.getDelayUnitTextSizeSp());
			int lpHeight = config.getDelayViewHeightPx();
			LayoutParams lp = delayCenterAnchorNormal.getLayoutParams();
			lp.height = lpHeight;
			delayCenterAnchorNormal.setLayoutParams(lp);
			resetRocketImage();
		}

		private void resetRocketImage() {
			if (rocket != null) {
				int resId = config.getRocketResource(false);
				if (resId != rocketResId) {
					rocketResId = resId;
					rocket.setImageResource(resId);
				}
			}
		}

		@Override
		public void init() {
			changeDelayVisible();
			resize();
			showDelay();
		}

		@Override
		public void cleanup() {}

		@Override
		public boolean onNetDelayChange() {
			if (super.onNetDelayChange()) {
				return true;
			}
			resetAppear();
			showDelay();
			return false;
		}

		/** 显示延迟值 */
		private void showDelay() {
			if (!ConfigManager.getInstance().getFloatwindowSwitchDelay()) {
				return;
			}
			//
			int delay = mergeTwoSectionDelayValue(gameInfo == null ? -1 : gameInfo.getUid(), refBoolean);
			String strValue;
			boolean hasUnit;
			if (delay < 0) {
				strValue = "---";
				hasUnit = false;
			} else if (delay >= 1000) {
				strValue = String.format(Locale.getDefault(), "%.1fs", delay * 0.001f);
				hasUnit = false;
			} else {
				strValue = Long.toString(delay);
				hasUnit = true;
			}
			UIUtils.setViewText(delayValueNormal, strValue);
			//
			ColorStateList textColor = refBoolean.get() ? colorUseFirstSectionDelay : colorUseTwoSectionDelay;
			UIUtils.setViewTextColor(delayValueNormal, textColor);
			UIUtils.setViewTextColor(delayUnitNormal, textColor);
			if (hasUnit) {
				if (UIUtils.setViewVisibility(delayUnitNormal, View.VISIBLE)) {
					RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
					layoutParams.addRule(RelativeLayout.ALIGN_TOP, delayCenterAnchorNormal.getId());
					delayValueNormal.setLayoutParams(layoutParams);
				}
			} else {
				if (UIUtils.setViewVisibility(delayUnitNormal, View.GONE)) {
					RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
					delayValueNormal.setLayoutParams(layoutParams);
				}
			}
		}

		private void changeDelayVisible() {
			int delayValueVisible;
			boolean rocketVisible;
			if (ConfigManager.getInstance().getFloatwindowSwitchDelay()) {
				rocketVisible = false;
				delayValueVisible = View.VISIBLE;
			} else {
				rocketVisible = true;
				delayValueVisible = View.GONE;
			}
			delayValueNormal.setVisibility(delayValueVisible);
			delayUnitNormal.setVisibility(delayValueVisible);
			delayCenterAnchorNormal.setVisibility(delayValueVisible);
			//
			ViewGroup parent = (ViewGroup) delayValueNormal.getParent();
			if (rocketVisible) {
				if (rocket == null) {
					rocket = new ImageView(getContext());
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT);
					lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
					parent.addView(rocket, lp);
					resetRocketImage();
				}
				rocket.setVisibility(View.VISIBLE);
			} else if (rocket != null) {
				parent.removeView(rocket);
				rocket = null;
				rocketResId = 0;
			}
		}
	}

	/**
	 * 某些不常出现的状态，动态创建一个View显示出来，以节省资源
	 */
	private abstract class StateSingleView extends State {

		private View view;

		@Override
		public boolean isContentDynamic() {
			return true;
		}

		@Override
		public void init() {
			if (view == null) {
				view = createView();
				if (view != null) {
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT);
					lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
					root.addView(view, lp);
				}
			}
		}

		@Override
		public void cleanup() {
			if (view != null) {
				root.removeView(view);
				view = null;
			}
		}

		protected abstract View createView();

	}

	private abstract class StateSingleViewImage extends StateSingleView {

		private ImageView image;

		protected abstract int getImageResId();

		@Override
		protected View createView() {
			if (image == null) {
				image = new ImageView(getContext());
				resetImage();
			}
			return image;
		}

		@Override
		public void resize() {
			if (image != null) {
				resetImage();
			}
		}

		private void resetImage() {
			image.setImageResource(getImageResId());
		}

	}

	/** 加速未开启时的状态 */
	private class StateAccelOff extends StateSingleViewImage {

		@Override
		protected int getImageResId() {
			return config.getAccelResource();
		}

	}

	/** 网络有异常时的状态 */
	private class StateException extends StateSingleViewImage {

		@Override
		protected int getImageResId() {
			return config.getClaimResource();
		}

	}

	/**
	 * 单行文本
	 */
	private abstract class StateSingleLineText extends StateSingleView {

		private TextView textView;
		
		private void resetTextSize() {
			if (textView != null) {
				textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.getDelayValueTextSizeSp());
			}
		}

		@Override
		protected View createView() {
			if (textView == null) {
				textView = new TextView(getContext());
				textView.setText(getText());
				textView.setTextColor(getContext().getResources().getColor(R.color.color_game_7));
			}
			resetTextSize();
			return textView;
		}

		@Override
		public void resize() {
			resetTextSize();
		}
		
		protected abstract String getText();
	}

	private abstract class StateRotateRing extends StateSingleView {
		private boolean animationStopped;
		private ImageView ring, rocket;

		protected boolean isAnimationStopped() {
			return animationStopped;
		}

		@Override
		protected View createView() {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			View view = inflater.inflate(R.layout.small_float_window_ring_child_wait, root, false);
			ring = (ImageView) view.findViewById(R.id.ring);
			rocket = (ImageView) view.findViewById(R.id.rocket);
			setImage();
			return view;
		}

		private void setImage() {
			if (ring != null) {
				ring.setImageResource(config.getWaitRingResource());
			}
			if (rocket != null) {
				rocket.setImageResource(config.getRocketResource(true));
			}
		}

		/**
		 * 转圈动画持续多长时间？
		 */
		protected abstract long getAnimationDuration();
		
		protected void onAnimationStop() {};

		@Override
		public void init() {
			super.init();
			//
			outRing.setVisibility(View.INVISIBLE);
			long duration = getAnimationDuration();
			RotateAnimation animation = new RotateAnimation(0f, 0.36f * duration, 
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
			animation.setDuration(duration);
			animation.setInterpolator(new LinearInterpolator());
			animation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationEnd(Animation animation) {
					stopAnimation();
				}
			});
			ring.startAnimation(animation);
		}

		@Override
		public void resize() {
			setImage();
			stopAnimation();
		}

		@Override
		public void cleanup() {
			super.cleanup();
			outRing.setVisibility(View.VISIBLE);
		}

		private void stopAnimation() {
			if (!animationStopped) {
				animationStopped = true;
				if (ring != null) {
					Animation ani = ring.getAnimation();
					ring.setAnimation(null);
					if (ani != null) {
						ani.cancel();
					}
					ring.setVisibility(View.GONE);
					outRing.setVisibility(View.VISIBLE);
				}
				if (rocket != null) {
					Drawable d = rocket.getDrawable();
					if (d != null && (d instanceof AnimationDrawable)) {
						((AnimationDrawable) d).stop();
					}
				}
				onAnimationStop();
			}
		}
	}

	/**
	 * 等待测速结果
	 */
	private class StateWait extends StateRotateRing {
		@Override
		protected long getAnimationDuration() {
			return 5000;
		}

		@Override
		protected void onAnimationStop() {
			super.onAnimationStop();
			if((currentState != null) && 
					StateWait.class.equals(currentState.getClass())){
				showBadNetStateRemind();
			}
		}
	}

	/**
	 * 首次进入转圈动画
	 */
	private class StateFirstEffect extends StateRotateRing {
		@Override
		protected long getAnimationDuration() {
			return 1000;
		}

		@Override
		public boolean onNetDelayChange() {
			if (isAnimationStopped()) {
				return super.onNetDelayChange();
			} else {
				return false;
			}
		}
		
		@Override
		protected void onAnimationStop() {
			changeStateIfNeed();
		}
	}
	
	private class State2G extends StateSingleLineText {
		@Override
		protected String getText() {
			return "2G";
		}
	}
	
	private class StateTimeout extends StateSingleLineText {

		@Override
		protected String getText() {
			return ">2s";
		}
		
	}

//	public void redisplay() {
//		AnimationListener listener = new AnimationListener() {
//
//			@Override
//			public void onAnimationStart(Animation animation) {}
//
//			@Override
//			public void onAnimationRepeat(Animation animation) {}
//
//			@Override
//			public void onAnimationEnd(Animation animation) {
//				if (gameInfo == null) {
//					return;
//				}
//				Context context = getContext();
//				Point pos;
//				if (posStack.empty()) {
//					pos = new Point(getX(), getY());
//				} else {
//					pos = posStack.peek();
//				}
//				destroyInstance();
//				createInstance(context, gameInfo, pos.x, pos.y, true);
//			}
//		};
//		Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.floatwindow_exit);
//		root.startAnimation(animation);
//		animation.setAnimationListener(listener);
//	}

	private void enterAnimation() {
		Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.floatwindow_enter);
		root.startAnimation(animation);
		BoxInGame boxIngame = showBoxIngame(instance.getView(), true);
		if (boxIngame != null) {
			boxIngame.openSettingPage();
		}
	}
	
	/**
	 * 小悬浮窗生存时长统计相关
	 */
	private static class LiveTime {
		private static final String FILE_NAME = "fwnd_live";
		
		private static final LiveTime instance = new LiveTime();
		
		/** 最近一次上报事件是哪天？ */
		private int dayReport;
		
		/** 最近一次上报的生存时长是多少？ */
		private long timeReport;
		
		/** 累计生存时长 */
		private long timeAmount;
		
		private File file;
		private Timer timer; 
		
		private static class Saver implements Runnable {
			private final File file;
			private final int dayReport;
			private final long timeReport;
			private final long timeAmount;
			
			public Saver(File file, int dayReport, long timeReport, long timeAmount) {
				this.file = file;
				this.dayReport = dayReport;
				this.timeReport = timeReport;
				this.timeAmount = timeAmount;
			}
			
			@Override
			public void run() {
				FileOutputStream output = null;
				try {
					output = new FileOutputStream(file);
					ByteBuffer bb = ByteBuffer.allocate(4 + 8 + 8);
					bb.order(ByteOrder.BIG_ENDIAN);
					bb.putInt(dayReport);
					bb.putLong(timeReport);
					bb.putLong(timeAmount);
					output.write(bb.array(), 0, bb.position());
					if (Logger.isLoggableDebug(TAG)) {
						Logger.d(TAG, String.format("Save: day=%d, timeReport=%d, timeAmount=%d", dayReport, timeReport, timeAmount));
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					com.subao.utils.Misc.safeClose(output);
				}
			}
		}
		
		private static class Timer implements Runnable {
			
			private long beginTime = now();
			
			@Override
			public void run() {
				Logger.d(TAG, "Timer run()");
				countTime();
				postNextRun();
			}

			public void countTime() {
				long now = now();
				LiveTime.instance.incTime(now - beginTime);
				beginTime = now;
			}
			
			public void postNextRun() {
				MainHandler.getInstance().postDelayed(this, 1000 * 60 * 5);
			}
			
			public void cancelAllPendingRun() {
				MainHandler.getInstance().removeCallbacks(this);
			}
		}
		
		private LiveTime() {
			this.file = FileUtils.getDataFile(FILE_NAME);
			load();
		}
		
		public void onCreate() {
			if (timer == null) {
				checkReportNeed();
				timer = new Timer();
				timer.postNextRun();
				Logger.d(TAG, "Start ...");
			}
		}
		
		public void onDestroy() {
			if (timer != null) {
				timer.cancelAllPendingRun();
				timer.countTime();
				timer = null;
				Logger.d(TAG, "Stopped");
			}
		}
		
		private void incTime(long deltaTime) {
			this.timeAmount += deltaTime;
			checkReportNeed();
			save();
		}

		private void save() {
			AsyncTask.SERIAL_EXECUTOR.execute(new Saver(file, dayReport, timeReport, timeAmount));
		}

		private void checkReportNeed() {
			int today = CalendarUtils.todayLocal();
			if (today != dayReport) {
				if (dayReport != 0) {
					report();
					timeReport = timeAmount;
				}
				dayReport = today;
				save();
			}
		}

		private void report() {
			long time = timeAmount - timeReport;
			if (time < 0) {
				time = 0;
			} else if (time > 24 * 3600 * 1000) {
				time = 24 * 3600 * 1000;
			}
			// 折算成粒度为10分钟
			int minutes = (int)time / (1000 * 60);
			minutes = minutes / 10 * 10;
			String param = Integer.toString(minutes);
			Statistic.addEvent(AppMain.getContext(), Statistic.Event.FLOATING_WINDOW_DISPLAY_TIME, param);
			//
			String param2;
			switch (MobileSystemTypeUtil.getSystemType()) {
			case MIUI:
				param2 = "M";
				break;
			case EMUI:
				param2 = "H";
				break;
			default:
				param2 = null;
				break;
			}
			if (param2 != null) {
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.FLOATING_WINDOW_DISPLAY_TIME_MI, param2 + param);
				if (Logger.isLoggableDebug(TAG)) {
					Logger.d(TAG, String.format("Report FLOATING_WINDOW_DISPLAY_TIME_MI: %s%s", param2, param));
				}
			}
			//
			if (Logger.isLoggableDebug(TAG)) {
				Logger.d(TAG, String.format("Report: delta time=%d (ms), minutes = %d", time, minutes));
			}
		}
		
		private void load() {
			FileInputStream input = null;
			try {
				input = new FileInputStream(file);
				byte[] buf = new byte[4 + 8 + 8];
				if (buf.length == input.read(buf)) {
					ByteBuffer bb = ByteBuffer.wrap(buf);
					bb.order(ByteOrder.BIG_ENDIAN);
					bb.position(0);
					dayReport = bb.getInt();
					timeReport = bb.getLong();
					timeAmount = bb.getLong();
					if (Logger.isLoggableDebug(TAG)) {
						Logger.d(TAG, "Load: " + this.toString());
					}
				}
			} catch (IOException e) {
			} finally {
				com.subao.common.Misc.close(input);
			}
		}
		
		private static long now() {
			return SystemClock.elapsedRealtime();
		}
		
		@Override
		public String toString() {
			return String.format("[day=%d, timeReport=%d, timeAmount=%d]", dayReport, timeReport, timeAmount);
		}
	}
	
	private void showBadNetStateRemind(){
		if(strategy!=null){
			return ;
		}
		
		String remindText = getRemindMsg(currentNetType);
		if(TextUtils.isEmpty(remindText)){
			return ;
		}
		
		List<String> textList = new ArrayList<String>(2);
		textList.add("正在加速……");
		textList.add(remindText);
		
		strategy = new ToastEx.FixedTimeStrategy(textList,ToastEx.Effect.FAIL);
		ToastEx.show(AppMain.getContext(), strategy, new ToastEx.OnToastExOverListener() {
			@Override
			public void onToastExOver(int id) {
				setInstanceVisibility(View.VISIBLE);
			}
		},false);
		
		setInstanceVisibility(View.GONE);
	}
	
	private static String getRemindMsg (NetTypeDetector.NetType type){	
		if(NetTypeDetector.NetType.WIFI.equals(type)){
			return "当前WiFi质量较差，建议切换WiFi或使用移动网络" ;
		}
		
		String mobileType = NetworkCheckManager.getNetTypeStr(type);
		if(TextUtils.isEmpty(mobileType)){
			return null ;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("当前");
		builder.append(mobileType);
		builder.append("质量较差，建议更换地点或尝试连接WiFi");
		
		return builder.toString();		
	}
}
