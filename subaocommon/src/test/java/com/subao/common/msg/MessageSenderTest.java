package com.subao.common.msg;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * MessageSenderTest
 * <p>Created by YinHaiBo on 2016/12/30.</p>
 */
public class MessageSenderTest {

    @Test
    public void testFreeFlowType() {
        assertEquals(4, MessageSender.FreeFlowType.values().length);
        assertEquals(0, MessageSender.FreeFlowType.UNKNOWN.intValue);
        assertEquals(1, MessageSender.FreeFlowType.CMCC.intValue);
        assertEquals(2, MessageSender.FreeFlowType.CUCC.intValue);
        assertEquals(3, MessageSender.FreeFlowType.CTCC.intValue);
    }
}