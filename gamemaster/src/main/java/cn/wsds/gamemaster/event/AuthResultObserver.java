package cn.wsds.gamemaster.event;

public interface AuthResultObserver{

	public void onGetJWTTokenResult(String jwtToken, long expires, String shortId,
									int userStatus, String expiredTime, boolean result, int code);
	
	public void onGetTokenResult(String ip, byte[] token, int length, int expires,
								 boolean result, int code) ;
	
	public void onGetUserAccelStatusResult(String shortId, int status, String expiredTime,
										   boolean result, int code);
	
	public void onGetUserConfigResult(String config, int code, boolean result);
}
