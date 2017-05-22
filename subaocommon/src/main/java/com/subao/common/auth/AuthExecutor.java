package com.subao.common.auth;

import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.data.Defines;
import com.subao.common.data.ServiceLocation;
import com.subao.common.io.Persistent;
import com.subao.common.msg.MessageEvent;
import com.subao.common.msg.MessageUserId;
import com.subao.common.net.ResponseCallback;
import com.subao.common.utils.CalendarUtils;
import com.subao.common.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 负责执行鉴权操作
 */
public class AuthExecutor {

    private static final String TAG = LogTag.AUTH;
    private static final UserConfigList userConfigList = new UserConfigList();
    private static Cache cache;

    /**
     * 预检查。如果有网络连接直接返回True，否则调用{@link ResponseCallback#doNetDisconnected()} 并返回false
     *
     * @param netConnected 当前网络是否连接
     * @param callback     {@link ResponseCallback}
     * @return true表示预检查通过，可以执行
     */
    static boolean preCheck(boolean netConnected, ResponseCallback callback) {
        if (netConnected) {
            return true;
        } else {
            callback.doNetDisconnected();
            return false;
        }
    }

    /**
     * 使用AuthExecutor之前必须首先调用本函数，初始化
     *
     * @param serviceLocation 服务资源位置
     * @param clientType      客户端类型（GUID或“Android”）
     * @param cachePersistent 持久化缓存
     */
    public static void init(ServiceLocation serviceLocation, String clientType, Persistent cachePersistent) {
        AuthService.init(serviceLocation, clientType);
        cache = new Cache(null); // 注：本版本暂时不进行持久化处理，只在内存里进行缓存 (cachePersistent);
    }

    /**
     * 使用HTTP还是HTTPS协议？
     *
     * @param useHttpProtocol true表示使用HTTP协议
     */
    public static void setProtocol(boolean useHttpProtocol) {
        AuthService.setProtocol(useHttpProtocol);
    }

    /**
     * 取最近一次从网上取得UserConfig
     */
    public static String getCachedUserConfig(String userId) {
        return userConfigList.getConfigString(userId);
    }

    /**
     * 鉴权（取JWTToken）
     *
     * @param controller     {@link Controller}
     * @param cid
     * @param userId         用户ID
     * @param token          游戏用户Token （SDK）
     * @param appId          游戏的AppID （SDK）
     * @param resultReceiver 结果回调
     */
    public static void getJWTToken(Controller controller, int cid, final String userId, String token, String appId, final AuthResultReceiver resultReceiver) {
        // 先从Cache取
//        JWTTokenResp cached = cache.getConfigString(userId);
//        if (cached != null) {
//            Logger.d(TAG, "JWTToken cache got.");
//            onJWTTokenGot(cached, 201, userId, resultReceiver);
//            return;
//        }
        // Cache里没有，向服务器申请
        ResponseCallback callback = new ResponseCallback(controller.getEventReporter(), cid) {
            @Override
            protected void onSuccess(int code, byte[] response) {
                AuthErrorCounter.remove(userId);
                if (response != null && response.length > 2) {
                    try {
                        JWTTokenResp jwtToken = JWTTokenResp.createFromJson(new ByteArrayInputStream(response));
                        cache.set(userId, jwtToken);
                        onJWTTokenGot(this.cid, jwtToken, code, userId, resultReceiver);
                        reportSuccessEvent();
                        return;
                    } catch (IOException e) {
                        // fall down
                    }
                }
                doFail(-1, null);
            }

            @Override
            protected void onFail(int code, byte[] responseData) {
                cache.set(userId, null);
                MessageUserId.resetUserInfo(userId);
                boolean allowRetry = true;
                if (code > 0) {
                    allowRetry = (AuthErrorCounter.UpdateResult.TOO_MANY_ERROR != AuthErrorCounter.update(userId, 2));
                }
                if (resultReceiver != null) {
                    if (Defines.moduleType != Defines.ModuleType.SDK) {
                        if (!allowRetry) {
                            code = 401; // FIXME 暂时用这种办法，在APP里防止JNI重试超过两次
                        }
                    }
                    resultReceiver.onGetJWTTokenResult(this.cid, null, -1, null, 0, null, false, code);
                }
            }

            @Override
            protected String getEventName() {
                return "auth_get_jwt_token";
            }

        };
        //
        if (preCheck(controller.isNetConnected(), callback)) {
            AuthService.JWTTokenParams params = new AuthService.JWTTokenParams(userId, token,
                AuthService.getClientType(), appId, callback);
            AuthService.getJWTToken(params);
        }
    }

