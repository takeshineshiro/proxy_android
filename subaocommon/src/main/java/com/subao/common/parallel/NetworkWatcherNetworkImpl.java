package com.subao.common.parallel;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.subao.common.ErrorCode;
import com.subao.common.Misc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocket;

/**
 * {@link NetworkWatcher.Network}的实现
 */
public class NetworkWatcherNetworkImpl implements NetworkWatcher.Network {
    private final android.net.Network network;

    public NetworkWatcherNetworkImpl(android.net.Network network) {
        if (network == null) {
            throw new NullPointerException("Null network");
        }
        this.network = network;
    }

    static int getNetIdFromNetwork(android.net.Network network) throws NetworkWatcher.OperationException {
        try {
            Field field = network.getClass().getDeclaredField("netId");
            if (field != null) {
                Object obj = field.get(network);
                if (obj != null && (obj instanceof Integer)) {
                    return (Integer) obj;
                }
            }
        } catch (Exception e) {
        }
        throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_FAIL_GET_NET_ID);
    }

    static void bindSocketToNetwork(DatagramSocket socket, int fd, int netId) throws NetworkWatcher.OperationException {
        try {
            Class<?> cls = Class.forName("android.net.NetworkUtils");
            if (cls != null) {
                Method m = cls.getDeclaredMethod("bindSocketToNetwork", int.class, int.class);
                if (m != null) {
                    socket.getReuseAddress();
                    int err = (Integer) m.invoke(null, fd, netId);
                    if (err == 0) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
        }
        throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_BIND_FD_FAIL_VER21);

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void bindToSocket(DatagramSocket socket) throws NetworkWatcher.OperationException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_ANDROID_VERSION_TOO_LOW);
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.1的，直接用API搞
            try {
                this.network.bindSocket(socket);
                return;
            } catch (Exception e) {
                throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_BIND_FD_FAIL_VER22);
            } catch (Error e) {
                throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_BIND_FD_FAIL_VER22);
            }
        }
        //
        int netId = getNetIdFromNetwork(network);
        ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.fromDatagramSocket(socket);
        if (parcelFileDescriptor == null) {
            throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR);
        }
        int fd;
        try {
            fd = parcelFileDescriptor.getFd();
        } catch (RuntimeException e) {
            throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_FAIL_GET_FD);
        }
        bindSocketToNetwork(socket, fd, netId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public NetworkInfo getInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return null;
        }
        return cm.getNetworkInfo(this.network);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public String toString() {
        return this.network.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof NetworkWatcherNetworkImpl)) {
            return false;
        }
        NetworkWatcherNetworkImpl other = (NetworkWatcherNetworkImpl) o;
        return Misc.isEquals(this.network, other.network);
    }
}
