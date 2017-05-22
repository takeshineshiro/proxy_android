package cn.wsds.gamemaster.ui.accel.progress;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import cn.wsds.gamemaster.R;

/**
 * 加速启动流程动画
 */
public class AccelProgress extends RelativeLayout {

	private static int instanceCount;
	
	public static int getInstanceCount() {
		return instanceCount;
	}
	
	private final List<AccelProgressGroup> groups = new ArrayList<AccelProgressGroup>(4);
	private boolean abortFlag;
	
	private class MyAniListener implements AccelProgressIcon.IconAniListener {

		private final AniListener aniListener;
		private int idxCurrentGroup;
		
		public MyAniListener(AniListener aniListener) {
			this.aniListener = aniListener;
		}

		@Override
		public void onAniEnd(Object sender) {
			if (abortFlag) { return; }
			++idxCurrentGroup;
			if (idxCurrentGroup < groups.size()) {
				groups.get(idxCurrentGroup).startAni(this);
				return;
			} else if (aniListener != null) {
				aniListener.onAniEnd(AccelProgress.this);
			}
		}
		
		@Override
		public void onAniAbort(Object sender) {
			if (aniListener != null) {
				aniListener.onAniAbort(AccelProgress.this);
			}
		}

		@Override
		public void onAniZoom(AccelProgressIcon sender, float scale) {
			if (abortFlag) { return; }
			if (idxCurrentGroup > 0) {
				AccelProgressGroup groupPrev = groups.get(idxCurrentGroup - 1);
				groupPrev.deflateBottom(scale);
			}

		}
	}

	public AccelProgress(Context context) {
		this(context, null);
	}
	
	public AccelProgress(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflate(context, R.layout.accel_progress, this);
		AccelProgressGroup group = (AccelProgressGroup) findViewById(R.id.group_my_net);
		group.setBoxType(AccelProgressBox.Type.LocalNetChecker);
		groups.add(group);
		groups.add((AccelProgressGroup) findViewById(R.id.group_cloud));
		group = (AccelProgressGroup) findViewById(R.id.group_server);
		group.setBoxType(AccelProgressBox.Type.AccelEffectPercent);
		groups.add(group);
		group = (AccelProgressGroup) findViewById(R.id.group_over);
		group.setBoxType(AccelProgressBox.Type.ACCEL_OVER);
		groups.add(group);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		++instanceCount;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		--instanceCount;
	}

	/**
	 * 开始动画流程
	 */
	public void startAni(AniListener aniListener) {
		AccelProgressGroup group = groups.get(0);
		if (group.getVisibility() != View.VISIBLE) {
			group.startAni(new MyAniListener(aniListener));
		}
	}
	
	@Override
	public void setAlpha(float alpha) {
		super.setAlpha(alpha);
		for (int i = getChildCount() - 1; i >= 0; --i) {
			getChildAt(i).setAlpha(alpha);
		}
	}

	/**
	 * 中断
	 */
	public void abort() {
		if (abortFlag) { return; }
		abortFlag = true;
		for (AccelProgressGroup group : groups) {
			group.abort();
		}
	}
}
