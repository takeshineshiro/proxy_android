package cn.wsds.gamemaster.data;

import android.content.Context;

import com.subao.common.LogTag;
import com.subao.common.data.AccelGame;
import com.subao.data.InstalledAppInfo;

import java.util.Iterator;
import java.util.List;

import cn.wsds.gamemaster.tools.VPNUtils;

public class AccelGameList implements Iterable<AccelGame> {

	private static AccelGameList instance;

	private final AccelGameMap accelGameMap;

	private final List<AccelGame> accelGameList;

	public static void init(Context context) {
		if (instance == null) {
			instance = new AccelGameList(context);
		}		
	}

	public static AccelGameList getInstance() {
		return instance;
	}

	private AccelGameList(Context context) {
		this.accelGameMap = new AccelGameMap(context);
		accelGameList = VPNUtils.getAccelGameList(LogTag.GAME);
		onDataChange();
	}

	private void onDataChange() {
		int size = 0;
		if(accelGameList != null) {
			size = accelGameList.size();
		}
		this.accelGameMap.assign(this, size);
	}
	
	public AccelGame findAccelGame(InstalledAppInfo info) {
		return info == null ? null : this.findAccelGame(info.getPackageName(), info.getAppLabel());
	}

	public AccelGame findAccelGame(String packageName, String appLabel) {
		return this.accelGameMap.findAccelGame(packageName, appLabel);
	}

	/**
	 * 判断给定的AppLabel是否包含“黑词”
	 */
	public boolean doesAppLabelIncludeBlackWord(String appLabel) {
		return this.accelGameMap.doesAppLabelIncludeBlackWord(appLabel);
	}

	@Override
	public Iterator<AccelGame> iterator() {
		return accelGameList.iterator();
	}

	public int getCount() {
		if (accelGameList != null) {
			return accelGameList.size();
		}
		return 0;
	}
}
