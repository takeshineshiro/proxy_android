package cn.wsds.gamemaster.pay.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 * Created by hujd on 16-8-4.
 */
public class WXPayOrdersResp extends PayOrdersResp {

	/**
	 * orderId : 00000000-858a-eb92-ebd0-811a554fd200
	 * prepayid : 123
	 * noncestr : 321
	 * timestamp : 2016-8-8 20:10
	 * sign : abc
	 */
	private final String prepayid;
	private final String noncestr;
	private final String timestamp;
	private final String sign;

	public WXPayOrdersResp(String orderId, String prepayid, String noncestr, String timestamp, String sign) {
		super(orderId);
		this.prepayid = prepayid;
		this.noncestr = noncestr;
		this.timestamp = timestamp;
		this.sign = sign;
	}

	public String getOrderId() {
		return orderId;
	}


	public String getPrepayid() {
		return prepayid;
	}



	public String getNoncestr() {
		return noncestr;
	}


	public String getTimestamp() {
		return timestamp;
	}



	public String getSign() {
		return sign;
	}

	public static WXPayOrdersResp deSerialer(String jsonStr) {
		try {
			return new Gson().fromJson(jsonStr, WXPayOrdersResp.class);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}
