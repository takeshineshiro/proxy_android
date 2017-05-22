package cn.wsds.gamemaster.ui.mainfloatwindow;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.dialog.UsageStateHelpDialog;
import cn.wsds.gamemaster.tools.AppsWithUsageAccess;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenHelpManager.OnFinshListener;
import cn.wsds.gamemaster.ui.view.OpenFloatWindowHelpDialog;

/**
 * 小米/华为 5.0 
 */
public class SpecialUILollipopHelp extends OpenFloatwindowHelp{
	

	@Override
	public void doOpenHelp(final Activity activity,final OnFinshListener onFinshListener) {
		OpenFloatWindowHelpDialog.open(activity, null, new Runnable() {
			@Override
			public void run() {
				if(!ConfigManager.getInstance().getShowUsageStateHelpDialog() || AppsWithUsageAccess.hasEnable()){
					// 已经显示授权引导，或者没有开关已经是打开的状态
					if(onFinshListener!=null){
						onFinshListener.onFinish();
					}
					return;
				}
				UsageStateHelpDialog.open(activity, new OnDismissListener() {
					
					@Override
					public void onDismiss(DialogInterface dialog) {
						if(onFinshListener!=null){
							onFinshListener.onFinish();
						}
					}
				});
			}
		});
	}

}
