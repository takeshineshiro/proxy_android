package cn.wsds.gamemaster.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.store.ActivityVip;

/**
 * Created by hujd on 17-2-22.
 */
public class AccelUpgradeDialog extends Dialog {
	private final Context context;

	private final View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			switch (view.getId()) {
				case R.id.button_to_vip:
					UIUtils.turnActivity(context, ActivityVip.class);
					AccelUpgradeDialog.this.safeDismiss();
					break;
				case R.id.button_accel_normal:
					AccelUpgradeDialog.this.safeDismiss();
					UIUtils.showToast("开始普通加速");
					break;
			}
		}
	};

	private void safeDismiss() {
		try {
			dismiss();
		} catch (RuntimeException e) { }
	}

	public AccelUpgradeDialog(Context context, boolean oldUser) {
		super(context,  R.style.AppDialogTheme);
		this.context = context;
		setContentView(R.layout.dialog_senior_accel_remind);

		initView(oldUser);

		findViewById(R.id.button_accel_normal).setOnClickListener(onClickListener);
		findViewById(R.id.button_to_vip).setOnClickListener(onClickListener);

	}

	private void initView(boolean oldUser) {
		if (oldUser) {
			TextView textView = (TextView) findViewById(R.id.text_change);
			textView.setText("快去升级账号继续享受VIP加速服务吧~");
		}
	}
}
