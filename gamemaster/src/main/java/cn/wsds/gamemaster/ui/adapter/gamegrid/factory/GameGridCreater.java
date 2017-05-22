package cn.wsds.gamemaster.ui.adapter.gamegrid.factory;

import java.util.List;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.GridView;
import cn.wsds.gamemaster.app.GameInfo;

public interface GameGridCreater {
	/**
	 * 创建单页的网格适配器
	 * @param infos 当前页显示的数据
	 * @return
	 */
	public BaseAdapter createPerGridAdapter(List<GameInfo> infos);
	/**
	 * 
	 * @param context
	 * @return
	 */
	public GridView createPerPage(Context context);
}
