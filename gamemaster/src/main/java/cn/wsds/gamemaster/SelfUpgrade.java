package cn.wsds.gamemaster;

import android.app.Activity;
import android.content.Context;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.io.PersistentFactory;
import com.subao.common.net.NetTypeDetector.NetType;
import com.subao.common.utils.InfoUtils;
import com.subao.net.NetManager;
import com.subao.upgrade.IgnoreVersionCodeList;
import com.subao.upgrade.PortalUpgradeConfig;
import com.subao.upgrade.PortalUpgradeConfig.Items;
import com.subao.upgrade.Upgrader;
import com.subao.utils.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;

import cn.wsds.gamemaster.data.DeviceInfo;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.ui.DialogUpgrade;

public class SelfUpgrade {

	private static final String TAG = LogTag.UPGRADE;

	private static final String FILENAME_CONFIG = "version.portal";
	private static final String FILENAME_IGNORE_LIST = "version.ignore";

	private class NetChangeListener extends EventObserver {

		@Override
		public void onNetChange(NetType netType) {
			if (netType != NetType.DISCONNECT && netType != NetType.MOBILE_2G) {
				Logger.d(TAG, "Network change, check version");
				check(null);
			}
		}

	}

	private class CallbackWrapper implements Upgrader.Callback {

		private final WeakReference<Upgrader.Callback> rawCallback;

		public CallbackWrapper(Upgrader.Callback callback) {
			this.rawCallback = new WeakReference<Upgrader.Callback>(callback);
		}

		@Override
		public void onCheckNewVersionResult(boolean errorWhenDownload, Items items, int minVer) {
			SelfUpgrade.this.minVer = minVer;
			SelfUpgrade.this.items = items;
			if (errorWhenDownload) {
				Logger.d(TAG, "Error when check version");
				if (netChangeListener == null) {
					netChangeListener = new NetChangeListener();
					TriggerManager.getInstance().addObserver(netChangeListener);
					Logger.d(TAG, "Add net observer");
				}
			} else {
				Logger.d(TAG, "No error when check version");
				if (netChangeListener != null) {
					TriggerManager.getInstance().deleteObserver(netChangeListener);
					netChangeListener = null;
					Logger.d(TAG, "Remove net observer");
				}
			}
			Upgrader.Callback callback = rawCallback.get();
			if (null != callback) {
				callback.onCheckNewVersionResult(errorWhenDownload, items, minVer);
			}
		}

	}

	private static final SelfUpgrade instance = new SelfUpgrade();

	private String myChannel;

	private int myVersionCode;

	/**
	 * 最后一次取到的，最小版本号
	 */
	private int minVer;

	/**
	 * 最近一次取到的，需要更新的版本信息
	 * 
	 * @see PortalUpgradeConfig.Item
	 */
	private PortalUpgradeConfig.Items items;

	private NetChangeListener netChangeListener;

	/**
	 * 用于管理“忽略版本列表”
	 * 
	 * @see IgnoreVersionCodeList
	 */
	private final IgnoreVersionCodeList ignoreVersionCodeList;

	public static SelfUpgrade getInstance() {
		return instance;
	}

	public SelfUpgrade init(Context context) {
		this.myVersionCode = InfoUtils.getVersionCode(context);
		this.myChannel = DeviceInfo.getUmengChannel(context);
		return this;
	}

	private SelfUpgrade() {
		File file = FileUtils.getDataFile(FILENAME_IGNORE_LIST);
		ignoreVersionCodeList = new IgnoreVersionCodeList(PersistentFactory.createByFile(file));
	}

	/**
	 * 发起一个更新检查请求，结果在Callback里返回
	 * 
	 * @param callback
	 *            回调
	 */
	public void check(Upgrader.Callback callback) {
		Upgrader upgrader = new Upgrader(
			NetManager.getInstance(),
			PersistentFactory.createByFile(FileUtils.getDataFile(FILENAME_CONFIG)),
			this.ignoreVersionCodeList);
		upgrader.checkNewVersion(myChannel, myVersionCode, new CallbackWrapper(callback));
	}

	/**
	 * 忽略指定的版本
	 */
	public void ignoreVersion(PortalUpgradeConfig.Item item) {
		this.ignoreVersionCodeList.add(item.channel, item.version, item.verCode);
		this.items = null;
	}

	/**
	 * 检查是否有更新版本，根据需要决定是否弹出对话框提示用户升级
	 * 
	 * @param activity
	 *            {@link Activity}
	 */
	public boolean showDialogWhenNeed(Activity activity, boolean includeIgnore, boolean allowIgnore) {
		if (this.items == null) {
			return false;
		}
		PortalUpgradeConfig.Item item;
		if (this.items.normal == null) {
			if (!includeIgnore || this.items.ignored == null) {
				return false;
			}
			item = items.ignored;
		} else {
			item = items.normal;
		}

		int currentVerCode = InfoUtils.getVersionCode(activity);
		DialogUpgrade dialog = new DialogUpgrade(activity, currentVerCode, this.minVer, item, allowIgnore);
		dialog.show();
		return true;
	}

}
