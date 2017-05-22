package cn.wsds.gamemaster.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.user.FragmentLogin;
import cn.wsds.gamemaster.ui.user.FragmentUserAccount;
import cn.wsds.gamemaster.ui.user.FragmentUserAccount.OnFragmentViewStateRestoredListener;
import cn.wsds.gamemaster.ui.user.FragmentUserBindPhone;
import cn.wsds.gamemaster.ui.user.FragmentUserRegister;
import cn.wsds.gamemaster.ui.user.FragmentUserResetPassword;
import cn.wsds.gamemaster.ui.user.FragmentUserUpdatePassword;

/**
 * 本Activity为用户帐号公用，是各FragmentUser的容器
 */
public class ActivityUserAccount extends ActivityBase {
	
	public static final String EXTRA_NAME_FRAGMENT_TYPE = "cn.wsds.gamemaster.ui.user.fragment_type";

	/**
	 * 注册
	 * 
	 * @see #open
	 */
	public static final int FRAGMENT_TYPE_REGUSTER = 1;

	/**
	 * 登录
	 * @see #open
	 */
	public static final int FRAGMENT_TYPE_LOGIN = 2;
	
	/**
	 * 重置密码
	 * @see #open
	 */
	public static final int FRAGMENT_TYPE_RESET_PASSWORD = 3;

	/**
	 * 绑定手机号
	 * @see #open
	 */
	public static final int FRAGMENT_TYPE_BIND_PHONE = 4;
	
	/**
	 * 修改密码
	 * @see #open
	 */
	public static final int FRAGMENT_TYPE_UPDATE_PASSWORD = 5;
	
	private Fragment currentFragment;
//	private TextView textMessage;
	
	/**
	 * 开启本页面，并加载指定的Fragment
	 * 
	 * @param context
	 * @param fragmentType
	 *            FRAGMENT_TYPE_xxxxxx
	 * @param backHistory 
	 */
	public static void open(Activity context, int fragmentType) {
		open(context, fragmentType, false);
	}

	/**
	 * 开启本页面，并加载指定的Fragment
	 * 
	 * @param context
	 * @param fragmentType
	 *            FRAGMENT_TYPE_xxxxxx
	 * @param backHistory 
	 */
	public static void open(Activity context, int fragmentType, boolean backHistory) {
		openForResult(context, fragmentType, backHistory, -1);
	}
	
	/**
	 * 开启本页面，并加载指定的Fragment
	 * 
	 * @param context
	 * @param fragmentType
	 *            FRAGMENT_TYPE_xxxxxx
	 * @param backHistory 
	 * @param requestCode 请求码，默认为-1
	 */
	public static void openForResult(Activity context, int fragmentType, boolean backHistory,int requestCode) {
		if(context == null){
			open(fragmentType);
			return;
		}
		if (context instanceof ActivityUserAccount) {
			((ActivityUserAccount) context).changeFragment(fragmentType,backHistory);
		} else {
			Intent intent = new Intent(context, ActivityUserAccount.class);
			intent.putExtra(EXTRA_NAME_FRAGMENT_TYPE, fragmentType);
			if(requestCode == -1){
				context.startActivity(intent);
			}else{
				context.startActivityForResult(intent,requestCode);
			}
		}
	}

	private static void open(int fragmentType) {
		Context context = AppMain.getContext();
		Intent intent = new Intent(context, ActivityUserAccount.class);
		intent.putExtra(EXTRA_NAME_FRAGMENT_TYPE, fragmentType);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_account);
//		textMessage = (TextView)findViewById(R.id.text_message);
		int fragmentType = getIntent().getIntExtra(EXTRA_NAME_FRAGMENT_TYPE, -1);
		changeFragment(fragmentType);
	}
	
	private void changeFragment(int fragmentType) {
		changeFragment(fragmentType,false);
	}

	private void changeFragment(int fragmentType,boolean backHistory) {
		final FragmentUserAccount fragment = createFragmentByFragmentType(fragmentType);
		if(fragment==null){
			throw new RuntimeException("fragment not find");
		}
		fragment.setOnFragmentViewStateRestoredListener(new OnFragmentViewStateRestoredListener() {
			
			@Override
			public void onViewStateRestored() {
				setDisplayHomeArrow(fragment.getTitle());
			}
		});
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		if(backHistory){
			transaction.replace(R.id.fragment_container, fragment);
			transaction.addToBackStack(null);
		}else{
			transaction.replace(R.id.fragment_container, fragment);
		}
		transaction.commit();
		this.currentFragment = fragment;
	}
	
	@Override
	protected void onActionBackOnClick() {
		FragmentManager fragmentManager = getFragmentManager();
		if(fragmentManager.getBackStackEntryCount() == 0){
			super.onActionBackOnClick();
		}else{
			fragmentManager.popBackStack();
		}
	}

	private FragmentUserAccount createFragmentByFragmentType(int fragmentType) {
		switch (fragmentType) {
		case FRAGMENT_TYPE_LOGIN:
			return new FragmentLogin();
		case FRAGMENT_TYPE_REGUSTER:
			return new FragmentUserRegister();
		case FRAGMENT_TYPE_BIND_PHONE:
			return new FragmentUserBindPhone();
		case FRAGMENT_TYPE_RESET_PASSWORD:
			return new FragmentUserResetPassword();
		case FRAGMENT_TYPE_UPDATE_PASSWORD:
			return new FragmentUserUpdatePassword();
		default:
			return null;
		}
	}
	
//	public void setTextMessage(CharSequence message, ColorStateList textColor) {
//		UIUtils.setViewText(textMessage, message);
//		UIUtils.setViewTextColor(textMessage, textColor);
//	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (currentFragment != null) {
			currentFragment.onActivityResult(requestCode, resultCode, data);
		}
//		ThirdPartLoginManager.instance.onActivityResult(this, requestCode, resultCode, data);
//		UMSsoHandler ssoHandler = SocializeConfig.getSocializeConfig().getSsoHandler(requestCode);
//		if (ssoHandler != null) {
//			ssoHandler.authorizeCallBack(requestCode, resultCode, data);
//		}
	}
	
	@Override
	protected ActivityType getPreActivityType() {
		return ActivityType.USER_CENTER;    
	}
}
