package cn.wsds.gamemaster.pay.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 * Created by hujd on 16-8-10.
 */
public class AliPayOrdersResp extends PayOrdersResp {

	/**
	 * orderId : 00000000-858a-eb92-ebd0-811a554fd200
	 * orderInfo : alipay
	 */

	private final String orderInfo;

	public AliPayOrdersResp(String orderId, String orderInfo) {
		super(orderId);
		this.orderInfo = orderInfo;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getOrderInfo() {
		return orderInfo;
	}

	public static AliPayOrdersResp deSerialer(String jsonStr) {
		try {
			return new Gson().fromJson(jsonStr, AliPayOrdersResp.class);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}
