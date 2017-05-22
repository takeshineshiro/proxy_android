package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.data.AppType;

import java.io.IOException;

/**
 * Created by hujd on 16-8-2.
 */
public class Message_Upgrade implements JsonSerializable {
	private final MessageUserId userId;
	private final long time;
	private final Message_VersionInfo oldVersionInfo;
	private final Message_VersionInfo newVersionInfo;
	private final AppType appType;

	public Message_Upgrade(MessageUserId userId, long time, Message_VersionInfo oldVersionInfo, Message_VersionInfo newVersionInfo, AppType appType) {
		this.userId = userId;
		this.time = time;
		this.oldVersionInfo = oldVersionInfo;
		this.newVersionInfo = newVersionInfo;
		this.appType = appType;
	}

	@Override
	public void serialize(JsonWriter writer) throws IOException {
		writer.beginObject();
		JsonUtils.writeSerializable(writer, "id", this.userId);
		writer.name("time").value(this.time);
		JsonUtils.writeSerializable(writer, "oldVersion", this.oldVersionInfo);
		JsonUtils.writeSerializable(writer, "newVersion", this.newVersionInfo);
		MessageJsonUtils.serializeEnum(writer, "type", appType);
		writer.endObject();
	}
}
