package com.subao.common.qos;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import com.subao.common.ErrorCode;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.data.AppType;
import com.subao.common.net.IPv4;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.NetUtils;
import com.subao.common.net.NetUtils.LocalIpFilter;
import com.subao.common.qos.QosManager.Action;
import com.subao.common.qos.QosManager.CallbackParam;

public class QosHelper {

    private static final String TAG = LogTag.QOS;

    private QosHelper() {}

    public interface Tools extends LocalIpFilter {
        AppType getAppType();

        String getChannel();

        String getVersionNum();

        String getSubaoId();

        NetTypeDetector getNetTypeDetector();

        String getIMSI();

    }

    public interface Callback {
        void onQosResult(QosManager.Action action, QosManager.CallbackParam param);
    }

    /**
     * Qos请求的回调处理
     */
    private static class CallbackWrapper implements QosManager.Callback {

        private final Callback callback;

        public CallbackWrapper(Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onQosResult(QosManager.Action action, int managerPort, QosManager.CallbackParam param) {
            if (Logger.isLoggableDebug(TAG)) {
                Log.d(TAG, String.format("Qos request [%s] result: %s", action, param));
            }
            if (callback != null) {
                callback.onQosResult(action, param);
            }
        }

    }

    public static class Opener implements Runnable {

        final QosManager.Key qosKey;
        private final NetTypeDetector netTypeDetector;
        private final QosManager.EndPoint2EndPoint e2e;
        private final int timeSeconds;

        private final Tools tools;

        private final Callback callback;

        public Opener(
            QosManager.Key qosKey,
            NetTypeDetector netTypeDetector,
            QosManager.EndPoint2EndPoint e2e,
            /** 提速时长，单位：秒 */
            int timeSeconds,
            /** 提供各必要参数 */
            Tools tools,
            /** 回调，不能为null */
            Callback callback) {
            if (callback == null) {
                throw new NullPointerException("Callback can not be null");
            }
            this.qosKey = qosKey;
            this.netTypeDetector = netTypeDetector;
            this.e2e = e2e;
            this.timeSeconds = timeSeconds;
            this.callback = callback;
            this.tools = tools;
        }

        @Override
        public void run() {
            QosManager.CallbackParam callbackParam = execute();
            if (callbackParam != null && callback != null) {
                callback.onQosResult(QosManager.Action.OPEN, callbackParam);
            }
        }

        /**
         * 创建一个请求
         *
         * @param imsi 用户的IMSI号
         */
        @SuppressLint("DefaultLocale")
        QosManager.Requester createRequester(QosParam qosParam, String imsi) {
            // 取得私有IP
            String privateIp;
            if (TextUtils.isEmpty(e2e.srcIp)) {
                privateIp = IPv4.ipToString(NetUtils.getLocalIp(tools));
                if (TextUtils.isEmpty(privateIp)) {
                    Log.w(TAG, "Cannot getConfigString private IP");
                }
            } else {
                privateIp = e2e.srcIp;
            }
            //
            // TerminalInfo
            QosTerminalInfo terminalInfo = new QosTerminalInfo(
                privateIp,      // 私网IP
                e2e.srcPort,    // 源端口
                null,           // 公网IP，本版本不填写
                imsi,           // IMSI
                null            // 手机号
            );
            //
            // MediaInfo
            QosMediaInfo mediaInfo = new QosMediaInfo(
                privateIp,
                e2e.srcPort,
                e2e.dstIp,
                e2e.dstPort,
                e2e.protocol.upperText);
            QosSetupRequest qsr = new QosSetupRequest(
                tools.getAppType(), tools.getChannel(), tools.getVersionNum(), tools.getSubaoId(),
                this.timeSeconds, terminalInfo, mediaInfo);
            return new QosManager.Requester_Open(qosKey, qosParam, qsr);
        }

