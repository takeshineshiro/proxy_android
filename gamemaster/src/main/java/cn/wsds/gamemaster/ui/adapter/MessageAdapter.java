package cn.wsds.gamemaster.ui.adapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.subao.common.utils.CalendarUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.message.MessageManager;
import cn.wsds.gamemaster.message.MessageManager.Record;

@SuppressLint("DefaultLocale")
public class MessageAdapter extends BaseAdapter{

	private MessageManager.RecordList data_list;
	private final LayoutInflater inflater;
	
	public MessageAdapter(Context context, MessageManager.RecordList data_list) {
		this.data_list = data_list;
		this.inflater = LayoutInflater.from(context);
	}
	
//	public void notifyDataSetChanged(MessageManager.RecordList data_list) {
//		this.data_list = data_list;
//		this.notifyDataSetChanged();
//	}

	private static class TimeTextBuilder {
		private static String format(Calendar c) {
			return format(c, "MM-dd");
		}
		
		private static String format(Calendar c, String fmt) {
			SimpleDateFormat sdf = new SimpleDateFormat(fmt,Locale.getDefault());
			return sdf.format(c.getTime());
		}
		
		@SuppressLint("DefaultLocale")
		public static String execute(long time) {
			final Calendar c = CalendarUtils.calendarLocal_FromMilliseconds(time);
			long now = System.currentTimeMillis();
			if (now <= time) {
				return format(c);
			}
			//
			final int today = CalendarUtils.todayLocal();
			final int day = CalendarUtils.dayFrom_CalendarLocal(c);
			int delta = today - day;
			if (delta == 0) {
				long delta_ms = now - time;
				if (delta_ms >= 1000 * 3600) {
					return String.format("%d小时前", delta_ms / (1000 * 3600));
				} else if (delta_ms >= 1000 * 60) {
					return String.format("%d分钟前", delta_ms / (1000 * 60));
				} else {
					return String.format("%d秒前", Math.min(1, delta_ms / 1000));
				}
			} else if (delta == 1) {
				return format(c, "昨天 HH:mm");
			} else if (delta == 2) {
				return format(c, "前天 HH:mm");
			} else {
				return format(c);
			}
		}
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		
		ViewHolder viewHolder = null;
		if (view == null){
			// 实例组件并绑定到组件集
			view = inflater.inflate(R.layout.item_message, parent,false);
			viewHolder = new ViewHolder();
			viewHolder.img = (ImageView) view.findViewById(R.id.img_flag);
			viewHolder.tvTitle = (TextView) view.findViewById(R.id.text_title);
			viewHolder.tvTime = (TextView) view.findViewById(R.id.text_time);
			viewHolder.tvComment = (TextView) view.findViewById(R.id.text_comment);
			//将组件集放入view 标记中放入下一列读取，避免重复 find
			view.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) view.getTag();
		}
		
		Record data = (Record) getItem(position);
		viewHolder.bindView(data);
		return view;
	}
	
	/**
	 * 组件集
	 */
	private static final class ViewHolder{
		private TextView tvTitle;//消息标题
		private TextView tvComment;//消息内容
		private TextView tvTime;//消息时间
		private ImageView img;//标志
		
		public void bindView(Record data){
			tvTitle.setText(data.title);	
			
			if(Record.TYPE_JPUSH_NOTIFY_URL==data.type){						 
				tvComment.setText(getNotifyUrlContent(data.content,data.extra));
			}else{
				tvComment.setText(R.string.click_to_view);
			}
			
			img.setImageResource(data.isRead() ? R.drawable.message_center_have_read : R.drawable.message_center);
			tvTime.setText(TimeTextBuilder.execute(data.time));
		}
		
		private String getNotifyUrlContent(String content ,String extra){		
			StringBuilder sb = new StringBuilder();
			if(!TextUtils.isEmpty(extra)){
				sb.append(extra);
				sb.append(": ");
			}
			
			sb.append(content);			
			return sb.toString();
		}
	}

	@Override
	public int getCount() {
		return data_list == null ? 0 : data_list.getCount();
	}

	@Override
	public Object getItem(int position) {
		return data_list == null ? null : data_list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
