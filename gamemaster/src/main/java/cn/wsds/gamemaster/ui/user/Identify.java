package cn.wsds.gamemaster.ui.user;

import android.text.TextUtils;

import com.subao.common.Logger;
import com.subao.common.SuBaoObservable;
import com.subao.common.utils.CalendarUtils;
import com.subao.net.NetManager;

import java.util.Calendar;
import java.util.List;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.event.AuthResultObserver;
import cn.wsds.gamemaster.event.AuthResultObserverManager;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.DateUtils;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.UIUtils;

public class Identify {

	public static final int IDENTIFY_CHECK_NO_ERROR = 0;
	public static final int IDENTIFY_CHECK_ERROR_NET = 1 ;
	public static final int IDENTIFY_CHECK_ERROR_SERVER = 2 ;
	public static final int IDENTIFY_CHECK_ERROR_BINDER = 3 ;

	private static final String TestToken = "eyJhbGciOiJSUzUxMiJ9.ey" +
			"JpYXQiOjE0NjkwOTUyMDYsImV4cCI6MTc4NDQ1" +
			"NTIwNiwiY29udGVudCI6IntcInRlbmFudFwiOlwiYW5kcm9" +
			"pZFwiLFwiYWNjb3VudFwiOlwiYzJkNTA0YzYtMWJkZC00MmQyLW" +
			"FkMTgtNDU2MjJjMjVkZDMzXCIsXCJzY29wZXNcIjpbXCJCYWNrYm9" +
			"uZVwiLFwiUW9zXCIsXCJNdWx0aVwiLFwiSHJcIixcIlZhdWx0XCJdfSJ9" +
			".0cMi6J3fItANTXs6xJRMjUwjf8yRTdR16yXaZEcTtDm6sUa94l_wdLTaY" +
			"X4kZmNqO5tr7FBhYuxH6dYgLVW-A8YzsS-m_CqKxfyQg6Dst-cavhoIuUM2JgSL" +
			"G8fjOLNWnyLbEGsbbehIvCHNAdsia0Jz5fSPo68Jnds22_qmOOk";

	private static final String TAG = "Identify" ;

	public enum VIPStatus{
		VIP_NO_ACTIVATED,
		VIP_FREE,
		VIP_VALID,
		VIP_EXPIRED,
		USER_NOT_LOGIN
	}

	/** 鉴权开始-结束监听器，用于鉴权结果对应的UI变换及加速开关控制 */
	public interface IdentifyCheckListener{
		void onCheckStart();
		void onCheckResult(VIPInfo result, int check_error_code);
	}

	public interface VIPInfoObserver {
		/**
		 * VIP信息更新
		 *
		 * @param vipInfo
		 *            上传结果
		 */
		void onVIPInfoChanged(VIPInfo vipInfo);
	}

	private static final Identify INSTANCE = new Identify();
	private static final VIPInfo VIP_INFO = new VIPInfo();

	private static final DefaultIdentifyCheckListener defaultListener =
			new DefaultIdentifyCheckListener();

	private static class DefaultIdentifyCheckListener implements IdentifyCheckListener {
		@Override
		public void onCheckStart() {

		}

		@Override
		public void onCheckResult(Identify.VIPInfo result, int check_error_code){
			switch (check_error_code){
				case IDENTIFY_CHECK_ERROR_NET:
					UIUtils.showToast("网络原因，请稍后再试");
					Statistic.addEvent(AppMain.getContext(),
							Statistic.Event.ACCOUNT_RIGHTCERTIFICATION_FAILED,
							"网络异常");
					break;
				case IDENTIFY_CHECK_ERROR_SERVER:
					UIUtils.showToast("VIP认证失败，开始普通加速");
					Statistic.addEvent(AppMain.getContext(),
							Statistic.Event.ACCOUNT_RIGHTCERTIFICATION_FAILED,
							"服务器异常");
					break;
				case IDENTIFY_CHECK_ERROR_BINDER:
					showBinderErrorToast();
					break;
				default:
					break;
			}
		}
	}

