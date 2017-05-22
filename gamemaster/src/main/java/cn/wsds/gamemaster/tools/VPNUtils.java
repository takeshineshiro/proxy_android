package cn.wsds.gamemaster.tools;

import android.os.RemoteException;
import android.util.Log;

import com.subao.common.Logger;
import com.subao.common.ProxyEngineCommunicator;
import com.subao.common.data.AccelGame;

import java.util.ArrayList;
import java.util.List;

import cn.wsds.gamemaster.AppInitializer;
import cn.wsds.gamemaster.LogTagGame;
import cn.wsds.gamemaster.service.aidl.IGameVpnService;
import cn.wsds.gamemaster.service.aidl.VpnAccelGame;
import cn.wsds.gamemaster.service.aidl.VpnSupportGame;
import cn.wsds.gamemaster.ui.UIUtils;

/**
 *Created by QinYanjun , 07.05.2016
 * 防清理版本需求：VPNManager的方法只对GameVPNService可见，
 * 因此UI层对于VPNManager的访问皆需通过VPNUtils方法来完成
 */
public class VPNUtils implements ProxyEngineCommunicator {
	
	private static final String TAG = LogTagGame.TAG;
	private static final boolean ISDEBUG = false ;
	public static  final VPNUtils instance = new VPNUtils();
	
    public static IGameVpnService getIGameVpnService(){
    	return AppInitializer.instance.getIVpnService() ;
    }


