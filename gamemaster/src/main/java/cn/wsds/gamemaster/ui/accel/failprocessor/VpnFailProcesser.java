package cn.wsds.gamemaster.ui.accel.failprocessor;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.statistic.StatisticAccProcessStart;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpener;

/**
 * VPN 方式开启出错处理
 */
public class VpnFailProcesser extends FailProcesser{

	@Override
	protected void onStartError(Activity activity,final AccelOpener accelOpener) {
		StatisticAccProcessStart.getInstance().addStep(StatisticAccProcessStart.STEP_VPN_ACCEL_ERROR);
		if(ConfigManager.getInstance().isAutoChangedAccelModel()){
			StatisticAccProcessStart.getInstance().end();
			UIUtils.showToast("未知原因开启失败");
			return;
		}
		

		StatisticAccProcessStart.getInstance().end();
		UIUtils.showToast("开启失败，请尝试重启手机");
	}

	@Override
	protected void onImpowerError(Activity activity,AccelOpener accelOpener) {
		onImpowerException();
	}

	private void onImpowerException() {
		if(ConfigManager.getInstance().isAutoChangedAccelModel()){
			UIUtils.showToast("加速开启失败");
		}else{
			UIUtils.showToast("未获得授权，开启失败");
 		}
	}

	@Override
	protected void onImpowerReject(Activity activity, AccelOpener accelOpener) {
		StatisticAccProcessStart.getInstance().end();
		onImpowerException();
	}

	@Override
	public void onDefectModel(Activity activity, AccelOpener accelOpener) {
		String message = UIUtils.isLollipopUser() ? 
				"手机系统缺失VPN模块，无法开启加速。" : 
			"手机系统缺失VPN模块，ROOT手机可正常使用加速功能。";
		CommonDialog dialog = buildDialog(activity);
		dialog.setMessage(message);
		dialog.setPositiveButton("我知道了", null);
		dialog.show();				
		StatisticAccProcessStart.getInstance().end(StatisticAccProcessStart.STEP_VPN_MISSING_MODULE,StatisticAccProcessStart.STEP_VPN_ACCEL_ERROR);
	}
	
	@Override
	protected void onFailAccelWhenWAP(Activity activity) {
		super.onFailAccelWhenWAP(activity);
		StatisticAccProcessStart.getInstance().end(StatisticAccProcessStart.STEP_PROMPT_WAP,StatisticAccProcessStart.STEP_VPN_ACCEL_ERROR);
	}
	
	@Override
	protected void showDialogWhenNetReject(Activity activity) {
		super.showDialogWhenNetReject(activity);
		StatisticAccProcessStart.getInstance().end(StatisticAccProcessStart.STEP_PROMPT_NETWORK_REJECT,StatisticAccProcessStart.STEP_VPN_ACCEL_ERROR);
	}

	@Override
	protected void onFailWifiAp(Activity activity) {
		super.onFailWifiAp(activity);
		StatisticAccProcessStart.getInstance().end(StatisticAccProcessStart.STEP_PROMPT_WIFI_AP_OPENED,StatisticAccProcessStart.STEP_VPN_ACCEL_ERROR);
	}
}
