package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;

import java.io.IOException;

public class Message_App implements JsonSerializable {

    public final String appLabel;
	public final String pkgName;

	public Message_App(String appLabel, String pkgName) {
		this.appLabel = appLabel;
		this.pkgName = pkgName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (!(o instanceof Message_App)) {
			return false;
		}
		Message_App other = (Message_App) o;
		return Misc.isEquals(this.appLabel, other.appLabel)
			&& Misc.isEquals(this.pkgName, other.pkgName);
	}

	@Override
	public void serialize(JsonWriter writer) throws IOException {
		writer.beginObject();
		JsonUtils.writeString(writer, "AppLabel", this.appLabel);
		JsonUtils.writeString(writer, "PkgName", this.pkgName);
		writer.endObject();
	}
}
