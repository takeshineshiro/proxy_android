package com.subao.common.qos;

import java.io.IOException;
import java.io.StringWriter;

import android.annotation.SuppressLint;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;

@SuppressLint("DefaultLocale")
class QosModifyRequest implements JsonSerializable {
	
	private final int timeLength;
	private final String securityToken;
	
	public QosModifyRequest(int timeLength, String securityToken) {
		this.timeLength = timeLength;
		this.securityToken = securityToken;
	}

	@Override
	public void serialize(JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name("timeLength").value(this.timeLength);
		JsonUtils.writeString(writer, "securityToken", this.securityToken);
		writer.endObject();
	}
	
	@Override
	public String toString() {
		StringWriter sw = new StringWriter(512);
		JsonWriter writer = new JsonWriter(sw);
		try {
			this.serialize(writer);
			writer.flush();
			return sw.toString();
		} catch (IOException e) {
			return String.format("[time=%d, token=%s]", this.timeLength, this.securityToken);
		} finally {
			com.subao.common.Misc.close(writer);
		}
	}
	
}
