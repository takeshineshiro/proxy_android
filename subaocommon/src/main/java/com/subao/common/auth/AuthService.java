package com.subao.common.auth;

import android.text.TextUtils;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.data.Defines;
import com.subao.common.data.ServiceLocation;
import com.subao.common.net.HttpClient;
import com.subao.common.net.RequestProperty;
import com.subao.common.net.ResponseCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * SDK底层交付服务 Created by hujd on 16-7-1.
 */
class AuthService {

	private static AuthService instance;

	/**
	 * 包含协议名和主机名以及基础路径，但不包括URL里的版本号，以'/'结尾
	 */
	private String urlBase;

	/**
	 * 客户端类型，如果是SDK则填写GUID，如果是APP则填写"android"
	 */
	private final String clientType;

    /**
     * 是否使用HTTP协议
     */
    private boolean useHttpProtocol;

    private final ServiceLocation serviceLocation;

    /**
     * 初始化
     *
     * @param serviceLocation 服务资源位置
     * @param clientType      客户端类型，如果是SDK则填写GUID，如果是APP则填写"android"
     */
    static void init(ServiceLocation serviceLocation, String clientType) {
        instance = new AuthService(serviceLocation, clientType);
    }

    /**
     * 创建AuthService实例
     *
     * @param serviceLocation 服务资源位置
     * @param clientType      客户端类型，如果是SDK则填写GUID，如果是APP则填写"android"
     */
    private AuthService(ServiceLocation serviceLocation, String clientType) {
        this.serviceLocation = serviceLocation;
        this.clientType = TextUtils.isEmpty(clientType) ? Defines.REQUEST_CLIENT_TYPE_FOR_APP : clientType;
        buildBaseUrl();
    }

    private void buildBaseUrl() {
        StringBuilder sb = new StringBuilder(512);
        String protocol;
        if (useHttpProtocol) {
            protocol = "http";
        } else if (this.serviceLocation == null) {
            protocol = "https";
        } else {
            protocol = this.serviceLocation.protocol;
        }
        sb.append(protocol).append("://");
        if (this.serviceLocation == null) {
            sb.append("api.xunyou.mobi");
        } else {
            sb.append(this.serviceLocation.host);
            if (this.serviceLocation.port > 0) {
                sb.append(':').append(serviceLocation.port);
            }
        }
        sb.append("/api/");
        urlBase = sb.toString();
    }

	static String getUrlBase(int version) {
		return instance.urlBase + "v" + version + "/";
	}

    /**
     * 设置使用http还是https协议
     *
     * @param useHttpProtocol true表示使用http协议，false表示使用https协议
     */
    public static void setProtocol(boolean useHttpProtocol) {
        if (instance.useHttpProtocol != useHttpProtocol) {
            instance.useHttpProtocol = useHttpProtocol;
            instance.buildBaseUrl();
        }
    }

	/**
	 * 返回客户端类型（gameGuid或"android"）
	 *
	 * @return gameGuid或"android"
	 */
	public static String getClientType() {
		return AuthService.instance.clientType;
	}

	public static class JWTTokenParams {
		public final String uid;
		public final String token;

		/**
		 * 客户端类型，如果是SDK填GUID，是APP填"android"
		 */
		public final String clientType;
		public final ResponseCallback mCallback;
		public final String appId;

		public JWTTokenParams(String uid, String token, String clientType, String appId, ResponseCallback callback) {
			this.mCallback = callback;
			this.uid = uid;
			this.token = token;
			this.clientType = clientType;
			this.appId = appId;
		}
	}

	/**
	 * 获取Token
	 */
	@SuppressWarnings("StringBufferReplaceableByString")
    public static void getToken(String ip, String authorization, ResponseCallback responseCallback) {
		StringBuilder urlBuilder = new StringBuilder(1024);
		urlBuilder.append("https://").append(ip).append(":801/api/v1/").append(instance.clientType).append("/token");
		String url = urlBuilder.toString();
		List<RequestProperty> headers = buildHttpHeader(authorization);
		HttpClient.get(headers, responseCallback, url);
	}

	private static List<RequestProperty> buildHttpHeader(String authorization) {
		List<RequestProperty> headers = new ArrayList<RequestProperty>(1);
		headers.add(new RequestProperty("Authorization", "Bearer " + authorization));
		return headers;
	}

	/**
	 * 获取JWT token
	 */
	public static void getJWTToken(JWTTokenParams params) {
		String url = getUrlBase(1) + params.clientType + "/sessions?grant_type=client_credentials";
		byte[] bytes = serializeToBytes(new JWTTokenReq(params.uid, params.token, params.appId));
		HttpClient.post(null, params.mCallback, url, bytes);
	}

	static byte[] serializeToBytes(JsonSerializable obj) {
		ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(output));
		try {
			obj.serialize(writer);
		} catch (IOException e) {
			return null;
		} finally {
			com.subao.common.Misc.close(writer);
		}
		return output.toByteArray();
	}

	/**
	 * 获取用户加速状态
	 */
	@SuppressWarnings("StringBufferReplaceableByString")
    public static void getUserAccelStatus(String userId, String jwtToken, ResponseCallback callback) {
		StringBuilder urlBuilder = new StringBuilder(1024);
		urlBuilder.append(getUrlBase(1)).append(instance.clientType).append("/accounts/").append(userId);
		String url = urlBuilder.toString();
		List<RequestProperty> headers = buildHttpHeader(jwtToken);
		HttpClient.get(headers, callback, url);
	}

    private enum ConfigAction {
        GET, POST
    }

	private static String buildUrlForConfig(String userId, String clientVersion, ConfigAction configAction) {
		StringBuilder urlBuilder = new StringBuilder(1024);
		urlBuilder.append(getUrlBase(2)).append(instance.clientType).append("/users/").append(userId).append("/configs");
        if (configAction == ConfigAction.POST) {
            urlBuilder.append("/userConfig");
        } else {
            urlBuilder.append("?clientVersion=").append(clientVersion);
        }
		return urlBuilder.toString();
	}

    /**
     * 获取用户配置和服务配置
     *
     * @param jwtToken      JWTToken，不能为NULL
     * @param userId        用户Id（对于腾讯游戏来说就是openId）
     * @param clientVersion 客户端版本号
     * @param callback      回调，不能为null
     * @see ResponseCallback
     */
    public static void getConfig(String jwtToken, String userId, String clientVersion, ResponseCallback callback) {
        String url = buildUrlForConfig(userId, clientVersion, ConfigAction.GET);
        List<RequestProperty> headers = buildHttpHeader(jwtToken);
        HttpClient.get(headers, callback, url);
    }

	/**
	 * 设置用户配置
	 *
	 * @param jwtToken
	 *            JWTToken，不能为NULL
	 * @param userId
	 *            用户Id（对于腾讯游戏来说就是openId）
	 * @param config
	 *            用户配置，不能为null
	 * @param callback
	 *            回调，不能为null
	 * @see ResponseCallback
	 */
	public static void setUserConfig(String jwtToken, String userId, byte[] config, ResponseCallback callback) {
		String url = buildUrlForConfig(userId, null, ConfigAction.POST);
		List<RequestProperty> headers = buildHttpHeader(jwtToken);
		HttpClient.post(headers, callback, url, config);
	}
}
