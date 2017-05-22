package cn.wsds.gamemaster.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.tools.AppsWithUsageAccess;
import cn.wsds.gamemaster.ui.UIUtils;

public class UsageStateHelpDialog extends Dialog {

	private static UsageStateHelpDialog instance;
	private final View.OnClickListener viewOnClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.close:
				dismiss();
				break;
			case R.id.button_start:
				try {
					AppsWithUsageAccess.toImpower(activity);
				} catch (Exception e) {
					e.printStackTrace();
					UIUtils.showToast("未找到授权页面");
				}
				dismiss();
				break;
			}
		}
	};
	private final Activity activity;
	
	private UsageStateHelpDialog(Activity context) {
		super(context, R.style.AppDialogTheme);
		this.activity = context;
		setContentView(R.layout.usage_state_help_dialog);
		initView();
		ConfigManager.getInstance().setShowUsageStateHelpDialog(false);
	}
	
	public static void open(Activity context,final OnDismissListener onDismissListener){
		UsageStateHelpDialog usageStateHelpDialog = new UsageStateHelpDialog(context);
		usageStateHelpDialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				if(onDismissListener!=null){
					onDismissListener.onDismiss(dialog);				
				}
				instance = null;
			}
		});
		usageStateHelpDialog.show();
		instance = usageStateHelpDialog; 
	}

	private void initView() {
		findViewById(R.id.close).setOnClickListener(viewOnClickListener);
		Button buttonImpower = (Button) findViewById(R.id.button_start);
		buttonImpower.setOnClickListener(viewOnClickListener);
		buttonImpower.setText("立即授权");
		TextView content = (TextView) findViewById(R.id.content);
		SpannableStringBuilder builder = new SpannableStringBuilder();
		String normalMess = "安卓5.0以上的系统需要获得“查看应用使用权限”的授权，";
		builder.append(normalMess);
		String importantMess = "否则可能无法正确显示悬浮窗";
		builder.append(importantMess);
		builder.append("。");
		int start = normalMess.length();
		builder.setSpan(new ForegroundColorSpan(getContext().getResources().getColor(R.color.color_game_11)), start, start + importantMess.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		content.setText(builder);
	}

	public static boolean isInstanceExists() {
		return instance != null;
	}

	public static void close() {
		if(instance!=null){
			instance.dismiss();
			instance = null;
		}
	}
}
