package cn.wsds.gamemaster.data;

import java.util.ArrayList;
import java.util.List;

import android.os.SystemClock;

import cn.wsds.gamemaster.pay.model.OrderDetail;
import cn.wsds.gamemaster.ui.exchange.AdapterExchangeCenter;
import cn.wsds.gamemaster.ui.exchange.AdapterExchangeHistory;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskRecord;

public final class DataCache<T> {

	private final List<T> datas = new ArrayList<T>();
	
	/**
	 * cache 有效期
	 */
	private static final long TIMEMILLIS_VALIDITY_CACHE = 60 * 1000;
	
	/**
	 * cache 修改时间
	 */
	private long timeMillisCacheUpdate;
	
	// !!! 注意，这里可能有一个潜在的隐患，即外部调用这个函数准备
    // 追加新数据的时候，如果缓存数据已脏应该怎么处理？语义不明！
	/**
	 * 这个函数的应用场景是：在已有数据后追加更多新数据,成功返回true，
	 * 失败返回false; 如果已有数据已经被清空，则调用追加没有意义，向
	 * 调用者返回false
	 */
	public final boolean addData(List<T> datas ){
		if(datas == null || datas.isEmpty()){
			return true;
		}
		if(this.datas.isEmpty()){
			return false;
		}
		
		this.datas.addAll(datas);
		timeMillisCacheUpdate = now();
		
		return true;
	}
	
	public final void setData(List<T> datas){
		this.datas.clear();
		if (datas != null && !datas.isEmpty()) {
			this.datas.addAll(datas);
		}
		timeMillisCacheUpdate = now();
	}
	
//	public final void removeItem(T t){
//		datas.remove(t);
//		timeMillisCacheUpdate = now();
//	}

	private static long now() {
		return SystemClock.elapsedRealtime();
	}
	
	public final List<T> getCacheData() {
		if ((now() - timeMillisCacheUpdate) <= TIMEMILLIS_VALIDITY_CACHE) {
			return new ArrayList<T>(this.datas);
		} else {
			this.datas.clear();
			return new ArrayList<T>();
		}
	}
	
	public final void clear() {
		this.datas.clear();
		timeMillisCacheUpdate = 0;
	}
	
	/**
	 * 推荐游戏缓存
	 */
	private static final DataCache<RecommandGameInfo> recommandGameInfosCache = new DataCache<RecommandGameInfo>();
	
	public static DataCache<RecommandGameInfo> getRecommandgameinfoscache() {
		return recommandGameInfosCache;
	}
	
	/**
	 * 积分历史缓存
	 */
	private static final DataCache<PointsHistoryRecord> pointsChangeRecordCache = new DataCache<PointsHistoryRecord>();
	
	public static DataCache<PointsHistoryRecord> getPointsChangeRecordCache() {
		return pointsChangeRecordCache;
	}

    /**
     * 礼品列表缓存
     */
    private static final DataCache<AdapterExchangeCenter.ExchangeGoodsInfo> exchangeGoodsInfoDataCache = new DataCache<AdapterExchangeCenter.ExchangeGoodsInfo>();

    public static DataCache<AdapterExchangeCenter.ExchangeGoodsInfo> getExchangeGoodsCache() {
        return exchangeGoodsInfoDataCache;
    }

    /**
     * 兑换历史缓存
     */
    private static final DataCache<AdapterExchangeHistory.ExchangeHistoryInfo> exchangeHistoryInfoDataCache = new DataCache<AdapterExchangeHistory.ExchangeHistoryInfo>();

    public static DataCache<AdapterExchangeHistory.ExchangeHistoryInfo> getExchangeHistoryCache() {
        return exchangeHistoryInfoDataCache;
    }
    
    /**
     * 任务列表缓存
     */
    private static final DataCache<TaskRecord> taskRecordsDataCache = new DataCache<TaskRecord>();

    public static DataCache<TaskRecord> getTaskRecordsDataCache() {
        return taskRecordsDataCache;
    }

	/**
	 * 订单历史缓存
	 */
	private static final DataCache<OrderDetail> orderHistoryDataCache = new DataCache<OrderDetail>();

	public static DataCache<OrderDetail> getOrderHistoryCache() {
		return orderHistoryDataCache;
	}
}
