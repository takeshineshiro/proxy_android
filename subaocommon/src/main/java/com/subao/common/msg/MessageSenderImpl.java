package com.subao.common.msg;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.JsonWriter;
import android.util.Log;

import com.subao.common.JsonSerializable;
import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.data.ParallelConfigDownloader;
import com.subao.common.data.RegionAndISP;
import com.subao.common.data.ServiceLocation;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.msg.Message_Link.Network;
import com.subao.common.net.Http;
import com.subao.common.net.Http.Method;
import com.subao.common.net.Http.Response;
import com.subao.common.net.NetTypeDetector.NetType;
import com.subao.common.net.NetUtils;
import com.subao.common.qos.QosUser4GRegionAndISP;
import com.subao.common.utils.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MessageSenderImpl implements MessageSender {

    private static final String TAG = LogTag.MESSAGE;

    final MessageHandler messageHandler;

    private boolean alreadyQuit;

    private MessageSenderImpl(ServiceLocation serviceLocation, MessageTools tools) {
        this.messageHandler = new MessageHandler(serviceLocation, tools);
    }

    public static MessageSender create(ServiceLocation serviceLocation, MessageTools tools) {
        MessageSenderImpl messageSender = new MessageSenderImpl(serviceLocation, tools);
        messageSender.messageHandler.post(messageSender.messageHandler.new Runnable_ProcessLastLinkMessage());
        return messageSender;
    }

    private static byte[] serializableMessage(JsonSerializable js) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(buf));
        try {
            js.serialize(writer);
        } finally {
            Misc.close(writer);
        }
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, buf.toString());
        }
        return buf.toByteArray();
    }

    /**
     * 判断王者荣耀传来的<br />
     * {@link Message_Link.DelayQuality}<br />
     * <br />或<br />
     * {@link Message_Link.DelayQualityV2}<br />
     * 是否需要上报
     *
     * @return true表示需要上报，false表示不需要
     */
    static boolean doesDelayQualityFeedbackNeedReport() {
        int userStatus = MessageUserId.getCurrentUserStatus();
        return userStatus == 2 || userStatus == 4 || userStatus == 6;
    }

    void setHttpTimeout(int milliseconds) {
        this.messageHandler.setHttpTimeout(milliseconds);
    }

    void quit() {
        boolean alreadyQuit;
        synchronized (this) {
            alreadyQuit = this.alreadyQuit;
            this.alreadyQuit = true;
        }
        if (!alreadyQuit) {
            this.messageHandler.getLooper().quit();
        }
    }

    String getGameServerId() {
        return this.messageHandler.getGameServerId();
    }

    @Override
    public void setGameServerId(String gameServerId) {
        this.messageHandler.setGameServerId(gameServerId);
    }

    FreeFlowType getFreeFlowType() {
        return this.messageHandler.getFreeFlowType();
    }

    @Override
    public void setFreeFlowType(FreeFlowType type) {
        this.messageHandler.setFreeFlowType(type);
    }

    @Override
    public void offerInstallation(Message_Installation msg) {
        messageHandler.post(messageHandler.new Performer_Installation(msg));
    }

    @Override
    public void offerUpgrade(Message_Upgrade msg) {
        messageHandler.post(messageHandler.new Performer_Upgrade(msg));
    }

    @Override
    public void offerStart(int nodeNum, int gameNum, List<Message_App> appList) {
        messageHandler.post(messageHandler.new Performer_Start(nodeNum, gameNum, appList));
    }

    @Override
    public void offerEvent(String eventName, String eventParam) {
        messageHandler.post(messageHandler.new Performer_EventByNameValue(eventName, eventParam));
    }

    @Override
    public void offerEvent(Message_EventMsg event) {
        messageHandler.post(messageHandler.new Performer_EventEntire(event));
    }

    @Override
    public void offerEvent(Message_EventMsg.Event event) {
        if (event != null) {
            messageHandler.post(messageHandler.new Performer_EventBody(event));
        }
    }

    @Override
    public void offerStructureEvent(String structureEvent) {
        messageHandler.post(messageHandler.new Performer_EventStructure(structureEvent));
    }

    @Override
    public void onJNILinkMsg(String messageId, String messageBody, boolean finish) {
        if (TextUtils.isEmpty(messageId)) {
            Log.w(TAG, "Empty or Null message id");
            return;
        }
        if (messageBody == null) {
            Log.w(TAG, "Null Message Body");
            return;
        }
        if (!finish && Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, String.format("onLinkMsg, id=%s, finish=%b, body:\n%s",
                messageId, finish, messageBody));
        }
        byte[] body = messageBody.getBytes();
        try {
            messageHandler.tools.getMessagePersistent().save(messageId, body);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (finish) {
            messageHandler.post(messageHandler.new Performer_Link(messageId, body));
        }
    }


