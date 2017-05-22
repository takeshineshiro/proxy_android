package cn.wsds.gamemaster.pay.weixin;


import android.content.Context;
import android.support.annotation.Nullable;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.tencent.mm.sdk.constants.Build;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import cn.wsds.gamemaster.pay.PaymentExecutor;
import cn.wsds.gamemaster.pay.model.WXPayOrdersResp;
import cn.wsds.gamemaster.pay.vault.PayApiService.PayFailureType;
import cn.wsds.gamemaster.social.AppId;

/**
 * 微信支付
 * Created by hujd on 16-8-3.
 */
public class WXPay {

	private static WXPay mWXPay;
	private IWXAPI mWXApi;
	private WXPayOrdersResp mPayParam;
	private WXPayResultCallBack mCallback;

	public static final int NO_OR_LOW_WX = -4;   //未安装微信或微信版本过低
	public static final int ERROR_PAY_PARAM = -2;  //支付参数错误
	public static final int ERROR_PAY = -3;  //支付失败

	public interface WXPayResultCallBack {
		void onSuccess(); //支付成功
		void onError(int error_code);   //支付失败
		void onCancel(boolean isActivityStoreValid);    //支付取消
	}

	public WXPay(Context context, String wx_appid) {
		mWXApi = WXAPIFactory.createWXAPI(context, null);
		mWXApi.registerApp(wx_appid);
	}

	public static void init(Context context, String wx_appid) {
		if(mWXPay == null) {
			mWXPay = new WXPay(context, wx_appid);
		}
	}
	
	public static WXPay getInstance(){
		return mWXPay;
	}

	public IWXAPI getWXApi() {
		return mWXApi;
	}
	/**
	 * 发起微信支付
	 */
	public void doPay(WXPayOrdersResp pay_param, WXPayResultCallBack callback) {
		mPayParam = pay_param;
		mCallback = callback;

		if(!check()) {
			if(mCallback != null) {
				mCallback.onError(PayFailureType.PAY_APP_VERSION_ERROR);
			}
			return;
		}
		
		if(PaymentExecutor.isCancel()){
			if(mCallback!=null){
				mCallback.onCancel(false);
			}
			return ;
		}

		PayReq req = createPayReq();
		if (req == null) {
			return;
		}

		Logger.d(LogTag.PAY, "dopay req,  appid: " + req.appId + " partnerId: " + req.partnerId +
				" prepayId: " + req.prepayId + " sign: " + req.sign + " nonceStr: " + req.nonceStr
				+ " timestamp: " + req.timeStamp);
		mWXApi.sendReq(req);
	}

	@Nullable
	private PayReq createPayReq() {

		if(mPayParam == null) {
			if(mCallback != null) {
				mCallback.onError(ERROR_PAY_PARAM);
			}
			return null;
		}

		PayReq req = new PayReq();
		req.appId = AppId.WEIXIN_APP_ID;
		req.partnerId = AppId.WEIXIN_APP_PARTNERID;
		req.prepayId = mPayParam.getPrepayid();
		req.packageValue = AppId.WEIXIN_APP_PACKAGE;
		req.nonceStr = mPayParam.getNoncestr();
		req.timeStamp = mPayParam.getTimestamp();
		req.sign = mPayParam.getSign();
		return req;
	}

	//支付回调响应
	public void onResp(int error_code) {
		if(mCallback == null) {
			return;
		}

		if(error_code == 0) {   //成功
			mCallback.onSuccess();
		} else if(error_code == -1) {   //错误
			mCallback.onError(ERROR_PAY);
		} else if(error_code == -2) {   //取消
			mCallback.onCancel(true);
		}

		mCallback = null;
	}

	//检测是否支持微信支付
	private boolean check() {
		return mWXApi.isWXAppInstalled() && mWXApi.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT;
	}
	
}
