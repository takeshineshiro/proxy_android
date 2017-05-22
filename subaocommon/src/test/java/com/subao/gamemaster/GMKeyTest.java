package com.subao.gamemaster;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;

/**
 * GMKeyTest
 * <p>Created by YinHaiBo on 2017/3/4.</p>
 */
@SuppressWarnings("deprecation")
public class GMKeyTest {

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RoboBase.testPrivateConstructor(GMKey.class);
    }

    @Test
    public void values() {
        assertEquals(100, GMKey.SGM_GET_NETDELAY);
        assertEquals(101, GMKey.SGM_GET_SERVER_NODE);
        assertEquals(102, GMKey.SGM_GET_ACCEL_STAT);
        assertEquals(103, GMKey.SGM_GET_GAME_FLOW);
        assertEquals(104, GMKey.SGM_GET_AUTO_START);
        assertEquals(105, GMKey.SGM_GET_BIG_SHOWN);
        assertEquals(106, GMKey.SGM_GET_REPAIR_SUCCESS_COUNT);
        assertEquals(107, GMKey.SGM_GET_ACCEL_EFFECT);
        assertEquals(108, GMKey.SGM_GET_VERSION_CODE);
        assertEquals(109, GMKey.SGM_GET_VERSION);
        assertEquals(110, GMKey.SGM_GET_ENGINE_STATE);
        assertEquals(300, GMKey.SGM_SET_GAME_PORT);
        assertEquals(301, GMKey.SGM_SET_AUTO_START);
        assertEquals(302, GMKey.SGM_SET_LOG_LEVEL);
        assertEquals(303, GMKey.SGM_SET_TEST_MODE);
        assertEquals(304, GMKey.SGM_SET_TEST_SERVER_NODE);
        assertEquals(305, GMKey.SGM_SET_GAME_FOREGROUND);
        assertEquals(306, GMKey.SGM_SET_GAME_BACKGROUND);
        assertEquals(307, GMKey.SGM_SET_GAME_SERVER_IP);
        assertEquals(308, GMKey.SGM_SET_CONNECT_TIMEOUT);

    }
}