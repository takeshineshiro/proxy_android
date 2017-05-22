package com.subao.common.data;

import android.text.TextUtils;
import android.util.JsonReader;

import com.subao.common.Misc;
import com.subao.common.jni.JniWrapper;
import com.subao.common.thread.ThreadPool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 汇聚节点列表
 * <p>Created by YinHaiBo on 2017/2/3.</p>
 */

public class ConvergenceNodesDownloader extends PortalDataDownloader {

    private final JniWrapper jniWrapper;

    protected ConvergenceNodesDownloader(Arguments arguments, JniWrapper jniWrapper) {
        super(arguments);
        this.jniWrapper = jniWrapper;
    }

    /**
     * 创建一个{@link ConvergenceNodesDownloader}实例，加载本地数据，并启动工作线程去网上下载
     *
     * @return 返回当前本地缓存的汇聚节点列表数据（可直接传递给JNI的格式），或null
     */
    public static String start(Arguments arguments, JniWrapper jniWrapper) {
        ConvergenceNodesDownloader downloader = new ConvergenceNodesDownloader(arguments, jniWrapper);
        PortalDataEx localData = downloader.loadFromPersistent();
        downloader.executeOnExecutor(ThreadPool.getExecutor(), localData);
        if (downloader.isVersionValid(localData)) {
            return extractDataContent(localData);
        } else {
            return null;
        }
    }

    @Override
    protected PortalDataEx doInBackground(PortalDataEx... params) {
        PortalDataEx result = super.doInBackground(params);
        if (result != null && result.isNewByDownload) {
            jniWrapper.setString(0, Defines.VPNJniStrKey.KEY_CONVERGENCE_NODE, extractDataContent(result));
        }
        return result;
    }

    /**
     * 从给定的{@link PortalDataEx}里解析出内容
     *
     * @param portalData {@link PortalDataEx}，可以为null
     * @return 解析出来的汇聚节点内容，或null
     */
    private static String extractDataContent(PortalDataEx portalData) {
        if (portalData == null) {
            return null;
        }
        byte[] data = portalData.getData();
        if (data == null || data.length <= 2) {
            return null;
        }
        JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(data)));
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("list".equals(name)) {
                    String result = reader.nextString();
                    if (!TextUtils.isEmpty(result)) {
                        if (result.charAt(result.length() - 1) != ',') {
                            result += ',';
                        }
                    }
                    return result;
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            Misc.close(reader);
        }
        return null;
    }

    @Override
    protected String getId() {
        return "convergence";
    }

    @Override
    protected String getUrlPart() {
        return "configs/convergence";
    }

}
