package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.RoboBase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MessageUserIdTest extends RoboBase {

    private static String getStringFromJsonObject(JSONObject obj, String name) throws JSONException {
        if (obj.has(name)) {
            return obj.getString(name);
        } else {
            return null;
        }
    }

    public static MessageUserId unserialize(JSONObject obj) throws JSONException {
        String id = getStringFromJsonObject(obj, "id");
        String userId =getStringFromJsonObject(obj, "userId");
        String serviceId = getStringFromJsonObject(obj, "serviceId");
        int stat = obj.getInt("stat");
        String userConfig = getStringFromJsonObject(obj, "config");
        return new MessageUserId(id, userId, serviceId, stat, userConfig);
    }

	@Test
	public void testMessageUserId() {
		MessageUserId mui = new MessageUserId("1", "2", "3", 4, "110");
		assertEquals("1", mui.subaoId);
		assertEquals("2", mui.userId);
		assertEquals("3", mui.serviceId);
		assertEquals(4, mui.userStatus);
        assertEquals("110", mui.userConfig);
		assertNotNull(mui.toString());
	}
	
	@Test
	public void testEquals() {
		MessageUserId mui = new MessageUserId("1", "2", "3", 4, "110");
		assertTrue(mui.equals(mui));
		assertFalse(mui.equals(null));
		assertFalse(mui.equals(new Object()));
		assertTrue(mui.equals(new MessageUserId("1", "2", "3", 4, "110")));
		assertFalse(mui.equals(new MessageUserId("1", "2", null, 4, "110")));
		assertFalse(mui.equals(new MessageUserId("1", null, "3", 4, "110")));
		assertFalse(mui.equals(new MessageUserId(null, "2", "3", 4, "110")));
		assertFalse(mui.equals(new MessageUserId("1", "2", "3", 5, "110")));
        assertFalse(mui.equals(new MessageUserId("1", "2", "3", 4, "111")));
    }

	@Test
	public void testSerializeToJson() throws IOException, JSONException {
		MessageUserId mui = new MessageUserId("1", "2", "3", 4, "110");
		StringWriter sw = new StringWriter(512);
		JsonWriter writer = new JsonWriter(sw);
		mui.serialize(writer);
		writer.close();
		//
		JSONObject obj = new JSONObject(sw.toString());
		assertEquals(mui.subaoId, obj.getString("id"));
		assertEquals(mui.userId, obj.getString("userId"));
		assertEquals(mui.serviceId, obj.getString("serviceId"));
		assertEquals(mui.userStatus, obj.getInt("stat"));
        assertEquals(mui.userConfig, obj.getString("config"));
	}

	@Test
	public void testSubaoId() {
		MessageUserId mui = new MessageUserId("1", "2", "3", 4, "110");
		assertFalse(mui.isSubaoIdValid());
		mui = new MessageUserId("6914CEAF-D141-48DF-A856-BC2FDB2C6C95", null, null, 0, "110");
		assertTrue(mui.isSubaoIdValid());
	}
}
