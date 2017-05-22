package cn.wsds.gamemaster.ui;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.utils.CalendarUtils;
import com.subao.net.NetManager;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.data.UserSession.UserSessionObserver;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.MainShareDialog;
import cn.wsds.gamemaster.dialog.UserSign;
import cn.wsds.gamemaster.dialog.UserSign.UserTaskHistory;
import cn.wsds.gamemaster.dialog.UserSigninDialog;
import cn.wsds.gamemaster.event.JPushMessageReceiver;
import cn.wsds.gamemaster.net.http.DefaultNoUIResponseHandler;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.UpdateUserInfoRequestor;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.UIUtils.IOnReloginConfirmListener;
import cn.wsds.gamemaster.ui.exchange.ActivityExchangeCenter;
import cn.wsds.gamemaster.ui.floatwindow.DelayTextSpannable;
import cn.wsds.gamemaster.ui.store.ActivityVip;
import cn.wsds.gamemaster.ui.user.ActivityTaskCenter;
import cn.wsds.gamemaster.ui.user.ActivityUserPointsHistory;
import cn.wsds.gamemaster.ui.user.UserTaskManager;
import cn.wsds.gamemaster.usersetting.ActivityUserSetting;

public class ActivityUser extends ActivityShare {
    private final static String TAG = "ActivityUser";
    private static final int EXCHANGE_NEED_SCORE = 150; 
    
    private final OnClickListener onClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_accel_setting:
                    UIUtils.turnActivity(ActivityUser.this, ActivitySetting.class);
                    break;
                case R.id.button_about:
                    UIUtils.turnActivity(ActivityUser.this, ActivityAbout.class);
                    break;
                case R.id.button_qa:
                    UIUtils.turnActivity(ActivityUser.this, ActivityQA.class);
                    break;
                case R.id.button_feedback:
                    UIUtils.turnActivity(ActivityUser.this, ActivityFeedback.class);
                    break;
                case R.id.login_group:
                    if (UserSession.isLogined()) {
                        UIUtils.turnActivity(ActivityUser.this, ActivityUserSetting.class);
                    } else {
                        ActivityUserAccount.open(ActivityUser.this,
                                ActivityUserAccount.FRAGMENT_TYPE_LOGIN);
                    }
                    break;
                case R.id.layout_market:
                    if(!isSameMonthOfYear()) {
                        ConfigManager.getInstance().setLastTimestampMillisMarket(System.currentTimeMillis());
                    }
                    UIUtils.turnActivity(ActivityUser.this, ActivityExchangeCenter.class);      
                    break;
                case R.id.button_share:
                	showShareDialog();	
                    break;
                case R.id.button_task_center:
                	Statistic.addEvent(getApplicationContext(), Statistic.Event.USER_TASK_CENTRE_CLICK);
                	UIUtils.turnActivity(ActivityUser.this, ActivityTaskCenter.class);
                	break;
				case R.id.button_user_vip_center:
					UIUtils.turnActivity(ActivityUser.this, ActivityVip.class);
					Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_IN);
					break;
                default:
                    break;
            }
        }
    }; 

    private UserMessageGroup userMessageGroup;
    private Avatar imageAvatar;
    private UserSignin userSignin;

    private final MenuIconMarker menuIconMark = new MenuIconMarker_Message();

    private final UserSessionObserver userSessionObserver = new UserSessionObserver() {

        public void onScoreChanged(int score) {
            userMessageGroup.setScore(score);
            refreshMarketImageView();
        }
       
        public void onUserInfoChanged(UserInfo info) {
            userMessageGroup.setUserMssage(info);
        }

        public void onSessionInfoChanged(SessionInfo info) {       	
        	refreshUserInfoUI();
        }

    };
	
