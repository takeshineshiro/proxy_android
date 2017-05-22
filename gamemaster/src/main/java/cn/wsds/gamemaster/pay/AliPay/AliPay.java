package cn.wsds.gamemaster.pay.AliPay;

import android.app.Activity;
import android.os.AsyncTask;

import com.alipay.sdk.app.PayTask;

import cn.wsds.gamemaster.pay.PaymentExecutor;
import cn.wsds.gamemaster.pay.vault.PayApiService.PayFailureType;
import cn.wsds.gamemaster.ui.ActivityBase;
import cn.wsds.gamemaster.ui.UIUtils;

public class AliPay {
	 
	private static final boolean ISDEBUG = false ;
	private static final String BACKTOCANCEL = "BackToCancel" ;
	
	public interface AliPayResultCallBack {
		void onSuccess(); //支付成功
		void onError(int error_code);   //支付失败
		void onCancel(boolean isActivityStoreValid);    //支付取消
	}
	
	public static boolean payOnAli(String orderInfo , AliPayResultCallBack resultCallback){
		Activity currentActivity = ActivityBase.getCurrentActivity();	
	    return AliPayRequestor.createAndExecute(currentActivity,orderInfo,resultCallback) ;
	}
	
	private static final class AliPayRequestor extends AsyncTask<String, Void, String>{

		private AliPayResultCallBack resultCallback ;
		private final PayTask alipay;
		
		private static boolean createAndExecute(Activity activity , String orderInfo , AliPayResultCallBack resultCallback){	
			
			if(PaymentExecutor.isCancel()){
				if(ISDEBUG){
					UIUtils.showToast("activity must be ActivityStore and not null or finishing");
				}	  
				
				if(resultCallback!=null){
					resultCallback.onCancel(false);
				}
				
				return false;
			}
			
			if((orderInfo == null)||(orderInfo.isEmpty())){
				if(ISDEBUG){
					UIUtils.showToast("orderInfo must not be null or empty");
				}				
				return false ;
			}
			
			if (resultCallback == null) {
				if(ISDEBUG){
					UIUtils.showToast("listener must not be null.");
				}			
				return false ;
			}
			
			AliPayRequestor task = new AliPayRequestor(activity , resultCallback);
			task.execute(orderInfo);
			
			return true ;
		}
		
		private AliPayRequestor(Activity activity , AliPayResultCallBack resultCallback){
			this.resultCallback = resultCallback ;
			alipay = new PayTask(activity);
		}
			
		@Override
		protected String doInBackground(String... params) {
			if(PaymentExecutor.isCancel()){
				return BACKTOCANCEL ;
			}

			return alipay.pay(params[0], true);
		}

		@Override
		protected void onPostExecute(String result) {	
			checkeResult(result);
		}
		 	
		private  void checkeResult(String result){		 
			if(resultCallback==null){
				return;
			}
			
			if(BACKTOCANCEL.equals(result)){
				resultCallback.onCancel(false);
			}
			
			AliPayResult payResult = new AliPayResult(result);
			String resultStatus = payResult.getResultStatus();
			
			int resultCode = 0;
			try{
				resultCode = Integer.parseInt(resultStatus);
			}catch(NumberFormatException e){
				e.printStackTrace();
			}
			
			//“9000”则代表支付成功,
			//"8000"代表等待支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
			//"6001"代表用户主动取消支付；
			//其他值就可以判断为支付失败,如系统返回的错误
			
			
			switch(resultCode){
			case 9000:
				resultCallback.onSuccess();
				break;
			case 8000:
				break;
			case 6001:
				resultCallback.onCancel(true);
				break;
			case 4000:
				resultCallback.onError(PayFailureType.PAY_APP_VERSION_ERROR);
				break;
			default:
				resultCallback.onError(PayFailureType.PAY_FAILUTE_SERVIE_RESPONSE);
				break;
			}	 
		}
	}
	
	
	
}