	public static int init(String tag){
    	IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag, "init()");			 
			return -1;
		}

		try {
			return iVpnService.vpnInit();
		} catch (RemoteException e) {
			printLogs(tag,e);
			return -1;
		}
    }

	public static void sendPutSupportGame(VpnSupportGame supportGame , String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"sendPutSupportGame()");	
			return ;
		}
		
		try {
			iVpnService.vpnSendPutSupportGame(supportGame);
			if (Logger.isLoggableDebug(tag)) {
				Logger.d(tag, "New Support Game: " + supportGame);
			}
		} catch (RemoteException e) {			 
			printLogs(tag,e);
		}	
	}
	
	public static void sendPutSupportGames(List<VpnSupportGame> supportGames , String tag){ // init null 
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"sendPutSupportGames()");	
			return ;
		}
		
		try {
			iVpnService.vpnSendPutSupportGames(supportGames);
			if (Logger.isLoggableDebug(tag)) {
				Logger.d(tag, "New Support Game: " + supportGames);
			}
		} catch (RemoteException e) {			 
			printLogs(tag,e);
		}		
	}
	
	/**
     * 设置log打印级别
     * @param level
     */
	public static void sendSetLogLevel(int level, String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"sendSetLogLevel()");
			return ;
		}
		
		try {
			iVpnService.vpnSendSetLogLevel(level);
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static int checkSocketState(String tag){  // init null
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"checkSocketState()");
			return -1; // FIXME: 17-3-28 hujd
		}
		
		try {
			return iVpnService.vpnCheckSocketState();
		} catch (RemoteException e) {
			printLogs(tag,e);
			return -1; //// FIXME: 17-3-28 hujd
		}	
		
	}
	
	public static int getAccelStatus(String tag){  // init null 
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"getAccelStatus()"); 
			return -1;
		}
		
		int state = -1 ;
		try {
			state = iVpnService.getVpnAccelState() ;
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
		
		return state ;
	}
	
	public static boolean networkCheck(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"networkCheck()");	
			return false;
		}
		
		try {
			return iVpnService.vpnNetworkCheck();
		} catch (RemoteException e) {
			printLogs(tag,e);
			return false ;
		}		
	}
	
	public static boolean setRootMode(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"setRootMode()");
			return false;
		}
		
		try {
			return iVpnService.vpnSetRootMode();
		} catch (RemoteException e) {
			printLogs(tag,e);
			return false ;
		}	
	}
	
	public static boolean startProxy(int mode , int vpnfd, String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"startProxy()");	
			return false;
		}
		try {
			return iVpnService.vpnStartProxy(mode ,vpnfd);
		} catch (RemoteException e) {
			printLogs(tag,e);
			return false ;
		}
		
	}
	
	public static void stopProxy(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"stopProxy()");	
			return ;
		}
		  
		try {
			iVpnService.vpnStopProxy();
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static void sendUnionAccelSwitch(boolean checked, String tag){  // init null 
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"sendUnionAccelSwitch()");
			return ;
		}
		
		try {
			iVpnService.vpnSendUnionAccelSwitch(checked);
		} catch (RemoteException e) {					 
			printLogs(tag,e);
		}
	}
	
	public static boolean isNodeAlreadyDetected(int uid, String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"isNodeAlreadyDetected()");
			return false;
		}
		boolean isStarted = false ;
		try {
			isStarted = (iVpnService.vpnIsNodeAlreadyDetected(uid));
		} catch (RemoteException e) {			 
			printLogs(tag,e);
			 
		}
		
		return isStarted;
	}
	
	/**
     * 启动节点测速
     * @param gameUID 游戏UID
     * @param force
     */
	public static void startNodeDetect(int gameUID, boolean force, String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"startNodeDetect()");	
			return ;
		}
		 
		try {
			iVpnService.vpnStartNodeDetect(gameUID, force);
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
		
	}
	
	public static String getAppLogCache(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"getAppLogCache()");	
			return null;
		}
		
		String logs = "" ;
		try {
			logs = iVpnService.vpnGetAppLogCache();
		} catch (RemoteException e) {		 
			printLogs(tag,e);
		}	
		return logs ;
	}
	
	public static void openQosAccelResult(int id, String speedId, int error ,String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"openQosAccelResult()");	
			return ;
		}
		
		try {
			iVpnService.vpnOpenQosAccelResult(id, speedId, error);
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static void modifyQosAccelResult(int id, int timeSeconds, int error, String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"modifyQosAccelResult()");
			return ;
		}
		
		try {
			iVpnService.vpnModifyQosAccelResult(id, timeSeconds, error);
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static void closeQosAccelResult(int id, int error , String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"closeQosAccelResult()");
			return ;
		}
		
		try {
			iVpnService.vpnCloseQosAccelResult(id, error);
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static void sendSetNetworkState(int type , String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"sendSetNetworkState()");
			return ;
		}
		
		try {
			iVpnService.vpnSendSetNetworkState(type);
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}

	
	public static void sendStartGameDelayDetect(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"sendStartGameDelayDetect()");		
			return ;
		}
		
		try {
			iVpnService.vpnSendStartGameDelayDetect();
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static void sendStopGameDelayDetect(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"sendStopGameDelayDetect()");		
			return ;
		}
		
		try {
			iVpnService.vpnSendStopGameDelayDetect();
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static void sendSetFrontGameUid(int uid ,String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"sendSetFrontGameUid()");			
			return ;
		}
		
		try {
			iVpnService.vpnSendSetFrontGameUid(uid);
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	/*@Override
	public void sendSetJNIValue(int key, boolean value) {
		IGameVpnService iVpnService = getIGameVpnService();
		if (iVpnService == null) {
			showErrorToast(TAG, "sendSetJNIValue(int, boolean)");
			return;
		}

		try {
			iVpnService.vpnSendSetJNIBooleanValue(key, value);
		} catch (RemoteException e) {
			printLogs(TAG, e);
		}
	}
    
	@Override
	public void sendSetJNIValue(int key, int value) {
		IGameVpnService iVpnService = getIGameVpnService();
		if (iVpnService == null) {
			showErrorToast(TAG, "sendSetJNIValue(int, int)");
			return;
		}

		try {
			iVpnService.vpnSendSetJNIIntValue(key, value);
		} catch (RemoteException e) {
			printLogs(TAG, e);
		}
	}

	@Override
	public void sendSetJNIValue(int key, String value) {
		IGameVpnService iVpnService = getIGameVpnService();
		if (iVpnService == null) {
			showErrorToast(TAG, "sendSetJNIValue(int, String)");
			return;
		}
		try {
			iVpnService.vpnSendSetJNIStringValue(key, value);
		} catch (RemoteException e) {
			printLogs(TAG, e);
		}
	}
	
	@Override
	public void sendSetJNIIntegers(Iterable<Pair<Integer, Integer>> keyValues) {
		if (keyValues == null) {
			return;
		}
		IGameVpnService iVpnService = getIGameVpnService();
		if (iVpnService == null) {
			showErrorToast(TAG, "sendSetJNIIntegers()");
			return;
		}
		List<JNIKeyValue> list = new ArrayList<JNIKeyValue>(8);
		for (Pair<Integer, Integer> pair : keyValues) {
			Integer key = pair.first;
			Integer value = pair.second;
			if (key != null && value != null) {
				list.add(new JNIKeyValue(key, value));
			}
		}
		if (!list.isEmpty()) {
			try {
				iVpnService.vpnSendSetJNIValues(list);
			} catch (RemoteException e) {
				printLogs(TAG, e);
			}
		}
	}*/

	private static void printLogs(String tag, Exception e) {
		printLogs(tag, e.toString());
	}
	
	private static void printLogs(String tag, String msg) {
		if (Logger.isLoggable(TAG, Log.ERROR)) {
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			builder.append(tag);
			builder.append("]");
			builder.append(" ");
			builder.append(msg);
			Log.e(TAG, builder.toString());
		}
	}
	
	private static void showErrorToast(String tag ,String methodName){
		if(ISDEBUG){
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			builder.append(tag);
			builder.append("]");
			builder.append("iVpnService == null !!");
			builder.append("method: ");
			builder.append(methodName);
			UIUtils.showToast(builder.toString());
		}	
	}

	public static void setUserToken(String openId, String token, String appId,String tag) {
    	IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"setUserToken()");			
			return ;
		}
		
		try {
			iVpnService.setUserToken(openId, token, appId);
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static void registCellularWatcher(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"registCellularWatcher()");			
			return ;
		}
		
		try {
			iVpnService.registCellularWatcher();
		} catch (RemoteException e) {
			printLogs(tag,e);
		}
	}
	
	public static String getVIPValidTime(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag,"getVIPValidTime()");			
			return null;
		}
		
		try {
			return iVpnService.getVIPValidTime();
		} catch (RemoteException e) {
			printLogs(tag,e);
			return null ;
		}
	}

	/**
	 * 获取加速状态
	 * @return 1代表有资格试用，2代表免费试用中， 3代表试用结束， 4代表VIP使用中，5代表VIP到期
	 */
	public static int getAccelerationStatus() {
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast("","getAccelerationStatus()");
			return -1;
		}

		try {
			return iVpnService.getAccelerationStatus();
		} catch (RemoteException e) {
			return -1;
		}
	}
	
	public static boolean isBinderEnable(){
		return getIGameVpnService()!=null ;
	}

	@Override
	public void setInt(int cid, String key, int value) {

	}

	@Override
	public void setString(int cid, String key, String value) {

	}

	@Override
	public void defineConst(String key, String value) {

	}

	/**
	 * 获取游戏列表
	 */
	public static List<AccelGame> getAccelGameList(String tag){
		IGameVpnService iVpnService = getIGameVpnService() ;
		if(iVpnService == null){
			showErrorToast(tag, "init()");
			return null;
		}

		try {
			List<VpnAccelGame> vpnAccelGames = iVpnService.getAccelGameList();
			if (vpnAccelGames == null) {
				return null;
			}

			List<AccelGame> accelGameList = new ArrayList<>(vpnAccelGames.size());
			for (VpnAccelGame vpnAccelGame : vpnAccelGames) {
				AccelGame.Builder builder = new AccelGame.Builder();
				builder.setWhiteIps(vpnAccelGame.getWhiteIps());
				builder.setFlags(vpnAccelGame.getFlags());
				builder.setAccelMode(vpnAccelGame.getAccelMode());
				builder.setBlackIps(vpnAccelGame.getBlackIps());
				builder.setBlackPorts(vpnAccelGame.getBlackPorts());
				builder.setWhitePorts(vpnAccelGame.getWhitePorts());
				AccelGame accelGame = builder.build(vpnAccelGame.getAppLabel());
				accelGameList.add(accelGame);
			}
			return accelGameList;
		} catch (RemoteException e) {
			printLogs(tag,e);
			return null;
		}
	}
}
