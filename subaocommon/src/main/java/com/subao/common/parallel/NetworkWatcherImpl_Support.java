package com.subao.common.parallel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;

import com.subao.common.ErrorCode;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.parallel.NetworkWatcher.TransportType;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link NetworkWatcher}的实现。仅Android 5.0及以上系统支持
 */
@SuppressLint("NewApi")
class NetworkWatcherImpl_Support implements NetworkWatcherImpl {

    private static final String TAG = LogTag.PARALLEL;

    private final Context context;

    private final List<ConnectivityManager.NetworkCallback> registeredCallbackList
        = new ArrayList<ConnectivityManager.NetworkCallback>(4);

    NetworkWatcherImpl_Support(Context context) {
        this.context = context.getApplicationContext();
    }

    private static int transportTypeToInt(TransportType type) {
        switch (type) {
        case WIFI:
            return android.net.NetworkCapabilities.TRANSPORT_WIFI;
        case BLUETOOTH:
            return android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH;
        case ETHERNET:
            return android.net.NetworkCapabilities.TRANSPORT_ETHERNET;
        case VPN:
            return android.net.NetworkCapabilities.TRANSPORT_VPN;
        default:
            return android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
        }
    }

    private void doRegister(TransportType transportType, ConnectivityManager.NetworkCallback callback) throws NetworkWatcher.OperationException {
        try {
            android.net.NetworkRequest.Builder builder = new android.net.NetworkRequest.Builder();
            builder.addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addTransportType(transportTypeToInt(transportType));
            android.net.NetworkRequest request = builder.build();
            if (request != null) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                cm.requestNetwork(request, callback);
                return;
            }
            Logger.e(LogTag.PARALLEL, "NetworkRequest.Builder.build() return null");
        } catch (RuntimeException e) {
            Logger.e(LogTag.PARALLEL, e.getMessage());
        }
        Logger.e(LogTag.PARALLEL, "requestNetwork() failed !!!");
        throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_REGISTER_FAIL);
    }

    @Override
    public Object register(TransportType transportType, NetworkWatcher.Callback callback) throws NetworkWatcher.OperationException {
        NetworkCallbackWrapper callbackWrapper = new NetworkCallbackWrapper(callback);
        doRegister(transportType, callbackWrapper);
        synchronized (this) {
            this.registeredCallbackList.add(callbackWrapper);
        }
        return callbackWrapper;
    }

    @Override
    public void unregister(Object registerObj) {
        if (registerObj != null) {
            int idx;
            synchronized (this) {
                idx = this.registeredCallbackList.indexOf(registerObj);
                if (idx >= 0) {
                    this.registeredCallbackList.remove(idx);
                }
            }
            if (idx >= 0) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                cm.unregisterNetworkCallback((ConnectivityManager.NetworkCallback) registerObj);
            }
        }
    }

    @Override
    public void dispose() {
        ConnectivityManager.NetworkCallback[] clone = null;
        synchronized (this) {
            int len = this.registeredCallbackList.size();
            if (len > 0) {
                clone = this.registeredCallbackList.toArray(new ConnectivityManager.NetworkCallback[len]);
                this.registeredCallbackList.clear();
            }
        }
        if (clone != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            for (ConnectivityManager.NetworkCallback nc : clone) {
                cm.unregisterNetworkCallback(nc);
            }
        }
    }

    private static class NetworkCallbackWrapper extends ConnectivityManager.NetworkCallback {

        private final NetworkWatcher.Callback callback;

        public NetworkCallbackWrapper(NetworkWatcher.Callback callback) {
            if (callback == null) {
                throw new NullPointerException("Null callback");
            }
            this.callback = callback;
        }

        @Override
        public void onAvailable(android.net.Network network) {
            this.callback.onAvailable(new NetworkWatcherNetworkImpl(network));
        }

        @Override
        public void onLost(android.net.Network network) {
            this.callback.onLost(new NetworkWatcherNetworkImpl(network));
        }

    }


}
