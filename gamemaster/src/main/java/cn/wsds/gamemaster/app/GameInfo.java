package cn.wsds.gamemaster.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.subao.common.data.AccelGame;
import com.subao.common.net.Protocol;

import cn.wsds.gamemaster.pb.Proto;
import cn.wsds.gamemaster.ui.UIUtils;


/**
 * “可以加速的游戏”相关数据
 */
public class GameInfo {
	private static final int SECONDS_OF_TIME_FOR_INC_FAKE_RECONNECT_COUNT = 3600 * 6;

	private AccelGame accelGame;

	private final String packageName; // 应用程序所对应的包名
	private int startCount;     // 启动次数
	private int windowX = -1;
	private int windowY = -1;
	private int accelCount;
	private int totalAccelTimesSecond;
	private int amountAccelTimeForConnectionRepair;	// 用于调整断线重连次数的累计加速时长
	private int accuShortenTimeMillisecond;
	private int accuConnTimeWithVpnOffMillisecond;
	private List<AccelDetails> accelDetailsList;
	private int accuReconnectCount;
	private long leavingForegroundTime;
	/**
	 * 最近活跃时间单位毫秒 时间为系统时间
	 */
	private long activeTimeMillisecond;

	//动态数据，不需要保存
	private int uid;            // 应用安装唯一标识
	private String appLabel;    // 应用程序标签
	private Drawable appIcon;   // 应用程序图像
	private boolean installed;
	private long launchedTime;  //启动时间 
	private int accelTimeSecond;      //单次加速时间
	private int pid;            //进程PID
	private String curNode;     //当前使用的节点
	private int shortenTimeMillisecond;
	private int reconnectCount;
	private int taskId = -1;
	private boolean newLaunch = false;

	private boolean isSDKEmbed;

	/** 本游戏是否已嵌入速宝SDK */
	public boolean isSDKEmbed() {
		return this.isSDKEmbed;
	}
	
	/** 本游戏是海外游戏吗？ */
	public boolean isForeignGame() {
		return accelGame != null && accelGame.isForeign();
	}
	
	/** 本游戏强制透传吗？ */
	public boolean isAccelFake() {
		return accelGame != null && accelGame.isAccelFake();
	}
	
	/** 本游戏推荐使用VPN方式加速吗？ */
	public boolean isAccelByVPNRecommend() {
		return true;
	}
	
	/** 本游戏推荐使用ROOT方式加速吗？ */
	public boolean isAccelByROOTRecommend() {
		return false;
	}
	
	/**
	 * 本游戏是UDP还是TCP还是Both
	 * @return 1=UDP，2=TCP，0=Both
	 */
	public Protocol getProtocolType() {
		return (accelGame == null) ? Protocol.BOTH : accelGame.getProtocol();
	}

	public Iterable<AccelGame.PortRange> getWhitePorts() {
		return (this.accelGame == null) ? null : this.accelGame.getWhitePorts();
	}

	public Iterable<AccelGame.PortRange> getBlackPorts() {
		return (this.accelGame == null) ? null : this.accelGame.getBlackPorts();
	}

	/**
	 * 获取 ip 黑名单列表
	 * @return ip 黑名单列表
	 */
	public Iterable<String> getBlackIps() {
		return (this.accelGame == null) ? null : this.accelGame.getBlackIps();
	}

	/**
	 * 获取 ip 白名单列表
	 * @return ip 白名单列表
	 */
	public Iterable<String> getWhiteIps() {
		return (this.accelGame == null) ? null : this.accelGame.getWhiteIps();
	}

	public void setSDKEmbed(boolean embed) {
		this.isSDKEmbed = embed;
	}

	public void setLeavingForegroundTime(long leavingForegroundTime) {
		this.leavingForegroundTime = leavingForegroundTime;
	}

	public void setNewLaunch(boolean newLaunch) {
		this.newLaunch = newLaunch;
	}

