package com.subao.common.msg;

import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;

public class MessageEvent {

    private static final String TAG = LogTag.MESSAGE;

    public static class ReportAllow {
        private static boolean tencent;
        private static boolean auth;

        private ReportAllow() {}

        public static void set(boolean tencent, boolean auth) {
            ReportAllow.tencent = tencent;
            ReportAllow.auth = auth;
            //
            if (Logger.isLoggableDebug(TAG)) {
                Log.d(TAG, String.format("ReportAllow set: tg=%b, auth=%b", tencent, auth));
            }
        }

        public static boolean getTencent() {
            return tencent;
        }

        public static boolean getAuth() {
            return auth;
        }
    }

    /**
     * 事件上报执行者
     */
    public interface Reporter {

        /**
         * 上报给定的事件
         *
         * @param eventName  事件名称
         * @param eventParam 事件参数
         */
        void reportEvent(String eventName, String eventParam);
    }

}
