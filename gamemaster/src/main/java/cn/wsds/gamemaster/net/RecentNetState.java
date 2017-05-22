package cn.wsds.gamemaster.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.os.SystemClock;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

/**
 * 最近的网络状态 
 */
public class RecentNetState {

	public static final RecentNetState instance = new RecentNetState();
	/** 近期网络状态记录 */
	private final BoundsStack<Record> records = new BoundsStack<Record>();
	/** records 列表最大长度 */
	private static final int RECORDS_CAPACITY = 15;
	/** 最近的时间范围为 15s */
	private final long RECENT_RANGE_TIME = 15000;
	/**
	 * 网络状态记录
	 */
	private static final class Record{
		public final long timeMillis;//发生时间
		public final NetTypeDetector.NetType nettype;//网络状态
		public final NetTypeDetector.NetType beforeType;
		public Record(NetTypeDetector.NetType currentNetworkType) {
			this(currentNetworkType, NetTypeDetector.NetType.UNKNOWN);
		}
		public Record(NetTypeDetector.NetType nettype,NetTypeDetector.NetType beforeType) {
			this.timeMillis = RecentNetState.getNow();
			this.nettype = nettype;
			this.beforeType = beforeType;
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Record [timeMillis=");
			builder.append(timeMillis);
			builder.append(", nettype=");
			builder.append(nettype);
			builder.append(", beforeType=");
			builder.append(beforeType);
			builder.append("]");
			return builder.toString();
		}
	}

	/**
     * Returns milliseconds since boot, including time spent in sleep.
     * @return elapsed milliseconds since boot.
     */
	private static long getNow() {
		return SystemClock.elapsedRealtime();
	}
	
	/**
	 * 监听网络状态变化
	 */
	private final class NetChangeObserver extends EventObserver{
		@Override
		public void onNetChange(NetTypeDetector.NetType state) {
			// 对栈添加一条网络状态记录 时间自动填充
			Record record = builedRecord(state);
			RecentNetState.instance.records.add(record);
		}
	}
	
	private Record builedRecord(NetTypeDetector.NetType state) {
		Record record;
		if(RecentNetState.instance.records.isEmpty()){
			record = new Record(state);
		}else{
			Record beforeRecord = RecentNetState.instance.records.peek();
			NetTypeDetector.NetType befortype = beforeRecord.nettype;
			record = new Record(state,befortype);
		}
		return record;
	}

	private RecentNetState() {}
	
	/**
	 * 在主线程初始化
	 */
	public void init(){	
		TriggerManager.getInstance().addObserver(new NetChangeObserver());
		records.setCapacity(RECORDS_CAPACITY);
	}
	
	/**
	 * 获得最近的网络状态记录
	 * @return
	 */
	private List<Record> getRecentRecord(){
		long now = getNow();
		List<Record> recentRecords = new ArrayList<Record>();
		/*
		 * 取出 栈中顶层记录 如果该记录发送的时间与当前的时间（当前线程流逝的时间）相差大于有效值
		 * （RECENT_RANGE_TIME）
		 * 则该记录以下记录皆无效应直接break，若有效则继续取出下一个消息，直至取出所有消息，或者大于有效值
		 */
		for (int i=records.size() - 1;i>=0;i--) {
			Record record = records.get(i);
			if (now - record.timeMillis > RECENT_RANGE_TIME)
				break;
			recentRecords.add(record);
		}
		NetManager.getInstance().refreshNetState();
		NetTypeDetector.NetType currentNetworkType = NetManager.getInstance().getCurrentNetworkType();
		Record currentRecord = builedRecord(currentNetworkType);
		recentRecords.add(currentRecord);
		return recentRecords;
	}
	
//	/**
//	 * 获得最近的网络状态记录
//	 * @return
//	 */
//	private List<Record> getRecentRecord(){
//		List<Record> recentRecords = new ArrayList<Record>();
//		
//		Record currentRecord = records.peek();
//		NetManager.getInstance().initNetState(AppMain.getContext());
//		int currentNetworkType = NetManager.getInstance().getCurrentNetworkType(AppMain.getContext());
//		if(currentRecord != null && getNow() - currentRecord.timeMillis <= RECENT_RANGE_TIME){
//			 recentRecords.add(currentRecord);
//		}
//		recentRecords.add(builedRecord(currentNetworkType));
//		return recentRecords;
//	}
	
