package com.subao.gamemaster;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.subao.common.ErrorCode;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.accel.AccelEngineInstance;

import java.io.IOException;
import java.util.List;

/**
 * 加速引擎的VPN Service
 * <ul>
 * <li>提供“开启代理”接口 ({@link GameMasterVpnServiceInterface#startProxy(List)})</li>
 * <li>提供“结束代理”接口 ({@link GameMasterVpnServiceInterface#stopProxy()})</li>
 * <li>服务被吊销时，断开VPN连接，并结束代理</li>
 * </ul>
 * <p>Created by YinHaiBo on 2017/3/22.</p>
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GameMasterVpnService extends VpnService {

    private static final String TAG = LogTag.GAME;

    private static final String INTERFACE_ADDRESS = "198.51.100.10";
    private static final String ROUTE = "0.0.0.0";

    private ParcelFileDescriptor vpnInterface;

    private static void outputLog(String message) {
        Log.d(TAG, "GameVpn: " + message);
    }

    /**
     * 开启服务
     */
    public static synchronized boolean open(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, GameMasterVpnService.class);
        return context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean addAllowedApplication(Builder builder, String packageName, boolean needLog) {
        if (needLog) {
            outputLog(String.format("add allowed app (%s)", packageName));
        }
        try {
            builder.addAllowedApplication(packageName);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onCreate() {
        if (Logger.isLoggableDebug(TAG)) {
            outputLog("service create");
        }
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder superBinder = super.onBind(intent);
        if (superBinder != null) {
            return superBinder;
        } else {
            return new Binder();
        }
    }

    @Override
    public void onRevoke() {
        if (Logger.isLoggableDebug(TAG)) {
            Logger.d(TAG, "service revoked");
        }
        stopProxy();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        if (Logger.isLoggableDebug(TAG)) {
            outputLog("service destroy");
        }
        stopProxy();
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void addAllowedAppToBuilder(Builder builder, Iterable<String> allowedPackageNames) {
        boolean needLog = Logger.isLoggableDebug(TAG);
        if (!addAllowedApplication(builder, getPackageName(), needLog)) {
            return;
        }
        if (allowedPackageNames != null) {
            for (String packageName : allowedPackageNames) {
                addAllowedApplication(builder, packageName, needLog);
            }
        }
    }

    /**
     * 开启迅游加速
     *
     * @param allowedPackageNames 代理白名单
     * @return 0表示成功，其它值为错误代码
     */
    int startProxy(Iterable<String> allowedPackageNames) {
        boolean needLog = Logger.isLoggableDebug(TAG);
        synchronized (this) {
            if (vpnInterface == null) {
                if (needLog) {
                    outputLog("establish ...");
                }
                try {
                    Builder builder = new Builder();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        addAllowedAppToBuilder(builder, allowedPackageNames);
                    }
                    builder.addAddress(INTERFACE_ADDRESS, 32);
                    builder.addRoute(ROUTE, 0);
                    // 注意：在某些机型的5.0手机上面老用户卸载时VPN没有手动断开 也会创建失败
                    vpnInterface = builder.setSession("迅游加速服务已开启").setConfigureIntent(null).establish();
                    if (needLog) {
                        outputLog("establish succeeded");
                    }
                } catch (Exception e) {
                    return ErrorCode.VPN_ESTABLISH_EXCEPTION;
                } catch (Error e) {
                    return ErrorCode.VPN_ESTABLISH_ERROR;
                }
            }
        }
        // 开启PROXY，并传递FD
        int fd = vpnInterface.getFd();
        return AccelEngineInstance.get().startVPN(fd) ? ErrorCode.OK : ErrorCode.VPN_JNI_START_FAIL;
    }

    /**
     * 停止迅游加速，断开VPN连接
     */
    void stopProxy() {
        boolean needLog = Logger.isLoggableDebug(TAG);
        synchronized (this) {
            if (vpnInterface != null) {
                // 关闭代理
                AccelEngineInstance.get().stopVPN();
                //
                if (needLog) {
                    outputLog("close interface");
                }
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                vpnInterface = null;
            }
        }
    }

    private class Binder extends GameMasterVpnServiceInterface.Stub {

        @Override
        public int startProxy(List<String> allowedPackageNames) throws RemoteException {
            return GameMasterVpnService.this.startProxy(allowedPackageNames);
        }

        @Override
        public void stopProxy() throws RemoteException {
            GameMasterVpnService.this.stopProxy();
        }

        @Override
        public boolean protectSocket(int socket) throws RemoteException {
            return GameMasterVpnService.this.protect(socket);
        }

    }

}
