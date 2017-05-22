package cn.wsds.gamemaster.ui.exchange;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.adapter.AdapterListRefreshBase;
import cn.wsds.gamemaster.ui.market.ActivityMarketWeb;
import hr.client.appuser.CouponCenter;

/**
 * Created by hujd on 16-2-26.
 */
public class AdapterExchangeCenter extends AdapterListRefreshBase<AdapterExchangeCenter.ExchangeGoodsInfo> {

    private Activity activity;
    private DisplayImageOptions options; //配置图片加载及显示选项

    @Override
    public long getItemId(int arg0) {
        long itemId = -1;
        ExchangeGoodsInfo item = getItem(arg0);
        if(item == null) {
            return itemId;
        }
        try {
            //TODO
            itemId = 27;
        } catch (NumberFormatException e) {

        }

        return itemId == 0 ? -1 : itemId;
    }

    public AdapterExchangeCenter(Context context) {
        super(context);

        options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.icon_present)
                .showImageOnFail(R.drawable.icon_present)
                .cacheInMemory(true)
                .displayer(new RoundedBitmapDisplayer(20))
                .build();
        activity = (Activity) context;
    }

    @Override
    protected View getRealView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null){
            convertView = LayoutInflater.from(
                    parent.getContext()).inflate(R.layout.item_exchange_goods,
                    parent,false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }else{
            Object tag = convertView.getTag();
            viewHolder = (ViewHolder) tag;
        }

        ExchangeGoodsInfo info = getItem(position);
        if(info != null) {
            viewHolder.bindView(info, parent);
        }
        return convertView;
    }

    private final class ViewHolder {
        private final TextView textExchangePoints;
        private final ImageView exchangeIcon;
        private final TextView exchangeName;
        private final TextView exchangeDesc;
        private final TextView newLabel;
        private final View view;
        public ViewHolder(View view) {
            this.view = view;
            textExchangePoints = (TextView) view.findViewById(R.id.text_exchange_points);
            exchangeIcon = (ImageView) view.findViewById(R.id.image_icon);
            exchangeName = (TextView) view.findViewById(R.id.text_name);
            exchangeDesc = (TextView) view.findViewById(R.id.text_recommand_reason);
            newLabel     = (TextView) view.findViewById(R.id.text_new);
        }


		public void bindView(final ExchangeGoodsInfo info, final ViewGroup viewGroup) {

            setExchangeOnClickListener(viewGroup, R.id.button_exchange, info.coupon);
            setExchangeOnClickListener(viewGroup, R.id.whole_layout, info.coupon);


            if(isFlowPackageType(info.coupon.getCouponType())) {
                setFlowView(info, viewGroup);
            } else {
                setNormalView(info, viewGroup);
            }
        }

        private void setExchangeOnClickListener(final ViewGroup viewGroup, int id, final CouponCenter.AppCoupon coupon) {
            final String couponId = coupon.getCouponId();
            final int couponType = coupon.getCouponType();
            view.findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onExchangeClick();
                }

                private void onExchangeClick() {
                    Context context = viewGroup.getContext();
                    if (isFlowPackageType(couponType)) {
                        UIUtils.turnActivity(context, ActivityMarketWeb.class);
                    } else {
                        Intent intent = new Intent(context, ActivityGoodsDiscription.class);
                        intent.putExtra("couponId", couponId);
                        activity.startActivityForResult(intent, 1);
                    }
                }
            });


        }
        private void setNormalView(ExchangeGoodsInfo info, ViewGroup viewGroup) {
             if(isVipPackageType(info.coupon.getCouponType())) {
                 textExchangePoints.setTextColor(ContextCompat.getColor(AppMain.getContext(),R.color.color_game_20));
                 textExchangePoints.setBackgroundResource(R.drawable.convert_inte_tag);
                 //还差显示new
                 newLabel.setVisibility(View.VISIBLE);
             }
            textExchangePoints.setText(info.coupon.getNeedPoints() + "积分");
            if (!TextUtils.isEmpty(info.coupon.getCouponName())) {
                exchangeName.setText(info.coupon.getCouponName());
            }
            String couponDescription = info.coupon.getCouponDescription();
            if (!TextUtils.isEmpty(couponDescription)) {
                String str = couponDescription.replaceAll("\n", ",");
                exchangeDesc.setClickable(true);
                exchangeDesc.setMovementMethod(LinkMovementMethod.getInstance());
                exchangeDesc.setText(Html.fromHtml(str));
            }
            String couponIconUrl = info.coupon.getCouponIconUrl();
            //couponIconUrl = "http://game.wsds.cn/d/icon/qingqiuhuchuanshuo.jpg";
            ImageLoader.getInstance().displayImage(couponIconUrl, exchangeIcon, options);
        }

        @SuppressWarnings("deprecation")
        private void setFlowView(ExchangeGoodsInfo info, ViewGroup viewGroup) {
            textExchangePoints.setText(info.coupon.getNeedPoints() + "积分起");
            exchangeIcon.setImageDrawable(viewGroup.getContext().getResources().getDrawable(R.drawable.icon_flow));
            exchangeName.setText("全网流量包");
            exchangeDesc.setText("100M/300M流量补给包～");
        }


    }
    public static final class ExchangeGoodsInfo {
        public final CouponCenter.AppCoupon coupon;

        public ExchangeGoodsInfo(CouponCenter.AppCoupon coupon) {
            this.coupon = coupon;
        }
    }

    public static boolean isFlowPackageType(int couponType) {

        return couponType == 0;
    }

    public static boolean isVipPackageType(int couponType) {

        return couponType == 2;
    }


    @Override
    public boolean isLoadMore() {
        return false;
    }
}
