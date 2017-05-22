package cn.wsds.gamemaster.service.aidl;
import java.util.List;
import cn.wsds.gamemaster.service.aidl.JNIKeyValue;
import cn.wsds.gamemaster.service.aidl.VpnSupportGame;
import cn.wsds.gamemaster.service.aidl.VPNStartParamTransporter;
import cn.wsds.gamemaster.service.aidl.VpnAccelGame;

interface IGameVpnService{
 
    boolean isVPNStarted();
    void setToForeground();
    void setToBackground();
    void closeVPN(in int reason);
    int startVPN(in List<String> supportPackageNames);
    boolean isIpEqualInterfaceAddress(in byte[] ip);
    boolean protect(in int fd);
    int getVpnAccelState();   
    boolean isInitFailException();

	int vpnInit();
	void vpnSendPutSupportGame(in VpnSupportGame supportGame);
	void vpnSendPutSupportGames(in List<VpnSupportGame> supportGames);
	void vpnSendSetLogLevel(in int level);
	int vpnCheckSocketState();
	int vpnGetAccelStatus();	
	boolean vpnNetworkCheck();
	boolean vpnSetRootMode();
	boolean vpnStartProxy(in int mode ,in int vpnfd);
	void vpnStopProxy();
	void vpnSendUnionAccelSwitch(in boolean checked);
	boolean vpnIsNodeAlreadyDetected(in int uid); 
	void vpnStartNodeDetect(in int gameUID, in boolean force);
	String vpnGetAppLogCache();
	void vpnOpenQosAccelResult(in int id, in String speedId, in int error);
	void vpnModifyQosAccelResult(in int id, in int timeSeconds, in int error);
	void vpnCloseQosAccelResult(in int id, in int error);
	void vpnSendSetNetworkState(in int type);
	void vpnOnNewMobileNetworkFD(in int fd);
	void vpnSendStartGameDelayDetect();
	void vpnSendStopGameDelayDetect();	
	void vpnSendSetFrontGameUid(in int uid);
	void vpnSendSetJNIBooleanValue(in int key, in boolean value);
	void vpnSendSetJNIIntValue(in int key, in int value);
	void vpnSendSetJNIStringValue(in int key, in String value);
	
	int  getLocalPort();
	
	void setUserToken(in String openId, in String token, in String appId);
	
	void registCellularWatcher();
	
	void onSubaoIdUpdate(String subaoId);
	
	void sendKeepalive();
	
	String getVIPValidTime();

	int getAccelerationStatus();
	
	void configChange(in int name, String value);

	List<VpnAccelGame> getAccelGameList();
}