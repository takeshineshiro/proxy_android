package cn.wsds.gamemaster.tools;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

class AppsWithUsageAccessImpl {
	
	/**
	 * 获得最近的应用统计数据
	 *   不需要考虑新版本问题，方法中低版本默认返回null
	 * @param duration 持续在这段时间内
	 * @return
	 * return null 持续在 duration这个时间内应用没有更新 或者新版本手机 
	 */
	@SuppressLint("NewApi")
	static String getRecentUsagePackageName(Context context, long duration) {
		long ts = System.currentTimeMillis();
		UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
		List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST,0, ts);
		if (queryUsageStats == null || queryUsageStats.isEmpty()) {
			return null;
		}
		UsageStats recentStats = null;
		for (UsageStats usageStats : queryUsageStats) {
			if(recentStats == null || (usageStats.getLastTimeUsed() <=System.currentTimeMillis() && recentStats.getLastTimeUsed() < usageStats.getLastTimeUsed())){
				recentStats = usageStats;
			}	
		}
		return recentStats == null ? null : recentStats.getPackageName();
	}

	/**
	 *  the user enabled my application
	 *  不需要考虑新版本问题，方法中低版本默认返回false
	 * @return
	 */
	@SuppressLint("NewApi")
	static boolean hasEnable(Context context){
		long ts = System.currentTimeMillis();
		UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
		List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST,0, ts);
		if (queryUsageStats == null || queryUsageStats.isEmpty()) {
			return false;
		}
		return true;
	}
	

	
}