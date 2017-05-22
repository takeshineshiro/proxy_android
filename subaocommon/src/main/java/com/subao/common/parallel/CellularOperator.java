package com.subao.common.parallel;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.subao.common.Disposable;
import com.subao.common.ErrorCode;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.SwitchState;
import com.subao.common.net.MobileNetTypeDetector;
import com.subao.common.net.NetSwitch;
import com.subao.common.net.NetTypeDetector.NetType;
import com.subao.common.parallel.NetworkWatcher.Network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * 蜂窝网络的相关操作
 */
public class CellularOperator implements Disposable {

    private static final String TAG = LogTag.PARALLEL;
    private final Impl impl;
    private Object registerObj;

    private CellularOperator(CellularStateListener cellularStateListener) {
        this.impl = new Impl(cellularStateListener);
    }

    /**
     * 创建一个对象实体
     *
     * @param context               {@link Context}
     * @param cellularStateListener 蜂窝网络状态侦听器
     * @return 对象实体
     */
    public static CellularOperator create(Context context, CellularStateListener cellularStateListener) throws NetworkWatcher.OperationException {
        NetworkWatcher.init(context);
        CellularOperator cellularOperator = new CellularOperator(cellularStateListener);
        cellularOperator.registerObj = NetworkWatcher.register(
            NetworkWatcher.TransportType.CELLULAR, cellularOperator.impl);
        return cellularOperator;
    }

    @Override
    public void dispose() {
        synchronized (this) {
            if (this.registerObj != null) {
                NetworkWatcher.unregister(this.registerObj);
                this.registerObj = null;
            }
        }
    }

    /**
     * 申请一个基于移动网络的FD
     *
     * @return 成功返回FD（该FD由请求者负责管理），失败抛出{@link NetworkWatcher.OperationException}
     */
    public int requestNewMobileFD(Context context) throws NetworkWatcher.OperationException {
        return impl.createNewMobileFD(context);
    }

    /**
     * 蜂窝网状态侦听器（蜂窝网是否可用）
     */
    public interface CellularStateListener {
        void onCellularStateChange(boolean available);
    }

    static class Impl implements NetworkWatcher.Callback {

        private final CellularStateListener listener;
        private final List<Network> availableNetworks = new ArrayList<Network>(2);

        Impl(CellularStateListener listener) {
            this.listener = listener;
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        static int createNewMobileFD(Network network) throws NetworkWatcher.OperationException {
            DatagramSocket socket;
            try {
                socket = new DatagramSocket();
            } catch (IOException e) {
                Logger.w(TAG, e.getMessage());
                throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_CREATE_FD_FAIL);
            }
            int result;
            try {
                network.bindToSocket(socket);
                ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.fromDatagramSocket(socket);
                if (parcelFileDescriptor == null) {
                    throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR_AFTER_BIND);
                }
                result = parcelFileDescriptor.detachFd();
            } catch (RuntimeException e) {
                throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR_AFTER_BIND);
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
            return result;
        }

        /**
         * 在没有可用的蜂窝网络时，根据当前的移动网络开关状态决定错误码
         *
         * @param mobileSwitchState 当前移动网络开关状态
         * @return 错误代码
         */
        static int getErrorCodeWhenNoAvailableMobileNetwork(SwitchState mobileSwitchState) {
            switch (mobileSwitchState) {
            case OFF:
                return ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_OFF;
            case ON:
                return ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON;
            default:
                return ErrorCode.WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN;
            }
        }

        /**
         * 预判能否基于给定的{@link Network}创建FD，如果不能，抛出{@link NetworkWatcher.OperationException}异常
         *
         * @param context {@link Context}
         * @param network 给定的 {@link Network}
         */
        static void beforeCreateNewMobileFD(Context context, Network network) throws NetworkWatcher.OperationException {
            try {
                NetworkInfo info = network.getInfo(context);
                if (info != null) {
                    if (ConnectivityManager.TYPE_MOBILE != info.getType()) {
                        Logger.d(TAG, "The network type is not mobile, can not create FD by mobile");
                        throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_NOT_MOBILE);
                    }
                    if (NetType.MOBILE_2G == MobileNetTypeDetector.getMobileNetworkType(info.getSubtype())) {
                        Logger.d(TAG, "The network type is 2G, can not create FD by mobile");
                        throw new NetworkWatcher.OperationException(ErrorCode.WIFI_ACCEL_NOT_4G);
                    }
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

        private Network getCurrentAvailableMobileNetwork() {
            Network result;
            synchronized (this) {
                if (availableNetworks.isEmpty()) {
                    result = null;
                } else {
                    result = availableNetworks.get(availableNetworks.size() - 1);
                }
            }
            return result;
        }

        /**
         * 创建一个基于蜂窝网络的FD
         *
         * @param context {@link Context}
         * @return FD 成功创建到的FD
         */
        int createNewMobileFD(Context context) throws NetworkWatcher.OperationException {
            Network network = getCurrentAvailableMobileNetwork();
            if (network == null) {
                Logger.d(TAG, "No available cellular network.");
                network = findAvailableMobileNetwork(context);
                if (network == null) {
                    SwitchState mobileSwitchState = NetSwitch.getMobileSwitchState(context);
                    int errorCode = getErrorCodeWhenNoAvailableMobileNetwork(mobileSwitchState);
                    throw new NetworkWatcher.OperationException(errorCode);
                }
            }
            beforeCreateNewMobileFD(context, network);
            return createNewMobileFD(network);
        }

        /**
         * 查找一个可用的移动网络
         *
         * @param context {@link Context}
         * @return {@link Network}
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        static Network findAvailableMobileNetwork(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return null;
            }
            android.net.Network[] networks = cm.getAllNetworks();
            if (networks == null || networks.length == 0) {
                return null;
            }
            for (android.net.Network n : networks) {
                NetworkInfo networkInfo = cm.getNetworkInfo(n);
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    // 本版，为了测试申请成功率，不做isAvailable的判断
                    // && networkInfo.isAvailable()) {
                    //
                    if (NetType.MOBILE_2G != MobileNetTypeDetector.getMobileNetworkType(networkInfo.getSubtype())) {
                        return new NetworkWatcherNetworkImpl(n);
                    }
                }
            }
            return null;
        }


        @Override
        public void onAvailable(Network network) {
            int countAvailable;
            synchronized (this) {
                for (int i = availableNetworks.size() - 1; i >= 0; --i) {
                    Network exists = availableNetworks.get(i);
                    if (exists.equals(network)) {
                        availableNetworks.set(i, network);  // 用新的覆盖原来的
                        return;
                    }
                }
                availableNetworks.add(network);
                countAvailable = availableNetworks.size();
            }
            if (this.listener != null && countAvailable == 1) {
                this.listener.onCellularStateChange(true);
            }
        }

        @Override
        public void onLost(Network network) {
            if (availableNetworks.isEmpty()) {
                return;
            }
            boolean isAvailableEmpty;
            synchronized (this) {
                for (int i = availableNetworks.size() - 1; i >= 0; --i) {
                    Network exists = availableNetworks.get(i);
                    if (exists.equals(network)) {
                        availableNetworks.remove(i);
                        break;
                    }
                }
                isAvailableEmpty = availableNetworks.isEmpty();
            }
            if (isAvailableEmpty && this.listener != null) {
                this.listener.onCellularStateChange(false);
            }
        }
    }

}