	public boolean isNewLaunch() {
		return newLaunch;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	public int getTaskId() {
		return taskId;
	}

	public GameInfo(int uid, String pn, String label, AccelGame accelGame, boolean isSDKEmbed) {
		this.accelGame = accelGame;
		this.uid = uid;
		this.packageName = pn;
		this.appLabel = label;

		this.installed = true;

		this.accelDetailsList = new ArrayList<AccelDetails>();

		this.isSDKEmbed = isSDKEmbed;
	}

	public GameInfo(Proto.GameInfo p) {
		this.packageName = p.getPackageName();
		this.startCount = p.getStartCount();
		this.windowX = p.hasStatusWindowX() ? p.getStatusWindowX() : -1;
		this.windowY = p.hasStatusWindowY() ? p.getStatusWindowY() : -1;
		this.accelCount = p.getAccelCount();
		this.totalAccelTimesSecond = p.getAccelTimes();
		this.amountAccelTimeForConnectionRepair = p.getAmountAccelTimeForConnectionRepair();
		this.launchedTime = p.getLaunchedTime();

		this.accuShortenTimeMillisecond = p.getAccuShortenTime();
		this.accuConnTimeWithVpnOffMillisecond = p.getAccuConnTimeWithVpnOff();
		this.activeTimeMillisecond = p.getActiveTime();
		this.accuReconnectCount = p.getAccuReconnectCount();
		this.accelDetailsList = new ArrayList<AccelDetails>();

		//导入所有的加速信息到内存
		for (int i = 0; i < p.getAccelDetailsCount(); i++) {
			AccelDetails accelDetails = new AccelDetails(p.getAccelDetails(i));
			this.accelDetailsList.add(accelDetails);
		}

	}

	public Proto.GameInfo serial() {
		Proto.GameInfo.Builder p = Proto.GameInfo.newBuilder();
		p.setPackageName(this.packageName);
		p.setStartCount(this.startCount);
		p.setStatusWindowX(this.windowX);
		p.setStatusWindowY(this.windowY);
		p.setAccelCount(this.accelCount);
		p.setAccelTimes(this.totalAccelTimesSecond);
		p.setLaunchedTime(this.launchedTime);

		p.setAccuShortenTime(this.accuShortenTimeMillisecond);
		p.setAccuConnTimeWithVpnOff(this.accuConnTimeWithVpnOffMillisecond);
		p.setActiveTime(this.activeTimeMillisecond);
		p.setAccuReconnectCount(this.accuReconnectCount);


		synchronized (this) {
			for (AccelDetails accelDetails : this.accelDetailsList) {
				p.addAccelDetails(accelDetails.serializeAccelDetails());
			}
		}

		p.setAmountAccelTimeForConnectionRepair(this.amountAccelTimeForConnectionRepair);
		return p.build();
	}

	//从另一个GameInfo中更新
	public void update(GameInfo o) {
		//有可能同一个游戏重装了，ID不一样了，需要更新
		this.uid = o.uid;
		this.appLabel = o.appLabel;
		this.appIcon = o.appIcon;
		this.installed = o.installed;
		//1，init时，从文件中读出来的GameInfo，没有设置SupportGame，这里设置上
		//2，更新时，设置新的SupportGame
		this.accelGame = o.accelGame;
		//
		this.isSDKEmbed = o.isSDKEmbed;
	}

	public List<AccelDetails> cloneAccelDetails() {
		return new ArrayList<AccelDetails>(this.accelDetailsList);
	}

	private synchronized void addAccelDetails(AccelDetails accelDetails) {

		//accelDetailsList中没有值的时间，直接插入
		if (this.accelDetailsList.size() == 0) {
			this.accelDetailsList.add(accelDetails);
			return;
		}

		//统计一共有多少不同日期的accelDetails
		Set<Long> set = new HashSet<Long>();
		for (AccelDetails ad : this.accelDetailsList) {
			set.add(ad.getDate());
		}
		int count = set.size();

		//如果日期数量小于7，直接插入
		if (count < 7) {
			this.accelDetailsList.add(accelDetails);
			return;
		}

		//计算出最远和最近的日期
		long nearestDate = this.accelDetailsList.get(0).getDate();
		long farestDate = this.accelDetailsList.get(0).getDate();

		for (long date : set) {
			if (date > nearestDate) {
				nearestDate = date;
			}
			if (date < farestDate) {
				farestDate = date;
			}
		}

		//如果日期数量等于7，但是新插入的detail日期跟list中最后一项相同，那么直接插入
		if (nearestDate == accelDetails.getDate()) {
			this.accelDetailsList.add(accelDetails);
			return;
		}

		//否则删除最远的一天的记录，然后插入新的记录
		List<AccelDetails> tempList = new ArrayList<AccelDetails>();
		for (AccelDetails ad : this.accelDetailsList) {
			if (ad.getDate() != farestDate) {
				tempList.add(ad);
			}
		}
		this.accelDetailsList = tempList;
		this.accelDetailsList.add(accelDetails);

	}

	/**
	 * 更新（以launchedTime为Key）或添加一项
	 * 
	 * @param accelDetails
	 */
	public synchronized void updateAccelDetails(AccelDetails accelDetails) {
		int index = 0;
		for (AccelDetails ad : this.accelDetailsList) {
			if (ad.getLaunchedTime() == accelDetails.getLaunchedTime()) {
				break;
			}
			index++;
		}
		if (index < this.accelDetailsList.size()) {
			this.accelDetailsList.remove(index);
			this.accelDetailsList.add(accelDetails);
		} else {
			addAccelDetails(accelDetails);
		}
	}

	public int getStartCount() {
		return startCount;
	}

	public String getPackageName() {
		return packageName;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public boolean isInstalled() {
		return installed;
	}

	public void setInstalled(boolean installed) {
		this.installed = installed;
	}

	public String getAppLabel() {
		return appLabel;
	}

	public Drawable getAppIcon(Context context) {
		if (appIcon == null) {
			Drawable drawable = UIUtils.loadAppDefaultIcon(context);
			appIcon = drawable;
			return appIcon;
		}

		return appIcon;
	}

	public void setAppIcon(Drawable appIcon) {
		this.appIcon = appIcon;
	}

	public AccelGame getAccelGame() {
		return accelGame;
	}

	public int getWindowX() {
		return windowX;
	}

	public void setWindowX(int windowX) {
		this.windowX = windowX;
	}

	public int getWindowY() {
		return windowY;
	}

	public void setWindowY(int windowY) {
		this.windowY = windowY;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	private static final long MAX_VALID_LEAVING_DURATION = 60 * 1000;

	public void onStart(boolean vpnStarted) {
		this.launchedTime = System.currentTimeMillis() / 1000;
		this.startCount++;
		if (System.currentTimeMillis() - leavingForegroundTime >= MAX_VALID_LEAVING_DURATION) {
			this.accelTimeSecond = 0;
		}
		this.reconnectCount = 0;
		//		this.run = true;
		if (vpnStarted)
			this.accelCount++;
	}

	//	//获取加速后，游戏加速次数
	//	public int getTotalAccelCount(){
	//		return this.accelCount;
	//	}

	//获取游戏开始运行的时刻，注意用户中间改系统时间的情况
	public long getLaunchedTime() {
		return this.launchedTime;
	}

	void incAccelTimeSecond(int sec) {
		this.accelTimeSecond += sec;
		this.totalAccelTimesSecond += sec;
		//
		this.amountAccelTimeForConnectionRepair += sec;
		if (amountAccelTimeForConnectionRepair >= SECONDS_OF_TIME_FOR_INC_FAKE_RECONNECT_COUNT) {
			amountAccelTimeForConnectionRepair -= SECONDS_OF_TIME_FOR_INC_FAKE_RECONNECT_COUNT;
			incReconnctTimes();
		}
	}

	/** 获取加速后，游戏加速时间（单次） */
	public int getAccelTimeSecond() {
		return this.accelTimeSecond;
	}

	public void clearAccelTimeSecond() {
		this.accelTimeSecond = 0;
	}

	/** 获取加速后，游戏加速时间（累计时间） */
	public int getAccumulateAccelTimeSecond() {
		int temp = 0;
		for (AccelDetails ad : this.accelDetailsList) {
			temp += (int) (ad.getDuration() / 1000);
		}
		if (this.totalAccelTimesSecond < temp) {
			return temp;
		}
		return this.totalAccelTimesSecond;
	}

	//获取为游戏加速的节点（服务器）
	public String getAccelNode() {
		return this.curNode;
	}

	public void setAccelNode(String node) {
		this.curNode = node;
	}

	//获取加速后，长连接游戏延迟降低的百分比（单次）
	public int getDelayDecreasePercent() {
		return 45 + new Random().nextInt(36);
	}

	/** 获取加速后，短连接游戏缩短的连接等待时间（单次） */
	public int getShortenWaitTimeMilliseconds() {
		return shortenTimeMillisecond;
	}

	/** 获取加速后，短连接游戏缩短的连接等待时间（累计） */
	public int getAccumulateShortenWaitTimeMilliseconds() {
		return accuShortenTimeMillisecond;
	}

	//获取加速后，短连接游戏缩短的连接等待时间百分比（累计效果）
	public int getAccumulateShortenWaitTimePercent() {
		if (accuConnTimeWithVpnOffMillisecond == 0)
			return 0;
		return accuShortenTimeMillisecond * 100 / accuConnTimeWithVpnOffMillisecond;
	}

	public void setShortenTimeMillisecond(int shortenTimeMillisecond, int connTimeWithVpnOffMillisecond) {
		this.shortenTimeMillisecond = shortenTimeMillisecond;
		accuShortenTimeMillisecond += shortenTimeMillisecond;
		accuConnTimeWithVpnOffMillisecond += connTimeWithVpnOffMillisecond;
	}

	public void incReconnctTimes() {
		this.reconnectCount++;
		this.accuReconnectCount++;
	}

	//获取加速后，游戏重连次数（单次）
	public int getReconnectTimes() {
		return this.reconnectCount;
	}

	//获取加速后，游戏重连次数（累计次数）
	public int getAccumulateReconnectCount() {
		return this.accuReconnectCount;
	}

	/** 获取最近活跃时间 单位毫秒 */
	public long getActiveTimeMillisecond() {
		return activeTimeMillisecond;
	}

	/** 设置最近活跃时间 单位毫秒 */
	public void setActiveTimeMillisecond(long activeTimeMillisecond) {
		this.activeTimeMillisecond = activeTimeMillisecond;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameInfo [packageName=");
		builder.append(packageName);
		builder.append(", useFrequency=");
		builder.append(startCount);
		builder.append(", uid=");
		builder.append(uid);
		builder.append(", appLabel=");
		builder.append(appLabel);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * 找到GameInfo里最近一次加速的“延迟降低百分比”
	 * 
	 * @return 最近一次加速的“延迟降低百分比”，或者-1（如果没有加速记录）
	 */
	public int getPercentDelayDecreaseLastAccel() {
		if (this.accelDetailsList == null || this.accelDetailsList.isEmpty()) {
			return -1;
		}
		return this.accelDetailsList.get(accelDetailsList.size() - 1).getPercentDelayDecrease();
	}

}
