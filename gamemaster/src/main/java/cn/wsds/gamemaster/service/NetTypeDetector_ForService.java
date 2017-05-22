package cn.wsds.gamemaster.service;

import com.subao.common.net.NetTypeDetector;

public class NetTypeDetector_ForService implements NetTypeDetector {
	
	private static final NetTypeDetector_ForService instance = new NetTypeDetector_ForService();
	
	private NetType currentNetworkType = NetType.UNKNOWN;
	
	public static NetTypeDetector_ForService getInstance() {
		return instance;
	}
	
	private NetTypeDetector_ForService() {}
	
	public void onNetChange(NetType netType) {
		this.currentNetworkType = netType;
	}

	@Override
	public NetType getCurrentNetworkType() {
		return this.currentNetworkType;
	}

	@Override
	public boolean isConnected() {
		return this.currentNetworkType != NetType.DISCONNECT;
	}

	@Override
	public boolean isMobileConnected() {
		switch (this.currentNetworkType) {
		case DISCONNECT:
		case WIFI:
			return false;
		default:
			return true;
		}
	}

	@Override
	public boolean isWiFiConnected() {
		return this.currentNetworkType == NetType.WIFI;
	}

}
