package com.subao.common.parallel;

import com.subao.common.ErrorCode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * NetworkWatcherInitTest
 * <p>Created by YinHaiBo on 2017/3/14.</p>
 */

public class NetworkWatcherInitTest {

    @Test
    public void testRegisterNotInit() throws NetworkWatcher.OperationException {
        // 未初始化前，错误码应该是NOT_INIT
        try {
            NetworkWatcher.register(NetworkWatcher.TransportType.CELLULAR, new NetworkWatcherTest.MockCallback());
            fail();
        } catch (NetworkWatcher.OperationException e) {
            assertEquals(ErrorCode.NOT_INIT, e.getErrorCode());
        }
    }
}
