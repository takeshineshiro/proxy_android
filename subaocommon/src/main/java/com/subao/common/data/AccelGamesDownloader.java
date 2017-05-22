package com.subao.common.data;

import android.util.JsonReader;

import com.subao.common.Misc;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责从Portal下载可加速（支持）游戏列表的下载器
 * <p>Created by YinHaiBo on 2017/3/27.</p>
 */
public class AccelGamesDownloader extends PortalDataDownloader {

    /**
     * 预计有多少款游戏
     * <p>对于迅游手游加速器APP，有超过15000款游戏。设置此值为合适的预期值，有助于容器效率的提升</p>
     */
    private final int capacityExpected;
    private final Listener listener;

    /**
     * 构造函数
     *
     * @param arguments        {@link Arguments} 相关的必要参数
     * @param capacityExpected 预计有多少款游戏
     * @param listener         侦听器
     */
    protected AccelGamesDownloader(Arguments arguments, int capacityExpected, Listener listener) {
        super(arguments);
        this.capacityExpected = capacityExpected;
        this.listener = listener;
    }

    /**
     * 启动下载线程，并返回本地缓存的、或缺省的游戏列表
     * <p><b>（仅当缓存文件的版本号正确时才使用本地缓存数据）</b></p>
     *
     * @param arguments                  参数
     * @param capacityExpected           容量。对于迅游手游加速器APP，有超过15000款游戏。设置此值为合适的预期值，有助于容器效率的提升。此值用于List对象创建时的初始容量参数
     * @param listener                   侦听器
     * @param jsonOfDefaultAccelGameList 缺省的，JSON格式的支持游戏列表。如果本地缓存数据不存在则对此数据进行解析并返回
     * @return {@link AccelNodesDownloader.NodesInfo}
     */
    public static List<AccelGame> start(
        Arguments arguments,
        int capacityExpected,
        Listener listener,
        byte[] jsonOfDefaultAccelGameList
    ) {
        AccelGamesDownloader downloader = new AccelGamesDownloader(arguments, capacityExpected, listener);
        PortalDataEx localData = downloader.loadFromPersistent();
        downloader.executeOnExecutor(ThreadPool.getExecutor(), localData);
        List<AccelGame> result = extractAccelGameListFromPortalData(localData, capacityExpected);
        if (result == null) {
            result = extractAccelGameListFromJson(jsonOfDefaultAccelGameList, capacityExpected);
        }
        return result;
    }

    /**
     * 从给定的{@link PortalDataEx}里解析出加速游戏加表
     *
     * @param data             {@link PortalDataEx}
     * @param capacityExpected 预期的容量（游戏个数）
     * @return null（比如data为空、data.getData()为空等）或一个 {@link AccelGame} 列表
     */
    private static List<AccelGame> extractAccelGameListFromPortalData(PortalDataEx data, int capacityExpected) {
        if (data == null || data.getData() == null) {
            return null;
        }
        return extractAccelGameListFromJson(data.getData(), capacityExpected);
    }

