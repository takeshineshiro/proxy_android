package cn.wsds.gamemaster.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.NinePatch;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.subao.common.utils.ThreadUtils;
import com.subao.utils.Misc;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil.SystemType;

public class UIUtils {
	
	private static Context context;
	
	public static void init(Context context) {
		UIUtils.context = context.getApplicationContext();
	}

	public static void showErrorDialog(Context context, CharSequence message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("ERROR");
		builder.setMessage(message);
		builder.setPositiveButton(R.string.confirm, null);
		builder.show();
	}

	public static void turnActivity(Context context, Class<?> toClass) {
		Intent i = new Intent(context, toClass);
		context.startActivity(i);
	}

	/**
	 * 打开游戏加速详情
	 * 
	 * @param context当前界面上下文对象
	 * @param uid
	 *            游戏的UId
	 */
	public static void turnGameAccelDesc(Activity context, String packageName) {
		Intent intent = new Intent(context, ActivityGameAccelDesc.class);
		intent.putExtra(IntentExtraName.INTENT_EXTRANAME_PACKAGE_NAME, packageName);
		context.startActivity(intent);
	}

	public static void showToast(CharSequence message) {
		showToast(message, Toast.LENGTH_SHORT);
	}

	public static void showToast(int resId) {
		showToast(resId, Toast.LENGTH_SHORT);
	}

	public static void showToast(CharSequence message, int duration) {
		if (ThreadUtils.isInAndroidUIThread()) {
			Toast.makeText(context, message, duration).show();
		} else {
			MainHandler.getInstance().showToast(message, duration);
		}
	}

	public static void showToast(int resId, int duration) {
		showToast(context.getString(resId), duration);
	}

	//	/**
	//	 * 当前我们是否在前台
	//	 */
	//	public static boolean ownerInFront() {
	//		Context context = AppMain.getInstance();
	//		RunningTaskInfo rt = getFrontAppInfo();
	//		if (rt != null) {
	//			String pn = rt.topActivity.getPackageName();
	//			return context.getApplicationInfo().packageName.equals(pn);
	//		}
	//		return false;
	//
	//	}

	//	/**
	//	 * 获得当前前台应用信息
	//	 * @return
	//	 */
	//	public static RunningTaskInfo getFrontAppInfo() {
	//		Context context = AppMain.getInstance();
	//		ActivityManager mActivityManager = (ActivityManager) context
	//				.getSystemService(Context.ACTIVITY_SERVICE);
	//		List<RunningTaskInfo> runningTaskInfos = mActivityManager
	//				.getRunningTasks(1);
	//		if (null != runningTaskInfos && !runningTaskInfos.isEmpty()) {
	//			RunningTaskInfo rt = runningTaskInfos.get(0);
	//			return rt;
	//		}
	//		return null;
	//	}

	/**
	 * 进度提醒对话框 创建该类对象，需要显示时 调用show 方法， 隐藏时调用dismiss 方法
	 */
	public static final class ProgressAlertDialog {
		private final Dialog dialog;
		private View imageInner;
		private View imageOuter;
		private RotateAnimation animationinner, animationouter;

