package com.subao.common.msg;

import java.io.IOException;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.Misc;

public class MessageSessionId implements JsonSerializable {
	
	public final String id;
	
	MessageSessionId(String id) {
		this.id = id;
	}
	
	public static MessageSessionId create(String id) {
		if (null == id || id.length() == 0) {
			return null;
		}
		return new MessageSessionId(id);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (!(o instanceof MessageSessionId)) {
			return false;
		}
		MessageSessionId other = (MessageSessionId)o;
		return Misc.isEquals(this.id, other.id);
	}
	
	@Override
	public void serialize(JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name("id").value(id);
		writer.endObject();
	}

}
