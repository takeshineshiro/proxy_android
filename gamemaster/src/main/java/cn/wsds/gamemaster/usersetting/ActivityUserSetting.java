package cn.wsds.gamemaster.usersetting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.data.UserSession.LogoutReason;
import cn.wsds.gamemaster.data.UserSession.UserSessionObserver;
import cn.wsds.gamemaster.data.VIPINFO;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.social.SOCIAL_MEDIA;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.ActivityBase;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.user.ActivityUserPointsHistory;
import cn.wsds.gamemaster.ui.user.Identify;
import cn.wsds.gamemaster.ui.user.ThirdPartLoginManager;

public class ActivityUserSetting extends ActivityBase {
	
	private static final String TAG = "ActivityUserSetting" ;
	
	private final UserSessionObserver userSessionObserver = new UserSessionObserver() {

        public void onScoreChanged(int score) {
           
        }
       
        public void onUserInfoChanged(UserInfo info) {

        }

        public void onSessionInfoChanged(SessionInfo info) {
        	 
        }

		@Override
		public void onVIPInfoChanged(VIPINFO vipinfo) {
			updateVIPValidTime();
		}
	};

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.phone_number:
                    if(!UserSession.isBoundPhoneNumber()){
                    	ActivityUserAccount.openForResult(ActivityUserSetting.this, ActivityUserAccount.FRAGMENT_TYPE_BIND_PHONE,false,1);
                    }
                    break;
                case R.id.setting_modify_password:
                	ActivityUserAccount.openForResult(ActivityUserSetting.this, ActivityUserAccount.FRAGMENT_TYPE_UPDATE_PASSWORD,false,0);
                    break;
                case R.id.setting_point_history:
                    UIUtils.turnActivity(ActivityUserSetting.this, ActivityUserPointsHistory.class);
                    break;
                case R.id.setting_exit_login:
                	requestLogout();
                    break;
                /*case R.id.vip_info_renew:
                	UIUtils.turnActivity(ActivityUserSetting.this, ActivityStore.class);
                	break; */
            }
        }

        /**
         * 向服务器发出登出请求
         */
		private void requestLogout() {
			SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
			if(sessionInfo==null){
				ActivityUserSetting.this.finish();
				return;
			}
			String sessionId = sessionInfo.getSessionId();
			if(TextUtils.isEmpty(sessionId)){
				ActivityUserSetting.this.finish();
				return;
			}
			HttpApiService.requestLogout(sessionInfo.getUserId(), sessionId, sessionInfo.getAccessToken(),
				new ResponseHandler(ActivityUserSetting.this) {

					@Override
					public void onNetworkUnavailable() {
						cleanup();
					};
					
					@Override
					protected void onSuccess(Response response) {
						cleanup();
					}
					
					protected void onFailure() {
						cleanup();
					}
					
					protected CharSequence getToastText_RequestFail() { return null; };

					private void cleanup() {
						logoutThirdPartAction();
						UserSession.logout(LogoutReason.USER_CLICK_EXIT);
						Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACC_HOMEPAGE_CLICK_STOP,
								AccelOpenManager.isRootModel() ? "ROOT" : "VPN");		
						if (!ActivityUserSetting.this.isFinishing()) {
							ActivityUserSetting.this.finish();
						}

						Identify.getInstance().notifyUserLogout();
					}
				}
				);
		}
    };
	private View modifyPassword;
    
	/**
	 * 退出第三方登录
	 */
	private void logoutThirdPartAction() {
		UserInfo userInfo = UserSession.getInstance().getUserInfo();
		if(userInfo==null){
			return;
		}
		SOCIAL_MEDIA social_MEDIA = userInfo.getSocial_MEDIA();
		if(social_MEDIA == null){
			return;
		}
		switch (social_MEDIA) {
		case QQ:
			ThirdPartLoginManager.instance.logoutQQ(this);
			break;
		case WEIBO:
			ThirdPartLoginManager.instance.logoutSinaWeibo();
			break;
		case WEIXIN:
			ThirdPartLoginManager.instance.logoutWeixin(this);
			break;
		default:
			break;
		
		}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setting);
        setDisplayHomeArrow("用户设置");
        findViewById(R.id.phone_number).setOnClickListener(onClickListener);
        modifyPassword = findViewById(R.id.setting_modify_password);
        modifyPassword.setOnClickListener(onClickListener);
        findViewById(R.id.setting_point_history).setOnClickListener(onClickListener);
        findViewById(R.id.setting_exit_login).setOnClickListener(onClickListener);
        //findViewById(R.id.vip_info_renew).setOnClickListener(onClickListener);
        
        updateVIPValidTime();
        UserSession.getInstance().registerUserSessionObserver(userSessionObserver);
    }

    
    @Override
	protected void onDestroy() {	
		UserSession.getInstance().unregisterUserSessionObserver(userSessionObserver); 
		super.onDestroy();
	}

    private void setDisplayPhoneNumber(boolean isBined,String phoneNumber) {
        TextView textView;
		if(isBined) {
			textView = (TextView)findViewById(R.id.setting_phone_number);
			textView.setText(UIUtils.getFormatPhoneNumber(phoneNumber));
			findViewById(R.id.phone_number).setBackgroundResource(R.drawable.setting_item_bg_normal);
        } else {
        	textView = (TextView)findViewById(R.id.setting_phone_number_binding);
        	findViewById(R.id.phone_number).setBackgroundResource(R.drawable.setting_item_bg);
        }

        ViewFlipper viewFipper = (ViewFlipper) findViewById(R.id.view_flipper_phone);
        int index = viewFipper.indexOfChild(textView);
        viewFipper.setDisplayedChild(index);
    }
    
    @Override
    protected void onStart() {
		super.onStart();
		boolean isBoundPhoneNumber = UserSession.isBoundPhoneNumber();
		UserInfo userInfo = UserSession.getInstance().getUserInfo();
        String phoneNumber = userInfo == null ? null : userInfo.getPhoneNumber();
		setDisplayPhoneNumber(isBoundPhoneNumber,phoneNumber);

		int visibility = isBoundPhoneNumber ? View.VISIBLE : View.GONE;
		modifyPassword.setVisibility(visibility);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if(!UserSession.isLogined()){
    		finish();
    	}
    	if(RESULT_OK == resultCode){
    		finish();
    	}
    }
    
    private void updateVIPValidTime(){
    	/*final TextView textExpireTime = (TextView)findViewById(R.id.vip_expired_time);
    	
    	if(!Identify.canAccel()){
    		if(CheckResultType.EXPIRED.equals(Identify.getCheckResultType())){
    			textExpireTime.setText("加速服务已过期");
    		}else{
    			textExpireTime.setText(R.string.vip_time_unknown);
    		}
    		         	
    		return ;
    	} 
    		
    	textExpireTime.setText("VIP有效时间获取中…");      	
        Runnable r = new Runnable(){

    		@Override
    		public void run() {
    			String time = VPNUtils.getVIPValidTime(TAG);
    			if((time==null)||(time.isEmpty())){
    				if((ActivityUserSetting.this==null)||
    					(ActivityUserSetting.this.isFinishing())){
    					 return ;
    				} 					 
    					 MainHandler.getInstance().postDelayed(this, 500);
    				}else{
    					 StringBuilder builder = new StringBuilder();
    					 builder.append("VIP有效期至");
    					 builder.append(time);
    					 textExpireTime.setText(builder.toString());
    				}
    		}   		
        };
        	
        MainHandler.getInstance().post(r);*/
    	 	
    }
    
    
}
