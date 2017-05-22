package cn.wsds.gamemaster.ui;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import cn.wsds.gamemaster.R;

public class ActivitySettings extends ActivityBase implements OnClickListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDisplayHomeArrow(R.string.settings);
		setContentView(R.layout.activity_settings);
		
		findViewById(R.id.button_accel_setting).setOnClickListener(this);
        findViewById(R.id.button_about).setOnClickListener(this);
        findViewById(R.id.button_qa).setOnClickListener(this);
        findViewById(R.id.button_feedback).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		 case R.id.button_accel_setting:
             UIUtils.turnActivity(ActivitySettings.this, ActivitySetting.class);
             break;
         case R.id.button_about:
             UIUtils.turnActivity(ActivitySettings.this, ActivityAbout.class);
             break;
         case R.id.button_qa:
             UIUtils.turnActivity(ActivitySettings.this, ActivityQA.class);
             break;
         case R.id.button_feedback:
             UIUtils.turnActivity(ActivitySettings.this, ActivityFeedback.class);
             break;
         default:
             break;
		} 
		
	}
	
	
	

}