    private static void onJWTTokenGot(int cid, JWTTokenResp jwtToken, int httpResponseCode, String userId, AuthResultReceiver resultReceiver) {
        String serviceId = jwtToken.shortId;
        if (Logger.isLoggableDebug(LogTag.AUTH)) {
            Calendar calendar = CalendarUtils.calendarLocal_FromMilliseconds(jwtToken.currentTime);
            Log.d(LogTag.AUTH, String.format("expire=[%d], serviceId=[%s], status=[%s], time=[%s], serverTime=[%d][%s]",
                jwtToken.expiresIn, serviceId, jwtToken.userStatus, jwtToken.accelExpiredTime,
                jwtToken.currentTime,
                CalendarUtils.calendarToString(
                    calendar,
                    CalendarUtils.FORMAT_DATE | CalendarUtils.FORMAT_TIME | CalendarUtils.FORMAT_ZONE
                    )
                ));
        }
        MessageUserId.setCurrentUserInfo(userId, serviceId, jwtToken.userStatus, jwtToken.accelExpiredTime);
        if (resultReceiver != null) {
            resultReceiver.onGetJWTTokenResult(cid, jwtToken.accelToken, jwtToken.expiresIn,
                serviceId, jwtToken.userStatus, jwtToken.accelExpiredTime, true, httpResponseCode);
        }
    }

    /**
     * 取加速Token。
     *
     * @param controller     {@link Controller}
     * @param ip             节点IP
     * @param jwtToken       JWTToken
     * @param resultReceiver 结果回调
     */
    public static void getToken(Controller controller, int cid, final String ip, final String jwtToken, final AuthResultReceiver resultReceiver) {
        ResponseCallback callback = new ResponseCallback(controller.getEventReporter(), cid) {
            @Override
            protected void onSuccess(int code, byte[] response) {
                TokenInfo tokenInfo = TokenInfo.createFromJson(new String(response));
                if (tokenInfo != null) {
                    if (Logger.isLoggableDebug(TAG)) {
                        Log.d(TAG, String.format("token=%s, expire=%d", tokenInfo.token, tokenInfo.expires_in));
                    }
                    byte[] tokenBytes = tokenInfo.token == null ? null : tokenInfo.token.getBytes();
                    int length = tokenBytes == null ? 0 : tokenBytes.length;
                    resultReceiver.onGetTokenResult(this.cid, ip, tokenBytes, length, tokenInfo.expires_in, true, code);
                    reportSuccessEvent();
                } else {
                    doFail(-1, null);
                }
            }

            @Override
            protected void onFail(int code, byte[] response) {
                if (code == 401) {
                    Logger.d(TAG, "GetToken failed, clear cache.");
                    cache.removeJWTToken(jwtToken);
                }
                resultReceiver.onGetTokenResult(this.cid, ip, null, 0, -1, false, code);
            }

            @Override
            protected String getEventName() {
                return "auth_get_node_token";
            }
        };
        if (preCheck(controller.isNetConnected(), callback)) {
            AuthService.getToken(ip, jwtToken, callback);
        }
    }

