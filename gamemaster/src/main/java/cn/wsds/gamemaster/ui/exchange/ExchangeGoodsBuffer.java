package cn.wsds.gamemaster.ui.exchange;

import hr.client.appuser.CouponCenter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hujd on 16-3-1.
 */
public class ExchangeGoodsBuffer {
    private static final ExchangeGoodsBuffer instance = new ExchangeGoodsBuffer();

    private static final ArrayList<AdapterExchangeCenter.ExchangeGoodsInfo> arrayList = new ArrayList<AdapterExchangeCenter.ExchangeGoodsInfo>(20);

    public static ExchangeGoodsBuffer getInstance() {
        return instance;
    }

    public CouponCenter.AppCoupon getCouponData(String id) {
        for (AdapterExchangeCenter.ExchangeGoodsInfo data : arrayList) {
            String couponId = data.coupon.getCouponId();
            if(couponId == null) {
                continue;
            }
            if(couponId.equals(id)) {
                return data.coupon;
            }
        }
        return null;
    }

    public void setExchangeGoods(List<AdapterExchangeCenter.ExchangeGoodsInfo> infos) {
        if(infos == null || infos.isEmpty()) {
            return;
        }

        arrayList.clear();
        arrayList.addAll(infos);
    }

//    private List<AdapterExchangeCenter.ExchangeGoodsInfo> testData() {
//        ArrayList<AdapterExchangeCenter.ExchangeGoodsInfo> temp = new ArrayList<>(20);
//        for (int i = 0; i < 20; i++) {
//            CouponCenter.AppCoupon.Builder p = CouponCenter.AppCoupon.newBuilder();
//            p.setCouponId("" + i);
//            p.setHowToUse("tian->youxi->tian->");
//            p.setRemainderPercent(i);
//            CouponCenter.TimeRange.Builder builder = CouponCenter.TimeRange.newBuilder();
//            builder.setFrom(System.currentTimeMillis());
//            builder.setTo(System.currentTimeMillis());
//            p.setExchangeTime(builder.build());
//            p.setCouponDescription("test" + i + " test");
//            p.setCouponName("test" + i);
//            p.setGameName("game" + i);
//            p.setNeedPoints(100);
//            temp.add(new AdapterExchangeCenter.ExchangeGoodsInfo(p.build()));
//        }
//        return temp;
//    }
}
