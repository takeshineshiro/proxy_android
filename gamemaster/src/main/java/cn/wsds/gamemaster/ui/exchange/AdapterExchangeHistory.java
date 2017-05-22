package cn.wsds.gamemaster.ui.exchange;

import hr.client.appuser.CouponCenter;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.adapter.AdapterListRefreshBase;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

/**
 * Created by hujd on 16-2-26.
 */
public class AdapterExchangeHistory extends AdapterListRefreshBase<AdapterExchangeHistory.ExchangeHistoryInfo> {
    private DisplayImageOptions options; //配置图片加载及显示选项

    @Override
    public long getItemId(int arg0) {
        long itemId = -1;
        ExchangeHistoryInfo item = getItem(arg0);
        if(item == null) {
            return itemId;
        }
        try {
            itemId = Long.valueOf(item.exchangeRecord.getTimestamp());
        } catch (NumberFormatException e) {

        }

        return itemId == 0 ? -1 : itemId;
    }
    public AdapterExchangeHistory(Context context) {
        super(context);
        options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.icon_present)
                .showImageOnFail(R.drawable.icon_present)
                .cacheInMemory(true)
                .displayer(new RoundedBitmapDisplayer(20))
                .build();
    }

    @Override
    protected View getRealView(int position, View convertView, ViewGroup parent) {
        ExchangeHistoryInfo info = getItem(position);
        if(info == null) {
            return convertView;
        }

        String gameName = info.exchangeRecord.getGameName();
        final BinderViewHolderBase binderViewHolder;
        if(convertView == null){
            binderViewHolder = new BinderViewHolderBase(null, parent, null, gameName);
        }else{
            Object tag = convertView.getTag();
            final ViewHolder viewHolder = (ViewHolder) tag;
            binderViewHolder = new BinderViewHolder(convertView, parent, viewHolder, gameName);
        }
        binderViewHolder.invoke();

        binderViewHolder.getViewHolder().bindView(info, parent);
        return binderViewHolder.getConvertView();
    }

    private View createBinderView(ViewGroup parent, int layoutId) {
        View convertView;
        convertView = LayoutInflater.from(
                parent.getContext()).inflate(layoutId,
                parent,false);
        return convertView;
    }

    private static abstract class ViewHolder {
        abstract public void bindView(ExchangeHistoryInfo info, final ViewGroup viewGroup);
        public boolean isLabelHolder(){
            return false;
        }
    }

    private static final class FlowViewHolder extends ViewHolder {
        private final ImageView exchangeIcon;
        private final TextView exchangeName;

        public FlowViewHolder(View view) {
            this.exchangeIcon = (ImageView) view.findViewById(R.id.image_icon);
            this.exchangeName = (TextView) view.findViewById(R.id.text_name);
        }

        @Override
        public boolean isLabelHolder(){
            return true;
        }
        @Override
        public void bindView(ExchangeHistoryInfo info, ViewGroup viewGroup) {
            String gameIconUrl = info.exchangeRecord.getGameIconUrl();
            if(gameIconUrl != null && !"".equals(gameIconUrl)) {
                exchangeIcon.setImageURI(Uri.parse(gameIconUrl));
            }

            String couponName = info.exchangeRecord.getCouponName();
            if(couponName != null && !"".equals(couponName)) {
                exchangeName.setText(couponName);
            }
        }
    }

    private final class VirtualViewHolder extends ViewHolder{
        private final ImageView exchangeIcon;
        private final TextView exchangeName;
        private final TextView textUsePeriod;
        private final TextView exchangeCode;
        private final View view;
        public VirtualViewHolder(View group) {
            this.view = group;
            exchangeIcon = (ImageView) group.findViewById(R.id.image_icon);
            exchangeName = (TextView) group.findViewById(R.id.text_name);
            textUsePeriod = (TextView) group.findViewById(R.id.text_use_period);
            exchangeCode = (TextView) group.findViewById(R.id.text_exchange_code);
        }

        @Override
        public void bindView(ExchangeHistoryInfo info, final ViewGroup viewGroup) {
            String couponName = info.exchangeRecord.getCouponName();
            if( couponName!= null && !"".equals(couponName)) {
                exchangeName.setText(couponName);
            }
            setHistoryPeriod(info);

            setCopyTextView(info, viewGroup);
            String gameIconUrl = info.exchangeRecord.getGameIconUrl();
            if( TextUtils.isEmpty(gameIconUrl)) {
                exchangeIcon.setImageResource(R.drawable.icon_present);
            } else {
                ImageLoader.getInstance().displayImage(gameIconUrl, exchangeIcon, options);
            }

            String couponContent = info.exchangeRecord.getCouponContent();
            if(couponContent != null && !"".equals(couponContent)) {
                exchangeCode.setText(couponContent);
            }


        }

        private void setCopyTextView(ExchangeHistoryInfo info, final ViewGroup viewGroup) {

            TextView textView = (TextView) view.findViewById(R.id.text_copy);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String str = exchangeCode.getText().toString().trim();
                    ActivityGoodsDiscription.copyToClip(viewGroup.getContext(), str);
                }
            });

            String serverTime = info.exchangeRecord.getServerTime();
            try {
                long currentTime = Long.valueOf(serverTime);
                CouponCenter.TimeRange useTime = info.exchangeRecord.getUseTime();
                if(useTime == null) {
                    return;
                }
                if(currentTime < useTime.getFrom() || currentTime > useTime.getTo()) {
                    textView.setText("过期");
                    textView.setEnabled(false);
                }
            } catch (NumberFormatException e) {

            }
        }

        private void setHistoryPeriod(ExchangeHistoryInfo info) {
            CouponCenter.TimeRange timeRange = info.exchangeRecord.getUseTime();
            if(timeRange == null) {
                return;
            }
            String from = TimeRangeFormatter.format(timeRange.getFrom());
            String to = TimeRangeFormatter.format(timeRange.getTo());
            textUsePeriod.setText("使用期限：\n" + from + "至" + to);
        }

    }
    public static final class ExchangeHistoryInfo {
        public final CouponCenter.ExchangeRecord exchangeRecord;

        public ExchangeHistoryInfo(CouponCenter.ExchangeRecord exchangeRecord) {
            this.exchangeRecord = exchangeRecord;
        }

    }

    private class BinderViewHolderBase{
        protected View mConvertView;
        protected ViewGroup mParent;
        protected ViewHolder mViewHolder;
        protected String mGameName;

        public BinderViewHolderBase(View convertView, ViewGroup parent, ViewHolder viewHolder, String gameName) {
            mConvertView = convertView;
            mParent = parent;
            mViewHolder = viewHolder;
            mGameName = gameName;
        }

        public View getConvertView() {
            return mConvertView;
        }

        public ViewHolder getViewHolder() {
            return mViewHolder;
        }

        public void invoke() {
            if(TextUtils.isEmpty(mGameName)) {
                setFlowBinderView();
            } else {
                setVirtualBinderView();
            }
        }

        protected void setFlowBinderView() {
            mConvertView = createBinderView(mParent, R.layout.item_exchange_history_flow);
            mViewHolder = new FlowViewHolder(mConvertView);
            mConvertView.setTag(mViewHolder);
        }

        protected void setVirtualBinderView() {
            mConvertView = createBinderView(mParent, R.layout.item_exchange_history);
            mViewHolder = new VirtualViewHolder(mConvertView);
            mConvertView.setTag(mViewHolder);
        }
    }
    private class BinderViewHolder extends BinderViewHolderBase {

        public BinderViewHolder(View convertView, ViewGroup parent, ViewHolder viewHolder, String gameName) {
            super(convertView, parent, viewHolder, gameName);
        }

        @Override
        public void invoke() {
            if(TextUtils.isEmpty(mGameName)) {
                if(!mViewHolder.isLabelHolder()) {
                    setFlowBinderView();
                }
            } else {
                if( mViewHolder.isLabelHolder()) {
                    setVirtualBinderView();
                }
            }
        }
    }
}
