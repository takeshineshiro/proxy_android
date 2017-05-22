package cn.wsds.gamemaster.ui.adapter.gamegrid.factory;

import java.util.List;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.GridView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.ui.adapter.gamegrid.AchieveGameGridAdapter;

/**
 * 创建“游戏使用成就”网络适配器
 */
public class AchieveGameGridAdpterCreater implements GameGridCreater{

	@Override
	public BaseAdapter createPerGridAdapter(List<GameInfo> infos) {
		return new AchieveGameGridAdapter(AchieveGameGridAdapter.formatGameInfoToDrawable(infos));
	}

	@Override
	public GridView createPerPage(Context context) {
		GridView gridView = new GridView(context);
		gridView.setVerticalSpacing((int) context.getResources().getDimension(R.dimen.space_size_16));
		gridView.setSelector(R.drawable.transparent);
		return gridView;
	}
	
}
