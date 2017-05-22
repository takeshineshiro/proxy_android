package cn.wsds.gamemaster.netdelay;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.net.TrafficStats;
import android.util.Log;
import android.util.SparseArray;

import com.subao.common.Misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


@SuppressLint("DefaultLocale")
public abstract class AppStats {
	private static final boolean LOG = false;
	private static final String TAG = "AppStats";
	
	int uid;
	long recvBytes; // 接收增量
	long sendBytes; // 发送增量

	AppStats(int uid) {
		this.uid = uid;
		recvBytes = 0;
		sendBytes = 0;
	}
	
	public abstract long getRecvBytes(int uid);
	public abstract long getSendBytes(int uid);
	
	/**
	 * 获取消耗流量的App列表
	 * @param context
	 * @param excludeUid 排除哪个uid
	 * @return
	 */
	public static SparseArray<AppStats> getAppList(Context context,	int excludeUid) {
		SparseArray<AppStats> appList = FileStats.getAppList(excludeUid); // 优先从文件读取流量
		if (appList == null || appList.size() == 0) {
			if (LOG) {
				Log.w(TAG, "FileStats.getAppList return empty");
			}
			
			appList = ApiStats.getRunningProcess(context, excludeUid);
		}
		return appList;
	}
	
	
	/////////// 使用TrafficStats类来获取流量信息 ///////////
	private static class ApiStats extends AppStats {	
		ApiStats(int uid) {
			super(uid);
		}
		
		@Override
		public long getRecvBytes(int uid) {
			return TrafficStats.getUidRxBytes(uid);
		}
		
		@Override
		public long getSendBytes(int uid) {
			return TrafficStats.getUidTxBytes(uid);
		}
		
		
		// 排除自己和前台app
		private static SparseArray<AppStats> getRunningProcess(Context context, int excludeUid) {
			// 获取正在运行的应用
			ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			if (am == null) {
				return null;
			}
			List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
			if (processes == null) {
				return null;
			}
			
			SparseArray<AppStats> apps = new SparseArray<AppStats>(processes.size());
			int myUid = android.os.Process.myUid();			
			for (RunningAppProcessInfo processInfo : processes) {
				if (LOG) {
					Log.i(TAG, String.format("process:%s, uid:%d, importance:%d, pkg:%s...", processInfo.processName, processInfo.uid, processInfo.importance,
						processInfo.pkgList[0]));
				}

				if (processInfo.uid == myUid || processInfo.uid == excludeUid || !Misc.isApplicationsUID(processInfo.uid)
					// || processInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND ) { // 前台进程
				) {
					continue;
				}

				Integer key = Integer.valueOf(processInfo.uid);
				if (apps.get(key) == null) {
					ApiStats stats = new ApiStats(processInfo.uid);
					apps.put(key, stats);
					if (LOG) {
						Log.i(TAG, String.format("插入MAP：process:%s, uid:%d,, pkg:%s", processInfo.processName, processInfo.uid, processInfo.pkgList[0]));
					}
				}
			}

			return apps;
		}
	}
	
	
	///////////// 通过分析文件获取流量信息 //////////////
	private static class FileStats extends AppStats {
		private static final String UID_STAT_DIR 	= "/proc/uid_stat";
		private static final String TCP_RCV_FILE 	= "tcp_rcv";
		private static final String TCP_SND_FILE	= "tcp_snd";
		
		FileStats(int uid) {
			super(uid);
		}
		
		@Override
		public long getRecvBytes(int uid) {
			return readTraffic(String.format("%s/%d/%s", UID_STAT_DIR, uid, TCP_RCV_FILE));
		}
		
		@SuppressLint("DefaultLocale")
		@Override
		public long getSendBytes(int uid) {
			return readTraffic(String.format("%s/%d/%s", UID_STAT_DIR, uid, TCP_SND_FILE));
		}
		
		// 解析文件，读取消耗流量
		private static int readTraffic(String filePath) {
			try {
				File file = new File(filePath);
				if (!file.exists()) {
					return 0;
				}
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String str = reader.readLine();
				reader.close();
				return Integer.parseInt(str);
			} catch (Exception e) {
				e.printStackTrace();
				return 0;
			}
		}
		
		// 获取耗费流量的app信息
		public static SparseArray<AppStats> getAppList(int excludeUid) {
			List<String> files = getFileList(UID_STAT_DIR);
			if (files == null) {			
				return null;
			}
			
			SparseArray<AppStats> apps = new SparseArray<AppStats>(files.size());
			int myUid = android.os.Process.myUid();
			for (String name : files) {
				int uid = Integer.parseInt(name);
				if (uid == myUid || uid == excludeUid || !Misc.isApplicationsUID(uid)) {
					continue;
				}
				Integer key = Integer.valueOf(uid);
				if (apps.get(key) == null) {
					FileStats stats = new FileStats(uid);
					apps.put(key, stats);
					if (LOG) {
						Log.i(TAG, String.format("插入MAP：uid:%d", uid));
					}
				}
			}

			return apps;
		}
		
		// 获取某目录下的文件列表
		private static List<String> getFileList(String path) {
			File dir = new File(path);
			if (!dir.isDirectory()) {
				return null;
			}
			
			File[] files = dir.listFiles();
			if (files == null) {
				return null;
			}
			
			List<String> list = new ArrayList<String>();
			for (File file : files) {
				list.add(file.getName());
			}
			return list;
		}
		
	}
	
}
