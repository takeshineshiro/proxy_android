package cn.wsds.gamemaster.net.http;

import android.text.TextUtils;

import com.google.protobuf.InvalidProtocolBufferException;

import java.net.HttpURLConnection;

import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.data.UserSession.LogoutReason;
import cn.wsds.gamemaster.data.UserSession.UserSessionObserver;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.user.Identify;
import hr.client.appuser.RefreshToken.AppUpdateAccessTokenResponse;

/**
 * 用户登陆状态下刷新令牌
 */
public class UpdateAccessTokenRequestor {
	
	private static UpdateAccessTokenRequestor instance;
	/**
	 * 检查间隔时间
	 */
	private static final int CHECK_INTEVAL = 30 * 60 * 1000;
	
	/**
	 * 距令牌有效期最大时间 需更新
	 */
	private static final int MAX_TIME_MILLIS_OVERDUE_TOKEN = 25 * 60 * 60 * 1000 ;
	private Runnable checkRunnable = new Runnable() {
		
		@Override
		public void run() {
			doCheck();
		}
	};
	private UpdateAccessTokenRequestor() {}
	
	public static void startCheck(){
		if(!UserSession.isLogined()){
			return;
		}
		if(instance==null){
			instance = new UpdateAccessTokenRequestor();
			instance.doCheck();
		}
	}
	
	public static final class SessionInfoObserver extends UserSessionObserver {
		
		@Override
		public void onSessionInfoChanged(SessionInfo info) {
			if(info==null){
				UpdateAccessTokenRequestor.stop();
			}else{
				UpdateAccessTokenRequestor.startCheck();
			}
		}
	}

	private static void update() {
		SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
		String sessionId = sessionInfo.getSessionId();
		String refreashToken = sessionInfo.getRefreshToken();
		if(TextUtils.isEmpty(refreashToken) || TextUtils.isEmpty(sessionId)){
			UserSession.logout(LogoutReason.SERVICE_ERROR);
			stop();
			return;
		}
		HttpApiService.requestAppUpdateAccessToken(refreashToken, sessionId, new AppUpdateAccessTokenCallBack());
	}

	private void doCheck(){
		SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
		if(sessionInfo==null){
			stop();
			return;
		}
		long deltaTimeMillis = System.currentTimeMillis() - sessionInfo.getUpdateTokenTimeMillis();
		if((sessionInfo.getExpiresIn() * 1000 - deltaTimeMillis) < MAX_TIME_MILLIS_OVERDUE_TOKEN){
			update();
		}else{
			MainHandler.getInstance().postDelayed(checkRunnable, CHECK_INTEVAL);
		}
	}
	
	public static void stop(){
		if(instance!=null){
			MainHandler.getInstance().removeCallbacks(instance.checkRunnable);
			instance = null;
		}
	}
	
	private static final class AppUpdateAccessTokenCallBack extends DefaultNoUIResponseHandler {
		
		public AppUpdateAccessTokenCallBack() {
			super(new DefaultOnHttpUnauthorizedCallBack());
		}

		@Override
		protected void onSuccess(Response response) {
			if(HttpURLConnection.HTTP_ACCEPTED == response.code){
				parseData(response.body);
			}else{
				UserSession.logout(LogoutReason.SERVICE_ERROR);
			}
			UpdateAccessTokenRequestor.startCheck();
		}

		private void parseData(byte[] body) {
			if (body != null) {
				try {
					AppUpdateAccessTokenResponse data = AppUpdateAccessTokenResponse.parseFrom(body);
					if (0 == data.getResultCode()) {
						String accessToken = data.getAccessToken();
						String refreshToken = data.getRefreshToken();
						int expiresIn = data.getExpiresIn();
						UserSession.getInstance().updateToken(accessToken, refreshToken, expiresIn);
						Identify.notifyStartCheck();
						return;
					}
				} catch (InvalidProtocolBufferException e) {}
			}
			UserSession.logout(LogoutReason.SERVICE_ERROR);
		}
		
	}
}