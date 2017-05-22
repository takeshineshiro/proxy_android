package cn.wsds.gamemaster.ui;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.statistic.Statistic;

public class ActivityMiuiUserReminder extends ActivityBase{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(R.string.miui_user_read_me);
	    setContentView(R.layout.activity_miui_read_me);
	    
	    initView();
	}
	
	private void initView(){
		((ImageView)findViewById(R.id.miui_goto_hide_settings_btn)).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				 
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.powerkeeper",
                		"com.miui.powerkeeper.ui.PowerHideModeActivity"));
                intent.putExtra("extra_pkgname", getPackageName());

                String result = "succeed";
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    UIUtils.showToast("目标页面不存在，请确认当前手机系统是否支持神隐模式！");
                    result = "failed";
                }finally{
                	Statistic.addEvent(ActivityMiuiUserReminder.this, Statistic.Event.XIAOMI_SHENYIN, result);
                }
			}			
		});
	}
}
