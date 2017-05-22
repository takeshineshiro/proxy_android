package cn.wsds.gamemaster.ui;

import com.subao.resutils.WeakReferenceHandler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import cn.wsds.gamemaster.AppInitializer;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.JPushUtils;
import cn.wsds.gamemaster.ui.view.StartPage;

/**
 * 启动页（注意，不要从BaseActivity继承）
 */
public class ActivityStart extends Activity {

	private static final long MIN_SHOW_TIME = 2000;
	private static final int currentGuidePageVersion = 1;

	private boolean alreayInit;
	private MyHandler handler;

	private boolean needEvent_InteractiveClientPower;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new MyHandler(this);
		if (AppInitializer.instance.isInitialized()) {
			alreayInit = true;
			needEvent_InteractiveClientPower = false;
		} else {
			needEvent_InteractiveClientPower = true;	// 等初始化完成后（UserActionManager也初始化了），需要发送一个特定的UserAction
			this.setContentView(new StartPage(this),
				new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && !alreayInit) {
			handler.sendMessage(handler.obtainMessage(MyHandler.MSG_INIT, Long.valueOf(SystemClock.elapsedRealtime())));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		JPushUtils.onActivityResume(this);
		if (alreayInit) {
			handler.sendEmptyMessage(MyHandler.MSG_TURN_ACTIVITY);
		}
	}

		@Override
		protected void onPause() {
			super.onPause();
			JPushUtils.onActivityPause(this);
		}

	/** 从Config里取得的GuidePageVersion整数值，需要与本掩码进行“与”操作，才是真正的GuidPageVersion */
	private static final int MASK_GUIDE_PAGE_VERSION = 0x7fff;

	private static final int FLAG_BASE = 0x8000;
	/** 从Config里取得的GuidePageVersion值，如果本标志位置位，表示已经看过“来电拦截”页 */
	private static final int FLAG_GUIDE_PHONE_INCOMING_SHOWN = FLAG_BASE;

	/** 从Config里取得的GuidePageVersion值，如果本标志位置位，表示已经看过“注册享好礼”页 */
	private static final int FLAG_REGISTER_GIFT_SHOWN = FLAG_BASE << 1;

	/** 从Config里取得的GuidePageVersion值，如果本标志位置位，表示已经看过“兑换升级”页 */
	private static final int FLAG_EXCHANGE_UPGRADE = FLAG_BASE << 2;

	private void turnActivity() {
		if (this.needEvent_InteractiveClientPower) {
			Statistic.addEvent(getApplicationContext(), Statistic.Event.INTERACTIVE_CLIENT_POWER);
		}

		int guidePageVersion = ConfigManager.getInstance().getGuidePageVersion();
		if (currentGuidePageVersion != (guidePageVersion & MASK_GUIDE_PAGE_VERSION)) {
			// 未显示过完整的三页引导，说明是新安装的用户
			// 需跳转到三页引导，并且打上标志“已显示过来电拦截引导页（新用户不显示这个引导页）”和“已显示过注册享好礼”

			ConfigManager.getInstance().setNewUserFirstStartTime(System.currentTimeMillis());

			ConfigManager.getInstance().setGuidePage(true);

			ConfigManager.getInstance().setGuidePageVersion(
				(currentGuidePageVersion & MASK_GUIDE_PAGE_VERSION) | FLAG_GUIDE_PHONE_INCOMING_SHOWN | FLAG_REGISTER_GIFT_SHOWN | FLAG_EXCHANGE_UPGRADE);
			ActivityGuider.show(this, false);
		} else {
			// 已显示过三页引导了，是老用户
			if ((guidePageVersion & FLAG_EXCHANGE_UPGRADE) == 0) {
				// 已显示过三页引导了，但没有显示过“兑换升级”引导页，说明是从2.0升级到2.1.0的用户
				ConfigManager.getInstance().setGuidePageVersion(guidePageVersion | FLAG_EXCHANGE_UPGRADE);
				ActivityGuider.show(this, true);
			} else {
				Intent intent = new Intent(this, ActivityMain.class);
				startActivity(intent);
			}
		}

		this.finish();
	}

	private static class MyHandler extends WeakReferenceHandler<ActivityStart> {

		private static final int MSG_INIT = 1;
		private static final int MSG_TURN_ACTIVITY = 2;

		private int msgProcessed;

		public MyHandler(ActivityStart ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(ActivityStart ref, Message msg) {
			if ((msgProcessed & msg.what) != 0) {
				return;
			}
			msgProcessed |= msg.what;
			switch (msg.what) {
			case MSG_INIT:
				doInit((Long) msg.obj, ref);
				break;
			case MSG_TURN_ACTIVITY:
				ref.turnActivity();
				break;
			}
		}

		private void doInit(long timeOfBeginVisible, ActivityStart ref) {
			boolean initOk = AppInitializer.instance.execute(AppInitializer.InitReason.START_ACTIVITY, ref);
			if (!initOk) {
				return;
			}
			long remain = MIN_SHOW_TIME - (SystemClock.elapsedRealtime() - timeOfBeginVisible);
			if (remain > 0) {
				this.sendEmptyMessageDelayed(MSG_TURN_ACTIVITY, remain);
			} else {
				ref.turnActivity();
			}
		}
	}
}
