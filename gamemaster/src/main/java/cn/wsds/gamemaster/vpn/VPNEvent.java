package cn.wsds.gamemaster.vpn;

import android.os.RemoteException;
import android.widget.Toast;

import com.subao.common.Logger;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.FirstNetDelayManager;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.service.aidl.IGameVpnService;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowInGame;

public class VPNEvent extends EventObserver {

	private static final String TAG = "VPNEvent";
	
	@Override
	public void onTopTaskChange(GameInfo info) {
		//VPNManager vpn = VPNManager.getInstance();
		if (info != null) {
			//vpn.sendSetFrontGameUid(info.getUid());
			VPNUtils.sendSetFrontGameUid(info.getUid(),TAG);
			FirstNetDelayManager.getInstance().startDetect();
		} else {
			FirstNetDelayManager.getInstance().stopDetect();
			VPNUtils.sendSetFrontGameUid(-1,TAG);
			//vpn.sendSetFrontGameUid(-1);
		}
		//
		FloatWindowInGame.destroyInstance();
		IGameVpnService iVpnService= VPNUtils.getIGameVpnService();
		if (info != null) {
			try {
				if(iVpnService!=null){
					iVpnService.setToForeground();
				}else{
					UIUtils.showToast(" VPNEvent:onTopTaskChange() :iVpnService == null ,iVpnService.setToForeground() failed !",Toast.LENGTH_LONG); 
				}
			} catch (RemoteException e) {				
				Logger.e(TAG, e.toString());
			}
			//GameVpnService.setToForeground();
			if (ConfigManager.getInstance().getShowFloatWindowInGame()) {
				FloatWindowInGame.createInstance(AppMain.getContext(), info, info.getWindowX(),
					info.getWindowY(), false);
			}
		} else if (!AccelOpenManager.isStarted()) {
			try {
                if(iVpnService!=null){
                	iVpnService.setToBackground();
				}else{
					UIUtils.showToast(" VPNEvent:onTopTaskChange() :iVpnService == null ,iVpnService.setToBackground() failed !",Toast.LENGTH_LONG); 
				}
				
			} catch (RemoteException e) {
				Logger.e(TAG, e.toString());
			}
			//GameVpnService.setToBackground();
		}
	}

	@Override
	public void onNetRightsDisabled() {
		UIUtils.showToast("迅游手游被禁止访问网络\r\n请检查您的防火墙或网络设置", Toast.LENGTH_LONG);
	}
}
