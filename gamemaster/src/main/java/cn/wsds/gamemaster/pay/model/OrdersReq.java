package cn.wsds.gamemaster.pay.model;

import com.google.gson.Gson;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 * Created by hujd on 16-8-4.
 */
public class OrdersReq {

	/**
	 * productId : 11111100-858a-eb92-ebd0-811a554fd200
	 * num : 2
	 */

	public final String productId;
	public final int num;

	public OrdersReq(String productId, int num) {
		this.productId = productId;
		this.num = num;
	}

	/**
	 * 序列化
	 */
	public String serialer() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
