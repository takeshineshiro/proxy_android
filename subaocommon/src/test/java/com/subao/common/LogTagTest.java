package com.subao.common;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;

public class LogTagTest {

    @Test
    public void testConstructor() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        RoboBase.testPrivateConstructor(LogTag.class);
    }

    @Test
    public void testConst() {
        assertEquals("SubaoQos", LogTag.QOS);
        assertEquals("SubaoNet", LogTag.NET);
        assertEquals("SubaoData", LogTag.DATA);
        assertEquals("SubaoMessage", LogTag.MESSAGE);
        assertEquals("SubaoParallel", LogTag.PARALLEL);
        assertEquals("SubaoGame", LogTag.GAME);
        assertEquals("SubaoUser", LogTag.USER);
        assertEquals("SubaoAuth", LogTag.AUTH);
        assertEquals("SubaoProxy", LogTag.PROXY);
        assertEquals("SubaoPay", LogTag.PAY);
    }

}