	private static class NotifyIdentifyCheckListener extends
			DefaultIdentifyCheckListener {

		private final long expiredRemindInterval = 24 * 3600 *1000 ;

		@Override
		public void onCheckResult(VIPInfo result, int check_error_code) {
			super.onCheckResult(result, check_error_code);
			if (result == null) {
				return;
			}

			boolean expired = isExpired(result.getStatus());
			if(expired) {
				AppNotificationManager.sendUserExpired();
			}else{
				checkExpiredTime(result.getExpiredTimeStr());
			}
		}

		private boolean isExpired(VIPStatus status){
			return  VIPStatus.VIP_EXPIRED.equals(status);
		}

		private void checkExpiredTime(String expireTime){
			if(TextUtils.isEmpty(expireTime)){
				return;
			}

			long currentTime = System.currentTimeMillis();
			long expire = DateUtils.string2long(expireTime,
					DateUtils.SERVER_TIME_FORMAT);

			if((expire-currentTime)<=expiredRemindInterval){
				notifyWillBeExpiredIfNeed(expire);
			}
		}

		private void notifyWillBeExpiredIfNeed(long expire){
			int today = CalendarUtils.todayLocal();
			if(today== ConfigManager.getInstance()
					.getDayOfRemindVIPWillBeExpired()) {
				return;
			}

			Calendar calendarExpire = DateUtils.long2Calender(expire);
			int month = calendarExpire.get(Calendar.MONTH)+1;
			int day = calendarExpire.get(Calendar.DAY_OF_MONTH);

			AppNotificationManager.sendUserWillBeExpired(
					getExpiredDateString(month, day));

			ConfigManager.getInstance().setDayOfRemindVIPWillBeExpired(today);
		}

		private String getExpiredDateString(int month , int day){
			StringBuilder sb = new StringBuilder();
			sb.append(dateFormat(month));
			sb.append("月");
			sb.append(dateFormat(day));
			sb.append("日");

			return sb.toString();
		}

		private String dateFormat(int date){
			return String.format("%02d",date);
		}
	}

	private ResultObserver checkReultObserver ;
	private final Observers vipInfoObservers = new Observers();

	public static class VIPInfo{
		private String jwtToken = null;
		private VIPStatus status = VIPStatus.USER_NOT_LOGIN;
		private String expiredTimeStr = null ;

		public VIPStatus getStatus() {
			return status;
		}

		private void setInfos(String jwtToken , VIPStatus status ,
							  String expiredTimeStr) {
			this.jwtToken = jwtToken;
			this.status = status;
			this.expiredTimeStr = expiredTimeStr ;
		}

		public String getExpiredTimeStr() {
			return expiredTimeStr;
		}

		public String getJwtToken() {
			return jwtToken;
		}

		private void reset(){
			jwtToken = null ;
			status = VIPStatus.USER_NOT_LOGIN;
			expiredTimeStr = null ;
		}
	}

	private static final class Observers extends
			SuBaoObservable<VIPInfoObserver> {

		public void onVIPInfoChanged(VIPInfo vipInfo) {
			List<VIPInfoObserver> list = this.cloneAllObservers();
			if (list != null) {
				for (VIPInfoObserver o : list) {
					o.onVIPInfoChanged(vipInfo);
				}
			}
		}
	}

	public void registerObservers(VIPInfoObserver observer) {
		vipInfoObservers.registerObserver(observer);
	}

	public void unregisterObserver(VIPInfoObserver observer) {
		vipInfoObservers.unregisterObserver(observer);
	}

	/** 鉴权结果观察者 */
	private final class ResultObserver implements AuthResultObserver{

		private final IdentifyCheckListener listener ;
		private final int checkFailedCountLimit = 2 ;
		private int failedCount = 0 ;

		ResultObserver(IdentifyCheckListener listener){
			this.listener = listener ;
		}

		@Override
		public void onGetJWTTokenResult(String jwtToken, long expires,
										String shortId, int userStatus,
										String expiredTime, boolean result,
										int code) {

			if(!isCheckNormal(code)){
				failedCount++ ;
				if(failedCountInLimit()){
					return ;
				}
			}

			int errorCode = refreshVIPInfo(jwtToken, expiredTime,userStatus);
			notifyCheckListner(listener,VIP_INFO,errorCode);
			notifyVIPInfoObservers(VIP_INFO);

			doCheckComplete();
		}

		private boolean failedCountInLimit(){
			//如果鉴权返回失败，次数<2的情况下，
			// 可以先不更新vip信息，等待底层再一次的重试结果
			return failedCount<checkFailedCountLimit ;
		}

		private boolean isCheckNormal(int code){
			return ((code==201)||(code==401));
		}

		private int refreshVIPInfo(String jwtToken, String  expiredTime,
								   int userStatus){

			VIPStatus status = toVIPStatus(userStatus);
			VIP_INFO.setInfos(jwtToken,status,expiredTime);

			if(isUserLogout(userStatus)||isGotVIPStatus(userStatus)){
				return IDENTIFY_CHECK_NO_ERROR;
			}else{
				return IDENTIFY_CHECK_ERROR_SERVER;
			}
		}

		private VIPStatus toVIPStatus(int userStatus){
			if(isUserLogout(userStatus)){
				return VIPStatus.USER_NOT_LOGIN;
			}

			switch (userStatus){
				case 1:
					return VIPStatus.VIP_NO_ACTIVATED;
				case 3:
				case 5:
					return VIPStatus.VIP_EXPIRED;
				case 2:
					return VIPStatus.VIP_FREE;
				case 4:
				case 6:
					return VIPStatus.VIP_VALID ;
				default:
					return VIPStatus.USER_NOT_LOGIN;
			}
		}

