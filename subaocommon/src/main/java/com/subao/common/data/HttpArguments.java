package com.subao.common.data;

import com.subao.common.net.NetTypeDetector;

/**
 * 执行HTTP请求的一些必要参数
 * <p>Created by YinHaiBo on 2017/2/22.</p>
 */

public class HttpArguments {

    /**
     * 客户端类型，用于构造数据下载的URL，如果是APP，应是常量"android"，如果是SDK，应该是游戏的GUID
     */
    public final String clientType;

    /**
     * 本程序（或SDK）的版本号
     */
    public final String version;

    /**
     * 服务地址
     */
    public final ServiceLocation serviceLocation;

    /**
     * 用于判断当前网络类型
     */
    public final NetTypeDetector netTypeDetector;

    /**
     * @param clientType      客户端类型，用于构造数据下载的URL，如果是APP，应是常量"android"，如果是SDK，应该是游戏的GUID
     * @param version         本程序（或SDK）的版本号
     * @param serviceLocation {@link ServiceLocation} 服务地址
     * @param netTypeDetector 用于判断当前网络类型
     * @see Defines#REQUEST_CLIENT_TYPE_FOR_APP
     */
    public HttpArguments(
        String clientType,
        String version,
        ServiceLocation serviceLocation,
        NetTypeDetector netTypeDetector
    ) {
        this.clientType = clientType;
        this.version = version;
        this.serviceLocation = serviceLocation;
        this.netTypeDetector = netTypeDetector;
    }

}
