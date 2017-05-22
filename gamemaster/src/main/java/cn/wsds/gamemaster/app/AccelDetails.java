package cn.wsds.gamemaster.app;

import cn.wsds.gamemaster.pb.Proto;

/**
 * 加速详情列表数据格式
 */
public class AccelDetails {
	
	public static enum NetKind {
		WIFI,
		MOBILE,
		BOTH,
		UNKNOWN,
	}
	
	private static final int MILLISECOND_OF_DAY = 24 * 3600 * 1000;
	
	private final long launchedTime; //时间（单位：ms）
	private final long duration;  //本次加速时长（单位：ms）
	private final NetKind netState;   //网络类型
	private final int percentDelayDecrease;      //平均降低延迟百分比
	private long flow;      //消耗流量（单位：byte）
	
	public AccelDetails(long launchedTime, long duration, NetKind netState, int percentDelayDecrease, long flow){
		this.launchedTime = launchedTime;
		this.duration = duration;
		this.netState = netState;
		this.percentDelayDecrease = percentDelayDecrease;
		this.flow = flow;
		
	}
	
	public AccelDetails(Proto.AccelDetails p){
		this.launchedTime = p.getLaunchedTime();
		this.duration = p.getDuration();
		this.netState = transIntToNetKind(p.getNetState());
		this.percentDelayDecrease = Math.max(0, p.getDelay());
		this.flow = p.getFlow();
	}
	
	/**
	 * 将序列化后的INT数据转换为NetKind<br />
	 * <b>注意：为兼容前一版本的数据，这里的对应关系不可更改</b>
	 */
	private static NetKind transIntToNetKind(int v) {
		switch (v) {
		case 0:
			return NetKind.WIFI;
		case 1:
			return NetKind.MOBILE;
		case 2:
			return NetKind.BOTH;
		default:
			return NetKind.UNKNOWN;
		}
	}
	
	/**
	 * 为序列化，将NetKind转换为INT<br />
	 * <b>注意：为兼容前一版本的数据，这里的对应关系不可更改</b>
	 */
	public static int transNetKindToInt(NetKind nk) {
		switch (nk) {
		case WIFI:
			return 0;
		case MOBILE:
			return 1;
		case BOTH:
			return 2;
		default:
			return 3;
		}
	}
	
	public Proto.AccelDetails serializeAccelDetails(){
		Proto.AccelDetails.Builder p = Proto.AccelDetails.newBuilder();
		p.setLaunchedTime(this.launchedTime);
		p.setDuration(this.duration);
		p.setNetState(transNetKindToInt(this.netState));
		p.setDelay(this.percentDelayDecrease);
		p.setFlow(this.flow);
		return p.build();
	}
	
	/**
	 * 返回“启动时刻”
	 * @return 启动时刻，单位毫秒
	 */
	public long getLaunchedTime(){
		return launchedTime;
	}
	
	public long getDuration(){
		return duration;
	}
	
	public NetKind getNetState(){
		return netState;
	}
	
	/** 平均降低延迟多少？ */
	public int getPercentDelayDecrease(){
		return percentDelayDecrease;
	}
	
	public long getFlow(){
		return flow;
	}
	
	public void addFlow(long flow){
		this.flow += flow;
	}
	
	public long getDate(){
		return launchedTime / MILLISECOND_OF_DAY;
	}
	
	

}
