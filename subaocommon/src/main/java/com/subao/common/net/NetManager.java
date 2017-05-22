package com.subao.common.net;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;

import com.subao.common.Disposable;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.utils.ThreadUtils;

public class NetManager implements NetTypeDetector, Disposable {

    private final static String TAG = LogTag.NET;

    private Context context;
    private ConnectivityManager connectivityManager;

    private boolean isNetworkConnected;
    private boolean isWiFiConnected;
    private boolean isMobileConnected;

    private NetType currentNetType;

    /**
     * 网络变化检测
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * 网络变化侦听
     */
    private Listener listener;

    /**
     * 必须在主线程里被调用
     */
    public NetManager(Context context) {
        if (!ThreadUtils.isInAndroidUIThread()) {
            Logger.e(TAG, "Call NetManager.createInstance() not in main thread");
        }
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                    resetConnectionState(context);
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(broadcastReceiver, filter);
//        filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//        context.registerReceiver(broadcastReceiver, filter);
        //
        loadCurrentConnectivityState(context);
    }

    @Override
    public void dispose() {
        synchronized (this) {
            if (context != null) {
                try {
                    context.unregisterReceiver(this.broadcastReceiver);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                this.context = null;
                this.broadcastReceiver = null;
                this.connectivityManager = null;
            }
            this.listener = null;
        }
    }

    //    public boolean isCurrent2G() {
//        return isMobileConnected && getActiveNetworkClass() == NetTypeDetector.NetType.MOBILE_2G;
//    }

    /**
     * 只在主线程里被调用
     */
    @SuppressLint("DefaultLocale")
    private void resetConnectionState(Context context) {
        loadCurrentConnectivityState(context);
        NetType newNetType = getCurrentConnectionType();
        if (newNetType != currentNetType) {
            Logger.d(TAG, String.format("Connection Changed: %d -> %d",
                currentNetType == null ? -1 : currentNetType.value,
                newNetType.value));
            currentNetType = newNetType;
            Listener listener = this.listener;
            if (listener != null) {
                listener.onConnectivityChange(currentNetType);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void loadCurrentConnectivityState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi != null && State.CONNECTED == wifi.getState()) {
            this.setWifiState();
            return;
        }

        NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobile != null && State.CONNECTED == mobile.getState()) {
            this.setMobileState();
            return;
        }

        this.setDisconnected();
    }

    private void setMobileState() {
        this.isNetworkConnected = true;
        this.isMobileConnected = true;
        this.isWiFiConnected = false;
    }

    private void setWifiState() {
        this.isNetworkConnected = true;
        this.isWiFiConnected = true;
        this.isMobileConnected = false;
    }

    private void setDisconnected() {
        this.isNetworkConnected = false;
        this.isWiFiConnected = false;
        this.isMobileConnected = false;
    }

    /**
     * Return general class of network type, such as "3G" or "4G". In cases
     * where classification is contentious, this method is conservative.
     *
     * @return NETWORK_CLASS_2_G、NETWORK_CLASS_3_G、NETWORK_CLASS_4_G、
     * NETWORK_CLASS_UNKNOWN
     */
    private NetType getActiveNetworkClass() {
        ConnectivityManager connectivityManager = this.connectivityManager;
        if (connectivityManager == null) {
            return NetTypeDetector.NetType.UNKNOWN;
        }
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
        if (ni == null) {
            Logger.w(TAG, "getActiveNetworkInfo() return null");
            return NetTypeDetector.NetType.DISCONNECT;
        }
        if (!ni.isConnectedOrConnecting()) {
            return NetTypeDetector.NetType.DISCONNECT;
        }
        switch (ni.getType()) {
        case ConnectivityManager.TYPE_WIFI:
            return NetTypeDetector.NetType.WIFI;
        case ConnectivityManager.TYPE_MOBILE:
            return MobileNetTypeDetector.getMobileNetworkType(ni.getSubtype());
        default:
            Logger.w(TAG, "NetworkInfo.getType() return: " + ni.getType());
            return NetTypeDetector.NetType.UNKNOWN;
        }
    }

    /**
     * 取当前网络连接的类型
     *
     * @return 如果当前未连接，返回NETWORK_CLASS_UNKNOWN，否则返回 NETWORK_CLASS_WIFI 或
     * 2G、3G或4G *
     */
    private NetTypeDetector.NetType getCurrentConnectionType() {
        if (this.isConnected()) {
            if (this.isWiFiConnected()) {
                return NetType.WIFI;
            } else {
                return getActiveNetworkClass();
            }
        } else {
            return NetType.DISCONNECT;
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public NetType getCurrentNetworkType() {
        return getCurrentConnectionType();
    }

    @Override
    public boolean isConnected() {
        return isNetworkConnected;
    }

    @Override
    public boolean isWiFiConnected() {
        return isWiFiConnected;
    }

    @Override
    public boolean isMobileConnected() {
        return isMobileConnected;
    }

    public interface Listener {
        void onConnectivityChange(NetTypeDetector.NetType netType);
    }

}
