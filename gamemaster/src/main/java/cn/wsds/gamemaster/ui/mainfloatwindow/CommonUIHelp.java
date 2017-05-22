package cn.wsds.gamemaster.ui.mainfloatwindow;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import cn.wsds.gamemaster.dialog.UsageStateHelpDialog;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenHelpManager.OnFinshListener;

/**
 * 小米/华为 5.0以下用户 
 */
public class CommonUIHelp extends OpenFloatwindowHelp{
	
	@Override
	public void doOpenHelp(final Activity activity,final OnFinshListener onFinshListener) {
		UsageStateHelpDialog.open(activity,new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				if(onFinshListener!=null){
					onFinshListener.onFinish();
				}
			}
		});
	}

}