		public ProgressAlertDialog(Activity context) {
			super();
			dialog = new Dialog(context, R.style.progress_dialog_bg);
			View view = View.inflate(context, R.layout.progress, null);
			dialog.setContentView(view);
			imageInner = view.findViewById(R.id.image_inner);
			imageOuter = view.findViewById(R.id.image_outer);
			animationinner = buildAnimation(0, 360);
			animationouter = buildAnimation(0, -360);
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					imageInner.clearAnimation();
					imageOuter.clearAnimation();
				}
			});
		}

		private RotateAnimation buildAnimation(float start, float end) {
			RotateAnimation animation = new RotateAnimation(start, end,
				Animation.RELATIVE_TO_PARENT, 0.5f,
				Animation.RELATIVE_TO_PARENT, 0.5f);
			animation.setDuration(1000);
			animation.setRepeatCount(-1);
			animation.setInterpolator(new LinearInterpolator());
			return animation;
		}

		public void show() {
			imageInner.startAnimation(animationinner);
			imageOuter.startAnimation(animationouter);
			dialog.show();
		}

		public boolean isShowing() {
			return dialog.isShowing();
		}

		public void dimiss() {
			dialog.dismiss();
		}
	}

	/**
	 * 将“累计节省时长”格式化为指定的字串
	 * 
	 * @param milliseconds
	 * @return 格式化后的字串
	 */
	public static String formatTotalSparedTime(int milliseconds) {
		if (milliseconds < 100) {
			return "0秒";
		}
		int seconds = milliseconds / 1000;
		int remain = (milliseconds % 1000) / 100;
		if (seconds < 60) {
			return String.format(Locale.getDefault(), "%d.%d秒", seconds, remain);
		}
		int minutes = seconds / 60;
		seconds %= 60;
		if (minutes < 60) {
			return String.format(Locale.getDefault(), "%d分%d秒", minutes, seconds);
		}
		int hours = minutes / 60;
		minutes %= 60;
		return String.format(Locale.getDefault(), "%d时%d分", hours, minutes);
	}

	/**
	 * 将“本次节省时长”格式化为指定的字串
	 * 
	 * @param milliseconds
	 * @return 格式化后的字串
	 */
	public static String formatThisTimeSparedTime(int milliseconds) {
		return String.format(Locale.getDefault(), "%.1fs", milliseconds * 0.001f);
	}

	/**
	 * 设置listview控件高度
	 * 
	 * @param pixels
	 *            高度值 单位像素
	 */
	public static void setListViewHeight(ListView listView, int pixels) {
		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = pixels;
		listView.setLayoutParams(params);
	}

	/**
	 * 加载默认icon
	 */
	@SuppressWarnings("deprecation")
	public static Drawable loadAppDefaultIcon(Context context) {
		return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
	}

	/**
	 * 根据指定的Bitmap创建NinePatch
	 * 
	 * @param bmp
	 *            Bitmap对象
	 * @return 成功返回NinePatch对象实例，失败返回null
	 */
	public static NinePatch createNinePatch(Bitmap bmp) {
		if (bmp != null && NinePatch.isNinePatchChunk(bmp.getNinePatchChunk())) {
			try {
				return new NinePatch(bmp, bmp.getNinePatchChunk(), null);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	public static final String KEY_QQ_GROUP_1 = "KHwFXb9Aj7g9EU5K5MK7SwgUih8gNGxK";
	public static final String KEY_QQ_GROUP_2 = "SvThh_Em2bgi97SqEIKu9GTwaueBFP3-";
	public static final String KEY_QQ_GROUP_KING_GLORY = "DGp6IDMPhv86dADiHQFjFKhQ4kihjtBh";
	public static final String KEY_QQ_GROUP_DEFAULT = KEY_QQ_GROUP_2;

	/**
	 * 打开QQ群对话
	 */
	public static void openQQgroup(Activity activity, String key) {
		try {
			Intent intent = new Intent();
			intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + key));
			activity.startActivity(intent);
		} catch (Exception e) {
			// 未安装手Q或安装的版本不支持
			Toast.makeText(activity, "您未安装手机QQ客户端", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 打开客服QQ对话
	 */
	public static void openServiceQQ(Activity activity) {
		if (Misc.isAppInstalled(activity, "com.tencent.mobileqq")) {
			try {
				String qq = activity.getString(R.string.qq_number_service);
				Uri uri = Uri.parse(String.format("mqqwpa://im/chat?chat_type=wpa&uin=%s", qq)); //                        String.format("http://wpa.qq.com/msgrd?v=3&uin=%s&site=qq&menu=yes", qq));
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				activity.startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(activity,
					"您未安装手机QQ客户端", Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(activity, "您未安装手机QQ客户端", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 游戏中来电提醒功能是否支持当前rom
	 * 
	 * @return true 支持 false 不支持
	 */
	public static boolean isCallRemindSupportCurrentRom() {
		SystemType systemType = MobileSystemTypeUtil.getSystemType();
		if (SystemType.MIUI.equals(systemType) || SystemType.MX.equals(systemType)) {
			return false;
		}
		// 5.0以上不支持这个功能
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			return false;
		}
		return true;
	}

	public static void onSwitchCallManagerChanged(boolean check, Context context) {
		ConfigManager.getInstance().setCallRemindGamePlaying(check);
		//		if(!check){
		//			StatisticDefault.addEvent(context, StatisticDefault.Event.SWITCH_CALLMANAGER_OFF);
		//		}
	}

	public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 100, output);
		if (needRecycle) {
			bmp.recycle();
		}

		byte[] result = output.toByteArray();
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static Bitmap bmpScaled(Bitmap bmp, int percent) {
		if (100 == percent) {
			return bmp;
		}
		return Bitmap.createScaledBitmap(bmp, bmp.getWidth() * percent / 100, bmp.getHeight() * percent / 100, true);
	}

	public static byte[] bmpCompressToByteArray(Bitmap bmp, int maxSize, boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		int quality = 100;
		bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
		while (output.size() > maxSize) {  //循环判断如果压缩后图片是否大于maxSize,大于继续压缩        
			if (quality < 0) {
				return null;
			}
			output.reset();
			bmp.compress(Bitmap.CompressFormat.JPEG, quality, output);
			quality -= 10;//每次都减少10  
		}
		if (needRecycle) {
			bmp.recycle();
		}
		byte[] result = output.toByteArray();
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static void setForground(int taskId) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME);
	}

	/**
	 * 设置给定View的Visibility
	 * 
	 * @return true表示该VIEW的Visibility已成功改变，false表示该VIEW原来的Visibility已经是试图设置的值了
	 */
	public static boolean setViewVisibility(View v, int visibility) {
		if (v.getVisibility() != visibility) {
			try {
				v.setVisibility(visibility);
				return true;
			} catch (RuntimeException e) {
				// 已知在某款机型中可能抛出 SecurityException，坑爹
			}
		}
		return false;
	}

	/**
	 * 设置给定TextView的Text
	 * 
	 * @return 
	 *         true表示成功改变该TextView的Text，false表示该TextView原来的Text已经与试图设置的Text相等（Equals
	 *         ），勿需重设
	 */
	public static boolean setViewText(TextView v, CharSequence text) {
		if (!v.getText().equals(text)) {
			v.setText(text);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 改变给定TextView的Color
	 * 
	 * @return true表示改变成功，false表示原来就已经是这个值了，勿需重设
	 */
	public static boolean setViewTextColor(TextView tv, ColorStateList textColor) {
		if (tv.getTextColors() != textColor) {
			tv.setTextColor(textColor);
			return true;
		}
		return false;
	}

	public static int getAccelPercent(int cleanCount) {
		int percent = cleanCount * 2 + 25 + ((int) System.currentTimeMillis() % 11);
		return Math.min(percent, 80);
	}

	/**
	 * 是否属于5.0及以上用户
	 */
	public static boolean isLollipopUser() {
		return android.os.Build.VERSION_CODES.LOLLIPOP <= android.os.Build.VERSION.SDK_INT;
	}

	/**
	 * 格式化手机号 中间4位数 替换为*
	 * 
	 * @param phoneNumber
	 *            手机号码字符串
	 * @return
	 */
	public static String getFormatPhoneNumber(String phoneNumber) {
		if (TextUtils.isEmpty(phoneNumber)) {
			return phoneNumber;
		}
		if (phoneNumber.length() != 11) {
			return phoneNumber;
		}

		char[] charArray = phoneNumber.toCharArray();
		char[] temp = new char[11];
		for (int i = 0; i < charArray.length; i++) {
			if (i < 7 && i > 2) {
				temp[i] = '*';
			} else {
				temp[i] = charArray[i];
			}
		}
		return String.valueOf(temp);
	}

	/**
	 * 登录失效时，提醒用户重新登录
	 * 
	 * @param context
	 */
	public static void showReloginDialog(Activity context) {
		showReloginDialog(context, null);
	}

	public interface IOnReloginConfirmListener {
		public void onConfirm();
	}

	/**
	 * 登录失效时，提醒用户重新登录
	 * 
	 * @param context
	 */
	public static void showReloginDialog(Activity context, IOnReloginConfirmListener listener) {
		if (context == null || context.isFinishing()) {
			return;
		}
		new StartReloginDialog(context, "登录已失效，请重新登录。", listener).show();
	}

	private static class DefaultReloginDialog {
		private final Activity context;
		private final String mess;
		private final IOnReloginConfirmListener listener;
		protected final CommonAlertDialog dialog;

		public DefaultReloginDialog(Activity context, String mess, IOnReloginConfirmListener listener) {
			this.context = context;
			this.mess = mess;
			this.listener = listener;
			dialog = new CommonAlertDialog(context);
		}

		protected void setNegativeButton() {
			dialog.setNegativeButton("暂不登录", null);
		}

		private void setReloginDialog() {
			dialog.setMessage(mess);
			dialog.setPositiveButton("立即登录", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					ActivityUserAccount.open(context, ActivityUserAccount.FRAGMENT_TYPE_LOGIN);
					if (listener != null) {
						listener.onConfirm();
					}
				}
			});
			setNegativeButton();
		}

		public void show() {
			setReloginDialog();
			dialog.show();
		}
	}

	private static class StartReloginDialog extends DefaultReloginDialog {
		private final Activity context;

		public StartReloginDialog(Activity context, String mess, IOnReloginConfirmListener listener) {
			super(context, mess, listener);
			this.context = context;
		}

		@Override
		protected void setNegativeButton() {
			dialog.setNegativeButton("暂不登录", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					turnActivity(context, ActivityUser.class);
				}
			});
		}
	}

	/**
	 * 登录失效时，提醒用户重新登录
	 * 
	 * @param context
	 */
	public static void showReloginDialog(final Activity context, String mess, final IOnReloginConfirmListener listener) {
		if (context == null || context.isFinishing() || mess == null) {
			return;
		}

		new DefaultReloginDialog(context, mess, listener).show();
	}

	/**
	 * 为了统一外观，所有SwipeRefreshLayout必须调用此方法来初始化
	 */
	public static void setSwipeRefreshAppear(SwipeRefreshLayout swipe) {
		swipe.setColorSchemeResources(R.color.color_game_11);
		swipe.setProgressViewOffset(false, 0, swipe.getResources().getDimensionPixelOffset(R.dimen.space_size_50));
	}

	public static int dp2px(float dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
	}

//	/**
//	 * 调用系统默认的 DownLoadManager下载指定的Url
//	 * 
//	 * @param context
//	 * @param uriString
//	 */
//	public static void downloadUrl(Context context, String uriString) {
//		DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
//		DownloadManager.Request request = null;
//		try {
//			request = new DownloadManager.Request(Uri.parse(uriString));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		if (null == request) {
//			return;
//		}
//		request.setTitle("游戏下载中");
//		request.setVisibleInDownloadsUi(true);
//		downloadManager.enqueue(request);
//	}

	public static void openUrl(Activity activity, String uri) {
		if (TextUtils.isEmpty(uri)) {
			return;
		}
		try {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			Uri content_url = Uri.parse(uri);
			intent.setData(content_url);
			activity.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void showNetDisconnectMessage(){
		showToast(AppMain.getContext().getResources().getString(R.string.net_disconnected));
	}
	
	public static void showNetErrorAccelMessage(){
		showToast(AppMain.getContext().getResources().getString(R.string.net_error_accel_open_failed));
	}
}
