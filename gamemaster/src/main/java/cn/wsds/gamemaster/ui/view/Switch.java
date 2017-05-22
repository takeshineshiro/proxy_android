package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import cn.wsds.gamemaster.R;

//import com.subao.gamemaster.ui.view.Switch.OnChangedListener;

public class Switch extends ImageView {

	public static interface OnChangedListener {
		// 当状态修改时，通知监听者
		void onCheckedChanged(Switch checkSwitch, boolean checked);
	}

	private final Drawable checkbutton;
	private final Drawable uncheckbutton;

	public static interface OnBeforeCheckChangeListener {
		/**
		 * Switch被点击，准备改变Check状态之前被调用
		 * 
		 * @param checkSwitch
		 *            控件本身
		 * @param expectation
		 *            预期将要变到哪个状态
		 * @return True表示允许改变，False表示不到改变
		 */
		boolean onBeforeCheckChange(Switch checkSwitch, boolean expectation);
	}

	private boolean checked;
	private OnChangedListener changeListener;
	private OnBeforeCheckChangeListener beforeCheckChangeListener;

	@SuppressWarnings("deprecation")
	public Switch(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (attrs == null) {
			checkbutton = getResources().getDrawable(R.drawable.common_switch_on);
			uncheckbutton = getResources().getDrawable(R.drawable.common_switch_off);
		} else {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Switch);
			Drawable defcheckbutton = ta.getDrawable(R.styleable.Switch_checkbutton);
			Drawable defuncheckbutton = ta.getDrawable(R.styleable.Switch_uncheckbutton);
			checkbutton = defcheckbutton == null ? getResources().getDrawable(R.drawable.common_switch_on)
				: defcheckbutton;
			uncheckbutton = defuncheckbutton == null ? getResources().getDrawable(R.drawable.common_switch_off)
				: defuncheckbutton;
			ta.recycle();
		}
		setImageDrawable(uncheckbutton);
		setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				boolean expectChecked = !checked;
				if (beforeCheckChangeListener != null) {
					if (!beforeCheckChangeListener.onBeforeCheckChange(Switch.this, expectChecked)) {
						return;
					}
				}
				setChecked(expectChecked);
				if (changeListener != null) {
					changeListener.onCheckedChanged(Switch.this, expectChecked);
				}
			}
		});
	}

	public Switch(Context context) {
		this(context, null);
	}

	public Switch(Context context, AttributeSet attrs, int defStyle) {
		this(context, attrs);
	}

	/**
	 * 设置开关状态
	 * <p>
	 * 注意，调用本函数来设置的开关状态，不会触发
	 * {@link OnChangedListener#onCheckedChanged(Switch, boolean)}事件，也不会预先检查
	 * {@link OnBeforeCheckChangeListener#onBeforeCheckChange(Switch, boolean)}
	 * </p>
	 * 
	 * @param expectChecked
	 *            期望设置的值，true=开，false=关
	 * @return 是否改变了原来的开关状态
	 */
	public boolean setChecked(boolean expectChecked) {
		if (this.checked != expectChecked) {
			this.checked = expectChecked;
			setImageDrawable(this.checked ? checkbutton : uncheckbutton);
			return true;
		}
		return false;
	}

	public boolean isChecked() {
		return checked;
	}

	// 设置监听器,当状态修改时，通知监听者
	public void setOnChangedListener(OnChangedListener l) {
		changeListener = l;
	}

	public void setOnBeforeCheckChangeListener(OnBeforeCheckChangeListener l) {
		this.beforeCheckChangeListener = l;
	}
}
