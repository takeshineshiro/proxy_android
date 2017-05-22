package com.subao.common.data;

import android.annotation.SuppressLint;
import com.subao.common.Misc;
import com.subao.common.net.Protocol;

/**
 * 此结构，由Java层传递给JNI代理层，指明支持的游戏的相关信息
 */
@SuppressLint("DefaultLocale")
public class SupportGame {
	
    public final int uid;
    public final String packageName;
    public final String appLabel;
    public final Protocol protocol;
    public final boolean isForeign;
    public final Iterable<AccelGame.PortRange> whitePorts;
    public final Iterable<AccelGame.PortRange> blackPorts;
    public final Iterable<String> whiteIps;
    public final Iterable<String> blackIps;

    public SupportGame(int uid, String packageName, String appLabel,
                       Protocol protocol, boolean isForeign,
                       Iterable<AccelGame.PortRange> whitePorts, Iterable<AccelGame.PortRange> blackPorts,
                       Iterable<String> whiteIps, Iterable<String> blackIps) {
        this.uid = uid;
        this.packageName = packageName;
        this.appLabel = appLabel;
        this.protocol = protocol;
        this.isForeign = isForeign;
        this.whitePorts = whitePorts;
        this.blackPorts = blackPorts;
        this.whiteIps = whiteIps;
        this.blackIps = blackIps;
    }
	
    @Override
    public String toString() {
        return String.format(
            "[%s (uid=%d), protocol=%s, foreign=%b, white-ports='%s', black-ports='%s', white-ips='%s', black-ips='%s']",
            packageName, uid, protocol.upperText, isForeign,
            whitePorts, blackPorts, whiteIps, blackIps);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (o == this) { return true; }
        if (!(o instanceof SupportGame)) { return false; }
        SupportGame other = (SupportGame)o;
        return this.uid == other.uid
            && this.protocol == other.protocol
            && this.isForeign == other.isForeign
            && Misc.isEquals(this.packageName, other.packageName)
            && Misc.isEquals(this.appLabel, other.appLabel)
            && Misc.isEquals(this.whitePorts, other.whitePorts)
            && Misc.isEquals(this.blackPorts, other.blackPorts)
            && Misc.isEquals(this.whiteIps, other.whiteIps)
            && Misc.isEquals(this.blackIps, other.blackIps);
    }
}