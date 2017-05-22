package cn.wsds.gamemaster.ui.doublelink;

import java.text.DecimalFormat;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.DoubleAccelTimeRecords;
import cn.wsds.gamemaster.data.DoubleLinkUseRecords;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.exchange.SpannableStringUtils;

/**
 * Created by hujd on 16-5-6.
 */
public final class DoubleLinkDescAdapter extends BaseAdapter{
    private final Item[] items;
    private final LayoutInflater inflate;
    private final Context context;
    private final PackageManager packageManager;

    public DoubleLinkDescAdapter(Context context, List<DoubleAccelTimeRecords.Record> records) {
        this.context = context;
        packageManager = context.getPackageManager();
        inflate = LayoutInflater.from(context.getApplicationContext());
        this.items = new Item[records.size()];
        for (int i = 0; i < records.size(); ++i) {
            DoubleAccelTimeRecords.Record rec = records.get(i);
            this.items[i] = new Item(rec);
        }
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        Item item = items[position];
        if (item.icon == null) {
            GameInfo gameInfo = GameManager.getInstance().getGameInfo(item.record.getPackageName());
            if(gameInfo != null) {
                item.icon = getIcon(gameInfo.getPackageName());
            }
        }
        return item;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if(view==null){
            view = inflate.inflate(R.layout.item_double_link, parent,false);
            holder = new ViewHolder();
            holder.init(view);
            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
        }

        bindView(holder, (Item) getItem(position));
        return view;
    }

    private final class Item{
        public Drawable icon;
        public final DoubleAccelTimeRecords.Record record;
        private Item(DoubleAccelTimeRecords.Record record) {
            this.record = record;
        }
    }

    /**
     * 将数据绑定到组件上
     * @param holder
     * @param item
     */
    private void bindView(ViewHolder holder, final Item item) {
        final DoubleAccelTimeRecords.Record record = item.record;
        holder.imageIcon.setImageDrawable(item.icon);
        GameInfo gameInfo = GameManager.getInstance().getGameInfo(record.getPackageName());
        if(gameInfo != null && gameInfo.getAppLabel() != null) {
            holder.gameName.setText(gameInfo.getAppLabel());
        }
        SpannableStringBuilder textBuilder = SpannableStringUtils.getTextBuilder(UtilFormatTime.formatDoubleAccelTime(record.getTime()), "", 0,
                context.getResources().getColor(R.color.color_game_7), context.getResources().getDimensionPixelSize(R.dimen.text_size_12));
        holder.usedCount.setText(textBuilder);
    }

    /**
     * 精确流量KB
     * @param usedFlow 消耗流量 单位字节
     * @return
     */
    public static String setFormatDecimal(long usedFlow) {
        return new DecimalFormat("0.000").format((double)usedFlow / DoubleLinkUseRecords.MB);
    }

    private Drawable getIcon(String packageName){
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return applicationInfo.loadIcon(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return UIUtils.loadAppDefaultIcon(context.getApplicationContext());
        }
    }

    /**
     * 组件集
     */
    private class ViewHolder{
        private TextView gameName, usedCount;
        private ImageView imageIcon;
        public void init(View view) {
            imageIcon = (ImageView) view.findViewById(R.id.image_icon);
            gameName = (TextView) view.findViewById(R.id.text_game_name);
            usedCount = (TextView) view.findViewById(R.id.text_used_count);
        }
    }

}
