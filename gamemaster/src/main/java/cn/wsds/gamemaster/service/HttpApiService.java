package cn.wsds.gamemaster.service;

import android.text.TextUtils;
import android.util.Base64;

import com.subao.common.data.SubaoIdManager;
import com.subao.utils.UrlConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.NetworkStateChecker;
import cn.wsds.gamemaster.net.http.GMHttpClient;
import cn.wsds.gamemaster.net.http.GMHttpClient.XwsseRequestPropertiesCreater;
import cn.wsds.gamemaster.net.http.RequestProperty;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.social.SOCIAL_MEDIA;
import cn.wsds.gamemaster.social.UserSocialBean;
import cn.wsds.gamemaster.tools.onlineconfig.OnlineConfigAgent;
import hr.client.appuser.LoginUsePhoneNum;
import hr.client.appuser.ModifyUser.ModifyUserInfo;
import hr.client.appuser.RefreshToken.AppUpdateAccessToken;
import hr.client.appuser.Regist;
import hr.client.appuser.RetrievePasswd;
import hr.client.appuser.ThirdPartLogin.ReportThirdPartAuthResult;
import hr.client.appuser.VerificationCode;

/**
 * Created by lidahe on 15/12/21.
 */

public class HttpApiService {


    public interface IRequestBody {
        byte[] getBytes();
    }

