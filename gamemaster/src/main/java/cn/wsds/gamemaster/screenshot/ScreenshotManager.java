package cn.wsds.gamemaster.screenshot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;

import com.subao.utils.FileUtils;

import java.io.File;
import java.util.Calendar;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.event.TaskManager;
import cn.wsds.gamemaster.tools.RootUtil;
import cn.wsds.gamemaster.tools.RootUtil.OnRequestRootListener;
import cn.wsds.gamemaster.tools.RootUtil.RequestRootResult;
import cn.wsds.gamemaster.tools.ScreenCapUtils;
import cn.wsds.gamemaster.tools.ScreenCapUtils.OnScreenCapListener;
import cn.wsds.gamemaster.ui.ActivityScreenShot;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindow;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindow.OnDestroyListener;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowInGame;
import cn.wsds.gamemaster.ui.floatwindow.ScreenShotMask;

/**
 * 截屏管理
 */
@SuppressLint("DefaultLocale")
public class ScreenshotManager {
	
	private static final ScreenshotManager instance = new ScreenshotManager();
	public static final String SCREENSHOT_TMP_FILE = "tmp.jpg";
	private ScreenshotManager() {}
	private boolean screenshoting = false;

	/**
	 * 点击截屏时
	 */
	public static void onScreenShot(){
		if(!RootUtil.isRoot()){
			UIUtils.showToast("截屏需要Root权限，你的手机尚未root");
			return;
		}

		if(RootUtil.isGotRootPermission()){
			instance.toScreenShot();
			return;
		}
		UIUtils.showToast("截屏需要Root权限，请授权");

		RootUtil.requestRoot(new OnRequestRootListener() {
			
			@Override
			public void onRequestRoot(RequestRootResult result) {
				if(RequestRootResult.Succeed == result){
					instance.toScreenShot();
				}else{
					UIUtils.showToast("获取Root权限失败，无法截屏，请确认授权root");			
				}
			}
		});
	}
	
	private void onInterrupt(){
		ScreenshotManager.changeFinishState();
		FloatWindowInGame.setInstanceVisibility(View.VISIBLE);
	}
	
	public static void changeFinishState(){
		instance.screenshoting = false; 
	}
	
	public static boolean isScreenshoting(){
		return instance.screenshoting;
	}
	
	private void changeStartState(){
		this.screenshoting = true;
	}

	/**
	 * 去截屏
	 */
	private void toScreenShot() {
		changeStartState();
		FloatWindowInGame.setInstanceVisibility(View.GONE);
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				showShutter();
			}
		};
		if(!MainHandler.getInstance().postDelayed(r, 50)){
			showShutter();
		}
	}
	
	private void showShutter() {
		OnDestroyListener listener = new OnDestroyListener() {
			
			@Override
			public void onFloatWindowDestroy(FloatWindow who) {
				beginScreenshot();
			}
		};
		ShutterPrompt prompt = ShutterPrompt.create();
		prompt.setOnDestroyListener(listener);
	}
	
	private void beginScreenshot() {
		final Context context = AppMain.getContext();
//		StatisticDefault.addEvent(context, StatisticDefault.Event.CLICK_SCREENSHOT);
		// 调用截屏
		String path = FileUtils.getDataFile(SCREENSHOT_TMP_FILE).getPath();
		boolean ok = ScreenCapUtils.screenCap(context, path, new OnScreenCapListener() {
			
			@Override
			public void onScreenCap(boolean result) {
				ScreenShotMask.destroyInstance();
				if(TaskManager.getInstance().getCurrentForegroundGame() == null){
					onInterrupt();
					String filePathName = ScreenshotManager.saveToMediaStore();
					onFinished(!TextUtils.isEmpty(filePathName));
					return;
				}
				String toastMessage;
				if(result){
					// 启动界面
					Intent i = new Intent(context, ActivityScreenShot.class);
//					i.putExtra("elapsed", benginTime);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(i);
					toastMessage = "截屏成功";
				}else{
					onInterrupt();
					toastMessage = "截屏失败";
				}
				UIUtils.showToast(toastMessage);
			}
		});
		if (ok) {
			MainHandler.getInstance().showScreenShotMask(500);
		}else{
			onInterrupt();
		}
	}
	
	/** 保存到本地图库 */
	public static String saveToMediaStore() {
		try {
			File fileSource = FileUtils
					.getDataFile(ScreenshotManager.SCREENSHOT_TMP_FILE);
			Calendar calendar = Calendar.getInstance();
			String name = String.format(
					"迅游手游截图_%d-%02d-%02d-%02d-%02d-%02d.jpg",
					calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH) + 1,
					calendar.get(Calendar.DAY_OF_MONTH),
					calendar.get(Calendar.HOUR_OF_DAY),
					calendar.get(Calendar.MINUTE),
					calendar.get(Calendar.SECOND));
			// 目标文件
			File path = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			path = new File(path, "Screenshots");
			path.mkdirs();
			File fileDest = new File(path, name);
			if (FileUtils.copyFile(fileSource, fileDest, false)) {
				// 通知Media Scanner
				MediaScannerConnection.scanFile(AppMain.getContext(),
						new String[] { fileDest.getAbsolutePath() }, null, null);
				return fileDest.getPath();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void onFinished(boolean result) {
		String toastResult = result ? "图片已保存到本地图库" : "保存失败";
		UIUtils.showToast(toastResult);
		FileUtils.deleteFileOrDirectory(FileUtils
				.getDataFile(ScreenshotManager.SCREENSHOT_TMP_FILE));
//		StatisticDefault.addEvent(AppMain.getContext(),
//				StatisticDefault.Event.CLICK_SCREENSHOT_CANCEL);
	}
}
