package cn.wsds.gamemaster.ui.exchange;

import hr.client.appuser.CouponCenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.Bundle;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.DataCache;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.ui.ActivityListRefreshBase;

import com.google.protobuf.InvalidProtocolBufferException;

public class ActivityExchangeHistory extends ActivityListRefreshBase<AdapterExchangeHistory.ExchangeHistoryInfo> {

    private static final int MAX_EXCHANGE_HISTORY = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new AdapterExchangeHistory(this));
        List<AdapterExchangeHistory.ExchangeHistoryInfo> cacheData = DataCache.getExchangeHistoryCache().getCacheData();
        //test
        //cacheData = testData();
        if(cacheData.isEmpty()) {
            doRefresh();
        } else {
            setData(cacheData);
        }
    }

//    private List<AdapterExchangeHistory.ExchangeHistoryInfo> testData() {
//        ArrayList<AdapterExchangeHistory.ExchangeHistoryInfo> temp = new ArrayList<>(20);
//        for (int i = 0; i < 20; i++) {
//            CouponCenter.ExchangeRecord.Builder builder = CouponCenter.ExchangeRecord.newBuilder();
//            builder.setCouponContent("12345678" + i);
//            builder.setCouponName("test" + i);
//            if(i == 0) {
//                builder.setGameName("game" + i);
//            }
//            CouponCenter.TimeRange.Builder builder1 = CouponCenter.TimeRange.newBuilder();
//            builder1.setFrom(System.currentTimeMillis());
//            builder1.setTo(System.currentTimeMillis());
//            builder.setUseTime(builder1);
//            temp.add(new AdapterExchangeHistory.ExchangeHistoryInfo(builder.build()));
//        }
//        return temp;
//    }

    @Override
    protected LoadMoreRequestor<AdapterExchangeHistory.ExchangeHistoryInfo> createLoadMoreRequestor() {
        return new ExchangeHistoryMore(this);
    }

    @Override
    protected RefreshRequestor<AdapterExchangeHistory.ExchangeHistoryInfo> createRefreshRequestor() {
        return new RefreshExchangeHistory(this);
    }

    private final class ExchangeHistoryMore extends LoadMoreRequestor<AdapterExchangeHistory.ExchangeHistoryInfo> {

        public ExchangeHistoryMore(ActivityListRefreshBase<AdapterExchangeHistory.ExchangeHistoryInfo> activity) {
            super(activity);
        }

        @Override
        protected boolean doRequest() {
            ActivityListRefreshBase<AdapterExchangeHistory.ExchangeHistoryInfo> ref = activityRef.get();
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
            return HttpApiService.requestExchangeHistory(before, MAX_EXCHANGE_HISTORY, this);
        }

        @Override
        protected void setData(byte[] body) throws InvalidProtocolBufferException {
            List<AdapterExchangeHistory.ExchangeHistoryInfo> infoList = doParse(body);
            if(infoList == null || infoList.isEmpty()) {
                onParseFailure();
                return;
            }

            sortHistoryInfo(infoList);
            DataCache.getExchangeHistoryCache().addData(infoList);
            ActivityListRefreshBase<AdapterExchangeHistory.ExchangeHistoryInfo> activity = activityRef.get();
            if(activity != null) {
                activity.getListAdapter().addData(infoList);
            }
        }
    }

    private void sortHistoryInfo(List<AdapterExchangeHistory.ExchangeHistoryInfo> infoList) {
        Collections.sort(infoList, new Comparator<AdapterExchangeHistory.ExchangeHistoryInfo>() {
            @Override
            public int compare(AdapterExchangeHistory.ExchangeHistoryInfo lhs, AdapterExchangeHistory.ExchangeHistoryInfo rhs) {
                return rhs.exchangeRecord.getTimestamp().compareTo(lhs.exchangeRecord.getTimestamp());
            }
        });
    }

    private final class RefreshExchangeHistory extends RefreshRequestor<AdapterExchangeHistory.ExchangeHistoryInfo> {

        public RefreshExchangeHistory(ActivityListRefreshBase<AdapterExchangeHistory.ExchangeHistoryInfo> activity) {
            super(activity);
        }

        @Override
        protected boolean doRequest() {
            return HttpApiService.requestExchangeHistory(0, MAX_EXCHANGE_HISTORY, this);
        }

        @Override
        protected void setData(byte[] body) throws InvalidProtocolBufferException {
            setExchangeHistoryData(body);
        }

        private void setExchangeHistoryData(byte[] body) {
            List<AdapterExchangeHistory.ExchangeHistoryInfo> infoList = doParse(body);
            if(infoList == null || infoList.isEmpty()) {
            	onParseFailure();
            	return;
            }
            sortHistoryInfo(infoList);
            DataCache.getExchangeHistoryCache().setData(infoList);
            ActivityListRefreshBase<AdapterExchangeHistory.ExchangeHistoryInfo> activity = activityRef.get();
            if(activity != null) {
                activity.setData(infoList);
            }
        }

		@Override
		protected void clearCache() {
			DataCache.getExchangeHistoryCache().clear();
		}
    }

    private List<AdapterExchangeHistory.ExchangeHistoryInfo> doParse(byte[] body) {
        try {
            CouponCenter.GetCouponExchangeHistoryResponse exchangeHistory = CouponCenter.GetCouponExchangeHistoryResponse.parseFrom(body);
            if(exchangeHistory.getResultCode() == 0 ) {
                int count = exchangeHistory.getExchangeHistoryListCount();
                if(count == 0) {
                    return null;
                }
                List<AdapterExchangeHistory.ExchangeHistoryInfo> historyInfos = new ArrayList<AdapterExchangeHistory.ExchangeHistoryInfo>(MAX_EXCHANGE_HISTORY);
                for (int i = 0; i < count; i++) {
                    historyInfos.add(new AdapterExchangeHistory.ExchangeHistoryInfo(exchangeHistory.getExchangeHistoryList(i)));
                }

                return historyInfos;
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_exchange_history;
    }
    
    @Override
    protected int getEmptyRefreshTextRes() {
    	return R.string.exchange_hirstory_empty;
    }
}
