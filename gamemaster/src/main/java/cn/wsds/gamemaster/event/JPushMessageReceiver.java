package cn.wsds.gamemaster.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.subao.common.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import cn.jpush.android.api.JPushInterface;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.message.MessageManager;
import cn.wsds.gamemaster.message.MessageManager.Record;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.ActivityMain;
import cn.wsds.gamemaster.ui.ActivityMessage;
import cn.wsds.gamemaster.ui.ActivityMessageView;
import cn.wsds.gamemaster.ui.ActivityUser;
import cn.wsds.gamemaster.ui.exchange.ActivityExchangeCenter;

/**
 * Created by Qinyanjun on 16-05-03.
 * This class is for processing to received Jpush notifications or messages
 */

public class JPushMessageReceiver extends BroadcastReceiver{
	
	private static final String TAG = "SuBaoJPush";
	
	private static final String CATEGORY_KEY = "category";	
	private static final String CATEGORY_VALUE_SHARE = "share";
	
	private static final String CATEGORY_VALUE_MAIN = "main";
	private static final String CATEGORY_VALUE_USERCENTER = "usercenter";
	private static final String CATEGORY_VALUE_EXCHANGE = "exchange";	
	private static final String CATEGORY_VALUE_MESSAGE = "messagecenter";
	
	private static final String URL_KEY = "url";
	
	/**
	 *  极光消息（标题、内容）
	 */
	private static final class Message{
		private final String title;
		private final String content;
		
		Message(String title , String content){
			this.title = title;
			this.content = content ;
		}
	}
	
	/**
	 *  附加信息（键值对），该信息对用户不可见，用于程序逻辑控制
	 */
	public static final class ExtraData{
		public final String key;
		public final String value ;
		
		ExtraData(String key , String value){
			this.key = key;
			this.value = value;
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		 
		 Bundle bundle = intent.getExtras();
	     StringBuilder builder = new StringBuilder();
	     
	     if(JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
	    	//首次安装，接收到极光注册ID 
	        builder.append("receive Registration Id : ");
	        builder.append(bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID));
	      
	     }else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {	    	 
	    	builder.append("[JPushMessageReceiver] onReceive - ACTION_MESSAGE_RECEIVED");
		    	
		    createMessageRecord(bundle,false);
		    
		  }else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {	    	 	    	
	    	builder.append("received push NotificationID: ");	    	
	    	builder.append(bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID));
	  
	    	//消息中心添加消息
	    	createMessageRecord(bundle,true);
	    	        	
	     } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {  
	    	builder.append("user opened notification");
	    	
	    	//打开通知，页面跳转
	        openNotification(context,bundle);     	
	     } 
	     
