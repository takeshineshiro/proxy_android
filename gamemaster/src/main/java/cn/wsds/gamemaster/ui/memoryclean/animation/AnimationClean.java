package cn.wsds.gamemaster.ui.memoryclean.animation;

public interface AnimationClean {

	/** 执行 */
	public void execute(OnAnimationEndListener onAnimationEndListener);
	/** 中断 */
	public void interrupt();
	
}
