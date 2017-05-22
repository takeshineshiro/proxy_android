package cn.wsds.gamemaster.ui.adapter;

import java.util.List;

import android.support.v4.view.PagerAdapter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.ui.adapter.gamegrid.factory.GameGridCreater;
/**
 * 游戏网络页面
 */
public class GamePageAdapter extends PagerAdapter{
	private final int pageNum;
	private final int columnsPerPage;
	private final int iconsPerPage;
	private final List<GameInfo> gameWholeList;
	private OnItemClickListener listener;
	private GameGridCreater creater;
	
	/** 每页最多有几列图标 */
	public static final int COLUMNS_PER_PAGE = 4;

	/** 每页最多有几行图标 */
	public static final int ROWS_PER_PAGE = 2;

	/** 每页图标个数 */
	public static final int ICONS_PER_PAGE = ROWS_PER_PAGE * COLUMNS_PER_PAGE;
	
	public GamePageAdapter(List<GameInfo> gameWholeList,GameGridCreater creater) {
		this(calcPages(gameWholeList.size()), COLUMNS_PER_PAGE, ICONS_PER_PAGE, gameWholeList,creater);
	}

	public GamePageAdapter(int pageNum, int columnsPerPage, int iconsPerPage,List<GameInfo> gameWholeList, GameGridCreater creater) {
		if(creater==null){
			throw new RuntimeException("GameGridAdpterCreater not null");
		}
		this.pageNum = pageNum;
		this.columnsPerPage = columnsPerPage;
		this.iconsPerPage = iconsPerPage;
		this.gameWholeList = gameWholeList;
		this.creater = creater;
	}
	
	public void setOnItemClickListener(OnItemClickListener listener) {
		this.listener = listener;
	}

	public static int calcPages(int icon_count) {
		return (icon_count + ICONS_PER_PAGE - 1) / ICONS_PER_PAGE;
	}
	
	@Override
	public int getCount() {
		return this.pageNum;
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0 == arg1;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeViewInLayout((View) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		GridView gridView = creater.createPerPage(container.getContext());
		gridView.setNumColumns(this.columnsPerPage);
		gridView.setGravity(Gravity.CENTER);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		gridView.setLayoutParams(layoutParams);
		gridView.setOnItemClickListener(listener);
		gridView.setAdapter(createGridAdapter(position));
		container.addView(gridView);
		return gridView;
	}

	private BaseAdapter createGridAdapter(int position) {
		int start = position * this.iconsPerPage;
		int end = Math.min((position + 1) * this.iconsPerPage, gameWholeList.size());
		List<GameInfo> infos = gameWholeList.subList(start, end);
		return creater.createPerGridAdapter(infos);
	}

	public int getPageNum() {
		return this.pageNum;
	}
}
