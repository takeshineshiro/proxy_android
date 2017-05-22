package com.subao.common.data;

import android.util.JsonReader;

import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 负责下载Portal键值对配置数据的Downloader
 * <p>Created by YinHaiBo on 2017/2/16.</p>
 */

public abstract class PortalKeyValuesDownloader extends PortalDataDownloader {

    protected PortalKeyValuesDownloader(Arguments arguments) {
        super(arguments);
    }

    /**
     * 启动处理工作，具体逻辑如下：
     * <ol>
     * <li>加载本地数据</li>
     * <li>如果本地数据不为null，则先处理一下（针对数据里的每一个键值对，调用{@link #process(String, String)}方法）</li>
     * <li>启动工作线程去网上下载</li>
     * <li>如果从网上下载到了新的数据（HTTP返回200），则再用新的数据重新处理下（{@link #process(String, String)}）</li>
     * </ol>
     *
     * @param downloader
     */
    public static void start(PortalKeyValuesDownloader downloader) {
        PortalDataEx localData = downloader.loadFromPersistent(); // 先加载本地数据
        if (localData != null) {
            if (downloader.isVersionValid(localData)) {
                // 如果本地缓存数据的版本号，和本客户端版本号一致，则先用本地数据搞一搞
                downloader.processPortalData(localData);
            } else {
                // 版本不符，不要用缓存的CacheTag了，重新从网上下载
                localData = null;
            }
        }
        downloader.executeOnExecutor(ThreadPool.getExecutor(), localData); // 再启动工作线程
    }

    @Override
    protected void onPostExecute(PortalDataEx portalDataEx) {
        super.onPostExecute(portalDataEx);
        if (portalDataEx != null && portalDataEx.isNewByDownload) {
            // 如果是新下载的，重新再搞一下
            processPortalData(portalDataEx);
        }
    }

    /**
     * 当所有的键值对都被遍历处理完以后调用，派生类可借此时机做些事情
     * <p><b>注意此函数可能在任何线程被调用</b></p>
     */
    protected void onAllProcessed() {
    }

    /**
     * 处理指定的{@link PortalDataEx}
     *
     * @param portalData
     */
    private void processPortalData(PortalDataEx portalData) {
        try {
            boolean allowLog = isDebugLogAllowedNow();
            if (portalData == null || portalData.getDataSize() <= 2) {
                if (allowLog) {
                    printDebugLog("config is null");
                }
                return;
            }
            JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(portalData.data)));
            try {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    String value = JsonUtils.readNextString(jsonReader);
                    if (allowLog) {
                        printDebugLog(String.format("process \"%s\":\"%s\"", name, value));
                    }
                    process(name, value);
                }
                jsonReader.endObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        } finally {
            onAllProcessed();
        }
    }

    /**
     * 派生类实现，处理数据里的键值对
     *
     * @param name  字段名
     * @param value 字段值
     */
    protected abstract void process(String name, String value);

}
