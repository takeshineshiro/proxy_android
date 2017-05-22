package com.subao.common.utils;

import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.RoboBase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonUtilsTest extends RoboBase {

    private static abstract class Foo implements JsonSerializable {

        public abstract String getExpectedString();

    }

    private static class FooString extends Foo {

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.value("foo");
        }

        @Override
        public String getExpectedString() {
            return "\"foo\"";
        }

    }

    private static class FooArray extends Foo {

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginArray();
            writer.value(1);
            writer.value(2);
            writer.endArray();
        }

        @Override
        public String getExpectedString() {
            return "[1,2]";
        }
    }

    private StringWriter stringWriter;
    private JsonWriter jsonWriter;
    private int position;

    @Before
    public void setUp() throws IOException {
        stringWriter = new StringWriter(1024);
        jsonWriter = new JsonWriter(stringWriter);
        jsonWriter.beginObject();
        jsonWriter.flush();
        position = stringWriter.getBuffer().length();
    }

    @Test
    public void testWriteSerializableNull() throws IOException {
        JsonUtils.writeSerializable(jsonWriter, "filename", null);
        assertEquals(position, stringWriter.getBuffer().length());
    }

    @Test
    public void testWriteSerializableString() throws IOException {
        Foo foo = new FooString();
        JsonUtils.writeSerializable(jsonWriter, "filename", foo);
        jsonWriter.endObject();
        jsonWriter.flush();
        assertEquals(String.format("{\"filename\":%s}", foo.getExpectedString()), stringWriter.toString());
    }

    @Test
    public void testWriteSerializableArray() throws IOException {
        Foo foo = new FooArray();
        JsonUtils.writeSerializable(jsonWriter, "filename", foo);
        jsonWriter.endObject();
        jsonWriter.flush();
        assertEquals(String.format("{\"filename\":%s}", foo.getExpectedString()), stringWriter.toString());
    }

    @Test
    public void testWriteStringNull() throws IOException {
        JsonUtils.writeString(jsonWriter, "filename", null);
        jsonWriter.flush();
        assertEquals("{", stringWriter.toString());
    }

    @Test
    public void testWriteString() throws IOException {
        JsonUtils.writeString(jsonWriter, "filename", "value");
        jsonWriter.flush();
        assertEquals("{\"filename\":\"value\"", stringWriter.toString());
    }

    @Test
    public void testWriteIntNull() throws IOException {
        JsonUtils.writeNumber(jsonWriter, "filename", null);
        jsonWriter.flush();
        assertEquals("{", stringWriter.toString());
    }

    @Test
    public void testWriteInt() throws IOException {
        JsonUtils.writeNumber(jsonWriter, "filename", -123);
        jsonWriter.flush();
        assertEquals("{\"filename\":-123", stringWriter.toString());
    }

    @Test
    public void testWriteUnsignedIntNull() throws IOException {
        JsonUtils.writeUnsignedInt(jsonWriter, "filename", null);
        jsonWriter.flush();
        assertEquals("{", stringWriter.toString());
    }

    @Test
    public void testWriteUnsignedInt() throws IOException {
        JsonUtils.writeUnsignedInt(jsonWriter, "filename", -123);
        jsonWriter.flush();
        assertEquals("{\"filename\":4294967173", stringWriter.toString());
    }

    @Test
    public void testReadNextStringNull() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("{\"filename\":null,\"hello\":1}"));
        reader.beginObject();
        reader.nextName();
        assertNull(JsonUtils.readNextString(reader));
        assertEquals("hello", reader.nextName());
        assertEquals(1, reader.nextInt());
    }

    @Test
    public void testReadNextString() throws IOException {
        JsonReader reader = new JsonReader(new StringReader("{\"filename\":\"null\"}"));
        reader.beginObject();
        reader.nextName();
        assertEquals("null", JsonUtils.readNextString(reader));
    }

    @Test
    public void testEncode1() {
        StringBuilder sb = new StringBuilder(16);
        JsonUtils.encode(sb, null);
        assertEquals("null", sb.toString());
        assertEquals("null", JsonUtils.encode(null));
    }

    @Test
    public void testEncode2() {
        assertEquals(quotation(""), JsonUtils.encode(""));
        assertEquals(quotation("abc"), JsonUtils.encode("abc"));
        assertEquals(quotation("\\\""), JsonUtils.encode("\""));
        assertEquals(quotation("\\\\"), JsonUtils.encode("\\"));
        assertEquals(quotation("\\t"), JsonUtils.encode("\t"));
        assertEquals(quotation("\\r"), JsonUtils.encode("\r"));
        assertEquals(quotation("\\n"), JsonUtils.encode("\n"));
        assertEquals(quotation("\\f"), JsonUtils.encode("\f"));
        assertEquals(quotation("\\b"), JsonUtils.encode("\b"));
        assertEquals(quotation("\\u2028"), JsonUtils.encode("\u2028"));
        assertEquals(quotation("\\u2029"), JsonUtils.encode("\u2029"));
        //
        for (int i = 0; i < 32; ++i) {
            switch (i) {
            case '\r':
            case '\n':
            case '\t':
            case '\b':
            case '\f':
                continue;
            }
            assertEquals(quotation(String.format("\\u%04x", i)), JsonUtils.encode(String.format("%c", i)));
        }

    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private static String quotation(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"').append(s).append('"');
        return sb.toString();
    }

	@Test
	public void testConstructor() {
		new JsonUtils();
	}

    @Test
    public void writeFloat() throws IOException, JSONException {
        Float f = Float.valueOf(1.23f);
        JsonUtils.writeNumber(jsonWriter, "f", f);
        jsonWriter.endObject();
        jsonWriter.flush();
        //
        JSONObject obj = new JSONObject(stringWriter.toString());
        Float f2 = new Float((Double)obj.get("f"));
        assertEquals(f2, f);
    }
}