        private QosManager.CallbackParam execute() {
            if (timeSeconds <= 0) {
                Log.w(TAG, String.format("Bad accel time: %d", this.timeSeconds));
                int errorCode = ErrorCode.QOS_INVALID_TIME_LENGTH;
                QosEventBuilder builder = new QosEventBuilder(QosManager.Action.OPEN, errorCode);
                builder.setRawData(Integer.toString(timeSeconds).getBytes());
                return new CallbackParam(qosKey.cid, errorCode, null, null, timeSeconds, builder.build());
            }
            boolean printDebugLog = Logger.isLoggableDebug(TAG);
            //
            // 是4G吗？
            if (netTypeDetector != null) {
                if (!netTypeDetector.isConnected()) {
                    if (printDebugLog) {
                        Log.d(TAG, "Network disconnected when Qos open attempt");
                    }
                    int errorCode = ErrorCode.QOS_NETWORK_DISCONNECT;
                    QosEventBuilder builder = new QosEventBuilder(QosManager.Action.OPEN, errorCode);
                    return new CallbackParam(qosKey.cid, errorCode, null, null, timeSeconds, builder.build());
                }
                NetTypeDetector.NetType currentNetType = netTypeDetector.getCurrentNetworkType();
                if (currentNetType != NetTypeDetector.NetType.MOBILE_4G && currentNetType != NetTypeDetector.NetType.UNKNOWN) {
                    if (printDebugLog) {
                        Log.d(TAG, "It is not 4G now");
                    }
                    int errorCode = ErrorCode.QOS_NOT_4G;
                    QosEventBuilder builder = new QosEventBuilder(QosManager.Action.OPEN, errorCode);
                    builder.setRawData(Integer.toString(currentNetType.value).getBytes());
                    return new CallbackParam(qosKey.cid, ErrorCode.QOS_NOT_4G, null, null, timeSeconds, builder.build());
                }
            }
            // 判断地区是否支持
            QosParam qosParam = QosUser4GRegionAndISP.getInstance().getQosParam();
            if (qosParam == null) {
                int errorCode = ErrorCode.QOS_REGION_NOT_SUPPORT;
                QosEventBuilder builder = new QosEventBuilder(QosManager.Action.OPEN, errorCode);
                return new CallbackParam(qosKey.cid, ErrorCode.QOS_REGION_NOT_SUPPORT, null, null, timeSeconds, builder.build());
            }
            // 创建Requester
            QosManager.Requester requester = this.createRequester(qosParam, tools.getIMSI());
            //
            if (printDebugLog) {
                Log.d(TAG, "Open request created: " + requester.toString());
            }
            //
            QosManager.getInstance().sendRequest(qosKey.node, qosKey.port, requester, new CallbackWrapper(this.callback));
            return null; // 返回null表示回调等会儿异步通知
        }
    }

    /**
     * {@link Closer} 和 {@link Modifier} 的共同基类
     */
    private abstract static class Runner implements Runnable {

        protected final QosManager.Key qosKey;
        protected final Callback callback;
        final String sessionId;

        protected Runner(QosManager.Key qosKey, String sessionId, Callback callback) {
            this.qosKey = qosKey;
            this.sessionId = sessionId;
            this.callback = callback;
        }

        protected abstract Action getAction();

        protected abstract QosManager.Requester createRequester(String sessionId);

        @Override
        public void run() {
            QosManager.Requester requester = createRequester(sessionId);
            QosManager.getInstance().sendRequest(qosKey.node, qosKey.port, requester, new CallbackWrapper(callback));
        }
    }

    /**
     * 关闭提速
     */
    public static class Closer extends Runner {

        public Closer(QosManager.Key qosKey, String sessionId, Callback callback) {
            super(qosKey, sessionId, callback);
        }

        @Override
        protected QosManager.Requester createRequester(String sessionId) {
            return new QosManager.Requester_Close(qosKey, sessionId);
        }

        @Override
        protected Action getAction() {
            return Action.CLOSE;
        }

    }

    /**
     * 续订Qos
     */
    public static class Modifier extends Runner {

        private final int timeSeconds;

        public Modifier(QosManager.Key qosKey, String sessionId, Callback callback, int timeSeconds) {
            super(qosKey, sessionId, callback);
            this.timeSeconds = timeSeconds;
        }

        @Override
        protected QosManager.Requester_Modify createRequester(String sessionId) {
            return new QosManager.Requester_Modify(qosKey, sessionId, timeSeconds);
        }

        @Override
        protected Action getAction() {
            return Action.MODIFY;
        }

    }

}
