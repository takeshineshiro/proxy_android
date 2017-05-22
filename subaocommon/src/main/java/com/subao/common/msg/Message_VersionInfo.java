package com.subao.common.msg;

import android.os.Build;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;

import java.io.IOException;

public class Message_VersionInfo implements JsonSerializable {

	/** 版本号，如：3.5.1，3.6.1等 */
	public final String number;

	/**
	 * 渠道名，如：BAIDU，XIAOMI等<br />
	 * 对于SDK来说，填写GUID
	 */
	public final String channel;

	/** 操作系统版本，如：MIUI 7.0等 */
	public final String osVersion;

	/** 安卓版本，如：22（5.1 lollipop）等 */
	public final String androidVersion;

	Message_VersionInfo(String number, String channel, String osVersion, String androidVersion) {
		this.number = number;
		this.channel = channel;
		this.osVersion = osVersion;
		this.androidVersion = androidVersion;
	}
	
	public static Message_VersionInfo create(String versionName, String channel) {
		return new Message_VersionInfo(
			versionName,
			channel,
			Build.FINGERPRINT, Build.VERSION.RELEASE);
	}

	@Override
	public void serialize(JsonWriter writer) throws IOException {
		writer.beginObject();
		JsonUtils.writeString(writer, "number", this.number);
		JsonUtils.writeString(writer, "channel", this.channel);
		JsonUtils.writeString(writer, "osVersion", this.osVersion);
		JsonUtils.writeString(writer, "androidVersion", this.androidVersion);
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
        if (!(o instanceof Message_VersionInfo)) {
            return false;
        }
        Message_VersionInfo other = (Message_VersionInfo) o;
        return Misc.isEquals(this.number, other.number)
            && Misc.isEquals(this.channel, other.channel)
            && Misc.isEquals(this.osVersion, other.osVersion)
            && Misc.isEquals(this.androidVersion, other.androidVersion);
    }
}
