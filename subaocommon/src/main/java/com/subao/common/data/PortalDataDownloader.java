package com.subao.common.data;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.io.Persistent;
import com.subao.common.net.Http;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.CalendarUtils;
import com.subao.common.utils.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.Executor;

/**
 * 负责从Portal系统下载数据<br />
 * <p>
 * 注：<br />
 * 在调用{@link PortalDataDownloader#execute(Object[])} 或 {@link PortalDataDownloader#executeOnExecutor(Executor, Object[])}
 * 的时候，可以传递一个初始的{@link PortalDataEx}，例如：
 * <pre><i>
 *         PortalDataDownlader downloader = new XXX(...);
 *         PortalDataEx data = downloader.loadFromPersistent();
 *         downloader.execute(data);
 * </i></pre>
 * 如果传递了这个初始的Data，无论它是不是null，在工作线程开始工作的时候，就不再从本地文件加载初始数据了。
 * 这通常用于类似于下面这种场景：
 * <p>
 * {@link PortalScriptDownloader} 为了在初始化JNI的时候就能有较新的脚本数据传递给JNI函数，所以在启动线程
 * 去网络下载之前，先执行{@link PortalDataDownloader#loadFromPersistent()}方法，从本地文件加载数据（注意加载
 * 到的数据可能为null），然后再启动下载线程。<br />
 * 通常情况下，下载线程会先从本地加载缓存数据以得到CacheTag。在上述场景下，线程启动前已加载过一次了，所以为了
 * 优化性能，这里就在execute的时候把启动线程前就加载的数据传递过来，不用重复加载了。<br />
 * <p>
 * </p>
 * </p>
 */
public abstract class PortalDataDownloader extends AsyncTask<PortalDataEx, Void, PortalDataEx> {

    public static final int CONNECT_TIMEOUT = 8 * 1000;
    public static final int RECEIVE_TIMEOUT = 8 * 1000;
    private static final String TAG = LogTag.DATA;

    /**
     * 参数
     */
    private final Arguments arguments;

    /**
     * 构造函数
     *
     * @param arguments {@link Arguments} 相关的必要参数
     */
    protected PortalDataDownloader(Arguments arguments) {
        this.arguments = arguments;
    }

    protected static boolean isDebugLogAllowedNow() {
        return Logger.isLoggableDebug(TAG);
    }

