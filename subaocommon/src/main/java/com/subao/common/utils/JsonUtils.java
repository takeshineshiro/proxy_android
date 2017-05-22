package com.subao.common.utils;

import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.Misc;

import java.io.IOException;
import java.io.StringWriter;

public class JsonUtils {

    /**
     * 将一个{@link JsonSerializable}对象序列化到String中
     *
     * @param jsonSerializable 要序列化的对象
     * @return 包含序列化内容的String
     */
    public static String serializeToString(JsonSerializable jsonSerializable) throws IOException {
        StringWriter stringWriter = new StringWriter(2048);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        jsonSerializable.serialize(jsonWriter);
        Misc.close(jsonWriter);
        return stringWriter.toString();
    }

    public static JsonWriter writeSerializable(JsonWriter writer, String name, JsonSerializable obj) throws IOException {
		if (obj != null) {
			if (!TextUtils.isEmpty(name)) {
				writer.name(name);
			}
			obj.serialize(writer);
		}
		return writer;
	}

	public static JsonWriter writeString(JsonWriter writer, String name, String value) throws IOException {
		if (value != null) {
			writer.name(name).value(value);
		}
		return writer;
	}

	public static JsonWriter writeNumber(JsonWriter writer, String name, Number value) throws IOException {
		if (value != null) {
			writer.name(name).value(value);
		}
		return writer;
	}

	public static JsonWriter writeUnsignedInt(JsonWriter writer, String name, Integer value) throws IOException {
		if (value != null) {
			writer.name(name).value((long) value & 0xffffffffL);
		}
		return writer;
	}

    public static JsonWriter writeObject(JsonWriter writer, String name, Object object) throws IOException {
        if (object != null) {
            writer.name(name).value(object.toString());
        }
        return writer;
    }

	///////////////////////////////////////////////////////////

    /**
     * 从{@link JsonReader}里读取下一个String或null
     *
     * @param reader
     * @return 下一个String或null
     * @throws IOException
     */
    public static String readNextString(JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.NULL) {
            reader.skipValue();
            return null;
        }
        return reader.nextString();
    }

	public static String encode(String s) {
		if (s == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder(s.length() + 16);
		encode(sb, s);
		return sb.toString();
	}

	public static StringBuilder encode(StringBuilder sb, String s) {
		if (s == null) {
			return sb.append("null");
		}
		sb.append('"');
		for (int i = 0, length = s.length(); i < length; i++) {
			char c = s.charAt(i);

			/*
			 * From RFC 4627, "All Unicode characters may be placed within the
			 * quotation marks except for the characters that must be escaped:
			 * quotation mark, reverse solidus, and the control characters
			 * (U+0000 through U+001F)."
			 * 
			 * We also escape '\u2028' and '\u2029', which JavaScript interprets
			 * as newline characters. This prevents eval() from failing with a
			 * syntax error.
			 * http://code.google.com/p/google-gson/issues/detail?id=341
			 */
			switch (c) {
			case '"':
			case '\\':
				sb.append('\\');
				sb.append(c);
				break;

			case '\t':
				sb.append("\\t");
				break;

			case '\b':
				sb.append("\\b");
				break;

			case '\n':
				sb.append("\\n");
				break;

			case '\r':
				sb.append("\\r");
				break;

			case '\f':
				sb.append("\\f");
				break;

			case '\u2028':
			case '\u2029':
				sb.append(String.format("\\u%04x", (int) c));
				break;
			default:
				if (c <= 0x1F) {
					sb.append(String.format("\\u%04x", (int) c));
				} else {
					sb.append(c);
				}
				break;
			}
		}
		sb.append('"');
		return sb;
	}
}
