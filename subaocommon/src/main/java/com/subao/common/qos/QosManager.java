package com.subao.common.qos;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.subao.common.ErrorCode;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.data.RegionAndISP;
import com.subao.common.msg.Message_EventMsg;
import com.subao.common.net.Http;
import com.subao.common.net.Http.Method;
import com.subao.common.net.Http.Response;
import com.subao.common.net.Protocol;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.AuthUtils;
import com.subao.common.utils.JsonUtils;
import com.subao.common.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.Executor;

@SuppressLint("DefaultLocale")
public class QosManager {

    private static final String TAG = LogTag.QOS;
    private static final QosManager instance = new QosManager();

    private final Executor executor = ThreadPool.getExecutor(); //new com.subao.common.thread.SerialExecutor();

    private QosManager() {
    }

    public static QosManager getInstance() {
        return instance;
    }

    /**
     * 向服务器提交请求
     */
    public void sendRequest(String serverHost, int serverPort, Requester requester, Callback callback) {
        Worker worker = WorkerFactory.create(serverHost, serverPort, requester, callback);
        worker.executeOnExecutor(this.executor);
    }

    public enum Action {
        OPEN("OPEN"),
        CLOSE("CLOSE"),
        MODIFY("MODIFY");

        private final String desc;

        Action(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return this.desc;
        }
    }

    interface Callback {
        void onQosResult(Action action, int managerPort, CallbackParam param);
    }

    /**
     * 端到端的五元组
     */
    public static class EndPoint2EndPoint {
        public final String srcIp;
        public final int srcPort;
        public final String dstIp;
        public final int dstPort;
        public final Protocol protocol;

        public EndPoint2EndPoint(String srcIp, int srcPort, String dstIp, int dstPort, Protocol protocol) {
            this.srcIp = srcIp;
            this.srcPort = srcPort;
            this.dstIp = dstIp;
            this.dstPort = dstPort;
            this.protocol = protocol;
        }

        @Override
        public String toString() {
            return String.format("[%s:%d-%s:%d(%s)]",
                this.srcIp, this.srcPort,
                this.dstIp, this.dstPort,
                this.protocol.upperText);
        }
    }

    public static class Key {
        /**
         * QosManager的缺省端口
         */
        public static final int DEFAULT_PORT = 30060;

        public final int cid;
        public final String node;
        public final int port;
        public final String accessToken;

        public Key(int cid, String node, String accessToken) {
            this(cid, node, DEFAULT_PORT, accessToken);
        }

        Key(int cid, String node, int port, String accessToken) {
            this.cid = cid;
            this.node = node;
            this.port = port;
            this.accessToken = accessToken;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key other = (Key) o;
            return this.cid == other.cid
                && this.port == other.port
                && Misc.isEquals(node, other.node)
                && Misc.isEquals(accessToken, other.accessToken);
        }
    }

    /**
     * “请求”的基类
     */
    public static abstract class Requester {

        public final Key key;

        protected Requester(Key key) {
            this.key = key;
        }

        abstract Action getAction();

        String getUrlPath() {
            return "/api/app/v2/qos/";
        }

        abstract PrepareResult prepare();

        abstract Method getHttpMethod();

        abstract String buildRequestParam() throws IOException;

        /**
         * 根据QosManager返回的{@link Response}构造一个{@link CallbackParam}
         *
         * @param response 由 Qos Manager 返回的 {@link Response}
         * @return {@link CallbackParam}
         */
        abstract CallbackParam buildCallbackParam(Response response);

        protected abstract CallbackParam buildErrorCallbackParam(int errorCode, Exception e, byte[] rawData);

        final QosEventBuilder makeEventBuilder(int errorCode, Exception e, byte[] rawData) {
            QosEventBuilder qosEventBuilder = new QosEventBuilder(getAction(), errorCode);
            qosEventBuilder.setException(e);
            qosEventBuilder.setRawData(rawData);
            return qosEventBuilder;
        }

        public static class PrepareResult {
            public final int errorCode;
            public final Message_EventMsg.Event event;

            public PrepareResult(int errorCode, Message_EventMsg.Event event) {
                this.errorCode = errorCode;
                this.event = event;
            }
        }

    }

