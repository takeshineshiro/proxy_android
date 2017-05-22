package cn.wsds.gamemaster.ui.mainfloatwindow;

import android.app.Activity;
import cn.wsds.gamemaster.ui.ActivitySettingFloatWindow;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenHelpManager.OnFinshListener;

public class SettingHelp extends OpenFloatwindowHelp {

	@Override
	public void doOpenHelp(Activity activity,final OnFinshListener onFinshListener) {
		UIUtils.turnActivity(activity, ActivitySettingFloatWindow.class);
	}

}
