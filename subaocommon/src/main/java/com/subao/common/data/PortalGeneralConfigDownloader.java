package com.subao.common.data;

import android.text.TextUtils;

import com.subao.common.jni.JniWrapper;

/**
 * Portal上面General键值对数据的下载和处理器
 * <p>Created by YinHaiBo on 2017/2/16.</p>
 */

public class PortalGeneralConfigDownloader extends PortalKeyValuesDownloader {

    private final JniWrapper jniWrapper;

    /**
     * 构造函数
     *
     * @param arguments  {@link Arguments}
     * @param jniWrapper {@link JniWrapper}
     */
    protected PortalGeneralConfigDownloader(Arguments arguments, JniWrapper jniWrapper) {
        super(arguments);
        this.jniWrapper = jniWrapper;
    }

    /**
     * 启动处理工作，具体逻辑如下：
     * <ol>
     * <li>加载本地数据</li>
     * <li>如果本地数据不为null，则先处理一下（针对数据里的每一个键值对，调用{@link #process(String, String)}方法）</li>
     * <li>启动工作线程去网上下载</li>
     * <li>如果从网上下载到了新的数据（HTTP返回200），则再用新的数据重新处理下（{@link #process(String, String)}）</li>
     * </ol>
     */
    public static void start(Arguments arguments, JniWrapper jniWrapper) {
        PortalGeneralConfigDownloader downloader = new PortalGeneralConfigDownloader(arguments, jniWrapper);
        PortalKeyValuesDownloader.start(downloader);
    }

    @Override
    protected void process(String name, String value) {
        if (!TextUtils.isEmpty(name)) {
            jniWrapper.defineConst(name, value);
        }
    }

    @Override
    protected String getId() {
        return "general";
    }

    @Override
    protected String getUrlPart() {
        return "configs/general";
    }

}
