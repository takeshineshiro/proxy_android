package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;

import java.io.IOException;

class QosTerminalInfo implements JsonSerializable {

    private final int srcPort;
    private final String publicIp;
    private final String imsi;
    private String privateIp;
    private String msisdn;
    private String securityToken;

    public QosTerminalInfo(String privateIp, int srcPort, String publicIp, String imsi, String msisdn) {
        this(privateIp, srcPort, publicIp, imsi, msisdn, null);
    }

    public QosTerminalInfo(String privateIp, int srcPort, String publicIp, String imsi, String msisdn, String securityToken) {
        this.privateIp = privateIp;
        this.srcPort = srcPort;
        this.publicIp = publicIp;
        this.imsi = imsi;
        this.msisdn = msisdn;
        this.securityToken = securityToken;
    }

    public String getSecurityToken() {
        return this.securityToken;
    }

    public void setSecurityToken(String value) {
        this.securityToken = value;
    }

    public String getMSISDN() {
        return this.msisdn;
    }

    public void setMSISDN(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getPrivateIp() {
        return this.privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof QosTerminalInfo)) {
            return false;
        }
        QosTerminalInfo other = (QosTerminalInfo) o;
        return this.srcPort == other.srcPort
            && Misc.isEquals(this.privateIp, other.privateIp)
            && Misc.isEquals(this.publicIp, other.publicIp)
            && Misc.isEquals(this.imsi, other.imsi)
            && Misc.isEquals(this.msisdn, other.msisdn)
            && Misc.isEquals(this.securityToken, other.securityToken);
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        JsonUtils.writeString(writer, "privateIp", this.privateIp);
        writer.name("srcPort").value(this.srcPort);
        JsonUtils.writeString(writer, "publicIp", this.publicIp);
        JsonUtils.writeString(writer, "imsi", this.imsi);
        JsonUtils.writeString(writer, "msisdn", this.msisdn);
        JsonUtils.writeString(writer, "securityToken", this.securityToken);
        writer.endObject();
    }
}
