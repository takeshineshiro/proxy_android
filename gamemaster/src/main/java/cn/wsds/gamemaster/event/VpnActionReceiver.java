package cn.wsds.gamemaster.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.service.VPNGlobalDefines;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.service.VPNGlobalDefines.VPNEvent;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.Statistic.Event;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;


public class VpnActionReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {	 
		String action = intent.getAction();
		Bundle bundle = intent.getExtras();

		if (VPNGlobalDefines.ACTION_VPN_SERVICE_CREATED.equals(action)) {
			TriggerManager.getInstance().raiseVpnServiceCreate();
		} else if (VPNGlobalDefines.ACTION_VPN_OPEN.equals(action)) {
			TriggerManager.getInstance().raiseVPNOpen();
		} else if (VPNGlobalDefines.ACTION_VPN_START_FAILED.equals(action)) {
			TriggerManager.getInstance().raiseStartVPNFailed(false);
		} else if (VPNGlobalDefines.ACTION_VPN_CLOSE.equals(action)) {
			TriggerManager.getInstance().raiseVPNClose();
		} else if (VPNGlobalDefines.ACTION_VPN_ACCEL_MANAGER_CLOSE.equals(action)) {
			if (bundle == null) {
				return;
			}
			int reason = bundle.getInt(VPNGlobalDefines.KEY_ACTION_VPN_CLOSE_REASON);
			AccelOpenManager.close(CloseReason.fromOrdinal(reason));
		} else if (VPNGlobalDefines.ACTION_VPN_ADD_EVENT.equals(action)) {
			processEvent(context, bundle);
		}	 	 
	}
	
	private  void processEvent(Context context, Bundle bundle ){
		if((context==null)||(bundle==null)){
			return ;
		}
		
		int id = bundle.getInt(VPNGlobalDefines.KEY_VPN_EVENT_ID);
	    Statistic.Event event = getEvent(id) ;
		 
	    if(event == null){
	    	return ;
	    }
	    
		switch(event){
		case NETWORK_VPN_STOP_SEASON:
		     {
		    	String param = bundle.getString(VPNGlobalDefines.KEY_ACTION_VPN_EVENT_PARAM,"");		 
	            Statistic.addEvent(context, event, param);
		     }             
			 break;
		case CLOSE_VPN_BY_PROXY_MODEL:
		     {
		    	int reason = bundle.getInt(VPNGlobalDefines.KEY_ACTION_VPN_CLOSE_REASON);	 
		 		CloseReason closeReson =  CloseReason.fromOrdinal(reason);
		 		
		    	 if (CloseReason.BY_PROXY.equals(closeReson)) {
		 			StringBuilder sb = new StringBuilder(256);
		 			GameInfo game = TaskManager.getInstance().getCurrentForegroundGame();
		 			if (game != null) {
		 				sb.append("game");
		 			} else if (TaskManager.getInstance().amIForeground(context)) {
		 				sb.append("myself");
		 			} else {
		 				sb.append("other");
		 			}
		 			sb.append(',');
		 			sb.append(android.os.Build.MODEL);
		 				 			
		 			Statistic.addEvent(context, Statistic.Event.CLOSE_VPN_BY_PROXY_MODEL, sb.toString()); 
		 		}
		     }
		break;
		default:
			break ;
	    }
	}
	
	private  final Statistic.Event getEvent(int id){
		Statistic.Event event = null;
		 
		if(VPNEvent.NETWORK_VPN_STOP_SEASON.ordinal()==id){
			return Event.NETWORK_VPN_STOP_SEASON;
		}else if(VPNEvent.CLOSE_VPN_BY_PROXY_MODEL.ordinal()==id){
			return Event.CLOSE_VPN_BY_PROXY_MODEL;
		}
		 
		return event;
	}
}
