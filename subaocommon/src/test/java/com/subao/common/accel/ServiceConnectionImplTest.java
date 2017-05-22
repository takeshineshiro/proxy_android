package com.subao.common.accel;

import android.content.ComponentName;
import android.os.RemoteException;

import com.subao.common.ErrorCode;
import com.subao.common.RoboBase;
import com.subao.gamemaster.GameMasterVpnService;
import com.subao.gamemaster.GameMasterVpnServiceInterface;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * ServiceConnectionImplTest
 * <p>Created by YinHaiBo on 2017/3/24.</p>
 */
public class ServiceConnectionImplTest extends RoboBase {

    @Test
    public void test() throws Exception {
        EngineWrapper.ServiceConnectionImpl target = new EngineWrapper.ServiceConnectionImpl(null);
        Binder binder = new Binder();
        //
        for (int i = 0; i < 2; ++i) {
            binder.throwException = (i == 1);
            target.onServiceConnected(
                new ComponentName(getContext(), GameMasterVpnService.class),
                binder);
            assertEquals("startProxy", binder.action[0]);
            assertNull(binder.action[1]); // FIXME
            //
            int expected = binder.throwException ? ErrorCode.VPN_PROTECT_SOCKET_FAIL : ErrorCode.OK;
            assertEquals(expected, target.protectSocket(123));
            assertEquals("protectSocket", binder.action[0]);
            assertEquals(123, binder.action[1]);
            //
            assertEquals(ErrorCode.VPN_PROTECT_SOCKET_FAIL, target.protectSocket(0));
            assertEquals("protectSocket", binder.action[0]);
            assertEquals(0, binder.action[1]);
            //
            target.stopProxy();
            assertEquals("stopProxy", binder.action[0]);
            //
            target.onServiceDisconnected(null);
        }
    }

    private static class Binder extends GameMasterVpnServiceInterface.Stub {

        public boolean throwException;
        public Object[] action;

        @Override
        public int startProxy(List<String> allowedPackageNames) throws RemoteException {
            action = new Object[]{
                "startProxy", allowedPackageNames
            };
            if (throwException) {
                throw new RemoteException();
            }
            return 0;
        }

        @Override
        public void stopProxy() throws RemoteException {
            action = new Object[]{
                "stopProxy"
            };
            if (throwException) {
                throw new RemoteException();
            }
        }

        @Override
        public boolean protectSocket(int socket) throws RemoteException {
            action = new Object[]{
                "protectSocket", socket
            };
            if (throwException) {
                throw new RemoteException();
            } else {
                return (socket > 0);
            }
        }
    }
}