package cn.wsds.gamemaster.tools;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.TagAliasCallback;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.DeviceInfo;
import cn.wsds.gamemaster.data.UserInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.net.NetworkStateChecker;

import com.subao.common.Logger;
import com.subao.common.utils.InfoUtils;
import com.subao.common.utils.PhoneNumber;

/**
 * Created by Qinyanjun on 16-05-09.
 * This class is for Jpush settings
 */

public class JPushUtils {

	private static final boolean ENABLED = true ;
	
	private static final String JPUSHTAG = "SuBaoJPush" ;
	
	private static final String NOGAME = "NoGame";
	private static final String[] GAMES = new String[]{"王者荣耀","穿越火线","球球大作战","部落冲突",
		"全民枪战","自由之战","王者荣耀","皇室战争","时空猎人","天天炫斗","全民超神","时空召唤",
		"全民突击","梦幻西游","火影忍者","问道","海岛奇兵","大话西游"};
	
	private static final String LOGIN = "login";
	private static final String NOTLOGIN = "notlogin";

	private static final String SCORE100W = "scroe_100W";
	private static final String SCORE200W = "scroe_200W";
	private static final String SCORE400W = "scroe_400W";
	
	private static final class JPushTagsCallback implements TagAliasCallback{
			
		private final Context context ;
		
		public JPushTagsCallback(Context context){
			this.context = context ;
		}
		
        @Override
        public void gotResult(int code, String alias, Set<String> tags) {
            switch (code) {
            case 0:                                
                if(!ConfigManager.getInstance().isHasSetTagForJPush()){
                	 ConfigManager.getInstance().setHasSetTagForJPush();
                }	
                Logger.d(JPUSHTAG,"Set tags success."); 
                break;
               
            case 6002:                               
                if (NetworkStateChecker.defaultInstance.isNetworkAvail()) {               	           	 
                	MainHandler.getInstance().postDelayed(new Runnable(){
						@Override
						public void run() {													
							setTagsForJPush(context);							
						}
                		
                	}, 2000);   
                	
                	Logger.d(JPUSHTAG, "Failed to set tags due to timeout. Try again after 2s.");
                	 
                }else{
                	Logger.e(JPUSHTAG, "Network invaliable, failed to set tags !");
                }
                break;
            
            default:  
            	StringBuilder builder = getStringBuilder();
            	builder.append("Failed set tags with error code : ");
            	builder.append(code);
            	Logger.e(JPUSHTAG, builder.toString());        
            	break;
            }                         
        }                 
    };   
    
    private static StringBuilder getStringBuilder(){
    	return new StringBuilder() ;
    }
  
    //用户分组，Jpush设置标签
    public static void setTagsForJPush(Context srcContext){ 	    	  
	    if(!ENABLED){
		    return ;
	    }
	    
	    if(srcContext==null){
	        Logger.e(JPUSHTAG, "srcContext is unavailble !");
	        return ;
	    } 
	    
	    Context context = srcContext.getApplicationContext();  
	    if(context==null){
	        Logger.e(JPUSHTAG, "ApplicationContext is unavailble !");
	        return ;
	    }
	    
	    Set<String> tagSet = new LinkedHashSet<String>();
	    setAndroidVersionTag(tagSet);	    	    
	    setAppInfoTag(context,tagSet);
	    setOperaterTag(context,tagSet);
	    setUserInfoTag(tagSet);
	    setGameTag(tagSet);
	    
	    JPushInterface.setTags(context, tagSet, new JPushTagsCallback(context));
    }
  
    //设置Android版本号标签
    private static void setAndroidVersionTag(Set<String> tagSet){
    	if(tagSet==null){
    		return ;
    	}
    	 
    	String androidVersion = android.os.Build.VERSION.RELEASE;
    	if(TextUtils.isEmpty(androidVersion)){
    		androidVersion = "unknown" ;
    	}else{
    		androidVersion = androidVersion.replace(".", "_");
    	}
    	 
    	StringBuilder builder = getStringBuilder();
    	builder.append("Android_");
    	builder.append(androidVersion);
    	 
    	tagSet.add(builder.toString());
    }
     