    /**
     * 取用户状态。
     *
     * @param controller     {@link Controller}
     * @param userId         用户ID
     * @param jwtToken       鉴权令牌
     * @param resultReceiver 结果回调
     */
    public static void getUserAccelStatus(Controller controller, int cid, final String userId, String jwtToken, final AuthResultReceiver resultReceiver) {
        ResponseCallback callback = new ResponseCallback(controller.getEventReporter(), cid) {

            @Override
            protected void onSuccess(int code, byte[] response) {
                UserAccelStatus accelStatus = UserAccelStatus.createFromJson(new String(response));
                if (accelStatus != null) {
                    int userStatus = accelStatus.status;
                    String serviceId = accelStatus.shortId;
                    MessageUserId.setCurrentUserInfo(userId, serviceId, userStatus, accelStatus.expiredTime);
                    resultReceiver.onGetUserAccelStatusResult(this.cid, serviceId, userStatus, accelStatus.expiredTime, true, code);
                    reportSuccessEvent();
                } else {
                    doFail(-1, null);
                }
            }

            @Override
            protected void onFail(int code, byte[] response) {
                if (code == 401) {
                    Logger.d(TAG, "GetUserAccelStatus failed, clear cache.");
                    cache.set(userId, null);
                }
                MessageUserId.resetUserInfo(userId);
                resultReceiver.onGetUserAccelStatusResult(this.cid, null, -1, null, false, code);
            }

            @Override
            protected String getEventName() {
                return "auth_get_user_status";
            }
        };
        //
        if (preCheck(controller.isNetConnected(), callback)) {
            AuthService.getUserAccelStatus(userId, jwtToken, callback);
        }
    }

    /**
     * 取用户配置
     *
     * @param controller     {@link Controller}
     * @param jwtToken       JWTToken，不能为null
     * @param userId         用户Id（对于腾讯游戏来说，即openId）
     * @param resultReceiver 结果回调
     */
    public static void getConfigs(Controller controller, int cid, final String jwtToken, final String userId, final AuthResultReceiver resultReceiver) {
        ResponseCallback callback = new ResponseCallback(controller.getEventReporter(), cid) {

            @Override
            protected void onSuccess(int code, byte[] response) {
                if (response == null) {
                    Log.w(TAG, "Configs: (null)");
                    doFail(-1, null);
                    return;
                }
                if (Logger.isLoggableDebug(TAG)) {
                    Log.d(TAG, "Configs: " + new String(response));
                }
                Configs configs = Configs.parseFromJson(response);
                if (configs != null) {
                    UserConfig userConfig = UserConfig.create(configs.userConfig);
                    if (userConfig != null) {
                        saveUserConfig(userId, userConfig);
                    }
                    resultReceiver.onGetUserConfigResult(this.cid, jwtToken, userId, configs, code, true);
                    reportSuccessEvent();
                    return;
                }
                doFail(-1, null);
            }

            @Override
            protected void onFail(int code, byte[] response) {
                if (code == 401) {
                    Logger.d(TAG, "GetUserConfig failed, clear cache.");
                    cache.set(userId, null);
                }
                resultReceiver.onGetUserConfigResult(this.cid, null, userId, null, code, false);
            }

            @Override
            protected String getEventName() {
                return "auth_get_config";
            }
        };
        if (preCheck(controller.isNetConnected(), callback)) {
            AuthService.getConfig(jwtToken, userId, controller.getClientVersion(), callback);
        }
    }

    private static void saveUserConfig(String userId, UserConfig uc) {
        userConfigList.put(userId, uc);
        MessageUserId.setCurrentUserConfig(userId, uc.value);
    }

