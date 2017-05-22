package cn.wsds.gamemaster.pay.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 * Created by hujd on 16-8-4.
 */
public class OrdersResp {

	/**
	 * orderId : 00000000-858a-eb92-ebd0-811a554fd200
	 */

	private final String orderId;

	public OrdersResp(String orderId) {
		this.orderId = orderId;
	}

	public String getOrderId() {
		return orderId;
	}

	public static OrdersResp deSerialer(String jsonStr) {
		try {
			return new Gson().fromJson(jsonStr, OrdersResp.class);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}
