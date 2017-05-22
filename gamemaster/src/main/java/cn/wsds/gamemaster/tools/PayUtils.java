//package cn.wsds.gamemaster.tools;
//
//import java.lang.ref.WeakReference;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import android.app.Activity;
//import android.content.Context;
//import android.os.AsyncTask;
//import cn.wsds.gamemaster.ui.UIUtils;
//import cn.wsds.gamemaster.wxapi.NotInstalledException;
//import cn.wsds.gamemaster.wxapi.WeixinUtils;
//
//import com.alipay.sdk.app.PayTask;
//import com.tencent.mm.sdk.constants.Build;
//import com.tencent.mm.sdk.modelpay.PayReq;
//import com.tencent.mm.sdk.openapi.IWXAPI;
//
//public class PayUtils {
//
//	public static void payOnWeiXin(Context context ,String orderInfo){
//		try {		
//			  IWXAPI wxApi = WeixinUtils.createWXApi(context);
//			  if(wxApi.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT){
//				JSONObject json;
//				PayReq req = null ;
//				try {
//					json = new JSONObject(orderInfo);
//					req = new PayReq();
//					req.appId			= json.getString("appid");
//					req.partnerId		= json.getString("partnerid");
//					req.prepayId		= json.getString("prepayid");
//					req.nonceStr		= json.getString("noncestr");
//					req.timeStamp		= json.getString("timestamp");
//					req.packageValue	= json.getString("package");
//					req.sign			= json.getString("sign");
//					req.extData			= "app data"; // optional
//				} catch (JSONException e) {					 
//					UIUtils.showToast(e.getMessage());
//				}
//				
//				if(req==null){
//					UIUtils.showToast("对不起，服务器出错，请稍候重试！");
//					return ;
//				}
//				wxApi.sendReq(req);
//			}else{
//				UIUtils.showToast("亲，您当前的微信版本过低，还不能支持支付功能哦！");
//			}
//		} catch (NotInstalledException e) {
//			UIUtils.showToast(e.getMessage());
//		}
//	}
//	
//	public static boolean payOnAli(Activity activity , String orderInfo , AliPayListener listener){				
//	    return AliPayRequestor.createAndExecute(activity,orderInfo,listener) ;
//	}
//	
//	private static final class AliPayRequestor extends AsyncTask<String, Void, String>{
//
//		private final WeakReference<Activity> ref ;
//		private AliPayListener listener ;
//		
//		public static boolean createAndExecute(Activity activity , String orderInfo , AliPayListener listener){
//			if ((activity == null)||(activity.isFinishing())) {
//	            throw new IllegalArgumentException("activity must not be null or finishing");
//	        }
//			
//			if((orderInfo == null)||(orderInfo.isEmpty())){
//				throw new IllegalArgumentException("orderInfo must not be null or empty");
//			}
//			
//			if (listener == null) {
//				throw new IllegalArgumentException("listener must not be null.");
//			}
//			
//			AliPayRequestor task = new AliPayRequestor(activity , listener);
//			task.execute(orderInfo);
//			
//			return true ;
//		}
//		
//		private AliPayRequestor(Activity activity , AliPayListener listener){
//			ref = new WeakReference<Activity>(activity) ;
//			this.listener = listener ;
//		}
//			
//		@Override
//		protected String doInBackground(String... params) {
//			 
//			if(ref ==null){
//				return null ;
//			}
//			
//			Activity activity =  ref.get();
//			if((activity==null)||(activity.isFinishing())){
//				return null ;
//			}
//			
//			PayTask alipay = new PayTask(activity);
//			// 调用支付接口，获取支付结果
//			String result = alipay.pay(params[0], true);
//			
//			return result;
//		}
//
//		@Override
//		protected void onPostExecute(String result) {			 
//			if(listener!=null){
//				listener.onComplete(result);
//				listener = null ;
//			}
//		}
//	}
//	
//	public interface AliPayListener{
//		public void onComplete(String result);
//	}
//
//}
