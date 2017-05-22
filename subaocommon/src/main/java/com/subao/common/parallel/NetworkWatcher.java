package com.subao.common.parallel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.subao.common.ErrorCode;
import com.subao.common.LogTag;
import com.subao.common.Logger;

import java.io.IOException;
import java.net.DatagramSocket;

/**
 * 针对Android 5.0及以上系统，对指定的网络进行监测
 *
 * @author YHB
 */
public class NetworkWatcher {

    private static final String TAG = LogTag.PARALLEL;

    private static NetworkWatcherImpl impl = new NetworkWatcherImpl_UnSupport(
        ErrorCode.NOT_INIT
    );

    private NetworkWatcher() {
    }

    private static synchronized void setImpl(NetworkWatcherImpl newImpl) {
        if (impl != null) {
            impl.dispose();
        }
        impl = newImpl;
    }

    /**
     * 初始化
     */
    public static void init(Context context) throws OperationException {
        int errorCode;
        if (!isAndroidVersionSupported()) {
            errorCode = ErrorCode.WIFI_ACCEL_ANDROID_VERSION_TOO_LOW;
        } else if (!hasRequiredPermission(context)) {
            errorCode = ErrorCode.WIFI_ACCEL_NO_PERMISSION;
        } else {
            setImpl(new NetworkWatcherImpl_Support(context));
            return;
        }
        setImpl(new NetworkWatcherImpl_UnSupport(errorCode));
        throw new OperationException(errorCode);
    }

    /**
     * 判断本设备是否支持这一特性
     * <p>
     * （Android 5.0及以上系统支持）
     * </p>
     *
     * @return true表示支持，false表示不支持
     */
    private static boolean isAndroidVersionSupported() {
        boolean support = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        if (!support && Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, "WiFi-Accel not supported on Android version " + android.os.Build.VERSION.SDK_INT);
        }
        return support;
    }

    private static boolean hasRequiredPermission(Context context) {
        // 5.x手机必须要有CHANGE_NETWORK_STATE权限
        // 6.x手机可以有CHANGE_NETWORK_STATE和WRITE_SETTINGS二者其一即可
        // 这里为了兼容，判断必须是CHANGE_NETWORK_STATE
        boolean has = hasRequiredPermission(context, "android.permission.CHANGE_NETWORK_STATE");
        if (!has) {
            Logger.d(TAG, "Has not required permission: CHANGE_NETWORK_STATE");
        }
        return has;
    }

    private static boolean hasRequiredPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 注册一个回调{@link Callback}
     *
     * @return 一个注册ID，今后反注册的时候要用
     * @throws OperationException
     * @see #unregister(Object)
     */
    public static Object register(TransportType type, Callback callback) throws OperationException {
        if (callback == null) {
            throw new NullPointerException("Callback cannot be null");
        }
        return impl.register(type, callback);
    }

    /**
     * 反注册回调 {@link Callback}
     *
     * @param registerObj 调用{@link #register(TransportType, Callback)}时得到的“注册对象”
     */
    public static void unregister(Object registerObj) {
        impl.unregister(registerObj);
    }

    /**
     * 网络类型
     */
    public enum TransportType {
        CELLULAR,
        WIFI,
        BLUETOOTH,
        ETHERNET,
        VPN
    }

    /**
     * 抽象的Network，提供所需方法
     */
    public interface Network {

        /**
         * 将本Network绑定到指定的Socket上面
         *
         * @throws IOException
         */
        void bindToSocket(DatagramSocket socket) throws OperationException;

        /**
         * 返回详细信息
         *
         * @param context {@link Context}
         * @return {@link NetworkInfo}
         */
        NetworkInfo getInfo(Context context);
    }

    /**
     * 当{@link Network}发生改变的时的回调
     */
    public interface Callback {

        /**
         * Network可用
         */
        void onAvailable(Network network);

        /**
         * 网络丢失
         */
        void onLost(Network network);

    }

    /**
     * 进行相关操作时可能发生的异常
     */
    public static class OperationException extends IOException {

        private final int errorCode;

        public OperationException(int errorCode) {
            super("Cellular Operation Exception, Error " + errorCode);
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return this.errorCode;
        }

    }

    private static class NetworkWatcherImpl_UnSupport implements NetworkWatcherImpl {

        private final int errorCode;

        NetworkWatcherImpl_UnSupport(int errorCode) {
            this.errorCode = errorCode;
        }

        @Override
        public Object register(TransportType transportType, Callback callback) throws OperationException {
            throw new OperationException(errorCode);
        }

        @Override
        public void unregister(Object registerObj) {
            // do nothing;
        }

        @Override
        public void dispose() {
            // do nothing;
        }
    }
}
