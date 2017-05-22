package com.subao.upgrade;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.data.AccelNodesDownloader;
import com.subao.common.data.PortalDataDownloader;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;
import com.subao.common.data.PortalDataEx;

/**
 * 升级信息
 */
public class PortalUpgradeConfig extends PortalDataDownloader {

    private static final String FIELD_NAME_ITEMS = "info";
    private static final String FIELD_NAME_MIN_VER = "min_ver";

    /**
     * 版本信息
     */
    public static class Item implements JsonSerializable {

        public static class Builder {
            private String channel;
            private int verCode;
            private String version;
            private String url;
            private int size;
            private String instructions;
            private String publishTime;
            private String md5;

            public Item build() {
                return new Item(channel, verCode, version, url, size, instructions, publishTime, md5);
            }

            public void setChannel(String channel) {
                this.channel = channel;
            }

            public void setVerCode(int verCode) {
                this.verCode = verCode;
            }

            public void setVersion(String version) {
                this.version = version;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public void setSize(int size) {
                this.size = size;
            }

            public void setInstructions(String instructions) {
                this.instructions = instructions;
            }

            public void setPublishTime(String publishTime) {
                this.publishTime = publishTime;
            }

            public void setMD5(String md5) {
                this.md5 = md5;
            }
        }

        public static final String CHANNEL_ALL = "*";

        private static final String KEY_URL = "url";
        private static final String KEY_SIZE = "size";
        private static final String KEY_INSTRUCTIONS = "instructions";
        private static final String KEY_PUBLISH_TIME = "publish_time";
        private static final String KEY_MD5 = "md5";

        public final String channel;
        public final int verCode;
        public final String version;
        public final String url;
        public final int size;
        public final String instructions;
        public final String publishTime;
        public final String md5;

        public Item(String channel, int verCode, String version, String url, int size, String instructions, String publishTime, String md5) {
            if (channel == null || version == null || url == null) {
                throw new NullPointerException();
            }
            if (channel.length() == 0 || version.length() == 0 || url.length() == 0) {
                throw new IllegalArgumentException();
            }
            this.channel = channel;
            this.verCode = verCode;
            this.version = version;
            this.url = url;
            this.size = size;
            this.instructions = instructions;
            this.publishTime = publishTime;
            this.md5 = md5;
        }

        @Override
        public void serialize(JsonWriter jsonWriter) throws IOException {
            jsonWriter.beginObject();
            JsonUtils.writeString(jsonWriter, Defines.JSON_NAME_CHANNEL, channel);
            jsonWriter.name(Defines.JSON_NAME_VERCODE).value(verCode);
            JsonUtils.writeString(jsonWriter, Defines.JSON_NAME_VERSION, version);
            JsonUtils.writeString(jsonWriter, KEY_URL, url);
            jsonWriter.name(KEY_SIZE).value(size);
            JsonUtils.writeString(jsonWriter, KEY_INSTRUCTIONS, instructions);
            JsonUtils.writeString(jsonWriter, KEY_PUBLISH_TIME, publishTime);
            JsonUtils.writeString(jsonWriter, KEY_MD5, md5);
            jsonWriter.endObject();
        }

        public static Item createFromJson(JsonReader reader) throws IOException {
            try {
                String channel = null;
                int verCode = 0;
                String version = null;
                String url = null;
                int size = 0;
                String instructions = null;
                String publishTime = null;
                String md5 = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (Defines.JSON_NAME_CHANNEL.equals(name)) {
                        channel = reader.nextString();
                    } else if (Defines.JSON_NAME_VERCODE.equals(name)) {
                        verCode = reader.nextInt();
                    } else if (Defines.JSON_NAME_VERSION.equals(name)) {
                        version = reader.nextString();
                    } else if (KEY_URL.equals(name)) {
                        url = reader.nextString();
                    } else if (KEY_SIZE.equals(name)) {
                        size = reader.nextInt();
                    } else if (KEY_INSTRUCTIONS.equals(name)) {
                        instructions = JsonUtils.readNextString(reader);
                    } else if (KEY_PUBLISH_TIME.equals(name)) {
                        publishTime = JsonUtils.readNextString(reader);
                    } else if (KEY_MD5.equals(name)) {
                        md5 = JsonUtils.readNextString(reader);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
                return new Item(channel, verCode, version, url, size, instructions, publishTime, md5);
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (null == o) {
                return false;
            }
            if (!(o instanceof Item)) {
                return false;
            }
            Item other = (Item) o;
            return this.verCode == other.verCode
                && this.size == other.size
                && Misc.isEquals(this.channel, other.channel)
                && Misc.isEquals(this.version, other.version)
                && Misc.isEquals(this.url, other.url)
                && Misc.isEquals(this.instructions, other.instructions)
                && Misc.isEquals(this.publishTime, other.publishTime)
                && Misc.isEquals(this.md5, other.md5);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("[VerCode: %d, Channel: %s]", this.verCode, this.channel);
        }

        /**
         * 比较两个{@link Item}，比较规则为Channel+VerCode，按以下顺序：
         * <ul>
         * <li>channel字段为"*"的表示适用于所有渠道，它小于channel不为"*"的其它{@link Item}</li>
         * <li>相同channel的，verCode较大的{@link Item}较大</li>
         * </ul>
         *
         * @param another 之本Item比较的另一个{@link Item}
         * @return 负数表示本Item较小，正数表示本Item较大，零表示相等
         */
        public int compareByChannelAndVerCode(Item another) {
            if (another == null) {
                return 1;
            }
            if (this == another) {
                return 0;
            }
            if (this.isChannelWildcard()) {
                if (another.isChannelWildcard()) {
                    return this.verCode - another.verCode;
                } else {
                    return -1;
                }
            } else {
                if (another.isChannelWildcard()) {
                    return 1;
                } else {
                    return this.verCode - another.verCode;
                }
            }
        }

        /**
         * 本Item是否适合于所有渠道？（Chanel字段的值为CHANNEL_ALL）
         */
        public boolean isChannelWildcard() {
            return CHANNEL_ALL.equals(this.channel);
        }
    }

    /**
     * {@link Item}组
     */
    public static class Items {

        /**
         * 正常的、未被用户标记为“需忽略”的{@link Item}
         */
        public final Item normal;

        /**
         * 被用户标记为“需要忽略”的{@link Item}
         */
        public final Item ignored;

        public Items(Item normal, Item ignored) {
            this.normal = normal;
            this.ignored = ignored;
        }
    }

    static class PortalUpgradeData implements JsonSerializable {

        private final int minVer;
        final List<Item> items;

        public PortalUpgradeData(int minVer, List<Item> items) {
            this.minVer = minVer;
            this.items = items;
        }

        public int getMinVer() {
            return minVer;
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            {
                writer.name(FIELD_NAME_MIN_VER).value(this.minVer);
                if (this.items != null && !this.items.isEmpty()) {
                    writer.name(FIELD_NAME_ITEMS);
                    writer.beginArray();
                    for (Item item : items) {
                        item.serialize(writer);
                    }
                    writer.endArray();
                }
            }
            writer.endObject();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof PortalUpgradeData)) {
                return false;
            }
            PortalUpgradeData other = (PortalUpgradeData) o;
            return this.minVer == other.minVer
                && Misc.isEquals(this.items, other.items);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("[minVer=%d, items=%s]",
                this.minVer,
                this.items == null ? "null" : Integer.toString(this.items.size()));
        }

