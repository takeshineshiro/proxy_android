package com.subao.common.msg;

import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.Misc;
import com.subao.common.data.RegionAndISP;
import com.subao.common.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MessageJsonUtils {

    /**
     * 辅助函数：从服务器的Response JSON里解析出SubaoId或SessionId（格式都一样的，如下：）
     * <pre>
     * {
     *     id:{
     *         id:"xxx-xxxxx-xxxx-xxxxx"
     *     }
     * }
     * </pre>
     *
     * @param data Response Body。如果为null或empty，则函数返回null
     * @return 解析得到的SubaoId或null
     */
    static String parseSubaoIdFromJson(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(data)));
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                if ("id".equals(reader.nextName())) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        if ("id".equals(reader.nextName())) {
                            return reader.nextString();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            // return null
        } catch (RuntimeException e) {
            // return null
        } finally {
            Misc.close(reader);
        }
        return null;
    }

    /**
     * 在给定的Json串里，插入一个Object
     *
     * @param json 原始Json串
     * @param name 要插入的Object的名字
     * @param obj  要插入的对象
     * @return 插入后的Json串
     * @throws IOException
     */
    static String insertObjectToJson(String json, String name, JsonSerializable obj) throws IOException {
        int idx = json.indexOf('{');    // 第一个出现的左花括号
        if (idx < 0) {
            return json;
        }
        StringWriter sw = new StringWriter(json.length() + 1024);
        if (idx > 0) {
            sw.write(json, 0, idx);    // 左花括号以前的部分
        }
        // 插入对象
        JsonWriter jw = new JsonWriter(new FilterWriter(sw));
        if (name != null) {
            jw.beginObject();
            jw.name(name);
        }
        obj.serialize(jw);
        if (name != null) {
            jw.endObject();
        }
        jw.flush();
        // 如果原来的Json里有对象（不是一对空的花括号），需要加一个逗号
        for (int i = idx + 1, length = json.length(); i < length; ++i) {
            char ch = json.charAt(i);
            if (ch > ' ') {
                if (ch != '}') {
                    sw.write(',');
                }
                break;
            }
        }
        // 添加原来的部分
        sw.write(json, idx + 1, json.length() - idx - 1);
        sw.close();
        return sw.toString();
    }

    static void serializeEnum(JsonWriter writer, String name, MessageEnum msgEnum) throws IOException {
        if (msgEnum != null) {
            writer.name(name);
            writer.value(msgEnum.getId());
        }
    }

    static void serializeAppList(JsonWriter writer, String name, Iterable<Message_App> appList) throws IOException {
        if (appList != null) {
            writer.name(name);
            writer.beginArray();
            for (Message_App ma : appList) {
                ma.serialize(writer);
            }
            writer.endArray();
        }
    }

    public static class LinkMsgData {
        public final String jsonFromJNI;
        public final String netDetail;
        public final String gameServerId;
        public final MessageSender.FreeFlowType freeFlowType;
        public final long timeMillis;

        public LinkMsgData(
            String jsonFromJNI, String netDetail, String gameServerId,
            MessageSender.FreeFlowType freeFlowType,
            long timeMillis
        ) {
            this.jsonFromJNI = jsonFromJNI;
            this.netDetail = netDetail;
            this.gameServerId = gameServerId;
            this.freeFlowType = freeFlowType;
            this.timeMillis = timeMillis;
        }
    }

    /**
     * 对JNI传来是Link消息Json串进行翻译改造<br />
     * <li>对network对象，补足detail</li> <li>在整个Json对象里，加上SessionId对象</li>
     */
    public static class MessageLinkJsonTranslater {

        private static final Pattern PATTERN_NETDETAIL = Pattern.compile("\"network\"\\s*:\\s*\\{([^}]*)\\}", Pattern.MULTILINE);
        private static final Pattern PATTERN_QOSINFO = Pattern.compile("\"qosInfo\"\\s*:\\s*\\{", Pattern.MULTILINE);

        /**
         * 将给定List里的{@link LinkMsgData}拼接成消息上报所需的Json串
         *
         * @param list         包含{@link LinkMsgData}的容器
         * @param sessionId    会话Id
         * @param regionAndISP 要填入QosInfo里的ISP地区和类型
         * @return 拼接好的Json串
         * @throws IOException
         */
        public static String execute(Iterable<LinkMsgData> list, String sessionId, RegionAndISP regionAndISP) throws IOException {
            String jsonSessionId = buildSessionIdJsonString(sessionId);
            StringBuilder sb = new StringBuilder(16384);
            sb.append("{\"links\":[");
            int count = 0;
            for (LinkMsgData data : list) {
                if (execute(sb, data, jsonSessionId, count, regionAndISP)) {
                    ++count;
                }
            }
            sb.append("]}");
            return sb.toString();
        }

        /**
         * 将给定的{@link LinkMsgData}做为数组里的一项拼接到StringBuilder里已有的Json串里
         *
         * @param sb            内容写入到哪个StringBuilder里
         * @param data          {@link LinkMsgData}
         * @param jsonSessionId 已序列化为JSON串的的会话Id
         * @param count         数组里已有多少项
         * @param regionAndISP  要填入到QosInfo里的ISP地区和类型
         * @return true表示成功
         */
        private static boolean execute(StringBuilder sb, LinkMsgData data, String jsonSessionId, int count, RegionAndISP regionAndISP) {
            int idx = data.jsonFromJNI.indexOf('{');    // 第一个出现的左花括号
            if (idx < 0) {
                return false;
            }
            if (count > 0) {
                sb.append(',');
            }
            if (idx > 0) {
                sb.append(data.jsonFromJNI, 0, idx);    // 左花括号前的部分
            }
            // 插入左花括号和序列化好的SessionId（不含右花括号）
            sb.append(jsonSessionId).append(',');
            // 插入区服信息和免流用户类型
            if (data.gameServerId != null) {
                sb.append("\"serverId\":").append(JsonUtils.encode(data.gameServerId)).append(',');
            }
            if (data.freeFlowType != null) {
                sb.append("\"flowType\":").append(data.freeFlowType.intValue).append(',');
            }
            // 原来的左花括号后的部分
            String remain = data.jsonFromJNI.substring(idx + 1, data.jsonFromJNI.length());
            remain = insertRegionAndISPToQosInfo(remain, regionAndISP);
            insertNetDetail(sb, remain, data.netDetail);
            return true;
        }

        private static String transDelayQualityToString(Message_Link.DelayQuality delayQuality) {
            return (delayQuality == null) ? "null" : delayQuality.serializeToJson();
        }

        private static StringBuilder insertNetDetail(StringBuilder sb, String json, String netDetail) {
            if (netDetail == null || netDetail.length() == 0) {
                sb.append(json);
                return sb;
            }
            Matcher matcher = PATTERN_NETDETAIL.matcher(json);
            if (!matcher.find()) {
                sb.append(json);
                return sb;
            }
            int endOfMatch = matcher.end();
            sb.append(json, 0, endOfMatch - 1);    // 加入匹配部分的前半截，不含结束花括号
            //
            // 仅当花括号之内有其它字段时，才添加逗号分隔符
            for (int i = endOfMatch - 2; i >= 0; --i) {
                char ch = json.charAt(i);
                if (ch == '{') {
                    break;
                } else if (ch > ' ') {
                    sb.append(',');
                    break;
                }
            }
            // 插入netDetail
            sb.append("\"detail\":");
            JsonUtils.encode(sb, netDetail);
            sb.append(json, endOfMatch - 1, json.length());
            return sb;
        }

        private static String insertRegionAndISPToQosInfo(String json, RegionAndISP regionAndISP) {
            if (regionAndISP == null) {
                return json;
            }
            Matcher matcher = PATTERN_QOSINFO.matcher(json);
            if (!matcher.find()) {
                return json;
            }
            StringBuilder sb = new StringBuilder(json.length() + 128);
            int endOfMatch = matcher.end();
            sb.append(json, 0, endOfMatch); // 加入 ["qosInfo":{]
            // 加入isp字段
            sb.append("\"isp\":");
            JsonUtils.encode(sb, regionAndISP.toText());
            // 加入json余下部分，且如果json余下部分在qosInfo字段里还有字段，需加上逗号
            for (int i = endOfMatch, len = json.length(); i < len; ++i) {
                char ch = json.charAt(i);
                if (ch >= ' ') {
                    if (ch != '}') {
                        sb.append(',');
                    }
                    sb.append(json, i, len);
                    break;
                }
            }
            return sb.toString();
        }

        /**
         * 将{@link MessageSessionId}序列化为不完整的JSON串
         *
         * @return 不完整的{@link MessageSessionId}
         * 的Json表示，缺最后一个'}'字符，用于插入到来自JNI的Json串里
         * @throws IOException
         */
        private static String buildSessionIdJsonString(String sessionId) throws IOException {
            StringBuilder sb = new StringBuilder(256);
            sb.append("{\"id\":{\"id\":");
            JsonUtils.encode(sb, sessionId);
            sb.append('}');
            return sb.toString();
        }
    }

    private static class FilterWriter extends Writer {

        private final Writer writer;
        private char[] lastChar;

        public FilterWriter(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void write(char[] buf, int offset, int count) throws IOException {
            if (count > 0) {
                if (lastChar != null) {
                    writer.write(lastChar);
                } else {
                    lastChar = new char[1];
                }
                writer.write(buf, offset, count - 1);
                lastChar[0] = buf[offset + count - 1];
            }
        }

    }

}
