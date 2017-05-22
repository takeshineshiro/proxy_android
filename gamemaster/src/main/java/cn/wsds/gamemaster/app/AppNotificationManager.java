package cn.wsds.gamemaster.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RemoteViews;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.SystemInfoUtil;
import cn.wsds.gamemaster.ui.ActivityMain;
import cn.wsds.gamemaster.ui.ActivityMessage;
import cn.wsds.gamemaster.ui.ActivityNewGamePlayAchieve;
import cn.wsds.gamemaster.ui.ActivityOpenGameInside;
import cn.wsds.gamemaster.ui.ActivityProccesClean;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.store.ActivityVip;

/**
 * 通知相关管理类
 */
@SuppressLint("DefaultLocale")
public class AppNotificationManager {

	/**
	 * 通知ID
	 */
	public enum NoticeId {
		/** DEBUG */
		DEBUG,
		/** 通知ＩＤ反馈回复　 */
		NEWFEEDBACK_REPLY,
		/** 通知ＩＤ新游戏通知　 */
		NEWSUPPORT_GAME,
		/** 加速使用成就　 */
		GAME_PLAY_ACHIEVE,
		/** 非活跃用户提醒 */
		INACTIVE_USER_REMIND,
		/** 前台服务 */
		FOREGROUND_SERVICE,
		/** 提醒用户在APK内启动游戏 */
		START_GAME_INSIDE,
		/** 引导打开“有权查看应用使用情况模块”权限*/
		USAGE_STATE_HELP,
		/** 通知ＩＤ 内存自动清理 */
		MEMORY_AUTO_CLEAN,
		/** 通知ＩＤ 引导UI兑换流量包 */
		EXCHANGE_FLOW,
		/** 通知新用户注册有礼 */
		NEW_USER_REGISTER,
		/** 加速服务启动、鉴权*/
		ACCEL_CHECK_START,
		/** 用户登出，关闭迅游加速服务*/
		LOGOUT_CLOSE_ACCEL,
		/** 用户过期*/
		CHECK_USER_EXPIRED,
		/** 用户即将过期*/
		CHECK_USER_WILL_BE_EXPIRED,
	}

	/**
	 * 用指定的标题、正文、大小图标，创建一个Builder
	 * 
	 * @param title
	 *            标题
	 * @param content
	 *            正文
	 * @param largeicon
	 *            大图标
	 * @param smallicon
	 *            小图标
	 */
	private static Builder createNotificationBuilder(String title, String content, String ticker, int largeicon,
		int smallicon) {
		Context context = AppMain.getContext();
		Bitmap icon = BitmapFactory.decodeResource(context.getResources(), largeicon);
		Builder builder = new Builder(context);
		builder.setLargeIcon(icon).setSmallIcon(smallicon);
		builder.setContentTitle(title).setContentText(content);
		if (!TextUtils.isEmpty(ticker)) {
			builder.setTicker(ticker);
		}
		return builder;
	}

	/**
	 * 用指定的标题、正文，以及缺少的大小图标，创建一个Builder
	 * 
	 * @param title
	 *            标题
	 * @param content
	 *            正文
	 */
	public static Builder createNotificationBuilder(String title, String content, String ticker) {
		return createNotificationBuilder(title, content, ticker, R.drawable.notify_icon_normal_big,
			R.drawable.notify_icon_normal);
	}

