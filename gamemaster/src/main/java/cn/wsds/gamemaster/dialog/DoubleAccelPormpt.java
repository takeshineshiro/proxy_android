package cn.wsds.gamemaster.dialog;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;

/**
 * 引导进入用户中心
 */
public class DoubleAccelPormpt extends BasePormpt{

	private static DoubleAccelPormpt instance;

    public DoubleAccelPormpt(Context context, View menuView) {
        super(context, menuView);
    }

    @Override
	public View createPormpt(Context context) {
		View view = View.inflate(context, R.layout.pormpt_double_accel, null);
		return view;
	}

    @Override
    protected RelativeLayout.LayoutParams createPormptParams(Context context, View menuView) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int[] menuLocation = new int[2];
        menuView.getLocationOnScreen(menuLocation);
        int startY = getStartY(menuView, menuLocation[1]);
        params.setMargins(0, startY + menuView.getHeight(), context.getResources().getDimensionPixelSize(R.dimen.space_size_70), 0);
        return params;
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
	 */
	public static void open(View menuView){
		if(menuView==null){
			return;
		}
		Context context = menuView.getContext();
		DoubleAccelPormpt prompt = new DoubleAccelPormpt(context, menuView);
		ConfigManager.getInstance().setMaskHelpUIStatus(ConfigManager.HELP_UI_STATUS_DOUBLE_ACCEl_PORMPT);
		prompt.show();
		DoubleAccelPormpt.instance = prompt;
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