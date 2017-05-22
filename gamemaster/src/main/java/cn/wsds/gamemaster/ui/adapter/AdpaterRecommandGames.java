package cn.wsds.gamemaster.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.RecommandGameInfo;
import cn.wsds.gamemaster.tools.SmallImageLoader;

public class AdpaterRecommandGames extends AdapterListRefreshBase<RecommandGameInfo>{

	private Context context;
	
	private static final String testIconURL = "http://images.csdn.net/20130609/zhuanti.jpg" ;
	
	public interface DownloadClickListener{
		public void OnClickDownloadButton(View parentView, String url ,RecommandGameInfo gameInfo);		
	}
	
	private DownloadClickListener downloadClickListerner;
	
	public AdpaterRecommandGames(Context context) {
		super(context);
		this.context = context ;
	}
	
	public void setDownloadClickListener(DownloadClickListener downloadClickListerner){
		this.downloadClickListerner = downloadClickListerner ;	
	}

	@Override
	protected View getRealView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null ;
		if(convertView == null){
			convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommand_game,
	                		 parent,false);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder)convertView.getTag();
		}
	
		RecommandGameInfo item = getItem(position);
		if(item!=null){
			String url = item.getIconUrl();
			if(url!=null){
				SmallImageLoader.getInstance(context).displayImage(url, holder.icon);
			}else{
				SmallImageLoader.getInstance(context).displayImage(testIconURL, holder.icon);
			}
			
			holder.name.setText(item.getName());
			holder.reason.setText(item.getReason());
			String packageUrl = item.getPackageUrl();
			holder.download.setOnClickListener(new OnDownloadClickListernerImpl(convertView,packageUrl,item));
		}
		return convertView;
	}
	
	private final class OnDownloadClickListernerImpl implements View.OnClickListener{

		private final View convertView;
		private final String packageUrl;
		private final RecommandGameInfo gameInfo ;

		public OnDownloadClickListernerImpl(View convertView, String packageUrl,RecommandGameInfo gameInfo) {
			this.convertView = convertView;
			this.packageUrl = packageUrl;
			this.gameInfo = gameInfo ;
		}

		@Override
		public void onClick(View v) {
			if(downloadClickListerner!=null){
				downloadClickListerner.OnClickDownloadButton(convertView, packageUrl,gameInfo);
			}		
		}		
	}
	
	private static final class  ViewHolder{
		private final ImageView icon;
		private final TextView name;
		private final TextView reason;
		private final TextView download;
		
		public ViewHolder(View convertView){
			this.icon = (ImageView)convertView.findViewById(R.id.image_icon);
			this.name = (TextView)convertView.findViewById(R.id.text_name);
			this.reason = (TextView)convertView.findViewById(R.id.text_recommand_reason);
			this.download = (TextView)convertView.findViewById(R.id.button_download);
		}
	}
	
}
