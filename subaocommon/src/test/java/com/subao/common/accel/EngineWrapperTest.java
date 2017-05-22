package com.subao.common.accel;

import android.content.Context;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.collection.Ref;
import com.subao.common.data.ConfigTest;
import com.subao.common.data.Defines;
import com.subao.common.data.HRDataTrans;
import com.subao.common.jni.InitJNIMode;
import com.subao.common.jni.JniWrapper;
import com.subao.common.jni.ShadowVPNJni;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.msg.MessageSender;
import com.subao.common.net.NetManager;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.Protocol;
import com.subao.gamemaster.GameMaster;
import com.subao.vpn.JniCallback;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * EngineWrapperTest
 * <p>Created by YinHaiBo on 2017/1/29.</p>
 */
@org.robolectric.annotation.Config(
    shadows = {ShadowVPNJni.class, ConfigTest.ShadowPersistentFactory.class})
public class EngineWrapperTest extends RoboBase {

    public static final String GAME_GUID = "The game guid";
    private EngineWrapper engineWrapper;
    private MockJniWrapper mockJniWrapper;

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        mockJniWrapper = new MockJniWrapper();
        engineWrapper = new EngineWrapper(getContext(), Defines.ModuleType.SDK,
                GAME_GUID, "2.0.0", new MockNetManager(getContext()), mockJniWrapper,true);
    }

    @After
    public void tearDown() {
        engineWrapper.dispose();
        mockJniWrapper.dispose();
        Logger.setLoggableChecker(null);
    }

    @Test
    public void initNotInMainThread() throws InterruptedException {
        final Ref<Integer> ref = new Ref<Integer>();
        Thread t = new Thread() {
            @Override
            public void run() {
                ref.set(engineWrapper.init(InitJNIMode.UDP, null, 222, null));
            }
        };
        t.start();
        t.join();
        Assert.assertEquals(GameMaster.GM_INIT_NOT_IN_MAIN_THREAD, (int) ref.get());
    }

    @Test
    public void init() {
        initEngineWrapper();
    }

    private void initEngineWrapper() {
        engineWrapper.init(InitJNIMode.UDP, null, 222, null);
    }

    @Test
    public void start() {
        assertFalse(engineWrapper.startAccel()); // 未初始化
        assertFalse(engineWrapper.isAccelOpened());
        initEngineWrapper();
        mockJniWrapper.resultStartProxy = false;
        assertFalse(engineWrapper.startAccel());
        assertFalse(engineWrapper.isAccelOpened());
        mockJniWrapper.resultStartProxy = true;
        assertTrue(engineWrapper.startAccel());
        assertTrue(engineWrapper.isAccelOpened());
    }

    @Test
    public void stop() {
        engineWrapper.stopAccel();
        initEngineWrapper();
        assertTrue(engineWrapper.startAccel());
        engineWrapper.stopAccel();
        assertTrue(mockJniWrapper.calledStopProxy);
    }

    @Test
    public void startNodeDetect() {
        engineWrapper.startNodeDetect(10011);
        initEngineWrapper();
        engineWrapper.startNodeDetect(10011);
        assertEquals("startNodeDetect", mockJniWrapper.actions[0]);
        assertEquals(10011, mockJniWrapper.actions[1]);
    }

    @Test
    public void isNodeDetected() {
        engineWrapper.isNodeDetected(10011);
        initEngineWrapper();
        engineWrapper.isNodeDetected(10011);
        assertEquals("isNodeDetected", mockJniWrapper.actions[0]);
        assertEquals(10011, mockJniWrapper.actions[1]);
    }

    @Test
    public void testAuthExecutorController() throws Exception {
        MessageSender messageSender = mock(MessageSender.class);
        MockNetTypeDetector mockNetTypeDetector = new MockNetTypeDetector();
        EngineWrapper.AuthExecutorController target = new EngineWrapper.AuthExecutorController(mockNetTypeDetector, messageSender);
        mockNetTypeDetector.setCurrentNetworkType(NetTypeDetector.NetType.DISCONNECT);
        assertFalse(target.isNetConnected());
        mockNetTypeDetector.setCurrentNetworkType(NetTypeDetector.NetType.WIFI);
        assertTrue(target.isNetConnected());
        //
        mockNetTypeDetector = new MockNetTypeDetector();
        target = new EngineWrapper.AuthExecutorController(mockNetTypeDetector, mock(MessageSender.class));
        assertNotNull(target.getEventReporter());
    }

    @Test
    public void getNetProtocol() {
        assertEquals(3, InitJNIMode.values().length);
        assertEquals(Protocol.TCP, EngineWrapper.getNetProtocol(InitJNIMode.TCP));
        assertEquals(Protocol.UDP, EngineWrapper.getNetProtocol(InitJNIMode.UDP));
        assertEquals(Protocol.UDP, EngineWrapper.getNetProtocol(InitJNIMode.VPN));  // FIXME ???
    }

    @Test
    public void freeFlowType() {
        assertEquals(3, EngineWrapper.FreeFlowType.values().length);
        assertEquals(0, EngineWrapper.FreeFlowType.CMCC.id);
        assertEquals(1, EngineWrapper.FreeFlowType.CNC.id);
        assertEquals(2, EngineWrapper.FreeFlowType.CTC.id);
    }

    @Test
    public void openProxyLog() {
        initEngineWrapper();
        engineWrapper.openProxyLog();
    }

    @Test
    public void getHRArguments() {
        initEngineWrapper();
        HRDataTrans.Arguments arguments = engineWrapper.getHRArguments();
        assertNotNull(arguments.clientType);
        assertNotNull(arguments.serviceLocation);
    }

    @Test
    public void testCellularStateListener() {
        EngineWrapper.CellularStateListener listener = new EngineWrapper.CellularStateListener(
            getContext(), this.mockJniWrapper
        );
        listener.onCellularStateChange(true);
        assertEquals(Defines.VPNJniStrKey.KEY_CELLULAR_STATE_CHANGE, mockJniWrapper.setIntKey);
        assertEquals(1, mockJniWrapper.setIntValue);
        //
        mockJniWrapper.setIntKey = null;
        listener.onCellularStateChange(false);
        assertEquals(Defines.VPNJniStrKey.KEY_CELLULAR_STATE_CHANGE, mockJniWrapper.setIntKey);
        assertEquals(0, mockJniWrapper.setIntValue);
    }

    public static class MockJniWrapper extends JniWrapper {

        boolean disposed;
        JniCallback jniCallback;
        boolean resultInit = true;
        boolean resultStartProxy = true;

        boolean calledStopProxy;
        int countProcessEvent;

        public String setIntKey;
        public int setIntValue;

        public String setStringKey;
        public String setStringValue;

        int accelRecommendation;
        int udpUDPEchoPort;

        public String recommendationGameIP;
        public int recommendationGamePort;

        public String lastMethod;

        public String webUIUrl;

        public final List arguments = new ArrayList(10);

        public Object[] actions;

        public MockJniWrapper() {
            super(null);
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public JniCallback setJniCallback(JniCallback jniCallback) {
            JniCallback old = this.jniCallback;
            this.jniCallback = jniCallback;
            return old;
        }

        @Override
        public boolean initJNI(int netState, InitJNIMode mode, byte[] luaPCode, String nodeList, String convergenceNodeList) {
            return resultInit;
        }

        @Override
        public boolean startProxy() {
            return resultStartProxy;
        }

        @Override
        public void stopProxy() {
            calledStopProxy = true;
        }

        @Override
        public int getAccelRecommendation() {
            return accelRecommendation;
        }

        @Override
        public String getAccelRecommendationData(int type) {
            return "";
        }

        @Override
        public void processEvent() {
            ++countProcessEvent;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }

        @Override
        public void setInt(int cid, String key, int value) {
            this.setIntKey = key;
            this.setIntValue = value;
        }

        @Override
        public void setString(int cid, String key, String value) {
            this.setStringKey = key;
            this.setStringValue = value;
        }

        @Override
        public void setUserToken(String userId, String token, String appid) {
            arguments.add(userId);
            arguments.add(token);
            arguments.add(appid);
        }

        @Override
        public String getWebUIUrl() {
            return this.webUIUrl;
        }

        @Override
        public void onUDPDelay(int millis) {
            arguments.add(millis);
        }

        @Override
        public void setUDPEchoPort(int port) {
            this.udpUDPEchoPort = port;
        }

        @Override
        public void setRecommendationGameIP(String ip, int port) {
            recommendationGameIP = ip;
            recommendationGamePort = port;
        }

        @Override
        public String getVIPValidTime() {
            return this.lastMethod = "getVIPValidTime";
        }

        @Override
        public void startNodeDetect(int uid) {
            actions = new Object[] {
                "startNodeDetect", uid
            };
        }

        @Override
        public boolean isNodeDetected(int uid) {
            actions = new Object[] {
                "isNodeDetected", uid
            };
            return false;
        }
    }

    public static class MockNetManager extends NetManager {

        NetType currentNetType = NetType.DISCONNECT;

        /**
         * 必须在主线程里被调用
         *
         * @param context
         */
        public MockNetManager(Context context) {
            super(context);
        }

        @Override
        public NetType getCurrentNetworkType() {
            return this.currentNetType;
        }
    }
}