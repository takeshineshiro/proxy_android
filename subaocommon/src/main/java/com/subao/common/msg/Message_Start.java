package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.data.AppType;

import java.io.IOException;
import java.util.List;

public class Message_Start implements JsonSerializable {

    /**
     * 结果
     */
    public enum Result {
        UNKNOWN_EXCE_RESULT(0),
        //无需下载执行动态脚本
        NO_SCRIPT(1),
        //下载失败
        SCRIPT_DOWNLOAD_FAIL(2),
        //执行成功
        SCRIPT_EXEC_SUCCESS(3),
        //执行失败
        SCRIPT_EXEC_FAIL(4);

        Result(int id) {
            this.id = id;
        }

        public final int id;
    }

    /**
     * 启动类型
     */
    public enum StartType {
        UNKNOWN_START_TYPE(0),
        //当日首次启动
        START(1),
        //没有退出或关机，每日定时上报
        DAILY(2);

        StartType(int id) {
            this.id = id;
        }

        public final int id;
    }

    public static class ScriptResult implements JsonSerializable {

        public final Result result;
        public final String note;

        public ScriptResult(Result result, String note) {
            this.result = result;
            this.note = note;
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            if (result != null) {
                writer.name("result");
                writer.value(result.id);
            }
            JsonUtils.writeString(writer, "note", note);
            writer.endObject();
        }

    }

    /**
     * Id
     */
    public final MessageUserId id;
    /**
     * unix time
     */
    public final long time;
    /**
     * 启动类型 @see {@link StartType}
     */
    public final StartType startType;
    /**
     * 获取到的节点数量，注意协议里是无符号整型
     */
    public final int nodeNum;
    /**
     * 获取到的支持的游戏数量，注意协议里是无符号类型
     */
    public final int gameNum;

    public final ScriptResult scriptResult;

    private final List<Message_App> appList;

    public final Message_VersionInfo version;

    public final AppType appType;

    public Message_Start(
        MessageUserId id,
        StartType startType,
        int nodeNum, int gameNum,
        ScriptResult scriptResult,
        List<Message_App> appList,
        Message_VersionInfo version,
        AppType appType) {
        this.id = id;
        this.time = System.currentTimeMillis() / 1000;
        this.startType = startType;
        this.nodeNum = nodeNum;
        this.gameNum = gameNum;
        this.scriptResult = scriptResult;
        this.appList = appList;
        this.version = version;
        this.appType = appType;
    }

    public Iterable<Message_App> getAppList() {
        return this.appList;
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        if (this.id != null) {
            writer.name("id");
            this.id.serialize(writer);
        }
        writer.name("time").value(this.time);
        if (startType != null) {
            writer.name("startType");
            writer.value(startType.id);
        }
        JsonUtils.writeUnsignedInt(writer, "nodeNum", nodeNum);
        JsonUtils.writeUnsignedInt(writer, "gameNum", gameNum);
        JsonUtils.writeSerializable(writer, "scriptResult", scriptResult);
        MessageJsonUtils.serializeAppList(writer, "appList", this.getAppList());
        JsonUtils.writeSerializable(writer, "version", this.version);
        MessageJsonUtils.serializeEnum(writer, "type", this.appType);
        writer.endObject();
    }

}
