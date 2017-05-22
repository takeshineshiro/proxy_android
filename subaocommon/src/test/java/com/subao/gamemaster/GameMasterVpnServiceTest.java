package com.subao.gamemaster;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import com.subao.common.Logger;
import com.subao.common.RoboBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * GameMasterVpnServiceTest
 * <p>Created by YinHaiBo on 2017/3/24.</p>
 */
public class GameMasterVpnServiceTest extends RoboBase {

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
    }

    @After
    public void tearDown() {
        Logger.setLoggableChecker(null);
    }

    @Test
    public void open() throws Exception {
        Context context = mock(Context.class);
        doReturn(context).when(context).getApplicationContext();
//        doReturn(true).when(context).bindService(any(Intent.class), any(ServiceConnection.class), Context.BIND_AUTO_CREATE);
        GameMasterVpnService.open(context, mock(ServiceConnection.class));
    }

    @Test
    public void instMethods() throws Exception {
        GameMasterVpnService service = new GameMasterVpnService();
        service.onCreate();
        assertNotNull(service.onBind(new Intent()));
        service.startProxy(null);
        service.stopProxy();
        service.onDestroy();
    }


}