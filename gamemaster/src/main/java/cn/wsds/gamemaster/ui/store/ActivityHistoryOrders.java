package cn.wsds.gamemaster.ui.store;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.common.msg.MessageEvent;
import com.subao.common.net.ResponseCallback;

import java.lang.ref.WeakReference;
import java.util.List;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.DataCache;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.pay.PaymentExecutor;
import cn.wsds.gamemaster.pay.model.OrderDetail;
import cn.wsds.gamemaster.pay.model.OrderDetails;
import cn.wsds.gamemaster.ui.ActivityListRefreshBase;

public class ActivityHistoryOrders extends ActivityListRefreshBase<OrderDetail> {

	private  static final  MessageEvent.Reporter reporter = new MessageEvent.Reporter() {

		@Override
		public void reportEvent(String s, String s1) {

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new AdapterHistoryOrderList(this));

		List<OrderDetail> cacheData = DataCache.getOrderHistoryCache().getCacheData();
		if(cacheData.isEmpty()) {
			doRefresh();
		} else {
			setData(cacheData);
		}
	}

	@Override
	protected LoadMoreRequestor<OrderDetail> createLoadMoreRequestor() {
		return new OrderHistoryMore(this);
	}

	@Override
	protected RefreshRequestor<OrderDetail> createRefreshRequestor() {
		return new RefreshOrderHistory(this);
	}

	@Override
	protected int getLayoutResID() {
		return R.layout.activity_history_orders;
	}

	@Override
	protected int getEmptyRefreshTextRes() {
		return R.string.order_hirstory_empty;
	}

	private static class OrderHistoryMore extends LoadMoreRequestor<OrderDetail>{

		private final class LoadMoreCallback extends ResponseCallback{

			private WeakReference<OrderHistoryMore> refRequest ;

			public LoadMoreCallback(MessageEvent.Reporter eventReporter) {
				super(eventReporter, 0);
			}

			@Override
			protected String getEventName() {
				return null;
			}

			@Override
			protected void onSuccess(int i, byte[] bytes) {
				OrderHistoryMore ref = refRequest.get() ;
				if (ref==null){
					return;
				}
				ref.onSuccess(new Response(null, bytes, i));
			}

			@Override
			protected void onFail(int i, byte[] bytes) {
				onFailure();
			}

			private void setRequest(OrderHistoryMore orderHistoryMore){
				refRequest = new WeakReference<>(orderHistoryMore);
			}
		}

		private final LoadMoreCallback resCallback = new LoadMoreCallback(reporter);

		public OrderHistoryMore(ActivityHistoryOrders activity) {
			super(activity);
		}

		@Override
		protected boolean doRequest() {
			ActivityListRefreshBase<OrderDetail> ref = activityRef.get();
			if(ref==null){
				return false;
			}
			int dataSize = ref.getListAdapter().getDataSize();
			long before;
			if(dataSize == 0){
				before = 0;
			}else{
				before = ref.getListAdapter().getItemId(dataSize - 1);
				if(before < 0){
					return false;
				}
			}

			resCallback.setRequest(this);
			return PaymentExecutor.getHistoryOrders(UserSession.getInstance().getUserId(),
					(int) before, DEFAULT_LIST_COUNT, resCallback);
		}

		@Override
		protected void setData(byte[] body) throws InvalidProtocolBufferException {
			//parseData(body, activityRef, this);
			List<OrderDetail> orderList = parseData(body);
			if (orderList == null) {
				onParseFailure();
				return;
			}
			ActivityListRefreshBase<OrderDetail> ref = activityRef.get();
			if(ref != null) {
				ref.getListAdapter().addData(orderList);
			}
		}
	}

	private static class RefreshOrderHistory extends RefreshRequestor<OrderDetail> {

		private final class RefreshCallback extends ResponseCallback{

			private WeakReference<RefreshOrderHistory> refRequest ;

