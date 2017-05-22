package com.subao.common.data;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.subao.common.net.Http;
import com.subao.common.net.NetTypeDetector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 负责与HR服务器通讯的抽象基类
 * <p>Created by YinHaiBo on 2017/2/22.</p>
 */

public abstract class HRDataTrans extends AsyncTask<Void, Void, HRDataTrans.Result> {

    static final int TIMEOUT = 8 * 1000;

    protected final Arguments arguments;
    protected final UserInfo userInfo;
    private final Http.Method httpMethod;
    private final byte[] extraData;

    protected HRDataTrans(Arguments arguments, UserInfo userInfo, Http.Method httpMethod, byte[] extraData) {
        this.arguments = arguments;
        this.userInfo = userInfo;
        this.httpMethod = httpMethod;
        this.extraData = extraData;
    }

    @Override
    protected Result doInBackground(Void... params) {
        URL url;
        try {
            url = buildUrl();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        //
        Result result = null;
        int delay = 10 * 1000;
        for (int i = 0; i < 3; ++i) {
            if (arguments.netTypeDetector.isConnected()) {
                result = doHttpRequest(url);
            } else {
                result = null;
            }
            if (httpMethod == Http.Method.POST && (result == null || result.response == null || result.response.code == 500)) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                delay *= 2;
            } else {
                break;
            }
        }
        return result;
    }

    private Result doHttpRequest(URL url) {
        Http.Response response = null;
        HttpURLConnection connection = null;
        try {
            connection = new Http(TIMEOUT, TIMEOUT).createHttpUrlConnection(url, httpMethod, Http.ContentType.JSON.str);
            if (useBearerAuth() && !TextUtils.isEmpty(userInfo.jwtToken)) {
                connection.addRequestProperty("Authorization", "Bearer " + userInfo.jwtToken);
            }
            switch (httpMethod) {
            case GET:
            case DELETE:
                response = Http.doGet(connection);
                break;
            default:
                response = Http.doPost(connection, extraData);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return new Result(connection, response);
    }

    private URL buildUrl() throws IOException {
        return new URL(
            getUrlProtocol(),
            arguments.serviceLocation.host,
            arguments.serviceLocation.port,
            getUrlPart());
    }

    /**
     * 使用http还是https协议？
     * <p><b>缺省实现：使用构造函数里Arguments.ServiceLocation指明的协议</b></p>
     *
     * @return "http" 或 "https"
     */
    protected String getUrlProtocol() {
        return arguments.serviceLocation.protocol;
    }

    /**
     * 是否在请求头里使用Bearer验证
     * <p><b>缺省实现：如果是https则使用，http不使用</b></p>
     *
     * @return true表示要使用，false表示不使用
     */
    protected boolean useBearerAuth() {
        return "https".equals(getUrlProtocol());
    }

    protected abstract String getUrlPart();


    public static class Arguments extends HttpArguments {

        public Arguments(String clientType, String version, ServiceLocation serviceLocation, NetTypeDetector netTypeDetector) {
            super(clientType, version, adjustServiceLocation(serviceLocation), netTypeDetector);
        }

        private static ServiceLocation adjustServiceLocation(ServiceLocation serviceLocation) {
            if (serviceLocation == null) {
                serviceLocation = new ServiceLocation("https", Address.EndPoint.HR.host, Address.EndPoint.HR.port);
            }
            return serviceLocation;
        }
    }

    public static class UserInfo {
        public final String userId;
        public final String jwtToken;

        public UserInfo(String userId, String jwtToken) {
            this.userId = userId;
            this.jwtToken = jwtToken;
        }
    }

    public static class Result {
        public final HttpURLConnection connection;
        public final Http.Response response;

        public Result(HttpURLConnection connection, Http.Response response) {
            this.connection = connection;
            this.response = response;
        }
    }
}
