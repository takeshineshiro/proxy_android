package cn.wsds.gamemaster.pay;

import com.subao.net.NetManager;

import cn.wsds.gamemaster.dialog.PayResultDialog;
import cn.wsds.gamemaster.dialog.PayResultWattiingMode;
import cn.wsds.gamemaster.pay.model.ProductDetail;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.user.Identify;

/**
 * 支付相关的view model
 * Created by hujd on 16-8-10.
 */
public class PaymentVM {
	private static ProductDetail product;

	public static void setProduct(ProductDetail product) {
		PaymentVM.product = product;
	}
	/**
	 * 处理支付结果，UI相应进行处理
	 */
	public static void doPayResult(boolean success, boolean isTokenInValid,int extras) {
		PayResultWattiingMode.stop();

		if(success){
			Identify.getInstance().defaultStartCheck();
		}else if(isTokenInValid){
			if(NetManager.getInstance().isConnected()){
				UIUtils.showToast("用户状态更新中，请稍后购买");
			}else{
				UIUtils.showNetDisconnectMessage();
			}
			
			return ;
		}

		PayResultDialog.show(product,success,extras);
	}
	
	public static void reportPayVersionError(){
		PayResultWattiingMode.stop();
		UIUtils.showToast("抱歉，您未安装支付方式所需版本的软件");
	}
	
	/*private static void resumeStoreUIState(){
		Activity currentActivity = ActivityBase.getCurrentActivity();
		if((currentActivity!=null)&&(!currentActivity.isFinishing())){
			if(currentActivity instanceof ActivityVip){
				//TODO
//				((ActivityVip)currentActivity).resumeStoreStates();
			}
		}
	} */
}
