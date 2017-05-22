package cn.wsds.gamemaster.dialog;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import cn.wsds.gamemaster.R;

/**
 * Created by hujd on 16-8-25.
 */
public class CommonCheckBoxDialog extends CommonDialog {

	private final CheckBox checkBoxPrompt;
	private final TextView textMessage;
	private Activity activity;

	public CommonCheckBoxDialog(Activity context) {
		this(context, R.style.AppDialogTheme);
	}
	protected CommonCheckBoxDialog(Activity context, int theme) {
		super(context, theme);
		this.setContentView(layout);
		this.activity = context;
		View view = inflateCustomLayout(R.layout.prompt_dialog);
		checkBoxPrompt = (CheckBox) view.findViewById(R.id.check_prompt);
		textMessage = (TextView) view.findViewById(R.id.text_message);
	}

	@Override
	public void setMessage(CharSequence message) {
		textMessage.setText(message);
	}

	public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
		checkBoxPrompt.setOnCheckedChangeListener(listener);
	}

	@Override
	public void show() {
		if (this.activity != null) {
			if (this.activity.isFinishing()) {
				this.activity = null;
			} else {
				super.show();
			}
		}
	}
	
	public void hideCheckBox(){
		checkBoxPrompt.setVisibility(View.GONE);
	}
}
