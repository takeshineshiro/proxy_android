package com.subao.common.msg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;
import com.subao.common.data.AppType;
import com.subao.common.utils.InfoUtils;
import com.subao.common.utils.StringUtils;

import java.io.IOException;
import java.util.List;

public class Message_Installation implements JsonSerializable {

    public final long unixTime;
    public final UserInfo userInfo;
    public final Message_DeviceInfo deviceInfo;
    public final Message_VersionInfo versionInfo;
    public final AppType appType;
    private final List<Message_App> appList;

    public Message_Installation(
        AppType appType,
        long unixTime, UserInfo userInfo, Message_DeviceInfo deviceInfo,
        Message_VersionInfo versionInfo,
        List<Message_App> appList
    ) {
        this.unixTime = unixTime;
        this.userInfo = userInfo;
        this.deviceInfo = deviceInfo;
        this.versionInfo = versionInfo;
        this.appType = appType;
        this.appList = appList;
    }

    public Iterable<Message_App> getAppList() {
        return this.appList;
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("time").value(this.unixTime);
        if (userInfo != null) {
            writer.name("user");
            this.userInfo.serialize(writer);
        }
        if (deviceInfo != null) {
            writer.name("device");
            deviceInfo.serialize(writer);

        }
        if (this.versionInfo != null) {
            writer.name("version");
            this.versionInfo.serialize(writer);
        }
        MessageJsonUtils.serializeAppList(writer, "appList", this.getAppList());
        MessageJsonUtils.serializeEnum(writer, "type", this.appType);
        writer.endObject();
    }

    public static class UserInfo implements JsonSerializable {
        public final String imsi;
        public final String sn;
        public final String mac;
        public final String deviceId;
        public final String androidId;

        public UserInfo(String imsi, String sn, String mac, String deviceId, String androidId) {
            this.imsi = imsi;
            this.sn = sn;
            this.mac = mac;
            this.deviceId = deviceId;
            this.androidId = androidId;
        }

        /**
         * 根据context进行创建，必须在Android主线程使用
         */
        @SuppressLint("HardwareIds")
        public static UserInfo create(Context context) {
            String androidId;
            try {
                androidId = android.provider.Settings.Secure.getString(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
            } catch (RuntimeException e) {
                androidId = StringUtils.EMPTY;
            }
            return new UserInfo(
                InfoUtils.getIMSI(context),
                android.os.Build.SERIAL,
                InfoUtils.getMacAddress(context),
                InfoUtils.getIMEI(context),
                androidId);
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            JsonUtils.writeString(writer, "imsi", this.imsi);
            JsonUtils.writeString(writer, "sn", this.sn);
            JsonUtils.writeString(writer, "mac", this.mac);
            JsonUtils.writeString(writer, "deviceId", this.deviceId);
            JsonUtils.writeString(writer, "androidId", this.androidId);
            writer.endObject();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof UserInfo)) {
                return false;
            }
            UserInfo other = (UserInfo) o;
            return Misc.isEquals(this.imsi, other.imsi)
                && Misc.isEquals(this.sn, other.sn)
                && Misc.isEquals(this.mac, other.mac)
                && Misc.isEquals(this.deviceId, other.deviceId)
                && Misc.isEquals(this.androidId, other.androidId);
        }
    }

}
