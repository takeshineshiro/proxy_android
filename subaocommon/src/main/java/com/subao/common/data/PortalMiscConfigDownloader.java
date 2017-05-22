package com.subao.common.data;

import com.subao.common.auth.AuthExecutor;
import com.subao.common.msg.MessageEvent;

/**
 * PortalMiscConfigDownloader
 * <p>Created by YinHaiBo on 2017/2/21.</p>
 */

public class PortalMiscConfigDownloader extends PortalKeyValuesDownloader {

    private final MiscConfig miscConfig = new MiscConfig();

    protected PortalMiscConfigDownloader(Arguments arguments) {
        super(arguments);
    }

    public static void start(Arguments arguments) {
        PortalMiscConfigDownloader downloader = new PortalMiscConfigDownloader(arguments);
        PortalKeyValuesDownloader.start(downloader);
    }

    @Override
    protected void process(String name, String value) {
        miscConfig.parse(name, value);
    }

    @Override
    protected String getUrlPart() {
        return "configs/misc";
    }

    @Override
    protected String getId() {
        return "misc-config";
    }

    @Override
    protected void onAllProcessed() {
        super.onAllProcessed();
        boolean tencent = MiscConfig.calcSpecEventReportAllow(miscConfig.erTencent);
        boolean auth = MiscConfig.calcSpecEventReportAllow(miscConfig.erAuth);
        MessageEvent.ReportAllow.set(tencent, auth);
        AuthExecutor.setProtocol(miscConfig.authHTTP);
        UserAccelInfoUploader.setProtocol(miscConfig.protocolAccelInfoUpload);
    }

    static class MiscConfig {
        static final String KEY_EVENT_RATIO_TG = "er_tg";        // tg_ 开头的几个Event的上报抽样比率
        static final String KEY_EVENT_RATIO_AUTH = "er_auth";    // 鉴权相关的几个Event的上报抽样比率
        static final String KEY_AUTH_HTTP = "auth_http";  // 鉴权时用HTTP协议吗？
        static final String KEY_ACCEL_INFO_UP_PROTOCOL = "acc_info_up_proto";   // 加速信息上传时用啥协议？

        static final int MAX_EVENT_RATIO = 10000;
        /**
         * tg_前缀的Event的抽样比例，单位：万分之一 （默认值：1%）
         */
        private int erTencent = MAX_EVENT_RATIO / 100;
        /**
         * 鉴权相关的几个Event的抽样比例，单位：万分之一 （默认值：10%）
         */
        private int erAuth = MAX_EVENT_RATIO / 10;
        /**
         * 鉴权是否采用HTTP协议
         */
        private boolean authHTTP;

        /**
         * 上传加速信息时用什么协议？
         */
        private String protocolAccelInfoUpload;

        private static boolean parseBoolean(String value) {
            return ("1".equals(value) || "true".equalsIgnoreCase(value));
        }

        /**
         * tg_前缀的Event的抽样比例，单位：万分之一 （默认值：1%）
         */
        int getEventRateTencent() {
            return this.erTencent;
        }

        /**
         * 鉴权相关的几个Event的抽样比例，单位：万分之一 （默认值：10%）
         */
        int getEventRateAuth() {
            return this.erAuth;
        }

        /**
         * 鉴权是否采用HTTP协议
         */
        boolean doesAuthUseHttp() {
            return this.authHTTP;
        }

        /**
         * 上传加速信息时用什么协议？
         */
        String getProtocolAccelInfoUpload() {
            return this.protocolAccelInfoUpload;
        }

        /**
         * 根据当前时刻，计算给定的抽样比率是否命中
         *
         * @return true比率命中
         */
        static boolean calcSpecEventReportAllow(int ratio) {
            return calcSpecEventReportAllow(ratio, (int) System.currentTimeMillis());
        }

        static boolean calcSpecEventReportAllow(int ratio, int randomValue) {
            if (ratio <= 0) {
                return false;
            }
            if (ratio >= MAX_EVENT_RATIO) {
                return true;
            }
            return (randomValue % MAX_EVENT_RATIO) < ratio;
        }

        void parse(String name, String value) {
            try {
                if (KEY_EVENT_RATIO_TG.equals(name)) {
                    this.erTencent = Integer.parseInt(value);
                } else if (KEY_EVENT_RATIO_AUTH.equals(name)) {
                    this.erAuth = Integer.parseInt(value);
                } else if (KEY_AUTH_HTTP.equals(name)) {
                    this.authHTTP = parseBoolean(value);
                } else if (KEY_ACCEL_INFO_UP_PROTOCOL.equals(name)) {
                    this.protocolAccelInfoUpload = value;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

        }
    }
}
