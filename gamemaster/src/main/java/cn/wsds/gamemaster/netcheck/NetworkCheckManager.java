package cn.wsds.gamemaster.netcheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.subao.common.SuBaoObservable;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.CalendarUtils;
import com.subao.data.InstalledAppInfo;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager.Checker.Result;
import cn.wsds.gamemaster.statistic.Statistic;

public class NetworkCheckManager extends AsyncTask<Void, Void, Result> {
	
	private static final String[] SECURITYAPPS = new String[]{"腾讯手机管家","360卫士",
			"百度手机卫士","猎豹安全大师","lbe安全大师"};
	
	public interface Checker {
		/**
		 * 网络检查的结果
		 */
		public enum Result {
			OK("网络正常"),
			ABORT("检测中断"),
			WIFI_FAIL_RETRIEVE_ADDRESS("WiFi地址获取失败"),
			WIFI_UNAVAILABLE("当前WiFi质量较差"), //当前WiFi不可用
			WIFI_SHOULD_AUTHORIZE("当前WiFi网络需要验证"),
			NETWORK_AUTHORIZATION_FORBIDDED("网络权限被禁止"),
			WAP_POINT("正在使用WAP接入点"),
			MOBILE_UNAVAILABLE("当前移动数据连接质量较差"), //当前移动数据连接不可用
			AIRPLANE_MODE("飞行模式已开启"),
			WIFI_MOBILE_CLOSED("WiFi与数据连接已关闭"),
			NETWORK_DISCONNECT("我的网络已断开"),
			IP_ADDR_ASSIGN_PENDING("内网地址尚未分配"),
			UNKNOWN("未知网络故障");
			
			private Result(String description) {
				this.description = description;
			}
			
			public final String description;
			
			@Override
			public String toString() {
				return this.description;
			}
		}
		public Result run();
	}
	
	public interface Observer {
		void onNetworkCheckResult(NetworkCheckManager.Checker.Result event);
	}
	
	private static NetworkCheckManager instance;

	private final Observers observers = new Observers();

	private static class Observers extends SuBaoObservable<Observer> {

		/**
		 * 通知所有观察者。
		 * <p>
		 * <b>本函数必须在主线程里调用</b>
		 * </p>
		 * 
		 */
		public void notifyObservers(Result result) {
			List<Observer> list = this.cloneAllObservers();
			if (list != null) {
				for (Observer o : list) {
					o.onNetworkCheckResult(result);
				}
			}
		}
	};
	
	private static class NetErrorReporter{
		private static boolean hasReportNetError = false ;
		
		private static void reportNetCheckEvent(Result result){
			if(result==null){
				return ;
			}
			
			if(netStateIsNormal(result)){
				hasReportNetError = false ;
			}else if(!hasReportNetError){
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.NETWORK_CHECK, result.toString());
				hasReportNetError = true ;				 
			}
		}
		
		private static boolean netStateIsNormal(Result result){
			return ((Result.OK==result)||(Result.ABORT==result)) ;
		}	
	}

	private final Queue<Checker> checkerQueue = new ConcurrentLinkedQueue<Checker>();
	
	private NetworkCheckManager(Context context, Observer observer) {
		checkerQueue.add(new NetworkStateChecker(context));
		// 如果需要，在这里继续添加Checker
		// ...
		//
		if (observer != null) {
			this.observers.registerObserver(observer);
		}
	}

	/**
	 * 主线程中调用，开始网络检查
	 */
	public static void start(Context context, Observer observer) {
		if (instance == null) {
			instance = new NetworkCheckManager(context, observer);
			instance.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
		} else {
			instance.observers.registerObserver(observer);
		}
	}

	/**
	 * 主线程中调用，停止网络检查
	 */
	public static void postStopRequest() {
		if (instance != null) {
			if (!instance.isCancelled()) {
				instance.cancel(true);
			}
		}
	}

	@Override
	protected Result doInBackground(Void... params) {
		while (!isCancelled()) {
			Checker checker = checkerQueue.poll();
			if (checker == null) {
				return Result.OK;
			}
			Result result = checker.run();
			if (result != null) {
				return result;
			}
		}
		return Result.ABORT;
	}

	@Override
	protected void onPostExecute(Result result) {
		cleanup(result);
	}

	@Override
	protected void onCancelled(Result result) {
		cleanup(result);
	}

	private void cleanup(Result result) {
		NetErrorReporter.reportNetCheckEvent(result);
		observers.notifyObservers(result);
		if(Result.UNKNOWN.equals(result)){			 
			if(doesReportUnknownNetworkNeed()){
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.NETWORK_NOT_IDENTIFY, android.os.Build.MODEL); 
			}		 	
		}
		
		this.observers.unregisterAllObservers();
		if (instance == this) {
			instance = null;
		}
	}
	
	public static void addNetForbiddedEvent(){		
		List<String> installedSecurityApps = new ArrayList<String>(2);	 
		InstalledAppInfo[] apps = GameManager.getInstance().getInstalledApps();
			
		for(InstalledAppInfo app :apps){
		    String appLabel = app.getAppLabel();
		    if(checkSecurityApp(appLabel)){
		    	installedSecurityApps.add(appLabel);
		    }
		}
		 	    
	    StringBuilder sb = new StringBuilder();
	    sb.append(android.os.Build.MODEL);  
	    sb.append(",");
	    sb.append(Build.DISPLAY);
	    
	    if(!installedSecurityApps.isEmpty()){
	    	sb.append(",");
	        int count = installedSecurityApps.size();     
	        for(int i =0 ; i<count ; i++){
	        	sb.append(installedSecurityApps.get(i));
	        	if(i<count-1){
	        		sb.append(",");
	        	}
	        } 	
	    }
	  
	    Statistic.addEvent(AppMain.getContext(), Statistic.Event.NETWORK_PERMISSION_DENY, sb.toString()); 
	}
	
	private static boolean checkSecurityApp(String appLabel){
		for(String label : SECURITYAPPS){
			if(label.equals(appLabel)){
				return true ;
			}
		}
		
		return false ;
	}
	
	/**
	 * 判断是否需要上报网络无法识别事件
	 * 
	 * @return true表示需要做，false表示不需要
	 */
	private static boolean doesReportUnknownNetworkNeed() {
		int today = CalendarUtils.todayLocal();
		return (today != ConfigManager.getInstance().getDayOfNetWorkUnknown());
	}
	
	public static String getNetTypeStr(NetTypeDetector.NetType type){	
		if(type==null){
			return null ;
		}
	 
		if(NetTypeDetector.NetType.WIFI.equals(type)){
			return "WiFi" ;
		}else if(NetTypeDetector.NetType.MOBILE_4G.equals(type)){
			return "4G";
		}else if(NetTypeDetector.NetType.MOBILE_3G.equals(type)){
			return "3G";
		}else if(NetTypeDetector.NetType.MOBILE_2G.equals(type)){
			return "2G";
		}else{
			return null ;
		}
	}
	
	public static String getBadNetStateMsg(NetTypeDetector.NetType type , boolean isInAppActivity){
		if(type==null){
			return null;
		}
		
		String netType = getNetTypeStr(type);
		if(netType == null){
			return null ;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("当前");
		builder.append(netType);
		builder.append("质量较差");
		if(isInAppActivity){
			builder.append(",建议您切换网络");
		}
		
		return builder.toString() ;
	}

}
