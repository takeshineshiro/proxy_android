package cn.wsds.gamemaster.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources.NotFoundException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

import com.subao.utils.StringUtils;

@SuppressLint("InflateParams") 
public abstract class CommonDialog extends Dialog {

	public final View layout;//对话框布局组件
	private final TextView title, message;//标题和内容组件
	private final TextView buttonPositive, buttonNegative;//按钮
	private final View groupPositive, groupNegative;//按钮外层父控件
	private final View spaceLeft, spaceRight;//按钮左右间距空间控件
	private final ImageView imageView;

	private ViewStub customStub;

	private final ButtonClickListener buttonClickListener = new ButtonClickListener();

	private DialogInterface.OnClickListener positiveButtonClickListener;
	private DialogInterface.OnClickListener negativeButtonClickListener;

	/**
	 * 按钮点击的时候对话框要不要关闭 默认为关闭
	 */
	private boolean dismissWhenButtonClick = true;
	private int id;
	public boolean cancelOnWindowLoseFocused;

//	public CommonDialog(Activity context) {
//		this(context, R.style.AppDialogTheme);
//	}

	protected CommonDialog(Context context, int theme) {
		super(context, theme);
		//实例化组件
		layout = LayoutInflater.from(context).inflate(R.layout.dialog, null);
		title = (TextView) layout.findViewById(R.id.text_title);
		message = (TextView) layout.findViewById(R.id.text_mess);
		//		buttonRegion = layout.findViewById(R.id.button);
		groupPositive = layout.findViewById(R.id.confirm_group);
		groupNegative = layout.findViewById(R.id.cancel_group);
		spaceLeft = layout.findViewById(R.id.space_left);
		spaceRight = layout.findViewById(R.id.space_right);
		buttonPositive = (TextView) layout.findViewById(R.id.button_confirm);
		buttonNegative = (TextView) layout.findViewById(R.id.button_cancel);
		imageView = (ImageView) layout.findViewById(R.id.show_image);
		customStub = (ViewStub) layout.findViewById(R.id.custom_stub);
	}

	public void setImage(int resId) {
		imageView.setVisibility(View.VISIBLE);
		imageView.setImageResource(resId);
	}

	/**
	 * 设置对话框ID
	 * 
	 * @param id
	 *            对话框id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * 读取对话框ID
	 * 
	 * @return 默认为0，否则为setId的值
	 */
	public int getId() {
		return id;
	}

	/**
	 * 设置：点击按钮时，是否自动Dissmiss。缺省为True
	 * 
	 * @param value
	 */
	public void setDismissWhenButtonClick(boolean value) {
		this.dismissWhenButtonClick = value;
	}

	/**
	 * @return 点击按钮时，是否自动Dissmiss
	 */
	public boolean getDismissWhenButtonClick() {
		return this.dismissWhenButtonClick;
	}

	/**
	 * Set the title text for this dialog's window.
	 * 
	 * @param title
	 *            The new text to display in the title.
	 */
	@Override
	public void setTitle(CharSequence message) {
		if (message == null) {
			this.title.setText("提示");
		} else {
			this.title.setText(message);
		}
	}

	/**
	 * Set the title text for this dialog's window. The text is retrieved from
	 * the resources with the supplied identifier.
	 * 
	 * @param titleId
	 *            the title's text resource identifier
	 */
	@Override
	public void setTitle(int message) {
		this.title.setText(message);
	}

	/**
	 * 设置对话框内容
	 * 
	 * @param message
	 *            文本信息
	 */
	public void setMessage(CharSequence message) {
		this.message.setText(StringUtils.replaceIfNull(message));
	}

	/**
	 * 设置对话框内容
	 * 
	 * @param message
	 *            文本资源
	 * @throws NotFoundException
	 *             Throws NotFoundException if the given ID does not exist.
	 */
	public void setMessage(int message) {
		this.message.setText(message);
	}

	/**
	 * 设置按钮
	 * 
	 * @param button
	 *            按钮实例
	 * @param text
	 *            按钮文本
	 */
	private void setButton(TextView button, CharSequence text) {
		button.setText(text);
		button.setOnClickListener(this.buttonClickListener);
	}