    private static URL createUrl(String baseUrl, String path) {
        try {
    //			URL url = new URL("http", USER_SERVER_HOST, USER_SERVER_PORT, "/" + path);
            String format = String.format("%s%s",baseUrl, path);
            URL url = new URL(format);
            return url;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

	private static URL createUrl(String path) {
        return createUrl(OnlineConfigAgent.getInstance().getBaseApiUrl(), path);
//        return createUrl(" http://test.api.wsds.cn:2100"/*OnlineConfigAgent.getInstance().getBaseApiUrl()*/, path);
	}

	private static URL createUserUrl(String action) {
		return createUrl("/api/app/" + action);
	}

	private static URL createTaskUrl(String taskId) {
		String base = UserSession.isLogined() ? "/api/app/v210/users/tasks/" : "/api/app/v210/tasks/";
		return createUrl(base+taskId);
	}

	public static boolean prepare(ResponseHandler callback) {
    	return prepare(callback, NetworkStateChecker.defaultInstance);
	}

    private static boolean prepare(ResponseHandler callback, NetworkStateChecker networkStateChecker) {
    	if (networkStateChecker.isNetworkAvail()) {
			return true;
		}
    	if (callback != null) {
    		callback.onNetworkUnavailable();
    	}
		return false;
	}

	/**
	 * 对给定字串计算SHA1摘要，并对摘要结果进行Base64编码后返回
	 * 
	 * @param input
	 *            需要加密的内容
	 * @return 字符串为空或长度为0，或者SHA1算法不被支持时，返回原字符串。<br />
	 * 其他情况下返回处理后的字符串
	 */
	public static String encodeBySHA1(String input) {
		if (TextUtils.isEmpty(input)) {
			return input;
		}
		try {
			MessageDigest digester = MessageDigest.getInstance("SHA1");
			return Base64.encodeToString(digester.digest(input.toString().getBytes()), Base64.DEFAULT | Base64.NO_WRAP);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return input;
	}

	/**
	 * 登录
	 * @param phoneNumber
	 * @param password
	 * @param callback
	 * @return
	 */
	public static boolean requestLogin(String phoneNumber, String password, ResponseHandler callback) {
        if (prepare(callback)) {
			LoginUsePhoneNum.AppUserLogin.Builder builder = LoginUsePhoneNum.AppUserLogin.newBuilder();
			String timeStamp = XwsseRequestPropertiesCreater.getUTCTimestamp();
			builder.setPasswordSha1(encodeBySHA1(encodeBySHA1(password) + timeStamp));
			builder.setPhoneNumber(phoneNumber);
			String subaoId = SubaoIdManager.getInstance().getSubaoId();
			if(!TextUtils.isEmpty(subaoId)){
				builder.setSubaoId(subaoId);
			}
			byte[] postData = builder.build().toByteArray();
			List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(timeStamp, false);
			return GMHttpClient.post(headers,callback, createUserUrl("sessions/"), postData);
		}
		return false;
    }

	/**
	 * 向服务器申请一个手机短信验证码
	 *
	 * @param phoneNumber
	 *            手机号码
	 * @param userId
	 * @param callback
	 *            回调
	 * @return 返回True表示请求已异步提交，False表示异步提交失败（如当前没有网络等）
	 */
	public static boolean requestVerificationCode(String phoneNumber,String userId, ResponseHandler callback) {
		if (prepare(callback)) {
			VerificationCode.GetVerificationCode.Builder builder = VerificationCode.GetVerificationCode.newBuilder();
			if(!TextUtils.isEmpty(phoneNumber)){
				builder.setPhoneNumber(phoneNumber);
			}
			if(!TextUtils.isEmpty(userId)){
				builder.setUserId(userId);
			}
			byte[] postData = builder.build().toByteArray();
			return GMHttpClient.post(callback, createUserUrl("verificationcode/"), postData);
		}
		return false;
	}

	/**
	 * 向服务器申请：新用户注册
	 *
	 * @param callback
	 *            回调
	 * @param phoneNumber
	 *            手机号
	 * @param password
	 *            密码
	 * @param verifyCode
	 *            验证码
	 * @return 返回True表示请求已异步提交，False表示异步提交失败（如当前没有网络等）
	 */
	public static boolean requestRegister(String phoneNumber, String password, String verifyCode,ResponseHandler callback) {
		if (prepare(callback)) {
			Regist.RegisteAppUser.Builder builder = Regist.RegisteAppUser.newBuilder();
			builder.setPasswordSha1(encodeBySHA1(password));
			builder.setPhoneNumber(phoneNumber);
			builder.setVerificationCode(verifyCode);
			String subaoId = SubaoIdManager.getInstance().getSubaoId();
			if(!TextUtils.isEmpty(subaoId)){
				builder.setSubaoId(subaoId);
			}
			byte[] postData = builder.build().toByteArray();
			return GMHttpClient.post(callback, createUserUrl("users/"), postData);
		}
		return false;
	}

	/**
	 * 绑定手机号
	 * @param phoneNumber
	 * @param password
	 * @param verifyCode
	 * @param userId
	 * @param callback
	 * @return
	 */
	public static boolean requestBindPhone(String phoneNumber, String password,String verifyCode,String sessionId,String userId,ResponseHandler callback) {
		if (prepare(callback)) {
			ModifyUserInfo.Builder builder = ModifyUserInfo.newBuilder();
			builder.setPhoneNumber(phoneNumber);
			builder.setVerificationCode(verifyCode);
			builder.setPasswordSha1(encodeBySHA1(password));
            builder.setSessionId(sessionId);
			byte[] postData = builder.build().toByteArray();
			List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
			return GMHttpClient.post(headers,callback, createUserUrl("users/" + userId), postData);
		}
		return false;
	}

	/**
	 * 修改密码
	 * @param oldPassword
	 * @param newPassWord
	 * @param callback
	 * @param userId
	 * @return
	 */
	public static boolean requestUpdatePassword(String oldPassword,String newPassWord,String userId,ResponseHandler callback) {
		if (prepare(callback)) {
			ModifyUserInfo.Builder builder = ModifyUserInfo.newBuilder();
			String timeStamp = XwsseRequestPropertiesCreater.getUTCTimestamp();
			builder.setOldPasswordSha1(encodeBySHA1(encodeBySHA1(oldPassword) + timeStamp));
			builder.setPasswordSha1(encodeBySHA1(newPassWord));
			byte[] postData = builder.build().toByteArray();
			List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(timeStamp,true);
			return GMHttpClient.post(headers,callback, createUserUrl("users/" + userId), postData);
		}
		return false;
	}

	/**
	 * 重置密码
	 * @param phoneNumber
	 * @param password
	 * @param verifyCode
	 * @param userId
	 * @param callback
	 * @return
	 */
	public static boolean requestResetPassword(String phoneNumber,String password, String verifyCode, String userId,ResponseHandler callback) {
		if (prepare(callback)) {
			RetrievePasswd.RetrievePassword.Builder builder = RetrievePasswd.RetrievePassword.newBuilder();
			if(TextUtils.isEmpty(userId)){
				builder.setPhoneNumber(phoneNumber);
			}else{
				builder.setUserId(userId);
			}
			String subaoId = SubaoIdManager.getInstance().getSubaoId();
			if(!TextUtils.isEmpty(subaoId)){
				builder.setSubaoId(subaoId);
			}
			builder.setPasswordSha1(encodeBySHA1(password));
			builder.setVerificationCode(verifyCode);
			byte[] postData = builder.build().toByteArray();
			return GMHttpClient.post(callback, createUserUrl("users/retrievepassword/"), postData);
		}
		return false;
	}

	/**
	 * 退出登录
	 * @param sessionId
	 * @param callback
	 * @return
	 */
	public static boolean requestLogout(String userId,String sessionId,String accessToken,ResponseHandler callback) {
		if (prepare(callback)) {
			List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
			return GMHttpClient.delete(headers, callback, createUserUrl("sessions/" + sessionId));
		}
		return false;
	}

	/**
	 * 上报三方账号信息
	 * @param userSocialBean
	 * @param callback
	 * @return
	 */
	public static boolean requestReportThirdPartAuthResult(UserSocialBean userSocialBean,ResponseHandler callback){
		if (prepare(callback)) {
			ReportThirdPartAuthResult.Builder builder = ReportThirdPartAuthResult.newBuilder();
			builder.setThirdPartUid(userSocialBean.openId);
			builder.setVendor(formatLoginVendor(userSocialBean.socailMedia));
			builder.setNickName(userSocialBean.name);
//			builder.setAccessToken(userSocialBean.getToken());
			String subaoId = SubaoIdManager.getInstance().getSubaoId();
			if(!TextUtils.isEmpty(subaoId)){
				builder.setSubaoId(subaoId);
			}
			byte[] postData = builder.build().toByteArray();
			return GMHttpClient.post(callback, createUserUrl("thirdpartsessions/"), postData);
		}
		return false;
	}

	/**
	 * 格式化三方厂商信息
	 */
	private static String formatLoginVendor(SOCIAL_MEDIA media){
		switch (media) {
		case QQ:
			return "qq";
		case WEIBO:
			return "weibo";
		case WEIXIN:
		default:
			return "weixin";
		}
	}

	/**
	 * 刷新令牌
	 * @param refreashToken
	 * @param sessionId
	 * @param callback
	 * @return
	 */
	public static boolean requestAppUpdateAccessToken(String refreashToken,String sessionId,ResponseHandler callback){
		if(TextUtils.isEmpty(refreashToken)){
			return false;
		}
		if (prepare(callback)) {
			AppUpdateAccessToken.Builder builder = AppUpdateAccessToken.newBuilder();
			builder.setRefreashToken(refreashToken);
			String subaoId = SubaoIdManager.getInstance().getSubaoId();
			if(!TextUtils.isEmpty(subaoId)){
				builder.setSubaoId(subaoId);
			}
			byte[] postData = builder.build().toByteArray();
			List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
			return GMHttpClient.post(headers, callback, createUserUrl("sessions/" + sessionId), postData);
		}
		return false;
	}

	/**
	 * 获取用户信息
	 * @param userId
	 * @param callback
	 * @return
	 */
	public static boolean requestUserDetail(String userId,ResponseHandler callback) {
		if (prepare(callback)) {
			List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
			return GMHttpClient.get(headers, callback, createUserUrl("users/" + userId));
		}
		return false;
	}
	
	/**
     * 向服务器发送请求“完成任务”
     * @param taskId 任务ID
     * @param responseHandler
     */
	public static boolean requestTaskFinished(String taskId,ResponseHandler responseHandler) {
		return requestTaskFinished(taskId, null,responseHandler);
    }

    /**
     * 向服务器发送请求“完成任务”
     * @param taskId 任务ID
     * @param responseHandler
     */
	public static boolean requestTaskFinished(String taskId ,byte[] postData,ResponseHandler responseHandler) {
		if (!prepare(responseHandler)) {
    		return false;
    	}
		if(taskId == null || responseHandler == null) {
            return false;
        }
        List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
        return GMHttpClient.post(headers, responseHandler, createTaskUrl(taskId), postData);
    }

    /**
     * 请求任务进度
     * @param taskId
     * @param responseHandler
     */
    public static void requestTasksProg(String taskId, ResponseHandler responseHandler) {
    	if (!prepare(responseHandler)) {
    		return;
    	}
        if(taskId == null || responseHandler == null) {
			return;
		}
        List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
        GMHttpClient.get(headers, responseHandler, createTaskUrl(taskId));
    }


	public static void requestGlobalConfig(String fullUrl, ResponseHandler responseHandler) {
		if (!prepare(responseHandler)) {
    		return;
    	}
		try {
			GMHttpClient.get(responseHandler, new URL(fullUrl));
		} catch (MalformedURLException e) {
			if (responseHandler != null) {
				responseHandler.finish(null);
			}
		}
	}

	public static boolean requestTaskList(String baseUrl, ResponseHandler responseHandler) {
		if (!prepare(responseHandler)) {
    		return false;
    	}
		List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
		String userId = UserSession.getInstance().getUserId();
		String path = TextUtils.isEmpty(userId) ? "/api/app/v210/tasks" : String.format("/api/app/v210/users/%s/tasks", userId);
		return GMHttpClient.get(headers,responseHandler, createUrl(baseUrl, path));
	}

	public static void requestCouponsStatus(String couponId, ResponseHandler responseHandler) {
		if (!prepare(responseHandler)) {
    		return;
    	}
		String path = String.format("users/coupons/status/%s", couponId);
		List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
		GMHttpClient.get(headers, responseHandler, createUserUrl(path));
	}

	public static String getCouponHtmlUrl() {
		return OnlineConfigAgent.getInstance().getGlobalConfig().getCouponUrl();
	}

	public static String getCouponApiUrl() {
		return createUrl("/api/app/v210/users/coupons/").toString();
	}

    public static String getFlowCouponApiUrl() {
        return createUrl("/api/cross/users/coupons/").toString();
    }
	/**
	 * APP用户登录后，可获取用户积分变更的历史
	 * @param before
	 * @param number
	 * @param callback
	 * @return 
	 */
	public static boolean requestGetUserPointsHistory(long before, int number,ResponseHandler callback) {
		if (prepare(callback)) {
			SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
			if(sessionInfo==null){
				return false;
			}
			List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
			String action = String.format("users/%s/pointshistory?before=%d&n=%d",sessionInfo.getUserId() ,before,number);
			return GMHttpClient.get(headers, callback, createUserUrl(action));
		}
		return false;
	}

    /**
     * 获取礼包列表
     *
	 * @param start
	 * @param maxExchangeCoupon
	 *@param callback  @return
     */
    public static boolean requestCouponsList(long start, int maxExchangeCoupon, ResponseHandler callback) {
        if(prepare(callback)) {
            URL url = createUrl("/api/app/v210/coupons");
            return GMHttpClient.get(callback, url);
        }

        return false;
    }
	
	
	public static boolean requestGameList(ResponseHandler callback){
		
		if(prepare(callback)){
			List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
			String action = "games";
			return GMHttpClient.get(headers, callback, createUserUrl(action));
		}
		
		return false;
	}

    /**
     * 兑换礼包
     * @param couponId
     * @param callback
     * @return
     */
    public static boolean requestExchangeCoupon(String couponId, ResponseHandler callback) {
        if(prepare(callback)) {
            List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
            String path = String.format("v210/users/coupons/%s", couponId);
            return GMHttpClient.post(headers, callback, createUserUrl(path), null);
        }
        return false;
    }

    /**
     * 获取用户可兑换礼包列表
     * @param callback
     * @return
     */
    public static boolean requestUserCouponList(long before, int number, ResponseHandler callback) {
        if(prepare(callback)) {
            SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
            if(sessionInfo==null){
                return false;
            }

            List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
            return GMHttpClient.get(headers, callback, createUserUrl("v210/users/coupons"));
        }

        return false;
    }
    /**
     * 获取兑换历史
     * @param before
     * @param number
     * @param callback
     * @return
     */
    public static boolean requestExchangeHistory(long before, int number,ResponseHandler callback) {
        if (prepare(callback)) {
            SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
            if(sessionInfo==null){
                return false;
            }
            List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
            String action = String.format("v210/users/exchangehistory?before=%d&n=%d", before,number);
            return GMHttpClient.get(headers, callback, createUserUrl(action));
        }
        return false;
    }
    
    /**
     * 读取
     * @param callback
     * @return
     */
    public static boolean requestReadExchangeFlow(ResponseHandler callback) {
        if(prepare(callback)) {
            List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
            return GMHttpClient.get(headers, callback, createUrl("/api/app/v210/users/stream"));
        }
        return false;
    }
    
    public static boolean requestProducts(ResponseHandler callback) {
        if(prepare(callback)) {
            List<RequestProperty> headers = XwsseRequestPropertiesCreater.create(true);
			return GMHttpClient.get(headers, callback,createUrlForProduct("products"));
		}
        return false;
    }
    
    private static URL createUrlForProduct(String path){
		String base_url = UrlConfig.instance.getServerType().equals(UrlConfig.ServerType.TEST)?
				"http://test-api.xunyou.mobi/api/v1/android/": "http://api.xunyou.mobi/api/v1/android/" ;
		URL url = createUrl(base_url,path);
		return  url ;
    }

	//return createUrl(" http://122.224.73.168:2001", path);
	//return GMHttpClient.get(headers, callback,createUrlForProductTest("/api/v1/android/product") );
}