//    @Override
//    public void sendLinkMsg(String messageId, byte[] messageBody, SendLinkMsgCallback callback) {
//        messageHandler.post(messageHandler.new Performer_Link(messageId, messageBody, callback));
//    }

    @Override
    public void onJNINetMeasureMsg(String jsonFromJNI) {
        if (jsonFromJNI != null && jsonFromJNI.length() > 0) {
            messageHandler.post(messageHandler.new Performer_NetMeasure(jsonFromJNI));
        } else {
            Log.w(TAG, "Empty or Null NetMeasurement from JNI");
        }
    }

    @Override
    public void onJNIQosMsg(String jsonFromJNI) {
        if (jsonFromJNI != null && jsonFromJNI.length() > 0) {
            messageHandler.post(messageHandler.new Performer_Qos(jsonFromJNI));
        } else {
            Log.w(TAG, "Empty or Null Qos from JNI");
        }
    }

    @Override
    public void onNetDelayQualityV2(Message_Link.DelayQualityV2 delayQualityV2) {
        if (doesDelayQualityFeedbackNeedReport()) {
            messageHandler.post(messageHandler.new Performer_DelayQualityV2Feedback(delayQualityV2));
        }
    }

    /**
     * 处理消息的队列，运行在“非主线程”里
     * <p>
     * 所有消息都放在这个线程的队列里，串行执行
     * </p>
     */
    static class MessageHandler extends Handler {

        //final Defines.ModuleType moduleType;
        final ServiceLocation serviceLocation;
        final MessageTools tools;
        final Message_DeviceInfo deviceInfo;
        /**
         * SDK自身（或加速器自身）的{@link Message_App}
         */
        private final Message_App messageAppSelf;
        /**
         * HTTP请求的超时值
         */
        private int httpTimeout = 7 * 1000;
        /**
         * SDK使用的：游戏区服列表
         */
        private volatile String gameServerId;
        /**
         * 王者荣耀SDK使用的：免流用户类型
         */
        private volatile FreeFlowType freeFlowType;

        MessageHandler(/*Defines.ModuleType moduleType,*/ ServiceLocation serviceLocation, MessageTools tools) {
            super(buildLooper());
            //this.moduleType = moduleType;
            this.serviceLocation = serviceLocation;
            this.tools = tools;
            this.messageAppSelf = MessageBuilder.buildMessageApp(tools.getContext());
            this.deviceInfo = new Message_DeviceInfo(tools.getContext());
        }

        private static Looper buildLooper() {
            HandlerThread handlerThread = new HandlerThread("subao_mu");
            handlerThread.start();
            return handlerThread.getLooper();
        }

        void setHttpTimeout(int milliseconds) {
            this.httpTimeout = milliseconds;
        }

        String getGameServerId() {
            return this.gameServerId;
        }

        void setGameServerId(String value) {
            this.gameServerId = value;
        }

        FreeFlowType getFreeFlowType() {
            return this.freeFlowType;
        }

        void setFreeFlowType(FreeFlowType type) {
            this.freeFlowType = type;
        }

        private Network createMessageNetwork() {
            return new Network(getCurrentNetworkType(), NetUtils.getCurrentNetName(tools.getContext(), tools.getNetTypeDetector()));
        }

        private Message_Link.NetworkType getCurrentNetworkType() {
            NetType nt = tools.getNetTypeDetector().getCurrentNetworkType();
            if (nt == null) {
                return Message_Link.NetworkType.UNKNOWN_NETWORKTYPE;
            }
            switch (nt) {
            case MOBILE_2G:
                return Message_Link.NetworkType.MOBILE_2G;
            case MOBILE_3G:
                return Message_Link.NetworkType.MOBILE_3G;
            case MOBILE_4G:
                return Message_Link.NetworkType.MOBILE_4G;
            case WIFI:
                return Message_Link.NetworkType.WIFI;
            default:
                return Message_Link.NetworkType.UNKNOWN_NETWORKTYPE;
            }
        }

        /**
         * 被投到主线程里执行的Runnable，负责设置SubaoId
         */
        private static class SubaoIdSetter implements Runnable {

            private final String subaoId;

            SubaoIdSetter(String subaoId) {
                this.subaoId = subaoId;
            }

            @Override
            public void run() {
                SubaoIdManager.getInstance().setSubaoId(this.subaoId);
            }
        }

        /**
         * 负责上传消息的抽象基类
         */
        abstract class Performer implements Runnable {

            /**
             * 名字，只是用来显示日志的时候比较好区分而已，无其它用途
             */
            public final String name;

            /**
             * 要Post到服务器的数据
             */
            private byte[] postData;

            /**
             * 要Post到哪个URL？
             */
            private URL url;

            Performer(String name) {
                this.name = name;
            }

            @Override
            public void run() {
                Method httpMethod = getHttpMethod();
                if (httpMethod == null) {
                    // 错误：没有提供有效的HttpMethod
                    throw new NullPointerException("Null HTTP method");
                }
                try {
                    URL url = buildURLIfNeed();
                    Http http = new Http(httpTimeout, httpTimeout);
                    HttpURLConnection connection = http.createHttpUrlConnection(url, httpMethod, Http.ContentType.JSON.str);
                    try {
                        Http.setRequestContentType(connection, Http.ContentType.JSON.str);
                        Http.Response response;
                        switch (httpMethod) {
                        case DELETE:
                        case GET:
                            response = Http.doGet(connection);
                            break;
                        default:
                            byte[] postData;
                            if (doesPostDataNeedCache()) {
                                if (this.postData == null) {
                                    this.postData = buildPostData();
                                }
                                postData = this.postData;
                            } else {
                                postData = buildPostData();
                            }
                            response = Http.doPost(connection, postData);
                            break;
                        }
                        onHttpResponse(response);
                    } finally {
                        connection.disconnect();
                    }
                } catch (IOException e) {
                    onException(e);
                } catch (RuntimeException e) {
                    // 在某些设备上，可能会古怪地抛出RuntimeException
                    onException(e);
                }
            }

            private URL buildURLIfNeed() throws IOException {
                if (url == null) {
                    String httpResName = getHttpResName();
                    url = new URL(serviceLocation.protocol, serviceLocation.host,
                        serviceLocation.port, httpResName == null ? "" : httpResName);
                }
                return url;
            }

            private void onException(Exception e) {
                onIOException();
            }

            /**
             * 将自己再次投递到队列中，以便下次再执行
             */
            final void postSelfDelayed(long millis) {
                MessageHandler.this.postDelayed(this, millis);
            }

            /**
             * 派生类决定，用啥协议？ <br />
             * （大多数消息用POST，也有用DELETE的）
             */
            protected Http.Method getHttpMethod() {
                return Http.Method.POST;
            }

            /**
             * 派生类实现：返回请求URL的资源名<br />
             * 例如：http://www.example.com:80/hello/world/这个URL的资源名即指：</br />
             * <b><i>"/hello/world/"</i></b><br />
             * <b>注意：这个函数只会被调用一次，结果会被缓存的</b>
             */
            protected abstract String getHttpResName();

            /**
             * 派生类实现：生成要POST的数据<br />
             * <b>注意：默认情况下这个操作只会被调用一次，生成的数据会缓存！</b>
             *
             * @see #doesPostDataNeedCache()
             */
            protected abstract byte[] buildPostData() throws IOException;

            /**
             * 本实现的{@link #buildPostData()}生成的数据是否需要被缓存？
             */
            boolean doesPostDataNeedCache() {
                return true;
            }

            /**
             * 派生类实现：当IO异常的时候，怎么办？
             */
            protected abstract void onIOException();

            /**
             * 派生类实现：当服务器回应的时候，怎么处理？
             */
            protected abstract void onHttpResponse(Http.Response response);

        }

        /**
         * 处理前一次缓存在本地的Link消息
         */
        class Runnable_ProcessLastLinkMessage implements Runnable {
            @Override
            public void run() {
                List<MessagePersistent.Message> list = tools.getMessagePersistent().loadList(50);
                if (list == null || list.isEmpty()) {
                    Logger.d(TAG, "No cached link message(s)");
                    return;
                }
                for (MessagePersistent.Message msg : list) {
                    Performer performer = new Performer_Link(msg.messageId, msg.messageBody);
                    post(performer);
                }
            }
        }

        /**
         * 负责发送Intallation消息
         */
        class Performer_Installation extends Performer {

            private final Message_Installation msg;
            private int retryDelaySeconds = 10;

            Performer_Installation(Message_Installation msg) {
                super("Installation");
                this.msg = msg;
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/installation/android";
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                return serializableMessage(msg);
            }

            @Override
            protected void onIOException() {
                retryNext();
            }

            @Override
            protected void onHttpResponse(Response response) {
                switch (response.code) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_CREATED:
                    String subaoId = MessageJsonUtils.parseSubaoIdFromJson(response.data);
                    if (subaoId != null) {
                        tools.runInMainThread(new SubaoIdSetter(subaoId));
                    }
                    break;
                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    retryNext();
                    break;
                }
                // 其它情况直接丢弃本消息了
            }

            // 按 10, 20, 40, 80, 160, 320 秒的节奏重试最多6次
            @SuppressLint("DefaultLocale")
            private void retryNext() {
                if (retryDelaySeconds <= 320) {
                    if (Logger.isLoggableDebug(TAG)) {
                        Log.d(TAG, String.format("Installation message post failed, retry after %d seconds", retryDelaySeconds));
                    }
                    postSelfDelayed(retryDelaySeconds * 1000);
                    retryDelaySeconds *= 2;
                } else {
                    Logger.d(TAG, "Retry stopped");
                }
            }
        }

        /**
         * 一个抽象基类：所有在失败后需要重试N次（间隔T秒）的，从此派生
         */
        private abstract class Performer_CanRetry extends Performer {
            private final int maxRetryCount;
            /**
             * 每一次重试是否将Delay翻倍?
             */
            private final boolean doubleDelayRetry;
            private long delayMills;
            private int retryCount;

            Performer_CanRetry(String name, int maxRetryCount) {
                this(name, maxRetryCount, 10 * 1000);
            }

            Performer_CanRetry(String name, int maxRetryCount, long delayMills) {
                this(name, maxRetryCount, delayMills, false);
            }

            Performer_CanRetry(String name, int maxRetryCount, long delayMills, boolean doubleDelayRetry) {
                super(name);
                this.maxRetryCount = maxRetryCount;
                this.delayMills = delayMills;
                this.doubleDelayRetry = doubleDelayRetry;
            }

            final boolean retryNext() {
                ++retryCount;
                if (retryCount <= maxRetryCount) {
                    this.postSelfDelayed(delayMills);
                    if (doubleDelayRetry) {
                        delayMills *= 2;
                    }
                    if (Logger.isLoggableDebug(TAG)) {
                        Log.d(TAG, String.format("[%s] retry after %d milliseconds (%d/%d)", this.name, delayMills, retryCount, maxRetryCount));
                    }
                    return true;
                }
                return false;
            }

            @Override
            protected void onIOException() {
                retryNext();
            }

            @Override
            protected void onHttpResponse(Response response) {
                if (response.code == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    retryNext();
                }
            }
        }

        /**
         * 只重复一次，重试间隔为10秒的Invoker
         */
        private abstract class Performer_RetryOnce extends Performer_CanRetry {

            Performer_RetryOnce(String name) {
                super(name, 1, 10 * 1000);
            }
        }

        /**
         * 负责发送Start消息
         * <p>
         * Start消息每天只发一次
         * </p>
         */
        class Performer_Start extends Performer_RetryOnce {

            private final int nodeNum, gameNum;
            private final List<Message_App> appList;

            Performer_Start(int nodeNum, int gameNum, List<Message_App> appList) {
                super("Start");
                this.nodeNum = nodeNum;
                this.gameNum = gameNum;
                this.appList = appList;
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/start/android";
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                Message_Start msg = tools.getMessageBuilder().buildMessageStart(
                    MessageUserId.build(), nodeNum,
                    gameNum, appList);
                return serializableMessage(msg);
            }

            /**
             * 处理成功的Response
             */
            private void onHttpResponseCreated(byte[] data) {
                tools.onMessageStartSent();
                String subaoId = MessageJsonUtils.parseSubaoIdFromJson(data);
                if (SubaoIdManager.isSubaoIdValid(subaoId)) {
                    // 服务器返回了有效的SubaoId，记录下来
                    if (Logger.isLoggableDebug(TAG)) {
                        Log.d(TAG, "Response of 'start': subaoId=" + subaoId);
                    }
                    tools.runInMainThread(new SubaoIdSetter(subaoId));
                } else {
                    Logger.d(TAG, "Response of 'start', subaoId is invalid");
                    // 服务器返回无效的SubaoId，需要重新发送Installation消息
                    tools.runInMainThread(new Runnable() {
                        @Override
                        public void run() {
                            SubaoIdManager.getInstance().setSubaoId(null);
                            Message_Installation msg = tools.getMessageBuilder().buildMessageInstallation(
                                System.currentTimeMillis() / 1000,
                                Message_Installation.UserInfo.create(tools.getContext()),
                                null);
                            MessageHandler.this.post(new Performer_Installation(msg));
                        }
                    });
                }
            }

            @Override
            protected void onHttpResponse(Response response) {
                switch (response.code) {
                case HttpURLConnection.HTTP_CREATED:
                    onHttpResponseCreated(response.data);
                    break;
                default:
                    super.onHttpResponse(response);
                    break;
                }
            }

        }

        /**
         * 负责发送Upgrade消息
         */
        class Performer_Upgrade extends Performer_RetryOnce {
            private final Message_Upgrade msg;

            Performer_Upgrade(Message_Upgrade msg) {
                super("Upgrade");
                this.msg = msg;
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/upgrade/android";
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                return serializableMessage(msg);
            }
        }

        class Performer_NetMeasure extends Performer_RetryOnce {

            private final String jsonFromJNI;

            Performer_NetMeasure(String jsonFromJNI) {
                super("NetMeasure");
                this.jsonFromJNI = jsonFromJNI;
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/network/measurement";
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                MessageUserId msgUserId = MessageUserId.build();
                if (msgUserId.isSubaoIdValid()) {
                    String translated = MessageJsonUtils.insertObjectToJson(jsonFromJNI, "id", msgUserId);
                    if (Logger.isLoggableDebug(TAG)) {
                        Log.d(TAG, "NetMeasure V2");
                        Log.d(TAG, translated);
                    }
                    return translated.getBytes();
                } else {
                    throw new IOException("Invalid Subao Id");
                }
            }

        }

        class Performer_Qos extends Performer_RetryOnce {

            private final String jsonFromJNI;

            Performer_Qos(String jsonFromJNI) {
                super("Qos");
                this.jsonFromJNI = jsonFromJNI;
                if (Logger.isLoggableDebug(TAG)) {
                    Log.d(TAG, "Perform Qos Message:\n" + this.jsonFromJNI);
                }
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/qos";
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                return jsonFromJNI.getBytes();
            }

        }

        /**
         * Link 消息上报
         */
        private class Performer_Link extends Performer_RetryOnce {

            private final String messageId;
            private final byte[] messageBody;

            Performer_Link(String messageId, byte[] messageBody) {
                super("Link");
                this.messageId = messageId;
                this.messageBody = messageBody;
                //
                if (Logger.isLoggableDebug(TAG)) {
                    Log.d(TAG, String.format("Perform Link Message: id=%s, body:\n%s",
                        messageId, new String(messageBody)));
                }
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/gaming/link";
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                return this.messageBody;
            }

            @Override
            protected void onHttpResponse(Response response) {
                if (response.code == 500) {
                    retryNext();
                } else if (response.code == 201 || response.code == 400) {
                    tools.getMessagePersistent().delete(messageId);
                }
            }
        }

        /**
         * GameDone
         */
        private class Performer_GameDone extends Performer_RetryOnce {

            private final String sessionId;

            Performer_GameDone(String sessionId) {
                super("GameDone");
                this.sessionId = sessionId;
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/gaming/" + sessionId;
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                return null;
            }

            @Override
            protected void onHttpResponse(Response response) {
            }

            @Override
            protected Http.Method getHttpMethod() {
                return Http.Method.DELETE;
            }
        }

        private abstract class Performer_DelayQualityFeedbackBase extends Performer_RetryOnce {

            private final JsonSerializable feedback;
            private final long time;

            Performer_DelayQualityFeedbackBase(String name, JsonSerializable feedback) {
                super(name);
                this.feedback = feedback;
                this.time = System.currentTimeMillis();
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/feedback";
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                MessageUserId messageUserId = MessageUserId.build();
                StringWriter stringWriter = new StringWriter(1024);
                JsonWriter writer = new JsonWriter(stringWriter);
                writer.beginObject();

                JsonUtils.writeSerializable(writer, "id", messageUserId);
                writer.name("time").value(this.time / 1000);
                MessageJsonUtils.serializeEnum(writer, "type", tools.getAppType());
                JsonUtils.writeSerializable(writer, "game", messageAppSelf);
                JsonUtils.writeSerializable(writer, "device", deviceInfo);
                JsonUtils.writeSerializable(writer, "version", tools.getMessageBuilder().getVersionInfo());
                JsonUtils.writeSerializable(writer, "network", createMessageNetwork());
                JsonUtils.writeSerializable(writer, "feedback", this.feedback);
                Message_Link.QosAccelInfo qosAccelInfo = createQosAccelInfo();
                Message_Link.WiFiAccelInfo wiFiAccelInfo = createWiFiAccelInfo(MessageUserId.getCurrentUserConfig());
                JsonUtils.writeSerializable(writer, "accelInfo",
                    new Message_Link.AccelInfo(qosAccelInfo, wiFiAccelInfo, null));
                writer.endObject();
                Misc.close(writer);
                String json = stringWriter.toString();
                if (Logger.isLoggableDebug(TAG)) {
                    Log.d(TAG, this.name);
                    Log.d(TAG, json);
                }
                return json.getBytes();
            }

            /**
             * 根据当前环境，填充{@link Message_Link.QosAccelInfo}
             * <p>（由于无法取得本局是否启用了Qos加速，所有一些字段无效）</p>
             *
             * @return {@link Message_Link.QosAccelInfo}，如果当前不是4G，则返回null
             */
            private Message_Link.QosAccelInfo createQosAccelInfo() {
                if (tools.getNetTypeDetector().getCurrentNetworkType() == NetType.MOBILE_4G) {
                    QosUser4GRegionAndISP qosUser4GRegionAndISP = QosUser4GRegionAndISP.getInstance();
                    String isp = RegionAndISP.toText(qosUser4GRegionAndISP.getCurrent());
                    boolean supported = qosUser4GRegionAndISP.getQosParam() != null;
                    return new Message_Link.QosAccelInfo(supported, false, null, isp);
                } else {
                    return null;
                }
            }

            /**
             * 根据当前环境，填充{@link Message_Link.WiFiAccelInfo}
             */
            private Message_Link.WiFiAccelInfo createWiFiAccelInfo(String userConfig) {
                // 机型是否支持？
                boolean support = ParallelConfigDownloader.isPhoneParallelSupported();
                // 用户配置是否打开？
                boolean open = (userConfig != null && userConfig.length() >= 2 && userConfig.charAt(1) == '1');
                return new Message_Link.WiFiAccelInfo(support, open, null, null);
            }
        }

        /**
         * 王者荣耀v17新增 (2016.12.12 by YHB)
         */
        private class Performer_DelayQualityV2Feedback extends Performer_DelayQualityFeedbackBase {
            Performer_DelayQualityV2Feedback(Message_Link.DelayQualityV2 feedback) {
                super("DelayQualityV2Feedback", feedback);
            }
        }

        /**
         * 上报Event（基类）
         */
        abstract class Performer_Event extends Performer_CanRetry {

            protected Performer_Event() {
                super("Event", 10);
            }

            @Override
            protected String getHttpResName() {
                return "/v3/report/client/event";
            }

        }

        /**
         * 上报已构造好的{@link Message_EventMsg}对象
         */
        class Performer_EventEntire extends Performer_Event {

            private Message_EventMsg msg;

            Performer_EventEntire(Message_EventMsg msg) {
                this.msg = msg;
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                if (this.msg != null) {
                    byte[] result = serializableMessage(this.msg);
                    this.msg = null;
                    if (Logger.isLoggableDebug(TAG)) {
                        Log.d(TAG, "MessageEvent: " + new String(result));
                    }
                    return result;
                } else {
                    return null;
                }
            }
        }

        /**
         * 上报Event。调用者提供事件名和事件内容
         */
        class Performer_EventByNameValue extends Performer_Event {
            private String eventName;
            private String eventParam;

            Performer_EventByNameValue(String eventName, String eventParam) {
                this.eventName = eventName;
                this.eventParam = eventParam;
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                if (eventName != null && eventParam != null) {
                    Message_EventMsg msg = tools.getMessageBuilder().buildMessageEvent(
                        MessageUserId.build(),
                        eventName, eventParam);
                    eventName = eventParam = null;
                    byte[] result = serializableMessage(msg);
                    if (Logger.isLoggableDebug(TAG)) {
                        Log.d(TAG, "MessageEvent: " + new String(result));
                    }
                    return result;
                } else {
                    return null;
                }
            }
        }

        /**
         * 上报已经构建好的事件
         */
        class Performer_EventStructure extends Performer_Event {

            private final byte[] postData;

            Performer_EventStructure(String structureEvent) {
                this.postData = TextUtils.isEmpty(structureEvent) ? null : structureEvent.getBytes();
                if (Logger.isLoggableDebug(TAG)) {
                    Log.d(TAG, "MessageEvent: " + structureEvent);
                }
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                return this.postData;
            }
        }

        class Performer_EventBody extends Performer_Event {

            private Message_EventMsg.Event event;

            Performer_EventBody(Message_EventMsg.Event event) {
                this.event = event;
            }

            @Override
            protected byte[] buildPostData() throws IOException {
                if (event != null) {
                    List<Message_EventMsg.Event> events = new ArrayList<Message_EventMsg.Event>(1);
                    events.add(event);
                    Message_EventMsg msg = tools.getMessageBuilder().buildMessageEvent(
                        MessageUserId.build(), events
                    );
                    event = null;
                    return serializableMessage(msg);
                } else {
                    return null;
                }
            }
        }

    }


}
