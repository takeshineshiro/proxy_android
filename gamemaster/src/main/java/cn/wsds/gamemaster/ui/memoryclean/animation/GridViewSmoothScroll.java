package cn.wsds.gamemaster.ui.memoryclean.animation;

import android.view.ViewGroup;
import android.widget.TextView;

/**
 * 应用图标网格列表平行滑动
 */
class GridViewSmoothScroll implements AnimationClean{
	private ViewGroup gridView;
	private int per;
	private static final int PER_COUNT = 3;
	private int totalCount;
	private Runnable action = new Runnable() {
		
		@Override
		public void run() {
			smooth();
		}
	};
	
	private OnAnimationEndListener onAnimationEndListener;
	private TextView textLabel;
	private int scorllHeight;
	public GridViewSmoothScroll (ViewGroup gameGridView,TextView textLabel) {
		this.gridView = gameGridView;
		this.textLabel = textLabel;
	}
	
	@Override
	public void execute(OnAnimationEndListener onAnimationEndListener) {
		this.onAnimationEndListener = onAnimationEndListener;
		textLabel.setText("正在清理...");
		smooth();
	}

	private void smooth() {
		if(gridView.getHeight()<=0){
			gridView.postDelayed(action,200);
			return;
		}
		if(gridView.getChildCount() == 0){
			end();
			return;
		}
		
		if(per==0){
			per = gridView.getChildAt(0).getMeasuredHeight() / PER_COUNT + 1;
		}
		scorllHeight+= per;
		gridView.scrollBy(0, per);
		totalCount ++;
		if(scorllHeight > gridView.getMeasuredHeight()){
			end();
		}else{
			int delayMillis = totalCount % PER_COUNT == 0 ? 300 : 80;
			gridView.postDelayed(action,delayMillis);
		}
	}

	private void end() {
		interrupt();
		textLabel.setText("已完成清理");
		if(onAnimationEndListener!=null){
			onAnimationEndListener.onAnimationEnd();
		}
	}

	@Override
	public void interrupt() {
		gridView.removeCallbacks(action);
	}
}