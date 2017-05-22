package com.subao.common.data;

import android.os.Environment;
import android.text.TextUtils;
import android.util.JsonReader;

import com.subao.common.utils.JsonUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * 测试人员对所有服务资源均可配置项，包括（不限于）：
 * <ul>
 * <li>Portal的访问地址</li>
 * <li>鉴权四大接口的URL</li>
 * <li>SDK H5的URL</li>
 * <li>测速节点列表</li>
 * <li>HR接口的URL</li>
 * </ul>
 * 所有字段，如果为null则表示测试人员留空未配
 * <p>Created by YinHaiBo on 2016/11/23.</p>
 */

public class ServiceConfig {

    private static final String FILE_NAME_PREFIX = "com.subao.gamemaster.service.config.";

    boolean initAlwaysFail;
    Integer logLevel;
    AccelNodesDownloader.NodesInfo nodesInfo;
    String urlH5;
    ServiceLocation portalServiceLocation;
    ServiceLocation authServiceLocation;
    ServiceLocation messageServiceLocation;
    ServiceLocation hrServiceLocation;
    Integer accelRecommendation;

    public static int validLogLevel(int logLevel) {
        if (logLevel < 1) {
            logLevel = 1;
        } else if (logLevel > 5) {
            logLevel = 5;
        }
        return logLevel;
    }

    /**
     * 返回文件存放的缺省目录
     *
     * @return 文件存放的缺省目录
     */
    static File getDefaultDirectory() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    /**
     * 构造一个指向测试配置文件的{@link File}对象
     *
     * @param dir   目录。如果为null，则使用缺省目录
     * @param isSDK true表示用于SDK，false表示用于APP
     * @return {@link File}
     * @see #getDefaultDirectory()
     */
    public static File createFile(File dir, boolean isSDK) {
        if (dir == null) {
            dir = getDefaultDirectory();
        }
        dir.mkdirs();
        return new File(dir, getFilename(isSDK));
    }

    private static String getFilename(boolean isSDK) {
        return FILE_NAME_PREFIX + (isSDK ? "sdk" : "app");
    }

    private static File findFile(File dir, boolean isSDK) {
        if (dir == null) {
            dir = getDefaultDirectory();
        }
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        File file = new File(dir, getFilename(isSDK));
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        return file;
    }


    /**
     * 从特定的目录中加载测试人员生成的配置文件
     *
     * @param dir   目录。如果指定null表示使用缺省目录
     * @param isSDK true表示SDK使用的文件，false表示APP使用的文件
     * @return true表示成功，否则返回false
     */

    public boolean loadFromFile(File dir, boolean isSDK) {
        try {
            File file = findFile(dir, isSDK);
            if (file != null) {
                loadFromReader(new BufferedReader(new FileReader(file), 2048));
                return true;
            }
        } catch (IOException e) {
        } catch (RuntimeException e) {
        }
        return false;
    }

    void loadFromReader(Reader reader) throws IOException {
        JsonReader jsonReader = new JsonReader(reader);
        try {
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if ("init".equals(name)) {
                    initAlwaysFail = "fail".equals(JsonUtils.readNextString(jsonReader));
                } else if ("url_h5".equals(name)) {
                    urlH5 = JsonUtils.readNextString(jsonReader);
                } else if ("accel_recommend".equals(name)) {
                    accelRecommendation = jsonReader.nextInt();
                } else if ("nodes_info".equals(name)) {
                    this.nodesInfo = parseNodesInfo(JsonUtils.readNextString(jsonReader));
                } else if ("log_level".equals(name)) {
                    logLevel = validLogLevel(jsonReader.nextInt());
                } else if ("url_portal".equals(name)) {
                    this.portalServiceLocation = ServiceLocation.parse(JsonUtils.readNextString(jsonReader));
                } else if ("url_auth".equals(name)) {
                    this.authServiceLocation = ServiceLocation.parse(JsonUtils.readNextString(jsonReader));
                } else if ("url_hr".equals(name)) {
                    this.hrServiceLocation  =  ServiceLocation.parse(JsonUtils.readNextString(jsonReader));
                } else if ("url_message".equals(name)) {
                    this.messageServiceLocation = ServiceLocation.parse(JsonUtils.readNextString(jsonReader));
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
        } finally {
            com.subao.common.Misc.close(jsonReader);
        }
    }

    private AccelNodesDownloader.NodesInfo parseNodesInfo(String text) {
        if (TextUtils.isEmpty(text)) {
            return new AccelNodesDownloader.NodesInfo(0, null);
        }
        int count = 0;
        for (int start = 0; ;) {
            int pos = text.indexOf(',', start);
            if (pos < 0) {
                break;
            }
            ++count;
            start = pos + 1;
        }
        return new AccelNodesDownloader.NodesInfo(count, text);
    }

    /**
     * 为了测试初始化失败，此项返回为True的时候，SDK和APP的初始化返回失败
     */
    public boolean isInitAlwaysFail() {
        return initAlwaysFail;
    }

    /**
     * 如果测试人员配置了logLevel，则此值优先于VPNManager.sendSetLogLevel(int)}设置的值
     */
    public Integer getLogLevel() {
        return logLevel;
    }

    /**
     * 取测试人员配置的Portal服务资源位置
     *
     * @return null表示未配置
     */
    public ServiceLocation getPortalServiceLocation() {
        return portalServiceLocation;
    }

    /**
     * 取测试人员配置的加速节点列表
     *
     * @return null表示未配置
     */
    public AccelNodesDownloader.NodesInfo getNodesInfo() {
        return nodesInfo;
    }

    /**
     * 测速人员配置的“内嵌SDK的游戏使用的H5页面地址”（只包括问号前面的部分，如"https://api.xunyou.mobi/payments"）
     *
     * @return null表示未配置
     */
    public String getUrlH5() {
        return urlH5;
    }

    /**
     * 测试人员配置的“内嵌SDK的游戏询问推荐加速值时给出的值”
     *
     * @return null表示未配置
     */
    public Integer getAccelRecommendation() {
        return accelRecommendation;
    }


    /**
     * 测试人员配置的“HR请求资源位置”
     *
     * @return null表示未配置
     */

     public  ServiceLocation   getHrServiceLocation ()  { return   hrServiceLocation ;}

    /**
     * 返回测试人员配置的“鉴权请求的服务资源位置”
     *
     * @return null表示未配置
     */
    public ServiceLocation getAuthServiceLocation() {
        return authServiceLocation;
    }

    /**
     * 返回测试人员配置的“消息上报请求的服务资源位置”
     *
     * @return null表示未配置
     */
    public ServiceLocation getMessageServiceLocation() {
        return this.messageServiceLocation;
    }

}
