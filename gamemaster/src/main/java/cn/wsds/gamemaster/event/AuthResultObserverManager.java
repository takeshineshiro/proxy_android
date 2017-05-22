package cn.wsds.gamemaster.event;

import com.subao.common.SuBaoObservable;

import java.util.List;

public class AuthResultObserverManager extends SuBaoObservable<AuthResultObserver>{

	private static final AuthResultObserverManager instance = new AuthResultObserverManager();
	
	public static AuthResultObserverManager getInstance(){
		return instance ;
	}
	
	public void onGetJWTTokenResult(String jwtToken, long expires, String shortId, 
			int userStatus, String expiredTime, boolean result,int code){
		List<AuthResultObserver> list = this.cloneAllObservers();
		if (list != null) {
			for (AuthResultObserver o : list) {
				 o.onGetJWTTokenResult(jwtToken, expires, shortId, userStatus,
						 expiredTime, result, code);
			}
		}
	}
	
	public void onGetTokenResult(String ip, byte[] token, int length, int expires,
			boolean result, int code){
		List<AuthResultObserver> list = this.cloneAllObservers();
		if (list != null) {
			for (AuthResultObserver o : list) {
				 o.onGetTokenResult(ip, token, length, expires, result, code);
			}
		}
	}
	
	public void onGetUserAccelStatusResult(String shortId, int status, String expiredTime,
			boolean result, int code){
		List<AuthResultObserver> list = this.cloneAllObservers();
		if (list != null) {
			for (AuthResultObserver o : list) {
				 o.onGetUserAccelStatusResult(shortId, status, expiredTime, result, code);
			}
		}
	}
	
	public void onGetUserConfigResult(String config, int code, boolean result){
		List<AuthResultObserver> list = this.cloneAllObservers();
		if (list != null) {
			for (AuthResultObserver o : list) {
				 o.onGetUserConfigResult(config, code, result);
			}
		}
	}
}
