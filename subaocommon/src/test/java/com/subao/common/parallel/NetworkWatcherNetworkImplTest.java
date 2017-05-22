package com.subao.common.parallel;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

import com.subao.common.RoboBase;

import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNetwork;

import java.net.DatagramSocket;
import java.net.SocketException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * NetworkWatcherNetworkImplTest
 * <p>Created by YinHaiBo on 2017/3/14.</p>
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@Config(sdk = 23)
public class NetworkWatcherNetworkImplTest extends RoboBase {

    private static Network createNetwork() {
        return ShadowNetwork.newInstance(123);
    }

    @Test(expected = NullPointerException.class)
    public void constructorNullArguments() {
        new NetworkWatcherNetworkImpl(null);
    }

    @Test
    public void test() throws Exception {
        Network network = createNetwork();
        NetworkWatcherNetworkImpl target = new NetworkWatcherNetworkImpl(network);
        assertNotNull(target.toString());
        assertTrue(target.equals(target));
        assertFalse(target.equals(null));
        assertFalse(target.equals(this));
        assertTrue(target.equals(new NetworkWatcherNetworkImpl(network)));
    }

    @Test
    public void testGetInfo() {
        NetworkWatcherNetworkImpl target = new NetworkWatcherNetworkImpl(createNetwork());
        Context context = mock(Context.class);
        doReturn(null).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = target.getInfo(context);
        assertNull(info);
        target.getInfo(getContext());
    }

    @Test
    public void bindSocket() throws SocketException, NetworkWatcher.OperationException {
        NetworkWatcherNetworkImpl target = new NetworkWatcherNetworkImpl(createNetwork());
        DatagramSocket socket = new DatagramSocket();
        try {
            target.bindToSocket(socket);
        } catch (Exception e) {}
    }

    @Test
    public void getNetIdFromNetwork() throws NetworkWatcher.OperationException {
        Network network = createNetwork();
        int netId = Integer.parseInt(network.toString());
        assertEquals(netId, NetworkWatcherNetworkImpl.getNetIdFromNetwork(network));
    }

    @Test
    public void bindSocketToNetwork() throws SocketException, NetworkWatcher.OperationException {
        DatagramSocket socket = new DatagramSocket();
        NetworkWatcherNetworkImpl.bindSocketToNetwork(socket, 100, 123);
    }
}