    static class PhoneNumberAndPrivateIp {

        public final int error;
        public final String phoneNumber;
        public final String privateIp;

        /**
         * 如果不为NULL，则此事件将上报Message系统
         */
        public final Message_EventMsg.Event event;

        public PhoneNumberAndPrivateIp(String phoneNumber, String privateIp) {
            this(0, phoneNumber, privateIp, null);
        }

        private PhoneNumberAndPrivateIp(int error, String phoneNumber, String privateIp, Message_EventMsg.Event event) {
            this.error = error;
            this.phoneNumber = phoneNumber;
            this.privateIp = privateIp;
            this.event = event;
        }

        public static PhoneNumberAndPrivateIp createByException(Action action, QosSetupRequest qosSetupRequest, int errorCode, Exception e) {
            QosEventBuilder qosEventBuilder = new QosEventBuilder(action, errorCode);
            qosEventBuilder.setQosSetupRequest(qosSetupRequest);
            qosEventBuilder.setException(e);
            return new PhoneNumberAndPrivateIp(errorCode, null, null, qosEventBuilder.build());
        }

        public static PhoneNumberAndPrivateIp createByRawData(Action action, QosSetupRequest qosSetupRequest, int errorCode, byte[] rawData) {
            QosEventBuilder qosEventBuilder = new QosEventBuilder(action, errorCode);
            qosEventBuilder.setQosSetupRequest(qosSetupRequest);
            qosEventBuilder.setRawData(rawData);
            return new PhoneNumberAndPrivateIp(errorCode, null, null, qosEventBuilder.build());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof PhoneNumberAndPrivateIp)) {
                return false;
            }
            PhoneNumberAndPrivateIp other = (PhoneNumberAndPrivateIp) o;
            return this.error == other.error
                && Misc.isEquals(this.phoneNumber, other.phoneNumber)
                && Misc.isEquals(this.privateIp, other.privateIp)
                && Misc.isEquals(this.event, other.event);
        }

        @Override
        public String toString() {
            return String.format("[%d, \"%s\",\"%s\"]", this.error, this.phoneNumber, this.privateIp);
        }

    }

    /**
     * 负责从第三方厂商获取电话号码、私网IP等数据
     */
    static class PhoneNumberGetter {

        /**
         * 判断给定的地区和ISP，是否需要先取手机号做为请求参数
         */
        public static boolean doesRequired(QosParam qosParam) {
            return qosParam != null && qosParam.provider == QosParam.Provider.ZTE;
        }

        public static PhoneNumberAndPrivateIp execute(Action action, QosSetupRequest qosSetupRequest, String host, int port) {
            boolean printDebugLog = Logger.isLoggableDebug(TAG);
            Response response;
            try {
                URL url = new URL("http", host, port, "bdproxy/?appid=xunyou");
                Http http = new Http(10 * 1000, 10 * 1000);
                HttpURLConnection connection = http.createHttpUrlConnection(url, Method.GET, null);
                response = Http.doGet(connection);
            } catch (IOException e) {
                logException(e);
                return PhoneNumberAndPrivateIp.createByException(action, qosSetupRequest, ErrorCode.QOS_THIRD_PROVIDER_IO_ERROR, e);
            } catch (RuntimeException e) {
                logException(e);
                return PhoneNumberAndPrivateIp.createByException(action, qosSetupRequest, ErrorCode.QOS_THIRD_PROVIDER_IO_ERROR, e);
            }
            if (printDebugLog) {
                Log.d(TAG, "Get phone number result:\n" + (response.data == null ? "(null)" : new String(response.data)));
            }
            if (response.code >= 200 && response.code < 300) {
                PhoneNumberAndPrivateIp result = parsePhoneNumberFromResponse(response.data);
                if (printDebugLog) {
                    Log.d(TAG, String.format("Phone number parse: %s", result == null ? "null" : result.toString()));
                }
                if (result == null) {
                    return PhoneNumberAndPrivateIp.createByRawData(action, qosSetupRequest, ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR, response.data);
                } else {
                    return result;
                }
            }
            if (printDebugLog) {
                Log.d(TAG, String.format("Get phone number failed, response code: %d", response.code));
            }
            return PhoneNumberAndPrivateIp.createByRawData(action, qosSetupRequest, ErrorCode.QOS_THIRD_PROVIDER_BASE + response.code, response.data);
        }

        private static PhoneNumberAndPrivateIp parsePhoneNumberFromResponse(byte[] data) {
            if (data == null || data.length == 0) {
                return null;
            }
            String phoneNumber = null;
            String privateIp = null;
            JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(data)));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if ("result".equals(name)) {
                        phoneNumber = JsonUtils.readNextString(reader);
                    } else if ("privateip".equals(name)) {
                        privateIp = JsonUtils.readNextString(reader);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                logException(e);
                return null;
            } catch (RuntimeException e) {
                logException(e);
                return null;
            } finally {
                Misc.close(reader);
            }
            if (TextUtils.isEmpty(phoneNumber)) {
                return null;
            }
            return new PhoneNumberAndPrivateIp(phoneNumber, privateIp);
        }

        private static void logException(Exception e) {
            if (Logger.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Get phone number failed: " + e.getMessage());
            }
        }
    }

    /**
     * 负责从爱唯光石获取Token
     */
    static class SecurityTokenGetter {

        static final String APP_KEY = "6ed68a7c-7ac3-4156-be61-a3cfbdab9c89";

        /**
         * 根据({@link QosParam})决定是否需要securityToken
         */
        public static boolean isTokenRequired(QosParam qosParam) {
            return qosParam != null && qosParam.provider == QosParam.Provider.IVTIME;
        }

        /**
         * 获取Token
         */
        public static Result execute(Action action, QosSetupRequest qosSetupRequest, String host, int port) {
            Result result = doHttpRequest(action, qosSetupRequest, host, port);
            if (Logger.isLoggableDebug(TAG)) {
                Log.d(TAG, "Security Token Get Result: " + result);
            }
            return result;
        }

        private static Result doHttpRequest(Action action, QosSetupRequest qosSetupRequest, String host, int port) {
            Response response;
            try {
                URL url = new URL("http", host, port, "t1?appid=" + APP_KEY);
                HttpURLConnection connection = new Http(10 * 1000, 10 * 1000).createHttpUrlConnection(url, Method.GET, null);
                response = Http.doGet(connection);
            } catch (IOException e) {
                logException(e);
                return Result.createByException(action, ErrorCode.QOS_THIRD_PROVIDER_IO_ERROR, qosSetupRequest, e);
            } catch (RuntimeException e) {
                logException(e);
                return Result.createByException(action, ErrorCode.QOS_THIRD_PROVIDER_IO_RUNTIME_EXCEPTION, qosSetupRequest, e);
            }
            if (response.code >= 200 && response.code < 300) {
                return parseResultFromIVTime(action, qosSetupRequest, response.data);
            } else {
                return Result.createByRawData(action, ErrorCode.QOS_THIRD_PROVIDER_BASE + response.code, qosSetupRequest, response.data);
            }
        }

        private static void logException(Exception e) {
            Logger.w(TAG, "Get security token failed: " + e.getMessage());
        }

        /**
         * 从爱唯光石服务器返回的Response里，解析出Token
         *
         * @throws IOException
         */
        private static Result parseResultFromIVTime(Action action, QosSetupRequest qosSetupRequest, byte[] responseData) {
            if (responseData == null || responseData.length == 0) {
                return Result.createByRawData(action, ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR, qosSetupRequest, responseData);
            }
            String token = null;
            JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(responseData)));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if ("result".equals(name)) {
                        token = reader.nextString();
                    } else if ("error".equals(name)) {
                        if (reader.peek() != JsonToken.NULL) {
                            return Result.createByRawData(action, ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_ERROR_CODE, qosSetupRequest, responseData);
                        }
                        reader.skipValue();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                logException(e);
                token = null;
            } catch (RuntimeException e) {
                logException(e);
                token = null;
            } finally {
                com.subao.common.Misc.close(reader);
            }
            if (TextUtils.isEmpty(token)) {
                return Result.createByRawData(action, ErrorCode.QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR, qosSetupRequest, responseData);
            } else {
                return new Result(token);
            }
        }

        /**
         * 获取安全令牌操作的返回值
         */
        static class Result {
            /**
             * 错误码
             */
            public final int error;

            /**
             * 获取到的安全令牌
             */
            public final String token;

            /**
             * 如果不为NULL，则此event将上报到Message系统
             */
            public final Message_EventMsg.Event event;

            /**
             * 构造一个表示操作成功的Result对象
             *
             * @param token
             */
            public Result(String token) {
                this(0, token, null);
            }

            private Result(int error, String token, Message_EventMsg.Event event) {
                this.error = error;
                this.token = token;
                this.event = event;
            }

            public static Result createByException(Action action, int error, QosSetupRequest qosSetupRequest, Exception e) {
                return new Result(error, null, makeEvent(action, error, qosSetupRequest, e, null));
            }

            public static Result createByRawData(Action action, int error, QosSetupRequest qosSetupRequest, byte[] rawData) {
                return new Result(error, null, makeEvent(action, error, qosSetupRequest, null, rawData));
            }

            private static Message_EventMsg.Event makeEvent(Action action, int error, QosSetupRequest qosSetupRequest, Exception e, byte[] rawData) {
                if (error != 0) {
                    QosEventBuilder qosEventBuilder = new QosEventBuilder(action, error);
                    qosEventBuilder.setQosSetupRequest(qosSetupRequest);
                    qosEventBuilder.setException(e);
                    qosEventBuilder.setRawData(rawData);
                    return qosEventBuilder.build();
                } else {
                    return null;
                }
            }

            @Override
            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }
                if (o == null) {
                    return false;
                }
                if (!(o instanceof Result)) {
                    return false;
                }
                Result other = (Result) o;
                return this.error == other.error
                    && Misc.isEquals(this.token, other.token)
                    && Misc.isEquals(this.event, other.event);
            }

            @Override
            public String toString() {
                return String.format("[error=%d, token=%s, event=%s]",
                    error, token,
                    event == null ? "null" : event.id);
            }
        }
    }

    /**
     * 开启Qos的Requester
     */
    public static class Requester_Open extends Requester {

        private final QosParam qosParam;
        private final QosSetupRequest qosSetup;

        public Requester_Open(Key key, QosParam qosParam, QosSetupRequest qosSetup) {
            super(key);
            this.qosParam = qosParam;
            this.qosSetup = qosSetup;
        }

        @Override
        Action getAction() {
            return Action.OPEN;
        }

        @Override
        PrepareResult prepare() {
            boolean printDebugLog = Logger.isLoggableDebug(TAG);
            //
            RegionAndISP regionAndISP = QosUser4GRegionAndISP.getInstance().getCurrent();
            if (regionAndISP != null) {
                this.qosSetup.setOperator(regionAndISP.toSimpleText());
            }
            if (printDebugLog) {
                Log.d(TAG, "The RegionAndISP is: " + StringUtils.objToString(regionAndISP));
                Log.d(TAG, "The QosParam is: " + StringUtils.objToString(qosParam));
            }
            if (SecurityTokenGetter.isTokenRequired(qosParam)) {
                if (printDebugLog) {
                    Log.d(TAG, "Security token required");
                }
                SecurityTokenGetter.Result tokenResult = SecurityTokenGetter.execute(getAction(), qosSetup, "i.speeed.cn", -1);
                if (tokenResult.error != ErrorCode.OK) {
                    return new PrepareResult(tokenResult.error, tokenResult.event);
                }
                this.qosSetup.setSecurityToken(tokenResult.token);
            } else if (PhoneNumberGetter.doesRequired(qosParam)) {
                if (printDebugLog) {
                    Log.d(TAG, "Phone Number required");
                }
                PhoneNumberAndPrivateIp numberAndPrivateIp = PhoneNumberGetter.execute(
                    getAction(),
                    qosSetup, "120.196.166.113", -1
                );
                if (numberAndPrivateIp.error != ErrorCode.OK) {
                    return new PrepareResult(numberAndPrivateIp.error, numberAndPrivateIp.event);
                }
                this.qosSetup.setPhoneNumber(numberAndPrivateIp.phoneNumber);
                String privateIp = numberAndPrivateIp.privateIp;
                if (privateIp != null && privateIp.length() >= 7) {
                    this.qosSetup.setPrivateIp(privateIp);
                }
            }
            return new PrepareResult(ErrorCode.OK, null);
        }

        @Override
        Method getHttpMethod() {
            return Method.POST;
        }

        @Override
        String buildRequestParam() throws IOException {
            return JsonUtils.serializeToString(qosSetup);
        }

        @Override
        protected CallbackParam buildErrorCallbackParam(int errorCode, Exception e, byte[] rawData) {
            QosEventBuilder qosEventBuilder = makeEventBuilder(errorCode, e, rawData);
            qosEventBuilder.setQosSetupRequest(this.qosSetup);
            return new CallbackParam(key.cid, errorCode, null, null,
                qosSetup.timeLength, qosEventBuilder.build());
        }

        @Override
        CallbackParam buildCallbackParam(Response response) {
            boolean printDebugLog = Logger.isLoggableDebug(TAG);
            if (response.code != HttpURLConnection.HTTP_CREATED) {
                if (printDebugLog) {
                    Log.d(TAG, "QosManager response code " + response.code);
                }
                return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_BASE + response.code, null, response.data);
            }
            // Response Code check ok, is response empty ?
            if (response.data == null || response.data.length == 0) {
                Log.w(TAG, String.format("QosManager return code is %d, but response empty", response.code));
                return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_NULL, null, null);
            }
            //
            Exception exception;
            try {
                QosSetupResponse osr = QosSetupResponse.parseFromJson(response.data);
                if (osr.resultCode != 0) {
                    if (printDebugLog) {
                        Log.d(TAG, "QosManager response body-result-code: " + osr.resultCode);
                    }
                    return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_RESULT_CODE_BASE + osr.resultCode, null, response.data);
                }
                if (TextUtils.isEmpty(osr.sessionId)) {
                    Log.w(TAG, "Parse SessionId from QosManager response is null");
                    return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_DATA_ERROR, null, response.data);
                }
                return new CallbackParam(key.cid, ErrorCode.OK, osr.sessionId, osr.speedingId, this.qosSetup.timeLength, null);
            } catch (IOException e) {
                exception = e;
            } catch (RuntimeException e) {
                exception = e;
            }
            Log.w(TAG, "QosManager response parse error");
            return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_PARSE_ERROR, exception, response.data);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(1024);
            sb.append("[Requester_Open: ");
            try {
                sb.append(buildRequestParam());
            } catch (IOException e) {
                sb.append('?');
            }
            sb.append(']');
            return sb.toString();
        }

    }

    /**
     * 基于会话的{@link Requester}，比如“关闭”和“续订”
     */
    private static abstract class Requester_Session extends Requester {

        public final String sessionId;

        protected Requester_Session(Key key, String sessionId) {
            super(key);
            this.sessionId = sessionId;
        }

        @Override
        String getUrlPath() {
            return super.getUrlPath() + sessionId;
        }

        @Override
        PrepareResult prepare() {
            return new PrepareResult(ErrorCode.OK, null);
        }

    }

    /**
     * 关闭操作的 {@link Requester}
     */
    public static class Requester_Close extends Requester_Session {

        protected Requester_Close(Key key, String sessionId) {
            super(key, sessionId);
        }

        @Override
        Action getAction() {
            return Action.CLOSE;
        }

        @Override
        Method getHttpMethod() {
            return Method.DELETE;
        }

        @Override
        String buildRequestParam() throws IOException {
            return null;
        }

        @Override
        protected CallbackParam buildErrorCallbackParam(int errorCode, Exception e, byte[] rawData) {
            QosEventBuilder builder = makeEventBuilder(errorCode, e, rawData);
            return new CallbackParam(key.cid, errorCode, sessionId, null, 0, builder.build());
        }

        @Override
        CallbackParam buildCallbackParam(Response response) {
            if (response.code == 204) {
                return new CallbackParam(key.cid, 0, sessionId, null, 0, null);
            } else {
                return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_BASE + response.code, null,
                    sessionId == null ? null : sessionId.getBytes());
            }
        }

    }

    /**
     * 续订操作的{@link Requester}
     */
    public static class Requester_Modify extends Requester_Session {

        private final int timeSeconds;
        private String securityToken;

        protected Requester_Modify(Key key, String sessionId, int timeSeconds) {
            super(key, sessionId);
            this.timeSeconds = timeSeconds;
        }

        @Override
        Action getAction() {
            return Action.MODIFY;
        }

        @Override
        Method getHttpMethod() {
            return Method.PUT;
        }

        @Override
        String buildRequestParam() throws IOException {
            QosModifyRequest request = new QosModifyRequest(timeSeconds, securityToken);
            return JsonUtils.serializeToString(request);
        }

        @Override
        PrepareResult prepare() {
            if (SecurityTokenGetter.isTokenRequired(QosUser4GRegionAndISP.getInstance().getQosParam())) {
                SecurityTokenGetter.Result tokenResult = SecurityTokenGetter.execute(getAction(), null, "i.speeed.cn", -1);
                if (tokenResult.error != ErrorCode.OK) {
                    return new PrepareResult(tokenResult.error, tokenResult.event);
                }
                this.securityToken = tokenResult.token;
            }
            return new PrepareResult(ErrorCode.OK, null);
        }

        @Override
        protected CallbackParam buildErrorCallbackParam(int errorCode, Exception e, byte[] rawData) {
            QosEventBuilder builder = makeEventBuilder(errorCode, e, rawData);
            return new CallbackParam(key.cid, errorCode, sessionId, null, timeSeconds, builder.build());
        }

        @Override
        CallbackParam buildCallbackParam(Response response) {
            boolean printDebugLog = Logger.isLoggableDebug(TAG);
            if (response.code != HttpURLConnection.HTTP_OK) {
                if (printDebugLog) {
                    Log.d(TAG, "QosManager response code is " + response.code);
                }
                return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_BASE + response.code, null, response.data);
            }
            if (response.data == null || response.data.length == 0) {
                Log.w(TAG, String.format("QosManager response code is %d, but body empty", response.code));
                return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_NULL, null, null);
            }
            Exception exception;
            try {
                QosResponse qosResponse = QosResponse.parseFromJson(new ByteArrayInputStream(response.data));
                if (qosResponse.resultCode == 0) {
                    return new CallbackParam(key.cid, 0, this.sessionId, null, this.timeSeconds, null);
                } else {
                    return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_RESULT_CODE_BASE + qosResponse.resultCode, null, response.data);
                }
            } catch (IOException e) {
                exception = e;
            } catch (RuntimeException e) {
                exception = e;
            }
            return buildErrorCallbackParam(ErrorCode.QOS_MANAGER_RESPONSE_PARSE_ERROR, exception, response.data);
        }

    }

    /**
     * 回调参数
     */
    public static class CallbackParam {

        public final int cid;

        /**
         * 错误码
         */
        public final int error;

        /**
         * Session ID
         */
        public final String sessionId;

        /**
         * 加速平台分配的加速标识（Speed Id）
         */
        public final String speedId;

        /**
         * 提速时长
         */
        public final int timeLength;

        /**
         * 需要上报的Event
         */
        public final Message_EventMsg.Event event;

        public CallbackParam(int cid, int error, String sessionId, String speedId, int timeLength, Message_EventMsg.Event event) {
            this.cid = cid;
            this.error = error;
            this.sessionId = sessionId;
            this.speedId = speedId;
            this.timeLength = timeLength;
            this.event = event;
        }

//        /**
//         * 用给定的错误码和{@link com.subao.common.msg.Message_EventMsg.Event}创建一个实例
//         */
//        public static CallbackParam createWhenError(int id, int error, Message_EventMsg.Event event) {
//            return new CallbackParam(id, error, null, null, 0, event);
//        }
//
//        public static CallbackParam createWhenException(int id, int error, Exception e) {
//            QosEventBuilder qosEventBuilder = new QosEventBuilder(error);
//            qosEventBuilder.setException(e);
//        }

        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "[cid=%d, Error=%d, SessionId=%s, SpeedId=%s, TimeLength=%d]",
                this.cid, this.error, this.sessionId, this.speedId, this.timeLength);
        }
    }

    /**
     * 执行Qos请求的线程（AsyncTask）
     */
    abstract static class Worker extends AsyncTask<Void, Void, CallbackParam> {

        private final String serverHost;
        private final int serverPort;
        private final Requester requester;
        private final Callback callback;

        public Worker(String serverHost, int serverPort, Requester requester, Callback callback) {
            this.serverHost = serverHost;
            this.serverPort = serverPort;
            this.requester = requester;
            this.callback = callback;
        }

        private static String buildWSSE() {
            String timestamp = AuthUtils.generateTimestamp();
            String nonce = AuthUtils.generateNonce();
            String digest;
            try {
                byte[] sha1 = AuthUtils.generateSHA1(nonce, timestamp, "Qos_Pass_517");
                digest = Base64.encodeToString(sha1, Base64.NO_WRAP);
            } catch (NoSuchAlgorithmException e) {
                digest = "";
            }
            return String.format("UsernameToken Username=\"%s\", PasswordDigest=\"%s\", Nonce=\"%s\", Created=\"%s\"",
                "subaoSdk",
                digest,
                nonce, timestamp);
        }

        static void setAuthParam(HttpURLConnection connection) {
            connection.setRequestProperty("Authorization", "WSSE profile=\"UsernameToken\"");
            connection.setRequestProperty("X-WSSE", buildWSSE());
        }

        private Response executeHttpRequest() throws IOException {
            URL url = new URL("http", serverHost, serverPort, requester.getUrlPath());
            boolean loggableDebug = Logger.isLoggableDebug(TAG);
            if (loggableDebug) {
                Log.d(TAG, "Try to request: " + url.toString());
            }
            Http http = new Http(10 * 1000, 10 * 1000);
            Method httpMethod = requester.getHttpMethod();
            HttpURLConnection connection = http.createHttpUrlConnection(url, httpMethod, Http.ContentType.JSON.str);
            setAuthParam(connection);
            connection.addRequestProperty("Access-Token", requester.key.accessToken);
            //
            switch (httpMethod) {
            case POST:
            case PUT:
                String requestParam = requester.buildRequestParam();
                if (loggableDebug) {
                    Log.d(TAG, String.format("Execute HTTP %s: %s", httpMethod.str, requestParam));
                }
                return Http.doPost(connection, requestParam == null ? null : requestParam.getBytes());
            default:
                if (loggableDebug) {
                    Log.d(TAG, String.format("Execute HTTP %s", httpMethod.str));
                }
                return Http.doGet(connection);
            }
        }

        protected final CallbackParam executeInBackground() {
            Requester.PrepareResult prepareResult = requester.prepare();
            if (prepareResult != null && prepareResult.errorCode != 0) {
                int errorCode = prepareResult.errorCode;
                Log.w(TAG, String.format("%s prepare return error: %d", requester.getAction().getDesc(), errorCode));
                return new CallbackParam(requester.key.cid, errorCode, null, null, 0, prepareResult.event);
            }
            Response httpResponse;
            int error;
            Exception exception;
            try {
                httpResponse = executeHttpRequest();
                return requester.buildCallbackParam(httpResponse);
            } catch (IOException e) {
                exception = e;
                error = ErrorCode.QOS_MANAGER_IO_BASE;
            } catch (RuntimeException e) {
                exception = e;
                error = ErrorCode.QOS_MANAGER_IO_RUNTIME_EXCEPTION;
            }
            Logger.w(TAG, exception.getMessage());
            return requester.buildErrorCallbackParam(error, exception, null);
        }

        protected void notifyCallback(CallbackParam result) {
            if (callback != null) {
                callback.onQosResult(requester.getAction(), this.serverPort, result);
            }
        }
    }

    private static class Worker_CallbackMainThread extends Worker {

        public Worker_CallbackMainThread(String serverHost, int serverPort, Requester requester, Callback callback) {
            super(serverHost, serverPort, requester, callback);
        }

        @Override
        protected CallbackParam doInBackground(Void... params) {
            return executeInBackground();
        }

        @Override
        protected void onPostExecute(CallbackParam result) {
            notifyCallback(result);
        }
    }

    public static class WorkerFactory {
        public static Worker create(String serverHost, int serverPort, Requester requester, Callback callback) {
            return new Worker_CallbackMainThread(serverHost, serverPort, requester, callback);
        }
    }

}
