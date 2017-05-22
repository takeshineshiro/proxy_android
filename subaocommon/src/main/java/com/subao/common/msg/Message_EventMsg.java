package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.Misc;
import com.subao.common.data.AppType;
import com.subao.common.utils.JsonUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Event消息
 * <ul>
 * <li>文档：/svn/tools/grpc/下一代数据上报接口.docx</li>
 * <li>协议：/svn/tools/grpc/client/event.proto</li>
 * </ul>
 */
public class Message_EventMsg implements Iterable<Message_EventMsg.Event>, JsonSerializable {

    /**
     * ID，在某些场合是允许为空的
     */
    public final MessageUserId msgUserId;

    /**
     * AppType
     *
     * @see AppType
     */
    public final AppType appType;

    /**
     * Version
     *
     * @see Message_VersionInfo
     */
    public final Message_VersionInfo versionInfo;

    /**
     * 具体的事件数据列表
     */
    private final List<Event> events;

    public Message_EventMsg(MessageUserId msgUserId, AppType appType, Message_VersionInfo versionInfo, List<Event> events) {
        this.msgUserId = msgUserId;
        this.appType = appType;
        this.versionInfo = versionInfo;
        this.events = events;
    }

    /**
     * 是否包含至少一个事件？
     *
     * @return true表示至少包含一件事件，false表示事件列表为空
     */
    public boolean hasEvents() {
        return events != null && !events.isEmpty();
    }

    @Override
    public Iterator<Event> iterator() {
        if (events != null) {
            return events.iterator();
        }
        return new Iterator<Event>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Event next() {
                return null;
            }

            @Override
            public void remove() {
            }
        };
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        JsonUtils.writeSerializable(writer, "id", this.msgUserId);
        MessageJsonUtils.serializeEnum(writer, "type", this.appType);
        JsonUtils.writeSerializable(writer, "version", this.versionInfo);
        // events
        if (this.hasEvents()) {
            writer.name("events");
            writer.beginArray();
            for (Event event : this) {
                event.serialize(writer);
            }
            writer.endArray();
        }
        writer.endObject();
    }

    /**
     * 具体的事件数据
     */
    public static class Event implements Iterable<Entry<String, String>>, JsonSerializable {

        /**
         * 事件名
         */
        public final String id;

        /**
         * 发生本条日志的时刻（UTC的秒）
         */
        public final long timeOfUTCSeconds;

        /**
         * 事件参数（键值对）
         */
        private final Map<String, String> paras;

        public Event(String id, Map<String, String> params) {
            this(id, System.currentTimeMillis() / 1000, params);
        }

        /**
         * 构造
         */
        public Event(String id, long timeOfUTCSeconds, Map<String, String> paras) {
            this.id = id;
            this.timeOfUTCSeconds = timeOfUTCSeconds;
            this.paras = paras;
        }

        private static boolean isMapEquals(Map<String, String> m1, Map<String, String> m2) {
            if (m1 == null) {
                return m2 == null;
            }
            if (m2 == null) {
                return false;
            }
            if (m1.size() != m2.size()) {
                return false;
            }
            for (Entry<String, String> entry : m1.entrySet()) {
                String key = entry.getKey();
                if (!Misc.isEquals(entry.getValue(), m2.get(key))) {
                    return false;
                }
            }
            return true;
        }

        public int getParamsCount() {
            return paras == null ? 0 : paras.size();
        }

        public String getParamValue(String key) {
            if (paras != null) {
                return paras.get(key);
            } else {
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (null == o) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (!(o instanceof Event)) {
                return false;
            }
            Event other = (Event) o;
            return this.timeOfUTCSeconds == other.timeOfUTCSeconds
                && Misc.isEquals(this.id, other.id)
                && isMapEquals(this.paras, other.paras);
        }

        @Override
        public Iterator<Entry<String, String>> iterator() {
            if (paras != null) {
                return paras.entrySet().iterator();
            }
            return new Iterator<Entry<String, String>>() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Entry<String, String> next() {
                    return null;
                }

                @Override
                public void remove() {
                }
            };
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("id").value(id);
            writer.name("time").value(timeOfUTCSeconds);
            int paramCount = getParamsCount();
            if (paramCount > 0) {
                writer.name("paras");
                writer.beginArray();
                for (Entry<String, String> entry : this) {
                    writer.beginObject();
                    // 如果只有一个键值对，则Key字段可以省略
                    if (paramCount > 1) {
                        writer.name("key").value(entry.getKey());
                    }
                    writer.name("value").value(entry.getValue());
                    writer.endObject();
                }
                writer.endArray();
            }
            writer.endObject();
        }
    }

    @Override
    public String toString() {
        return String.format("[Message_Event: count=%d]", this.events == null ? 0 : this.events.size());
    }
}
