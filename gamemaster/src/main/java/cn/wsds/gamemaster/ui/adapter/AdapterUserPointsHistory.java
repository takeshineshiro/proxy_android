package cn.wsds.gamemaster.ui.adapter;


import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.PointsHistoryRecord;

public class AdapterUserPointsHistory extends AdapterListRefreshBase<PointsHistoryRecord> {

    public AdapterUserPointsHistory(Context context) {
    	super(context);
    }

    @Override
    public long getItemId(int arg0) {
        PointsHistoryRecord item = getItem(arg0);
		long itemId = item == null ? -1 : (item.milliseconds ==0 ? -1 :item.milliseconds);
		return itemId;
    }
    
    @Override
    protected View getRealView(int position, View convertView, ViewGroup parent) {
    	ViewHolder viewHolder;
    	if(convertView == null){
    		convertView = LayoutInflater.from(
                 parent.getContext()).inflate(R.layout.item_points_history,
                		 parent,false);
    		viewHolder = new ViewHolder(convertView);
    		convertView.setTag(viewHolder);
    	}else{
    		Object tag = convertView.getTag();
			viewHolder = (ViewHolder) tag;
    	}
    	PointsHistoryRecord item = getItem(position);
    	if(item!=null){
    		int old = position-1;
    		if(old < 0){
    			viewHolder.bindView(null,item);
    		}else{
    			viewHolder.bindView(getItem(old),item);
    		}
    	}
    	return convertView;
    }
    
    
    private static final class ViewHolder {
    	
    	private final TextView textTitle;
    	private final TextView textScore;
    	private final TextView textDate;
    	private final TextView textYear;
    	private final View diver;
		public ViewHolder(View group) {
			this.textTitle = (TextView) group.findViewById(R.id.text_title);
			this.textScore = (TextView) group.findViewById(R.id.text_score);
			this.textDate = (TextView) group.findViewById(R.id.text_date);
			this.textYear = (TextView) group.findViewById(R.id.text_year);
			this.diver = group.findViewById(R.id.diver);
		}
    	
    	public void bindView(PointsHistoryRecord oldRecord,PointsHistoryRecord record){
    		String title = record.changeInfoDesc;
    		this.textTitle.setText(title);
    		long socre = record.socre;
    		Resources resources = this.textScore.getResources();
			this.textScore.setText(createSocreSpannable(socre,resources));
			if(record.recordMonth == 0 && record.recordDay == 0){
				this.textDate.setText("");
			}else{
				this.textDate.setText(String.format("%d/%d", record.recordMonth,record.recordDay));
			}
			if(oldRecord==null){
				this.textYear.setVisibility(View.GONE);
				this.diver.setVisibility(View.GONE);
			}else if(oldRecord.recordYear == record.recordYear || record.recordYear == 0){
				this.textYear.setVisibility(View.GONE);
				this.diver.setVisibility(View.VISIBLE);
			}else{
				this.textYear.setVisibility(View.VISIBLE);
				this.diver.setVisibility(View.GONE);
				this.textYear.setText(record.recordYear + "年");
			}
    	}

		private CharSequence createSocreSpannable(long socre,Resources resources) {
			SpannableStringBuilder builder = new SpannableStringBuilder();
			String before = "积分  ";
			builder.append(before);
			int specialColor;
			String socreStr;
			if(socre > 0){
				socreStr = "+"+String.valueOf(socre);
				specialColor = resources.getColor(R.color.color_game_11);
			}else if(socre == 0){
				socreStr = "0";
				specialColor = resources.getColor(R.color.color_game_11);
			}else{
				socreStr = String.valueOf(socre);
				specialColor = resources.getColor(R.color.color_game_16);
			}
			builder.append(socreStr);
			builder.setSpan(new AbsoluteSizeSpan(14, true), before.length(), builder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.setSpan(new ForegroundColorSpan(specialColor), before.length(), builder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			return builder;
		} 
    }
    
}