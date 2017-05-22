package com.subao.common.data;

/**
 * ServiceConfigForTest
 * <p>Created by YinHaiBo on 2016/11/24.</p>
 */

public class ServiceConfigForTest {

    private static final ServiceConfig serviceConfig;

    static {
        serviceConfig = new ServiceConfig();
        serviceConfig.logLevel = 2;
        serviceConfig.nodesInfo = new AccelNodesDownloader.NodesInfo(
            1, "1:122.224.73.168:bgp,"
        );
        serviceConfig.urlH5 = null;
        serviceConfig.portalServiceLocation = new ServiceLocation(null, "uat.xunyou.mobi", 2400);
        serviceConfig.authServiceLocation = new ServiceLocation("https", "uat.xunyou.mobi", -1);
        serviceConfig.messageServiceLocation = new ServiceLocation(null, "uat.xunyou.mobi", 501);
    }

    public static ServiceConfig getServiceConfig() {
        return serviceConfig;
    }
}