    private static long parseExpireTimeFromHttpResponse(HttpURLConnection conn) {
        String field = conn.getHeaderField("Cache-Control");
        if (TextUtils.isEmpty(field)) {
            return 0L;
        }
        String key = "max-age=";
        if (field.length() <= key.length()) {
            return 0L;
        }
        if (!field.startsWith(key)) {
            return 0L;
        }
        String value = field.substring(key.length());
        try {
            long result = Long.parseLong(value);
            return (result * 1000L) + System.currentTimeMillis();
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 按统一的格式输出Debug日志
     */
    protected final void printDebugLog(String log) {
        Log.d(TAG, buildLogMessage(log));
    }

    protected final void printWarningLog(String log) {
        Log.w(TAG, buildLogMessage(log));
    }

    private String buildLogMessage(String log) {
        return "Portal." + getId() + ": " + log;
    }

    protected final URL buildUrl() throws MalformedURLException {
        String path = "/api/v1/" + arguments.clientType + "/" + getUrlPart();
        return new URL(arguments.serviceLocation.protocol,
            arguments.serviceLocation.host,
            arguments.serviceLocation.port,
            path);
    }

    private String buildFilename() {
        return getId() + ".portal2";
    }

    private Persistent getPersistent() {
        return arguments.createPersistent(this.buildFilename());
    }

    /**
     * 由派生类实现，返回一个唯一的URL段，比如：
     * <ul>
     * <li>"configs/general"</li>
     * <li>"nodes"</li>
     * <li>"configs/convergence"</li>
     * </ul>
     * {@link PortalDataDownloader}用其值构建请求的URL，形如：<br />
     * <p><b>"http://portal.wsds.cn/api/v1/827006BE-64F7-4082-B252-33ACF328A3A5/configs/general"</b></p>
     */
    protected abstract String getUrlPart();

    /**
     * 由派生类实现，返回一个唯一标识，用于：
     * <ul>
     * <li>本地缓存的文件名</li>
     * <li>日志输出</li>
     * </ul>
     * 示例："general"、"convergence" ...<br />
     * {@link PortalDataDownloader}用其值构建文件名，如："general.portal2"
     */
    protected abstract String getId();

    /**
     * 派生类可重写，指明HTTP头里的AcceptType。默认为"application/json"
     */
    protected String getHttpAcceptType() {
        return Http.ContentType.JSON.str;
    }

    /**
     * 派生类可重写，实现对数据的检查。默认实现直接返回true
     *
     * @return true表示数据是OK的，可以用
     */
    protected boolean checkDownloadData(PortalDataEx data) {
        return data != null;
    }

    /**
     * 从持久化介质里加载数据
     *
     * @return 读取失败时返回null
     */
    protected final PortalDataEx loadFromPersistent() {
        PortalDataEx result = null;
        Persistent persistent = getPersistent();
        if (persistent.exists()) {
            try {
                result = PortalDataEx.deserialize(persistent.openInput());
            } catch (IOException e) {
                printWarningLog(e.getMessage());
            }
        }
        return result;
    }

    final void deletePersistent() {
        getPersistent().delete();
    }

    /**
     * 给定的{@link PortalDataEx}中的版本号与当前版本相符吗？
     */
    final boolean isVersionValid(PortalDataEx data) {
        return data != null && Misc.isEquals(this.arguments.version, data.getVersion());
    }

    public final Arguments getArguments() {
        return this.arguments;
    }

    /**
     * 在工作线程里执行下载操作
     *
     * @param params 如果传递了一个PortalDataEx初始数据，就不再去本地文件加载了
     */
    @SuppressLint("DefaultLocale")
    @Override
    protected PortalDataEx doInBackground(PortalDataEx... params) {
        boolean allowLog = isDebugLogAllowedNow();
        //
        // 使用初始数据，如果没有则加载本地文件
        PortalDataEx data;
        if (params.length > 0) {
            data = params[0];
            // 无论 params[0] 是否等于 null，都用这个当初始数据，不用再去文件加载了
            if (allowLog) {
                printDebugLog("Use init data: " + StringUtils.objToString(data));
            }
        } else {
            data = loadFromPersistent();
            if (allowLog) {
                printDebugLog("Load from file: " + StringUtils.objToString(data));
            }
        }
        //
        // 有网吗？
        if (!this.arguments.netTypeDetector.isConnected()) {
            if (allowLog) {
                printDebugLog("No network connection");
            }
            return data;
        }
        //
        // 过期了吗？
        boolean isLocalDataValid = isVersionValid(data);
        if (isLocalDataValid) {
            if (System.currentTimeMillis() < data.getExpireTime()) {
                if (allowLog) {
                    printDebugLog("Data not expired");
                }
                return data;
            }
        }
        //
        // 去网络下载
        if (allowLog) {
            printDebugLog("Try download from network ...");
        }
        String cacheTagFromResponse;
        long expireTimeFromResponse;
        Http.Response response;
        try {
            Http http = new Http(CONNECT_TIMEOUT, RECEIVE_TIMEOUT);
            HttpURLConnection conn = http.createHttpUrlConnection(buildUrl(), Http.Method.GET, Http.ContentType.JSON.str);
            Http.setRequestAccept(conn, this.getHttpAcceptType());
            if (isLocalDataValid) {
                // 仅当版本号相符时，才使用CacheTAG
                conn.setRequestProperty(Http.CACHE_IF_NONE_MATCH, data.getCacheTag());
            }
            response = Http.doGet(conn);
            cacheTagFromResponse = conn.getHeaderField("ETag");
            expireTimeFromResponse = parseExpireTimeFromHttpResponse(conn);
        } catch (IOException e) {
            printWarningLog(e.getMessage());
            return data;
        } catch (RuntimeException e) {
            printWarningLog(e.getMessage());
            return data;
        }
        //
        switch (response.code) {
        case HttpURLConnection.HTTP_OK:
            PortalDataEx downloadData = new PortalDataEx(cacheTagFromResponse, expireTimeFromResponse, this.arguments.version, response.data, true);
            if (checkDownloadData(downloadData)) {
                data = downloadData;
                if (allowLog) {
                    printDebugLog("Serialize download data " + downloadData);
                }
                saveDataToLocal(data);
            } else {
                printDebugLog("Invalid download data " + downloadData);
            }
            break;
        case HttpURLConnection.HTTP_NOT_MODIFIED:
            if (allowLog) {
                printDebugLog("Portal data not modified.");
            }
            if (isLocalDataValid) {
                data.setExpireTime(expireTimeFromResponse);
                saveDataToLocal(data);
            }
            break;
        case HttpURLConnection.HTTP_NOT_FOUND:
            if (allowLog) {
                printDebugLog("Response 404 not found, remove local cache.");
            }
            deletePersistent();
            break;
        default:
            if (allowLog) {
                printDebugLog("Server response: " + response.code);
            }
            break;
        }
        return data;
    }

    private void saveDataToLocal(PortalDataEx data) {
        if (isDebugLogAllowedNow()) {
            Calendar calendar = CalendarUtils.calendarLocal_FromMilliseconds(data.getExpireTime());
            printDebugLog("Save data, expire time: " +
                CalendarUtils.calendarToString(calendar,
                    CalendarUtils.FORMAT_DATE | CalendarUtils.FORMAT_TIME | CalendarUtils.FORMAT_ZONE));
        }
        try {
            data.serialize(getPersistent().openOutput());
        } catch (IOException e) {
            printWarningLog(e.getMessage());
        }
    }

    /**
     * 执行Protal参数下载的一些必要参数
     */
    public static abstract class Arguments extends HttpArguments {

        /**
         * @param clientType      客户端类型，用于构造数据下载的URL，如果是APP，应是常量"android"，如果是SDK，应该是游戏的GUID
         * @param version         本程序（或SDK）的版本号
         * @param serviceLocation {@link ServiceLocation} 服务地址。如果为null则使用缺省值。
         * @param netTypeDetector 用于判断当前网络类型
         * @see Defines#REQUEST_CLIENT_TYPE_FOR_APP
         */
        public Arguments(String clientType, String version, ServiceLocation serviceLocation, NetTypeDetector netTypeDetector) {
            super(clientType, version, adjustServiceLocation(serviceLocation), netTypeDetector);
        }

        private static ServiceLocation adjustServiceLocation(ServiceLocation serviceLocation) {
            if (serviceLocation == null) {
                serviceLocation = new ServiceLocation("http", Address.EndPoint.PORTAL.host, Address.EndPoint.PORTAL.port);
            }
            return serviceLocation;
        }

        /**
         * 由派生类实现：根据指定的文件名构造一个{@link Persistent}对象
         *
         * @param filename 文件名
         * @return {@link Persistent}
         */
        public abstract Persistent createPersistent(String filename);

    }

}
