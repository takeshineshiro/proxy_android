package cn.wsds.gamemaster.pay.vault;

import com.subao.common.net.HttpClient;
import com.subao.common.net.RequestProperty;
import com.subao.common.net.ResponseCallback;
import com.subao.utils.UrlConfig;

import java.util.ArrayList;
import java.util.List;

import cn.wsds.gamemaster.pay.model.OrdersReq;
import cn.wsds.gamemaster.pay.model.PayOrdersReq;

/**
 * 支付Api相关服务
 * Created by hujd on 16-8-4.
 */
public class PayApiService {

	public static final class PayType {
		public static final int PAY_TYPE_ALIPAY = 1;
		public static final int PAY_TYPE_WEIXIN = 4;
	}

	public static final class PayStatus {
		public static final int PAY_STATUS_CREATED = 1; //代表订单已创建
		public static final int PAY_STATUS_PAYING = 2; //代表付款中
		public static final int PAY_STATUS_PAY_SUCCESS = 3; //代表交易成功
		public static final int PAY_STATUS_PAY_FAILED = 4; //代表交易失败
		public static final int PAY_STATUS_REFUNDING = 5;//代表退款中
		public static final int PAY_STATUS_REFUND_SUCCESS = 6; //退款成功
		public static final int PAY_STATUS_REFUND_FAILED = 7; //退款失败
		public static final int PAY_STATUS_USER_CANCEL = 8; //用户已取消
	}
	
	public static final class PayFailureType{
		public static final int PAY_CANCEL_BY_USER = 0;
		public static final int PAY_FAILURE_NET_ERROR = 1;
		public static final int PAY_FAILUTE_SERVIE_RESPONSE = 2;
		public static final int PAY_APP_VERSION_ERROR = 3; 
	}

	//private static final String VAULT_BASE_URL = "https://uat.xunyou.mobi/api/v1/android/";
	public static final String VAULT_BASE_URL = UrlConfig.instance.getServerType().equals(UrlConfig.ServerType.TEST)?
			"https://uat.xunyou.mobi/api/v1/android/":"https://api.xunyou.mobi/api/v1/android/" ;
	/**
	 * 创建订单
	 * @param productId
	 * @param num
	 * @param token
	 * @param callback
	 */
    public static void createOrders(String productId, int num, String token, ResponseCallback callback) {
		if(productId == null || token == null || num <= 0 || callback == null) {
			throw new IllegalArgumentException("param failed");
		}

		String jsaonStr = new OrdersReq(productId, num).serialer();
		String url = VAULT_BASE_URL + "orders";
		HttpClient.post(buildHttpHeader(token), callback, url, jsaonStr.getBytes());
	}

	/**
	 * 创建支付订单
	 * @param orderId
	 * @param token
	 * @param payType
	 * @param callback
	 */
	public static void createPayOrders(String orderId, String token, int payType, ResponseCallback callback) {
		if (orderId == null || token == null || callback == null) {
			throw new IllegalArgumentException("param failed");
		}
		String url = VAULT_BASE_URL + "orders/" + orderId + "/payment";
		HttpClient.post(buildHttpHeader(token), callback, url, new PayOrdersReq(payType).serialer().getBytes());
	}

	/**
	 * 获取制定订单ID详细信息
	 */
	public static void getOrderDetail(String orderId, String token, ResponseCallback callback) {
		if (orderId == null || token == null || callback == null) {
			throw new IllegalArgumentException("param failed");
		}
		String url = VAULT_BASE_URL + "orders/" + orderId;
		HttpClient.get(buildHttpHeader(token), callback, url);
	}

	/**
	 * 获取指定User的所用订单
	 */
	public static void getOrders(String userId, String token, int start ,int number ,
			ResponseCallback callback) {
		if (userId == null || token == null || callback == null) {
			throw new IllegalArgumentException("param failed");
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(VAULT_BASE_URL);
		builder.append("accounts/");
		builder.append(userId);
		builder.append("/orders");
	
        if((start>=0)&&(number>0)){
        	builder.append("?");
        	builder.append("start=");
        	builder.append(start);
        	builder.append("&");
        	builder.append("num=");
        	builder.append(number);
        	builder.append("&type=pay");
		}
        
        String url = builder.toString();
        
		HttpClient.get(buildHttpHeader(token), callback, url);
	}
	
	public static void deleteOrder(String orderId , String token , ResponseCallback callback){
		if (orderId == null || token == null || callback == null) {
			throw new IllegalArgumentException("param failed");
		}
		String url = VAULT_BASE_URL + "orders/" + orderId;
		HttpClient.delete(buildHttpHeader(token), callback, url);
	}
	
	
	private static List<RequestProperty> buildHttpHeader(String authorization) {
		List<RequestProperty> headers = new ArrayList<RequestProperty>(1);

		headers.add(new RequestProperty("Authorization", addPrefix(authorization)));
		return headers;
	}

	private static String addPrefix(String key) {
		return "Bearer " + key;
	}
}
