package com.subao.upgrade;

import com.subao.common.data.*;
import com.subao.common.data.Defines;
import com.subao.common.io.FileOperator;
import com.subao.common.io.Persistent;
import com.subao.common.io.PersistentFactory;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.thread.ThreadPool;
import  com.subao.common.net.NetManager;

import java.io.File;
import java.io.IOException;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;

/**
 * APP自身的升级管理器
 */
public class Upgrader {

	/**
	 * 回调接口
	 */
	public interface Callback {

		/**
		 * 当检查结果从服务器取得后，回调此方法
		 * 
		 * @param items
		 *            {@link com.subao.upgrade.PortalUpgradeConfig.Items}
		 *            ，为null时表示没有更新版本
		 * @param minVer
		 *            服务器上配置的“客户端最低要求版本号”
		 */
		void onCheckNewVersionResult(boolean errorWhenDownload, PortalUpgradeConfig.Items items, int minVer);
	}

	private static class Checker extends PortalUpgradeConfig {

		private Callback callback;
		private final String currentChannel;
		private final int currentVersionCode;
		private final IgnoreVersionCodeList ignoreVersionCodeList;

		public Checker(
				PortalUpgradeArguments arguments,
				String currentChannel,
				int currentVersionCode,
				IgnoreVersionCodeList ignoreVersionCodeList,
				Callback callback) {
			super(arguments);
			this.callback = callback;
			this.currentChannel = currentChannel;
			this.currentVersionCode = currentVersionCode;
			this.ignoreVersionCodeList = ignoreVersionCodeList;
		}

		@Override
		protected void onPostExecute(PortalDataEx dataResult) {
			Callback callback = this.callback;
			if (callback != null) {
				if (dataResult.data == null) {
					callback.onCheckNewVersionResult(false, null, 0);
				} else {
					PortalUpgradeData dataContent;
					try {
						dataContent = DataCreator.createPortalData(dataResult.getData());
					} catch (IOException e) {
						callback.onCheckNewVersionResult(false, null, 0);
						return;
					}
					Items items = dataContent.find(currentChannel, currentVersionCode, ignoreVersionCodeList);
					callback.onCheckNewVersionResult(true, items, dataContent.getMinVer());
				}
				this.callback = null;
			}
		}

		/**
		 * 开始下载
		 */
		public void start() {
			PortalDataEx localData = loadFromPersistent();
			this.executeOnExecutor(ThreadPool.getExecutor(), localData);
		}
	}

	final NetTypeDetector netTypeDetector;
	final Persistent persistentUpgradeConfig;
	final IgnoreVersionCodeList ignoreVersionCodeList;

	public Upgrader(
		NetTypeDetector netTypeDetector,
		Persistent persistentUpgradeConfig,
		IgnoreVersionCodeList ignoreVersionCodeList) {
		this.netTypeDetector = netTypeDetector;
		this.persistentUpgradeConfig = persistentUpgradeConfig;
		this.ignoreVersionCodeList = ignoreVersionCodeList;
	}

	/**
	 * 判断是否有更新的版本
	 */
	public void checkNewVersion(
		String currentChannel,
		int currentVersionCode,
		Callback callback) {

		ServiceConfig serviceConfig = new ServiceConfig();
		serviceConfig.loadFromFile(null, false);
		String version = AppMain.getContext().getResources().getString(R.string.app_version);
		PortalUpgradeArguments arguments = new PortalUpgradeArguments(
				Defines.REQUEST_CLIENT_TYPE_FOR_APP, version,
				serviceConfig.getPortalServiceLocation(), new NetManager(AppMain.getContext()));
		Checker checker = new Checker(
				arguments,
				currentChannel, currentVersionCode,
				ignoreVersionCodeList, callback);
		checker.start();
	}

	private static class PortalUpgradeArguments extends PortalDataDownloader.Arguments {
		public PortalUpgradeArguments(String clientType, String version, ServiceLocation serviceLocation, NetTypeDetector netTypeDetector) {
			super(clientType, version, serviceLocation, netTypeDetector);
		}

		@Override
		public Persistent createPersistent(String filename) {
			File file = FileOperator.getDataFile(filename);
			return PersistentFactory.createByFile(file);
		}
	}
}
