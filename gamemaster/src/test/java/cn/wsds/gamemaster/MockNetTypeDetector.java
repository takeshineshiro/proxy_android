package cn.wsds.gamemaster;

import com.subao.common.net.NetTypeDetector;

/**
 * MockNetTypeDetector
 * <p>Created by YinHaiBo on 2017/3/3.</p>
 */

public class MockNetTypeDetector implements NetTypeDetector {

    public NetType currentNetworkType = NetType.UNKNOWN;

    @Override
    public NetType getCurrentNetworkType() {
        return currentNetworkType;
    }

    @Override
    public boolean isConnected() {
        return currentNetworkType != NetType.DISCONNECT;
    }

    @Override
    public boolean isWiFiConnected() {
        return currentNetworkType == NetType.WIFI;
    }

    @Override
    public boolean isMobileConnected() {
        switch (currentNetworkType) {
        case MOBILE_2G:
        case MOBILE_3G:
        case MOBILE_4G:
            return true;
        default:
            return false;
        }
    }
}
