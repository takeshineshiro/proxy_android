package cn.wsds.gamemaster.net;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

public class NetTypeDetectorImpl implements NetTypeDetector {
	
	private static final NetTypeDetectorImpl instance = new NetTypeDetectorImpl();
	
	public static NetTypeDetector getInstance() {
		return instance;
	}
	
	private NetTypeDetectorImpl() {}

	@Override
	public NetTypeDetector.NetType getCurrentNetworkType() {
		return NetManager.getInstance().getCurrentNetworkType();
	}

	@Override
	public boolean isConnected() {
		return NetManager.getInstance().isConnected();
	}

	@Override
	public boolean isMobileConnected() {
		return NetManager.getInstance().isMobileConnected();
	}

	@Override
	public boolean isWiFiConnected() {
		return NetManager.getInstance().isWiFiConnected();
	}

}
