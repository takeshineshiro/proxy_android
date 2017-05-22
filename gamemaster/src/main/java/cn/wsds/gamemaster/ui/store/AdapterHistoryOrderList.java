package cn.wsds.gamemaster.ui.store;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.pay.model.OrderDetail;
import cn.wsds.gamemaster.pay.vault.PayApiService;
import cn.wsds.gamemaster.ui.adapter.AdapterListRefreshBase;

public class AdapterHistoryOrderList extends  AdapterListRefreshBase<OrderDetail>{

	public AdapterHistoryOrderList(Context context) {
		super(context);
	}
	
	@Override
	public View getRealView(int position, View convertView, ViewGroup parent) {		
	
		Context context = parent.getContext();
		
		ViewHolder holder ;
		if(convertView==null){
			convertView = LayoutInflater.from(context)
					.inflate(R.layout.item_history_order, parent, false);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder)convertView.getTag();
		}
		
		OrderDetail order = getItem(position);
		if(order == null){
			return null ;
		}
					
		holder.bindView(order);
		
		return convertView ;
	}
	
	private static final class ViewHolder{
		
		private TextView orderId;
		private TextView time;
		private TextView name;
		private TextView payType;	
		private TextView state ;
		
		ViewHolder(View contentView){
			orderId = (TextView)contentView.findViewById(R.id.text_order_id);			 
			time = (TextView)contentView.findViewById(R.id.text_order_time);
			name = (TextView)contentView.findViewById(R.id.text_name);
			payType = (TextView)contentView.findViewById(R.id.text_pay_type);
			state= (TextView)contentView.findViewById(R.id.text_state);
		}
		
		void bindView(OrderDetail order){
			if(order==null){
				return;
			}
			
			StringBuilder builder = new StringBuilder();
			builder.append("订单编号：");
			String strOrderId = order.getOrderId();
			int length = strOrderId.length();
			if(length>8){
				strOrderId = strOrderId.substring(0,8);
			}
			builder.append(strOrderId);
			orderId.setText(builder.toString());
			
			String createTime = order.getCreateTime();
			if(createTime!=null){
				String[] times = createTime.split(" ");
				
				if((times!=null)&&(times.length>0)){
					time.setText(times[0]);
				}				
			}

			builder = new StringBuilder();
			builder.append("产品类别：");
			builder.append(order.getDescription());
			name.setText(builder.toString());
			
			builder = new StringBuilder();
			builder.append("支付方式：");
			builder.append(OrderDetail.payType(order.getPayType()));
			payType.setText(builder.toString());
			
			builder = new StringBuilder();
		    state.setText(null);
		    
		    int textColor = AppMain.getContext().getResources().getColor(R.color.color_game_7);
		    int statusColor = getStatusColor(order.getStatus());
		    SpannableString ssText = new SpannableString("状态：");
			ForegroundColorSpan spanText = new ForegroundColorSpan(textColor);
			ssText.setSpan(spanText, 0, ssText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			state.append(ssText);
			
			SpannableString ssStatus= new SpannableString(OrderDetail.orderState(order.getStatus()));
			ForegroundColorSpan spanStatus = new ForegroundColorSpan(statusColor);
			ssStatus.setSpan(spanStatus, 0, ssStatus.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			state.append(ssStatus);
		}
	}
	
	private static int getStatusColor(int status){
		switch(status){
		case PayApiService.PayStatus.PAY_STATUS_CREATED:
			return AppMain.getContext().getResources().getColor(R.color.color_game_7);
		case PayApiService.PayStatus.PAY_STATUS_PAYING:
		case PayApiService.PayStatus.PAY_STATUS_REFUNDING:
			return AppMain.getContext().getResources().getColor(R.color.order_status_paying);		 
		case PayApiService.PayStatus.PAY_STATUS_PAY_SUCCESS:			
		case PayApiService.PayStatus.PAY_STATUS_REFUND_SUCCESS:
			return AppMain.getContext().getResources().getColor(R.color.order_status_success);		
		default:
			return AppMain.getContext().getResources().getColor(R.color.order_status_failed);			
		}
		 
	}
}
