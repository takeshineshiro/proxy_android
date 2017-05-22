package cn.wsds.gamemaster.ui.adapter.gamegrid.factory;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.widget.BaseAdapter;
import android.widget.GridView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.ui.adapter.gamegrid.ShortcutGameGridAdapter;

/**
 * 创建“桌面快捷方式”网格适配器
 */
public class ShortcutGameGridAdpterCreater implements GameGridCreater{

	@Override
	public BaseAdapter createPerGridAdapter(List<GameInfo> infos) {
		return new ShortcutGameGridAdapter(infos);
	}

	@Override
	public GridView createPerPage(Context context) {
		GridView gridView = new GridView(context);
		Resources resources = context.getResources();
		gridView.setPadding((int) resources.getDimension(R.dimen.space_size_20), (int) resources.getDimension(R.dimen.space_size_22),
			(int) resources.getDimension(R.dimen.space_size_20), (int) resources.getDimension(R.dimen.space_size_16));
		gridView.setHorizontalSpacing((int) resources.getDimension(R.dimen.space_size_14));
		gridView.setVerticalSpacing((int) resources.getDimension(R.dimen.space_size_8));
		gridView.setSelector(R.drawable.selector_desktop_shortcut);
		return gridView;
	}
	
}
 