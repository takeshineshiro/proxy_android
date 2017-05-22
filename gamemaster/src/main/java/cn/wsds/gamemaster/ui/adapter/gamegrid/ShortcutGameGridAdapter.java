package cn.wsds.gamemaster.ui.adapter.gamegrid;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;

public class ShortcutGameGridAdapter extends BaseAdapter{
	private final LayoutInflater layoutInflater;
	private final List<GameInfo> gameList;

	public ShortcutGameGridAdapter(List<GameInfo> gameList) {
		layoutInflater = LayoutInflater.from(AppMain.getContext());
		this.gameList = gameList;
	}

	@Override
	public int getCount() {
		return gameList.size();
	}

	@Override
	public Object getItem(int position) {
		return gameList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.layout_shortcut_item, parent,false);
			viewHolder = new ViewHolder();
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.shortcut_game_icon);
			viewHolder.label = (TextView) convertView.findViewById(R.id.shortcut_game_label);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		setResources(viewHolder, position);
		return convertView;
	}

	private void setResources(ViewHolder viewHolder, int position) {
		GameInfo gameInfo = (GameInfo) getItem(position);
		viewHolder.icon.setImageDrawable(gameInfo.getAppIcon(AppMain.getContext()));
		viewHolder.label.setText(gameInfo.getAppLabel());
	}
	
	private static class ViewHolder {
		private ImageView icon;
		private TextView label;
	}
}
