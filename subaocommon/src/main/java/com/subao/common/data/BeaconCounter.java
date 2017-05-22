package com.subao.common.data;

import android.os.AsyncTask;

import com.subao.common.net.Http;
import com.subao.common.thread.ThreadPool;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 负责向服务端发起计数请求
 * <p>Created by YinHaiBo on 2017/3/6.</p>
 */
public class BeaconCounter extends AsyncTask<BeaconCounter.Callback, Void, Boolean> {

    private final String clientType;
    private final ServiceLocation serviceLocation;
    private final String counterType;

    private BeaconCounter(String clientType, ServiceLocation serviceLocation, String counterType) {
        this.clientType = clientType;
        this.serviceLocation = serviceLocation;
        this.counterType = counterType;
    }

    /**
     * 启动工作线程对服务器计数器进行请求
     *
     * @param clientType      客户端类型。如果是APP，则为常量"android"（{@link Defines#REQUEST_CLIENT_TYPE_FOR_APP}，如果是SDK，则为游戏的GUID
     * @param serviceLocation 服务位置
     * @param counterType     计数类型
     * @param callback        成功或失败时的回调
     */
    public static void start(String clientType, ServiceLocation serviceLocation, String counterType, Callback callback) {
        serviceLocation = new ServiceLocation(null, serviceLocation.host, serviceLocation.port);    // 总是用http协议
        BeaconCounter beaconCounter = new BeaconCounter(clientType, serviceLocation, counterType);
        beaconCounter.executeOnExecutor(ThreadPool.getExecutor(), callback);
    }

    @Override
    protected Boolean doInBackground(Callback... params) {
        boolean succeed = false;
        Http http = new Http(3000, 3000);
        try {
            URL url = new URL(serviceLocation.protocol,
                serviceLocation.host, serviceLocation.port,
                "/api/v1/" + clientType + "/counters/" + counterType
            );
            HttpURLConnection connection = http.createHttpUrlConnection(url, Http.Method.POST, Http.ContentType.JSON.str);
            Http.Response response = Http.doPost(connection, null);
            if (response.code == 201) {
                succeed = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        params[0].onCounter(succeed);
        return succeed;
    }

    /**
     * 请求成功或失败时的异步回调
     * <p><b>注意，这是在工作线程中进行的回调</b></p>
     */
    public interface Callback {

        /**
         * 请求成功或失败时的异步回调
         * <p><b>注意，这是在工作线程中进行的回调</b></p>
         *
         * @param succeed true表示计数成功，false表示计数失败
         */
        void onCounter(boolean succeed);
    }

}
