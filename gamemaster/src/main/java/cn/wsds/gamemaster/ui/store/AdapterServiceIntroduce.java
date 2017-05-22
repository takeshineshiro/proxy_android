package cn.wsds.gamemaster.ui.store;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.ui.view.TickView;
import cn.wsds.gamemaster.ui.view.TickViewAni;

/**
 * 加速服务 adapter
 * Created by hujd on 17-2-12.
 */
public class AdapterServiceIntroduce extends BaseAdapter{
	private List<ServiceSupport> supportList = new ArrayList<>(3);
	private Context context;

	public AdapterServiceIntroduce(Context context) {
		this.context = context;
	}

	public void setData(List<ServiceSupport> list) {
		if (list == null || list.isEmpty()) {
			return;
		}

		supportList.clear();
		supportList.addAll(list);
		notifyDataSetChanged();

	}

	@Override
	public int getCount() {
		return supportList.size();
	}

	@Override
	public ServiceSupport getItem(int position) {
		if (position >= 0 && position < supportList.size()) {
			return supportList.get(position);
		} else {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {


		ViewHolder viewHolder;
		if(convertView == null){
			convertView = LayoutInflater.from(
					parent.getContext()).inflate(R.layout.item_service_introduce,
					parent,false);
			viewHolder = new ViewHolder(convertView);
			convertView.setTag(viewHolder);
		}else{
			Object tag = convertView.getTag();
			viewHolder = (ViewHolder) tag;
		}
		ServiceSupport support = getItem(position);
		if (support != null) {
			viewHolder.bindView(support);
		}
		return convertView;
	}

	private final class ViewHolder {
		private final TextView accelModeView;
		private final ImageView normalUserView;
		private final ImageView vipUserView;
		private final TickViewAni tickAniView;
		private final TickView tickView;

		public ViewHolder(View convertView) {
			accelModeView = (TextView) convertView.findViewById(R.id.text_accel_mode);
			normalUserView = (ImageView) convertView.findViewById(R.id.normal_user);
			vipUserView = (ImageView) convertView.findViewById(R.id.vip_user);
			tickAniView = (TickViewAni) convertView.findViewById(R.id.tick_view_ani);
			tickView = (TickView) convertView.findViewById(R.id.tick_view);
		}

		public void bindView(ServiceSupport serviceSupport) {
			accelModeView.setText(serviceSupport.accelMode);
			accelModeView.setCompoundDrawablesWithIntrinsicBounds(serviceSupport.accelModeId, 0, 0, 0);
			normalUserView.setImageResource(serviceSupport.normalUser);

//			TickView tickView = new TickView(AppMain.getContext());
//			LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.item_service_introduce, null, false);
//			linearLayout.addView(tickView);

			if(UserSession.isLogined() && UserSession.getInstance().vipStatus()) {
				vipUserView.setVisibility(View.GONE);
				normalUserView.setVisibility(View.GONE);
				if("普通加速".equals(serviceSupport.accelMode)) {
					tickView.setVisibility(View.VISIBLE);
					tickAniView.setVisibility(View.GONE);
					return;
				}
				tickAniView.setVisibility(View.VISIBLE);
				tickAniView.startAnimation();
			}
		}
	}

	public static class ServiceSupport {
		private final String accelMode;
		private final int normalUser;
		private final int accelModeId;

		public ServiceSupport(String accelMode, int normalUser, int accelModeId) {
			this.accelMode = accelMode;
			this.normalUser = normalUser;
			this.accelModeId = accelModeId;
		}
	}
}
