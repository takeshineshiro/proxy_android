package com.subao.common.jni;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.data.Defines;
import com.subao.common.msg.Message_DeviceInfo;
import com.subao.common.msg.Message_VersionInfo;
import com.subao.common.net.Protocol;
import com.subao.vpn.JniCallback;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * JniWrapperTest
 * <p>Created by YinHaiBo on 2017/1/29.</p>
 */
@Config(shadows = ShadowVPNJni.class)
public class JniWrapperTest extends RoboBase {

    private MockJniWrapper jniWrapper;

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        jniWrapper = new MockJniWrapper();
        ShadowVPNJni.cleanup();
    }

    @After
    public void tearDown() {
        jniWrapper.dispose();
        jniWrapper = null;
        Logger.setLoggableChecker(null);
    }

    @Test
    public void strToBytes() {
        assertEquals(0, JniWrapper.strToBytes(null).length);
        assertEquals(0, JniWrapper.strToBytes("").length);
        String s = "hello";
        assertEquals(s, new String(JniWrapper.strToBytes(s)));
    }

    @Test
    public void transIpStringToInt() {
        int ip = JniWrapper.transIpStringToInt("256.2.3.4");
        assertEquals(0, ip);
        ip = JniWrapper.transIpStringToInt("1.2.3.4");
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            assertEquals(0x04030201, ip);
        } else {
            assertEquals(0x01020304, ip);
        }
    }


    @Test
    public void setCallback() {
        JniCallback jc = new JniCallbackNull();
        JniCallback old = jniWrapper.setJniCallback(jc);
        assertNotNull(old);
        assertEquals(jc, jniWrapper.getJniCallback());
        assertEquals(jc, jniWrapper.setJniCallback(old));
    }

    @Test
    public void initJNI() {
        assertTrue(jniWrapper.initJNI(1, InitJNIMode.UDP, null, null, null));
    }

    @Test
    public void startProxy() {
        jniWrapper.startProxy();
    }

    @Test
    public void stopProxy() {
        jniWrapper.stopProxy();
    }

    @Test
    public void injectScript() {
        jniWrapper.injectScript("test script");
        jniWrapper.injectScript("test script".getBytes());
    }

    @Test
    public void processEvent() {
        jniWrapper.processEvent();
    }

    @Test
    public void setInt() {
        jniWrapper.setInt(0, "key", 1234);
    }

    @Test
    public void setUDPEchoPort() {
        jniWrapper.setUDPEchoPort(222);
    }

    @Test
    public void setUserToken() {
        jniWrapper.setUserToken("user", "token", "appid");
    }

    @Test
    public void setMessageInformation() {
        Message_VersionInfo version = Message_VersionInfo.create(
            "version", "channel"
        );
        Message_DeviceInfo deviceInfo = new Message_DeviceInfo(getContext());
        jniWrapper.setMessageInformation(version, deviceInfo);
        //
        assertEquals(9, jniWrapper.set.size());
        assertEquals(version.number, jniWrapper.set.get(Defines.VPNJniStrKey.KEY_VERSION));
        assertEquals(version.channel, jniWrapper.set.get(Defines.VPNJniStrKey.KEY_CHANNEL));
        assertEquals(version.osVersion, jniWrapper.set.get(Defines.VPNJniStrKey.KEY_OS_VERSION));
        assertEquals(version.androidVersion, jniWrapper.set.get(Defines.VPNJniStrKey.KEY_ANDROID_VERSION));
        //
        assertEquals(deviceInfo.getModel(), jniWrapper.set.get(Defines.VPNJniStrKey.KEY_PHONE_MODEL));
        assertEquals(deviceInfo.getROM(), jniWrapper.set.get(Defines.VPNJniStrKey.KEY_ROM));
        assertEquals(deviceInfo.getCpuSpeed(), jniWrapper.set.get(Defines.VPNJniStrKey.KEY_CPU_SPEED));
        assertEquals(deviceInfo.getCpuCore(), jniWrapper.set.get(Defines.VPNJniStrKey.KEY_CPU_CORE));
        assertEquals(deviceInfo.getMemory(), jniWrapper.set.get(Defines.VPNJniStrKey.KEY_MEMORY));
    }

    @Test
    public void userAuthResult() {
        jniWrapper.userAuthResult(0, true, 0, "jwt", 2323, "sid", 2, "expired");
    }

    @Test
    public void linkAuthResult() {
        jniWrapper.linkAuthResult(0, false, 1, "1.2.3.4", "token".getBytes(), 123);
    }

    @Test
    public void userStateResult() {
        jniWrapper.userStateResult(0, false, 1, 2, "sid", "date");
    }

    @Test
    public void userConfigResult() {
        jniWrapper.userConfigResult(0, true, 0, "110");
    }

    @Test
    public void requestMobileFDResult() {
        jniWrapper.requestMobileFDResult(12, 0, 134, false);
    }

    @Test
    public void onUDPDelay() {
        jniWrapper.onUDPDelay(123);
    }

    @Test
    public void getAccelRecommendation() {
        jniWrapper.getAccelRecommendation();
    }

    @Test
    public void setRecommendationGameIP() {
        jniWrapper.setRecommendationGameIP("1.2.3.4", 222);
    }

    @Test
    public void setSDKGameServerIP() {
        jniWrapper.setSDKGameServerIP("127.0.0.1");
    }

    @Test
    public void getWebUIUrl() {
        jniWrapper.getWebUIUrl();
    }

    @Test
    public void getAccelerationStatus() {
        jniWrapper.getAccelerationStatus();
    }

    @Test
    public void getSDKUDPIsProxy() {
        jniWrapper.getSDKUDPIsProxy();
    }

    @Test
    public void getVIPValidTime() {
        jniWrapper.getVIPValidTime();
    }

    @Test
    public void openQosAccelResult() {
        jniWrapper.openQosAccelResult(0, "sessionId", "speedId", 1);
    }

    @Test
    public void closeQosAccelResult() {
        jniWrapper.closeQosAccelResult(0, 12);
    }

    @Test
    public void modifyQosAccelResult() {
        jniWrapper.modifyQosAccelResult(0, 123, 0);
    }

    @Test
    public void defineConst() {
        jniWrapper.defineConst("key", "value");
    }

    @Test
    public void injectPCode() {
        jniWrapper.injectPCode(null);
    }

    @Test
    public void requestLoadDataResult() {
        jniWrapper.requestLoadDataResult(1, null);
    }

    @Test
    public void getAccelRecommendationData() {
        jniWrapper.getAccelRecommendationData(1);
    }

    @Test
    public void onAccelRecommendationResult() {
        jniWrapper.onAccelRecommendationResult(0, true);
    }

    @Test
    public void startNodeDetect() {
        jniWrapper.startNodeDetect(10001);
    }

    @Test
    public void isNodeDetected() {
        jniWrapper.isNodeDetected(10051);
    }

    @Test
    public void addSupportGame() {
        int uid = 10051;
        String packageName = "com.subao.game";
        String label = "Hello";
        Protocol protocol = Protocol.UDP;
        jniWrapper.addSupportGame(uid, packageName, label, protocol);
        assertEquals(String.format("%d:%s:%s:%s", uid, packageName, label, protocol.lowerText),
            jniWrapper.set.get(Defines.VPNJniStrKey.KEY_ADD_GAME));
        //
        jniWrapper.set.clear();
        jniWrapper.addSupportGame(uid, packageName, null, protocol);
        assertEquals(String.format("%d:%s::%s", uid, packageName, protocol.lowerText),
            jniWrapper.set.get(Defines.VPNJniStrKey.KEY_ADD_GAME));
    }

    @Test
    public void startVPN() {
        int fd = 123;
        jniWrapper.startVPN(fd);
        assertEquals("startVPN", ShadowVPNJni.getLastCallMethodName());
        assertEquals(fd, ShadowVPNJni.getWorkflowItem(2));
    }

    @Test
    public void stopVPN() {
        jniWrapper.stopVPN();
        assertEquals("stopVPN", ShadowVPNJni.getLastCallMethodName());
    }

    private static class MockJniWrapper extends JniWrapper {

        final Map<String, Object> set = new HashMap<String, Object>();

        public MockJniWrapper() {
            super(null);
        }

        @Override
        public void setString(int cid, String key, String value) {
            super.setString(cid, key, value);
            set.put(key, value);
        }

        @Override
        public void setString(int cid, String key, byte[] value) {
            super.setString(cid, key, value);
            set.put(key, value);
        }

        @Override
        public void setInt(int cid, String key, int value) {
            super.setInt(cid, key, value);
            set.put(key, value);
        }
    }

}