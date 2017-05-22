package com.subao.common.mock;

import com.subao.common.net.NetTypeDetector;

public class MockNetTypeDetector implements NetTypeDetector {

    private NetType currentNetworkType;

    public void setCurrentNetworkType(NetType value) {
        this.currentNetworkType = value;
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
    public boolean isWiFiConnected() {
        return this.currentNetworkType == NetType.WIFI;
    }

    @Override
    public boolean isMobileConnected() {
        return currentNetworkType == NetType.MOBILE_4G
            || currentNetworkType == NetType.MOBILE_3G
            || currentNetworkType == NetType.MOBILE_2G;
    }
}
