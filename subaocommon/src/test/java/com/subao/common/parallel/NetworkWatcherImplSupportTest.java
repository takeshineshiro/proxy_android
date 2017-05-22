package com.subao.common.parallel;

import android.content.Context;

import com.subao.common.RoboBase;
import com.subao.common.parallel.NetworkWatcher.Network;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class NetworkWatcherImplSupportTest extends RoboBase {

    private final Context context = RuntimeEnvironment.application.getApplicationContext();
    private final Callback testCallback = new Callback();
    private NetworkWatcherImpl_Support networkWatcher;

    private static NetworkWatcher.TransportType toTransportType(int ordinal) {
        NetworkWatcher.TransportType[] all = NetworkWatcher.TransportType.values();
        int length = all.length;

        if ((ordinal < 0) || (ordinal >= length)) {
            return null;
        }

        return all[ordinal];
    }

    @Before
    public void setup() {
        networkWatcher = new NetworkWatcherImpl_Support(context);
    }

    @Test
    public void testRegister() throws NetworkWatcher.OperationException {
        int count = NetworkWatcher.TransportType.values().length;
        for (int i = 0; i < count; i++) {
            NetworkWatcher.TransportType type = toTransportType(i);
            assertNotNull(type);
            Object obj = null;
            try {
                obj = networkWatcher.register(type, testCallback);
            } catch (NetworkWatcher.OperationException e) {

            } finally {
                networkWatcher.unregister(obj);
            }
            try {
                networkWatcher.register(type, null);
                fail();
            } catch (NullPointerException e) {

            }
        }

    }

    @Test
    public void testUnregister() {
        networkWatcher.unregister(null);
    }

    private static class Callback implements NetworkWatcher.Callback {

        @Override
        public void onAvailable(Network network) {
            assertNotNull(network);
        }

        @Override
        public void onLost(Network network) {
            assertNotNull(network);
        }

    }
}