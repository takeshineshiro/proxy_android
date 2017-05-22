package cn.wsds.gamemaster.pay.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 * Created by hujd on 16-8-4.
 */
public class OrderDetails {
	/**
	 * orderId : 00000000-858a-eb92-ebd0-811a554fd200
	 * tradeNo : 2016042021001001860234193355
	 * productId : 11111100-858a-eb92-ebd0-811a554fd200
	 * productName : 包月
	 * description : 30天游戏加速
	 * totalFee : 20
	 * payType : 1
	 * status : 1
	 * createTime : 2016-04-20 20:28:00
	 * paidTime : 2016-04-20 20:30:00
	 * dealedTime : 2016-04-22 20:28:00
	 * refundTime : 2016-04-22 21:28:00
	 */

	private final List<OrderDetail> orderList;

	public OrderDetails(List<OrderDetail> orderList) {
		this.orderList = orderList;
	}

	public List<OrderDetail> getOrderList() {
		return orderList;
	}

	public static OrderDetails deSerialer(String jsonStr) {
		try {
			return new Gson().fromJson(jsonStr, OrderDetails.class);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}
