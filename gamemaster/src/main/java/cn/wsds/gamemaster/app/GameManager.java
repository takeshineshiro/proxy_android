package cn.wsds.gamemaster.app;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.common.Logger;
import com.subao.common.SuBaoObservable;
import com.subao.common.data.AccelGame;
import com.subao.common.data.SupportGame;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.NetTypeDetector.NetType;
import com.subao.common.net.Protocol;
import com.subao.common.utils.ThreadUtils;
import com.subao.data.InstalledAppInfo;
import com.subao.net.NetManager;
import com.subao.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.LogTagGame;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.data.AccelGameList;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.GameForegroundDetector;
import cn.wsds.gamemaster.event.TaskManager;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.net.FirstSegmentNetDelayContainer;
import cn.wsds.gamemaster.pb.Proto;
import cn.wsds.gamemaster.service.aidl.VpnSupportGame;
import cn.wsds.gamemaster.thread.AccelDetailsObserver;
import cn.wsds.gamemaster.tools.ProcessLauncher;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

//线程安全的游戏管理器
public class GameManager extends SuBaoObservable<GameManager.Observer> implements GameInfoContainer {

	private static final String GAME_DATA_FILE = "game.data";
	private final int SAVE_DELAY = 2 * 60 * 1000;

	private static final String TAG = LogTagGame.TAG;

	public static final GameInfo UNINSTALLED_GAMEINFO = new GameInfo(-2, null, null, null, false);

	/** 加速状态 */
	public static enum AccelState {
		/** 等待发起连接 */
		WAIT,
		/** 加速成功（非透传） */
		ACCEL,
		/** 透传 */
		DIRECT_TRANS
	}

	/**
	 * 观察者
	 */
	public static interface Observer {
		/**
		 * 加速时长发生改变
		 * 
		 * @param seconds
		 */
		public void onAccelTimeChanged(int seconds);

		/**
		 * 游戏列表发生改变
		 */
		public void onGameListUpdate();

		/**
		 * 并联加速时长发生改变
		 * 
		 * @param packageName
		 *            包名
		 * @param seconds
		 *            秒
		 */
		void onDoubleAccelTimeChanged(String packageName, int seconds);
	}

	private static class GameList implements Iterable<GameInfo> {
		private HashMap<String, GameInfo> name2info = new HashMap<String, GameInfo>(16);
		private final SparseArray<GameInfo> uid2info = new SparseArray<GameInfo>(16);

		@Override
		public Iterator<GameInfo> iterator() {
			return name2info.values().iterator();
		}

		/**
		 * 根据“当前已安装的游戏列表”更新
		 * 
		 * @param installedGameList
		 *            当前已安装的（支持的）游戏列表
		 */
		public void update(List<GameInfo> installedGameList) {
			HashMap<String, GameInfo> old = this.name2info;
			this.name2info = new HashMap<String, GameInfo>(installedGameList.size());
			// 遍历已安装列表
			// （不在【安装并支持列表】中的，自然就丢弃了）
			for (GameInfo g : installedGameList) {
				GameInfo o = old.get(g.getPackageName());
				if (o != null) {
					// 以前有。沿用以前的扩展数据，更新现有的基础数据
					o.update(g);
					this.name2info.put(o.getPackageName(), o);
					if (Logger.isLoggableDebug(TAG)) {
						Logger.d(TAG, String.format("old add:%s uid:%d start:%d", o.getAppLabel(), o.getUid(), o.getStartCount()));
					}
				} else {
					this.name2info.put(g.getPackageName(), g);
					if (Logger.isLoggableDebug(TAG)) {
						Logger.d(TAG, String.format("new add:%s uid:%d start:%d", g.getAppLabel(), g.getUid(), g.getStartCount()));
					}
				}
			}
			// 更新 uid2info
			this.uid2info.clear();
			for (GameInfo info : this.name2info.values()) {
				this.uid2info.put(info.getUid(), info);
			}
		}

		/** 移除指定包名的GameInfo */
		public void remove(String packageName) {
			GameInfo removed = name2info.remove(packageName);
			if (removed != null) {
				this.uid2info.remove(removed.getUid());
			}
		}

		/** 根据包名取GameInfo */
		public GameInfo getByPackageName(String packageName) {
			return name2info.get(packageName);
		}

		/** 根据UID取GameInfo */
		public GameInfo getByUID(int uid) {
			return uid2info.get(uid);
		}

		/** 添加一个GameInfo */
		public void put(GameInfo info) {
			GameInfo old = name2info.put(info.getPackageName(), info);
			if (old != null) {
				uid2info.remove(old.getUid());
			}
			uid2info.put(info.getUid(), info);
		}

		public int size() {
			return name2info.size();
		}

		public void clear() {
			this.name2info.clear();
			this.uid2info.clear();
		}

		public Collection<GameInfo> infoList() {
			return this.name2info.values();
		}

	}

	private static final GameManager instance = new GameManager();
	private final GameList games = new GameList();

	/** <b>注意</b>，由于有跨线程操作installedApp成员，所以涉及到该成员的操作，都必须针对GameManager实例加锁 */
	private HashMap<String, InstalledAppInfo> installedApp;

	private final FirstSegmentNetDelayContainer firstSegmentNetDelay = new FirstSegmentNetDelayContainer();
	private final SecondSegmentNetDelayContainer secondSegmentNetDelay = new SecondSegmentNetDelayContainer();

