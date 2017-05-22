package cn.wsds.gamemaster.data;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.subao.common.SuBaoObservable;
import com.subao.common.utils.ThreadUtils;

import java.util.List;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.net.http.UpdateAccessTokenRequestor;
import cn.wsds.gamemaster.social.SOCIAL_MEDIA;
import cn.wsds.gamemaster.tools.JPushUtils;
import hr.client.appuser.SessionInfoOuterClass;
import hr.client.appuser.UserInfoOuterClass;


public class UserSession {

	/**
	 * 是否是Vip
	 * @return true/false
	 */
	public boolean vipStatus() {
		return true;
	}

	public enum UserType {
		NOT_LOGIN(0), LOGIN(1), BIND_PHONE(2);

		private int value;

		UserType(int _value) {
			this.value = _value;
		}

		public int getValue() {
			return value;
		}

		public static UserType fromValue(int value) {
			for (UserType my: UserType.values()) {
				if (my.value == value) {
					return my;
				}
			}
			return null;
		}
	}
	
	public enum LogoutReason{
		USER_CLICK_EXIT,
		LOGIN_ON_OTHER_DEVICE,
		SERVICE_ERROR,
		NET_ERROR,
		OTHER_REASON;
	}

	private static final UserSession instance = new UserSession();
	private static final String TAG = "UserSession";
	private SessionInfo sessionInfo;
	private UserInfo userInfo;
	/**
	 * 用户信息被观察者
	 */
	public final UserSessionObservable userSessionObservable = new UserSessionObservable();
	
	private UserSession() {
		SessionInfo sessionInfo = SessionInfo.loadFromProto();
		setSessionInfo(sessionInfo);
		UserInfo userInfo = UserInfo.loadFromProto();
		setUserInfo(userInfo);
	}
	
	public static UserSession getInstance() {
		if (!ThreadUtils.isInAndroidUIThread()) {
			Log.e(TAG, "UserSession must be called in main thread");
		}
		return instance;
	}
	
	/**
	 * 判断用户是否登录
	 * @return
	 */
	public static boolean isLogined(){
		return instance.sessionInfo!=null && instance.userInfo!=null;
	}
	
	public static void logout(LogoutReason reason){
    	SessionInfo.clear();
    	UserInfo.clear();
    	instance.updateSessionInfo(null);
    	instance.updateUserInfo(null);
    	UpdateAccessTokenRequestor.stop();
    	DataCache.getPointsChangeRecordCache().clear();
    	
    	/*if (AccelOpenManager.isStarted()) {
			AccelOpenManager.close(CloseReason.SETTING_CLOSE);
			
			String msg ;
			switch(reason){
	    	case USER_CLICK_EXIT:
	    		msg = "用户已注销，迅游加速关闭" ;	    		
	    		break;
	    	case LOGIN_ON_OTHER_DEVICE:
	    		msg = "用户在其他终端登录，迅游加速关闭" ;
	    		break;
	    	default:
	    		return;	    		 
	    	}
			AppNotificationManager.sendNoticeLogoutCloseAccel(msg);
			UIUtils.showToast(msg);
		}*/
    		
    }

	/**
	 * 获取会话信息
	 * @return
	 *  不保证一定有值
	 */
	public SessionInfo getSessionInfo() {
		return sessionInfo;
	}
	
	/**
	 * 获取用户信息
	 * @return
	 */
	public UserInfo getUserInfo() {
		return userInfo;
	}
	
	/**
	 * 获取当前用户的UserID
	 * @return 如果已登录，返回当前用户的UserID；否则返回空串
	 */
	public String getUserId() {
		return userInfo == null ? "" : userInfo.userId;
	}

	private void setSessionInfo(SessionInfo sessionInfo) {
		this.sessionInfo = sessionInfo;
	}

