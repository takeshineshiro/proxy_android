package cn.wsds.gamemaster.pay.vault;

import cn.wsds.gamemaster.pay.model.PayOrdersResp;
import cn.wsds.gamemaster.pay.model.WXPayOrdersResp;
import cn.wsds.gamemaster.pay.weixin.WXPay;


public class WXPaymentStrateg extends PaymentStrateg {

	@Override
	protected PayOrdersResp deSerialer(byte[] bytes) {
		return WXPayOrdersResp.deSerialer(new String(bytes));
	}

	@Override
	public void sendPay(final PayOrdersResp payOrdersResp) {
		final WXPayOrdersResp wxPayOrdersResp = (WXPayOrdersResp) payOrdersResp;
		WXPay.WXPayResultCallBack callBack = new WXPay.WXPayResultCallBack() {
			@Override
			public void onSuccess() {
				getPayStatus(wxPayOrdersResp.getOrderId(),wxPayOrdersResp,null);
			}

			@Override
			public void onError(int error_code) {
				onPayError(error_code);
			}

			@Override 
			public void onCancel(boolean isActivityStoreValid) {
				sendCancel(wxPayOrdersResp.orderId,isActivityStoreValid);
			}
		};
		
		WXPay.getInstance().doPay(wxPayOrdersResp, callBack);
	}
}