        /**
         * 查找一个适合给定Channel和VersionCode的{@link Item}
         *
         * @param channel               APP的渠道名
         * @param versionCode           APP的当前VersionCode
         * @param ignoreVersionCodeList 要忽略的版本列表
         * @return null表示未找到，否则为一个最合适的{@link Item}
         */
        public Items find(String channel, int versionCode, IgnoreVersionCodeList ignoreVersionCodeList) {
            if (this.items == null) {
                return null;
            }
            Item normal = null, ignored = null;
            for (Item item : this.items) {
                if (item.verCode <= versionCode) {
                    // 版本号小于等于当前版本的，跳过
                    continue;
                }
                if (!item.channel.equals(channel) && !item.isChannelWildcard()) {
                    // 渠道名不符、且也不是通配渠道，跳过
                    continue;
                }
                if (ignoreVersionCodeList != null) {
                    if (versionCode >= minVer && ignoreVersionCodeList.needIgnore(item.verCode)) {
                        // 如果在忽略列表里，且客户端当前版本不低于minVer
                        if (item.compareByChannelAndVerCode(ignored) > 0) {
                            ignored = item;
                        }
                        continue;
                    }
                }
                if (item.compareByChannelAndVerCode(normal) > 0) {
                    normal = item;
                }
            }
            if (normal == null && ignored == null) {
                return null;
            }
            return new Items(normal, ignored);
        }
    }

    static class DataCreator {

        public static PortalUpgradeData deserializePortalData(InputStream inputStream) throws IOException {
            try {
                PortalUpgradeData content = null;
                JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
                try {
                    content = loadContent(reader);
                } finally {
                    Misc.close(reader);
                }
                if (content == null) {
                    throw new IOException("No content data found");
                }
                return content;
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage());
            }
        }

        public static PortalUpgradeData createPortalData(byte[] bytes) throws IOException {
            if (bytes == null || bytes.length <= 0) {
                return null;
            }

            JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
            try {
                return loadContent(reader);
            } finally {
                Misc.close(reader);
            }
        }

        private static List<Item> loadItems(JsonReader reader) throws IOException {
            List<Item> items = new ArrayList<Item>(4);
            reader.beginArray();
            while (reader.hasNext()) {
                Item item = Item.createFromJson(reader);
                items.add(item);
            }
            reader.endArray();
            return items;
        }

        private static PortalUpgradeData loadContent(JsonReader reader) throws IOException {
            int minVer = 0;
            List<Item> items = null;
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (FIELD_NAME_MIN_VER.equals(name)) {
                        minVer = reader.nextInt();
                    } else if (FIELD_NAME_ITEMS.equals(name)) {
                        items = loadItems(reader);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage());
            }
            return new PortalUpgradeData(minVer, items);
        }

    }

    /**
     * 启动下载线程，并返回本地缓存数据
     * <p><b>（仅当缓存文件的版本号正确时才使用本地缓存数据）</b></p>
     *
     * @param arguments 参数
     * @return {@link AccelNodesDownloader.NodesInfo}
     */
    public static void start(Arguments arguments) {
        PortalUpgradeConfig downloader = new PortalUpgradeConfig(arguments);
        PortalDataEx localData = downloader.loadFromPersistent();
        downloader.executeOnExecutor(ThreadPool.getExecutor(), localData);
    }

    protected PortalUpgradeConfig(Arguments arguments) {
        super(arguments);
    }

    @Override
    protected String getId() {
        return "PortalUpgradeConfig";
    }

    @Override
    protected String getUrlPart() {
       return "versions";
    }
}
