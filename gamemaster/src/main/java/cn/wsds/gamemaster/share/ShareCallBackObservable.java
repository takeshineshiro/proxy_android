package cn.wsds.gamemaster.share;

import java.util.ArrayList;
import java.util.List;

import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;

/**
 * 分享结果相关的被观察者
 * @author Administrator
 *
 */
public class ShareCallBackObservable {
	
	private static ShareCallBackObservable instance;
	private final List<ShareObserver> observers = new ArrayList<ShareObserver>(); 
	
	private ShareCallBackObservable(){ }
	
	/**
	 * 获取实例
	 * @return
	 */
	public static ShareCallBackObservable getInstance(){
		if(instance == null){
			instance = new ShareCallBackObservable();
		}
		return instance;
	}
	
	/**
	 * 注册观察者
	 * @param observer
	 */
	public void registShareObserver(ShareObserver observer){
		observers.add(observer);
	}
	
	/**
	 * 注销观察者
	 * @param observer
	 */
	public void removeShareObserver(ShareObserver observer){
		observers.remove(observer);
	}
	
	/**
	 * 通知被观察者
	 * @param shareType
	 * @param resultCode
	 */
	public void callbackShareResult(ShareType shareType, int resultCode){
		// 因为在callbackResult里面会有观察者反注册，所以先Clone一个副本出来进行遍历
		List<ShareObserver> copy = new ArrayList<ShareObserver>(observers);
		for (ShareObserver shareObserver : copy) {
			shareObserver.callbackResult(shareType, resultCode);
		}
	}
}
