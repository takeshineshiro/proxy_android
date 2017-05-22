package cn.wsds.gamemaster.ui.exchange;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.DataCache;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.event.JPushMessageReceiver;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.StatisticUtils;
import cn.wsds.gamemaster.ui.ActivityListRefreshBase;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.exchange.AdapterExchangeCenter.ExchangeGoodsInfo;
import hr.client.appuser.CouponCenter;

/**
 * “兑换中心”
 * <p>
 * 页面布局分为上下两部分，上部分为流量包兑换，下部分为礼包列表
 * </p>
 */
public class ActivityExchangeCenter extends ActivityListRefreshBase<AdapterExchangeCenter.ExchangeGoodsInfo> {

    private static final int MAX_EXCHANGE_COUPON = 20;
    private final UserSession.UserSessionObserver userSessionObserver = new UserSession.UserSessionObserver() {
        @Override
        public void onSessionInfoChanged(SessionInfo info) {
            initHeaderView();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UserSession.getInstance().unregisterUserSessionObserver(userSessionObserver);
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(R.string.label_activity_exchange_center);
        setListAdapter(new AdapterExchangeCenter(this));
        StatisticUtils.statisticUserExchangeCenterInScore(getApplicationContext());

        UserSession.getInstance().registerUserSessionObserver(userSessionObserver);
	}

    @Override
    protected void onStart() {
        super.onStart();
        loadData();
    }

    @Override
    protected LoadMoreRequestor<ExchangeGoodsInfo> createLoadMoreRequestor() {
        return new LoadMoreCoupon(this);
    }
    
    @Override
	protected void reportEvent(Bundle bundle) {
		if(JPushMessageReceiver.jpushTurnExchange(bundle)){
			Statistic.addEvent(this, Statistic.Event.USER_EXCHANGE_CENTRE_CLICK,true); 
			return ;
		}
		
		Statistic.addEvent(this, Statistic.Event.USER_EXCHANGE_CENTRE_CLICK,false); 
	}

	private final class LoadMoreCoupon extends LoadMoreRequestor<ExchangeGoodsInfo>  {

        public LoadMoreCoupon(ActivityListRefreshBase<ExchangeGoodsInfo> activity) {
            super(activity);
        }

        @Override
        protected boolean doRequest() {
            ActivityListRefreshBase<ExchangeGoodsInfo> activity = activityRef.get();
            if (activity == null) {
                return false;
            }

            int dataSize = activity.getListAdapter().getDataSize();
            long before;
            if(dataSize == 0){
                before = 0;
            }else{
                before = activity.getListAdapter().getItemId(dataSize - 1);
                if(before < 0){
                    return false;
                }
            }
            return performCoupons(before, this);
        }

        @Override
        protected void setData(byte[] body) throws InvalidProtocolBufferException {
            List<ExchangeGoodsInfo> goodsInfos = handleExchangeGoodsInfos(body);
            if (goodsInfos == null) {
                onParseFailure();
                return;
            }
            ActivityListRefreshBase<ExchangeGoodsInfo> ref = activityRef.get();
            if(ref != null) {
                ref.getListAdapter().addData(goodsInfos);
            }
        }
    }
    @Override
    protected RefreshRequestor<ExchangeGoodsInfo> createRefreshRequestor() {
        return new RefreshExchangeList(this);
    }

    @Override
	protected int getLayoutResID() {
		return R.layout.activity_exchange_center;
	}

    private void loadData() {
        initHeaderView();

        List<ExchangeGoodsInfo> cacheData = DataCache.getExchangeGoodsCache().getCacheData();
        //test
        //cacheData = testData();
        if(cacheData.isEmpty()) {
           doRefresh();
        } else {
            setData(cacheData);
        }
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
        SpannableStringBuilder textBuilder = SpannableStringUtils.getTextBuilderUnderline(getResources().getString(R.string.login_text));
        textLogin.setText(textBuilder);
        textLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityUserAccount.open(ActivityExchangeCenter.this,
                        ActivityUserAccount.FRAGMENT_TYPE_LOGIN);
            }
        });

        TextView textUser = (TextView) findViewById(R.id.text_user);
        textUser.setText(R.string.unlogin_text);
    }

    private void initLoginView() {
        UserInfo userInfo = UserSession.getInstance().getUserInfo();
        if(userInfo != null) {
            setUserNameView(userInfo);

            setLoginView(userInfo);
        }
    }

    private void setLoginView(UserInfo userInfo) {
        TextView textLogin = (TextView)findViewById(R.id.text_login);
        textLogin.setTextColor(getResources().getColor(R.color.color_game_6));
        String str = String.format("%d分", userInfo.getScore());
        SpannableStringBuilder textBuilder = SpannableStringUtils.getTextBuilder("当前积分：", str, "分".length(),
                this.getResources().getColor(R.color.color_game_11), getResources().getDimensionPixelSize(R.dimen.text_size_16));
        textLogin.setText(textBuilder);
        textLogin.setClickable(false);
    }

    private void setUserNameView(UserInfo userInfo) {
        TextView textUser = (TextView) findViewById(R.id.text_user);
        textUser.setText("用户：" + userInfo.getUserName());
    }

    private static boolean performCoupons(long start, ResponseHandler callback) {
        boolean result;
        if(UserSession.isLogined()) {
            result = HttpApiService.requestUserCouponList(start, MAX_EXCHANGE_COUPON, callback);
        } else {
            result = HttpApiService.requestCouponsList(start, MAX_EXCHANGE_COUPON, callback);
        }
        return result;
    }

    private static List<ExchangeGoodsInfo> doParse(byte[] body) {
        try {
            CouponCenter.ListCouponsResponse response = CouponCenter.ListCouponsResponse.parseFrom(body);
            if(response.getResultCode() == 0) {
                return parseGoodsInfos(response);
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static List<ExchangeGoodsInfo> parseGoodsInfos(CouponCenter.ListCouponsResponse response) {
        int count = response.getCouponListCount();
        if(count == 0) {
            return null;
        }
        ArrayList<ExchangeGoodsInfo> infos = new ArrayList<ExchangeGoodsInfo>(20);
        for (int i = 0; i < count; i++) {
            CouponCenter.AppCoupon coupon = response.getCouponList(i);
            if(AdapterExchangeCenter.isFlowPackageType(coupon.getCouponType()) ) {
                continue;
            }
            infos.add(new ExchangeGoodsInfo(coupon));
        }
        return infos;
    }
       //包含重复元素排序
    private  static  void  sortExchangeCenter(List<ExchangeGoodsInfo>goodsInfos) {
           //默认为升序
        Collections.sort(goodsInfos, new Comparator<ExchangeGoodsInfo>() {
            @Override
            public int compare(ExchangeGoodsInfo lhs, ExchangeGoodsInfo rhs) {
                //先按couponNum进行排序
                int  flag =0 ;
                flag =   String.valueOf(lhs.coupon.getCouponNum()).compareTo(String.valueOf(rhs.coupon.getCouponNum()));
                //如果couponNum相同，则按进入先后排序
                if(flag==0) {
                   flag  = -1;
                }
                return  flag ;
            }
        });

    }

    private static CouponCenter.AppCoupon createFlowCoupon(CouponCenter.AppCoupon coupon) {
        String couponId = coupon.getCouponId();
        CouponCenter.AppCoupon.Builder builder = CouponCenter.AppCoupon.newBuilder();
        if(!TextUtils.isEmpty(couponId)) {
            builder.setCouponId(couponId);
        }
        builder.setCouponType(coupon.getCouponType());
        builder.setNeedPoints(100);
        return builder.build();
    }

    @Nullable
    private static List<ExchangeGoodsInfo> handleExchangeGoodsInfos(byte[] body) {
        List<ExchangeGoodsInfo> goodsInfos = doParse(body);
        if(goodsInfos == null || goodsInfos.isEmpty()) {
            return null;
        }
        //游戏展示排序　默认序号从小到大排列
         sortExchangeCenter(goodsInfos) ;

        ExchangeGoodsBuffer.getInstance().setExchangeGoods(goodsInfos);
        DataCache.getExchangeGoodsCache().setData(goodsInfos);
        return goodsInfos;
    }

    private final class RefreshExchangeList extends RefreshRequestor<AdapterExchangeCenter.ExchangeGoodsInfo> {

        public RefreshExchangeList(ActivityListRefreshBase<AdapterExchangeCenter.ExchangeGoodsInfo> activity) {
            super(activity);
        }

        @Override
        protected boolean doRequest() {
            return performCoupons(0, this);
        }

        @Override
        protected void setData(byte[] body) throws InvalidProtocolBufferException {
            List<ExchangeGoodsInfo> goodsInfos = handleExchangeGoodsInfos(body);
            if (goodsInfos == null) {
                onParseFailure();
                return;
            }
            ActivityListRefreshBase<ExchangeGoodsInfo> ref = activityRef.get();
            if(ref != null) {
                ref.setData(goodsInfos);
            }
        }




        @Override
		protected void clearCache() {
			DataCache.getExchangeGoodsCache().clear();
		}
    }

    @Override
    protected int getEmptyRefreshTextRes() {
    	return R.string.exchange_goods_empty;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_exchange_center, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exchange_history:
                if(UserSession.isLogined()) {
                    UIUtils.turnActivity(this, ActivityExchangeHistory.class);
                } else {
                    UIUtils.showReloginDialog(this, "查看兑换历史，需要登录！", null);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        System.out.println("hujd result resultCode: " + resultCode + "requestCode: " + requestCode);
        if(resultCode == 1 && requestCode == 1) {
            doRefresh();
        }
    }

    public static final class UserSessionChangeObserver extends UserSession.UserSessionObserver {
        @Override
        public void onSessionInfoChanged(SessionInfo info) {
            DataCache.getExchangeGoodsCache().clear();
            DataCache.getExchangeHistoryCache().clear();
        }
    }

	@Override
	protected ActivityType getPreActivityType() {
		return ActivityType.USER_CENTER;
	}
}
