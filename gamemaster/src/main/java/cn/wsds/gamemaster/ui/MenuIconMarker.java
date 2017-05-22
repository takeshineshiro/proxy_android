package cn.wsds.gamemaster.ui;

import android.app.Activity;
import android.view.MenuItem;

/**
 * Menu图标标注
 */
public abstract class MenuIconMarker {

	private Activity activity;

	/**
	 * 挂接Activity。
	 * <p>
	 * <b>只能被调用一次</b>，通常在Activity的onCreate()事件里执行
	 * </p>
	 */
	public final void attachActivity(Activity activity) {
		if (this.activity != null) {
			throw new IllegalStateException();
		}
		this.activity = activity;
		onAttachActivity();
	}

	/**
	 * 
	 */
	public final void detachActivity() {
		if (this.activity != null) {
			onDetachActivity();
			this.activity = null;
		}
	}

	protected final void refreshMenu() {
		if (this.activity != null) {
			this.activity.invalidateOptionsMenu();
		}
	}

	/**
	 * 当Activity的onPrepareOptionsMenu事件发生时，调根据当前情况重新设置MenuItem的图标
	 */
	public void resetMenuIcon(MenuItem mi) {
		recheckState();
		mi.setIcon(isStateStriking() ? getResIdStriking() : getResIdNormal());
	}

	/** 当执行{@link #attachActivity(Activity)}的时候被调用 */
	protected abstract void onAttachActivity();

	/** 当执行{@link #detachActivity()}的时候被调用 */
	protected abstract void onDetachActivity();

	/**
	 * 重新检查状态
	 */
	protected abstract void recheckState();

	/**
	 * 当前是醒目状态吗？
	 */
	protected abstract boolean isStateStriking();

	/** 常规状态下的图标资源ID */
	protected abstract int getResIdNormal();

	/** 醒目状态下的图标资源ID */
	protected abstract int getResIdStriking();

}