	private int accelTimeAmount;				// 所有游戏累计加速时长
	private int gameForegroundTimeAmount;		// 所有游戏在前台的时长

	private static class GameLauncher {
		private int lastUID;
		private long lastTime;

		/**
		 * 启动游戏
		 * 
		 * @param context
		 * @param game
		 * @return true表示成功启动，false表示失败（失败会有TOAST提示）
		 */
		public boolean execute(Context context, GameInfo game) {
			if (ProcessLauncher.execute(context, game.getPackageName())) {
				lastUID = game.getUid();
				lastTime = SystemClock.elapsedRealtime();
				return true;
			} else {
				UIUtils.showToast("启动失败，请尝试手动启动游戏");
				return false;
			}
		}

		public void reset() {
			this.lastUID = 0;
			this.lastTime = 0;
		}
	}

	private final GameLauncher gameLauncher = new GameLauncher();

	/**
	 * 启动游戏
	 * 
	 * @param context
	 * @param game
	 * @return true表示成功启动，false表示失败（失败会有TOAST提示）
	 */
	public boolean launchGame(Context context, GameInfo game) {
		return gameLauncher.execute(context, game);
	}

	/**
	 * 判断给定游戏是否从APP里启动
	 */
	public boolean isGameLaunchFromMe(int uid) {
		return uid == gameLauncher.lastUID && (SystemClock.elapsedRealtime() - gameLauncher.lastTime) <= 60 * 1000;
	}

	/**
	 * 取当前的“第一段”网络延迟
	 * 
	 *
	 *  是否根据加速开关与否，做些调整？
	 * @return 当前网络延迟，单位毫秒
	 */
	public int getFirstSegmentNetDelay() {
		return firstSegmentNetDelay.getAdjusted();
	}

	/**
	 * 取“第二段”（加速节点到游戏服务器）的时延
	 * 
	 * @return 加速节点到游戏服务器的时延（保证不返回null）
	 */
	public SecondSegmentNetDelay getSecondSegmentNetDelay(int uid) {
		return secondSegmentNetDelay.get(uid);
	}

	public static GameManager getInstance() {
		if (GlobalDefines.CHECK_MAIN_THREAD) {
			if (!ThreadUtils.isInAndroidUIThread()) {
				MainHandler.getInstance().showDebugMessage("GameManager.getInstance() called by non-main thread");
			}
		}
		return instance;
	}

	private GameManager() {
		TriggerManager.getInstance().addObserver(0, new EventObserver() {

			@Override
			public void onNetChange(NetTypeDetector.NetType state) {
				switch (state) {
				case DISCONNECT:
					firstSegmentNetDelay.reset(GlobalDefines.NET_DELAY_TEST_FAILED);
					break;
				default:
					resetFirstSegmentNetDelay();
					break;
				}
			}

			@Override
			public void onReconnectResult(ReconnectResult result) {
				if (result.success) {
					onConnectionRepairSucceed(result.uid);
				}
			}

			@Override
			public void onAccelSwitchChanged(boolean state) {
				if (!state) {
					lastRunninGameInfo = null;
					secondSegmentNetDelay.clear();
					gameLauncher.reset();
				}
			}

			@Override
			public void onTopTaskChange(GameInfo info) {
				if (!AccelOpenManager.isStarted()) {
					return;
				}
				if (info != null) {
					lastRunninGameInfo = info;
					firstSegmentNetDelay.onGameForeground(now());
				}
			}

		});
	}

	public synchronized void onAppInstalled(InstalledAppInfo info) {
		// 注意，由于有跨线程操作installedApp成员，所以涉及到该成员的操作，都必须针对GameManager实例加锁

		//新安装应用，放到安装列表中
		String packageName = info.getPackageName();
		if (installedApp != null) {
			installedApp.put(packageName, info);
		}
		AccelGame accelGame = AccelGameList.getInstance().findAccelGame(info);
		if (Logger.isLoggableDebug(TAG)) {
			Logger.d(TAG, String.format("Check: %s, %s, %b", info.getPackageName(), info.getAppLabel(), info.hasSuBaoSDKPermission()));
		}
		if (accelGame == null) {
			Logger.d(TAG, "Not found in AccelGameList, remove it");
			games.remove(packageName);
			return;
		}
		GameInfo g = games.getByPackageName(packageName);
		if (g != null) {
			//是之前就支持的游戏（这种情况下是升级，先删后安装）
			g.setUid(info.getUid());
			g.setInstalled(true);
			g.setSDKEmbed(info.hasSuBaoSDKPermission());
			if (Logger.isLoggableDebug(TAG)) {
				Logger.d(TAG, String.format("re add:%s uid:%d", info.getPackageName(), info.getUid()));
			}
		} else {
			//是新支持的游戏
			g = new GameInfo(info.getUid(), packageName, info.getAppLabel(), accelGame, info.hasSuBaoSDKPermission());
			g.setAppIcon(info.getAppIcon(AppMain.getContext()));
			games.put(g);
			if (Logger.isLoggableDebug(TAG)) {
				Logger.d(TAG, String.format("new add:%s uid:%d", info.getPackageName(), info.getUid()));
			}
		}

		// 通知代理层
		SupportGame supportGame = gameInfoToSupportGame(g);
		if (supportGame != null) {
			VpnSupportGame vpnSupportGame = new VpnSupportGame(supportGame.uid,
					supportGame.packageName,supportGame.appLabel,
					supportGame.protocol.ordinal(),
					supportGame.isForeign,
					portRangeListToString(supportGame.whitePorts),
					portRangeListToString(supportGame.blackPorts),
					ipList2String(supportGame.blackIps),
					ipList2String(supportGame.whiteIps));
			VPNUtils.sendPutSupportGame(vpnSupportGame, TAG);
			/*
			 * VPNManager.getInstance().sendPutSupportGame(supportGame); if
			 * (Logger.isLoggableDebug(TAG)) { Logger.d(TAG,
			 * "New Support Game: " + supportGame); }
			 */
		}

		notifyOnGameListUpdateListener();
	}

