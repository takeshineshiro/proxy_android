package cn.wsds.gamemaster.ui.accel.failprocessor;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.provider.Settings;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.CommonDesktopDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenWorker;
import cn.wsds.gamemaster.ui.accel.AccelOpener;

/**
 * 加速开启错误处理
 */
public abstract class FailProcesser {
	
	public void process(AccelOpener accelOpener,FailType type, Activity activity) {
		switch(type) {
		case WifiAP:
			onFailWifiAp(activity);
			break;
		case NetworkCheck:
			showDialogWhenNetReject(activity);
			break;
		case WAP:
			onFailAccelWhenWAP(activity);
			break;
		case ImpowerError:
			onImpowerError(activity,accelOpener);
			break;
		case ImpowerReject:
			onImpowerReject(activity,accelOpener);
			break;
		case StartError:
			onStartError(activity,accelOpener);
			break;
		case DefectModel:
			onDefectModel(activity,accelOpener);
			break;
		default:
			break;  
		}
	}

	protected void autoChangeAccelModeAndOpenAccel(AccelOpener accelOpener) {
		AccelOpenWorker accelOpenWorker = accelOpener.getAccelOpenWorker(AppMain.getContext());
		if(accelOpenWorker!=null){
			ConfigManager.getInstance().setRootMode(!accelOpener.isRootModel());
			ConfigManager.getInstance().setAutoChangedAccelModel();
			accelOpenWorker.openAccel();
		}
	}
	
	protected abstract void onDefectModel(Activity activity,AccelOpener accelOpener);

	protected abstract void onStartError(Activity activity, AccelOpener accelOpener);

	protected abstract void onImpowerError(Activity activity,AccelOpener accelOpener);

	protected abstract void onImpowerReject(Activity activity,AccelOpener accelOpener);


	protected void onFailWifiAp(final Activity activity) {
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean ok = (which == DialogInterface.BUTTON_POSITIVE);

				if (ok) {
					gotoSystemSetting(activity,Settings.ACTION_WIRELESS_SETTINGS);
				}
			}
		};

		showDialogWhenWiFiAP(activity,listener,null);
	}
	
	private void gotoSystemSetting(Context context, String action) {
		try {
			Intent intent = new Intent(action);
			if (context == null) {
				context = AppMain.getContext();
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			}
			context.startActivity(intent);
		} catch (RuntimeException re) {
			UIUtils.showToast("未能在您的系统中找到相关设置页面，请手工设置");
		}
	}
	

	/** 检测到WiFi热点时，显示对话框 */
	protected void showDialogWhenWiFiAP(Activity activity,DialogInterface.OnClickListener listener, OnCancelListener cancelListener) {
		CommonDialog dialog = buildDialog(activity);
		dialog.setMessage("您正在使用WiFi热点共享，开启游戏加速会导致共享用户无法上网，是否关闭热点？  ");
		dialog.setPositiveButton("关闭热点", listener);
		dialog.setNegativeButton("稍后再说", listener);
		dialog.setCanceledOnTouchOutside(true);
		dialog.setCanceledOnWindowLoseFocused(true);
		dialog.setOnCancelListener(cancelListener);
		dialog.show();
	}

	/** 检测到网络权限被禁时，显示一个框 */
	protected void showDialogWhenNetReject(Activity activity) {
		CommonDialog dialog = buildDialog(activity);
		Context context = getSafeContext(activity);
		dialog.setMessage(context.getString(R.string.app_name) 
			+ "被禁止网络访问，无法为您的游戏加速。\n请检查您的手机网络设置或第三方管理软件的限制");
		dialog.setPositiveButton("我知道了", null);
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	protected CommonDialog buildDialog(Activity activity) {
		if(activity==null || activity.isFinishing()){
			return new CommonDesktopDialog();
		}else{
			return new CommonAlertDialog(activity);
		}
	}	
	
	/** 检测到WAP方式时，显示对话框 */
	protected void onFailAccelWhenWAP(final Activity activity) {
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean gotoSetting = (which == DialogInterface.BUTTON_POSITIVE);
				if (gotoSetting) {
					gotoSystemSetting(activity,Settings.ACTION_APN_SETTINGS);
				}
			}
		};
		CommonDialog dialog = buildDialog(activity);
		Context context = getSafeContext(activity);
		dialog.setMessage(String.format("您正在使用WAP网络接入点，%s将无法为游戏加速\n建议您切换到NET接入点使用更好的网络服务",
				context.getString(R.string.app_name)));
		dialog.setPositiveButton("去设置", listener);
		dialog.setNegativeButton("取消", listener);

		dialog.setCanceledOnWindowLoseFocused(true);
		dialog.show();
	}

	protected Context getSafeContext(Activity activity) {
		Context context = activity;
		if(context == null)
			context = AppMain.getContext();
		return context;
	}
	
	protected void promptChangeAccelModel(Activity activity,final AccelOpener accelOpener,String mess,final OnCancelListener onCancelListener){
		
		CommonDialog dialog = buildDialog(activity);
		dialog.setMessage(mess);
		OnClickListener clickListener = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(DialogInterface.BUTTON_POSITIVE == which){
					autoChangeAccelModeAndOpenAccel(accelOpener);
				}else{
					onCancelListener.onCancel(dialog);
				}
			}
		};
		dialog.setPositiveButton("是", clickListener);
		dialog.setNegativeButton("否", clickListener);
		dialog.setOnCancelListener(onCancelListener);
		dialog.show();
	}

}
