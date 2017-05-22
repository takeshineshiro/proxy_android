package com.subao.common.data;

/**
 * 智能IP列表，返回最适合当前网络情况的IP
 */
public interface SmartIPSelector {

    /**
     * 根据当前网络情况返回最适合的一个IP
     */
    String smartSelectIP();
}