	private void notifyOnGameListUpdateListener() {
		List<Observer> observers = cloneAllObservers();
		if (observers != null) {
			for (Observer o : observers) {
				o.onGameListUpdate();
			}
		}
	}

	/**
	 * 判断给定的应用是不是支持的游戏
	 */
	public static boolean isSupportGame(InstalledAppInfo info) {
		if (info == null) {
			return false;
		}
		return null != AccelGameList.getInstance().findAccelGame(info);
	}

	public synchronized void onAppRemoved(String packageName) {
		// 注意，由于有跨线程操作installedApp成员，所以涉及到该成员的操作，都必须针对GameManager实例加锁
		if (installedApp != null) {
			installedApp.remove(packageName);
		}
		//有可能更新的情况，是删除了马上安装，所以不直接删，避免掉数据
		GameInfo g = games.getByPackageName(packageName);
		if (g != null) {
			g.setInstalled(false);
		}
		notifyOnGameListUpdateListener();
	}

	public boolean updateSupportGames() {
		Logger.d(TAG, "Before update support games: " + this.games.size());
		//
		//获取支持的游戏列表
		AccelGameList manager = AccelGameList.getInstance();
		if (manager == null || manager.getCount() <= 0) {
			Logger.e(TAG, "updateSupportGames error, manager.length <= 0");
			return false;
		}
		//
		//获取已安装，并支持的游戏，放到curGames列表中
		InstalledAppInfo[] instList = this.getInstalledApps();
		Logger.d(TAG, "Installed app count: " + instList.length);
		List<GameInfo> curGames = new ArrayList<GameInfo>();

		for (InstalledAppInfo inst : instList) {
			AccelGame accelGame = manager.findAccelGame(inst);
			if (accelGame == null) {
				continue;
			}
			GameInfo g = new GameInfo(inst.getUid(), inst.getPackageName(), inst.getAppLabel(), accelGame,
				inst.hasSuBaoSDKPermission());
			g.setAppIcon(inst.getAppIcon(AppMain.getContext()));
			curGames.add(g);
		}

		//跟旧数据合并
		this.games.update(curGames);

		putGamesToProxyJNI();

		TriggerManager.getInstance().raiseSupportedGameUpdate();
		if (Logger.isLoggableDebug(TAG)) {
			Logger.d(TAG, "After update support games: " + this.games.size());
		}
		return true;
	}

	/**
	 * 将支持的游戏列表相关信息，告之底层
	 */
	private void putGamesToProxyJNI() {
		//List<VPNManager.SupportGame> supportGames = new ArrayList<VPNManager.SupportGame>(64);
		List<VpnSupportGame> supportGames = new ArrayList<VpnSupportGame>(64);
		for (GameInfo info : this.games) {
			SupportGame sg = gameInfoToSupportGame(info);
			if (sg != null) {
				//supportGames.add(sg);
				VpnSupportGame vpnSupportGame = new VpnSupportGame(sg.uid,
						sg.packageName,sg.appLabel,sg.protocol.ordinal(), sg.isForeign,
						portRangeListToString(sg.whitePorts), portRangeListToString(sg.blackPorts),
						ipList2String(sg.blackIps),
						ipList2String(sg.whiteIps));
				supportGames.add(vpnSupportGame);
				if (Logger.isLoggableDebug(TAG)) {
					Logger.d(TAG, String.format("#%d Supported Game: %s",
							supportGames.size(), sg));
				}
			}
		}
		if (!supportGames.isEmpty()) {
			VPNUtils.sendPutSupportGames(supportGames, TAG);
			//VPNManager.getInstance().sendPutSupportGames(supportGames);
		}
	}

	private static SupportGame gameInfoToSupportGame(GameInfo info) {
		if (info.isAccelFake()) {
			return null;
		}
		return new SupportGame(
			info.getUid(), info.getPackageName(), info.getAppLabel(), info.getProtocolType(),
			info.isForeignGame(),
			info.getWhitePorts(),
			info.getBlackPorts(),
			info.getBlackIps(),
			info.getWhiteIps());
	}


