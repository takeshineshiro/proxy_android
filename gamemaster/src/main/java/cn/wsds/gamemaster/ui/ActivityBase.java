package cn.wsds.gamemaster.ui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Stack;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import cn.wsds.gamemaster.AppInitializer;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.statistic.Statistic;

public abstract class ActivityBase extends Activity {

	private static final Stack<Activity> stack = new Stack<Activity>();

	protected enum ActivityType {
		MAIN,
		USER_CENTER,
		MESSAGE_CENTER,
	}
	
	private boolean resumed;

	/**
	 * 本Activity是否在前台（前面没有任何对话框或其它页面遮挡）
	 */
	public boolean isForeground() {
		return this.resumed;
	}

	public static void finishAll(Activity exclude) {
		while (!stack.isEmpty()) {
			Activity a = stack.pop();
			if (a != exclude && !a.isFinishing()) {
				a.finish();
			}
		}
		if (exclude != null) {
			stack.push(exclude);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		stack.push(this);
		//
		if (autoInvokeAppInitIfNeed()) {
			AppInitializer.instance.execute(AppInitializer.InitReason.OTHER_ACTIVITY, this);
		}

		//魅族手机适配
		if (hasSmartBar()) { //判断是否支持SmartBar
			getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
			ActionBar actionbar = getActionBar();
			if (actionbar != null) {
				setBackIcon(getActionBar(), getResources().getDrawable(R.drawable.basepage_back));
			}
		} else {  //设置显示SmartBar
			getWindow().setUiOptions(0);
		}
		
		reportEvent(getIntent().getExtras());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stack.remove(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		resumed = true;
		Statistic.onActivityResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		resumed = false;
		Statistic.onActivityPause(this);
	}

	protected void setDisplayHomeArrow(int resId) {
		setDisplayHomeArrow(this.getString(resId));
	}

	protected boolean isMainActivity() {
		return false;
	}

	// 设置添加返回箭头
	@SuppressWarnings("deprecation")
	protected final void setDisplayHomeArrow(CharSequence title) {
		ActionBar actionBar = getActionBar();
		if (actionBar == null) {
			return;
		}
		if (hasSmartBar()) {
			actionBar.setDisplayShowHomeEnabled(false);
			actionBar.setDisplayUseLogoEnabled(false);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(title);
			actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_bg));
		} else {
			actionBar.setDisplayUseLogoEnabled(false);
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setDisplayShowHomeEnabled(false);
			actionBar.setDisplayShowCustomEnabled(true);
			//
			boolean isMainActivity = isMainActivity();
			View v = View.inflate(this, isMainActivity ? R.layout.actionbar_no_arrow_view
				: R.layout.actionbar_main_view, null);
			((TextView) v.findViewById(R.id.actionbar_title)).setText(title);
			ActionBar.LayoutParams layout = new ActionBar.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
			actionBar.setCustomView(v, layout);
			if (!isMainActivity) {
				v.findViewById(R.id.action_back).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						onActionBackOnClick();
					}
				});
			}
		}
	}

	protected void onActionBackOnClick() {
		//APP处于后台的情况下，由极光通知呼出页面后，点击回退箭头时需要呼出上一级页面，
		//因而有此处理，正常情况下无需做这样的处理；
		processIfCallFroamJPush();
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 判断是否有SmartBar
	 * 
	 * @return
	 */
	public static boolean hasSmartBar() {
		try {
			// 新型号可用反射调用Build.hasSmartBar()
			Method method = Class.forName("android.os.Build").getMethod("hasSmartBar");
			return ((Boolean) method.invoke(null)).booleanValue();
		} catch (Exception e) {}

		// 反射不到Build.hasSmartBar()，则用Build.DEVICE判断
		if (Build.DEVICE.equals("mx2")) {
			return true;
		} else if (Build.DEVICE.equals("mx") || Build.DEVICE.equals("m9")) {
			return false;
		}

		return false;
	}

	/**
	 * 设置SmartBar的返回图标
	 * 
	 * @param actionbar
	 * @param backIcon
	 */
	public void setBackIcon(ActionBar actionbar, Drawable backIcon) {
		try {
			Method method = Class.forName("android.app.ActionBar").getMethod("setBackButtonDrawable", new Class[] {
				Drawable.class
			});
			try {
				method.invoke(actionbar, backIcon);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 派生类实现，返回布尔值，决定是否在onCreate事件里自动调用一次AppInit<br />
	 * 缺省返回true
	 * 
	 * @return true=是的，需要调用
	 */
	protected boolean autoInvokeAppInitIfNeed() {
		return true;
	}

	/**
	 * 可由派生类改写，返回本Activity的前一级Activity类型
	 */
	protected ActivityType getPreActivityType() {
		return null;
	}

	@Override
	public void onBackPressed() {
		processIfCallFroamJPush();
		super.onBackPressed();
	}

	protected void reportEvent(Bundle bundle){
		
	}
	
	private void processIfCallFroamJPush() {
		//对于Jpush呼出的页面，
		//根据栈深度决定是否需要呼出上一级页面
		ActivityType preActivityType = getPreActivityType();
		if (preActivityType != null && stack.size() == 1) {
			switch (preActivityType) {
			case MAIN:
				UIUtils.turnActivity(this, ActivityMain.class);
				break;
			case USER_CENTER:
				UIUtils.turnActivity(this, ActivityUser.class);
				break;
			case MESSAGE_CENTER:
				UIUtils.turnActivity(this, ActivityMessage.class);
			}
		}
	}
	
	public static Activity getCurrentActivity(){
		if(stack == null){
			return null ;
		}
		
		int count = stack.size() ;
		if(count==0){
			return null ;
		}
		
		return stack.get(count-1) ;
	}
	
	public static int getStackDepth(){
		if(stack == null){
			return 0 ;
		}
		
		return stack.size();
	}

}