	/**
	 * 返回最近的网络状态描述
	 * @return
	 */
	public RecentState getRecentState(){
		List<Record> recentRecord = getRecentRecord();
		RecentNetType build = RecentNetType.build(recentRecord);
		RecentState  recentState = build.getRecentNetState();
		return recentState;
	}
	
	/**
	 * 通过最近的网络类型计算所属的类型
	 */
	private static final class RecentNetType{
		boolean mobile = false,wifi = false,disconnect=false;
		/**
		 * 判断最近的网络记录判断最近的 移动数据、wifi、断网的详细情况
		 * @param recentRecord
		 * @return
		 */
		public static RecentNetType build(List<Record> recentRecords){
			RecentNetType Recent = new RecentNetType();
			for (Record record : recentRecords) {
				// 判断是否存在过wifi
				boolean isNetworkClassWifi = record.nettype == NetTypeDetector.NetType.WIFI ||
						record.beforeType == NetTypeDetector.NetType.WIFI;
				if(isNetworkClassWifi){
					Recent.wifi = true;
				}
				// 判断是否存在过断网
				boolean isNetworkClassDisconnect = record.nettype == NetTypeDetector.NetType.DISCONNECT
						|| record.beforeType == NetTypeDetector.NetType.DISCONNECT;
				if(isNetworkClassDisconnect){
					Recent.disconnect = true;
				}
				// 判断是否存在过数据网络
				boolean isNetworkClassMobile = NetManager.isNetworkClassMobile(record.nettype) ||
						NetManager.isNetworkClassMobile(record.beforeType) ;
				if(isNetworkClassMobile){
					Recent.mobile = true;
				}
			}
			return Recent;
		}
		
		/**
		 * 根据最近的网络记录判断最近的 移动数据、wifi、断网的情况得出应属的定义网络类型
		 * @return
		 */
		public RecentState getRecentNetState(){
			RecentState  recentState = null;
			if(wifi&&mobile){//wifi + 数据
				recentState = RecentState.wifi_data;
			}else if(!disconnect){//没有发生断网 
				recentState = RecentState.no_disconnect;
			}else if(disconnect&&!wifi&&!mobile){//只有断网   并且没有 wifi没有 数据
				recentState = RecentState.disconnect;
			}else if(wifi&&disconnect){// wifi + 断网
				recentState = RecentState.wifi;
			}else{//其他  （数据+ 断网）
				recentState = RecentState.data;
			}
			return recentState;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("RecentNetType [mobile=");
			builder.append(mobile);
			builder.append(", wifi=");
			builder.append(wifi);
			builder.append(", disconnect=");
			builder.append(disconnect);
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	/**
	 * 最近状态定义
	 */
	public enum RecentState{
		/** wifi + 数据 */
		wifi_data("网络发生切换，开始断线重连"),
		/** 没有发生断网 */
		no_disconnect("网络发生异常，开始断线重连"),
		/** 只有断网 */
		disconnect("网络连接已断开，开始断线重连"),
		/** wifi + 断网 */
		wifi("WiFi已断开，开始断线重连"),
		/** 其他*/
		data("移动数据已断开，开始断线重连");
		private String desc;//描述信息
		RecentState(String desc) {
			this.desc = desc;
		}
		public String getDesc() {
			return desc;
		}
	}
	
	/**
	 * 自定义数据结构
	 * 添加数据最大长度限制 
	 * @param <T>
	 */
	private static final class BoundsStack<T> extends Stack<T>{
		private static final long serialVersionUID = 1L;
		private int capacity = -1;

		public void setCapacity(int capacity) {
			this.capacity = capacity;
		}
		
		@Override
		public boolean add(T object) {
			if(capacity>0 && size() >= capacity){
				remove(0);
			}
			return super.add(object);
		}
	}
}