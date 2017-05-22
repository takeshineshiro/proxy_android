package cn.wsds.gamemaster.pay;

import android.content.Context;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.msg.MessageEvent;
import com.subao.common.net.ResponseCallback;

import cn.wsds.gamemaster.pay.model.OrdersResp;
import cn.wsds.gamemaster.pay.vault.AliPaymentStrateg;
import cn.wsds.gamemaster.pay.vault.PayApiService;
import cn.wsds.gamemaster.pay.vault.PaymentStrateg;
import cn.wsds.gamemaster.pay.vault.WXPaymentStrateg;
import cn.wsds.gamemaster.pay.weixin.WXPay;
import cn.wsds.gamemaster.social.AppId;
import cn.wsds.gamemaster.ui.store.ActivityVip;
import cn.wsds.gamemaster.ui.user.Identify;

/**
 * 支付委托层
 * Created by hujd on 16-8-9.
 */
public class PaymentExecutor {
	private final int payType;
	private PaymentStrateg paymentStrateg;
	private static boolean ISCANCEL;

	public PaymentExecutor(Context context, int payType) {
		this.payType = payType;
		ISCANCEL = false ;
		
		if(payType == PayApiService.PayType.PAY_TYPE_ALIPAY) {
			paymentStrateg = new AliPaymentStrateg();
		} else if (payType == PayApiService.PayType.PAY_TYPE_WEIXIN) {
			paymentStrateg = new WXPaymentStrateg();
			//向微信注册
			WXPay.init(context, AppId.WEIXIN_APP_ID);
		} else {
			throw  new IllegalArgumentException("parameters failed");
		}
	}

	/**
	 * 支付
	 */
	public void doPay(String products, int num) {

		MessageEvent.Reporter reportor = new MessageEvent.Reporter() {
			@Override
			public void reportEvent(String s, String s1) {

			}
		};

		//TODO for cid
		ResponseCallback callback = new ResponseCallback(reportor,0) {

			@Override
			protected String getEventName() {
				return null;
			}

			@Override
			protected void onSuccess(int i, byte[] bytes) {
				success(bytes);
			}

			private void success(byte[] bytes) {		
				OrdersResp ordersResp = OrdersResp.deSerialer(new String(bytes));
				if(ordersResp != null) {				
					if(ISCANCEL){
						doCancel(ordersResp.getOrderId());						
						return ;
					}
					
					String jwtToken = Identify.getInstance().getJWTToken();
					if(jwtToken == null) {
						onFail(-1, bytes);
						return;
					}
					
					createPayOrders(ordersResp.getOrderId(), payType, jwtToken);
				} else {				 
					onFail(-1,bytes);					 		
				}
			}

			@Override
			protected void onFail(int i,byte[] arg1) {
				if(!ISCANCEL){
					PaymentVM.doPayResult(false,false,0);
				}
			}
		};

		String jwtToken = Identify.getInstance().getJWTToken();
		if(jwtToken != null) {
			PayApiService.createOrders(products, num, jwtToken, callback);
		} else {
			PaymentVM.doPayResult(false,true,0);
		}
	}

	/**
	 * 创建支付订单
	 */
	private void createPayOrders(final String orderId, final int payType, String jwtToken) {
		MessageEvent.Reporter reportor = new MessageEvent.Reporter() {
			@Override
			public void reportEvent(String s, String s1) {

			}
		};

		//TODO for cid
		ResponseCallback callback = new ResponseCallback(reportor,0) {

			@Override
			protected String getEventName() {
				return null;
			}

			@Override
			protected void onSuccess(int i, byte[] bytes) {
				Logger.d(LogTag.PAY, "onSuccess() called with: " + "i = [" + i + "]");
				if(ISCANCEL){
					doCancel(orderId);
					return;
				}
				
				paymentStrateg.doPay(bytes);
			}

			@Override
			protected void onFail(int i,byte[] arg1) {
				if(!ISCANCEL){
					PaymentVM.doPayResult(false, false,0);
				}	
			}
		};
		
		PayApiService.createPayOrders(orderId, jwtToken, payType, callback);
	}
	
	public static boolean getHistoryOrders(String userId ,int start ,int number, ResponseCallback callback){
		String jwtToken = Identify.getInstance().getJWTToken();
		if(jwtToken != null) {
			PayApiService.getOrders(userId, jwtToken,start,number,callback);
			return true ;
		}  
		
		return false ;
	}

	/**
	 * 申请免费试用
	 */
	public static boolean getFreeTrial(String productId ,String jwtToken, ResponseCallback callback) {
		if (jwtToken != null) {
			PayApiService.createOrders(productId, 1, jwtToken, callback);
			return true;
		}
		return false;
	}
	
	public static boolean isCancel(){
		return (ISCANCEL||(!ActivityVip.isActivityStoreValid())) ;
	}

	public static void notifyCancel(){
		ISCANCEL = true ;
	}

	private void doCancel(String orderId){
		if((paymentStrateg!=null)&&(orderId!=null)){
			paymentStrateg.sendCancel(orderId,ActivityVip.isActivityStoreValid());
		}
	}
}
