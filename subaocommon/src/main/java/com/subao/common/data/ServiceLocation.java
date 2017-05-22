package com.subao.common.data;

import android.text.TextUtils;

import com.subao.common.Misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网络服务的地址，由“协议”、“主机”和“端口”组成。用于各种网络资源的请求
 * <p>Created by YinHaiBo on 2016/11/22.</p>
 */

public class ServiceLocation {

    /**
     * 协议。例如 "http", "https" ...
     */
    public final String protocol;

    /**
     * 主机。例如 "api.xunyou.mobi" ...
     */
    public final String host;

    /**
     * 端口。例如 80, 443 ...<br />
     * -1 表示使用协议默认端口。比如http默认为80
     */
    public final int port;

    /**
     * 构造
     *
     * @param protocol 协议。例如"http"、"https"等。null或空串将被转换为"http"
     * @param host     主机名
     * @param port     端口号
     */
    public ServiceLocation(String protocol, String host, int port) {
        this.protocol = TextUtils.isEmpty(protocol) ? "http" : protocol;
        this.host = host;
        this.port = port;
    }

    /**
     * 将给定的URL字符串解析成{@link ServiceLocation}
     *
     * @param url 给定的URL字符串，形如：https://api.xunyou.mobi:501/hello/world
     * @return null表示输入的字符串格式无效
     */
    public static ServiceLocation parse(String url) {
        if (url == null || url.length() == 0) {
            return null;
        }
        Pattern pattern = Pattern.compile("^(?:(.+)://)?([^/:?]+)(?::(\\d+))?/?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
            return null;
        }
        String host = matcher.group(2);
        if (host == null || host.length() == 0) {
            return null;
        }
        int port = -1;
        String portStr = matcher.group(3);
        if (portStr != null) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new ServiceLocation(matcher.group(1), host, port);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof ServiceLocation)) {
            return false;
        }
        ServiceLocation other = (ServiceLocation) o;
        return this.port == other.port
            && Misc.isEquals(this.host, other.host)
            && Misc.isEquals(this.protocol, other.protocol);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(protocol.length() + host.length() + 64);
        sb.append(protocol).append("://").append(host);
        if (port >= 0) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }
}
