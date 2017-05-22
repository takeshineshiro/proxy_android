package com.subao.upgrade;

import android.annotation.SuppressLint;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;
import com.subao.common.io.Persistent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理“忽略的版本列表”
 */
public class IgnoreVersionCodeList implements JsonSerializable {

    private static final String JSON_NAME_LIST = "list";

    static class Item implements JsonSerializable {

        private final String channel;
        private final String versionName;
        private final int versionCode;

        public Item(String channel, String versionName, int versionCode) {
            this.channel = channel;
            this.versionCode = versionCode;
            this.versionName = versionName;
        }

        public String getChannel() {
            return channel;
        }

        public String getVersionName() {
            return versionName;
        }

        public int getVersionCode() {
            return versionCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (!(o instanceof Item)) {
                return false;
            }
            Item other = (Item) o;
            return this.versionCode == other.versionCode
                && Misc.isEquals(this.versionName, other.versionName)
                && Misc.isEquals(this.channel, other.channel);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("[VC=%d, VN=%s, C=%s]", this.versionCode, this.versionName, this.channel);
        }

        @Override
        public void serialize(JsonWriter jsonWriter) throws IOException {
            jsonWriter.beginObject();
            JsonUtils.writeString(jsonWriter, Defines.JSON_NAME_CHANNEL, this.channel);
            JsonUtils.writeString(jsonWriter, Defines.JSON_NAME_VERSION, this.versionName);
            jsonWriter.name(Defines.JSON_NAME_VERCODE).value(this.versionCode);
            jsonWriter.endObject();
        }

        public static Item createFromJson(JsonReader reader) throws IOException {
            String channel = null, versionName = null;
            int versionCode = 0;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (Defines.JSON_NAME_CHANNEL.equals(name)) {
                    channel = reader.nextString();
                } else if (Defines.JSON_NAME_VERSION.equals(name)) {
                    versionName = reader.nextString();
                } else if (Defines.JSON_NAME_VERCODE.equals(name)) {
                    versionCode = reader.nextInt();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return new Item(channel, versionName, versionCode);
        }
    }

    private final Persistent persistent;
    private List<Item> itemList;

    public IgnoreVersionCodeList(Persistent persistent) {
        this.persistent = persistent;
        loadItemList();
    }

    boolean loadItemList() {
        if (persistent == null) {
            return false;
        }
        itemList = null;
        JsonReader reader = null;
        try {
            reader = new JsonReader(new InputStreamReader(persistent.openInput()));
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (JSON_NAME_LIST.equals(name)) {
                    itemList = new ArrayList<Item>(4);
                    reader.beginArray();
                    while (reader.hasNext()) {
                        Item item = Item.createFromJson(reader);
                        itemList.add(item);
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            itemList = null;
        } catch (RuntimeException e) {
            itemList = null;
        } finally {
            Misc.close(reader);
        }
        return itemList != null;
    }

    public void add(String channel, String versionName, int versionCode) {
        if (itemList == null) {
            itemList = new ArrayList<Item>(4);
        }
        itemList.add(new Item(channel, versionName, versionCode));
        save();
    }

    public boolean isEmpty() {
        return itemList == null || itemList.isEmpty();
    }

    /**
     * 将所有小于或等于给定的App的VersionCode的Item都去掉
     *
     * @param appCurrentVersionCode 给定的APP的VersionCode。列表里所有小于或等于此值的Item都将被删除
     */
    public void remove(int appCurrentVersionCode) {
        if (this.itemList != null && !this.itemList.isEmpty()) {
            int oldSize = itemList.size();
            for (int i = itemList.size() - 1; i >= 0; --i) {
                Item item = itemList.get(i);
                if (item.versionCode <= appCurrentVersionCode) {
                    itemList.remove(i);
                }
            }
            if (oldSize != itemList.size()) {
                save();
            }
        }
    }

    /**
     * 保存
     */
    private void save() {
        if (persistent == null) {
            return;
        }
        if (itemList == null || itemList.isEmpty()) {
            persistent.delete();
            return;
        }
        JsonWriter writer = null;
        try {
            writer = new JsonWriter(new OutputStreamWriter(persistent.openOutput()));
            serialize(writer);
        } catch (RuntimeException e) {

        } catch (IOException e) {

        } finally {
            Misc.close(writer);
        }
    }

    /**
     * 判断给定的VersionCode是否忽略列表里（是否需要忽略）
     *
     * @param versionCode 要判断的VersionCode
     * @return true表示在忽略列表里，需要忽略
     */
    public boolean needIgnore(int versionCode) {
        if (this.itemList != null) {
            for (Item item : itemList) {
                if (item.versionCode == versionCode) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void serialize(JsonWriter jsonWriter) throws IOException {
        if (this.itemList != null) {
            jsonWriter.beginObject();
            {
                jsonWriter.name(JSON_NAME_LIST);
                serializeItemList(jsonWriter, this.itemList);
            }
            jsonWriter.endObject();
        }
    }

    static void serializeItemList(JsonWriter jsonWriter, Iterable<Item> itemList) throws IOException {
        jsonWriter.beginArray();
        if (itemList != null) {
            for (Item item : itemList) {
                item.serialize(jsonWriter);
            }
        }
        jsonWriter.endArray();
    }
}
