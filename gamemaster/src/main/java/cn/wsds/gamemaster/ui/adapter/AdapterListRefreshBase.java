package cn.wsds.gamemaster.ui.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

/**
 * 刷新列表的适配器
 * @param <T> 数据实体
 */
public abstract class AdapterListRefreshBase<T> extends BaseAdapter {

	private enum State {
		/** 加载状态 */
		loading,
		/** 没有更多数据状态 */
		noMore,
		/** 初始状态或成功完成加载状态 */
		nothing;
	}

    private State state;
    private final FooterView footerView;
    private final ArrayList<T> mDatas = new ArrayList<T>();

    public AdapterListRefreshBase(Context context) {
		this.footerView = new FooterView(View.inflate(context, R.layout.load_more_footer, null));
		setState(State.nothing);
    }

    @Override
	public final int getCount() {
    	if(!isLoadMore()){
    		return getDataSize();
    	}
    	if(getDataSize() == 0){
    		return 0;
    	}
    	return getDataSize() + 1;
	}

    public final int getDataSize() {
        return mDatas.size();
    }

    @Override
    public long getItemId(int arg0) {
		return arg0;
    }
    
    @Override
    public T getItem(int position) {
		if (position >= 0 && position < mDatas.size()) {
			return mDatas.get(position);
		} else {
			return null;
		}
    }
    
    private void setState(State state) {
    	if(this.state == state){
    		return;
    	}
    	this.state = state;
    	switch (this.state) {
		case nothing:
			this.footerView.setVisibility(View.GONE);
			break;
		case loading:
			setFooterViewLoading();
			break;
		case noMore:
		default:
			setFooterViewText(R.string.points_hirstory_nodata);
			break;
		}
    }

    /**
     * 重置列表数据
     * @param data
     */
    public final void setData(List<T> data) {
    	mDatas.clear();
    	setState(State.nothing);
		if (data == null || data.isEmpty()) {
			notifyDataSetChanged();
			return;
		}
    	doAddData(data);
    	notifyDataSetChanged();
    }
    
	/**
	 * 添加数据
	 * 
	 * @param data
	 */
	public final void addData(List<T> data) {
		if (data == null || data.isEmpty()) {
			setState(State.noMore);
		} else {
			setState(State.nothing);
			doAddData(data);
			notifyDataSetChanged();
		}
	}

	/**
	 * 将数据添加到列表上
	 * @param data
	 */
	protected void doAddData(List<T> data){
		mDatas.addAll(data);
	}
	
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	if(!isLoadMore()){
    		return getRealView(position, convertView, parent);
    	}
    	
        if (position == getCount() - 1) {// 最后一条
            return getFooterView();
        }
        if (position < 0) {
            position = 0; // 若列表没有数据，是没有footview/headview的
        }
        
        if(convertView!=null && R.id.load_more_footer == convertView.getId()){
        	// 不显示 footView 时，如果 convertView 是 footView 是不可复用的
        	convertView = null;
        }
        return getRealView(position, convertView, parent);
    }

    protected abstract View getRealView(int position, View convertView, ViewGroup parent);
    
    public final View getFooterView() {
        return this.footerView.group;
    }
    
    /**
     * 是否可以加载
     * @return
     */
    public final boolean canLoading(){
    	return this.state == State.nothing;
    }
    
    private void setFooterViewLoading(){
    	this.footerView.setVisibility(View.VISIBLE);
		startAnimation();
    	footerView.textView.setVisibility(View.GONE);
        footerView.refreshView.setVisibility(View.VISIBLE);
    }

    private void setFooterViewText(String msg) {
    	this.footerView.setVisibility(View.VISIBLE);
    	footerView.textView.setText(msg);
    	footerView.textView.setVisibility(View.VISIBLE);
		stopAnimation();
		footerView.refreshView.setVisibility(View.GONE);
    }

	private void startAnimation() {
		AnimationDrawable drawable = (AnimationDrawable) footerView.refreshView.getDrawable();
		if(drawable != null) {
			drawable.start();
		}
	}

	private void stopAnimation() {
		AnimationDrawable drawable = (AnimationDrawable) footerView.refreshView.getDrawable();
		if(drawable != null) {
			drawable.stop();
		}
	}


	private void setFooterViewText(int msg) {
    	setFooterViewText(footerView.textView.getResources().getString(msg));
    }

    private static final class FooterView {
    	private final TextView textView;
    	private final View group;
    	private final ImageView refreshView;
		private FooterView(View group) {
			this.group = group;
			this.textView = (TextView) group.findViewById(R.id.text);
			this.refreshView = (ImageView) group.findViewById(R.id.progressbar);
		}
		public void setVisibility(int visibility){
			this.group.setVisibility(visibility);
		}
    }

    /**
     * 显示开始加载的样式
     */
	public final void onLoading() {
		if(State.nothing == this.state){
			setState(State.loading);
		}
	}
	
	/**
	 * 显示加载结束的样式
	 */
	public final void loadComplete(){
		if(State.loading == this.state){
			setState(State.nothing);
		}
	}

	/**
	 * 是否可以加载更多
	 * @return
	 */
	public boolean isLoadMore() {
		return true;
	}
	
	/**
	 * 移除某条数据
	 * @param item
	 */
	public final void removeItem(T item){
		mDatas.remove(item);
		notifyDataSetChanged();
	}
}