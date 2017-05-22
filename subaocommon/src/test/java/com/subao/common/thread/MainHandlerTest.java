package com.subao.common.thread;

import com.subao.common.RoboBase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * MainHandlerTest
 * <p>Created by YinHaiBo on 2017/2/28.</p>
 */
public class MainHandlerTest extends RoboBase {

    @Test
    public void test() {
        assertNotNull(MainHandler.getInstance());
    }

}