package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.data.AppType;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class MessageGamingTester extends RoboBase {

	@Test
	public void testAccelMode() {

		assertEquals(Message_Gaming.AccelMode.values().length, 4);

		assertEquals(0, Message_Gaming.AccelMode.UNKNOWN_ACCEL_MODE.getId());
		assertEquals(1, Message_Gaming.AccelMode.NOT_ACCEL_MODE.getId());
		assertEquals(2, Message_Gaming.AccelMode.ROOT_MODE.getId());
		assertEquals(3, Message_Gaming.AccelMode.VPN_MODE.getId());
	}

	@Test
	public void testMessageGaming() {
		MessageUserId msgUserId = new MessageUserId(UUID.randomUUID().toString(), "userId", "deviceId", 2, "110");
		long time = System.currentTimeMillis() / 1000;
		AppType appType = AppType.ANDROID_SDK;
		Message_App game = new Message_App("game", "com.example.app");
		Message_VersionInfo version = Message_VersionInfo.create("2.2.4", "g_official");
		Message_Gaming.AccelMode mode = Message_Gaming.AccelMode.VPN_MODE;
		Message_DeviceInfo device = new Message_DeviceInfo("model", 100, 2, 1024, "rom");
		Message_Gaming gaming = new Message_Gaming(msgUserId, time, appType, game, mode, version, device);
		//
		assertEquals(msgUserId, gaming.id);
		assertEquals(time, gaming.time);
		assertEquals(appType, gaming.appType);
		assertEquals(game, gaming.game);
		assertEquals(version, gaming.version);
		assertEquals(mode, gaming.mode);
		assertEquals(device, gaming.device);
	}

	@Test
	public void testSerial() throws IOException, JSONException {
		for (AppType appType : AppType.values()) {
			MessageUserId msgUserId = new MessageUserId("id", "userId", "abc", 1, "110");
			long time = System.currentTimeMillis() / 1000;
			Message_App game = new Message_App("game", "cn.wsds.gams");
			Message_VersionInfo version = Message_VersionInfo.create("2.2.4", "g_official");
			Message_Gaming.AccelMode mode = Message_Gaming.AccelMode.VPN_MODE;
			Message_DeviceInfo device = new Message_DeviceInfo("model", 100, 2, 1024, "rom");
			Message_Gaming gaming = new Message_Gaming(msgUserId, time, appType, game, mode, version, device);
			//
			String s = msg2str(gaming);
			JSONObject json = new JSONObject(s);
			JSONObject id = json.getJSONObject("id");
			assertEquals("id", id.getString("id"));
			assertEquals("userId", id.getString("userId"));
			assertEquals("abc", id.getString("serviceId"));
			assertEquals(1, id.getInt("stat"));
			//
			assertEquals(time, json.getLong("time"));
		}
	}

	private static String msg2str(Message_Gaming msg) throws IOException {
		StringWriter sw = new StringWriter(1024);
		JsonWriter writer = new JsonWriter(sw);
		msg.serialize(writer);
		Misc.close(writer);
		return sw.toString();
	}
}
