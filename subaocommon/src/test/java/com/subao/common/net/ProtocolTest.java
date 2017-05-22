package com.subao.common.net;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * ProtocolTest
 * <p>Created by YinHaiBo on 2017/3/24.</p>
 */
public class ProtocolTest {

    @Test
    public void test() {
        assertEquals(3, Protocol.values().length);
        assertEquals("TCP", Protocol.TCP.upperText);
        assertEquals("tcp", Protocol.TCP.lowerText);
        assertEquals("UDP", Protocol.UDP.upperText);
        assertEquals("udp", Protocol.UDP.lowerText);
        assertEquals("UDP", Protocol.BOTH.upperText); // FIXME：应该是BOTH
        assertEquals("udp", Protocol.BOTH.lowerText); // FIXME: 应该是both
    }

}