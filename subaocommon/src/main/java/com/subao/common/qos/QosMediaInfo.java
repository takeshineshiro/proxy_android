package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;

import java.io.IOException;

class QosMediaInfo implements JsonSerializable {

    /**
     * 源IP（私网）
     */
    public final String srcIp;

    /**
     * 源端口号（私网）
     */
    public final int srcPort;

    /**
     * 目的IP
     */
    public final String dstIp;

    /**
     * 目的端口号
     */
    public final int dstPort;

    /**
     * 协议类型：
     * <ul>
     * <li>“TCP”：TCP协议</li>
     * <li>“UDP”：UDP协议</li>
     * </ul>
     */
    public final String protocol;

    public QosMediaInfo(String srcIp, int srcPort, String dstIp, int dstPort, String protocol) {
        this.srcIp = srcIp;
        this.srcPort = srcPort;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
        this.protocol = protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof QosMediaInfo)) {
            return false;
        }
        QosMediaInfo other = (QosMediaInfo) o;
        return this.srcPort == other.srcPort
            && this.dstPort == other.dstPort
            && Misc.isEquals(this.srcIp, other.srcIp)
            && Misc.isEquals(this.dstIp, other.dstIp)
            && Misc.isEquals(this.protocol, other.protocol);
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        JsonUtils.writeString(writer, "srcIp", this.srcIp);
        writer.name("srcPort").value(srcPort);
        JsonUtils.writeString(writer, "dstIp", this.dstIp);
        writer.name("dstPort").value(dstPort);
        JsonUtils.writeString(writer, "protocol", this.protocol);
        writer.endObject();
    }
}