	/**
	 * 生成一条Ticker通知
	 * 
	 * @param content
	 */
	@SuppressWarnings("deprecation")
	public static void sendTickerNotice(String content) {
		Context context = AppMain.getContext();
		Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.notify_icon_normal_big);
		Builder builder = new Builder(context).setLargeIcon(icon).setSmallIcon(R.drawable.notify_icon_normal)
			.setContentTitle(context.getResources().getString(R.string.app_name)).setTicker(content)
			.setContentText(content);
		NotificationManager mNotificationManager = (NotificationManager) context
			.getSystemService(Context.NOTIFICATION_SERVICE);
		builder.setAutoCancel(true);
		builder.setTicker(content);
		mNotificationManager.notify(0, builder.getNotification());
		mNotificationManager.cancel(0);
	}

	/**
	 * 清除所有通知
	 * 
	 * @param context
	 */
	public static void removeAllNotice(Context context) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}

	/**
	 * 创建intent意图
	 * 
	 * @param cls
	 *            需要打开的activity
	 * @param extras
	 *            需要传递的数据
	 * @return
	 */
	private static Intent createIntent(Class<?> cls, Bundle extras) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		if (extras != null) {
			intent.putExtras(extras);
		}
		if (cls != null) {
			Context context = AppMain.getContext();
			intent.setClass(context, cls);
		}
		return intent;
	}

	private static PendingIntent createPendingIntent(Intent intent, int id) {
		Intent[] intents = new Intent[2];
		intents[0] = Intent.makeRestartActivityTask(new ComponentName(AppMain.getContext(), ActivityMain.class));
		intents[1] = intent;
		Context context = AppMain.getContext();
		return PendingIntent.getActivities(context, id, intents, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	// ====

	/**
	 * 生成一条非常驻通知
	 * 
	 * @param noticId
	 *            通知ID
	 * @param builder
	 *            相关参数的Builder
	 */
	private static void sendNotice(NoticeId noticId, Builder builder) {
		Context context = AppMain.getContext();
		NotificationManager mNotificationManager = (NotificationManager) context
			.getSystemService(Context.NOTIFICATION_SERVICE);
		@SuppressWarnings("deprecation")
		Notification notification = builder.getNotification();
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(noticId.ordinal(), notification);
	}

	/**
	 * 推送一条通知：有新的反馈回复
	 */
	public static void sendNoticeHasNewFeedbackReply() {
		String content = "请点击查看";
		String title = "您的反馈收到回复啦~";
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = NoticeId.NEWFEEDBACK_REPLY;
		Intent intent = createIntent(ActivityMessage.class, null);
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}

	/**
	 * 推送一条通知：有新游戏提醒
	 * 
	 * @param appLabel
	 *            应用名
	 */
	public static void sendNewAppNotify(String appLabel) {
		String content = appLabel;
		String title = "您有新的游戏可以加速";
		String ticker = String.format("%s可使用游戏加速", appLabel);
		if(SystemInfoUtil.isStrictOs()){
			title = "如需加速新装游戏请重启加速" ;
			content = AppMain.getContext().getResources().getString(R.string.strict_os_new_game_notify_content);
		}
		
		Builder builder = createNotificationBuilder(title, content, ticker);

		Bundle extras = new Bundle();
//		extras.putBoolean(IntentExtraName.NOTICE_INTENT_EXTRANAME_NEWAPPNOTIFY, true);
		NoticeId noticeId = AppNotificationManager.NoticeId.NEWSUPPORT_GAME;
		builder.setContentIntent(createPendingIntent(createIntent(ActivityMain.class, extras), noticeId.ordinal()));
		sendNotice(noticeId, builder);
	}

	//	/**
	//	 * 推送一条通知：使用成就
	//	 * 
	//	 * @param range
	//	 *            提醒范围 单位小时
	//	 */
	//	public static void sendGamePlayAchievements(int range) {
	//		String content = "点击查看详情";
	//		String title = String.format("迅游手游已为您加速超过%d小时", range);
	//		Builder builder = createNotificationBuilder(title, content, title);
	//		NoticeId id = AppNotificationManager.NoticeId.GAME_PLAY_ACHIEVE;
	//		Intent intent = createIntent(ActivityNewGamePlayAchieve.class, null);
	//		Context context = AppMain.getContext();
	//		builder.setContentIntent(PendingIntent.getActivity(context, id.ordinal(), intent, PendingIntent.FLAG_CANCEL_CURRENT));
	//		sendNotice(id, builder);
	//	}

	/**
	 * 推送一条通知：使用成就
	 * 
	 * @param achieve
	 *            提醒范围 单位小时
	 */
	public static void sendGamePlayAchievements(int achieve) {
		String content = "点击查看详情";
		String title = String.format("您在迅游手游中击败了%d%%小伙伴", achieve);
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = AppNotificationManager.NoticeId.GAME_PLAY_ACHIEVE;
		Intent intent = createIntent(ActivityNewGamePlayAchieve.class, null);
		Context context = AppMain.getContext();
		builder.setContentIntent(PendingIntent.getActivity(context, id.ordinal(), intent,
			PendingIntent.FLAG_CANCEL_CURRENT));
		sendNotice(id, builder);
	}

	/**
	 * 推送一条通知：从APP内启动游戏
	 */
	public static void sendOpenGameInside() {
		String content = "从迅游内启动游戏才有最好的加速效果哦~";
		Builder builder = createNotificationBuilder("加速效果更进一步！", content, content);
		NoticeId noticeId = AppNotificationManager.NoticeId.START_GAME_INSIDE;
		builder.setContentIntent(createPendingIntent(createIntent(ActivityOpenGameInside.class, null),
			noticeId.ordinal()));
		sendNotice(noticeId, builder);
		Statistic.addEvent(AppMain.getContext(), Statistic.Event.NOTIFICATION_PUSH_GAME_OPTIMIZE);
	}

	/**
	 * 发送 debug 通知
	 * 
	 * @param content
	 *            提示内容
	 */
	public static void sendDebugNotice(String content) {
		String title = "DEBUG";
		Builder builder = createNotificationBuilder(title, content, content);
		NoticeId noticeId = AppNotificationManager.NoticeId.GAME_PLAY_ACHIEVE;
		sendNotice(noticeId, builder);
	}

	public static void sendRemindInactiveUser() {
		String content = "点击立刻开启";
		String title = "迅游手游已准备好为游戏加速";
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = AppNotificationManager.NoticeId.INACTIVE_USER_REMIND;
		Bundle bundle = new Bundle();
		bundle.putBoolean(IntentExtraName.START_FROM_NOTIFICATION, true);
		Intent intent = createIntent(ActivityMain.class, bundle);
		Context context = AppMain.getContext();
		builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
		sendNotice(id, builder);
	}

	/**
	 * 将加速结果整理成通知发送到通知栏
	 * 
	 * @param accelTimeSenonds
	 *            游戏加速时长（单位： 秒 ）
	 * @param delay
	 *            延迟降低多少
	 * @param info
	 *            {@link GameInfo}
	 */
	public static void sendGameAccelResult(int accelTimeSenonds, int delay, GameInfo info) {
		Context context = AppMain.getContext();
		NotificationManager mNotificationManager = (NotificationManager) context
			.getSystemService(Context.NOTIFICATION_SERVICE);
		Builder builder = NoticeGameAccelResultViewStruct.builder(accelTimeSenonds, info.getAppIcon(context), delay);
		Intent intent = createIntent(ActivityMain.class, null);
		intent.putExtra(IntentExtraName.START_FROM_NOTIFICATION, true);
		intent.putExtra(IntentExtraName.INTENT_EXTRANAME_PACKAGE_NAME, info.getPackageName());
		int id = info.getUid();
		builder.setContentIntent(createPendingIntent(intent, id));
		@SuppressWarnings("deprecation")
		Notification notification = builder.getNotification();
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(id, notification);
	}
	
	/**
	 * 引导打开"有权查看应用使用情况模块"
	 */
	public static void sendUsageStateHelp(){
		String content = "未获得查看应用使用情况权限，点击授权";
		String title = "加速效果受限o(≧口≦)o~";
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = AppNotificationManager.NoticeId.USAGE_STATE_HELP;
		Intent intent = new Intent("android.settings.USAGE_ACCESS_SETTINGS");
		Context context = AppMain.getContext();
		builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
		sendNotice(id, builder);
	}
	
	/**
	 * 发送通知-内存自动清理设置引导
	 */
	public static void sendMemoryAutoClean(){
		String title = "有应用正在偷偷运行，影响游戏速度";
		String content = "新增自动清理功能，请立即设置";
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = AppNotificationManager.NoticeId.MEMORY_AUTO_CLEAN;
		builder.setContentIntent(createPendingIntent(createIntent(ActivityProccesClean.class, null),
                id.ordinal()));
		sendNotice(id, builder);
	}

	
	/**
	 * 推送一条通知：提醒新用户注册有礼 
	 */
	
	/*public static void sendNewUserRegisterNotify() {
		String content = "请点击查看";
		String title = "注册分享拿好礼，流量礼包送不停";
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = NoticeId.NEW_USER_REGISTER;
		Bundle bundle = new Bundle();
		bundle.putBoolean(IntentExtraName.CALL_FROM_NOTIFICATION, true);
		Intent intent = createIntent(ActivityUser.class, bundle);
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}*/
	public static void sendNoticeAccelStarting() {
		String content = "";
		String title = AppMain.getContext().getResources().getString(R.string.accel_checking);
		String ticker = String.format("%s可使用游戏加速", "xx");
		Builder builder = createNotificationBuilder(title, content, ticker);
		NoticeId id = NoticeId.ACCEL_CHECK_START;
		Intent intent = new Intent();
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}

	/**
	 * 推送一条通知：用户未登录
	 */
	public static void sendUserLogout() { 
		String content = "请点击登录";
		String title = AppMain.getContext().getResources().getString(R.string.raccel_remind_login);
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = NoticeId.ACCEL_CHECK_START;
		Bundle extras = new Bundle();
		extras.putInt(ActivityUserAccount.EXTRA_NAME_FRAGMENT_TYPE, ActivityUserAccount.FRAGMENT_TYPE_LOGIN);
		Intent intent = createIntent(ActivityUserAccount.class, extras);
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}
	
	/**
	 * 推送一条通知：VIP服务已过期或被迫登出，或网络原因导致鉴权失败
	 */
	public static void sendNoticeAccelInvalid(String title) {
		Builder builder = createNotificationBuilder(title, "", title);
		NoticeId id = NoticeId.ACCEL_CHECK_START;
		Intent intent = new Intent();
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}
	
	/**
	 * 推送一条通知：迅游加速服务成功开启
	 */
	public static void sendNoticeAccelStarted() {
		String content = "请点击查看";
		String title = AppMain.getContext().getResources().getString(R.string.accel_success);
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = NoticeId.ACCEL_CHECK_START;
		Intent intent = new Intent();
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}
	
	/**
	 * 推送一条通知：用户登出，加速服务关闭
	 */
	public static void sendNoticeLogoutCloseAccel(String title) {
		if((title==null)||(title.isEmpty())){
			return ;
		}
		String content = "";
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = NoticeId.LOGOUT_CLOSE_ACCEL;
		Intent intent = new Intent();
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}

	/**
	 * 发送用户过期通知
	 */
	public static void sendUserExpired() {
		String content = "温馨提示，您的VIP套餐已到期， 请前往续费以继续享受VIP加速服务^-^";
		String title = AppMain.getContext().getResources().getString(R.string.accel_expired);
		Builder builder = createNotificationBuilder(title, content, title);
		NoticeId id = NoticeId.CHECK_USER_EXPIRED;
		Intent intent = new Intent();
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}

	/**
	 * VIP服务即将过期通知
	 */
	public static void sendUserWillBeExpired(String expiredTime){
		if(TextUtils.isEmpty(expiredTime)){
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("温馨提示，您的VIP套餐将在明天（");
		sb.append(expiredTime);
		sb.append("）到期，请前往续费以继续享受VIP加速服务哦^-^");

		String title = AppMain.getContext().getResources().
				getString(R.string.accel_will_expired);
		Builder builder = createNotificationBuilder(title, sb.toString(), title);
		NoticeId id = NoticeId.CHECK_USER_WILL_BE_EXPIRED;
		Intent intent = createIntent(ActivityVip.class,null);
		builder.setContentIntent(createPendingIntent(intent, id.ordinal()));
		sendNotice(id, builder);
	}
	/**
	 * 创建加速结果通知builder
	 */
	private static final class NoticeGameAccelResultViewStruct {

		/**
		 * 创建加速结果通知builder
		 * 
		 * @param accelTimeSenonds
		 *            游戏加速时长（单位： 秒 ）
		 * @param gameIcon
		 *            游戏图标
		 * @param delay
		 *            延迟平均降低多少
		 * @return 加速结果通知builder
		 */
		private static Builder builder(int accelTimeSenonds, Drawable gameIcon, int delay) {
			Context context = AppMain.getContext();
			Builder builder = new Builder(context);
			builder.setSmallIcon(R.drawable.notify_icon_normal).setContentTitle("");
			// 自定义view并填充信息
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_gameaccel_result);
			String title = formatTitle(accelTimeSenonds);
			views.setTextViewText(R.id.text_title, title);
			builder.setTicker(title);
			String formatresultDelay = context.getResources().getString(R.string.gameaccel_result_delay);
			views.setTextViewText(R.id.text_delay, String.format(formatresultDelay, delay));
			views.setImageViewBitmap(R.id.image_gameicon, ((BitmapDrawable) gameIcon).getBitmap());
			builder.setContent(views);
			return builder;
		}

		/**
		 * 格式化时间值(单位：时分/分)
		 * 
		 * @param accelTime
		 *            accelTimeSenonds 游戏加速时长(单位： 秒 )
		 * @return
		 */
		private static String formatTitle(int accelTime) {
			int minute = ((accelTime / 60) % 60);
			int hour = (accelTime / 3600);
			if (hour > 0) {
				return String.format("本次加速时长%d时%d分", hour, minute);
			} else {
				return String.format("本次加速时长%d分", minute);
			}

		}
	}

}
