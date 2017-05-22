package cn.wsds.gamemaster.net.http;
import android.os.SystemClock;

import com.google.protobuf.InvalidProtocolBufferException;

import java.net.HttpURLConnection;

import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.service.HttpApiService;
import hr.client.appuser.GetUserInfo.GetUserDetailResponse;

public class UpdateUserInfoRequestor {
	
	/**
	 * 最小更新间隔时间 （毫秒）
	 */
	private final static long MIN_UPDATE_INTERVAL_TIME_MILLIS = 60 * 1000;
	
	public static final UpdateUserInfoRequestor instance = new UpdateUserInfoRequestor();
    
    private long requestTimeMillis = 0;

	private UpdateUserInfoRequestor() {}
	
	/**
	 * 请求刷新用户信息
	 * @param force 是否强制刷新（不考虑缓存过期时间）
	 * @return
	 * 当用户登录并且距上次刷新超过  {@value #MIN_UPDATE_INTERVAL_TIME_MILLIS} 或者用户从未刷新过用户信息返回 true
	 * 否则返回 false
	 */
	public boolean request(boolean force) {
		// 测试用
		if(!GlobalDefines.CLIENT_PRE_CHECK){
			return doRequest();
		}
		
		if (!UserSession.isLogined()) {
            return false;
        }
		
		if (force || requestTimeMillis == 0){
			return doRequest();
		}
		
		if(SystemClock.elapsedRealtime() - requestTimeMillis > MIN_UPDATE_INTERVAL_TIME_MILLIS){
			return doRequest();
		}
		
		return false;
	}

	private boolean doRequest() {
		SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
		if (sessionInfo != null) {
			String userId = sessionInfo.getUserId();
			if (HttpApiService.requestUserDetail(userId, new RequestUserDetailCallback())) {
				requestTimeMillis = SystemClock.elapsedRealtime();
				return true;
			}
		}
		return false;
	}
	
	private static final class RequestUserDetailCallback extends DefaultNoUIResponseHandler {
		
		public RequestUserDetailCallback() {
			super(new DefaultOnHttpUnauthorizedCallBack());
		}

		@Override
		protected void onSuccess(Response response) {
            if (response.body != null && HttpURLConnection.HTTP_OK == response.code) {
                try {
                    GetUserDetailResponse obj = GetUserDetailResponse.parseFrom(response.body);
                    if (0 == obj.getResultCode()) {
                        UserSession.getInstance().updateUserInfoByServerProto(obj.getUserInfo());
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
}
