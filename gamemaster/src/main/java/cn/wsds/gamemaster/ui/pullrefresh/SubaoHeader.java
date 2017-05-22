package cn.wsds.gamemaster.ui.pullrefresh;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import cn.wsds.gamemaster.R;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrUIHandler;
import in.srain.cube.views.ptr.indicator.PtrIndicator;

/**
 * subao 下拉刷新头
 * Created by hujd on 16-11-17.
 */
public class SubaoHeader extends FrameLayout implements PtrUIHandler {

	private AnimationDrawable mDrawable;
	private ImageView mImageView;

	public SubaoHeader(Context context) {
		super(context);
		initView();
	}

	public SubaoHeader(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public SubaoHeader(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		View viewHeader = LayoutInflater.from(getContext()).inflate(R.layout.ptr_subao_header, this);
		mImageView = (ImageView)viewHeader.findViewById(R.id.subao_refresh_view);
	}

	private void resetView() {
		mImageView.setVisibility(INVISIBLE);
	}

	@Override
	public void onUIReset(PtrFrameLayout frame) {
		resetView();
	}

	@Override
	public void onUIRefreshPrepare(PtrFrameLayout frame) {
		mImageView.setVisibility(VISIBLE);
		mDrawable = (AnimationDrawable)mImageView.getDrawable();
		if(mDrawable != null) {
			mDrawable.stop();
		}
	}

	@Override
	public void onUIRefreshBegin(PtrFrameLayout frame) {
		if(mDrawable != null) {
			mDrawable.start();
		}
	}

	@Override
	public void onUIRefreshComplete(PtrFrameLayout frame) {
		if(mDrawable != null) {
			mDrawable.stop();
		}
	}

	@Override
	public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {
		if(mDrawable != null) {
			mDrawable.start();
		}
	}
}
