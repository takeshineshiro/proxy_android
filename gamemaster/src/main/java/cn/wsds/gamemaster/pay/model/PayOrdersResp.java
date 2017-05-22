package cn.wsds.gamemaster.pay.model;

/**
 * model目录下面的类用来进行序列化和反序列化的，字段名不能修改，也不能混淆
 * Created by hujd on 16-8-10.
 */
public class PayOrdersResp {
	public final String orderId;

	public PayOrdersResp(String orderId) {
		this.orderId = orderId;
	}
}