//	private TextView textExpectPointBySignIn;

    private ImageView imageViewMarket;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDisplayHomeArrow(R.string.user_center);
        setContentView(R.layout.activity_user);
  
        findViewById(R.id.button_accel_setting).setOnClickListener(onClickListener);
        findViewById(R.id.button_about).setOnClickListener(onClickListener);
        findViewById(R.id.button_qa).setOnClickListener(onClickListener);
        findViewById(R.id.button_feedback).setOnClickListener(onClickListener);
        findViewById(R.id.login_group).setOnClickListener(onClickListener);

        userSignin = new UserSignin((Button) findViewById(R.id.button_signin), ActivityUser.this);
    	
    	
//      textExpectPointBySignIn = (TextView) findViewById(R.id.text_sign_score);
        
        userMessageGroup = new UserMessageGroup(
                (ViewFlipper) findViewById(R.id.user_message_group));

        imageAvatar = new Avatar((ImageView) findViewById(R.id.image_avatar),(ImageView) findViewById(R.id.image_ring));

		initSubView(R.id.button_user_vip_center, onClickListener, getButtonBuilder("会员中心", "成为VIP会员，专享极速服务"));

		initSubView(R.id.button_share, onClickListener, getButtonBuilder("分享", "每月首次成功分享可获得积分奖励"));

		initSubView(R.id.button_task_center, onClickListener, getButtonBuilder("任务中心", "完成任务，积分送不停"));

        initMarketView();

        UpdateUserInfoRequestor.instance.request(false);
        checkOrder();

        menuIconMark.attachActivity(this);
        
        if(UserSession.isLogined()){
        	ConfigManager.getInstance().setToadyEnterUserCenterOnLogin();
        }   
        
        Bundle bundle = getIntent().getExtras();
        doForExtras(bundle);
	}

	private void initSubView(int button_user_vip_center, OnClickListener onClickListener, SpannableStringBuilder sb) {
		Button buttonVip = (Button) findViewById(button_user_vip_center);
		buttonVip.setOnClickListener(onClickListener);
		buttonVip.setText(sb);
	}

	private void doForExtras(Bundle bundle) {
		if(bundle==null){
			reportUserCenterIn(false);
        	return ;
        }

        if(JPushMessageReceiver.jpushTurnUserCenter(bundle)){
            //从极光消息进入用户中心    
        	reportUserCenterIn(true); 
        }else if(JPushMessageReceiver.jpushTurnSharePage(bundle)){
        	//从极光消息进入分享页面
            showShareDialog();	
        }else{
            reportUserCenterIn(false);
        }      
    }
	
	private void reportUserCenterIn(boolean isFromJPush){
		Statistic.addEvent(this, Statistic.Event.INTERACTIVE_USER_CENTER_IN,isFromJPush); 		
	}
	
	private void showShareDialog(){
		new MainShareDialog(ActivityUser.this).show();		
	}
    
	private void initMarketView() {
        imageViewMarket = (ImageView) findViewById(R.id.image_market);
        View layoutMarket = findViewById(R.id.layout_market);
        layoutMarket.setOnClickListener(onClickListener);
        TextView textView = (TextView)findViewById(R.id.text_market);
        textView.setText(getButtonBuilder("兑换中心", "小小积分，兑换大大精彩"));
    }

    private void refreshMarketImageView() {
    	 if(needAlertScoreEnough()) {
             imageViewMarket.setVisibility(View.VISIBLE);
         } else {
             imageViewMarket.setVisibility(View.INVISIBLE);
         }
    }

    /**
     * 是否需要提示积分已购
     * @return
     * true 需要提示 false 不需要提示
     */
	public static boolean needAlertScoreEnough() {
		if(UserSession.isLogined()) {
			UserInfo userInfo = UserSession.getInstance().getUserInfo();
			if(userInfo != null) {
				return userInfo.getScore() >= EXCHANGE_NEED_SCORE && !isSameMonthOfYear();
			}
		}
		return false;
	}

    private static boolean isSameMonthOfYear() {
        long timestampMillis = ConfigManager.getInstance().getLastTimestampMillisMarket();
        if (timestampMillis == 0) {
            return false;
        }

        Calendar lastTimestamp = Calendar.getInstance();
        lastTimestamp.setTimeInMillis(timestampMillis);
        Calendar nowTimestamp = Calendar.getInstance();
        return nowTimestamp.get(Calendar.MONTH) == lastTimestamp.get(Calendar.MONTH) &&
                nowTimestamp.get(Calendar.YEAR) == lastTimestamp.get(Calendar.YEAR);
    }


    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	menuIconMark.detachActivity();
    }
    

    private void checkOrder() {
        if (UserSession.isLogined()) {
            final List<String> orderIds = ConfigManager.getInstance().getAllOrderId();

            if (orderIds != null && orderIds.size() > 0) {//判断订单历史id
                for (String orderId : orderIds) {
                    HttpApiService.requestCouponsStatus(orderId, new DefaultNoUIResponseHandler() {
                        @Override
                        protected void onSuccess(Response response) {
                            if (!ActivityUser.this.isFinishing()) {
                                if (response.code == 200 || response.code == 417) {
                                    String message = "";
                                    if (response.code == 200) {
                                        message = "流量兑换成功，已充至绑定手机内。";
                                    } else if (response.code == 417) {
                                        message = "流量兑换失败 ，兑换积分已返回至账户内。";
                                        UpdateUserInfoRequestor.instance.request(true);
                                    }
                                    CommonAlertDialog dialog = new CommonAlertDialog(ActivityUser.this);
                                    dialog.setMessage(message);
                                    dialog.setPositiveButton("确定", null);
                                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            Log.i(TAG, "onDismiss :" + orderIds);
                                            ConfigManager.getInstance().removeOrderId(orderIds);
                                        }
                                    });
                                    dialog.show();
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshUserInfoUI();
        refreshMarketImageView();
        UserSession.getInstance().registerUserSessionObserver(userSessionObserver);   
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	UserSession.getInstance().unregisterUserSessionObserver(userSessionObserver);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	userSignin.refreshButtonSign();
        //
        if (UserSession.isLogined() && userSignin.buttonSignIn.isEnabled() && NetManager.getInstance().isConnected()) {
        	ConfigManager config = ConfigManager.getInstance();
        	int today = CalendarUtils.todayLocal();
        	if (config.getDaySignDialogPopup() != today) {
        		userSignin.onButtonSignClick();
        	}
        }
    }

    /**
     * 刷新用户信息UI
     */
    private void refreshUserInfoUI() {
        boolean isLogined = UserSession.isLogined();
        if (isLogined) {
            userMessageGroup.displayLoginPage();
            UserInfo userInfo = UserSession.getInstance().getUserInfo();
            if (userInfo != null) {
                userMessageGroup.setUserMssage(userInfo);
                imageAvatar.displayloginAvatar();
            }
        } else {
            userMessageGroup.displayUnLoginPage();
            imageAvatar.displayUnloginAvatar();
        }
    }

    private static final class UserMessageGroup {
        private final int unLoginIndex;
        private final int loginIndex;
        private final ViewFlipper userMessageGroup;
        private final TextView textPoints;	// 用户积分
        private final TextView textPhone;

        private UserMessageGroup(ViewFlipper userMessageGroup) {
            this.userMessageGroup = userMessageGroup;
            View textUnlogin = userMessageGroup
                    .findViewById(R.id.unlogin_text);
//            textUnlogin
//                    .setText(getUnLoginSpannable(textUnlogin.getResources()));
            this.unLoginIndex = userMessageGroup.indexOfChild(textUnlogin);
            View loginPage = userMessageGroup.findViewById(R.id.login_page);
            this.loginIndex = userMessageGroup.indexOfChild(loginPage);
            textPoints = (TextView) loginPage.findViewById(R.id.text_points);
            textPhone = (TextView) loginPage.findViewById(R.id.text_phone);
            //
            textPoints.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					UIUtils.turnActivity(v.getContext(), ActivityUserPointsHistory.class);
				}
			});
        }

//        private Spannable getUnLoginSpannable(Resources resources) {
//            String special = "注册有礼";
//            Spannable builder = DelayTextSpannable.getSpecialInBehind(
//                    "登录/注册  ", special,
//                    resources.getColor(R.color.color_game_6));
//            builder.setSpan(new AbsoluteSizeSpan(12, true), builder.length()
//                            - special.length(), builder.length(),
//                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
//            return builder;
//        }

        public void displayLoginPage() {
            userMessageGroup.setDisplayedChild(loginIndex);
        }

        public void setUserMssage(UserInfo userInfo) {
            int score = userInfo == null ? 0 : userInfo.getScore();
            setScore(score);
            String nickName = userInfo == null ? "" : userInfo.getUserName();
            textPhone.setText(nickName);
        }

        private void setScore(int score) {
            Resources resources = textPoints.getResources();
            textPoints.setText(getIntergralSpannable(score, resources));
        }

        private Spannable getIntergralSpannable(int integral, Resources resources) {
            String label = "积分";
            String space = "  ";
            Spannable builder = DelayTextSpannable.getSpecialInBehind(label
                            + space, String.valueOf(integral),
                    resources.getColor(R.color.color_game_8));
            builder.setSpan(new AbsoluteSizeSpan(30, true), label.length() + space.length(), builder.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return builder;
        }

        public void displayUnLoginPage() {
            userMessageGroup.setDisplayedChild(unLoginIndex);
        }
    }

    private SpannableStringBuilder getButtonBuilder(String title, String desc) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(title);
        char separator = '\n';
        builder.append(separator);
        builder.append(desc);
        int start = title.length() + 1;
        int end = builder.length();
        builder.setSpan(
                new ForegroundColorSpan(getResources().getColor(
                        R.color.color_game_6)), start, end,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        builder.setSpan(new AbsoluteSizeSpan(12, true), start, end,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return builder;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_message:
                UIUtils.turnActivity(this, ActivityMessage.class);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		menuIconMark.resetMenuIcon(menu.findItem(R.id.action_message));
		return super.onPrepareOptionsMenu(menu);
    }

    private static final class Avatar {
        private final ImageView imageView;
        private final ImageView imageRing;

        private Avatar(ImageView imageView, ImageView imageRing) {
			this.imageView = imageView;
			this.imageRing = imageRing;
		}

		public void displayUnloginAvatar() {
        	imageRing.setVisibility(View.INVISIBLE);
            imageView.setImageResource(R.drawable.user_photo);
        }

        public void displayloginAvatar() {
        	imageRing.setVisibility(View.VISIBLE);
        	Bitmap drawableAvatar = getDrawableAvatar();
            if (drawableAvatar == null) {
                imageView.setImageResource(R.drawable.user_photo_login);
            } else {
                imageView.setImageBitmap(drawableAvatar);
            }
        }

        private Bitmap getDrawableAvatar() {
            UserInfo userInfo = UserSession.getInstance().getUserInfo();
            if (userInfo == null) {
                return null;
            }
            Bitmap drawableAvatar = userInfo.getDrawableAvatar();
            return drawableAvatar;
        }

    }
    
    /**
     * 用户签到相关逻辑
     */
    public static final class UserSignin {
    	
    	private final Button buttonSignIn;
    	private WeakReference<Activity> activityRef;
//    	private final OnChangeButtonSignInListener onChangeButtonSignInListener;
//    	public interface OnChangeButtonSignInListener {
//    		public void changeButtonSignIn(boolean canSignToday);
//    	}
		public UserSignin(Button buttonSignIn,final Activity activity) {
			this.buttonSignIn = buttonSignIn;
			activityRef = new WeakReference<Activity>(activity);
			buttonSignIn.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onButtonSignClick();
				}
			});
		}
		
		private void changeButtonSignIn(boolean canSignToday) {
			if (canSignToday) {
	            buttonSignIn.setText(R.string.sign_in);
	            buttonSignIn.setEnabled(true);
	        } else {
	            buttonSignIn.setText(R.string.already_signed);
	            if (GlobalDefines.CLIENT_PRE_CHECK) {
	                buttonSignIn.setEnabled(false);
	            }
	        }
		}
		
		 /**
	     * 重置右上角“签到”按钮（灰掉还是可点击）
	     * <p>本操作不进行网络请求，仅根据本地数据进行判断</p>
	     */
		public void refreshButtonSign() {
			long lastSignTime = UserSign.getInstance().getLastSignTimestamp(UserSession.getInstance().getUserId());
			if (lastSignTime <= 0) {
				Logger.d(LogTag.USER, "Last sign time not found");
				changeButtonSignIn(true);
				return;
			}
			int today = Misc.millisecondsOfUTCToDayOfSCT(System.currentTimeMillis());
			int lastDayOfSign = Misc.millisecondsOfUTCToDayOfSCT(lastSignTime);
			if (Logger.isLoggableDebug(LogTag.USER)) {
				Logger.d(LogTag.USER, String.format("Last sign time = %s, local today = %s",
						Misc.formatCalendar(Misc.millisecondsOfUTCToLocalCarlendar(lastSignTime)),
						Misc.formatCalendar(Calendar.getInstance())));
			}
			changeButtonSignIn(today > lastDayOfSign);
		}
		
		/**
	     * 当右上角“签到”按钮被点击时，被调用
		 * @param activity 
	     */
	    private void onButtonSignClick() {
	    	if(!UserSession.isLogined()){
	    		final Activity ref = activityRef.get();
	    		if(ref == null || ref.isFinishing()){
	    			return;
	    		}
	    		UIUtils.showReloginDialog(ref, UserTaskManager.RELOGIN_MESS, new IOnReloginConfirmListener() {
					
					@Override
					public void onConfirm() {
						ref.finish();
					}
				});
				return;
			}
	    	
	        UserSign.RequestUserTaskHistoryCallback callback = new UserSign.RequestUserTaskHistoryCallback() {
	        	
	        	@Override
	        	public void onComplete(UserTaskHistory history) {
	                if (history != null) {
	                    UserSigninDialog.Listener listener = new UserSigninDialog.Listener() {
	                        @Override
	                        public void onFinish(boolean isSuccess) {
	                            UserSigninDialog.stopRefreshAnimation();
	                            refreshButtonSign();
	                        }

							@Override
							public void onDismiss(UserSigninDialog dialog) {
								refreshButtonSign();
							}
	                    };
	                    Activity activity = activityRef.get();
	        	        if(activity==null){
	        	        	return;
	        	        }
	        	        UserSigninDialog.createUserSignin(activity, listener, history);
	                    if (!UserSign.canSignToday()) {
	                    	// 今日不可签到，但代码能执行到这里，说明是因为特殊原因（比如另一设备已签到，但本机不知）造成的
	                    	// 需要强制刷新一下用户信息（主要是更新一下积分）
	                        UpdateUserInfoRequestor.instance.request(true);
	                        refreshButtonSign();
	                    }
	                } else {
						UIUtils.showToast("读取失败，请稍后再试");
	                }
	            }
	           
	        };

	        Activity activity = activityRef.get();
	        if(activity!=null){
	        	UserSign.getInstance().asyncRequestSignHistory(callback, activity,true);
	        }
	    }
    	
    }
	
	@Override
	protected ActivityType getPreActivityType() {
		return ActivityType.MAIN;
	}
	
	private static boolean isActivityUserFront(Activity act){
		if((act==null)||(act.isFinishing())){
			return false ;
		}
		
		return (act instanceof ActivityUser) ;
	}
	
	public static boolean  toShareDialog(){
		Activity act = ActivityBase.getCurrentActivity();
		if(!isActivityUserFront(act)){
			return false;
		}
		
		((ActivityUser)act).showShareDialog();
		return true ;
	}

}
