package com.subao.common.jni;

import com.subao.common.ErrorCode;
import com.subao.vpn.JniCallback;

/**
 * JniCallbackNull
 * <p>Created by YinHaiBo on 2017/1/15.</p>
 */

public class JniCallbackNull implements JniCallback {

    @Override
    public void onProxyActive(boolean open) {

    }

    @Override
    public void requestUserAuth(int cid, String userId, String token, String appId) {

    }

    @Override
    public void requestLinkAuth(int cid, String nodeIP, String jwtToken) {

    }

    @Override
    public void requestUserConfig(int cid, String userId, String jwtToken) {

    }

    @Override
    public void requestUserState(int cid, String userId, String jwtToken) {

    }

    @Override
    public void requestMobileFD(int cid) {

    }

    @Override
    public void requestISPInformation(int cid) {

    }

    @Override
    public void onLinkMessage(String msgId, String msgContent, boolean end) {

    }

    @Override
    public void openQosAccel(int cid, String node, String accessToken, String sourIp, int sourPort, String destIp, int destPort, String protocol, int timeSeconds) {

    }

    @Override
    public void closeQosAccel(int cid, String sessionId, String node, String accessToken) {

    }

    @Override
    public void modifyQosAccel(int cid, String sessiongId, String node, String accessToken, int timeSeconds) {

    }

    @Override
    public void onQosMessage(String message) {

    }

    @Override
    public void onJNIReportEvent(String message) {

    }

    @Override
    public void onLuaError(String content) {

    }

    @Override
    public void onAccelInfoUpload(String content, String userId, String jwtToken) {

    }

    @Override
    public void requestSaveData(String name, String value) {

    }

    @Override
    public void requestLoadData(int cid, String name) {

    }

    @Override
    public void requestBeaconCounter(int cid, String counterName) {

    }

    @Override
    public int protectFD(int socket) {
        return ErrorCode.VPN_SERVICE_NOT_EXISTS;
    }
}
