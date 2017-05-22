package cn.wsds.gamemaster.ui.store;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.subao.common.msg.MessageEvent;
import com.subao.common.net.ResponseCallback;
import com.subao.net.NetManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.dialog.PayResultDialog;
import cn.wsds.gamemaster.dialog.PayResultWattiingMode;
import cn.wsds.gamemaster.net.http.DefaultNoUIResponseHandler;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.pay.PaymentExecutor;
import cn.wsds.gamemaster.pay.PaymentVM;
import cn.wsds.gamemaster.pay.model.OrdersResp;
import cn.wsds.gamemaster.pay.model.ProductDetail;
import cn.wsds.gamemaster.pay.model.Products;
import cn.wsds.gamemaster.pay.vault.PayApiService;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.ActivityBase;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.exchange.SpannableStringUtils;
import cn.wsds.gamemaster.ui.pullrefresh.PtrSubaoFrameLayout;
import cn.wsds.gamemaster.ui.user.Identify;
import cn.wsds.gamemaster.ui.user.Identify.VIPInfoObserver;
import cn.wsds.gamemaster.ui.user.Identify.VIPStatus;
import cn.wsds.gamemaster.ui.view.InertiaListView;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;

public class ActivityVip extends ActivityBase
                           implements OnClickListener ,
		                   AdapterProductList.OnProductSelectedListener{

	private AdapterProductList adapter ;
	private PtrSubaoFrameLayout swipeRefresher;
	private ListView productListView;
	private View emptyView ;

	private final UserSession.UserSessionObserver userSessionObserver =
			new UserSession.UserSessionObserver() {

		public void onUserInfoChanged(UserInfo info) {
			updateUserInfoView(info);
		}
	};

	private final VIPInfoObserver vipInfoObserver = new VIPInfoObserver(){

		@Override
		public void onVIPInfoChanged(Identify.VIPInfo vipInfo) {
			updateVipTitleView();
			adapter.updateView();
		}
	};

	private static class  ProductResponse extends DefaultNoUIResponseHandler {

		WeakReference<ActivityVip> ref ;
		@Override
		protected void onSuccess(Response response) {

			if(HttpsURLConnection.HTTP_OK == response.code){
				parseData(response.body);
				stopRefresh();
			}else{
				onFailure();
			}
			stopRefresh();
		}

		@Override
		public void onNetworkUnavailable() {
			ActivityVip activityVip = ref.get();
			if(activityVip!=null){
				activityVip.setEmptyView(true);
			}
			stopRefresh();
		}

		@Override
		protected void onFailure() {
			ActivityVip activityVip = ref.get();
			if(activityVip!=null){
				activityVip.showServiceErrorToast();
			}
			stopRefresh();
		}

		protected void stopRefresh(){
			ActivityVip activityVip = ref.get();
			if(activityVip!=null){
				activityVip.swipeRefresher.refreshComplete();
			}
		}

		private void parseData(byte[] data){
			Products products = Products.deSerialer(new String(data));
			if (products == null) {
				return ;
			}

			List<ProductDetail> productList = products.getProductList();
			if(productList == null || productList.isEmpty()) {
				return ;
			}

			ActivityVip activityVip = ref.get();
			if(activityVip!=null){
				activityVip.setData(productList);
			}
		}

		private void setActivity(ActivityVip activity){
			ref = new WeakReference<>(activity);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(R.string.purchase);
		setContentView(R.layout.activity_vip);

		initView();
		requestData();
	}

	@Override
	protected void onStart() {
		super.onStart();
		initHeaderView();
		initViewIntroduce();

		UserSession.getInstance().registerUserSessionObserver(
				userSessionObserver);
		Identify.getInstance().registerObservers(vipInfoObserver);
	}

	@Override
	protected void onStop() {
		super.onStop();

		UserSession.getInstance().unregisterUserSessionObserver(
				userSessionObserver);
		Identify.getInstance().unregisterObserver(vipInfoObserver);
	}

	private void initHeaderView() {
		if(UserSession.isLogined()) {
			initLoginView();
		} else {
			initUnloginView();
		}
	}

	private void initUnloginView() {
		final TextView textLogin = (TextView)findViewById(R.id.text_login);
		textLogin.setClickable(true);
		textLogin.setTextColor(getResources().getColor(R.color.color_game_11));
		SpannableStringBuilder textBuilder = SpannableStringUtils.
				getTextBuilderUnderline(getResources().getString(R.string.login_text));
		textLogin.setText(textBuilder);
		textLogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ActivityUserAccount.open(ActivityVip.this,
						ActivityUserAccount.FRAGMENT_TYPE_LOGIN);
			}
		});

		TextView textUser = (TextView) findViewById(R.id.text_user);
		textUser.setText(R.string.unlogin_text);
	}

	private void initLoginView() {
		UserInfo userInfo = UserSession.getInstance().getUserInfo();
		updateUserInfoView(userInfo);
	}

	private void updateUserInfoView(UserInfo userInfo){
		if(userInfo != null) {
			setUserNameView(userInfo);
			setLoginView();
		}else{
			initUnloginView();
		}
	}

	private void setLoginView() {
		TextView textLogin = (TextView)findViewById(R.id.text_login);
		textLogin.setVisibility(View.GONE);
	}

	private void setUserNameView(UserInfo userInfo) {
		TextView textUser = (TextView) findViewById(R.id.text_user);
		textUser.setText("用户：" + userInfo.getUserName());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_store, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.transaction_history:
				if(UserSession.isLogined()) {
                    UIUtils.turnActivity(this, ActivityHistoryOrders.class);
				} else {
                    UIUtils.showReloginDialog(this, "查看历史订单，需要登录！",
							null);
                }
				break;
			default:
				break;
        }
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
									Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {

	}

	private void initView(){
		//initHeaderView();
		initViewProducts();
		initRefresherView();
	}

	private void initViewIntroduce() {
		//TODO 是否是vip
		if(UserSession.isLogined() && UserSession.getInstance().vipStatus()) {
			findViewById(R.id.head_service_introduce).setVisibility(View.GONE);
			findViewById(R.id.head_vip_service_introduce).setVisibility(
					View.VISIBLE);
		} else {
			findViewById(R.id.head_service_introduce).setVisibility(View.VISIBLE);
			findViewById(R.id.head_vip_service_introduce).setVisibility(View.GONE);
		}

		InertiaListView introducelistView = (InertiaListView)
				findViewById(R.id.service_introduce_list);
		AdapterServiceIntroduce adapter = new AdapterServiceIntroduce(this);
		introducelistView.setAdapter(adapter);
		List<AdapterServiceIntroduce.ServiceSupport> list = getServiceSupports();

		adapter.setData(list);
	}

	@NonNull
	private List<AdapterServiceIntroduce.ServiceSupport> getServiceSupports() {
		//TODO
		List<AdapterServiceIntroduce.ServiceSupport> list =
				new ArrayList<AdapterServiceIntroduce.ServiceSupport>();

			list.add(new AdapterServiceIntroduce.ServiceSupport("普通加速",
					R.drawable.purchase_first_accelerated_service_yes_icon,
					R.drawable.purchase_first_accelerated_service_ordinary_icon));
			list.add(new AdapterServiceIntroduce.ServiceSupport("Wifi加速",
					R.drawable.purchase_first_accelerated_service_no_icon,
					R.drawable.purchase_first_accelerated_service_wifi_icon));
			list.add(new AdapterServiceIntroduce.ServiceSupport("4G加速",
					R.drawable.purchase_first_accelerated_service_no_icon,
					R.drawable.purchase_first_accelerated_service_4g_icon));

		return list;
	}

	private void initViewProducts() {
		updateVipTitleView();

		productListView = (ListView)findViewById(
				R.id.list_product);
		adapter = new AdapterProductList(this);
		productListView.setAdapter(adapter);
		emptyView = findViewById(R.id.error_view);

		/*List<ProductDetail> products = Store.getInstance().getPoducts();
		adapter.setData(products);*/
	}

	private void setData(List<ProductDetail> products){
		setEmptyView(false);
		adapter.setData(products);
	}

	private void updateVipTitleView(){
		TextView vip_title = (TextView)findViewById(R.id.text_vip_info);
		initVipTitleView(vip_title);

		VIPStatus status = getVIPStatus();
		if (status==null){
			return;
		}

		switch (status){
			case VIP_FREE:
			case VIP_VALID:
				vip_title.setText(getVIPExpiredInfo(status));
				break;
		}
	}

	private void initVipTitleView(TextView vipTitleView){
		if(vipTitleView==null){
			return;
		}

		vipTitleView.setText("VIP套餐");
	}

	private VIPStatus getVIPStatus(){
		return Identify.getInstance().getVIPStatus();
	}

	private String getVIPExpiredInfo(VIPStatus status){
		String type = "";
        switch (status){
			case VIP_FREE:
				type ="试用";
				break;
			case VIP_VALID:
				type ="套餐";
				break;
			default:
				return type;
		}

		StringBuilder builder = new StringBuilder();
		builder.append(type);
		builder.append("到期时间：");
		builder.append(Identify.getInstance().getExpiredTimeStr());

		return builder.toString();
	}

	private void requestData(){
		initPtrHandler();
		swipeRefresher.postDelayed(new Runnable() {
			@Override
			public void run() {
				swipeRefresher.autoRefresh();
			}
		},100);
	}

	private void initPtrHandler(){
		swipeRefresher.setPtrHandler(new PtrHandler() {
			@Override
			public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
				return PtrDefaultHandler.checkContentCanBePulledDown(frame,content,header);
			}

			@Override
			public void onRefreshBegin(PtrFrameLayout frame) {
				ProductResponse response = new ProductResponse();
				response.setActivity(ActivityVip.this);
				HttpApiService.requestProducts(response);
			}
		});
	}

	private void initRefresherView(){
		swipeRefresher = (PtrSubaoFrameLayout)findViewById(R.id.swipeRefresher);
		swipeRefresher.setRatioOfHeaderHeightToRefresh(0.8f);
	}

	public static boolean isActivityStoreValid() {
		return true;
	}

	@Override
	public void onProductSelected(ProductDetail product) {
		if (product==null){
			return;
		}

		PayResultDialog.reportEvent(product, false);

		if(!UserSession.isLogined()){
			showLoginRemindToast(product.getPrice());
			return;
		}

		PayMode payMode = new PayMode(this,product);
		payMode.action();
	}

	private void showLoginRemindToast(float price){
		String remind = "购买需要先登录哦~";
		if(!needPay(price)){
			remind = "亲，您需要先登录哦~";
		}

		UIUtils.showReloginDialog(this, remind, null);
	}

	private void showServiceErrorToast(){
		UIUtils.showToast("服务器异常，请稍后再试");
	}

	private void setEmptyView(boolean error){
		if(error){
			emptyView.setVisibility(View.VISIBLE);
			productListView.setVisibility(View.GONE);
		}else {
			emptyView.setVisibility(View.GONE);
			productListView.setVisibility(View.VISIBLE);
		}
	}

	private boolean needPay(float price){
		return price>0;
	}

	private class PayMode{
		private final ProductDetail product ;
		private final Context context ;
        private AlertDialog dialog ;
		private final MessageEvent.Reporter reportor =
				new MessageEvent.Reporter() {
			@Override
			public void reportEvent(String s, String s1) {}
		};

		private final ResponseCallback freeTrialCallback =
				new com.subao.common.net.ResponseCallback(reportor, 0) {
			@Override
			protected String getEventName() {
				return null;
			}

			@Override
			protected void onSuccess(int i, byte[] bytes) {
				if(isResponseNormal(bytes)){
					showFreeTrialGotInfo();
					PayResultWattiingMode.stop();
				}else{
					onFail(i,bytes);
				}
			}

			@Override
			protected void onFail(int i, byte[] bytes) {
				showErrorInfo();
				PayResultWattiingMode.stop();
			}

		};

		private final OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				PaymentExecutor payExecutor = null;
				switch (v.getId()) {
					case R.id.layout_pay_Ali:
						payExecutor = new PaymentExecutor(context,
								PayApiService.PayType.PAY_TYPE_ALIPAY);
						Statistic.addEvent(AppMain.getContext(),
								Statistic.Event.VIPCENTER_WAY_ALIPAY_CLICK);
						break;
					case R.id.layout_pay_Weixin:
						payExecutor = new PaymentExecutor(context,
								PayApiService.PayType.PAY_TYPE_WEIXIN);
						Statistic.addEvent(AppMain.getContext(),
								Statistic.Event.VIPCENTER_WAY_WECHAT_CLICK);
						break;
					default:
						break;
				}

				if(payExecutor!=null){
					PaymentVM.setProduct(product);
					payExecutor.doPay(product.getProductId(), 1);
				}

				dialog.dismiss();
				PayResultWattiingMode.start(context, true);
			}
		};

		private PayMode(Context context,ProductDetail product){
			this.context = context ;
			this.product = product ;
		}

		private void action(){
			if(needPay(product.getPrice())){
				payAction();
			}else{
				getFreeTrial();
			}
		}

		private void  payAction(){
			View layout = LayoutInflater.from(context).
					inflate(R.layout.dialog_pay_mode, null);
			layout.findViewById(R.id.layout_pay_Ali).
					setOnClickListener(listener);
			layout.findViewById(R.id.layout_pay_Weixin).
					setOnClickListener(listener);

			dialog = new AlertDialog.Builder(context).setView(layout).create();
			dialog .show();
		}

		private void getFreeTrial(){
			PayResultWattiingMode.start(context,false);
			PaymentExecutor.getFreeTrial(product.getProductId(),
			Identify.getInstance().getJWTToken(),
					freeTrialCallback);
		}

		private boolean isResponseNormal(byte[] bytes){
			return ((bytes!=null)&&
					(OrdersResp.deSerialer(new String(bytes))!=null));
		}

		private void showFreeTrialGotInfo(){
			Identify.defaultStartCheck();
			UIUtils.showToast("开始试用VIP加速服务");
		}

		private void showErrorInfo(){
			if(NetManager.getInstance().isConnected()){
				showServiceErrorToast();
			}else{
				showNetErrorToast();
			}
		}

		private void showNetErrorToast(){
			UIUtils.showToast("网络异常，请稍后再试");
		}
	}



}
