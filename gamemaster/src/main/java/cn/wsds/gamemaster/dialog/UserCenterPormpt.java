package cn.wsds.gamemaster.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;

/**
 * 引导进入用户中心
 */
public class UserCenterPormpt extends BasePormpt{
	
	private static UserCenterPormpt instance;

    public UserCenterPormpt(Context context, View menuView) {
        super(context, menuView);
    }

    @Override
    protected RelativeLayout.LayoutParams createPormptParams(Context context, View menuView) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int[] menuLocation = new int[2];
        menuView.getLocationOnScreen(menuLocation);
        int startY = getStartY(menuView, menuLocation[1]);
        params.setMargins(0, startY + menuView.getHeight(), context.getResources().getDimensionPixelSize(R.dimen.space_size_32), 0);
        return params;
    }
    @Override
	public View createPormpt(Context context) {
		View view = View.inflate(context, R.layout.pormpt_user_center, null);
		TextView textContent = (TextView) view.findViewById(R.id.text_content);
		SpannableStringBuilder builder = new SpannableStringBuilder();
		String[] stringArray = context.getResources().getStringArray(R.array.pormpt_user_center);
		int specialColor = context.getResources().getColor(R.color.color_game_8);
		int index = 0;
		for (String s : stringArray) {
			int start = builder.length();
			builder.append(s);
			if(index % 2  != 0){
				builder.setSpan(new ForegroundColorSpan(specialColor), start, start + s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			}
			index++;
		}
		textContent.setText(builder);
		return view;
	}

	@Override
	public void onStop() {
		super.onStop();
		if (instance == this) {
			instance = null;
		}
	}

	/**
	 * 打开引导对话框
	 * @param menuView
	 * @param onDimissCallback 
	 */
	public static void open(View menuView, final Runnable onDimissCallback){
		if(menuView==null){
			return;
		}
		Context context = menuView.getContext();
		UserCenterPormpt prompt = new UserCenterPormpt(context, menuView);
		prompt.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				if(onDimissCallback!=null){
					onDimissCallback.run();
				}
			}
		});
		ConfigManager.getInstance().setMaskHelpUIStatus(ConfigManager.HELP_UI_STATUS_USER_CENTER_PORMPT);
		prompt.show();
		UserCenterPormpt.instance = prompt;
	}

	public static boolean isInstanceExists() {
		return instance != null;
	}
	
	public static void close(){
		if(instance!=null){
			instance.dismiss();
			instance = null;
		}
	}
}