	     if(Logger.isLoggableDebug(TAG)){
	    	 Logger.d(TAG, builder.toString());
	     }
	}
	
	private  static void createMessageRecord(Bundle bundle, boolean isNotification){
		Statistic.addEvent(AppMain.getContext(), Statistic.Event.NOTIFICATION_PUSH_SHOWUP); 
		
		if(bundle==null){
			return ;
		}
		 
		Message message = getMessage(bundle,isNotification);
		if(message==null){
			return ;
		}
			 
		ExtraData extraData = getExtraData(bundle);		
		if((extraData!=null)&&isUrl(extraData)){			 
			MessageManager.getInstance().createLocalMessage_Notify(Record.TYPE_JPUSH_NOTIFY_URL,
					message.title, extraData.value, message.content);
			return ;			 
		}
			
		MessageManager.getInstance().createLocalMessage_Notify(Record.TYPE_JPUSH_NOTIFY_TEXT, 
				message.title, message.content, message.content);			 
	}
	
	private  static Message getMessage(Bundle bundle , boolean isNotification){
		if(bundle==null){
			return null ;
		}
		
		String title ;
		String content ;
		if(isNotification){
			title = bundle.getString(JPushInterface.EXTRA_NOTIFICATION_TITLE);;
			content = bundle.getString(JPushInterface.EXTRA_ALERT);
		}else{
			title = bundle.getString(JPushInterface.EXTRA_TITLE);
			content = bundle.getString(JPushInterface.EXTRA_MESSAGE);
		}
		
		if(TextUtils.isEmpty(title)){
			title = "消息" ;
		}
		
		return new Message(title,content);
	}

	private static void openNotification(Context context, Bundle bundle){
		Statistic.addEvent(AppMain.getContext(), Statistic.Event.NOTIFICATION_PUSH_CLICK);
		
		ExtraData extraData = getExtraData(bundle);
		if(extraData==null){
			toMainActivity(context,bundle);
			return;
		}
		
		if(isUrl(extraData)){
			toWebView(context,bundle,extraData.value,true);
		}else{
			turnActivity(context,bundle,extraData.value);
		}			
	}
	
	private static boolean isUrl(ExtraData data){
		if(data==null){
			return false ;
		}
		return ((URL_KEY.equals(data.key))&&(data.value!=null));
	}
	
	private static void toWebView(Context context ,Bundle bundle ,String url,boolean isNotifyOpen){
		if((context==null)||(bundle==null)||(url==null)){
			return;
		}
		
		Message message = getMessage(bundle,isNotifyOpen);
		if(message!=null){
			Record record = new Record(0, Record.TYPE_JPUSH_NOTIFY_URL, System.currentTimeMillis(), 
					message.title,url, message.content, false);		
			ActivityMessageView.show(context, record,true);
		}
	}
	
	private static void toMainActivity(Context context , Bundle bundle ){
		 turnActivity(context,bundle,CATEGORY_VALUE_MAIN);
	}
	
	private static void turnActivity(Context context , Bundle bundle ,String type){
		if(context==null){
			return ;
		}
		
		Intent intent ;
		if(CATEGORY_VALUE_USERCENTER.equals(type)){
			intent = new Intent(context, ActivityUser.class);
		}else if(CATEGORY_VALUE_EXCHANGE.equals(type)){
			intent = new Intent(context, ActivityExchangeCenter.class);
		}else if(CATEGORY_VALUE_SHARE.equals(type)){
			if(ActivityUser.toShareDialog()){
				return ;
			}
			intent = new Intent(context, ActivityUser.class);			
		}else if(CATEGORY_VALUE_MESSAGE.equals(type)){
			intent = new Intent(context, ActivityMessage.class);
		}else{
			intent = new Intent(context, ActivityMain.class);
		}
		
		intent.putExtras(bundle);		
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
		context.startActivity(intent);
	}
	
	private static ExtraData getExtraData(Bundle bundle){
		if(bundle == null){
			return null ;
		}
			
		String jsnStr = bundle.getString(JPushInterface.EXTRA_EXTRA) ;
		if(jsnStr==null){
			return null ;
		}

		if(!jsnStr.isEmpty()){		
    		try{
				JSONObject json = new JSONObject(jsnStr);
				Iterator<String> it =  json.keys();

				if(!it.hasNext()){
					return null;
				}
											
				String key = it.next().toString();
				String value = json.optString(key); 
				return new ExtraData(key,value);
				
			} catch (JSONException e) {
				Logger.e(TAG, "Get message extra JSON error!");
			}
		}
		
		return null ;		
	}
	
	private static boolean isJPushTurnActivity(Bundle bundle, String targetValue){
		if((bundle==null)||(TextUtils.isEmpty(targetValue))){
			return false ;
		}
		
		ExtraData data = getExtraData(bundle);
		if(data==null){
			return false ;
		}
		
		if(CATEGORY_KEY.equals(data.key)&&targetValue.equals(data.value)){			
			return true ;
		}
		 
		return false ;
	}
	
	public static boolean jpushTurnUserCenter(Bundle bundle){	
		return isJPushTurnActivity(bundle,CATEGORY_VALUE_USERCENTER) ;
	}
	
	public static boolean jpushTurnSharePage(Bundle bundle){
		return isJPushTurnActivity(bundle,CATEGORY_VALUE_SHARE) ;
	}
	
	public static boolean jpushTurnExchange(Bundle bundle){
		return isJPushTurnActivity(bundle,CATEGORY_VALUE_EXCHANGE) ;
	}
	
	public static boolean jpushTrunMessage(Bundle bundle){
		return isJPushTurnActivity(bundle,CATEGORY_VALUE_MESSAGE) ;
	}	 
}
