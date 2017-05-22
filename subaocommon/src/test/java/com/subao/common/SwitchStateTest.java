package com.subao.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * SwitchStateTest
 * <p>Created by YinHaiBo on 2017/3/7.</p>
 */
public class SwitchStateTest {

    @Test
    public void test() {
        assertEquals(3, SwitchState.values().length);
        assertEquals(-1, SwitchState.UNKNOWN.getId());
        assertEquals(0, SwitchState.OFF.getId());
        assertEquals(1, SwitchState.ON.getId());
    }

}