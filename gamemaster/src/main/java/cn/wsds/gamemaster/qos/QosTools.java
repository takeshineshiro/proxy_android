package cn.wsds.gamemaster.qos;

import android.content.Context;
import android.os.RemoteException;

import com.subao.common.Logger;
import com.subao.common.data.AppType;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.NetUtils.LocalIpFilter;
import com.subao.common.utils.InfoUtils;
import com.subao.net.NetManager;

import cn.wsds.gamemaster.data.DeviceInfo;
import cn.wsds.gamemaster.service.aidl.IGameVpnService;
import cn.wsds.gamemaster.tools.VPNUtils;

public class QosTools implements com.subao.common.qos.QosHelper.Tools {
	private static final String TAG = "QosTools" ;
	private static boolean staticIsValidLocalIp(byte[] ip) {
		
		IGameVpnService iVpnService= VPNUtils.getIGameVpnService();
		if(iVpnService == null){
			return false;
		}
		
		try {
			return !iVpnService.isIpEqualInterfaceAddress(ip);
		} catch (RemoteException e) {
			Logger.e(TAG, e.toString());
			return false ;
		}
		
		//return !GameVpnService.isIpEqualInterfaceAddress(ip);
	}

	private final Context context;
	private final String channel;
	private final String version;
	
	public static class DefaultLocalIpFilter implements LocalIpFilter {
		@Override
		public boolean isValidLocalIp(byte[] ip) {
			return staticIsValidLocalIp(ip);
		}
	}
	
	public QosTools(Context context) {
		this.context = context.getApplicationContext();
		this.channel = DeviceInfo.getUmengChannel(this.context);
		this.version = InfoUtils.getVersionName(this.context);
	}

	@Override
	public boolean isValidLocalIp(byte[] ip) {
		return staticIsValidLocalIp(ip);
	}

	@Override
	public AppType getAppType() {
		return AppType.ANDROID_APP;
	}

	@Override
	public String getChannel() {
		return this.channel;
	}

	@Override
	public NetTypeDetector getNetTypeDetector() {
		return NetManager.getInstance();
	}

	@Override
	public String getSubaoId() {
		return SubaoIdManager.getInstance().getSubaoId();
	}

	@Override
	public String getVersionNum() {
		return this.version; 
	}

	@Override
	public String getIMSI() {
		return InfoUtils.getIMSI(context);
	}

	/*@Override
	public String getPhoneNumber() {
		return null;
	}*/
}