    /**
     * 从给定的JSON数据里解析出加速游戏加列表
     *
     * @param json             格式与Portal格式一致的JSON数据
     * @param capacityExpected 预期的容量（游戏个数）
     * @return null或一个 {@link AccelGame} 的列表
     */
    private static List<AccelGame> extractAccelGameListFromJson(byte[] json, int capacityExpected) {
        if (json != null && json.length > 2) {
            InputStream input = new ByteArrayInputStream(json);
            try {
                return JsonParser.parse(input, capacityExpected);
            } catch (IOException e) {
                // fall
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(PortalDataEx portalDataEx) {
        super.onPostExecute(portalDataEx);
        if (listener != null && portalDataEx != null && portalDataEx.isNewByDownload) {
            List<AccelGame> list = extractAccelGameListFromPortalData(portalDataEx, capacityExpected);
            if (list != null) {
                listener.onAccelGameListDownload(list);
            }
        }
    }

    @Override
    protected String getUrlPart() {
        return "games";
    }

    @Override
    protected String getId() {
        return "AccelGames";
    }

    /**
     * 侦听游戏列表发生改变（从本地加载到新的，或从网络下载到新的）
     */
    public interface Listener {

        /**
         * 当新的游戏列表下载到时
         *
         * @param list 最新的游戏列表
         */
        void onAccelGameListDownload(List<AccelGame> list);
    }

    static class JsonParser {

        private static final String KEY_APP_LABEL = "appLabel";
        private static final String KEY_ACCEL_MODE = "accelMode";
        private static final String KEY_BIT_FLAG = "bitFlag";
        private static final String KEY_BLACK_PORTS = "blackPorts";
        private static final String KEY_WHITE_PORTS = "whitePorts";
        private static final String KEY_BLACK_IPS = "blackIps";
        private static final String KEY_WHITE_IPS = "whiteIps";
        private static final String KEY_START = "start";
        private static final String KEY_END = "end";

        private JsonParser() {
        }

        public static List<AccelGame> parse(InputStream input, int capacity) throws IOException {
            List<AccelGame> list = null;
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if ("gameList".equals(name)) {
                        list = parseGameList(reader, capacity);
                        break;
                    } else {
                        reader.skipValue();
                    }
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw new IOException();
            } finally {
                Misc.close(reader);
            }
            return list;
        }

        private static List<AccelGame> parseGameList(JsonReader reader, int capacity) throws IOException {
            List<AccelGame> list = new ArrayList<AccelGame>(capacity);
            reader.beginArray();
            while (reader.hasNext()) {
                AccelGame accelGame = parseGame(reader);
                if (accelGame != null) {
                    list.add(accelGame);
                }
            }
            reader.endArray();
            return list;
        }

        private static AccelGame parseGame(JsonReader reader) throws IOException {
            String appLabel = null;
            int accelMode = 1;
            int flags = 0;
            List<AccelGame.PortRange> whitePorts = null;
            List<AccelGame.PortRange> blackPorts = null;
            List<String> blackIps = null;
            List<String> whiteIps = null;
            //
            reader.beginObject();
            while (reader.hasNext()) {
                String game = reader.nextName();
                if (KEY_APP_LABEL.equals(game)) {
                    appLabel = JsonUtils.readNextString(reader);
                } else if (KEY_ACCEL_MODE.equals(game)) {
                    accelMode = reader.nextInt();
                } else if (KEY_BIT_FLAG.equals(game)) {
                    flags = reader.nextInt();
                } else if (KEY_WHITE_PORTS.equals(game)) {
                    whitePorts = parsePorts(reader);
                } else if (KEY_BLACK_PORTS.equals(game)) {
                    blackPorts = parsePorts(reader);
                } else if (KEY_BLACK_IPS.equals(game)) {
                    blackIps = parseIpList(reader);
                } else if (KEY_WHITE_IPS.equals(game)) {
                    whiteIps = parseIpList(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return AccelGame.create(appLabel, accelMode, flags, whitePorts, blackPorts, whiteIps, blackIps);
        }

        private static List<String> parseIpList(JsonReader reader) throws IOException {
            ArrayList<String> result = null;
            reader.beginArray();
            while (reader.hasNext()) {
                String ip = JsonUtils.readNextString(reader);
                if (ip != null) {
                    if (result == null) {
                        result = new ArrayList<String>();
                    }
                    result.add(ip);
                }
            }
            reader.endArray();
            return result;
        }

        private static List<AccelGame.PortRange> parsePorts(JsonReader reader) throws IOException {
            ArrayList<AccelGame.PortRange> result = null;
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                Integer start = null;
                Integer end = null;
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (KEY_START.equals(name)) {
                        start = reader.nextInt();
                    } else if (KEY_END.equals(name)) {
                        end = reader.nextInt();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
                if (start != null && end != null) {
                    AccelGame.PortRange portRange = new AccelGame.PortRange(start, end);
                    if (result == null) {
                        result = new ArrayList<AccelGame.PortRange>(8);
                    }
                    result.add(portRange);
                }
            }
            reader.endArray();
            return result;
        }
    }
}
