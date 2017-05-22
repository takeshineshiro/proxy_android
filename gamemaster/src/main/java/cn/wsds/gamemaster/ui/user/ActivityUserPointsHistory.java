package cn.wsds.gamemaster.ui.user;

import hr.client.appuser.GetPointsHistory.GetUserPointsHistoryResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.os.Bundle;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.DataCache;
import cn.wsds.gamemaster.data.PointsHistoryRecord;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.ActivityListRefreshBase;
import cn.wsds.gamemaster.ui.adapter.AdapterUserPointsHistory;

import com.google.protobuf.InvalidProtocolBufferException;

public class ActivityUserPointsHistory extends ActivityListRefreshBase<PointsHistoryRecord> {
	
	private static final int REQUEST_HIRSTORY_COUNT = 30;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(this.getTitle());
		
		setListAdapter(new AdapterUserPointsHistory(this));
		
		List<PointsHistoryRecord> cacheData = DataCache.getPointsChangeRecordCache().getCacheData();
		if(cacheData.isEmpty()){
			doRefresh();
		}else{
			setData(cacheData);
		}
	}
	
	@Override
	protected RefreshRequestor<PointsHistoryRecord> createRefreshRequestor() {
		return new HistoryRefresh(this);
	}
	
	@Override
	protected LoadMoreRequestor<PointsHistoryRecord> createLoadMoreRequestor() {
		return new HistoryLoadMore(this);
	}
	
	private static final class HistoryRefresh extends RefreshRequestor<PointsHistoryRecord> {

		public HistoryRefresh(ActivityUserPointsHistory activity) {
			super(activity);
		}


		protected void setData(byte[] body) throws InvalidProtocolBufferException {
			List<PointsHistoryRecord> records = ActivityUserPointsHistory.doParse(body);
			if(records == null || records.isEmpty()){
				onParseFailure();
				return;
			}
			DataCache.getPointsChangeRecordCache().setData(records);
			ActivityListRefreshBase<PointsHistoryRecord> ref = activityRef.get();
			if(ref!=null){
				ref.setData(records);
			}
		}
		
		@Override
		protected boolean doRequest() {
			return HttpApiService.requestGetUserPointsHistory(0, getRequestNumber(), this);
		}


		@Override
		protected void clearCache() {
			DataCache.getPointsChangeRecordCache().clear();
		}
		

	}
	
	public static int getRequestNumber(){
		int debugPointHistoryRequest = ConfigManager.getInstance().getDebugPointHistoryRequest();
		int requestNumber = debugPointHistoryRequest >= 0 ? debugPointHistoryRequest : REQUEST_HIRSTORY_COUNT;
		return requestNumber;
	}
	
	private static final class HistoryLoadMore extends LoadMoreRequestor<PointsHistoryRecord> {

		public HistoryLoadMore(ActivityUserPointsHistory activity) {
			super(activity);
		}

		@Override
		protected boolean doRequest() {
			ActivityListRefreshBase<PointsHistoryRecord> ref = activityRef.get();
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
			return HttpApiService.requestGetUserPointsHistory(before, getRequestNumber(), this);
		}
		
		@Override
		protected void setData(byte[] body) throws InvalidProtocolBufferException {
			List<PointsHistoryRecord> records = ActivityUserPointsHistory.doParse(body);
			if(records == null || records.isEmpty()){
				onParseFailure();
				return;
			}
			DataCache.getPointsChangeRecordCache().addData(records);
			ActivityListRefreshBase<PointsHistoryRecord> ref = activityRef.get();
			if(ref!=null){
				if(records.isEmpty()){
					records = DataCache.getPointsChangeRecordCache().getCacheData();
				}
				ref.getListAdapter().addData(records);
			}
		}
	}


	public static List<PointsHistoryRecord> doParse(byte[] body) throws InvalidProtocolBufferException {
		GetUserPointsHistoryResponse parse = GetUserPointsHistoryResponse.parseFrom(body);
		if(0 == parse.getResultCode()){
			List<PointsHistoryRecord> data = new ArrayList<PointsHistoryRecord>();
			int userPointsHistoryCount = parse.getUserPointsHistoryCount();
			if(userPointsHistoryCount == 0){
				return null;
			}
			Calendar calendar = Misc.createCalendarOfCST();
			for (int i = 0; i < userPointsHistoryCount; i++) {
				data.add(new PointsHistoryRecord(parse.getUserPointsHistory(i), calendar));
			}
			return data;
		}
		return null;
	}

	@Override
	protected int getLayoutResID() {
		return R.layout.activity_points_history;
	}

}