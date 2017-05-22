package com.subao.common.data;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.subao.common.net.Protocol;

import java.util.List;

/**
 * AccelGame 可加速的游戏配置
 * <p>Created by YinHaiBo on 2017/3/27.</p>
 */
public class AccelGame {
    public static final int ACCEL_MODE_ALLOW = 1;
    public static final int ACCEL_MODE_FAKE = 2;
    public static final int ACCEL_MODE_DENY = 3;

    public static final int FLAG_FOREIGN = 1;
    public static final int FLAG_EXACT_MATCH = 2;

    //废弃
//    public static final int FLAG_RECOMMEND_VPN = 4;
//    public static final int FLAG_RECOMMEND_ROOT = 8;

    public static final int FLAG_UDP = 16;
    public static final int FLAG_TCP = 32;

    public final String appLabel;
    public final int accelMode;
    public final int flags;
    public final boolean isLabelThreeAsciiChar;

    private final List<PortRange> whitePorts;
    private final List<PortRange> blackPorts;

    private final List<String> blackIps;
    private final List<String> whiteIps;

    private AccelGame(String appLabel, int accelMode, int flags, List<PortRange> whitePorts, List<PortRange> blackPorts, List<String> whiteIps, List<String> blackIps) {
        this.appLabel = appLabel;
        this.accelMode = accelMode;
        this.flags = flags;
        if (this.appLabel.length() == 3) {
            this.isLabelThreeAsciiChar = isAllAscii(this.appLabel);
        } else {
            this.isLabelThreeAsciiChar = false;
        }
        //
        this.whitePorts = whitePorts;
        this.blackPorts = blackPorts;
        //
        this.whiteIps = whiteIps;
        this.blackIps = blackIps;
    }

    private static boolean isAllAscii(String s) {
        for (int i = s.length() - 1; i >= 0; --i) {
            char ch = s.charAt(i);
            if (ch < 32 || ch > 127) {
                return false;
            }
        }

        return true;
    }

    public static AccelGame create(String appLabel, int accelMode, int flags, List<PortRange> whitePorts, List<PortRange> blackPorts, List<String> whiteIps, List<String> blackIps) {
        return TextUtils.isEmpty(appLabel) ? null : new AccelGame(appLabel, accelMode, flags, whitePorts, blackPorts, whiteIps, blackIps);
    }

    public boolean isForeign() {
        return (this.flags & 1) != 0;
    }

    public boolean needExactMatch() {
        return (this.flags & FLAG_EXACT_MATCH) != 0;
    }

    public boolean isAccelFake() {
        return this.accelMode == ACCEL_MODE_FAKE;
    }

    public Protocol getProtocol() {
        Protocol protocol;
        if ((this.flags & FLAG_UDP) != 0) {
            protocol = Protocol.UDP;
        } else {
            protocol = null;
        }
        if ((this.flags & FLAG_TCP) != 0) {
            if (protocol == null) {
                protocol = Protocol.TCP;
            } else {
                protocol = Protocol.BOTH;
            }
        }
        return protocol;
    }

    public Iterable<PortRange> getWhitePorts() {
        return this.whitePorts;
    }

    public Iterable<PortRange> getBlackPorts() {
        return this.blackPorts;
    }

    public Iterable<String> getBlackIps() {
        return this.blackIps;
    }

    public Iterable<String> getWhiteIps() {
        return this.whiteIps;
    }

    public static class PortRange {
        public final int start;
        public final int end;

        public PortRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @SuppressLint("DefaultLocale")
        public String toString() {
            return String.format("[startPort=%d, endPort=%d]", this.start, this.end);
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o == this) {
                return true;
            } else if (!(o instanceof PortRange)) {
                return false;
            } else {
                PortRange other = (PortRange) o;
                return this.start == other.start && this.end == other.end;
            }
        }
    }

    public static class Builder {

        private int accelMode;
        private int flags;

        private List<PortRange> whitePorts, blackPorts;
        private List<String> whiteIps, blackIps;

        public AccelGame build(String appLabel) {
            return AccelGame.create(appLabel, accelMode, flags, whitePorts, blackPorts, whiteIps, blackIps);
        }

        public void setAccelMode(int accelMode) {
            this.accelMode = accelMode;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public void setWhitePorts(List<PortRange> whitePorts) {
            this.whitePorts = whitePorts;
        }

        public void setBlackPorts(List<PortRange> blackPorts) {
            this.blackPorts = blackPorts;
        }

        public void setWhiteIps(List<String> whiteIps) {
            this.whiteIps = whiteIps;
        }

        public void setBlackIps(List<String> blackIps) {
            this.blackIps = blackIps;
        }
    }

}
