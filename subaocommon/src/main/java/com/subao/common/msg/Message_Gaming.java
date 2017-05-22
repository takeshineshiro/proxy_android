package com.subao.common.msg;

import java.io.IOException;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.data.AppType;

public class Message_Gaming implements JsonSerializable {

	public enum AccelMode implements MessageEnum {
		UNKNOWN_ACCEL_MODE(0),
		NOT_ACCEL_MODE(1),
		ROOT_MODE(2),
		VPN_MODE(3);

		AccelMode(int id) {
			this.id = id;
		}

		private final int id;

		@Override
		public int getId() {
			return this.id;
		}
	}

	public final MessageUserId id;

	public final long time;

	public final AppType appType;

	public final Message_App game;

	/** SDK 不填此字段 */
	public final AccelMode mode;

	public final Message_VersionInfo version;

	/** Android必选字段 */
	public final Message_DeviceInfo device;

	public Message_Gaming(MessageUserId id, long time, AppType appType, Message_App game, AccelMode mode, Message_VersionInfo version,
                          Message_DeviceInfo device) {
		this.id = id;
		this.time = time;
		this.appType = appType;
		this.game = game;
		this.version = version;
		this.mode = mode;
		this.device = device;
	}

	@Override
	public void serialize(JsonWriter writer) throws IOException {
		writer.beginObject();
		JsonUtils.writeSerializable(writer, "id", this.id);
		writer.name("time").value(this.time);
		MessageJsonUtils.serializeEnum(writer, "type", appType);
		JsonUtils.writeSerializable(writer, "game", this.game);
		MessageJsonUtils.serializeEnum(writer, "mode", mode);
		JsonUtils.writeSerializable(writer, "version", this.version);
		JsonUtils.writeSerializable(writer, "device", this.device); // 2016.7.27新增（GamingV2）
		writer.endObject();
	}

}
