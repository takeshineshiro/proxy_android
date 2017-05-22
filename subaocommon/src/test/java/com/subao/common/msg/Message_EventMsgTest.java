package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.RoboBase;
import com.subao.common.data.AppType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Message_EventMsgTest
 * <p>Created by YinHaiBo on 2016/11/18.</p>
 */
public class Message_EventMsgTest extends RoboBase {

    private static AppType toAppType(int ordinal) {
        if (ordinal < 0) {
            return null;
        }

        AppType[] all = AppType.values();
        if (ordinal >= all.length) {
            return null;
        }

        return all[ordinal];
    }

    private static Message_VersionInfo createMessageVersionInfo(String number) {
        return Message_VersionInfo.create(number, "unknown_channel");
    }

    private static Message_EventMsg.Event deserializeEvent(JSONObject jsonObject) throws JSONException {
        String id = jsonObject.getString("id");
        long timeOfUTCSeconds = jsonObject.getLong("time");
        Map<String, String> map = null;
        if (jsonObject.has("paras")) {
            JSONArray paras = jsonObject.getJSONArray("paras");
            map = new HashMap<String, String>(paras.length());
            for (int i = 0; i < paras.length(); ++i) {
                JSONObject obj = paras.getJSONObject(i);
                String key;
                if (obj.has("key")) {
                    key = obj.getString("key");
                } else {
                    assertEquals(1, paras.length());
                    key = "";
                }
                assertNull(map.put(key, obj.getString("value")));
            }
        }
        return new Message_EventMsg.Event(id, timeOfUTCSeconds, map);
    }

    @Test
    public void serialize() throws IOException, JSONException {
        Message_VersionInfo versionInfo = createMessageVersionInfo("test");
        List<Message_EventMsg.Event> events = new ArrayList<Message_EventMsg.Event>(1);
        Map<String, String> map = new HashMap<String, String>(1);
        map.put("hello", "world");
        map.put("hi", "china");
        events.add(new Message_EventMsg.Event("event_id", 123456, map));
        MessageUserId messageUserId = MessageUserId.build();
        AppType appType = AppType.UNKNOWN_APPTYPE;
        Message_EventMsg msg = new Message_EventMsg(
            messageUserId, appType,
            versionInfo, events);
        //
        StringWriter stringWriter = new StringWriter(2048);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        msg.serialize(jsonWriter);
        //
        String json = stringWriter.toString();
        JSONObject jsonObject = new JSONObject(json);
        assertEquals(MessageUserIdTest.unserialize(jsonObject.getJSONObject("id")), messageUserId);
        assertEquals(appType.getId(), jsonObject.getInt("type"));
        assertEquals(versionInfo, Message_VersionInfoTest.unserialize(jsonObject.getJSONObject("version")));
        //
        JSONArray eventsObj = jsonObject.getJSONArray("events");
        assertEquals(1, eventsObj.length());
        Message_EventMsg.Event event = deserializeEvent(eventsObj.getJSONObject(0));
        assertEquals(2, event.getParamsCount());
        assertEquals("world", event.getParamValue("hello"));
        assertEquals("china", event.getParamValue("hi"));
        //
        event = new Message_EventMsg.Event("test", System.currentTimeMillis(), null);
        assertNull(event.getParamValue("key"));
    }

    @Test
    public void testMessageEventMsg() {
        List<Message_EventMsg.Event> events = new ArrayList<Message_EventMsg.Event>();

        for (int i = 0; i < 3; i++) {
            String id = "" + i;
            long time = System.currentTimeMillis() / 1000 + i;
            Map<String, String> map = new HashMap<String, String>();
            map.put(id + i, "abc" + id);
            Map<String, String> map1 = new HashMap<String, String>();
            map1.put("adb", "the value");

            Map<String, String> map2 = new HashMap<String, String>();
            map2.put("adv", "mdi");
            map2.put("key 3", "value 3");

            Message_EventMsg.Event event = new Message_EventMsg.Event(id, time, map);
            Message_EventMsg.Event event1 = new Message_EventMsg.Event(id, time, map);
            Message_EventMsg.Event event2 = new Message_EventMsg.Event(id + 1, time + 1, map);
            Message_EventMsg.Event event3 = new Message_EventMsg.Event(id, time, null);
            Message_EventMsg.Event event4 = new Message_EventMsg.Event(id, time, map1);
            Message_EventMsg.Event event5 = new Message_EventMsg.Event(id, time, map2);
            //Message_EventMsg.Event event6 = new Message_EventMsg.Event(otherId,time,map);


            String str = "test";
            assertTrue(event.getParamsCount() > 0);
            assertEquals(event3.getParamsCount(), 0);
            assertFalse(str.equals(event));
            assertFalse(event.equals(str));
            assertTrue(event.equals(event));
            assertTrue(event.equals(event1));
            assertFalse(event.equals(event2));
            assertFalse(event.equals(event3));
            assertFalse(event3.equals(event));
            assertFalse(event.equals(event4));
            assertFalse(event.equals(event5));
            //assertFalse(event.equals(event6));
            assertFalse(event.equals(null));
            assertNotNull(event.iterator());
            events.add(event);

            assertNotNull(event3.iterator());
            assertFalse(event3.iterator().hasNext());
            assertNull(event3.iterator().next());
            event3.iterator().remove();
        }

        int count = AppType.values().length;
        for (int i = 0; i < count; i++) {
            String subaoId = UUID.randomUUID().toString();
            MessageUserId.setCurrentSubaoId(subaoId);
            AppType type = toAppType(i);
            Message_VersionInfo versionInfo = createMessageVersionInfo("version_" + i);

            Message_EventMsg msg = new Message_EventMsg(MessageUserId.build(), type, versionInfo, events);
            assertNotNull(msg);
            assertTrue(msg.hasEvents());
            assertNotNull(msg.iterator());
        }

        String subaoId = UUID.randomUUID().toString();
        MessageUserId.setCurrentSubaoId(subaoId);
        AppType type = toAppType(5);
        Message_VersionInfo versionInfo = createMessageVersionInfo("version_" + 5);

        Message_EventMsg msg = new Message_EventMsg(MessageUserId.build(), type, versionInfo, null);
        assertNotNull(msg);
        assertFalse(msg.hasEvents());
        assertNotNull(msg.iterator());
        assertFalse(msg.iterator().hasNext());
        assertNull(msg.iterator().next());
        msg.iterator().remove();

        List<Message_EventMsg.Event> emptyEvents = new ArrayList<Message_EventMsg.Event>();
        Message_EventMsg msg1 = new Message_EventMsg(MessageUserId.build(), type, versionInfo, emptyEvents);
        assertFalse(msg1.hasEvents());
        assertNotNull(msg1.toString());
    }
}