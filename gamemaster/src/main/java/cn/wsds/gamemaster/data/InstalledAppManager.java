package cn.wsds.gamemaster.data;

import java.util.List;

import com.subao.data.InstalledAppInfo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.SparseArray;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;

/**
 * 已安装应用的管理器
 */
public class InstalledAppManager extends java.util.Observable {

	public static class Entry {
		public final String packageName;
		public final String appLabel;

		public Entry(String packageName, String appLabel) {
			this.packageName = packageName;
			this.appLabel = appLabel;
		}
	}

	private static InstalledAppManager instance;

	public static InstalledAppManager getInstance() {
		return instance;
	}

	public static void init(Context context) {
		if (instance == null) {
			instance = new InstalledAppManager(context);
		}
	}

	private final Context context;

	/** UID到{@link Entry}的映射 */
	private final SparseArray<Entry> container = new SparseArray<Entry>();

	/** 网速大师的UID */
	private int uidOfWSDS = -1;

	private InstalledAppManager(Context context) {
		this.context = context.getApplicationContext();
		initContainer();
		registerEvents();
	}

	/** 取“网速大师”的UID */
	public int getUidOfWSDS() {
		return this.uidOfWSDS;
	}

	public Entry find(int uid) {
		return container.get(uid);
	}

	private void registerEvents() {
		TriggerManager.getInstance().addObserver(new EventObserver() {
			@Override
			public void onAppInstalled(InstalledAppInfo info) {
				container.put(info.getUid(), new Entry(info.getPackageName(), info.getAppLabel()));
				if (isItWSDS(info.getPackageName())) {
					uidOfWSDS = info.getUid();
				}
			}

			@Override
			public void onAppRemoved(String packageName) {
				for (int i = container.size() - 1; i >= 0; --i) {
					int key = container.keyAt(i);
					String pn = container.get(key).packageName;
					if (packageName.equals(pn)) {
						container.removeAt(i);
						break;
					}
				}
				if (isItWSDS(packageName)) {
					uidOfWSDS = -1;
				}
			}

			@Override
			public void onMediaMounted() {
				initContainer();
			}
		});
	}

	private void initContainer() {
		container.clear();
		PackageManager pm = context.getPackageManager();
		if (pm == null) {
			return;
		}
		try {
			List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
			if (list != null) {
				for (ApplicationInfo ai : list) {
					container.put(ai.uid, new Entry(ai.packageName, ai.loadLabel(pm).toString()));
					if (isItWSDS(ai.packageName)) {
						this.uidOfWSDS = ai.uid;
					}
				}
			}
		} catch (RuntimeException e) {
			// 前方高能，请注意。在某个坑爹的手机上面，pm.getInstalledApplications函数里面抛了NullPointerExcepion
			// 妈蛋，每个Android程序员上辈子都是折翼的天使
		}
	}

	private static boolean isItWSDS(String packageName) {
		return "com.subao.husubao".equals(packageName);
	}
}
