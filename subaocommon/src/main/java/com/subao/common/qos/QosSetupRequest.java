package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.data.AppType;
import com.subao.common.utils.JsonUtils;

import java.io.IOException;

public class QosSetupRequest implements JsonSerializable {

    public final AppType appType;
    public final String channel;
    public final String versionNum;
    public final String subaoId;
    public final int timeLength;
    public final QosTerminalInfo terminalInfo;
    private final QosMediaInfo[] mediaInfoList;
    private String operator;

    private QosSetupRequest(
        AppType appType,
        String channel, String versionNum, String subaoId,
        int timeLength,
        QosTerminalInfo terminalInfo,
        QosMediaInfo[] mediaInfoList) {
        this.appType = appType;
        this.channel = channel;
        this.versionNum = versionNum;
        this.subaoId = (subaoId == null ? "" : subaoId);
        this.timeLength = timeLength;
        this.terminalInfo = terminalInfo;
        this.mediaInfoList = mediaInfoList;
    }

    public QosSetupRequest(
        AppType appType,
        String channel, String versionNum, String subaoId,
        int timeLength,
        QosTerminalInfo terminalInfo,
        QosMediaInfo mediaInfo) {
        this(appType, channel, versionNum, subaoId, timeLength, terminalInfo,
            mediaInfo == null ? null : new QosMediaInfo[]{mediaInfo});
    }

    public void setSecurityToken(String value) {
        if (this.terminalInfo != null) {
            this.terminalInfo.setSecurityToken(value);
        }
    }

    public void setPhoneNumber(String value) {
        if (this.terminalInfo != null) {
            this.terminalInfo.setMSISDN(value);
        }
    }

    public void setPrivateIp(String value) {
        if (this.terminalInfo != null) {
            this.terminalInfo.setPrivateIp(value);
        }
    }

    public QosMediaInfo getMediaInfo(int index) {
        return mediaInfoList[index];
    }

    public int getMediaInfoCount() {
        if (mediaInfoList == null) {
            return 0;
        } else {
            return mediaInfoList.length;
        }
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("appType").value(this.appType.getId());
        JsonUtils.writeString(writer, "channel", this.channel);
        JsonUtils.writeString(writer, "versionNum", this.versionNum);
        JsonUtils.writeString(writer, "userId", this.subaoId);
        JsonUtils.writeString(writer, "operator", this.operator);
        writer.name("timeLength").value(this.timeLength);
        JsonUtils.writeSerializable(writer, "terminalInfo", this.terminalInfo);
        if (this.mediaInfoList != null && this.mediaInfoList.length > 0) {
            writer.name("mediaInfo");
            writer.beginArray();
            for (QosMediaInfo mi : this.mediaInfoList) {
                JsonUtils.writeSerializable(writer, null, mi);
            }
            writer.endArray();
        }
        writer.endObject();
    }

    public String getOperator() {
        return this.operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

}
