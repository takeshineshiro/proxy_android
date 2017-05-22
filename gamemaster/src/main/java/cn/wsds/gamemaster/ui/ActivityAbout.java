package cn.wsds.gamemaster.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import cn.wsds.gamemaster.R;

import com.subao.common.data.SubaoIdManager;
import com.subao.common.utils.InfoUtils;
import com.subao.utils.Misc;

/**
 * 关于界面
 */
public class ActivityAbout extends ActivityBase {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		setDisplayHomeArrow(R.string.main_menu_about);
		initView();
	}

	/**
	 * 初始化显示元素
	 */
	private void initView() {
		View.OnClickListener l = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.about_list_qq_group:   //官方Q群
					UIUtils.openQQgroup(ActivityAbout.this, UIUtils.KEY_QQ_GROUP_DEFAULT);
					break;
				case R.id.about_list_qq_service:	//客服QQ
					UIUtils.openServiceQQ(ActivityAbout.this);
					break;
				case R.id.about_list_weibo:        //官方微博
					openWeiboUserInfo("5340021273");
					break;
				/*case R.id.about_list_weixin:     //微信公众号
					opendWeixinMainPage();
					break;*/
				case R.id.about_list_agreement:		//服务协议
					UIUtils.turnActivity(ActivityAbout.this, ActivityLicence.class);
					break;
				case R.id.text_url_homepage:
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(((TextView)v).getText().toString())));
					break;
				}				
			}
		};
		findViewById(R.id.about_list_qq_group).setOnClickListener(l);
		findViewById(R.id.about_list_qq_service).setOnClickListener(l);
		findViewById(R.id.about_list_weibo).setOnClickListener(l);
		findViewById(R.id.about_list_agreement).setOnClickListener(l);
		findViewById(R.id.text_url_homepage).setOnClickListener(l);		
		
		setTextToView(R.id.text_about_app_version, InfoUtils.getVersionName(this));
		setTextToView(R.id.text_about_android_version, "Android SDK " + android.os.Build.VERSION.SDK_INT);
		setTextToView(R.id.text_about_model, android.os.Build.MODEL);
		
		findViewById(R.id.img_logo).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				TextView tv = (TextView)findViewById(R.id.text_subaoid);
				tv.setText(SubaoIdManager.getInstance().getSubaoId());
				UIUtils.setViewVisibility(tv, View.VISIBLE);
				return true;
			}
		});
	}


	private void setTextToView(int textViewResId, CharSequence text) {
		((TextView)findViewById(textViewResId)).setText(text);
	}

	/**
     * 通过uid打开个人资料界面。
     * 
     * @param uid   用户ID
     */
    private void openWeiboUserInfo(String uid){
    	if(Misc.isAppInstalled(ActivityAbout.this, "com.sina.weibo")){
    		try{
		        Intent intent=new Intent();
		        intent.setAction(Intent.ACTION_VIEW);
		        intent.addCategory("android.intent.category.DEFAULT");
		        intent.setData(Uri.parse("sinaweibo://userinfo?uid="+uid));
		        startActivity(intent);
    		}catch(Exception e){
    			e.printStackTrace();
    			UIUtils.showToast("您未安装微博手机客户端", Toast.LENGTH_SHORT);
    		}
    	}else{
    		Toast.makeText(getApplicationContext(), "您未安装微博手机客户端", Toast.LENGTH_SHORT).show();
    	}
    }
    
//    /**
//     * 打开微信主界面
//     */
//    public void opendWeixinMainPage(){
//		try{
//			Intent intent = new Intent();
//			ComponentName cmp = new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
//			intent.setAction(Intent.ACTION_MAIN);
//			intent.addCategory(Intent.CATEGORY_LAUNCHER);
//			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			intent.setComponent(cmp);
//			startActivityForResult(intent, 0);
//		}catch (Exception e) {
//			UIUtils.showToast("您未安装微信客户端", Toast.LENGTH_SHORT);
//		}
//    }
}
