package cn.wsds.gamemaster.ui.mainfloatwindow;

import android.app.Activity;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenHelpManager.OnFinshListener;
import cn.wsds.gamemaster.ui.view.OpenFloatWindowHelpDialog;

/**
 * 小米/华为 5.0以下用户 
 */
public class SpecialUIHelp extends OpenFloatwindowHelp{
	
	@Override
	public void doOpenHelp(final Activity activity,final OnFinshListener onFinshListener) {
		OpenFloatWindowHelpDialog.open(activity, null, new Runnable() {
			
			@Override
			public void run() {
				if(onFinshListener!=null){
					onFinshListener.onFinish();
				}
			}
		});
	}

}
