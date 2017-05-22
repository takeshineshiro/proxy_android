package com.subao.common.qos;

import android.util.JsonReader;

import com.subao.common.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class QosResponse {

	public final int resultCode;
	public final String errorInfo;
	
	private QosResponse(int resultCode, String errorInfo) {
		this.resultCode = resultCode;
		this.errorInfo = errorInfo;
	}
	
	public static QosResponse parseFromJson(InputStream input) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(input));
		try {
			return parseFromJson(reader);
		} finally {
			com.subao.common.Misc.close(reader);
		}
	}
	
	public static QosResponse parseFromJson(JsonReader reader) throws IOException {
		int resultCode = -1;
		String errorInfo = null;
		try {
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if ("resultCode".equals(name)) {
					resultCode = reader.nextInt();
				} else if ("errorInfo".equals(name)) {
					errorInfo = JsonUtils.readNextString(reader);
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
		} catch (RuntimeException e) {
			throw new IOException();
		}
		return new QosResponse(resultCode, errorInfo);
	}
	
}
