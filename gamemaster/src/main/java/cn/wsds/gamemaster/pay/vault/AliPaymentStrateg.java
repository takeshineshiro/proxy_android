package cn.wsds.gamemaster.pay.vault;

import cn.wsds.gamemaster.pay.AliPay.AliPay;
import cn.wsds.gamemaster.pay.AliPay.AliPay.AliPayResultCallBack;
import cn.wsds.gamemaster.pay.model.AliPayOrdersResp;
import cn.wsds.gamemaster.pay.model.PayOrdersResp;

public class AliPaymentStrateg extends PaymentStrateg {


	@Override
	protected PayOrdersResp deSerialer(byte[] bytes) {
		return AliPayOrdersResp.deSerialer(new String(bytes));
	}

	@Override
	public void sendPay(PayOrdersResp payOrdersResp) {
		final AliPayOrdersResp aliPayOrderResp = (AliPayOrdersResp) payOrdersResp;
		AliPayResultCallBack payResultCallback = new AliPayResultCallBack(){

			@Override
			public void onSuccess() {
				getPayStatus(aliPayOrderResp.getOrderId(),aliPayOrderResp,null);
			}

			@Override
			public void onError(int error_code ) {
				onPayError(error_code);
			}

			@Override
			public void onCancel(boolean isActivityStoreValid) {
				sendCancel(aliPayOrderResp.orderId,isActivityStoreValid);
			}
		};
		
		AliPay.payOnAli(aliPayOrderResp.getOrderInfo(), payResultCallback);
	}
}
