package cn.wsds.gamemaster.data;

/**
 * 在适当的时候，执行数据更新检查
 */
public class AccelDataUpdater {

	/*private static final String TAG = LogTag.DATA;

	private static final AccelDataUpdater instance = new AccelDataUpdater();

	private boolean lastSucceedNodes, lastSucceedGames;

	private static abstract class DataDownloadListener implements AccelDataListManager.Listener {

		@Override
		public void onDataDownloadError() {
			String param = getFailStatisticEventParam();
			Statistic.addEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_RES_REQUEST_FAIL_NEW, param);
			Logger.d(TAG, "Downlaod Fail: " + param);
		}

		protected abstract String getFailStatisticEventParam();

		@Override
		public void onDataDownloadComplete(boolean hasNewData) {

		}
	}

	private class AccelNodesDownloadListener extends DataDownloadListener {

		@Override
		protected String getFailStatisticEventParam() {
			return "NODES";
		}

		@Override
		public void onDataDownloadComplete(boolean hasNewData) {
			super.onDataDownloadComplete(hasNewData);
			lastSucceedNodes = true;
			if (!hasNewData) {
				return;
			}
			//TODO: 节点列表变了？要通知代理层吗？
		}

		@Override
		public void onDataDownloadError() {
			super.onDataDownloadError();
			lastSucceedNodes = false;
		}
	}

	private class AccelGamesDownloadListener extends DataDownloadListener {

		@Override
		protected String getFailStatisticEventParam() {
			return "GAMES";
		}

		@Override
		public void onDataDownloadComplete(boolean hasNewData) {
			super.onDataDownloadComplete(hasNewData);
			lastSucceedGames = true;
			if (!hasNewData) {
				return;
			}
			// 更新GameManager并保存
			GameManager gm = GameManager.getInstance();
			gm.updateSupportGames();
			gm.save(true);
		}

		@Override
		public void onDataDownloadError() {
			super.onDataDownloadError();
			lastSucceedGames = false;
		}
	};

	public static AccelDataUpdater getInstance() {
		if (GlobalDefines.CHECK_MAIN_THREAD) {
			if (!ThreadUtils.isInAndroidUIThread()) {
				MainHandler.getInstance().showDebugMessage("DataUpdater.getInstance() call in non-main thread");
			}
		}
		return instance;
	}

	private AccelDataUpdater() {
		TriggerManager.getInstance().addObserver(new EventObserver() {
			@Override
			public void onNetChange(NetTypeDetector.NetType state) {
				if (NetManager.getInstance().isConnected()) {
					if (!lastSucceedGames || !lastSucceedNodes) {
						MainHandler.getInstance().tryUpdateAccelData(0);
					}
				}
			}
		});
	}

	*//**
	 * 执行检查
	 *//*
	public void execute(Context context, boolean force) {
		Logger.d(TAG, String.format("DataUpdater.execute(%b)", force));
		//
		if (force) {
			lastSucceedNodes = lastSucceedGames = false;
		}
		AccelDataListManagerStrategy strategy = new AccelDataListManagerStrategy(context);
		if (!lastSucceedNodes) {
			AccelNodeListManagerImpl.getInstance().download(strategy, new AccelNodesDownloadListener());
		}
		if (!lastSucceedGames) {
			AccelGameList.getInstance().download(strategy, new AccelGamesDownloadListener());
		}
	}*/

}
