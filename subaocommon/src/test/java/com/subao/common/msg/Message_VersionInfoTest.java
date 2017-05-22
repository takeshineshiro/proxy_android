package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class Message_VersionInfoTest extends RoboBase {

    private static final String NUMBER = "2.2.4";
    private static final String CHANNEL = "g_official";
    private static final String OS_VERSION = "MIUI 7.0";
    private static final String ANDROID_VERSION = "Android 5.0";

    private static String getStringFromJsonObject(JSONObject jsonObject, String name) throws JSONException {
        if (jsonObject.has(name)) {
            return jsonObject.getString(name);
        }
        return null;
    }

    public static Message_VersionInfo unserialize(JSONObject jsonObject) throws JSONException {
        String number = getStringFromJsonObject(jsonObject, "number");
        String channel = getStringFromJsonObject(jsonObject, "channel");
        String osVersion = getStringFromJsonObject(jsonObject, "osVersion");
        String androidVersion = getStringFromJsonObject(jsonObject, "androidVersion");
        return new Message_VersionInfo(number, channel, osVersion, androidVersion);
    }

    @Test
	public void testMessageVersionInfo() {
        Message_VersionInfo info = createMessageVersionInfo();
		assertEquals(NUMBER, info.number);
		assertEquals(CHANNEL, info.channel);
		assertEquals(OS_VERSION, info.osVersion);
		assertEquals(ANDROID_VERSION, info.androidVersion);
	}

    public Message_VersionInfo createMessageVersionInfo() {
        return new Message_VersionInfo(NUMBER, CHANNEL, OS_VERSION, ANDROID_VERSION);
    }

    @Test
	public void testCreate() {
		String number = "2.2.4";
		String channel = "g_official";
		Message_VersionInfo info = Message_VersionInfo.create(number, channel);
		assertEquals(number, info.number);
		assertEquals(channel, info.channel);
	}
	
	@Test
	public void serialize() throws IOException {
		Message_VersionInfo info = createMessageVersionInfo();
		//
		StringWriter sw = new StringWriter(1024);
		JsonWriter writer = new JsonWriter(sw);
		info.serialize(writer);
		Misc.close(writer);
		//
		String expected = String.format("{\"number\":\"%s\",\"channel\":\"%s\",\"osVersion\":\"%s\",\"androidVersion\":\"%s\"}",
			info.number, info.channel, info.osVersion, info.androidVersion);
		assertEquals(expected, sw.toString());
	}

    @Test
    public void testEquals() {
        Message_VersionInfo info = createMessageVersionInfo();
        assertEquals(info, info);
        assertFalse(info.equals(null));
        assertFalse(info.equals(this));
        assertEquals(info, createMessageVersionInfo());
        assertFalse(info.equals(new Message_VersionInfo(null, CHANNEL, OS_VERSION, ANDROID_VERSION)));
        assertFalse(info.equals(new Message_VersionInfo(NUMBER, null, OS_VERSION, ANDROID_VERSION)));
        assertFalse(info.equals(new Message_VersionInfo(NUMBER, CHANNEL, null, ANDROID_VERSION)));
        assertFalse(info.equals(new Message_VersionInfo(NUMBER, CHANNEL, OS_VERSION, null)));
    }
}
