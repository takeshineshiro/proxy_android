package com.subao.common.auth;

/**
 * 鉴权结果的观察者
 */
public interface AuthResultReceiver {

    void onGetJWTTokenResult(int cid, String jwtToken, long expires, String shortId, int userStatus,
                             String expiredTime, boolean result, int code);

    void onGetUserAccelStatusResult(int cid, String shortId, int status, String expiredTime, boolean result, int code);

    void onGetTokenResult(int cid, String ip, byte[] token, int length, int expires, boolean result, int code);

    /**
     * 当用户配置取得时
     *
     * @param cid      Call Id
     * @param jwtToken JWT-Token
     * @param userId   UserId
     * @param configs  {@link AuthExecutor.Configs}
     * @param code     服务端返回的Response Code
     * @param result   是否成功
     */
    void onGetUserConfigResult(int cid, String jwtToken, String userId, AuthExecutor.Configs configs, int code, boolean result);

}
