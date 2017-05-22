package cn.wsds.gamemaster.ui.floatwindow;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.widget.Toast;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.ui.UIUtils;

public class CallUtils {
	@SuppressWarnings("deprecation")
	public static void changeToDefaultAnswer(){
		int taskId = -1;
		ActivityManager am = (ActivityManager) AppMain.getContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> list = am.getRunningTasks(1000);
		for (RunningTaskInfo runningTaskInfo : list){
			String pkg = runningTaskInfo.topActivity.getPackageName();
			if (pkg.equals("com.android.phone") || pkg.equals("com.android.incallui")){
				taskId = runningTaskInfo.id;
				break;
			}
		}
		if (taskId != -1){
			am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME);
		} else {
			UIUtils.showToast("TaskId is not found", Toast.LENGTH_LONG);
		}
	}
}
