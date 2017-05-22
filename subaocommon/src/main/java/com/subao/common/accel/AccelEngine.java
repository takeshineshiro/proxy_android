package com.subao.common.accel;

/**
 * AccelEngine
 * <p>Created by YinHaiBo on 2017/3/1.</p>
 */

public interface AccelEngine {

    boolean isAccelOpened();

    boolean startAccel();

    void stopAccel();

    /**
     * 启动VPN代理
     *
     * @param fd VPN接口的FD
     */
    boolean startVPN(int fd);

    /**
     * 关闭VPN代理
     */
    void stopVPN();

}
