package cn.wsds.gamemaster.ui.pullrefresh;

import android.content.Context;
import android.util.AttributeSet;

import cn.wsds.gamemaster.ui.UIUtils;
import in.srain.cube.views.ptr.PtrFrameLayout;

/**
 * Created by hujd on 16-11-17.
 */
public class PtrSubaoFrameLayout extends PtrFrameLayout {
	public PtrSubaoFrameLayout(Context context) {
		super(context);
		initView();
	}

	public PtrSubaoFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public PtrSubaoFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		SubaoHeader subaoHeader = new SubaoHeader(getContext());
		subaoHeader.setPadding(0, UIUtils.dp2px(15), 0, UIUtils.dp2px(10));
		setHeaderView(subaoHeader);
		addPtrUIHandler(subaoHeader);
	}
}
