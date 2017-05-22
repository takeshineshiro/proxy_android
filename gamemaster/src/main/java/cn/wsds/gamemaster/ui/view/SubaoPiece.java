package cn.wsds.gamemaster.ui.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.subao.common.Disposable;

public class SubaoPiece implements Disposable {

	private ViewGroup parent;
	private View view;

	public SubaoPiece(ViewGroup parent, int layoutResId) {
		this(parent, layoutResId, LayoutInflater.from(parent.getContext()));
	}

	public SubaoPiece(ViewGroup parent, int layoutResId, LayoutInflater inflater) {
		this.parent = parent;
		this.view = inflater.inflate(layoutResId, parent, false);
		this.parent.addView(view);
	}

	@Override
	public void dispose() {
		if (parent != null) {
			parent.removeView(view);
			parent = null;
			view = null;
		}
	}

	public View getView() {
		return this.view;
	}

}
