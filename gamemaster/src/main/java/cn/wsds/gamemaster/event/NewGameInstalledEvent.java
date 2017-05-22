package cn.wsds.gamemaster.event;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.text.TextUtils;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.UserSession;

import com.subao.data.InstalledAppInfo;

public class NewGameInstalledEvent extends EventObserver {

	/**
	 * 安装时间（手机本地时间）距现在（手机本地时间）是否超出有效时间
	 */
	private static final long MAX_VALID_INTEVAL_TIME = 24 * 3600 * 1000;

	@SuppressLint("CommitPrefEdits")
	@Override
	public void onAppInstalled(InstalledAppInfo info) {
		String name = info.getAppLabel();
		recordTimeGameInstalled(name);
	}

	/**
	 * @param name 游戏名称
	 */
	private static void recordTimeGameInstalled(String name) {
		SharedPreferences sharedPreferences = getInfoSharedPreferences();
		if(sharedPreferences==null){
			return;
		}
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putLong(name, now());
		editor.commit();
	}

	private static SharedPreferences getInfoSharedPreferences() {
		
		if(UserSession.isLogined()){		
			return AppMain.getContext().getSharedPreferences(
					"NewGameInstalledInfos"+UserSession.getInstance().getUserId(), 0);
		}
		
		return null;
				
	}

	private static long now() {
		return System.currentTimeMillis();
	}

	@Override
	public void onAppRemoved(String packageName) {
		SharedPreferences sharedPreferences = getInfoSharedPreferences();
		if(sharedPreferences==null){
			return ;
		}
		
		SharedPreferences.Editor editor = sharedPreferences.edit();
		GameInfo gameInfo = GameManager.getInstance().getGameInfo(packageName);
		if (gameInfo == null) {
			return;
		}
		String name = gameInfo.getAppLabel();
		if (TextUtils.isEmpty(name)) {
			return;
		}
		editor.remove(name);
		editor.commit();
	}

	/**
	 * 判断是否是新安装游戏，及安装时间距现在有没有超出有效时间 {@link #MAX_VALID_INTEVAL_TIME}
	 * @param name 游戏名称
	 * @return
	 *  true 是新游戏并且安装时间没有超出有效时间 false 不是新游戏或者安装时间超出有效期
	 */
	public static boolean isValidNewGame(String name) {
		SharedPreferences sharedPreferences = getInfoSharedPreferences();
		if(sharedPreferences==null){
			return false;
		}
		
		long time = sharedPreferences.getLong(name, 0);
		if (time == 0) {
			return false;
		}
		long now = now();
		if (now - time <= MAX_VALID_INTEVAL_TIME) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void whenToAddGamesTask(String appLabel){
		if(TextUtils.isEmpty(appLabel)){
			return;
		}
		InstalledAppInfo[] installedApps = GameManager.getInstance().getInstalledApps();
		for (InstalledAppInfo installedAppInfo : installedApps) {
			if(appLabel.equals(installedAppInfo.getAppLabel())){
				recordTimeGameInstalled(appLabel);
				break;
			}
		}
		
	}
}

