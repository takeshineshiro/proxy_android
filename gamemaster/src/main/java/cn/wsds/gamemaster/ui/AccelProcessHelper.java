package cn.wsds.gamemaster.ui;

import android.app.Activity;
import android.content.DialogInterface;
import cn.wsds.gamemaster.debugger.DebugParams;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenWorker;
import cn.wsds.gamemaster.ui.accel.AccelOpener;
import cn.wsds.gamemaster.ui.accel.AccelOpener.OpenSource;

public class AccelProcessHelper {
	private final Activity activity;
	private AccelOpener accelOpener;
	private AccelOpener.Listener accelOpenerListener;
	private final OpenSource openSource;
	
	private static class OpenAccelAlwaysFailTester implements AccelOpener.Tester {
		@Override
		public AccelOpener.Tester.FakeOpenAccelResult fakeOpenAccel(boolean isRootMode) {
			return AccelOpener.Tester.FakeOpenAccelResult.OPEN_ACCEL_FAIL;
		}
	}
	
	public AccelProcessHelper(Activity activity, OpenSource openSource, AccelOpener.Listener accelOpenerListener){
		this.activity= activity;
		this.accelOpenerListener = accelOpenerListener;
		this.openSource = openSource;
	}
	
	public interface OnCloseAnimationListener{
		public void onCloseAnimation();
	}
	
	public void openAccel(AccelOpenWorker accelOPenWorker){
		if (accelOpener == null) {// 如果已经是在请求开启加速过程中，就忽略其他请求，请求结束时会将这个对象置空
			this.accelOpener = AccelOpenManager.createOpener(accelOpenerListener, this.openSource,
				DebugParams.getOpenAccelAlwaysFail() ? new OpenAccelAlwaysFailTester() : null);
			this.accelOpener.setAccelOPenWorker(accelOPenWorker);
			this.accelOpener.tryOpen(activity);
		}
	}
	
	public static void closeAccel(final CloseReason closeReason,Activity activity,final OnCloseAnimationListener onCloseAnimationListener) {
		if (!AccelOpenManager.isStarted()) {
			return;
		}

		CommonAlertDialog dialog = new CommonAlertDialog(activity);
		
		dialog.setTitle("提示");
		dialog.setMessage("关闭加速会中断已连接的游戏，建议保持加速服务开启。");
		dialog.setPositiveButton("开启(推荐)",null);
		dialog.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				if (AccelOpenManager.isStarted()) {
					if (onCloseAnimationListener != null){
						onCloseAnimationListener.onCloseAnimation();
					}
					AccelOpenManager.close(closeReason);
				}
			}
		});
		dialog.show();
	}
	
	
	public AccelOpener getAccelOpener(){
		return this.accelOpener;
	}
	
	public void clearAccelOpener(){
		this.accelOpener = null;
	}
	
}