	/**
	 * set negative button and click event
	 * 
	 * @param text
	 * @param listener
	 */
	public void setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
		this.negativeButtonClickListener = listener;
		setButton(this.buttonNegative, text);
	}

	public void setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
		this.positiveButtonClickListener = listener;
		setButton(this.buttonPositive, text);
	}

	public void setPositiveButton(int res, DialogInterface.OnClickListener listener) {
		this.positiveButtonClickListener = listener;
		setButton(this.buttonPositive, res);
	}

	public void setNegativeButton(int res, DialogInterface.OnClickListener listener) {
		this.negativeButtonClickListener = listener;
		setButton(this.buttonNegative, res);
	}

	private void setButton(TextView button, int res) {
		button.setText(res);
		button.setOnClickListener(this.buttonClickListener);
	}

	/**
	 * 按钮点击监听
	 */
	private class ButtonClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_confirm:// positive button click event
				if (positiveButtonClickListener != null) {
					positiveButtonClickListener.onClick(CommonDialog.this, DialogInterface.BUTTON_POSITIVE);
				}
				break;
			case R.id.button_cancel:// negative button click event
				if (negativeButtonClickListener != null) {
					negativeButtonClickListener.onClick(CommonDialog.this, DialogInterface.BUTTON_NEGATIVE);
				}
				break;
			default:
				return;
			}
			// 判断是否需要dimiss
			if (dismissWhenButtonClick) {
				dismiss();
			}
		}

	}

	/**
	 * 根据设置的按钮文本内容，设置文本界面布局
	 */
	protected final void adjustButtonLayout() {
		boolean visiblePositionButton = !TextUtils.isEmpty(this.buttonPositive.getText());
		boolean visibleNegativeButton = !TextUtils.isEmpty(this.buttonNegative.getText());
		if (!visiblePositionButton) {
			groupPositive.setVisibility(View.GONE);
			spaceVisible(false);
		}
		
		if (!visibleNegativeButton) {
			groupNegative.setVisibility(View.GONE);
			spaceVisible(true);
		}
	}

	private void spaceVisible(boolean visible) {
		int visibleValue = visible ? View.VISIBLE : View.GONE;
		spaceLeft.setVisibility(visibleValue);
		spaceRight.setVisibility(visibleValue);
	}

//	public static class Builder {
//		private CharSequence title, message;
//
//		private CharSequence positiveButtonText;
//		private DialogInterface.OnClickListener positiveButtonClickListener;
//
//		private CharSequence negativeButtonText;
//		private DialogInterface.OnClickListener negativeButtonClickListener;
//
//		private DialogInterface.OnDismissListener onDismissListener;
//
//		public Builder setTitle(Context context, int resId) {
//			return this.setTitle(context.getText(resId));
//		}
//
//		public Builder setTitle(CharSequence title) {
//			this.title = title;
//			return this;
//		}
//
//		public Builder setMessage(CharSequence message) {
//			this.message = message;
//			return this;
//		}
//
//		public Builder setMessage(Context context, int stringResId) {
//			this.message = context.getText(stringResId);
//			return this;
//		}
//
//		public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
//			this.positiveButtonText = text;
//			this.positiveButtonClickListener = listener;
//			return this;
//		}
//
//		public Builder setPositiveButton(Context context, int resId, DialogInterface.OnClickListener listener) {
//			return setPositiveButton(context.getResources().getText(resId), listener);
//		}
//
//		public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
//			this.negativeButtonText = text;
//			this.negativeButtonClickListener = listener;
//			return this;
//		}
//
//		public Builder setNegativeButton(Context context, int resId, DialogInterface.OnClickListener listener) {
//			return setNegativeButton(context.getResources().getText(resId), listener);
//		}
//
//		public Builder setOnDismissListener(DialogInterface.OnDismissListener onDismissLisener) {
//			this.onDismissListener = onDismissLisener;
//			return this;
//		}
//
//		private boolean visibleNegativeButton() {
//			return !TextUtils.isEmpty(this.positiveButtonText);
//		}
//
//		private boolean visiblePositiveButton() {
//			return !TextUtils.isEmpty(this.positiveButtonText);
//		}
//
//		/**
//		 * 根据当前builder数据对 传入的对话框 属性添加
//		 * 
//		 * @return null 和 对话框
//		 */
//		public CommonDialog build(CommonDialog dialog) {
//			dialog.setTitle(this.title);
//			dialog.setMessage(this.message);
//			dialog.setPositiveButton(this.positiveButtonText, this.positiveButtonClickListener);
//			dialog.setNegativeButton(this.negativeButtonText, this.negativeButtonClickListener);
//			dialog.setOnDismissListener(this.onDismissListener);
//			dialog.judgeButtonVisiable(visiblePositiveButton(), visibleNegativeButton());
//			return dialog;
//		}
//
//	}

	public void setCanceledOnWindowLoseFocused(boolean cancel) {
		this.cancelOnWindowLoseFocused = cancel;
	}

	public void setContentSize(int width, int height) {
		LayoutParams layoutParams = this.message.getLayoutParams();
		layoutParams.width = width;
		layoutParams.height = height;
		this.message.setLayoutParams(layoutParams);
	}

	/**
	 * 嵌入自定义的布局主体，只能嵌一次。<br />
	 * 嵌入成功后，纯文本的主体文字会被隐藏
	 * @param resId 布局资源Id
	 * @return 如果成功，返回嵌入的布局主体，否则返回null。
	 */
	public View inflateCustomLayout(int resId) {
		if (customStub != null) {
			customStub.setLayoutResource(resId);
			View view = customStub.inflate();
			customStub = null;
			message.setVisibility(View.GONE);
			return view;
		}
		return null;
	}
	
	@Override
	public void show() {
		adjustButtonLayout();
		super.show();
	}
	
}
