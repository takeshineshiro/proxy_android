package com.subao.common.msg;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.Misc;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.utils.JsonUtils;
import com.subao.gamemaster.GameMaster;

import java.io.IOException;


/**
 * 用于消息上报的用户/设备的标识</ br>
 * 来源：id.proto 定义
 */
public class MessageUserId implements JsonSerializable, Parcelable {

    public static final Creator<MessageUserId> CREATOR
        = new Creator<MessageUserId>() {
        public MessageUserId createFromParcel(Parcel in) {
            String subaoId = in.readString();
            String userId = in.readString();
            String serviceId = in.readString();
            int userStatus = in.readInt();
            String userConfig = in.readString();
            return new MessageUserId(subaoId, userId, serviceId, userStatus, userConfig);
        }

        public MessageUserId[] newArray(int size) {
            return new MessageUserId[size];
        }
    };
    private static UpdateListener updateListener;

    private static String currentSubaoId;

    private static String currentUserId;

    private static String currentServiceId;

    private static int currentUserStatus;

    private static String currentUserConfig;

    private static String currentExpireTime;

    /**
     * SubaoID
     */
    public final String subaoId;

    /**
     * 对于SDK来说指游戏角色或帐号，比如腾讯的openid。对于APP来说，是速宝帐号
     */
    public final String userId;

    /**
     * 服务ID（客服使用）
     */
    public final String serviceId;

    /**
     * 用户状态
     */
    public final int userStatus;

    /**
     * 用户配置
     */
    public final String userConfig;

    MessageUserId(String subaoId, String userId, String serviceId, int userStatus, String userConfig) {
        this.subaoId = subaoId;
        this.userId = userId;
        this.serviceId = serviceId;
        this.userStatus = userStatus;
        this.userConfig = userConfig;
    }

    private static synchronized UpdateListener getUpdateListener() {
        return updateListener;
    }

    /**
     * 设置一个{@link UpdateListener}，以便当MessageUserId的各数据值发生改变时，得到通知，做一些事情<br />
     * （主要是给APP用的）
     *
     * @param listener {@link UpdateListener}
     */
    public static synchronized void setUpdateListener(UpdateListener listener) {
        updateListener = listener;
    }

    /**
     * 根据当前值，构造一个新的MessageUserId
     *
     * @return 根据当前设置的各数据值构造的新的{@link MessageUserId}对象
     */
    public static MessageUserId build() {
        return new MessageUserId(getCurrentSubaoId(), getCurrentUserId(), getCurrentServiceId(), getCurrentUserStatus(), getCurrentUserConfig());
    }

    /**
     * 取当前的SubaoId
     *
     * @return 当前SubaoId
     */
    public static synchronized String getCurrentSubaoId() {
        return currentSubaoId;
    }

    /**
     * 设置当前SubaoId为给定值
     *
     * @param subaoId SubaoId
     */
    public static synchronized void setCurrentSubaoId(String subaoId) {
        currentSubaoId = subaoId;
        UpdateListener listener = getUpdateListener();
        if (null != listener) {
            listener.onSubaoIdUpdate(subaoId);
        }
    }

    /**
     * 通常在用户登录前被调用，清空相关信息
     *
     * @param userId 用户ID
     */
    public static void resetUserInfo(String userId) {
        currentUserId = userId;
        currentServiceId = null;
        currentUserStatus = GameMaster.SDK_NOT_QUALIFIED;
        currentUserConfig = null;
        currentExpireTime = null;
    }


    /**
     * 设置用户当前资料
     *
     * @param userId     User帐号
     * @param serviceId  服务ID
     * @param userStatus 用户状态
     * @param expireTime 过期时间
     */
    public static synchronized void setCurrentUserInfo(String userId, String serviceId, int userStatus, String expireTime) {
        currentUserId = userId;
        currentServiceId = serviceId;
        currentUserStatus = userStatus;
        currentUserConfig = null;
        currentExpireTime = expireTime;
        UpdateListener listener = getUpdateListener();
        if (null != listener) {
            listener.onUserInfoUpdate(userId, serviceId, userStatus);
        }
    }

    /**
     * 取当前的UserId
     *
     * @return 当前UserId
     */
    public static synchronized String getCurrentUserId() {
        return currentUserId;
    }

    /**
     * 取当前服务Id
     *
     * @return 当前服务Id
     */
    public static synchronized String getCurrentServiceId() {
        return currentServiceId;
    }

    /**
     * 取当前用户状态
     *
     * @return 当前用户状态
     */
    public static synchronized int getCurrentUserStatus() {
        return currentUserStatus;
    }

    /**
     * 取当前用户的配置
     *
     * @return 当前用户配置
     */
    public static synchronized String getCurrentUserConfig() {
        return currentUserConfig;
    }

    /**
     * 取当前用户的到期时间
     *
     * @return 当前用户的到期时间
     */
    public static synchronized String getCurrentExpireTime() {
        return currentExpireTime;
    }

    public static void setCurrentUserConfig(String userId, String value) {
        if (userId != null && userId.equals(currentUserId)) {
            currentUserConfig = value;
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("[subaoId=%s, userId=%s, serviceId=%s, userStatus=%d, config=%s]", subaoId, userId, serviceId, userStatus, userConfig);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof MessageUserId)) {
            return false;
        }
        MessageUserId other = (MessageUserId) o;
        return this.userStatus == other.userStatus
            && Misc.isEquals(this.subaoId, other.subaoId)
            && Misc.isEquals(this.userId, other.userId)
            && Misc.isEquals(this.serviceId, other.serviceId)
            && Misc.isEquals(this.userConfig, other.userConfig);
    }

    public boolean isSubaoIdValid() {
        return SubaoIdManager.isSubaoIdValid(this.subaoId);
    }

    /**
     * 序列化到{@link JsonWriter}
     *
     * @param writer {@link JsonWriter}
     * @throws IOException
     */
    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        JsonUtils.writeString(writer, "id", this.subaoId);
        JsonUtils.writeString(writer, "userId", this.userId);
        JsonUtils.writeString(writer, "serviceId", this.serviceId);
        writer.name("stat").value(this.userStatus);
        JsonUtils.writeString(writer, "config", this.userConfig);
        writer.endObject();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.subaoId);
        dest.writeString(this.userId);
        dest.writeString(this.serviceId);
        dest.writeInt(this.userStatus);
        dest.writeString(this.userConfig);
    }

    /**
     * 数据发生改变的观察者
     */
    public interface UpdateListener {

        /**
         * 当MessageUserId的SubaoId字段发生改变时被调用
         */
        void onSubaoIdUpdate(String subaoId);

        /**
         * 当MessageUserId的User相关各数据（UserId，ServiceId和UserStatus）发生改变时被调用
         */
        void onUserInfoUpdate(String userId, String serviceId, int userStatus);
    }
}