		private boolean isUserLogout(int userStatus){
			return (userStatus<=0) ; //((userStatus==0)&&(code==401)) ;
		}

		private boolean isGotVIPStatus(int userStatus){
			return ((userStatus>=1)&&(userStatus<=6)) ;
		}

		@Override
		public void onGetTokenResult(String ip, byte[] token, int length,
									 int expires, boolean result, int code) {

		}

		@Override
		public void onGetUserAccelStatusResult(String shortId, int status,
											   String expiredTime,
											   boolean result, int code) {

		}

		@Override
		public void onGetUserConfigResult(String config, int code,
										  boolean result) {

		}

		private void reset(){
			failedCount = 0 ;
		}
	}

	public static Identify getInstance(){
		return INSTANCE ;
	}

	public void startCheck(IdentifyCheckListener listener){

		if(!init(listener)){
			return;
		}

		registerAuthObserver(listener);

		VPNUtils.setUserToken(UserSession.getInstance().getUserId(),
				getsessinInfo().getAccessToken(), "cn.wsds.gamemaster",TAG); //开始鉴权
		if(listener!=null){
			listener.onCheckStart();
		}
	}

	private boolean init(IdentifyCheckListener listener){

		VIP_INFO.reset();

		return (isNetValid(listener)&&isLogin(listener)&&
				isSessionInfoValid(listener)&&isServiceBinderEnable(listener));
	}

	private boolean isNetValid(IdentifyCheckListener listener){
		if(NetManager.getInstance().isConnected()){
			return true ;
		}

		notifyCheckListner(listener,VIP_INFO,IDENTIFY_CHECK_ERROR_NET);

		return false;
	}

	private boolean isLogin(IdentifyCheckListener listener){
		if(UserSession.isLogined()){
			return true;
		}

		VIP_INFO.setInfos(null,VIPStatus.USER_NOT_LOGIN,null);
		notifyCheckListner(listener,VIP_INFO,IDENTIFY_CHECK_NO_ERROR);

		return false;
	}

	private boolean isSessionInfoValid(IdentifyCheckListener listener){
		if(getsessinInfo()!=null) {
			return true;
		}

		notifyCheckListner(listener,VIP_INFO,IDENTIFY_CHECK_NO_ERROR);
		return false;
	}

	private SessionInfo getsessinInfo(){
		return UserSession.getInstance().getSessionInfo();
	}

	//登记鉴权结果观察者
	private void registerAuthObserver(IdentifyCheckListener listener){
		checkReultObserver = new ResultObserver(listener);
		AuthResultObserverManager.getInstance().
				registerObserver(checkReultObserver);

	}

	private boolean isServiceBinderEnable(IdentifyCheckListener listener){
		if(VPNUtils.isBinderEnable()){
			return true ;
		}

		Logger.d(TAG, "vpnGameService has problem , can not start accel!");
		notifyCheckListner(listener,VIP_INFO,IDENTIFY_CHECK_ERROR_BINDER);

		return false;
	}

	private static void showBinderErrorToast(){
		UIUtils.showToast("抱歉，模块异常导致无法开启加速，请尝试退出app后重启服务！");
	}

	private static void notifyCheckListner(IdentifyCheckListener listener ,
										   VIPInfo info ,int error_code){
		if(listener==null){
			return;
		}

		listener.onCheckResult(info,error_code);
	}

	public void notifyUserLogout(){
		VIP_INFO.reset();
		notifyVIPInfoObservers(VIP_INFO);
	}

	public String getJWTToken() {
		return VIP_INFO.getJwtToken();
		//return TestToken;
	}

	public VIPStatus getVIPStatus(){
		return VIP_INFO.getStatus() ;
	}

	public String getExpiredTimeStr(){
		return VIP_INFO.getExpiredTimeStr();
	}

	public VIPInfo getVipInfo(){
		return VIP_INFO;
	}

	private void notifyVIPInfoObservers(VIPInfo info){
		vipInfoObservers.onVIPInfoChanged(info);
	}

	private void doCheckComplete(){
		checkReultObserver.reset();

		if(checkReultObserver!=null){
			AuthResultObserverManager.getInstance().
					unregisterObserver(checkReultObserver);
		}
	}

	/**
	 * 默认的鉴权处理
	 */
	public static void defaultStartCheck() {
		Identify.getInstance().startCheck (defaultListener);
	}

	/**
	 * 鉴权失败通知
	 */
	public static void notifyStartCheck() {
		Identify.getInstance().startCheck(new NotifyIdentifyCheckListener());
	}
}
