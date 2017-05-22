package com.subao.vpn;

/**
 * VPN事件的观察者
 */
public interface VpnEventObserver {

    /**
     * 当VPN代理状态发生改变时，<b>在主线程中</b>被调用
     *
     * @param active true表示VPN代理开，false表示VPN代理关
     */
    void onVPNStateChanged(boolean active);

}
