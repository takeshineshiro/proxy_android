package com.subao.vpn;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.collection.Ref;
import com.subao.common.jni.JniCallbackNull;
import com.subao.common.jni.ShadowVPNJni;
import com.subao.common.net.IPv4;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * VPNJniTest
 * <p>Created by YinHaiBo on 2017/2/17.</p>
 */
public class VPNJniTest extends RoboBase {

    @Test
    public void testConstDefines() {
        assertEquals(0, VPNJni.INIT_MODE_UDP);
        assertEquals(1, VPNJni.INIT_MODE_TCP);
        assertEquals(2, VPNJni.INIT_MODE_VPN);
    }

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        testPrivateConstructor(VPNJni.class);
    }

    @Test
    public void loadLibrary() {
        try {
            VPNJni.loadLibrary(new JniCallbackNull(), "test");
        } catch (UnsatisfiedLinkError e) {
        }
        VPNJni.loadLibrary(new JniCallbackNull(), "test");
    }

    @Test
    public void onProxyActive() {
        final Ref<Boolean> called = new Ref<Boolean>();
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertTrue((Boolean) invocation.getArgument(0));
                called.set(true);
                return null;
            }
        }).when(jniCallback).onProxyActive(anyBoolean());
        VPNJni.setCallback(jniCallback);
        assertEquals(jniCallback, VPNJni.getCallback());
        VPNJni.onProxyActive(0, true);
        assertTrue(called.get());
    }


    @Test
    public void requestMobileFD() {
        final Ref<Boolean> called = new Ref<Boolean>();
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(1, invocation.getArgument(0));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestMobileFD(anyInt());
        VPNJni.setCallback(jniCallback);
        VPNJni.requestMobileFD(1);
        assertTrue(called.get());
    }

    @Test
    public void userAuth() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 1234;
        final String userId = "user";
        final String token = "token";
        final String appId = "appId";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(userId, invocation.getArgument(1));
                assertEquals(token, invocation.getArgument(2));
                assertEquals(appId, invocation.getArgument(3));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestUserAuth(cid, userId, token, appId);
        VPNJni.setCallback(jniCallback);
        VPNJni.userAuth(cid, userId, token, appId);
        assertTrue(called.get());
    }

    @Test
    public void linkAuth() {
        final Ref<Boolean> called = new Ref<Boolean>();
        int node = 0x45325678;
        final int cid = 1234;
        final String nodeIP = IPv4.ipToString(IPv4.ntohl(node));
        final String token = "token";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(nodeIP, invocation.getArgument(1));
                assertEquals(token, invocation.getArgument(2));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestLinkAuth(cid, nodeIP, token);
        VPNJni.setCallback(jniCallback);
        VPNJni.linkAuth(cid, node, token);
        assertTrue(called.get());
    }

    @Test
    public void userConfig() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 1234;
        final String userId = "user";
        final String token = "token";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(userId, invocation.getArgument(1));
                assertEquals(token, invocation.getArgument(2));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestUserConfig(cid, userId, token);
        VPNJni.setCallback(jniCallback);
        VPNJni.userConfig(cid, userId, token);
        assertTrue(called.get());
    }

    @Test
    public void userState() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 1234;
        final String userId = "user";
        final String token = "token";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(userId, invocation.getArgument(1));
                assertEquals(token, invocation.getArgument(2));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestUserState(cid, userId, token);
        VPNJni.setCallback(jniCallback);
        VPNJni.userState(cid, userId, token);
        assertTrue(called.get());
    }

    @Test
    public void onLuaError() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final String message = "error";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(message, invocation.getArgument(0));
                called.set(true);
                return null;
            }
        }).when(jniCallback).onLuaError(message);
        VPNJni.setCallback(jniCallback);
        VPNJni.onLuaError(0, message);
        assertTrue(called.get());
    }

    @Test
    public void onLinkMessage() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String msgId = "msgId";
        final String msgBody = "msgBody";
        final boolean finish = false;
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(msgId, invocation.getArgument(0));
                assertEquals(msgBody, invocation.getArgument(1));
                assertEquals(finish, invocation.getArgument(2));
                called.set(true);
                return null;
            }
        }).when(jniCallback).onLinkMessage(msgId, msgBody, finish);
        VPNJni.setCallback(jniCallback);
        VPNJni.onLinkMessage(123, msgId, msgBody, finish);
        assertTrue(called.get());
    }

    @Test
    public void openQosAccel() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String node = "node";
        final String accessToken = "accessToken";
        final String sourIp = "sourIp";
        final int sourPort = 222;
        final String destIp = "destIp";
        final int destPort = 333;
        final String protocol = "protocol";
        final int timeSeconds = 300;
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(node, invocation.getArgument(1));
                assertEquals(accessToken, invocation.getArgument(2));
                assertEquals(sourIp, invocation.getArgument(3));
                assertEquals(sourPort, invocation.getArgument(4));
                assertEquals(destIp, invocation.getArgument(5));
                assertEquals(destPort, invocation.getArgument(6));
                assertEquals(protocol, invocation.getArgument(7));
                assertEquals(timeSeconds, invocation.getArgument(8));
                called.set(true);
                return null;
            }
        }).when(jniCallback).openQosAccel(cid, node, accessToken, sourIp, sourPort, destIp, destPort, protocol, timeSeconds);
        VPNJni.setCallback(jniCallback);
        VPNJni.openQosAccel(cid, node, accessToken, sourIp, sourPort, destIp, destPort, protocol, timeSeconds);
        assertTrue(called.get());
    }

    @Test
    public void closeQosAccel() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String sessionId = "sessionId";
        final String node = "node";
        final String accessToken = "accessToken";

        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(sessionId, invocation.getArgument(1));
                assertEquals(node, invocation.getArgument(2));
                assertEquals(accessToken, invocation.getArgument(3));
                called.set(true);
                return null;
            }
        }).when(jniCallback).closeQosAccel(cid, sessionId, node, accessToken);
        VPNJni.setCallback(jniCallback);
        VPNJni.closeQosAccel(cid, sessionId, node, accessToken);
        assertTrue(called.get());
    }

    @Test
    public void modifyQosAccel() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String sessionId = "sessionId";
        final String node = "node";
        final String accessToken = "accessToken";
        final int timeSeconds = 399;
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(sessionId, invocation.getArgument(1));
                assertEquals(node, invocation.getArgument(2));
                assertEquals(accessToken, invocation.getArgument(3));
                assertEquals(timeSeconds, invocation.getArgument(4));
                called.set(true);
                return null;
            }
        }).when(jniCallback).modifyQosAccel(cid, sessionId, node, accessToken, timeSeconds);
        VPNJni.setCallback(jniCallback);
        VPNJni.modifyQosAccel(cid, sessionId, node, accessToken, timeSeconds);
        assertTrue(called.get());
    }

    @Test
    public void onQosMessage() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String message = "message";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(message, invocation.getArgument(0));
                called.set(true);
                return null;
            }
        }).when(jniCallback).onQosMessage(message);
        VPNJni.setCallback(jniCallback);
        VPNJni.onQosMessage(cid, message);
        assertTrue(called.get());
    }

    @Test
    public void getISP() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestISPInformation(cid);
        VPNJni.setCallback(jniCallback);
        VPNJni.getISP(cid);
        assertTrue(called.get());
    }

    @Test
    public void onReportEvent() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String msg = "msg";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(msg, invocation.getArgument(0));
                called.set(true);
                return null;
            }
        }).when(jniCallback).onJNIReportEvent(msg);
        VPNJni.setCallback(jniCallback);
        VPNJni.onReportEvent(cid, msg);
        assertTrue(called.get());
    }

    @Test
    public void onAccelInfoUpload() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String content = "content";
        final String userId = "userId";
        final String jwtToken = "jwtToken";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(content, invocation.getArgument(0));
                assertEquals(userId, invocation.getArgument(1));
                assertEquals(jwtToken, invocation.getArgument(2));
                called.set(true);
                return null;
            }
        }).when(jniCallback).onAccelInfoUpload(content, userId, jwtToken);
        VPNJni.setCallback(jniCallback);
        //
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        try {
            VPNJni.onAccelInfoUpload(cid, null, userId, jwtToken);
            assertNull(called.get());
            VPNJni.onAccelInfoUpload(cid, content, null, jwtToken);
            assertNull(called.get());
        } finally {
            Logger.setLoggableChecker(null);
        }
        //
        VPNJni.onAccelInfoUpload(cid, content, userId, jwtToken);
        assertTrue(called.get());
    }

    @Test
    public void onCacheData() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String name = "name";
        final String value = "value";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(name, invocation.getArgument(0));
                assertEquals(value, invocation.getArgument(1));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestSaveData(name, value);
        VPNJni.setCallback(jniCallback);
        VPNJni.onCacheData(cid, name, value);
        assertTrue(called.get());
    }

    @Test
    public void onLoadData() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String name = "name";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(name, invocation.getArgument(1));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestLoadData(cid, name);
        VPNJni.setCallback(jniCallback);
        VPNJni.onLoadData(cid, name);
        assertTrue(called.get());
    }

    @Test
    public void requestBeaconCounter() {
        final Ref<Boolean> called = new Ref<Boolean>();
        final int cid = 123;
        final String counterName = "name";
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(cid, invocation.getArgument(0));
                assertEquals(counterName, invocation.getArgument(1));
                called.set(true);
                return null;
            }
        }).when(jniCallback).requestBeaconCounter(cid, counterName);
        VPNJni.setCallback(jniCallback);
        VPNJni.requestBeaconCounter(cid, counterName);
        assertTrue(called.get());
    }

    @Test
    public void protectFD() {
        JniCallback jniCallback = mock(JniCallback.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                assertEquals(123, invocation.getArgument(0));
                return null;
            }
        }).when(jniCallback).protectFD(anyInt());
        VPNJni.protectFD(123);
    }

    @Test
    @Config(shadows = ShadowVPNJni.class)
    public void testStartVPN() throws InterruptedException {
        MockObserver observer = new MockObserver();
        VPNJni.registerVpnEventObserver(observer);
        try {
            assertNull(observer.active);
            VPNJni.doStartVPN(123);
            assertTrue(observer.active);
            VPNJni.doStopVPN();
            assertFalse(observer.active);
            //
            Thread t = new Thread() {
                @Override
                public void run() {
                    VPNJni.doStopVPN();
                }
            };
            t.start();
            t.join();
        } finally {
            VPNJni.unregisterVpnEventObserver(observer);
        }
    }

    @Test
    public void testObserverNotifier() {
        VPNJni.ObserverNotifier.notify(true);
        VpnEventObserver[] observers = new VpnEventObserver[] {
            new MockObserver(), new MockObserver()
        };
        VPNJni.ObserverNotifier notifier = new VPNJni.ObserverNotifier(observers, true);
        notifier.run();
        for (VpnEventObserver o : observers) {
            MockObserver mockObserver = (MockObserver) o;
            assertTrue(mockObserver.active);
        }
    }


    private static class MockObserver implements VpnEventObserver {

        Boolean active;

        @Override
        public void onVPNStateChanged(boolean active) {
            this.active = active;
        }
    }
}