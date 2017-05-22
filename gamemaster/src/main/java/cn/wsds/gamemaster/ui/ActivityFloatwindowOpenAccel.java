package cn.wsds.gamemaster.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import cn.wsds.gamemaster.ui.accel.AccelOpenDesktopListener;
import cn.wsds.gamemaster.ui.accel.AccelOpenDesktopListener.OnEndListener;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpener;
import cn.wsds.gamemaster.ui.accel.AccelOpener.OpenSource;


/**
 * 空界面在悬浮窗开启加速
 */
public class ActivityFloatwindowOpenAccel extends ActivityBase{
	
	/**
	 * 开启加速
	 * @param context
	 */
	public static void startAccel(Context context){
		AccelOpener opener = AccelOpenManager.createOpener(new AccelOpenDesktopListener(null),OpenSource.Floatwindow);
		if(opener.isRootModel() || opener.isGotPermission()){//root 模式直接开启||已经取得授权
			opener.tryOpen(null);
		}else{//vpn模式需要 activity 则打开界面
			openActivity(context);
		}
	}
	
	public static void startAccelNoPormpt(){
		AccelOpener opner = AccelOpenManager.createOpener(new AccelOpenDesktopListener(null),OpenSource.Floatwindow);
		opner.tryOpen(null);
	}

	/**
	 * 打开 activity 界面
	 * @param context
	 */
	private static void openActivity(Context context) {
		Intent intent = new Intent(context, ActivityFloatwindowOpenAccel.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	//
	private AccelOpener accelOpener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accelOpener = AccelOpenManager.createOpener(new AccelOpenDesktopListener(new OnEndListener() {
			
			@Override
			public void onEnd(boolean result) {
				finish();
				accelOpener = null;
			}
		}),OpenSource.Floatwindow);
		accelOpener.tryOpen(this);
	}
	
	@Override
	public void onActivityResult(int request, int result, Intent data) {
		if (this.accelOpener != null) {
			this.accelOpener.checkResult(request, result, data);
//			this.accelOpener = null;
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(this.accelOpener!=null){
			this.accelOpener.interrupt();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		accelOpener = null;
	}
}