    //根据APP 版本号 、渠道设置标签
	private static void setAppInfoTag(Context context, Set<String> tagSet) {
		if ((context == null) || (tagSet == null)) {
			return;
		}

		String appVersion = InfoUtils.getVersionName(context);
		String channel = DeviceInfo.getUmengChannel(context);

		if (TextUtils.isEmpty(appVersion)) {
			appVersion = context.getString(R.string.unknown);
		} else {
			appVersion = appVersion.replace(".", "_");
		}

		if (TextUtils.isEmpty(channel)) {
			channel = "channel_unknwon";
		}

		StringBuilder appStrBuilder = getStringBuilder();
		appStrBuilder.append("app_");
		appStrBuilder.append(appVersion);

		StringBuilder vcBuilder = getStringBuilder();
		vcBuilder.append("vc_");
		vcBuilder.append(InfoUtils.getVersionCode(context));

		tagSet.add(appStrBuilder.toString());
		tagSet.add(channel);
		tagSet.add(vcBuilder.toString());
	}
	     
    //设置运营商标签
    private static void setOperaterTag(Context context , Set<String> tagSet){
    	if((context==null)||(tagSet==null)){
    		return ;
    	}
    	 
    	String phoneOperater = getPhoneOperater(context) ;
    	if(TextUtils.isEmpty(phoneOperater)){
    		 phoneOperater = "Operater_Unknown" ;
    	}
    	 
    	tagSet.add(phoneOperater);
    }
     
    //按照用户登录/注册状态 、 积分设置标签
    private static void setUserInfoTag(Set<String> tagSet){
    	if(tagSet==null){
    		return ;
    	}
    	
 		String userLoginState = NOTLOGIN ;
 		String userScore = "";
 		if(UserSession.isLogined()){
 			userLoginState = LOGIN ;
 			UserInfo userInfo = UserSession.getInstance().getUserInfo();
 			if(userInfo != null) {
 				userScore = getScoreDescription(userInfo.getScore());
 			}
 		}
 		
 		tagSet.add(userLoginState);
 		if(!TextUtils.isEmpty(userScore)){
 			tagSet.add(userScore);
 		}
    }
     
    //按照指定游戏名称设置标签
    private static void setGameTag(Set<String> tagSet){
    	if(tagSet==null){
    		return ;
    	}
     	
 		List<GameInfo> installedGameInfos = GameManager.getInstance()
 				.getSupportedAndReallyInstalledGames();
 		
 		if((installedGameInfos!=null)&&(!installedGameInfos.isEmpty())){ 
 			for(GameInfo installedGameInfo :installedGameInfos){
 		    	String gameName = installedGameInfo.getAppLabel();
 		   			   
 		    	for(String tagGamInfo : GAMES){
 		   			if(gameName.contains(tagGamInfo)){
 		   				tagSet.add(tagGamInfo);
 		   				break;
 		   			}
 		   		}
 		   	}			 
 		}else{
 			tagSet.add(NOGAME);
 		}
    }
     
	private static String getPhoneOperater(Context context){
		String imsi = InfoUtils.getIMSI(context);
		return PhoneNumber.getIMSIType(imsi).strValue;	 
	}
	 
	private static String getScoreDescription(int score){
		String scoreDescription = "" ;
		if((score>100)&&(score<=200)){
			scoreDescription = SCORE100W;
		}else if((score>200)&&(score<=400)){
			scoreDescription = SCORE200W;
		}else if(score>400){
			scoreDescription = SCORE400W;
		}
		 
		return scoreDescription;
	}
	 
	public static void onActivityResume(Activity a) {
		if (ENABLED) {
			JPushInterface.onResume(a);
		}
	}
	 
	public static void onActivityPause(Activity a) {
		if (ENABLED) {
			JPushInterface.onPause(a);
		}
	}

	public static void init(Context context) {
		if (ENABLED) {
			JPushInterface.setDebugMode(false);					 	// 发布时设为false，调试时设为true;
			JPushInterface.init(context.getApplicationContext());	// 初始化 JPush  
		}
	}

	public static void requestPermission(Context context) {
		if (ENABLED) {
			JPushInterface.requestPermission(context);
		}
	}
	
	public static String getExtraName() {
		return JPushInterface.EXTRA_EXTRA;
	}
}
