package com.subao.gamemaster;

import android.content.Context;
import android.os.ConditionVariable;
import android.util.JsonWriter;
import android.util.Pair;

import com.subao.common.ErrorCode;
import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.accel.EngineWrapper;
import com.subao.common.accel.EngineWrapperTest;
import com.subao.common.collection.Ref;
import com.subao.common.data.Defines;
import com.subao.common.data.SupportGame;
import com.subao.common.data.SupportGameList;
import com.subao.common.data.SupportGameListTest;
import com.subao.common.jni.InitJNIMode;
import com.subao.common.jni.ShadowVPNJni;
import com.subao.common.msg.MessageUserId;
import com.subao.common.msg.Message_DeviceInfo;
import com.subao.common.parallel.NetworkWatcher;
import com.subao.vpn.VpnEventObserver;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * GameMasterTest
 * <p>Created by YinHaiBo on 2017/1/29.</p>
 */
@SuppressWarnings("deprecation")
@Config(shadows = ShadowVPNJni.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GameMasterTest extends RoboBase {

    private EngineWrapperTest.MockNetManager mockNetManager;
    private EngineWrapperTest.MockJniWrapper mockJniWrapper;
    private EngineWrapper engineWrapper;

    private static EngineWrapper initGameMasterWithMockEngineWrapper() {
        return GameMaster.engineWrapper = mock(EngineWrapper.class);
    }

    private void initGameMaster() {
        mockNetManager = new EngineWrapperTest.MockNetManager(getContext());
        mockJniWrapper = new EngineWrapperTest.MockJniWrapper();
        engineWrapper = new EngineWrapper(getContext(), Defines.ModuleType.SDK,
            "The Game Guid", "2.0.0", mockNetManager, mockJniWrapper, true);
        int r = GameMaster.init(getContext(), "The Game Guid", InitJNIMode.UDP, "", 222,
            null,
            engineWrapper,
            new GameMaster.RequiredPermissionChecker() {
                @Override
                public boolean hasRequiredPermission(Context context) {
                    return true;
                }
            });
        assertEquals(0, r);
        assertEquals(engineWrapper, GameMaster.engineWrapper);
    }

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        GameMaster.engineWrapper = null;
    }

    @After
    public void tearDown() {
        EngineWrapper engineWrapper = GameMaster.engineWrapper;
        GameMaster.engineWrapper = null;
        if (engineWrapper != null) {
            engineWrapper.dispose();
        }
        Logger.setLoggableChecker(null);
    }

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RoboBase.testPrivateConstructor(GameMaster.class);
    }

    @Test
    public void transHookTypeToInitJNIMode() {
        assertEquals(InitJNIMode.TCP, GameMaster.transHookTypeToInitJNIMode(GameMaster.HOOK_TYPE_CONNECT));
        assertEquals(InitJNIMode.UDP, GameMaster.transHookTypeToInitJNIMode(GameMaster.HOOK_TYPE_SENDMSG_RECVMSG));
        assertEquals(InitJNIMode.UDP, GameMaster.transHookTypeToInitJNIMode(GameMaster.HOOK_TYPE_SENDTO_RECVFROM));
        assertNull(GameMaster.transHookTypeToInitJNIMode(3));
    }

    @Test
    public void testConstDefines() {
        assertEquals(-3, GameMaster.GM_INIT_NOT_IN_MAIN_THREAD);
        assertEquals(-2, GameMaster.GM_INIT_NO_PERMISSION);
        assertEquals(-1, GameMaster.GM_INIT_FAILURE);
        assertEquals(0, GameMaster.GM_INIT_SUCCESS);
        assertEquals(1, GameMaster.GM_INIT_ALREADY);

        // 网络连接类型
        assertEquals(1, GameMaster.NETWORK_CLASS_WIFI);
        assertEquals(2, GameMaster.NETWORK_CLASS_2G);
        assertEquals(3, GameMaster.NETWORK_CLASS_3G);
        assertEquals(4, GameMaster.NETWORK_CLASS_4G);
        assertEquals(0, GameMaster.NETWORK_CLASS_UNKNOWN);
        assertEquals(-1, GameMaster.NETWORK_CLASS_DISCONNECT);

        assertEquals(0, GameMaster.HOOK_TYPE_CONNECT);
        assertEquals(1, GameMaster.HOOK_TYPE_SENDTO_RECVFROM);
        assertEquals(2, GameMaster.HOOK_TYPE_SENDMSG_RECVMSG);

        assertEquals(0, GameMaster.SDK_NOT_QUALIFIED);
        assertEquals(1, GameMaster.SDK_QUALIFIED);
        assertEquals(2, GameMaster.SDK_FREE_TRIAL);
        assertEquals(3, GameMaster.SDK_TRIAL_EXPIRED);
        assertEquals(4, GameMaster.SDK_IN_USE);
        assertEquals(5, GameMaster.SDK_EXPIRED);
        assertEquals(6, GameMaster.SDK_FREE);

        assertEquals(-1, GameMaster.ACCEL_RECOMMENDATION_UNKNOWN);
        assertEquals(0, GameMaster.ACCEL_RECOMMENDATION_NONE);
        assertEquals(1, GameMaster.ACCEL_RECOMMENDATION_NOTICE);
        assertEquals(2, GameMaster.ACCEL_RECOMMENDATION_WIFI);
        assertEquals(3, GameMaster.ACCEL_RECOMMENDATION_HAS_NEW_FEATURE);
        assertEquals(4, GameMaster.ACCEL_RECOMMENDATION_PROMPT_MONTH_REPORT);
        assertEquals(5, GameMaster.ACCEL_RECOMMENDATION_VIP_EXPIRED);

        assertEquals(0, GameMaster.PAY_TYPE_START);
        assertEquals(0, GameMaster.PAY_TYPE_ALIPAY);
        assertEquals(1, GameMaster.PAY_TYPE_WECHAT);
        assertEquals(2, GameMaster.PAY_TYPE_QQ);
        assertEquals(3, GameMaster.PAY_TYPE_UNIONPAY);
        assertEquals(4, GameMaster.PAY_TYPE_PHONE);
        assertEquals(5, GameMaster.PAY_TYPE_OTHER);
        assertEquals(6, GameMaster.PAY_TYPE_END);
    }

    @Test
    public void initWithVPN() {
        GameMaster.initWithVPN(getContext(), "The Game Guid", null);
    }

    @Test
    public void initAlready() {
        initGameMaster();
        int r = GameMaster.init(
            getContext(), "The Game Guid", InitJNIMode.UDP,
            null, 222,
            null,
            engineWrapper,
            new GameMaster.RequiredPermissionChecker() {
                @Override
                public boolean hasRequiredPermission(Context context) {
                    return true;
                }
            });
        assertEquals(GameMaster.GM_INIT_ALREADY, r);
    }

    @Test
    public void initFail() {
        assertEquals(GameMaster.GM_INIT_ILLEGAL_ARGUMENT, GameMaster.init(
            getContext(), 80, "GameGuid", "Channel", null, 222
        ));
        // 无效的gameGuid
        assertEquals(GameMaster.GM_INIT_ILLEGAL_ARGUMENT, GameMaster.init(getContext(), 0, null, null, null, 0));
        assertEquals(GameMaster.GM_INIT_NO_PERMISSION, GameMaster.init(
            getContext(), "GameGuid", InitJNIMode.UDP, "", 222,
            null,
            mock(EngineWrapper.class),
            new GameMaster.RequiredPermissionChecker() {
                @Override
                public boolean hasRequiredPermission(Context context) {
                    return false;
                }
            }));
        assertNull(GameMaster.engineWrapper);
        // 非主线程
        final Ref<Integer> result = new Ref<Integer>();
        Thread thread = new Thread() {
            @Override
            public void run() {
                result.set(GameMaster.init(getContext(), 0, "GUID", "", null, 222));
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
        }
        assertEquals(GameMaster.GM_INIT_NOT_IN_MAIN_THREAD, (int) result.get());
    }

    @Test
    public void testDefaultEngineWrapper() {
        int r = GameMaster.init(getContext(), "GUID", InitJNIMode.UDP, null, 222,
            null,
            null,
            new GameMaster.RequiredPermissionChecker() {
                @Override
                public boolean hasRequiredPermission(Context context) {
                    return true;
                }
            });
        assertEquals(GameMaster.GM_INIT_SUCCESS, r);
    }

    @Test
    public void testEngineWrapperDisposeWhenInitFail() {
        EngineWrapper engineWrapper = mock(EngineWrapper.class);
        doReturn(GameMaster.GM_INIT_FAILURE).when(engineWrapper).init(
            any(InitJNIMode.class), anyString(), anyInt(), any(byte[].class)
        );
        final Ref<Boolean> disposeMethodCalled = new Ref<Boolean>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                disposeMethodCalled.set(true);
                return null;
            }
        }).when(engineWrapper).dispose();
        int r = GameMaster.init(getContext(), "GUID", InitJNIMode.UDP, "", 222,
            new byte[]{},
            engineWrapper,
            new GameMaster.RequiredPermissionChecker() {
                @Override
                public boolean hasRequiredPermission(Context context) {
                    return true;
                }
            });
        assertTrue(disposeMethodCalled.get());
        assertEquals(GameMaster.GM_INIT_FAILURE, r);
    }

    @Test
    public void start() {
        assertFalse(GameMaster.start(0));
        assertFalse(GameMaster.isAccelOpened());
        initGameMaster();
        assertFalse(GameMaster.isAccelOpened());
        assertTrue(GameMaster.start(0));
        assertTrue(GameMaster.isAccelOpened());
    }

    @Test
    public void stop() {
        GameMaster.stop();
        initGameMaster();
        GameMaster.stop();
    }

    @Test
    public void prepareVPN() {
        GameMaster.prepareVPN(getContext());
    }

    @Test
    public void openVPN() {
        assertFalse(GameMaster.openVPN());
        initGameMaster();
        GameMaster.openVPN();
    }

    @Test
    public void closeVPN() {
        GameMaster.closeVPN();
        initGameMaster();
        GameMaster.closeVPN();
    }

    @Test
    public void isUDPProxy() {
        GameMaster.isUDPProxy();
        initGameMaster();
        GameMaster.isUDPProxy();
    }

    @Test
    public void isEngineRunning() {
        assertFalse(GameMaster.isEngineRunning());
        initGameMaster();
        assertTrue(GameMaster.isEngineRunning());
    }

    @Test
    public void setOnAccelSwitchListener() {
        assertFalse(GameMaster.x7(null));
        EngineWrapper engineWrapper = initGameMasterWithMockEngineWrapper();
        final Ref<Boolean> setValue = new Ref<Boolean>();
        GameMaster.OnAccelSwitchListener gameMasterListener = new GameMaster.OnAccelSwitchListener() {
            @Override
            public void onAccelSwitch(boolean accelOn) {
                setValue.set(accelOn);
            }
        };
        Answer<Void> answer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                EngineWrapper.OnAccelSwitchListener engineWrapperListener = invocation.getArgument(0);
                engineWrapperListener.onAccelSwitch(true);
                return null;
            }
        };
        doAnswer(answer).when(engineWrapper).setOnAccelSwitchListener(any(EngineWrapper.OnAccelSwitchListener.class));
        assertTrue(GameMaster.x7(gameMasterListener));
        assertTrue(setValue.get());
    }

    @Test
    public void setUdpEchoPort() {
        int port = 1234;
        GameMaster.setUdpEchoPort(port);
        EngineWrapper engineWrapper = initGameMasterWithMockEngineWrapper();
        final Ref<Integer> ref = new Ref<Integer>();
        Answer<Void> answer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ref.set((Integer) invocation.getArgument(0));
                return null;
            }
        };
        doAnswer(answer).when(engineWrapper).setUdpEchoPort(anyInt());
        GameMaster.setUdpEchoPort(port);
        assertEquals(ref.get().intValue(), port);
    }

    @Test
    public void getCurrentConnectionType() {
        assertEquals(GameMaster.NETWORK_CLASS_UNKNOWN, GameMaster.getCurrentConnectionType());
        initGameMaster();
        GameMaster.getCurrentConnectionType();
    }

    @Test
    public void setUserToken() {
        String userId = "User ID";
        String token = "User Token";
        String appId = "App Id";
        GameMaster.setUserToken(userId, token, appId);
        initGameMaster();
        GameMaster.setUserToken(userId, token, appId);
        assertEquals(userId, mockJniWrapper.arguments.get(0));
        assertEquals(token, mockJniWrapper.arguments.get(1));
        assertEquals(appId, mockJniWrapper.arguments.get(2));
    }

    @Test
    public void setFreeFlowUser() {
        GameMaster.setFreeFlowUser(1);
        assertEquals(EngineWrapper.INVALID_FREE_FLOW_TYPE_VALUE, GameMaster.getCurrentUserFreeFlowType());
        initGameMaster();
        assertEquals(EngineWrapper.INVALID_FREE_FLOW_TYPE_VALUE, GameMaster.getCurrentUserFreeFlowType());
        for (int type = -1; type <= 3; ++type) {
            GameMaster.setFreeFlowUser(type);
            assertEquals(type, GameMaster.getCurrentUserFreeFlowType());
            boolean isFreeFlowUser = (type >= 0 && type <= 2);
            assertEquals(isFreeFlowUser, GameMaster.engineWrapper.isFreeFlowUser());
            assertEquals(Defines.VPNJniStrKey.KEY_FREE_FLOW_TYPE, mockJniWrapper.setIntKey);
            assertEquals(type, mockJniWrapper.setIntValue);
        }
    }

    @Test
    public void gameForeground() {
        GameMaster.gameForeground();
        initGameMaster();
        GameMaster.gameForeground();
    }

    @Test
    public void gameBackground() {
        GameMaster.gameBackground();
        initGameMaster();
        GameMaster.gameBackground();
    }

    @Test
    public void setGameServerIP() {
        GameMaster.setGameServerIP("127.0.0.1");
        initGameMaster();
        GameMaster.setGameServerIP("127.0.0.1");
    }

    @Test
    public void deprecatedFunctions() {
        assertTrue(GameMaster.isNodeDetectSucceed());
        assertEquals("", GameMaster.getString(0));
        GameMaster.setString(0, "hello");
        assertEquals(0L, GameMaster.getLong(0));
        GameMaster.setLong(0, 1);
        GameMaster.setSDKMode(0);
        GameMaster.onNetDelayQuality(1.2f, 3.4f, 5.6f, 10);
    }

    @Test
    public void clearUDPCache() {
        GameMaster.clearUDPCache();
    }

    @Test
    public void getAccelRecommendation() {
        // 未初始化SDK，一律返回Unknown
        assertEquals(GameMaster.ACCEL_RECOMMENDATION_UNKNOWN, GameMaster.getAccelRecommendation());
        initGameMaster();
        int value = GameMaster.getAccelRecommendation();
        assertTrue(value >= 0 && value <= 5);

//        for (int i = 0; i <= 2; ++i) {
//            mockJniWrapper.accelRecommendation = i;
//            assertEquals(i, GameMaster.getAccelRecommendation());
//        }
//        // 如果JNI推荐是NONE，总是返回NONE
//        mockJniWrapper.accelRecommendation = GameMaster.ACCEL_RECOMMENDATION_NONE;
//        assertEquals(GameMaster.ACCEL_RECOMMENDATION_NONE, GameMaster.getAccelRecommendation());
//        // 当加速已开，但JNI推荐结果是NOTICE，则修正为NONE
//        mockJniWrapper.accelRecommendation = GameMaster.ACCEL_RECOMMENDATION_NOTICE;
//        assertEquals(GameMaster.ACCEL_RECOMMENDATION_NONE, GameMaster.getAccelRecommendation());
//        // 2G、未知网络、断网，这几种情况都不推荐开
//        NetTypeDetector.NetType[] unsupportedNetTypes = new NetTypeDetector.NetType[]{
//            NetTypeDetector.NetType.MOBILE_2G, NetTypeDetector.NetType.UNKNOWN, NetTypeDetector.NetType.DISCONNECT
//        };
//        for (NetTypeDetector.NetType netType : unsupportedNetTypes) {
//            mockNetManager.currentNetType = netType;
//            mockJniWrapper.accelRecommendation = GameMaster.ACCEL_RECOMMENDATION_NOTICE;
//            assertEquals(GameMaster.ACCEL_RECOMMENDATION_NONE, GameMaster.getAccelRecommendation());
//            mockJniWrapper.accelRecommendation = GameMaster.ACCEL_RECOMMENDATION_WIFI;
//            assertEquals(GameMaster.ACCEL_RECOMMENDATION_NONE, GameMaster.getAccelRecommendation());
//        }
//        // 免流用户
//        for (int freeFlowUserType = 0; freeFlowUserType <= 2; ++freeFlowUserType) {
//            GameMaster.setFreeFlowUser(freeFlowUserType);
//            mockJniWrapper.accelRecommendation = GameMaster.ACCEL_RECOMMENDATION_WIFI;
//            mockNetManager.currentNetType = NetTypeDetector.NetType.WIFI;
//            assertEquals(GameMaster.ACCEL_RECOMMENDATION_NONE, GameMaster.getAccelRecommendation());
//            for (int i = -1; i <= 2; ++i) {
//                mockJniWrapper.accelRecommendation = i;
//                mockNetManager.currentNetType = NetTypeDetector.NetType.MOBILE_4G;
//                assertEquals(GameMaster.ACCEL_RECOMMENDATION_NONE, GameMaster.getAccelRecommendation());
//            }
//            // 如果是WiFi环境，免流用户也可以推荐开
//            mockNetManager.currentNetType = NetTypeDetector.NetType.WIFI;
//            mockJniWrapper.accelRecommendation = GameMaster.ACCEL_RECOMMENDATION_NOTICE;
//            assertEquals(GameMaster.ACCEL_RECOMMENDATION_NOTICE, GameMaster.getAccelRecommendation());
//        }
//        //
//        // 非免流用户
//        GameMaster.setFreeFlowUser(-1);
//        NetTypeDetector.NetType[] supportedNetTypes = new NetTypeDetector.NetType[]{
//            NetTypeDetector.NetType.MOBILE_3G, NetTypeDetector.NetType.MOBILE_4G, NetTypeDetector.NetType.WIFI,
//        };
//        int[] resultFromJNIList = new int[]{
//            GameMaster.ACCEL_RECOMMENDATION_NONE, GameMaster.ACCEL_RECOMMENDATION_NOTICE,
//            GameMaster.ACCEL_RECOMMENDATION_WIFI
//        };
//        for (int resultFromJNI : resultFromJNIList) {
//            mockJniWrapper.accelRecommendation = resultFromJNI;
//            for (NetTypeDetector.NetType netType : supportedNetTypes) {
//                mockNetManager.currentNetType = netType;
//                assertEquals(resultFromJNI, GameMaster.getAccelRecommendation());
//            }
//        }
    }

    @Test
    public void onNetDelay() {
        GameMaster.onNetDelay(1234);
        initGameMaster();
        GameMaster.onNetDelay(1234);
        GameMaster.start(0);
        GameMaster.onNetDelay(1234);
        assertEquals(1234, mockJniWrapper.arguments.get(0));
    }

    @Test
    public void onNetDelayQuality2() {
        GameMaster.onNetDelayQuality2(1f, 2f, 0.3f, 0.4f, 0.5f);
        initGameMaster();
        GameMaster.onNetDelayQuality2(1f, 2f, 0.3f, 0.4f, 0.5f);
        GameMaster.start(0);
        GameMaster.onNetDelayQuality2(1f, 2f, 0.3f, 0.4f, 0.5f);
    }

    @Test
    public void setGameId() {
        GameMaster.setGameId(123);
        initGameMaster();
        int id = 456;
        GameMaster.setGameId(id);
        assertEquals(Defines.VPNJniStrKey.KEY_GAME_SERVER_ID, mockJniWrapper.setStringKey);
        assertEquals(Integer.toString(id), mockJniWrapper.setStringValue);
    }

    @Test
    public void setRecommendationGameIP() {
        String ip = "1.2.3.4";
        int port = 5678;
        GameMaster.setRecommendationGameIP(ip, port);
        initGameMaster();
        GameMaster.setRecommendationGameIP(ip, port);
        assertEquals(ip, mockJniWrapper.recommendationGameIP);
        assertEquals(port, mockJniWrapper.recommendationGamePort);
    }

    @Test
    public void getVIPValidTime() {
        assertEquals("", GameMaster.getVIPValidTime());
        initGameMaster();
        String s = "getVIPValidTime";
        assertEquals(s, GameMaster.getVIPValidTime());
        assertEquals(s, mockJniWrapper.lastMethod);
    }

    @Test
    public void getAccelerationStatus() {
        assertEquals(GameMaster.SDK_NOT_QUALIFIED, GameMaster.getAccelerationStatus());
        initGameMaster();
        assertEquals(MessageUserId.getCurrentUserStatus(), GameMaster.getAccelerationStatus());
    }

    @Test
    public void getUserConfig() {
        assertEquals(MessageUserId.getCurrentUserConfig(), GameMaster.getUserConfig());
    }

    @Test
    public void getWebUIUrl() {
        String defaultResult = EngineWrapper.buildDefaultWebUIUrl("", "");
        assertEquals("http://service.xunyou.mobi/?appid=&userid=", defaultResult);
        assertEquals(defaultResult, GameMaster.getWebUIUrl());
        initGameMaster();
        assertEquals(defaultResult, GameMaster.getWebUIUrl());
        mockJniWrapper.webUIUrl = "test";
        assertTrue(GameMaster.getWebUIUrl().startsWith("test"));
    }

    @Test
    public void getAccelRecommendationData() {
        for (int i = 0; i <= 1; ++i) {
            if (i > 0) {
                initGameMaster();
            }
            for (int type = -1; type <= 6; ++type) {
                assertEquals("", GameMaster.getAccelRecommendationData(type));
            }
        }
    }

    @Test
    public void onAccelRecommendationResult() {
        GameMaster.onAccelRecommendationResult(0, false);
        GameMaster.onAccelRecommendationResult(4, true);
        initGameMaster();
        GameMaster.onAccelRecommendationResult(0, false);
        GameMaster.onAccelRecommendationResult(4, true);
    }

    @Test
    public void uploadUserConfig() {
        GameMaster.x2("user", "cfg");
        initGameMaster();
        GameMaster.x2("user", "cfg");
    }

    @Test
    public void setPayTypeWhiteList() {
        GameMaster.setPayTypeWhiteList(null);
        GameMaster.setPayTypeWhiteList(1);
        initGameMaster();
        GameMaster.setPayTypeWhiteList(null);
        assertEquals(Defines.VPNJniStrKey.KEY_PAY_TYPE_WHITE_LIST, mockJniWrapper.setStringKey);
        assertNull(mockJniWrapper.setStringValue);
        //
        GameMaster.setPayTypeWhiteList("1234");
        assertEquals(Defines.VPNJniStrKey.KEY_PAY_TYPE_WHITE_LIST, mockJniWrapper.setStringKey);
        assertEquals("1234", mockJniWrapper.setStringValue);
        //
        for (int bits = 0; bits < (1 << 12); ++bits) {
            mockJniWrapper.setStringKey = null;
            mockJniWrapper.setStringValue = null;
            GameMaster.setPayTypeWhiteList(bits);
            assertEquals(Defines.VPNJniStrKey.KEY_PAY_TYPE_WHITE_LIST, mockJniWrapper.setStringKey);
            String actualValue = mockJniWrapper.setStringValue;
            if (bits == 0) {
                assertNull(actualValue);
                continue;
            }
            assertTrue(actualValue.length() >= GameMaster.PAY_TYPE_START
                && actualValue.length() <= (GameMaster.PAY_TYPE_END - GameMaster.PAY_TYPE_START));
            for (int n = actualValue.length() - 1; n >= 0; --n) {
                int ch = actualValue.charAt(n) - '0';
                assertTrue(ch >= GameMaster.PAY_TYPE_START && ch < GameMaster.PAY_TYPE_END);
            }
            int pos = 0;
            int typeValue = GameMaster.PAY_TYPE_ALIPAY;
            int mask = 1 << typeValue;
            if ((bits & mask) != 0) {
                assertEquals(actualValue.charAt(pos), Integer.toString(typeValue).charAt(0));
                ++pos;
            }
            typeValue = GameMaster.PAY_TYPE_WECHAT;
            mask = 1 << typeValue;
            if ((bits & mask) != 0) {
                assertEquals(actualValue.charAt(pos), Integer.toString(typeValue).charAt(0));
                ++pos;
            }
            typeValue = GameMaster.PAY_TYPE_QQ;
            mask = 1 << typeValue;
            if ((bits & mask) != 0) {
                assertEquals(actualValue.charAt(pos), Integer.toString(typeValue).charAt(0));
                ++pos;
            }
            typeValue = GameMaster.PAY_TYPE_UNIONPAY;
            mask = 1 << typeValue;
            if ((bits & mask) != 0) {
                assertEquals(actualValue.charAt(pos), Integer.toString(typeValue).charAt(0));
                ++pos;
            }
            typeValue = GameMaster.PAY_TYPE_PHONE;
            mask = 1 << typeValue;
            if ((bits & mask) != 0) {
                assertEquals(actualValue.charAt(pos), Integer.toString(typeValue).charAt(0));
                ++pos;
            }
            typeValue = GameMaster.PAY_TYPE_OTHER;
            mask = 1 << typeValue;
            if ((bits & mask) != 0) {
                assertEquals(actualValue.charAt(pos), Integer.toString(typeValue).charAt(0));
                ++pos;
            }
        }
    }

    @Test
    public void setWiFiAccelSwitch() {
        GameMaster.setWiFiAccelSwitch(true);
        initGameMaster();
        GameMaster.setWiFiAccelSwitch(false);
        MessageUserId.resetUserInfo("test");
        GameMaster.setWiFiAccelSwitch(true);
    }

    @Test
    public void enableWiFiAccelSwitch() {
        GameMaster.enableWiFiAccelSwitch();
        initGameMaster();
        GameMaster.enableWiFiAccelSwitch();
    }

    @Test
    public void startNodeDetect() {
        int uid = 10012;
        GameMaster.startNodeDetect(uid);
        initGameMaster();
        GameMaster.startNodeDetect(uid);
    }

    @Test
    public void isNodeDetected() {
        int uid = 10013;
        assertFalse(GameMaster.isNodeDetected(uid));
        initGameMaster();
        GameMaster.isNodeDetected(uid);
    }

    @Test
    public void getSupportGameList() {
        List<String> list = GameMaster.getSupportGameList();
        assertEquals(0, list.size());
        //
        EngineWrapper engineWrapper = mock(EngineWrapper.class);
        doReturn(0).when(engineWrapper).init(
            any(InitJNIMode.class), anyString(), anyInt(), any(byte[].class)
        );
        SupportGameList supportGameList = SupportGameListTest.buildNotEmptySupportGameList();
        doReturn(supportGameList).when(engineWrapper).getSupportGameList();
        GameMaster.init(getContext(), "guid", InitJNIMode.VPN, "", 222,
            new byte[] {}, engineWrapper, null);
        List<String> packageNames = GameMaster.getSupportGameList();
        assertEquals(packageNames.size(), supportGameList.getCount());
        int i = 0;
        for (SupportGame supportGame : supportGameList) {
            assertEquals(supportGame.packageName, packageNames.get(i));
            ++i;
        }
    }

    @Test
    public void launcherGame() {
        assertFalse(GameMaster.launcherGame(getContext(), null));
    }

    @Test
    public void debugFunction1() {
        Pair<Integer, Integer> pair = GameMaster.x1();
        int fd = pair.first;
        int error = pair.second;
        assertEquals(ErrorCode.NOT_INIT, error);
        assertTrue(fd < 0);
        initGameMaster();
        pair = GameMaster.x1();
        fd = pair.first;
        error = pair.second;
        if (error == 0) {
            assertTrue(fd > 0);
        } else {
            assertTrue(fd < 0);
            assertTrue(error >= 2000 && error <= 2006);
        }
    }

    @Test
    public void debugFunction1Exception() throws NetworkWatcher.OperationException {
        EngineWrapper engineWrapper = initGameMasterWithMockEngineWrapper();
        doReturn(10).when(engineWrapper).requestNewMobileFD();
        Pair<Integer, Integer> pair = GameMaster.x1();
        assertEquals(10, (int) pair.first);
        assertEquals(0, (int) pair.second);
    }

    @Test
    public void debugFunction3() {
        assertNotNull(GameMaster.x3(getContext()));
    }

    @Test
    @Config(shadows = ShadowMessageDeviceInfo.class)
    public void debugFunction3Exception() {
        assertNull(GameMaster.x3(getContext()));
    }

    @Test
    public void debugFunction4() {
        final Ref<Boolean> called = new Ref<Boolean>();
        EngineWrapper engineWrapper = initGameMasterWithMockEngineWrapper();
        doAnswer(
            new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    called.set(true);
                    return null;
                }
            }
        ).when(engineWrapper).openProxyLog();
        GameMaster.x4();
        assertTrue(called.get());
    }

    @Test
    public void debugFunction5() {
        assertNotNull(GameMaster.x5(null, true));
    }

    @Test
    public void debugFunction6() {
        assertNotNull(GameMaster.x6(true));
    }

    @Test
    public void aTestX8() {
        File file = GameMaster.x8(getContext());
        assertTrue(file.isDirectory() && file.exists());
    }

    @Test
    public void x9() {
        final Ref<Boolean> succeed = new Ref<Boolean>();
        GameMaster.x9("test", new GameMaster.I1() {
            @Override
            public void a(boolean s) {
                succeed.set(s);
            }
        });
        assertFalse(succeed.get());
        //
        final ConditionVariable cv = new ConditionVariable();
        succeed.set(null);
        assertNull(succeed.get());
        initGameMaster();
        GameMaster.x9("test", new GameMaster.I1() {
            @Override
            public void a(boolean s) {
                succeed.set(s);
                cv.open();
            }
        });
        cv.block();
        assertNotNull(succeed.get());
    }

    @Test
    public void x10() {
        final Ref<Integer> ref = new Ref<Integer>();
        GameMaster.I2 i2 = new GameMaster.I2() {
            @Override
            public void a(int p) {
                ref.set(p);
            }
        };
        Object watcher = GameMaster.x10(getContext(), i2);
        GameMaster.x11(watcher);
        //
        GameMaster.SignalWatcherListener l = new GameMaster.SignalWatcherListener(i2);
        l.onSignalChange(50);
        assertEquals(50, (int) ref.get());
    }

    @Test
    public void x12() throws InterruptedException {
        ThreadIPQuery t = new ThreadIPQuery("61.139.2.69");
        t.start();
        t.join();
        assertNotNull(t.getResult());
    }

    @Test
    public void registerVpnEventObserver() {
        VpnEventObserver o = mock(VpnEventObserver.class);
        GameMaster.x13(o);
        GameMaster.x14(o);
    }

    @Test
    public void xy() {
        GameMaster.xy(getContext());
    }

    private static class ThreadIPQuery extends Thread {

        private final String ip;
        private String result;

        public ThreadIPQuery(String ip) {
            this.ip = ip;
        }

        public String getResult() {
            return this.result;
        }

        @Override
        public void run() {
            this.result = GameMaster.x12(ip);
        }
    }

    @Implements(value = EngineWrapper.class, isInAndroidSdk = false, callThroughByDefault = false)
    public static class ShadowEngineWrapper {

    }

    @Implements(value = Message_DeviceInfo.class, isInAndroidSdk = false)
    public static class ShadowMessageDeviceInfo {
        @Implementation
        public void serialize(JsonWriter writer) throws IOException {
            throw new IOException();
        }
    }


}