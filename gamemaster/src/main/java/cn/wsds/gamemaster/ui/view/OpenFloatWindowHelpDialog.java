package cn.wsds.gamemaster.ui.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil.SystemProp;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenHelpManager;

/**
 * 悬浮窗设置帮助界面
 */
public class OpenFloatWindowHelpDialog {

	private static HelpDialog dialogInstance;
	
	/**
	 * 悬浮窗设置帮助对话框
	 */
	public static void open(Activity activity) {
		open(activity, null, null);
	}

	/**
	 * 悬浮窗设置帮助对话框
	 */
	public static void open(Activity activity, SystemProp uiProp, final Runnable onDismissCallback) {
		if (dialogInstance != null) {
			return;
		}
		if (uiProp == null) {
			uiProp = MobileSystemTypeUtil.getSystemProp();
		}
		HelpDialog helpDialog = createHelpDialog(activity, uiProp);
		if (helpDialog == null) {
			gotoAppSetting(activity);
		} else {
			dialogInstance = helpDialog;
			ConfigManager.getInstance().setOpenFloatWindowHelpPageCount(1);
			helpDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (dialogInstance == dialog) {
						dialogInstance = null;
					}
					if (onDismissCallback != null) {
						onDismissCallback.run();
					}
				}
			});
			helpDialog.show();
		}
	}

	public static boolean isInstanceExists() {
		return dialogInstance != null;
	}

	public static void close() {
		if (dialogInstance != null) {
			dialogInstance.dismiss();
			dialogInstance = null;
		}
	}

	/**
	 * 通过系统信息创建引导帮助对话框
	 * 
	 * @param context
	 * @param uiProp
	 * @return 通过不同的手机类型创建不同的引导对话框。返回null说明没有符合该手机类型的引导对话框
	 */
	private static HelpDialog createHelpDialog(Activity activity, SystemProp uiProp) {
		if (TextUtils.isEmpty(uiProp.prop)) {
			return null;
		}
		if (activity.isFinishing()) {
			return null;
		}
		OpenFloatWindowHelpDialog openFloatWindowHelpDialog = new OpenFloatWindowHelpDialog();
		switch (uiProp.type) {
		case EMUI:
			return createEmuiDialog(activity,uiProp.prop);
		case MIUI:
			int layOutId = 0;
			if ("V6".compareToIgnoreCase(uiProp.prop) <= 0) {
				layOutId = R.layout.miui_floatwindow_help_dialog_v6;
			} else {
				layOutId = R.layout.miui_floatwindow_help_dialog_v5;
			}
			return new HelpDialog(activity, layOutId, openFloatWindowHelpDialog.onMiuiStratButtonOnClickListener);
		case MX:	
			if(OpenHelpManager.canOpenFloatWindowOnMX(uiProp.prop)){
				int layoutId = R.layout.meizu_floatwindow_help_dialog_high_version;
				    return new HelpDialog(activity, layoutId, openFloatWindowHelpDialog.onMeizuStratButtonOnClickListener);	
			}else{
				return null ;
			}
		default:
			return null;
		}
	}

	private static HelpDialog createEmuiDialog(Activity activity, String prop) {
		List<Character> emuiVersionStr = getEmuiVersionStr(prop);
		if(emuiVersionStr.isEmpty()){
			return null;
		}
		char verMajor = emuiVersionStr.get(0);
		if(verMajor < '3'){
			return HelpDialog.createEmuiLowerVersionDialog(activity);
		}else{
			return HelpDialog.createEmuiHighVersionDialog(activity, verMajor);
		}
	}

	private static List<Character> getEmuiVersionStr(String prop) {
		int length = prop.length();
		List<Character> versrionStr = new ArrayList<Character>(length); 
		for (int i = length-1; i >= 0; i--) {
			char c = prop.charAt(i);
			if(c == '_'){
				break;
			}
			if(c<='9' && c>='0'){
				versrionStr.add(c);
			}
		}
		Collections.reverse(versrionStr);
		return versrionStr;
	}


	private final View.OnClickListener onMiuiStratButtonOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Context context = v.getContext();
			statisticGotoSettingClicked(context);
			//跳转到设置界面
			try {
				Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
				intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
				intent.putExtra("extra_pkgname", context.getPackageName());
				intent.putExtra(IntentExtraName.CALL_FROM_FLOATWIDOW_DIALOG, true);
				context.startActivity(intent);
			} catch (ActivityNotFoundException ex) {
				gotoAppSetting(context);
			}
		}
	};
	
	private final View.OnClickListener onMeizuStratButtonOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Context context = v.getContext();
			statisticGotoSettingClicked(context);
			//跳转到设置界面
			try {
				Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");			
				intent.addCategory(Intent.CATEGORY_DEFAULT);
				intent.putExtra("packageName", context.getPackageName());
				intent.putExtra(IntentExtraName.CALL_FROM_FLOATWIDOW_DIALOG, true);
				context.startActivity(intent);
			} catch (ActivityNotFoundException ex) {
				gotoAppSetting(context);
			}
		}
	};

	private static final class HelpDialog extends Dialog {
		
		private static final String CLASS_NAME_NOTIFICATION_MANAGER = "com.huawei.notificationmanager.ui.NotificationManagmentActivity";
		private static final String CLASS_NAME_VIEW_MONITOR = "com.huawei.systemmanager.addviewmonitor.AddViewMonitorActivity";

		private HelpDialog(Activity activity, int layoutId, final View.OnClickListener onClickListener) {
			super(activity, R.style.AppDialogTheme);
			Statistic.addEvent(activity, Statistic.Event.INTERACTIVE_PROMPT_DIALOG_UI_FLOATING);
			setContentView(layoutId);
			findViewById(R.id.image_close).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					dismiss();
				}
			});
			View button = findViewById(R.id.button_start);
			if (button != null) {
				button.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						//关闭提示框
						dismiss();
						onClickListener.onClick(v);
					}
				});
			}

			Window dialogWindow = getWindow();
			WindowManager.LayoutParams lp = dialogWindow.getAttributes();
			DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
			int width = displayMetrics.widthPixels > displayMetrics.heightPixels ? displayMetrics.heightPixels : displayMetrics.widthPixels;
			lp.width = (int) (width * 0.9);
			dialogWindow.setGravity(Gravity.CENTER);
			dialogWindow.setAttributes(lp);
		}

		public static HelpDialog createEmuiHighVersionDialog(Activity activity, final char verMajor) {
			if (activity.isFinishing()) {
				return null;
			}
			return new HelpDialog(activity, R.layout.emui_floatwindow_help_dialog_high_version, new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Context context = v.getContext();
					statisticGotoSettingClicked(context);
					//
					String packageName = "com.huawei.systemmanager";
					Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
					if (intent == null) {
						gotoAppSetting(context);
						return;
					}
					//
					String clsName;
					if (verMajor == '3') {
						clsName = CLASS_NAME_NOTIFICATION_MANAGER;
					} else {
						clsName = CLASS_NAME_VIEW_MONITOR;
					}
					try {						
						intent.setClassName(packageName, clsName);
						intent.putExtra(IntentExtraName.CALL_FROM_FLOATWIDOW_DIALOG, true);
						context.startActivity(intent);
					} catch (ActivityNotFoundException ex) {
						gotoAppSetting(context);
					}
				}
			});
		}

		public static HelpDialog createEmuiLowerVersionDialog(Activity activity) {
			if (activity.isFinishing()) {
				return null;
			}
			return new HelpDialog(activity, R.layout.emui_floatwindow_help_dialog_lower_version, new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Context context = v.getContext();
					statisticGotoSettingClicked(context);
					//
					Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.huawei.systemmanager");				
					if (intent == null) {
						gotoAppSetting(context);
					} else {
						try {
							intent.putExtra(IntentExtraName.CALL_FROM_FLOATWIDOW_DIALOG, true);
							context.startActivity(intent);
						} catch (ActivityNotFoundException ex) {
							gotoAppSetting(context);
						}
					}
				}
			});
		}
	}

	private static void statisticGotoSettingClicked(Context context) {
		Statistic.addEvent(context, Statistic.Event.INTERACTIVE_SETTING_CLICK_UI_FLOATING);
	}

	public static void gotoAppSetting(Context context) {
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromParts("package", context.getPackageName(), null);
		intent.setData(uri);
		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException ex) {
			UIUtils.showToast("跳转到设置页面失败");
		}
	}

}
