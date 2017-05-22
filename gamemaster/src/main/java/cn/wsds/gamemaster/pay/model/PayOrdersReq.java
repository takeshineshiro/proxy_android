package cn.wsds.gamemaster.pay.model;

import com.google.gson.Gson;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 * Created by hujd on 16-8-4.
 */
public class PayOrdersReq {

	/**
	 * payType : 1
	 */

	public final int payType;

	public PayOrdersReq(int payType) {
		this.payType = payType;
	}

	public String serialer() {
		return new Gson().toJson(this);
	}


}