    /**
     * 设置用户配置
     *
     * @param controller {@link Controller}
     * @param jwtToken   JWTToken，如果为null则使用缓存里的Token
     * @param userId     用户Id（对于腾讯游戏来说，即openId）
     * @param userConfig 用户配置
     */
    public static void setUserConfig(Controller controller, int cid, String jwtToken, final String userId, String userConfig) {
        if (jwtToken == null) {
            JWTTokenResp data = cache.get(userId);
            if (data != null) {
                jwtToken = data.accelToken;
            }
        }
        //
        ResponseCallback callback = new ResponseCallback(controller.getEventReporter(), cid) {

            private String parseResponse(byte[] response) {
                if (response == null || response.length < 2) {
                    return null;
                }
                JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(response)));
                try {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if ("userConfig".equals(name)) {
                            return reader.nextString();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } catch (IOException e) {
                    // do nothing, fall
                } catch (RuntimeException e) {
                    // do nothing, fall
                }
                return null;
            }

            @Override
            protected void onSuccess(int code, byte[] response) {
                if (code != 201) {
                    Log.w(TAG, "Try upload user config, response code: " + code);
                    doFail(code, response);
                }
                reportSuccessEvent();
            }

            @Override
            protected void onFail(int code, byte[] response) {
                // do nothing
            }

            @Override
            protected String getEventName() {
                return "auth_set_config";
            }
        };
        //
        if (preCheck(controller.isNetConnected(), callback)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(buf));
            try {
                writer.beginObject();
                JsonUtils.writeString(writer, "userConfig", userConfig);
                writer.endObject();
            } catch (IOException e) {
                return;
            } finally {
                Misc.close(writer);
            }
            AuthService.setUserConfig(jwtToken, userId, buf.toByteArray(), callback);
        }
    }

    /**
     * 设置用户配置，只改变“WiFi加速”项
     *
     * @param controller
     * @param userId
     * @param parallelSwitch 开还是关“WiFi加速功能”？
     */
    public static void setUserConfig_ParallelOnly(Controller controller, int cid, String userId, boolean parallelSwitch) {
        if (TextUtils.isEmpty(userId)) {
            Logger.w(TAG, "Empty or Null userId");
            return;
        }
        UserConfig uc = userConfigList.get(userId);
        if (uc == null) {
            Logger.w(TAG, "No user config exists");
            return;
        }
        JWTTokenResp data = cache.get(userId);
        if (data == null || data.accelToken == null || data.accelToken.length() == 0) {
            Logger.w(TAG, "Set user config failed (#1)");
            return;
        }
        uc = new UserConfig(uc.accel, parallelSwitch, uc.accelMode);
        saveUserConfig(userId, uc);
        setUserConfig(controller, cid, data.accelToken, userId, uc.value);
    }

    /**
     * 为{@link AuthExecutor}各方法提供一些必要的支持
     */
    public interface Controller {

        /**
         * 判断当前网络是否连接着
         *
         * @return true网络已连接
         */
        boolean isNetConnected();

        /**
         * 取事件上报执行者
         *
         * @return {@link MessageEvent.Reporter}
         */
        MessageEvent.Reporter getEventReporter();

        /**
         * 取客户端当前版本号
         */
        String getClientVersion();

    }

    /**
     * 用户的配置
     */
    public static class Configs {

        private final static String[] FIELD_NAMES = new String[] {
            "userConfig", "serviceConfig", "scriptId",
        };

        public final String userConfig;
        public final String serverConfig;
        public final String scriptId;

        Configs(String userConfig, String serverConfig, String scriptId) {
            this.userConfig = userConfig;
            this.serverConfig = serverConfig;
            this.scriptId = scriptId;
        }

        static Configs parseFromJson(byte[] json) {
            if (json == null || json.length < 2) {
                return null;
            }
            String[] values = new String[3];
            JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(json)));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    boolean found = false;
                    for (int i = FIELD_NAMES.length - 1; i >= 0; --i) {
                        if (FIELD_NAMES[i].equals(name)) {
                            values[i] = JsonUtils.readNextString(reader);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                return null;
            } catch (RuntimeException e) {
                return null;
            } finally {
                Misc.close(reader);
            }
            return new Configs(values[0], values[1], values[2]);
        }
    }

    /**
     * 负责缓存JWTToken以及用户状态等
     */
    static class Cache {

        private static final String FIELD_NAME_LIST = "list";
        private static final String FIELD_NAME_USER = "user";
        private static final String FIELD_NAME_DATA = "data";

        private final Persistent persistent;

        private Map<String, JWTTokenResp> container;

        Cache(Persistent persistent) {
            this.persistent = persistent;
            this.container = loadFromCache(persistent);
        }

        private static byte[] makeKey() {
            ByteBuffer buf = ByteBuffer.allocate(64);
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putShort((short) 2016).put("SuBao".getBytes()).put((byte) 8).put("GameMaster".getBytes());
            return Arrays.copyOf(buf.array(), buf.position());
        }

        static void encode(byte[] data) {
            encode(data, 0, data.length);
        }

        static void encode(byte[] data, int start, int end) {
            byte[] key = makeKey();
            for (int i = start, key_idx = 0, key_len = key.length; i < end; ++i) {
                byte b = data[i];
                byte k = key[key_idx++];
                if (key_idx == key_len) {
                    key_idx = 0;
                }
                data[i] = (byte) (b ^ k);
            }
        }

        private static Map<String, JWTTokenResp> loadMap(JsonReader reader) throws IOException {
            Map<String, JWTTokenResp> map = new HashMap<String, JWTTokenResp>(4);
            reader.beginArray();
            while (reader.hasNext()) {
                String userId = null;
                JWTTokenResp data = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (FIELD_NAME_USER.equals(name)) {
                        userId = reader.nextString();
                    } else if (FIELD_NAME_DATA.equals(name)) {
                        data = JWTTokenResp.createFromJson(reader);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
                if (!TextUtils.isEmpty(userId) && data != null) {
                    map.put(userId, data);
                }
            }
            reader.endArray();
            return map.isEmpty() ? null : map;
        }

        @SuppressWarnings("resource")
        private static Map<String, JWTTokenResp> loadFromDecoded(byte[] buf) {
            JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(buf)));
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (FIELD_NAME_LIST.equals(name)) {
                        return loadMap(reader);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (IOException e) {
                // fall down
            } catch (RuntimeException e) {
                // fall down
            } finally {
                Misc.close(reader);
            }
            return null;
        }

        private static Map<String, JWTTokenResp> loadFromCache(Persistent persistent) {
            if (persistent == null || !persistent.exists()) {
                return null;
            }
            byte[] buf = new byte[1024];
            int size;
            InputStream input = null;
            try {
                input = persistent.openInput();
                size = input.read(buf);
            } catch (IOException e) {
                return null;
            } finally {
                Misc.close(input);
            }
            encode(buf, 0, size);
            Map<String, JWTTokenResp> map = loadFromDecoded(buf);
            if (map == null) {
                persistent.delete();
            }
            return map;
        }


        private static void save(Persistent persistent, Map<String, JWTTokenResp> map) {
            if (persistent == null) {
                return;
            }
            if (map == null || map.isEmpty()) {
                persistent.delete();
                return;
            }
            try {
                StringWriter sw = new StringWriter(1024);
                JsonWriter jw = new JsonWriter(sw);
                try {
                    jw.beginObject();
                    jw.name(FIELD_NAME_LIST);
                    jw.beginArray();
                    for (Map.Entry<String, JWTTokenResp> entry : map.entrySet()) {
                        jw.beginObject();
                        jw.name(FIELD_NAME_USER).value(entry.getKey());
                        jw.name(FIELD_NAME_DATA);
                        entry.getValue().serialize(jw);
                        jw.endObject();
                    }
                    jw.endArray();
                    jw.endObject();
                } finally {
                    Misc.close(jw);
                }
                //
                byte[] bytes = sw.toString().getBytes();
                encode(bytes);
                OutputStream output = persistent.openOutput();
                try {
                    output.write(bytes);
                } finally {
                    Misc.close(output);
                }
            } catch (IOException ignored) {
                persistent.delete();
            }
        }

        public JWTTokenResp get(String userId) {
            return get(userId, System.currentTimeMillis());
        }

        JWTTokenResp get(String userId, long now) {
            if (TextUtils.isEmpty(userId)) {
                return null;
            }
            synchronized (this) {
                if (this.container == null) {
                    return null;
                }
                JWTTokenResp data = container.get(userId);
                if (data == null) {
                    return null;
                }
                if (data.expiresIn - 60 * 1000L > now) {
                    return data.cloneWithNewExpiredIn((data.expiresIn - now) / 1000);
                }
                // 过期了
                container.remove(userId);
                save(persistent, container);
            }
            return null;
        }

        public void set(String userId, JWTTokenResp responseFromServer) {
            if (TextUtils.isEmpty(userId)) {
                return;
            }
            if (responseFromServer == null) {
                synchronized (this) {
                    if (container != null) {
                        if (null != container.remove(userId)) {
                            save(persistent, container);
                        }
                    }
                }
                return;
            }
            //
            JWTTokenResp data = responseFromServer.cloneWithNewExpiredIn(
                System.currentTimeMillis() + responseFromServer.expiresIn * 1000);
            synchronized (this) {
                if (this.container == null) {
                    this.container = new HashMap<String, JWTTokenResp>(4);
                }
                container.put(userId, data);
                save(persistent, container);
            }
        }

        public void removeJWTToken(String jwtToken) {
            if (TextUtils.isEmpty(jwtToken)) {
                return;
            }
            synchronized (this) {
                if (container == null) {
                    return;
                }
                boolean modified = false;
                Iterator<Map.Entry<String, JWTTokenResp>> iterator = container.entrySet().iterator();
                while (iterator.hasNext()) {
                    JWTTokenResp data = iterator.next().getValue();
                    if (data != null) {
                        if (jwtToken.equals(data.accelToken)) {
                            iterator.remove();
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    save(persistent, container);
                }
            }
        }

        int size() {
            synchronized (this) {
                return container == null ? 0 : container.size();
            }
        }

        boolean isDataEquals(Cache other) {
            if (this == other) {
                return true;
            }
            if (null == other) {
                return false;
            }
            return Misc.isEquals(this.container, other.container);
        }
    }

    /**
     * 用于保存给定UserId的鉴权，出现错误的次数
     */
    static class AuthErrorCounter {

        private static final List<AuthErrorCounter> list = new ArrayList<AuthErrorCounter>(2);
        private final String userId;
        private int errorCount;

        private AuthErrorCounter(String userId) {
            this.userId = userId;
            this.errorCount = 1;
        }

        /**
         * 清空给定UserId的鉴权错误记录
         */
        public static boolean remove(String userId) {
            for (int i = list.size() - 1; i >= 0; --i) {
                AuthErrorCounter authErrorCounter = list.get(i);
                if (authErrorCounter.userId.equals(userId)) {
                    list.remove(i);
                    return true;
                }
            }
            return false;
        }

        /**
         * 递增指定UserId的鉴权错误次数，如果递增后的次数，小于给定的maxErrorCount，
         * 则返回{@link UpdateResult#SUCCEED}，否则清空对此UserId的所有记录，并返回
         * {@link UpdateResult#TOO_MANY_ERROR}
         *
         * @param userId        给定的UserId
         * @param maxErrorCount 错误数上限。如果本次递增后的错误数大于等于此值，函数将
         *                      清空该UserId的记录，并返回{@link UpdateResult#SUCCEED}
         * @return 如果本次递增后的错误数大于等于maxErrorCount，函数将清空该UserId的记录，
         * 并返回{@link UpdateResult#SUCCEED}
         */
        public static UpdateResult update(String userId, int maxErrorCount) {
            for (int i = list.size() - 1; i >= 0; --i) {
                AuthErrorCounter authErrorCounter = list.get(i);
                if (authErrorCounter.userId.equals(userId)) {
                    ++authErrorCounter.errorCount;
                    if (authErrorCounter.errorCount >= maxErrorCount) {
                        list.remove(i);
                        return UpdateResult.TOO_MANY_ERROR;
                    } else {
                        return UpdateResult.SUCCEED;
                    }
                }
            }
            AuthErrorCounter authErrorCounter = new AuthErrorCounter(userId);
            list.add(authErrorCounter);
            return UpdateResult.SUCCEED;
        }

        /**
         * 函数{@link #update(String, int)}的返回值定义
         */
        public enum UpdateResult {
            /**
             * 已成功递增错误次数
             */
            SUCCEED,

            /**
             * 递增后的错误次数大于或等于给定的错误数上限
             */
            TOO_MANY_ERROR,
        }
    }

}