	private static String ipList2String(Iterable<String> blackIps) {
		if (blackIps == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder(64);
		for (String ip : blackIps) {
			sb.append(ip).append(',');
		}

		return sb.toString();
	}

	private static String portRangeListToString(Iterable<AccelGame.PortRange> list) {
		if (list == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(1024);
		for (AccelGame.PortRange pr : list) {
			sb.append(pr.start).append('~').append(pr.end).append(',');
		}
		return sb.toString();
	}

	/**
	 * 程序初始化时，用本地文件初始化GameManager
	 * 
	 * @param context
	 * @return true表示成功，false表示失败
	 */
	public synchronized boolean initFromLocal(Context context) {
		// 注意，由于有跨线程操作installedApp成员，所以涉及到该成员的操作，都必须针对GameManager实例加锁
		this.installedApp = InstalledAppInfo.getInstalledApp(context);

		//读保存的游戏信息
		if (!this.loadFromFile()) {
			Log.e(TAG, "load error, infos = null");
			return false;
		}

		MainHandler.getInstance().postDelayed(saveRunnable, SAVE_DELAY);
		return true;
	}

	private final Runnable saveRunnable = new Runnable() {
		@Override
		public void run() {
			save(true);
			MainHandler.getInstance().postDelayed(this, SAVE_DELAY);
		}
	};

	//	public GameInfo[] cloneGameInfoList() {
	//		int size = games.size();
	//		GameInfo[] arrays = new GameInfo[size];
	//		games.values().toArray(arrays);
	//		return arrays;
	//	}

	//	/**
	//	 * 获得加过速的游戏
	//	 * 
	//	 * @return
	//	 */
	//	public List<GameInfo> getSupportedAndHasAccelGames() {
	//		List<GameInfo> gameList = new ArrayList<GameInfo>();
	//		for (GameInfo gameInfo : games.values()) {
	//			if (gameInfo.isInstalled() && gameInfo.getAccumulateAccelTimeSecond() > 0) {
	//				gameList.add(gameInfo);
	//			}
	//		}
	//		return gameList;
	//	}

	/**
	 * 返回当前已安装、且支持加速的游戏列表
	 * 
	 * @return 不为null的{@link GameInfo}列表
	 */
	public List<GameInfo> getSupportedAndReallyInstalledGames() {
		List<GameInfo> gameList = new ArrayList<GameInfo>();
		for (GameInfo gameInfo : games) {
			if (gameInfo.isInstalled()) {
				gameList.add(gameInfo);
			}
		}
		return gameList;
	}

	/**
	 * 判断已安装游戏里是否至少有一个是内嵌了SDK的
	 */
	public boolean hasSDKEmbedGameInstalled() {
		for (GameInfo gameInfo : games) {
			if (gameInfo.isInstalled() && gameInfo.isSDKEmbed()) {
				return true;
			}
		}
		return false;
	}

	public GameInfo getGameInfo(String packageName) {
		// 为了上层逻辑简化，这里参数packageName允许为null
		// 上层将不再检查参数是否为null了，所以这里不要更改
		return packageName == null ? null : this.games.getByPackageName(packageName);
	}

	//	/**
	//	 * 根据包名查找是否是支持的游戏（<b>排除那些嵌入了速宝SDK的游戏</b>）
	//	 * 
	//	 * @param packageName
	//	 *            包名
	//	 * @return 如果是支持的游戏，返回{@link GameInfo}，如果不是则返回null。<br />
	//	 *         <b>注意：本函数会排除那些嵌入了速宝SDK的游戏</b>
	//	 */
	//	public GameInfo getGameInfoExcludeSDKEmbedded(String packageName) {
	//		GameInfo gi = getGameInfo(packageName);
	//		if (gi != null && !gi.isSDKEmbed()) {
	//			return gi;
	//		}
	//		return null;
	//	}

	public GameInfo getInstalledGameInfo(String packageName) {
		// 为了上层逻辑简化，这里参数packageName允许为null
		// 上层将不再检查参数是否为null了，所以这里不要更改
		if (packageName == null) {
			return null;
		}
		GameInfo gameInfo = this.games.getByPackageName(packageName);
		if (gameInfo == null) {
			return null;
		}
		if (!gameInfo.isInstalled()) {
			return UNINSTALLED_GAMEINFO;
		}
		return gameInfo;
	}

	@Override
	public GameInfo getGameInfoByUID(int uid) {
		return games.getByUID(uid);
	}

	/**
	 * 取“已安装应用列表”的一个副本。线程安全
	 * 
	 * @return “已安装应用列表”的一个副本
	 */
	public synchronized InstalledAppInfo[] getInstalledApps() {
		if (installedApp == null || installedApp.isEmpty()) {
			return new InstalledAppInfo[0];
		}
		int size = installedApp.size();
		InstalledAppInfo[] arrays = new InstalledAppInfo[size];
		return installedApp.values().toArray(arrays);
	}

	//--------------序列化部分---------------

	/**
	 * 从文件加载
	 * 
	 * @return true成功，false=失败
	 */
	private boolean loadFromFile() {
		// 先从上一次存储里加载
		// 如果上一次存储没有，说明是第一次安装启动
		File file = FileUtils.getDataFile(GAME_DATA_FILE);
		byte[] data = FileUtils.read(file);
		if (data == null) {
			Log.w(TAG, "load game data file error, maybe first load?");
			return this.updateSupportGames();
		}

		Proto.GameManager proto;
		try {
			proto = Proto.GameManager.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return false;
		}

		//注意，读出来的只有基础数据，结合updateSupportGames生成完整数据
		this.games.clear();
		this.accelTimeAmount = 0;
		boolean hasAccelTimeAmount = proto.hasAccelTimeAmount();
		for (int i = 0; i < proto.getInfosCount(); i++) {
			GameInfo info = new GameInfo(proto.getInfos(i));
			this.games.put(info);
			//兼容以前的版本，如果累计加速时长没有存盘过，就需要从每个游戏的信息里面取出来
			if (!hasAccelTimeAmount) {
				this.accelTimeAmount += info.getAccumulateAccelTimeSecond();
			}
		}

		//如果累计加速时长已经存盘过，直接取出来即可
		if (hasAccelTimeAmount) {
			this.accelTimeAmount = proto.getAccelTimeAmount();
		}

		// 累计游戏在前台时长
		this.gameForegroundTimeAmount = proto.hasGameForegroundTimeAmount() ? proto.getGameForegroundTimeAmount() : this.accelTimeAmount;

		return this.updateSupportGames();
	}

	private static class SaveExecutor extends Thread {

		private static SaveExecutor currentInstance;

		/**
		 * 执行一个同步或异步的存储操作
		 * <p>
		 * <b>本函数只能在主线程中调用！！</b>
		 * </p>
		 */
		public static void execute(GameManager gm, boolean async) {
			if (Logger.isLoggableDebug(TAG)) {
				Logger.d(TAG, "SaveExecutor.execute, async = " + async);
			}
			SaveExecutor exists = currentInstance;
			if (exists == null) {
				// 当前没有存储线程在运行
				if (async) {
					// 异步方式
					currentInstance = new SaveExecutor(gm);
					currentInstance.start();
				} else {
					// 同步方式，直接存储
					SaveExecutor.saveGameInfoList(gm.games, gm.accelTimeAmount, gm.gameForegroundTimeAmount);
				}
			} else {
				// 有线程在运行，等待它完成
				try {
					exists.join(5000);
				} catch (InterruptedException e) {}
			}
		}

		private final List<GameInfo> infoList;
		private final int accelTimeAmount, gameForegroundTimeAmount;

		private SaveExecutor(GameManager gm) {
			this.infoList = new ArrayList<GameInfo>(gm.games.infoList());	// 生成一个副本，避免多线程冲突
			this.accelTimeAmount = gm.accelTimeAmount;
			this.gameForegroundTimeAmount = gm.gameForegroundTimeAmount;
		}

		@Override
		public void run() {
			try {
				saveGameInfoList(this.infoList, this.accelTimeAmount, this.gameForegroundTimeAmount);
			} finally {
				currentInstance = null;
			}
		}

		private static void saveGameInfoList(Iterable<GameInfo> infoList, int accelTimeAmount, int gameForegroundTimeAmount) {
			Proto.GameManager.Builder b = Proto.GameManager.newBuilder();
			for (GameInfo info : infoList) {
				b.addInfos(info.serial());
			}
			b.setAccelTimeAmount(accelTimeAmount);
			b.setGameForegroundTimeAmount(gameForegroundTimeAmount);
			byte[] data = b.build().toByteArray();

			File file = FileUtils.getDataFile(GAME_DATA_FILE);
			FileUtils.write(file, data);
			Logger.d(TAG, "Game data saved");
		}
	}

	public void save(boolean async) {
		SaveExecutor.execute(this, async);
	}

	public void incShortenTime(int uid, int connTime) {
		GameInfo gi = this.getGameInfoByUID(uid);
		if (gi == null)
			return;

		int connTimeWithVpnOff;
		switch (NetManager.getInstance().getCurrentNetworkType()) {
		case MOBILE_2G:
		case MOBILE_3G:
		case UNKNOWN:
			connTimeWithVpnOff = 1200 + (int) (Math.random() * 600);
			break;
		case MOBILE_4G:
		case WIFI:
			connTimeWithVpnOff = 200 + (int) (Math.random() * 400);
			break;
		default:
			connTimeWithVpnOff = 0;
		}

		int shortenTime = Math.max(0, connTimeWithVpnOff - connTime);
		gi.setShortenTimeMillisecond(shortenTime, connTimeWithVpnOff);
	}

	// 上一次触发OnAccelTimeChanged事件时，时长总计是多少？
	private int lastAccelTimeAmountWhenChangedEventRaised = -1000;

	/**
	 * 根据包名，增加指定游戏的“游戏时长”或“加速时长”
	 * 
	 * @param packageName
	 *            包名
	 * @param sec
	 *            加速时长（秒）
	 * @param accelOpened
	 *            当前是否开着加速
	 */
	public void incGameTimeSeconds(String packageName, int sec, boolean accelOpened) {
		GameInfo gi = this.getGameInfo(packageName);
		if (gi == null) {
			return;
		}
		// 游戏时长递增
		gameForegroundTimeAmount += sec;
		// 如果加速开启，则还需要
		if (accelOpened) {
			gi.incAccelTimeSecond(sec);
			accelTimeAmount += sec;
			if (accelTimeAmount - lastAccelTimeAmountWhenChangedEventRaised >= 60) {
				lastAccelTimeAmountWhenChangedEventRaised = accelTimeAmount;
				List<Observer> observers = cloneAllObservers();
				if (observers != null) {
					for (Observer o : observers) {
						o.onAccelTimeChanged(this.accelTimeAmount);
					}
				}
			}
			notifyDoubleAccelTime(packageName, sec);
		}
	}

	private void notifyDoubleAccelTime(String packageName, int sec) {
		if (NetManager.getInstance().isWiFiConnected() && ConfigManager.getInstance().isEnableDoubleAccel()) {
			List<Observer> observers = cloneAllObservers();
			if (observers != null) {
				for (Observer o : observers) {
					o.onDoubleAccelTimeChanged(packageName, sec);
				}
			}
		}
	}

	/**
	 * 取所有游戏的累计加速时长
	 * 
	 * @return 所有游戏的累计加速时长
	 */
	public int getAccelTimeSecondsAmount() {
		return accelTimeAmount;
	}

	//测试专用
	public void setAccelTimeSecondsAmount(int accelTimeAmount) {
		this.accelTimeAmount = accelTimeAmount;
	}

	public int getGameForegroundTimeAmount() {
		return this.gameForegroundTimeAmount;
	}

	/**
	 * 获得游戏的减少等待时长
	 * 
	 * @return 默认为0
	 */
	public int getTotalShortenWaitTimeMilliseconds() {
		int waitTimeMilliseconds = 0;
		for (GameInfo gameInfo : games) {
			if (gameInfo.isInstalled()) {
				waitTimeMilliseconds += gameInfo.getAccumulateShortenWaitTimeMilliseconds();
			}
		}
		return waitTimeMilliseconds;
	}

	public int getTotalReconnectCount() {
		int count = 0;
		for (GameInfo gameInfo : games) {
			if (gameInfo.isInstalled()) {
				count += gameInfo.getAccumulateReconnectCount();
			}
		}
		return count;
	}

	public void clearAllAccelTime() {
		for (GameInfo gameInfo : games) {
			gameInfo.clearAccelTimeSecond();
		}
	}

	/**
	 * 当底层产生游戏日志时被调用
	 * 
	 * @param log
	 *            游戏日志的JSON字串
	 */
	public void onGameLog(String log) {
		try {
			JSONObject jsonObj = new JSONObject(log);
			String packageName = jsonObj.getString("name");
			GameInfo gameInfo = this.getGameInfo(packageName);
			if (gameInfo == null) {
				return;
			}
			long sendMobile = jsonObj.getLong("ms");
			long recvMobile = jsonObj.getLong("mr");
			long mobile = sendMobile + recvMobile;
			if (mobile > 0) {
				//				gameInfo.increaseFlow(mobile);
				AccelDetailsObserver.getInstance().onFlowProduce(gameInfo, mobile);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 当底层通知断线重连成功时被调用
	 * 
	 * @param uid
	 *            游戏的UID
	 */
	private void onConnectionRepairSucceed(int uid) {
		GameInfo gameInfo = getGameInfoByUID(uid);
		if (gameInfo != null) {
			gameInfo.incReconnctTimes();
		}
	}



	/**
	 * 返回最近的、至多150条延迟记录里，异常值所占百分比
	 * 
	 * @return 0~100的值
	 */
	public int getBadPercentOfFirstSegmentNetDelay() {
		return firstSegmentNetDelay.getBadPercent();
	}

	/**
	 * 第二段（节点到游戏服务器）网络延迟
	 * 
	 * @see FirstSegmentNetDelayContainer
	 */
	private static class SecondSegmentNetDelayContainer {

		private final SparseArray<GameManager.SecondSegmentNetDelay> array = new SparseArray<GameManager.SecondSegmentNetDelay>();

		private final Runnable adjust = new Runnable() {

			private final Random random = new Random();

			@Override
			public void run() {
				GameInfo gameInfo = TaskManager.getInstance().getCurrentForegroundGame();
				if (gameInfo != null) {
					SecondSegmentNetDelay secondDelay = array.get(gameInfo.getUid());
					if (secondDelay != null && secondDelay.rawValue > 0
						&& secondDelay.rawValue < GlobalDefines.NET_DELAY_TIMEOUT) {
						int delta = random.nextInt(7) - 3;
						int value = secondDelay.rawValue + delta;
						if (value <= 0) {
							value = 1;
						}
						if (value != secondDelay.value) {
							secondDelay.value = value;
							TriggerManager.getInstance().raiseSecondSegmentNetDelayChange(gameInfo.getUid(),
								secondDelay);
						}
					}
				}
				MainHandler.getInstance().postDelayed(this, 60 * 1000);
			}
		};

		public SecondSegmentNetDelayContainer() {
			MainHandler.getInstance().postDelayed(adjust, 10 * 1000);
		}

		/**
		 * 第二段延迟值产生
		 * 
		 * @param gameInfo
		 *            哪个游戏
		 * @param value
		 *            值
		 * @param isDirectTrans
		 *            是透传吗？
		 * @param isFake 是人造数据吗？
		 * 	
		 */
		public void set(GameInfo gameInfo, int value, boolean isDirectTrans, boolean isFake) {
			int uid = gameInfo.getUid();
			SecondSegmentNetDelay secondDelay = array.get(uid);
			if (secondDelay == null) {
				secondDelay = new GameManager.SecondSegmentNetDelay(value, isDirectTrans, gameInfo.isForeignGame(), isFake);
				array.put(uid, secondDelay);
			} else {
				secondDelay.set(value, isDirectTrans, gameInfo.isForeignGame(), isFake);
			}
			TriggerManager.getInstance().raiseSecondSegmentNetDelayChange(uid, secondDelay);
		}

		/**
		 * 清除记录

		 *
		 * @see {@link #(int)}
		 */
		public void clear() {
			array.clear();
		}

		/**
		 * 根据UID取SecondSegmentNetDelay，保证不返回null
		 */
		public GameManager.SecondSegmentNetDelay get(int uid) {
			GameManager.SecondSegmentNetDelay result = array.get(uid);
			if (result == null) {
				return GameManager.SecondSegmentNetDelay.NULL;
			} else {
				return result;
			}
		}

	}

	/**
	 * 第二段网络时延值
	 */
	public static final class SecondSegmentNetDelay {
		
		public static final long TIMEOUT_TRUE_DATA = 5 * 60 * 1000L;

		public static final SecondSegmentNetDelay NULL = new SecondSegmentNetDelay(GlobalDefines.NET_DELAY_TEST_WAIT,
			false, false, true);

		/** 原始未加工的值 */
		private int rawValue = GlobalDefines.NET_DELAY_TEST_WAIT;

		/** 定时在rawValue基础上微调的值 */
		private int value = GlobalDefines.NET_DELAY_TEST_WAIT;

		/** 是透传吗？ */
		private boolean isDirectTrans;

		/** 在透传时，将透传时延分成两段，其中第一段参与调整“第一段延迟值” */
		private int first = GlobalDefines.NET_DELAY_TEST_WAIT;
		
		/** 最近一次的更新时刻（仅限真实数据） */
		private long lastUpdateTimeOfTrueData = -TIMEOUT_TRUE_DATA;

		public SecondSegmentNetDelay(int value, boolean isDirectTrans, boolean isForeign, boolean isFake) {
			set(value, isDirectTrans, isForeign, isFake);
		}

		/**
		 * 设置延迟值
		 * 
		 * @param value
		 *            延迟值
		 * @param isDirectTrans
		 *            是透传吗？
		 * @param isForeign
		 *            是海外服吗？
		 * @param isFake
		 *            是人造数据吗？
		 */
		public void set(int value, boolean isDirectTrans, boolean isForeign, boolean isFake) {
			if (value == 0) {
				value = 1;
			}
			this.isDirectTrans = isDirectTrans;
			if (isDirectTrans) {
				if (isDelayValueException(value)) {
					first = GlobalDefines.NET_DELAY_TEST_WAIT;
					value = GlobalDefines.NET_DELAY_TEST_WAIT; // v2.0.1 新需求，透传的时候如遇异常，令2段值为-2（界面上显示为白色---）
				} else {
					// 分成两部分
					int r = isForeign ? 4 : 3;
					int second = Math.max(value * r / 10, 1);
					first = Math.max(value - second, 1);
					value = second;
					if (!isForeign && value >= 50) {
						// v2.0.1 新需求，如果是国服，不要让第2段值超过50
						value = (int) (40 + (System.currentTimeMillis() % 10));
					}
				}
			}
			this.rawValue = this.value = value;
			if (!isFake) {
				this.lastUpdateTimeOfTrueData = now();
			}
		}
		
		/**
		 * 返回自最近一次更新以来过去的时间，单位毫秒
		 * @return 自最近一次更新以来过去的时间，单位毫秒
		 */
		public long getElapsedTimeSinceLastUpdate() {
			return now() - this.lastUpdateTimeOfTrueData;
		}

		/** 返回延迟值 */
		public int getDelayValue() {
			return this.value;
		}

		/** 是透传吗？ */
		public boolean isDirectTrans() {
			return this.isDirectTrans;
		}

		public int adjustFirstSegmentDelayWhenDirectTrans(int rawFirstSegmentDelay) {
			if (!isDirectTrans) {
				return rawFirstSegmentDelay;
			}
			if (isDelayValueException(rawFirstSegmentDelay)) {
				return rawFirstSegmentDelay;
			}
			if (isDelayValueException(first)) {
				return rawFirstSegmentDelay;
			}
			return Math.max(1, (first + rawFirstSegmentDelay) >> 1);
		}
	}

	/**
	 * 底层通知：网络延迟改变
	 * 
	 * @param delayValue
	 *            经过加工的延迟值（当网络未断但UDP测速异常时，用TCP测速值替代）
	 * @param rawUDPValue
	 *            原始的未加工过的UDP测速值
	 */
	public int onFirstSegmentNetDelayChange(int delayValue, int rawUDPValue, NetTypeDetector netTypeDetector, GameForegroundDetector gfd, boolean isWiFiAccelActivated) {
		// 如果在测试模式里，始终用测试设定的值
		if (testValue_FirstSegmentDelay != null) {
			delayValue = rawUDPValue = testValue_FirstSegmentDelay;
		}
		if (delayValue == 0) {
			delayValue = 1;
		}
		if (rawUDPValue == 0) {
			rawUDPValue = 1;
		}
		//
		// 如果延迟值是正常的，要看看是不是透传，是的话得和透传值按一定算法做平均
		delayValue = adjustFirstDelayIfDirectTrans(delayValue, gfd);
		//
		// 将真实的延迟值记录到历史记录中，并标记其中质量较差的
		NetType currentNetworkType = netTypeDetector.getCurrentNetworkType();
		firstSegmentNetDelay.offerDelayValue(rawUDPValue, delayValue, currentNetworkType, now());
		int result = firstSegmentNetDelay.getAdjusted();
		if (!isWiFiAccelActivated || isDelayValueException(result)) {
			return result;
		}
		//
		// 在WiFi加速状态中，修饰延迟值
		int avg = firstSegmentNetDelay.getAverageOfGoodValue();
		if (avg > 0) {
			if (!FirstSegmentNetDelayContainer.isBadDelay(result, currentNetworkType)) {
				avg = (avg + result) / 2; 
			}
			result = avg;
		} else {
			result = result * 9 / 10;
		}
		if (result <= 0) {
			result = 1;
		}
		firstSegmentNetDelay.adjustWithWiFiAccel(result);
		return result;
	}

	private int adjustFirstDelayIfDirectTrans(int value, GameForegroundDetector gfd) {
		if (isDelayValueException(value)) {
			return value;
		}
		GameInfo currentForegroundGame = gfd.getCurrentForegroundGame();
		if (currentForegroundGame == null) {
			return value;
		}
		int uid = currentForegroundGame.getUid();
		SecondSegmentNetDelay nd = secondSegmentNetDelay.get(uid);
		if (nd == null) {
			return value;
		}
		return nd.adjustFirstSegmentDelayWhenDirectTrans(value);
	}


	public synchronized void onMediaMounted() {
		// 注意，由于有跨线程操作installedApp成员，所以涉及到该成员的操作，都必须针对GameManager实例加锁

		this.installedApp = InstalledAppInfo.getInstalledApp(AppMain.getContext());
		this.updateSupportGames();
	}

	private GameInfo lastRunninGameInfo;

	public GameInfo getLastRunningGameInfo() {
		return this.lastRunninGameInfo;
	}

	public boolean lastRunningGameInfoIsOverseas() {
		if (this.lastRunninGameInfo != null) {
			return this.lastRunninGameInfo.isForeignGame();
		}
		return false;
	}

	/**
	 * 底层通知：第二段（节点到游戏服务器）的时延发生改变
	 */
	public void onSecondSegmentNetDelayChange(int uid, int delay, boolean isFake) {
		this.onSecondSegmentNetDelayChange(uid, delay, isFake, this);
	}

	// 本方法还会被单元测试使用
	void onSecondSegmentNetDelayChange(int uid, int delay, boolean isFake, GameInfoContainer gic) {
		GameInfo gameInfo = gic.getGameInfoByUID(uid);
		if (gameInfo != null) {
			this.secondSegmentNetDelay.set(gameInfo, delay, false, isFake);
		}
	}

	/**
	 * 底层通知：发生透传了
	 */
	void onDirectTrans(int uid, int port, int delay, GameInfoContainer gic) {
		// 是不是80或443端口啊？是的话，直接忽略了
		if (port == 80 || port == 443) {
			return;
		}
		// 判断一下UID，是不是游戏，不是游戏就直接忽略
		GameInfo gameInfo = gic.getGameInfoByUID(uid);
		if (gameInfo == null) {
			return;
		}
		// 是游戏的透传
		this.secondSegmentNetDelay.set(gameInfo, delay, true, false);
	}

	/**
	 * @see {@link #onDirectTrans(int, int, int, GameInfoContainer)}
	 */
	public void onDirectTrans(int uid, int port, int delay) {
		this.onDirectTrans(uid, port, delay, this);
	}
	
	public long getElapsedTimeSinceLastSecondDelayUpdate(int uid) {
		SecondSegmentNetDelay sd = this.secondSegmentNetDelay.get(uid);
		if (sd == null) {
			return 10 * 60 * 1000L;
		}
		return sd.getElapsedTimeSinceLastUpdate();
	}

	/**
	 * 判断给定的延迟值是否异常？
	 * 
	 * @param milliseconds
	 *            给定的延迟值，单位毫秒
	 * @return true，表示给定的延迟值是一个表示异常状态的值
	 */
	private static boolean isDelayValueException(int milliseconds) {
		return milliseconds < 0 || milliseconds >= GlobalDefines.NET_DELAY_TIMEOUT;
	}

	static long now() {
		return SystemClock.elapsedRealtime();
	}

	/**
	 * 这是一个给测试模块使用的接口：随机返回一个GameInfo
	 */
	public GameInfo getRandomGame() {
		// 简化：返回第一个
		for (GameInfo info : this.games) {
			return info;
		}
		return null;
	}

	/**
	 * 重置第一段延迟值为初始状态
	 * <p>
	 * （本函数主要被单元测试模块使用）
	 * </p>
	 */
	public void resetFirstSegmentNetDelay() {
		this.firstSegmentNetDelay.reset(GlobalDefines.NET_DELAY_TEST_WAIT);
	}

	////// 以下是测试部所用的专用函数 ///////////////////

	private Integer testValue_FirstSegmentDelay;

	/**
	 * 给测试部用的函数：将“第一段延迟值”锁定为给定值
	 */
	public void setTestValue_FirstSegmentDelay(Integer value) {
		testValue_FirstSegmentDelay = value;
	}

	/**
	 * 取测试人员设置的“第一段延迟值”
	 * <p>
	 * <b>注意检查返回值是否为null</b>
	 * </p>
	 */
	public Integer getTestValue_FirstSegmentDelay() {
		return testValue_FirstSegmentDelay;
	}

	/**
	 * 测试人员触发一个透传事件
	 */
	public void setTestValue_DirectTrans(int uid, int port, int delay) {
		this.onDirectTrans(uid, port, delay);
	}

}