			public RefreshCallback(MessageEvent.Reporter eventReporter) {
				super(eventReporter, 0);
			}

			@Override
			protected String getEventName() {
				return null;
			}

			@Override
			protected void onSuccess(int i, byte[] bytes) {
				RefreshOrderHistory ref = refRequest.get() ;
				if (ref==null){
					return;
				}
				ref.onSuccess(new Response(null, bytes, i));
			}

			@Override
			protected void onFail(int i, byte[] bytes) {
				onFailure();
			}

			private void setRequest(RefreshOrderHistory refreshOrderHistory){
				refRequest = new WeakReference<>(refreshOrderHistory);
			}
		}

		private final RefreshCallback resCallback = new RefreshCallback(reporter);

		public RefreshOrderHistory(ActivityHistoryOrders activity) {
			super(activity);
		}

		@Override
		protected void clearCache() {
			DataCache.getOrderHistoryCache().clear();
		}

		@Override
		protected boolean doRequest() {
			resCallback.setRequest(this);
			return PaymentExecutor.getHistoryOrders(UserSession.getInstance().getUserId(),
					0, DEFAULT_LIST_COUNT,resCallback);
		}

		@Override
		protected void setData(byte[] body) throws InvalidProtocolBufferException {
			List<OrderDetail> orderList = parseData(body);
			if (orderList == null) {
				onParseFailure();
				return;
			}
			ActivityListRefreshBase<OrderDetail> ref = activityRef.get();
			if(ref != null) {
				ref.setData(orderList);
			}
		}

		@Override
		public void onParseFailure() {
			ActivityHistoryOrders ref =(ActivityHistoryOrders) activityRef.get();
			if(ref != null) {
				ref.setEmptyViewRes(true);;
			}
			super.onParseFailure();
		}
	}

	private static List<OrderDetail> parseData(byte[] body) {
		OrderDetails orderDetails = OrderDetails.deSerialer(new String(body));
		if (orderDetails == null) {
			return null;
		}

		List<OrderDetail> orderList = orderDetails.getOrderList();
		if(orderList == null || orderList.isEmpty()) {
			return null;
		}

		return  orderList ;
	}

	@Override
	protected void onFailure() {
		super.onFailure();
		setEmptyViewRes(false);
	}

	@Override
	protected void onNetworkUnavailable() {
		super.onNetworkUnavailable();
		setEmptyViewRes(false);
		getEmptyTextView().setText(getNetError());
		setRefreshTextVisibility(true);
	}

	@Override
	protected void setEmptyViewVisible(boolean visible, int emptyTextRes) {
		super.setEmptyViewVisible(visible, emptyTextRes);
		if(!visible){
			setRefreshTextVisibility(false);
		}
	}

	private void setEmptyViewRes(boolean success){
		setRefreshTextVisibility(false);
		if(success){
			setEmptyInfo();
		}

		getEmptyTextView().setCompoundDrawables(null,
				getDrawable(success),null,null);
	}

	private Drawable getDrawable(boolean success){

		int resId = R.drawable.ahistorical_page_pic;
		if(!success){
			resId = R.drawable.no_network_page_pic;
		}

		Drawable drawable = getResources().getDrawable(resId);
		if(drawable!=null){
			drawable.setBounds(0, 0, drawable.getMinimumWidth(),
					drawable.getMinimumHeight());
		}

		return drawable;
	}

	private String getNetError(){
		return  "  网络请求失败\n请检测您的网络";
	}

	private void setEmptyInfo(){
		getEmptyTextView().setText(getEmptyRefreshTextRes());
	}

	private void setRefreshTextVisibility(boolean needShow){
		int visibility = needShow? View.VISIBLE:View.GONE;
		findViewById(R.id.text_refresh_load).setVisibility(visibility);
	}

	private TextView getEmptyTextView(){
		return ((TextView) findViewById(R.id.empty_view));
	}
}
