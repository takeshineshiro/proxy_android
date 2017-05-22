package com.subao.common.jni;

import com.subao.common.ErrorCode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * JniCallbackNullTest
 * <p>Created by YinHaiBo on 2017/2/3.</p>
 */
public class JniCallbackNullTest {

    private final JniCallbackNull jniCallbackNull = new JniCallbackNull();

    @Test
    public void fo() {
        jniCallbackNull.onProxyActive(true);
    }

    @Test
    public void requestUserAuth() throws Exception {
        jniCallbackNull.requestUserAuth(0, "a", "b", "c");
    }

    @Test
    public void requestLinkAuth() throws Exception {
        jniCallbackNull.requestLinkAuth(0, "a", "b");
    }

    @Test
    public void requestUserConfig() throws Exception {
        jniCallbackNull.requestUserConfig(1, "u", "t");
    }

    @Test
    public void requestUserState() throws Exception {
        jniCallbackNull.requestUserState(0, "u", "t");
    }

    @Test
    public void requestMobileFD() throws Exception {
        jniCallbackNull.requestMobileFD(0);
    }

    @Test
    public void onLinkMessage() throws Exception {
        jniCallbackNull.onLinkMessage("message id", "message body", false);
    }

    @Test
    public void openQosAccel() {
        jniCallbackNull.openQosAccel(0, "", "", "sourIp", 123, "destIp", 456, "TCP", 300);
    }

    @Test
    public void closeQosAccel() {
        jniCallbackNull.closeQosAccel(0, "sessionId", "node", "token");
    }

    @Test
    public void modifyQosAccel() {
        jniCallbackNull.modifyQosAccel(0, "sessionId", "node", "token", 300);
    }

    @Test
    public void requestISPInformation() {
        jniCallbackNull.requestISPInformation(0);
    }

    @Test
    public void onQosMessage() {
        jniCallbackNull.onQosMessage("msg");
    }

    @Test
    public void onEventMessage() {
        jniCallbackNull.onJNIReportEvent("event");
    }

    @Test
    public void onLuaError() {
        jniCallbackNull.onLuaError("lua");
    }

    @Test
    public void onAccelInfoUpload() {
        jniCallbackNull.onAccelInfoUpload("content", "userId", "jwtToken");
    }

    @Test
    public void requestSaveData() {
        jniCallbackNull.requestSaveData("name", "value");
    }

    @Test
    public void requestLoadData() {
        jniCallbackNull.requestLoadData(12, "name");
    }


    @Test
    public void requestBeaconCounter() {
        jniCallbackNull.requestBeaconCounter(1, "test");
    }

    @Test
    public void protectFD() {
        assertEquals(ErrorCode.VPN_SERVICE_NOT_EXISTS, jniCallbackNull.protectFD(12));
    }
}