	private void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}
	
	public boolean updateToken(String accessToken, String refreshToken,int expiresIn) {
		if(this.sessionInfo==null){
			return false;
		}
		boolean updateToken = this.sessionInfo.updateToken(accessToken, refreshToken, expiresIn);
		if(updateToken){
			this.sessionInfo.save();
			userSessionObservable.onSessionInfoChanged(sessionInfo);
		}
		return updateToken;
	}

	/**
	 * 更新从服务端取的会话信息
	 * @param protoInfo 从服务端取的用户信息
	 */
	public void updateSessionInfoByServerProto(SessionInfoOuterClass.SessionInfo protoInfo){
		SessionInfo sessionInfo = new SessionInfo(protoInfo);
		if(this.sessionInfo==null || sessionInfo == null){
			updateSessionInfo(sessionInfo);
			return;
		}else if(!this.sessionInfo.equals(sessionInfo)){
			updateSessionInfo(sessionInfo);
		}
	}

	private void updateSessionInfo(SessionInfo sessionInfo) {
		setSessionInfo(sessionInfo);
		userSessionObservable.onSessionInfoChanged(this.sessionInfo);
		if(this.sessionInfo!=null){
			this.sessionInfo.save();
		}
	}
	
	public void updateUserInfoByServerProto(UserInfoOuterClass.UserInfo protoInfo){
		if(this.userInfo!=null && this.userInfo.thirdPart){
			updateUserInfoByServerProto(protoInfo, this.userInfo.getThirdPartNickName(),this.userInfo.getSocial_MEDIA(),this.userInfo.getDrawableAvatar());
		}else{
			updateUserInfoByServerProto(protoInfo, null, null, null);
		}
	}

	/*public void updateVIPInfo(CheckResultType checkResultType, String expriedTime) {
		userSessionObservable.onVIPInfo(new VIPINFO(checkResultType, expriedTime));
	}*/
	
	public void updateUserInfoByServerProto(UserInfoOuterClass.UserInfo protoInfo, String thirdPartNickName,SOCIAL_MEDIA social_MEDIA, Bitmap drawableAvatar) {
		UserInfo userInfo = new UserInfo(protoInfo,thirdPartNickName,social_MEDIA,drawableAvatar);
		if(this.userInfo==null || userInfo == null){
			updateUserInfo(userInfo);
		}else if(!this.userInfo.equals(userInfo)){
			updateUserInfo(userInfo);
		}
	}

	private void updateUserInfo(UserInfo userInfo) {
		setUserInfo(userInfo);
		onUpdateUserInfo();
	}
	
	private void onUpdateUserInfo() {
		userSessionObservable.onUserInfoChanged(this.userInfo);
		if(this.userInfo!=null){
			this.userInfo.save();
		}
	}
	
	/**
	 * 用户是否绑定了手机号
	 */
	public static boolean isBoundPhoneNumber(){
		if(!isLogined()) {
			return false;
		}
		if(instance.userInfo==null){
			return false;
		}
		boolean phoneNumberEmpty = TextUtils.isEmpty(instance.userInfo.phoneNumber);
		return !phoneNumberEmpty;
	}


	/**
	 * return: 0:未登录 1:已登录 2:已验证手机号
	 */
	public static UserType userType() {
		UserType result = UserType.NOT_LOGIN;
		if(isLogined()) {
			result = UserType.LOGIN;
			if(isBoundPhoneNumber()) {
				result = UserType.BIND_PHONE;
			}
		}
		return result;
	}

	/**
	 * 修改用户积分
	 * @param score
	 * @return
	 *  修改结果 当且仅当用户信息对象不存在并且用户原积分数据和 {@code score} 不一样时返回true 其他返回false
	 */
	public boolean updateSorce(int score) {
		if(null == this.userInfo){
			return false;
		}
		boolean updateScore = this.userInfo.updateScore(score);
		if(updateScore){
			this.userInfo.save();
			userSessionObservable.onScoreChanged(this.userInfo.getScore());
		}
		return updateScore;
	}
	
	/**
	 * 修改签到时间戳
	 * @param timestampSign 签到时间戳
	 * @return
	 * true 修改成功并通知各个观察者用户信息被修改
	 * false userInfo 对象为空 ，或者时间和原时间一样，无需更新
	 */
	public boolean updateTimestampSignin(long timestampSign){
		if(null == this.userInfo){
			return false;
		}
		boolean updateTimestampSign = this.userInfo.updateTimestampSignin(timestampSign);
		if(updateTimestampSign){
			this.userInfo.save();
			userSessionObservable.onUserInfoChanged(this.userInfo);
		}
		return updateTimestampSign;
	}
	
	/**
	 * 用户信息及会话信息被观察者
	 */
	private static final class UserSessionObservable extends SuBaoObservable<UserSessionObserver> {
		
		public void onScoreChanged(int score) {
			//积分改变时，设置JPush tag           
			setJpushTag();
			List<UserSessionObserver> list = this.cloneAllObservers();
			if (list != null) {
				for (UserSessionObserver o : list) {
					o.onScoreChanged(score);
				}
			}
		}
		
		public void onUserInfoChanged(UserInfo info){
			List<UserSessionObserver> list = this.cloneAllObservers();
			if (list != null) {
				for (UserSessionObserver o : list) {
					o.onUserInfoChanged(info);
				}
			}
		}

		public void onVIPInfo(VIPINFO vipinfo) {
			List<UserSessionObserver> list = this.cloneAllObservers();
			if (list != null) {
				for (UserSessionObserver o : list) {
					o.onVIPInfoChanged(vipinfo);
				}
			}
		}
		public void onSessionInfoChanged(SessionInfo info){
			List<UserSessionObserver> list = this.cloneAllObservers();
			if (list != null) {
				for (UserSessionObserver o : list) {
					o.onSessionInfoChanged(info);
				}
			}
		}
	}
	
	/**
	 * 用户信息及会话信息观察者
	 */
	public static abstract class UserSessionObserver {
		
		/**
		 * 积分发生变化
		 * @param score 积分数值
		 */
		public void onScoreChanged(int score){};
		
		/**
		 * 当用户信息发生变化
		 * @param info 用户信息
		 */
		public void onUserInfoChanged(UserInfo info){};
		
		/**
		 * 当会话信息发生变化
		 * @param info 会话信息
		 */
		public void onSessionInfoChanged(SessionInfo info){};

		/**
		 * vip信息
		 * @param vipinfo 信息
		 */
		public void onVIPInfoChanged(VIPINFO vipinfo){}
	}
	
	/**
	 * 注册用户信息观察者
	 *  当不需要观察时务必反注册，否则杀无赦
	 * @param observer 用户信息观察者
	 */
	public void registerUserSessionObserver(UserSessionObserver observer){
		this.userSessionObservable.registerObserver(observer);
	}
	
	public void unregisterUserSessionObserver(UserSessionObserver observer){
		this.userSessionObservable.unregisterObserver(observer);
	}

	public static void init() { }
	
	private static void setJpushTag(){
		JPushUtils.setTagsForJPush(AppMain.getContext());
	}
}
