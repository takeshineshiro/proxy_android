package cn.wsds.gamemaster.ui.adapter.gamegrid;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;

import com.subao.data.InstalledAppInfo;

public class AchieveGameGridAdapter extends BaseAdapter{
	private final List<Drawable> listDrawables = new ArrayList<Drawable>();
	public static List<Drawable> formatGameInfoToDrawable(List<GameInfo> gameInfos){
		List<Drawable> listDrawables = new ArrayList<Drawable>();
		Context context = AppMain.getContext();
		for (GameInfo gameInfo : gameInfos) {
			listDrawables.add(gameInfo.getAppIcon(context));
		}
		return listDrawables;
	}
	
	public static List<Drawable> formatInstallInfoToDrawable(List<InstalledAppInfo> installedApps){
		List<Drawable> listDrawables = new ArrayList<Drawable>();
		Context context = AppMain.getContext();
		for (InstalledAppInfo installedAppInfo : installedApps) {
			listDrawables.add(installedAppInfo.getAppIcon(context));
		}
		return listDrawables;
	}
	
	public AchieveGameGridAdapter(List<Drawable> listDrawables){
		this.listDrawables.addAll(listDrawables);
	}

	@Override
	public int getCount() {
		return listDrawables.size();
	}

	@Override
	public Object getItem(int position) {
		return listDrawables.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = createView();
		}
		((ImageView) convertView).setImageDrawable(listDrawables.get(position));
		return convertView;
	}

	private View createView() {
		Context context = AppMain.getContext();
		ImageView convertView = new ImageView(context);
		Resources resources = context.getResources();
		int width = resources.getDimensionPixelSize(R.dimen.space_size_48);
		int height = resources.getDimensionPixelSize(R.dimen.space_size_48);
		LayoutParams params = new LayoutParams(width, height);
		convertView.setLayoutParams(params);
		return convertView;
	}
}
