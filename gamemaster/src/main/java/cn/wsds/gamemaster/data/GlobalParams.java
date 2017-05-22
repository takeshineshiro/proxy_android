package cn.wsds.gamemaster.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 全局动态参数
 */
public class GlobalParams {

	/**
	 * 当前服务器时间 
	 */
	public final Date currentServerTime;
	
	/**
	 * 
	 */
	public final String baseApiUrl;
	
	/**
	 * 礼包URL
	 */
	public final String coupounUrl;
	
	/**
	 * 任务列表 
	 */
	//TODO task  
	public final List<Task> tasks;
	
	/**
	 * 参数 版本
	 */
	public final Integer version;

	private static GlobalParams instance;
	
	private GlobalParams(Date currentServerTime, String baseApiUrl,String coupounUrl, List<Task> tasks, Integer version) {
		super();
		this.currentServerTime = currentServerTime;
		this.baseApiUrl = baseApiUrl;
		this.coupounUrl = coupounUrl;
		this.tasks = tasks;
		this.version = version;
	}
	
	public static GlobalParams getInstance(){
		//TODO 
		if(instance == null){
			Date currentServerTime = new Date(System.currentTimeMillis());
			String baseApiUrl = "";
			String coupounUrl = "";
			List<Task> tasks = new ArrayList<Task>();
			int version = 1;
			instance = new GlobalParams(currentServerTime, baseApiUrl, coupounUrl, tasks, version);
		}
		return instance;
	}
		
}

