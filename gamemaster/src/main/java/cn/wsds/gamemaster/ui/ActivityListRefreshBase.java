package cn.wsds.gamemaster.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.net.http.DefaultNoUIResponseHandler;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.UIUtils.IOnReloginConfirmListener;
import cn.wsds.gamemaster.ui.adapter.AdapterListRefreshBase;
import cn.wsds.gamemaster.ui.pullrefresh.PtrSubaoFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;

/**
 * 列表刷新基类
 */
public abstract class ActivityListRefreshBase<T> extends ActivityBase {
	protected static final int DEFAULT_LIST_COUNT = 20;
	private PtrSubaoFrameLayout swipeRefresher;
	protected ListView listView;
	private TextView emptyView;
	protected DataRequestor<T> dateLoadMore;
	private DataRequestor<T> dataRefresh;
	protected AdapterListRefreshBase<T> adapter;
	private static final int STATE_NONE = 0;
	private static final int STATE_REFRESH = 1;
	private int mState = STATE_NONE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(this.getTitle());
		setContentView(getLayoutResID());
		initView();
		
		dateLoadMore = createLoadMoreRequestor();
		dataRefresh = createRefreshRequestor();
	}
	
	protected abstract LoadMoreRequestor<T> createLoadMoreRequestor();
	
	protected abstract RefreshRequestor<T> createRefreshRequestor();

	protected abstract int getLayoutResID();
	
	protected void setListAdapter(AdapterListRefreshBase<T> adapter){
		this.adapter = adapter;
		listView.setAdapter(adapter);
	}

	protected void initView(){
		initRefreshView();

		listView = (ListView) findViewById(android.R.id.list);
		listView.setOnScrollListener(OnScrollListener);
		this.emptyView = (TextView) findViewById(R.id.empty_view);
	}

	private void initRefreshView() {
		this.swipeRefresher = (PtrSubaoFrameLayout) findViewById(R.id.swipeRefresher);
//		swipeRefresher.setLoadingMinTime(1000);
		swipeRefresher.setRatioOfHeaderHeightToRefresh(0.8f);
	}

	protected void doRefresh() {
		if(dataRefresh!=null){
			swipeRefresher.postDelayed(new Runnable() {
				@Override
				public void run() {
					swipeRefresher.autoRefresh();
				}
			}, 100);
			setPtrHandler();
		}
	}

	private void setPtrHandler() {
		swipeRefresher.setPtrHandler(new PtrHandler() {
			@Override
			public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
				return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
			}

			@Override
			public void onRefreshBegin(final PtrFrameLayout frame) {
				dataRefresh.request();
			}
		});
	}

	public void setData(List<T> data) {
		setEmptyViewVisible(data == null || data.isEmpty(), getEmptyRefreshTextRes());
		setPtrHandler();
		adapter.setData(data);
	}
	
	protected int getEmptyRefreshTextRes() {
		return R.string.points_hirstory_empty;
	}

	private final OnScrollListener OnScrollListener = new OnScrollListener() {

        private int scrollState;
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			this.scrollState = scrollState;
			if(SCROLL_STATE_TOUCH_SCROLL == scrollState){
				return;
			}
			if (mState == STATE_NONE && isScrollEnd(view)) {
				tryLoad();
			}
		}

		private void tryLoad() {
			if (!adapter.isLoadMore()){
				return;
			}
			if (adapter == null || adapter.getDataSize() == 0) {
				return;
			}
			if (adapter.canLoading()) {
				dateLoadMore.request();
			}
		}

		private boolean isScrollEnd(AbsListView view) {
			// 判断是否滚动到底部
			try {
				View footerView = adapter.getFooterView();
				int positionForView = view.getPositionForView(footerView);
				int lastVisiblePosition = view.getLastVisiblePosition();
				if (positionForView == lastVisiblePosition){
					return true;
				}
			} catch (Exception e) {
				if(adapter.getDataSize() == view.getLastVisiblePosition()){
					return true;
				}
			}
			return false;
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if(SCROLL_STATE_TOUCH_SCROLL != scrollState){
				return;
			}
			if(mState == STATE_NONE && (firstVisibleItem + visibleItemCount == totalItemCount)){
				tryLoad();
			}
		}

	};

	protected static abstract class DataRequestor<T> extends DefaultNoUIResponseHandler {
		
		private static class MyOnReloginConfirmListener implements IOnReloginConfirmListener {
			
			private final WeakReference<Activity> ref;
			
			public MyOnReloginConfirmListener(Activity activity) {
				this.ref = new WeakReference<Activity>(activity);
			}
			
			@Override
			public void onConfirm() {
				Activity activity = ref.get();
				if(activity != null && !activity.isFinishing()){
					activity.finish();
				}
			}
		}
		
		private static final class MyReLoginOnHttpUnauthorizedCallBack extends ReLoginOnHttpUnauthorizedCallBack {

			private DataRequestor<?> dataRequestor;

			private MyReLoginOnHttpUnauthorizedCallBack(Activity context,
					IOnReloginConfirmListener listener) {
				super(context, listener);
			}

			@Override
			public void onHttpUnauthorized(Response response) {
				super.onHttpUnauthorized(response);
				if(dataRequestor!=null){
					dataRequestor.stopAnimation();
				}
			}
			
			public void setDataRequestor(DataRequestor<?> dataRequestor) {
				this.dataRequestor = dataRequestor;
			}
		}
		
		protected static final int MIN_MILLISECONDS_ANIMATION_RUNNING = 800;
		private long animationStartMinlliseconds;
		protected final WeakReference<ActivityListRefreshBase<T>> activityRef;
		public DataRequestor(ActivityListRefreshBase<T> activity) {			
			super(new MyReLoginOnHttpUnauthorizedCallBack(activity,new MyOnReloginConfirmListener(activity)));
			((MyReLoginOnHttpUnauthorizedCallBack) onHttpUnauthorizedCallBack).setDataRequestor(this);
			activityRef = new WeakReference<ActivityListRefreshBase<T>>(activity);
		}

		public void stopAnimation(){
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
				ref.mState = STATE_NONE;
			}
		}
		
		public final void request(){
			if(!HttpApiService.prepare(this)) {
				return;
			}

			if(doRequest()){
				ActivityListRefreshBase<T> ref = activityRef.get();
				if(ref!=null){
					ref.mState = STATE_REFRESH;
					startAnimation();
				}
			}else{
				onRequestError();
			}
		}

		private void onRequestError() {
			stopAnimation();
			onFailure();
		}

		protected abstract boolean doRequest();

		protected void startAnimation(){
			animationStartMinlliseconds = SystemClock.elapsedRealtime();
		}
		
		@Override
		protected void onSuccess(final Response response) {
			if (!isAnimationRuntimeOutMin()) {
				// 动画还没有播足时间，延时再处理
				ActivityListRefreshBase<T> ref = activityRef.get();
				if (ref != null) {
					long delayMillis = MIN_MILLISECONDS_ANIMATION_RUNNING - animationRuntime();
					ref.swipeRefresher.postDelayed(new Runnable() {
						@Override
						public void run() {
							onSuccess(response);
						}
					}, delayMillis);
					return;
				}
			}

			if(HttpsURLConnection.HTTP_OK == response.code){
				parseFrom(response.body);
				stopAnimation();
			}else{
				onFailure();
			}
		}

		private boolean isAnimationRuntimeOutMin() {
			if(animationStartMinlliseconds==0){
				return true;
			}
			return animationRuntime() >= MIN_MILLISECONDS_ANIMATION_RUNNING;
		}

		private long animationRuntime() {
			return SystemClock.elapsedRealtime() - animationStartMinlliseconds;
		}

		protected void parseFrom(byte[] body) {
			if(body==null||body.length==0){
				onParseFailure();
				return;
			}
			try {
				setData(body);
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
				onParseFailure();
			}
		}

		@Override
		public final void onNetworkUnavailable() {
			stopAnimation();
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
				ref.onNetworkUnavailable();
			}
		}
		
		@Override
		protected final void onFailure() {
			if(!isAnimationRuntimeOutMin()){
				ActivityListRefreshBase<T> ref = activityRef.get();
				if(ref!=null){
					ref.swipeRefresher.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							onFailure();
						}
					},MIN_MILLISECONDS_ANIMATION_RUNNING - animationRuntime());
					return;
				}
			}
			stopAnimation();
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
				ref.onFailure();
			}
		}
		
		public abstract void onParseFailure();

		protected abstract void setData(byte[] body) throws InvalidProtocolBufferException;
		
	}
	
	protected static abstract class LoadMoreRequestor<T> extends DataRequestor<T> {

		public LoadMoreRequestor(ActivityListRefreshBase<T> activity) {
			super(activity);
		}

		@Override
		public final void startAnimation() {
			super.startAnimation();
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
				ref.getListAdapter().onLoading();
				//ref.getSwipeRefresher().setEnabled(false);
			}
		}
		
		@Override
		public final void stopAnimation() {
			super.stopAnimation();
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
				//ref.getSwipeRefresher().setEnabled(true);
				ref.getListAdapter().loadComplete();
			}
		}

		@Override
		public final void onParseFailure() {
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
				ref.getListAdapter().addData(null);
			}
		}
		
	}
	
	protected static abstract class RefreshRequestor<T> extends DataRequestor<T> {


		public RefreshRequestor(ActivityListRefreshBase<T> activity) {
			super(activity);
		}

		@Override
		public final void startAnimation() {
			super.startAnimation();
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
//				ref.getSwipeRefresher()
			}
		}
		
		@Override
		public final void stopAnimation() {
			super.stopAnimation();
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
				ref.getSwipeRefresher().refreshComplete();
			}
		}

		@Override
		public void onParseFailure() {
			ActivityListRefreshBase<T> ref = activityRef.get();
			if(ref!=null){
				ref.setEmptyViewVisible(true,ref.getEmptyRefreshTextRes());
			}
			clearCache();
		}
		
		protected abstract void clearCache();

	}

	protected PtrSubaoFrameLayout getSwipeRefresher() {
		return this.swipeRefresher;
	}

	protected void onNetworkUnavailable() {
		onFail(R.string.list_refresh_neterror);
	}

	private void onFail(int messRes) {
		boolean empty = adapter.getDataSize() == 0;
		setEmptyViewVisible(empty,messRes);
		if(!empty){
			UIUtils.showToast(messRes);
		}
	}

	/**
	 * @param visible 是否可见
	 * @param emptyTextRes  
	 */
	protected void setEmptyViewVisible(boolean visible,int emptyTextRes) {
		TextView emptyView = getEmptyView();
		if(visible){
			emptyView.setVisibility(View.VISIBLE);
			getListView().setVisibility(View.GONE);
			emptyView.setText(emptyTextRes);
		}else{
			emptyView.setVisibility(View.GONE);
			getListView().setVisibility(View.VISIBLE);
		}
	}
	
	protected void onFailure() {
		onFail(R.string.list_refresh_server_error);
	}

	public AdapterListRefreshBase<T> getListAdapter() {
		return this.adapter;
	}

	private TextView getEmptyView() {
		return this.emptyView;
	}

	protected ListView getListView(){
		return this.listView;
	}

}