package com.subao.common.net;

/**
 * Protocol
 * <p>Created by YinHaiBo on 2017/3/24.</p>
 */

public enum Protocol {

    UDP("UDP", "udp"),
    TCP("TCP", "tcp"),
    BOTH("UDP", "udp"); // FIXME: 在SO支持both之前，暂时用udp代替

    public final String upperText;
    public final String lowerText;

    Protocol(String upperText, String lowerText) {
        this.upperText = upperText;
        this.lowerText = lowerText;
    }
}
