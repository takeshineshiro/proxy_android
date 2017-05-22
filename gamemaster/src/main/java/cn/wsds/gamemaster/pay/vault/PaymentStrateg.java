package cn.wsds.gamemaster.pay.vault;

import android.os.SystemClock;

import com.subao.common.msg.MessageEvent;
import com.subao.common.net.ResponseCallback;

import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.pay.PaymentVM;
import cn.wsds.gamemaster.pay.model.OrderDetail;
import cn.wsds.gamemaster.pay.model.PayOrdersResp;
import cn.wsds.gamemaster.ui.user.Identify;

/**
 * 支付策略基类
 * Created by hujd on 16-8-9.
 */
public abstract class PaymentStrateg {

	private class OrderStatusChecker extends ResponseCallback{

		private static final int CHECK_DURATION = 5*60*1000;

		private String orderId ;
		private PayOrdersResp payOrdersResp;
		private long firstCheckTime =  -1 ;

		private final Runnable checkStatusRunnable = new Runnable(){
			@Override
			public void run() {
				getPayStatus(orderId,payOrdersResp,OrderStatusChecker.this);
			}
		};

		//TODO for cid
		public OrderStatusChecker(MessageEvent.Reporter eventReporter,int cid) {
			super(eventReporter,cid);
		}

		@Override
		protected String getEventName() {
			return null;
		}

		@Override
		protected void onSuccess(int i, byte[] bytes) {
			OrderDetail orderDetail = OrderDetail.deSerialer(new String(bytes));
			if(orderDetail != null) {
				checkStatus(orderDetail);
			} else {
				onFail(i,bytes);
			}
		}

		@Override
		protected void onFail(int i, byte[] bytes) {

		}

		private void init(String orderId,PayOrdersResp payOrdersResp){
			this.orderId = orderId;
			this.payOrdersResp = payOrdersResp ;
		}

		private void checkStatus(OrderDetail orderDetail) {
			if (orderDetail==null){
				return;
			}

			switch (orderDetail.getStatus()){
				case PayApiService.PayStatus.PAY_STATUS_PAY_SUCCESS:
					stop();
					PaymentVM.doPayResult(true, false,orderDetail.getFreeDays());
					break;
				case PayApiService.PayStatus.PAY_STATUS_PAYING:
					postStatusCheckLooper();
					break;
				default:
					stop();
					PaymentVM.doPayResult(false, false,0);
					break;
			}
		}

		private void postStatusCheckLooper(){ //5分钟内进行支付订单轮询，直到状态不为“付款中”
			long currentTime = SystemClock.elapsedRealtime();

			if(firstCheckTime<0){
				firstCheckTime = currentTime;
			}

			if(currentTime-firstCheckTime <CHECK_DURATION){
				MainHandler.getInstance().postDelayed(checkStatusRunnable, 1000);
			}else{
				stop();
			}
		}

		private void stop(){
			firstCheckTime = -1 ;
		}
	}

	public  void doPay(byte[] bytes) {
		PayOrdersResp payOrdersResp = deSerialer(bytes);
		if(payOrdersResp != null) {
			sendPay(payOrdersResp);
		} else {
			PaymentVM.doPayResult(false, false,0);
		}
	}

	/**
	 * 从服务器获取支付状态
	 */
	protected void getPayStatus(String orderId , PayOrdersResp payOrdersResp ,
								OrderStatusChecker statusCallback) {
		if ((orderId==null)||(payOrdersResp==null)){
			return;
		}

		OrderStatusChecker callback = statusCallback;
		if (callback==null){
			//TODO for cid
			callback = new OrderStatusChecker(getReporter(),0);
			callback.init(orderId, payOrdersResp);
		}

		String jwtToken = Identify.getInstance().getJWTToken();
		if(jwtToken != null) {
			PayApiService.getOrderDetail(orderId, jwtToken, callback);
		} else {
			PaymentVM.doPayResult(false, false,0);
		}
	}
	
	protected abstract PayOrdersResp deSerialer(byte[] bytes);

	public abstract void sendPay(PayOrdersResp payOrdersResp);
	
	public void sendCancel(String orderId ,boolean isUIValid){
		//TODO for cid
		ResponseCallback cancelResultCallback = new ResponseCallback(getReporter(),0){
			@Override
			protected String getEventName() {
				return null;
			}

			@Override
			protected void onFail(int arg0, byte[] arg1) {
			}

			@Override
			protected void onSuccess(int arg0, byte[] arg1) {
			}
		};
		
		PayApiService.deleteOrder(orderId, Identify.getInstance().getJWTToken(),
				cancelResultCallback);

		if(isUIValid){
			PaymentVM.doPayResult(false, false,0);
		}
	}

	protected void onPayError(int error_code){
		if(PayApiService.PayFailureType.PAY_APP_VERSION_ERROR == error_code){
			PaymentVM.reportPayVersionError();
		}else{
			PaymentVM.doPayResult(false, false,0);
		}
	}

	private MessageEvent.Reporter getReporter(){

		return new MessageEvent.Reporter() {
			@Override
			public void reportEvent(String s, String s1) {

			}
		};
	}

}
