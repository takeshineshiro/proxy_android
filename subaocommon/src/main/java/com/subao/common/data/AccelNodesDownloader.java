package com.subao.common.data;

import android.util.JsonReader;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 加速节点列表下载器
 * <p>Created by YinHaiBo on 2017/2/17.</p>
 */

public class AccelNodesDownloader extends PortalDataDownloader {

    private static final String TAG = LogTag.DATA;

    protected AccelNodesDownloader(Arguments arguments) {
        super(arguments);
    }

    /**
     * 启动下载线程，并返回本地缓存数据
     * <p><b>（仅当缓存文件的版本号正确时才使用）</b></p>
     *
     * @param arguments 参数
     * @return {@link NodesInfo}
     */
    public static NodesInfo start(Arguments arguments) {
        AccelNodesDownloader downloader = new AccelNodesDownloader(arguments);
        PortalDataEx localData = downloader.loadFromPersistent();
        downloader.executeOnExecutor(ThreadPool.getExecutor(), localData);
        if (downloader.isVersionValid(localData)) {
            return extractDataForJNI(localData);
        } else {
            return new NodesInfo(0, null);
        }
    }

    private static NodesInfo transFormatForJNI(JsonReader reader) throws IOException {
        int jsonArrayCount = 0, actualCount = 0;
        StringBuilder sb = new StringBuilder(12 * 1024);
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            String id = null;
            String ip = null;
            String isp = null;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("id".equals(name)) {
                    id = JsonUtils.readNextString(reader);
                } else if ("ip".equals(name)) {
                    ip = JsonUtils.readNextString(reader);
                } else if ("isp".equals(name)) {
                    isp = JsonUtils.readNextString(reader);
                } else {
                    reader.skipValue();
                }
            }
            ++jsonArrayCount;
            reader.endObject();
            if (id != null && ip != null && ip.length() >= 7 && isp != null && isp.length() != 0) {
                ++actualCount;
                sb.append(id).append(':').append(ip);
                String[] ispList = isp.split(",");
                for (String s : ispList) {
                    sb.append(':').append(s);
                }
                sb.append(',');
            }
        }
        reader.endArray();
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, String.format("Parse nodes from json: %d / %d", actualCount, jsonArrayCount));
        }
        return new NodesInfo(actualCount, sb.toString());
    }

    /**
     * 从给定的{@link PortalDataEx}里解析出{@link NodesInfo}
     *
     * @param data 给定的{@link PortalDataEx}
     * @return null或 {@link NodesInfo}
     */
    static NodesInfo extractDataForJNI(PortalDataEx data) {
        if (data == null) {
            return null;
        }
        byte[] bytes = data.getData();
        if (bytes == null || bytes.length < 8) {
            return null;
        }
        NodesInfo result = null;
        JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        try {
            result = transFormatForJNI(jsonReader);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            Misc.close(jsonReader);
        }
        return result;
    }

    @Override
    protected String getUrlPart() {
        return "nodes";
    }

    @Override
    protected String getId() {
        return "AccelNodes";
    }

    @Override
    protected boolean checkDownloadData(PortalDataEx data) {
        return data != null && data.getDataSize() > 16;
    }

    public static class NodesInfo {
        public final int count;
        public final String dataForJNI;

        public NodesInfo(int count, String dataForJNI) {
            this.count = count;
            this.dataForJNI = dataForJNI;
        }

        @Override
        public String toString() {
            return String.format("[Accel Nodes %d]", count);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (!(o instanceof NodesInfo)) {
                return false;
            }
            NodesInfo other = (NodesInfo) o;
            return this.count == other.count
                && Misc.isEquals(this.dataForJNI, other.dataForJNI);
        }
    }
}
