package cn.wsds.gamemaster.ui;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.ui.store.ActivityVip;

public class ActivityUpdateReminder extends ActivityBase implements OnClickListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_update_reminder);
		initView();
		ConfigManager.getInstance().setHasShowRemindGuideActivity();
	}
	
	private void initView(){
		ImageView imgTitle = (ImageView)findViewById(R.id.remind_title);
		ImageView imgRechargeText = (ImageView)findViewById(R.id.recharge_text);
		ImageView btnAction = (ImageView)findViewById(R.id.btn_action);
		ImageView btnSkip = (ImageView)findViewById(R.id.btn_skip);
		
		if(isUserBoundPhone()){
			imgTitle.setImageResource(R.drawable.guide_page_title_2);
			btnAction.setImageResource(R.drawable.guide_page_button_2);
		}else{
			imgTitle.setImageResource(R.drawable.guide_page_title_1);
			imgRechargeText.setVisibility(View.GONE);
			btnAction.setImageResource(R.drawable.guide_page_button_1);
		}
		
		btnAction.setOnClickListener(this);
		btnSkip.setOnClickListener(this);
	}
	
	private void doAction(){
		if(isUserBoundPhone()){
			UIUtils.turnActivity(this, ActivityVip.class);
		}else{
			if(UserSession.isLogined()){
				ActivityUserAccount.open(this,ActivityUserAccount.FRAGMENT_TYPE_BIND_PHONE);
			}else{
				ActivityUserAccount.open(this,ActivityUserAccount.FRAGMENT_TYPE_LOGIN);
			}	
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_action:
			doAction();
			break;
		case R.id.btn_skip:
			UIUtils.turnActivity(this, ActivityMain.class);	
			break;
		default:
			break;
		}
		
		finish();
	}
	
	private static boolean isUserBoundPhone(){
		if(UserSession.isLogined()&&UserSession.isBoundPhoneNumber()){
			 return true ;
		}

		return false ;
	}
}
