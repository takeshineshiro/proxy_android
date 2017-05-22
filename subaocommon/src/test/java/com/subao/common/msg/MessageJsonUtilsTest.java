package com.subao.common.msg;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.data.ChinaISP;
import com.subao.common.data.ChinaRegion;
import com.subao.common.data.RegionAndISP;
import com.subao.common.msg.MessageJsonUtils.LinkMsgData;
import com.subao.common.msg.MessageJsonUtils.MessageLinkJsonTranslater;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MessageJsonUtilsTest extends RoboBase {

    private static String toJson(JsonSerializable obj) throws IOException {
        StringWriter sw = new StringWriter(256);
        JsonWriter writer = new JsonWriter(sw);
        obj.serialize(writer);
        Misc.close(writer);
        return sw.toString();
    }

    @Test
    public void testParseSubaoIdFromJson() {
        String id = "0CEF65A2-4FAB-45E9-967B-3F4880DC013C";
        String json = String.format("{\"id\":{\"hello\":\"world\"}, \"foo\":1, \"id\":{\"bar\":\"bar\", \"id\":\"%s\"}}", id);
        Assert.assertEquals(id, MessageJsonUtils.parseSubaoIdFromJson(json.getBytes()));
        Assert.assertEquals(null, MessageJsonUtils.parseSubaoIdFromJson(null));
        Assert.assertEquals(null, MessageJsonUtils.parseSubaoIdFromJson("".getBytes()));
        Assert.assertEquals(null, MessageJsonUtils.parseSubaoIdFromJson("Invalid Json".getBytes()));
        Assert.assertEquals(null, MessageJsonUtils.parseSubaoIdFromJson("{}".getBytes()));
    }

    @Test
    public void testLinkMsgData() {
        String jsonFromJNI = "1";
        String netDetail = "2";
        String gameServerId = "体验服";
        MessageSender.FreeFlowType freeFlowType = MessageSender.FreeFlowType.CMCC;
        long time = 1234L;
        LinkMsgData data = new LinkMsgData(jsonFromJNI, netDetail, gameServerId, freeFlowType, time);
        assertEquals(jsonFromJNI, data.jsonFromJNI);
        assertEquals(netDetail, data.netDetail);
        assertEquals(gameServerId, data.gameServerId);
        assertEquals(freeFlowType, data.freeFlowType);
        assertEquals(time, data.timeMillis);
    }

    private Message_Link.DelayQuality createDelayQuality() {
        return new Message_Link.DelayQuality(
            1.23f, 2.34f, 3.45f, 6f, 1f, 3
        );
    }

    @Test
    public void testMessageLinkJsonTranslater1() throws Exception {
        String sessionId = "xxxxxxxxxxasdfk23";
        String netDetail = "{\"hello\"}";
        List<LinkMsgData> list = new ArrayList<LinkMsgData>(5);
        list.add(new LinkMsgData("{\"a\":1, \"network\" : {\"type\":1}}", netDetail, "hello",
            MessageSender.FreeFlowType.CMCC, 1234L));
        String json = MessageLinkJsonTranslater.execute(list, sessionId, null);
        //
        JSONObject obj = new JSONObject(json);
        JSONArray array = obj.getJSONArray("links");
        assertEquals(1, array.length());
        obj = array.getJSONObject(0);
        JSONObject id = (JSONObject) obj.get("id");
        assertEquals(sessionId, id.getString("id"));
        //
        assertEquals(1, obj.getInt("a"));
        assertEquals("hello", obj.getString("serverId"));
        assertEquals(MessageSender.FreeFlowType.CMCC.intValue, obj.getInt("flowType"));
        //
        JSONObject network = obj.getJSONObject("network");
        assertEquals(1, network.getInt("type"));
        assertEquals(netDetail, network.getString("detail"));
        //
//        JSONObject fdq = obj.getJSONObject("feedback");
//        assertTrue(Float.compare((float) fdq.getDouble("delayAvg"), 1.23f) == 0);
//        assertTrue(Float.compare((float) fdq.getDouble("delaySD"), 2.34f) == 0);
//        assertTrue(Float.compare((float) fdq.getDouble("lossRatio"), 3.45f) == 0);
//        assertTrue(Float.compare((float) fdq.getDouble("delayMax"), 6f) == 0);
//        assertTrue(Float.compare((float) fdq.getDouble("delayMin"), 1f) == 0);
    }

    @Test
    public void testMessageLinkJsonTranslater2() throws Exception {
        String sessionId = "xxxxxxxxxxasdfk23";
        String netDetail = "{\"hello\"}";
        List<LinkMsgData> list = new ArrayList<LinkMsgData>(5);
        list.add(new LinkMsgData("{\"a\":1, \"network\" : {}}", netDetail, null, null, 1234L));
        String json = MessageLinkJsonTranslater.execute(list, sessionId, null);
        //
        JSONObject obj = new JSONObject(json);
        JSONArray array = obj.getJSONArray("links");
        assertEquals(1, array.length());
        obj = array.getJSONObject(0);
        JSONObject id = (JSONObject) obj.get("id");
        assertEquals(sessionId, id.getString("id"));
        //
        assertEquals(1, obj.getInt("a"));
        assertFalse(obj.has("serverId"));
        assertFalse(obj.has("flowType"));
        //
        JSONObject network = obj.getJSONObject("network");
        assertEquals(netDetail, network.getString("detail"));
    }

    @Test
    public void testMessageLinkJsonTranslater3() throws IOException, JSONException {
        String[] jsonFromJNIList = new String[]{
            "{\"id\":{\"id\":\"ecfd1542-deef-432e-be40-d578974a460d\"},\"ip\":\"122.224.73.168\",\"port\":9011,\"protocal\":2,\"network\":{\"type\":1,\"detail\":\"\\\"Hello World\\\"\"},\"stat\":1,\"nodeIp\":\"122.224.73.168\",\"clientIp\":\"223.87.36.144\",\"measureOutcome\":{\"accessDelay\":54, \"forwardDelay\":1},\"compareOutcome\":{\"directDelay\":53, \"directLossRatio\":0.00, \"proxyDelay\":52, \"proxyLossRatio\":0.00},\"echoPort\":222,\"startTime\":1482573999,\"route\":[{\"ip\":\"122.224.73.168\",\"localDelay\":1,\"peerDelay\":4} ],\"accelInfo\":{\"qosInfo\":{\"support\":false}, \"multipathInfo\":{\"support\":false}, \"method\":1}}",
            "{\n" +
                "    \"accelInfo\": {\n" +
                "        \"qosInfo\": {}\n" +
                "    }\n" +
                "}",
            "{\n" +
                "    \"accelInfo\": {\n" +
                "        \"qosInfo\": {\n" +
                "            \"hello\": null\n" +
                "        }\n" +
                "    }\n" +
                "}",
        };
        for (String jsonFromJNI : jsonFromJNIList) {
            LinkMsgData linkMsgData = new LinkMsgData(jsonFromJNI, "the net detail",
                "game server id", MessageSender.FreeFlowType.CMCC, System.currentTimeMillis());
            List<LinkMsgData> links = new ArrayList<LinkMsgData>(1);
            links.add(linkMsgData);
            //
            RegionAndISP[] regionAndISPList = new RegionAndISP[]{
                null, new RegionAndISP(ChinaRegion.Beijing.num, ChinaISP.CHINA_UNICOM.num),
            };
            for (RegionAndISP regionAndISP : regionAndISPList) {
                String json = MessageJsonUtils.MessageLinkJsonTranslater.execute(links, "session id", regionAndISP);
                JSONObject jsonObject = new JSONObject(json);
                JSONObject link = jsonObject.getJSONArray("links").getJSONObject(0);
                JSONObject accelInfo = link.getJSONObject("accelInfo");
                JSONObject qosAccelInfo = accelInfo.getJSONObject("qosInfo");
                //
                String expected = RegionAndISP.toText(regionAndISP);
                if (expected == null) {
                    assertFalse(qosAccelInfo.has("isp"));
                } else {
                    assertEquals(expected, qosAccelInfo.getString("isp"));
                }
            }
        }
    }

    @Test
    public void testInsertObjectToJson() throws IOException {
        String raw = "\"hello\":\"world\"";
        String rawJson = "{" + raw + "}";
        JsonSerializable js = new JsonSerializable() {
            @Override
            public void serialize(JsonWriter writer) throws IOException {
                writer.beginObject();
                writer.name("1").value("2");
                writer.endObject();
            }
        };
        String json = MessageJsonUtils.insertObjectToJson(rawJson, null, js);
        assertEquals("{\"1\":\"2\"," + raw + "}", json);
        json = MessageJsonUtils.insertObjectToJson(rawJson, "test", js);
        assertEquals("{\"test\":" + toJson(js) + "," + raw + "}", json);
    }
}
