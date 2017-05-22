package com.subao.common.msg;

import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.mock.MockPersistent;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 针对{@link Message_Link}的单元测试
 */
public class Message_LinkTest extends RoboBase {

    private static void checkDuplicate(List<Integer> exists, int newValue) {
        assertFalse(exists.contains(newValue));
        exists.add(newValue);
    }

    private static Message_Link.QosAccelInfo parseQosAccelInfoFromJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        boolean support = obj.getBoolean("support");
        boolean open = obj.getBoolean("open");
        String isp = obj.has("isp") ? obj.getString("isp") : null;
        Integer duration = obj.has("duration") ? obj.getInt("duration") : null;
        return new Message_Link.QosAccelInfo(support, open, duration, isp);
    }

    private static Message_Link.WiFiAccelInfo parseWiFiAccelInfoFromJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        boolean support = obj.getBoolean("support");
        boolean open = obj.getBoolean("open");
        Integer duration = obj.has("duration") ? obj.getInt("duration") : null;
        Integer traffic = obj.has("traffic") ? obj.getInt("traffic") : null;
        return new Message_Link.WiFiAccelInfo(support, open, duration, traffic);
    }

    @Test
    public void constructor() {
        new Message_Link();
    }

//	@Test
//	public void testEnumProtocal() {
//		int count = 3;
//		assertEquals(count, Message_Link.Protocol.values().valueLength);
//		List<Integer> valueExists = new ArrayList<Integer>(count);
//		int id;
//		id = 0;
//		assertEquals(id, Message_Link.Protocol.UNKNOWN_PROTOCAL.getId());
//		checkDuplicate(valueExists, id);
//		id = 1;
//		assertEquals(id, Message_Link.Protocol.TCP.getId());
//		checkDuplicate(valueExists, id);
//		id = 2;
//		assertEquals(id, Message_Link.Protocol.UDP.getId());
//		checkDuplicate(valueExists, id);
//	}

    @Test
    public void delayQuality() {
        Message_Link.DelayQuality dq = new Message_Link.DelayQuality(1.23f, 2.34f, 3.45f, 6f, 1f, 3);
        assertNotNull(dq.toString());
        assertTrue(dq.equals(dq));
        assertFalse(dq.equals(null));
        assertFalse(dq.equals(new Object()));
        Message_Link.DelayQuality dq2 = new Message_Link.DelayQuality(100f, 2.34f, 3.45f, 6f, 1f, 3);
        assertFalse(dq.equals(dq2));
        dq2 = new Message_Link.DelayQuality(1.23f, 100f, 3.45f, 6f, 1f, 3);
        assertFalse(dq.equals(dq2));
        dq2 = new Message_Link.DelayQuality(1.23f, 2.34f, 100f, 6f, 1f, 3);
        assertFalse(dq.equals(dq2));
        dq2 = new Message_Link.DelayQuality(1.23f, 2.34f, 3.45f, 16f, 1f, 3);
        assertFalse(dq.equals(dq2));
        dq2 = new Message_Link.DelayQuality(1.23f, 2.34f, 3.45f, 6f, 100f, 3);
        assertFalse(dq.equals(dq2));
        dq2 = new Message_Link.DelayQuality(1.23f, 2.34f, 3.45f, 6f, 100f, null);
        assertFalse(dq.equals(dq2));
        dq2 = new Message_Link.DelayQuality(1.23f, 2.34f, 3.45f, 6f, 1f, 3);
        assertTrue(dq.equals(dq2));
    }

//	@Test
//	public void testEnumQosStats() {
//		int count = 4;
//		assertEquals(count, Message_Link.QosStats.values().valueLength);
//		List<Integer> valueExists = new ArrayList<Integer>(count);
//		int id;
//		id = 0;
//		assertEquals(id, Message_Link.QosStats.UNKNOWN_QOS.getId());
//		checkDuplicate(valueExists, id);
//		id = 1;
//		assertEquals(id, Message_Link.QosStats.NO_QOS.getId());
//		checkDuplicate(valueExists, id);
//		id = 2;
//		assertEquals(id, Message_Link.QosStats.QOS_SUCCESS.getId());
//		checkDuplicate(valueExists, id);
//		id = 3;
//		assertEquals(id, Message_Link.QosStats.QOS_FAIL.getId());
//		checkDuplicate(valueExists, id);
//	}

//	@Test
//	public void testEnumAccelStat() {
//		int count = 16;
//		assertEquals(count, Message_Link.AccelStat.values().valueLength);
//		List<Integer> valueExists = new ArrayList<Integer>(count);
//		int id;
//		id = 0;
//		assertEquals(id, Message_Link.AccelStat.UNKNOWN_ACCEL_STATE.getId());
//		checkDuplicate(valueExists, id);
//		id = 1;
//		assertEquals(id, Message_Link.AccelStat.NORMAL.getId());
//		checkDuplicate(valueExists, id);
//		id = 2;
//		assertEquals(id, Message_Link.AccelStat.NOT_PROXY.getId());
//		checkDuplicate(valueExists, id);
//		id = 3;
//		assertEquals(id, Message_Link.AccelStat.DIRECT_FAIL.getId());
//		checkDuplicate(valueExists, id);
//		id = 4;
//		assertEquals(id, Message_Link.AccelStat.DIRECT_TIMEOUT.getId());
//		checkDuplicate(valueExists, id);
//		id = 5;
//		assertEquals(id, Message_Link.AccelStat.TCP_SYN_FAIL.getId());
//		checkDuplicate(valueExists, id);
//		id = 6;
//		assertEquals(id, Message_Link.AccelStat.PROXY_FAIL.getId());
//		checkDuplicate(valueExists, id);
//		id = 7;
//		assertEquals(id, Message_Link.AccelStat.PROXY_TIMEOUT.getId());
//		checkDuplicate(valueExists, id);
//		id = 8;
//		assertEquals(id, Message_Link.AccelStat.BOTH_FAIL.getId());
//		checkDuplicate(valueExists, id);
//		id = 9;
//		assertEquals(id, Message_Link.AccelStat.NETWORK_ERROR.getId());
//		checkDuplicate(valueExists, id);
//		id = 10;
//		assertEquals(id, Message_Link.AccelStat.UDP_ECHO_FAIL.getId());
//		checkDuplicate(valueExists, id);
//		id = 11;
//		assertEquals(id, Message_Link.AccelStat.SPEED_MEASURE_FAIL.getId());
//		checkDuplicate(valueExists, id);
//		id = 12;
//		assertEquals(id, Message_Link.AccelStat.SPEED_MEASUREING.getId());
//		checkDuplicate(valueExists, id);
//		id = 13;
//		assertEquals(id, Message_Link.AccelStat.MISS_IP.getId());
//		checkDuplicate(valueExists, id);
//		id = 14;
//		assertEquals(id, Message_Link.AccelStat.INVALID_DELAY.getId());
//		checkDuplicate(valueExists, id);
//		id = 128;
//		assertEquals(id, Message_Link.AccelStat.STAT_INVALID.getId());
//	}

    @Test
    public void delayQualitySerialize() throws IOException {
        Message_Link.DelayQuality dq = new Message_Link.DelayQuality(1.23f, 2.34f, 3.45f, null, null, null);
        MockPersistent mp = new MockPersistent();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(mp.openOutput()));
        dq.serialize(writer);
        Misc.close(writer);
        assertTrue(mp.getData().length > 0);
        //
        String json = dq.toString();
        json = "{\"hello\":null," + json.substring(1);
        for (int i = 0; i < 2; ++i) {
            assertTrue(json.length() > 0);
            mp.setData(json.getBytes());
            JsonReader reader = new JsonReader(new InputStreamReader(mp.openInput()));
            Message_Link.DelayQuality dq2 = Message_Link.DelayQuality.parseFromJson(reader);
            Misc.close(reader);
            assertEquals(dq, dq2);
            //
            dq = new Message_Link.DelayQuality(1.23f, 2.34f, 3.45f, 6f, 1f, 3);
            json = dq.toString();
        }
    }

    //	@Test
    //	public void testDelayQuality() throws IOException {
    //		DelayQuality dq = new DelayQuality(1.2f, 3.4f, 5.6f, 7f, 8f);
    //		assertEquals(0, Float.compare(dq.delayAvg, 1.2f));
    //		assertEquals(0, Float.compare(dq.delaySD, 3.4f));
    //		assertEquals(0, Float.compare(dq.lossRatio, 5.6f));
    //		assertEquals(0, Float.compare(dq.delayMax, 7f));
    //		assertEquals(0, Float.compare(dq.delayMin, 8f));
    //
    //		//
    //		assertEquals(dq, dq);
    //		assertFalse(dq.equals(null));
    //		assertFalse(dq.equals(new Object()));
    //		DelayQuality dq2 = new DelayQuality(dq.delayAvg, dq.delaySD, dq.lossRatio, dq.delayMax, dq.delayMin);
    //		assertEquals(dq, dq2);
    //		assertFalse(dq.equals(new DelayQuality(1.1f, 3.4f, 5.6f, 7f, 8f)));
    //		assertFalse(dq.equals(new DelayQuality(1.2f, 3.3f, 5.6f, 7f, 8f)));
    //		assertFalse(dq.equals(new DelayQuality(1.2f, 3.4f, 5.5f, 7f, 8f)));
    //		assertFalse(dq.equals(new DelayQuality(1.2f, 3.4f, 5.6f, 8f, 8f)));
    //		assertFalse(dq.equals(new DelayQuality(1.2f, 3.4f, 5.6f, 7f, 7f)));
    //		//
    //		assertNotNull(dq.toString());
    //		//
    //		StringWriter sw = new StringWriter(1024);
    //		JsonWriter writer = new JsonWriter(sw);
    //		dq.serialize(writer);
    //		Misc.close(writer);
    //		//
    //		JsonReader reader = new JsonReader(new StringReader(sw.toString()));
    //		dq2 = DelayQuality.parseFromJson(reader);
    //		Misc.close(reader);
    //		assertEquals(dq, dq2);
    //
    //	}

    //	@Test
    //	public void testNetworkQuality() {
    //		Random random = new Random();
    //		for (QosStats qosStats : QosStats.values()) {
    //			DelayQuality beforeQos = new DelayQuality(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat());
    //			DelayQuality afterQos = new DelayQuality(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat());
    //			String qosID = Double.toString(random.nextDouble());
    //			int duration = random.nextInt(1000);
    //			NetworkQuality nq = new NetworkQuality(qosStats, beforeQos, afterQos, qosID, duration);
    //			assertEquals(qosStats, nq.qos);
    //			assertEquals(beforeQos, nq.beforeQos);
    //			assertEquals(afterQos, nq.afterQos);
    //			assertEquals(qosID, nq.qosID);
    //			assertEquals(duration, nq.duration);
    //			//
    //			NetworkQuality nq2 = new NetworkQuality(qosStats, beforeQos, afterQos, qosID, duration);
    //			assertEquals(nq, nq2);
    //			//
    //			assertEquals(nq, nq);
    //			assertFalse(nq.equals(null));
    //			assertFalse(nq.equals(new Object()));
    //			assertFalse(nq.equals(new NetworkQuality(qosStats, beforeQos, afterQos, "xx", 0)));
    //		}
    //	}

    //	@Test
    //	public void testNetwork() {
    //		Random random = new Random();
    //		for (QosStats qosStats : QosStats.values()) {
    //			for (NetworkType nt : NetworkType.values()) {
    //				DelayQuality beforeQos = new DelayQuality(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat());
    //				DelayQuality afterQos = new DelayQuality(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat());
    //				String qosID = Double.toString(random.nextDouble());
    //				int duration = random.nextInt(1000);
    //				NetworkQuality networkQuality = new NetworkQuality(qosStats, beforeQos, afterQos, qosID, duration);
    //				String detail = Double.toString(Math.random());
    //				Network network = new Network(nt, detail, networkQuality);
    //				//
    //				assertEquals(nt, network.type);
    //				assertEquals(detail, network.detail);
    //				assertEquals(networkQuality, network.quality);
    //				//
    //				Network network2 = new Network(nt, detail, networkQuality);
    //				assertEquals(network2, network);
    //				//
    //				assertEquals(network, network);
    //				assertFalse(network.equals(null));
    //				assertFalse(network.equals(new Object()));
    //			}
    //		}
    //	}

    //	@Test
    //	public void testEffectStatistics() {
    //		Random random = new Random();
    //		DelayQuality direct = new DelayQuality(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat());
    //		DelayQuality proxy = new DelayQuality(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat());
    //		int duration = random.nextInt(1000);
    //		EffectStatistics es = new EffectStatistics(direct, proxy, duration);
    //		assertEquals(direct, es.direct);
    //		assertEquals(proxy, es.proxy);
    //		assertEquals(duration, es.duration);
    //		//
    //		assertTrue(es.equals(es));
    //		assertFalse(es.equals(null));
    //		assertFalse(es.equals(new Object()));
    //		assertEquals(es, new EffectStatistics(direct, proxy, duration));
    //		assertFalse(es.equals(new EffectStatistics(direct, proxy, duration + 1)));
    //	}

    @Test
    public void testEnumNetworkType() {
        int count = 6;
        assertEquals(count, Message_Link.NetworkType.values().length);
        List<Integer> exists = new ArrayList<Integer>(count);
        int id;
        id = 0;
        assertEquals(id, Message_Link.NetworkType.UNKNOWN_NETWORKTYPE.getId());
        checkDuplicate(exists, id);
        id = 1;
        assertEquals(id, Message_Link.NetworkType.WIFI.getId());
        checkDuplicate(exists, id);
        id = 2;
        assertEquals(id, Message_Link.NetworkType.MOBILE_2G.getId());
        checkDuplicate(exists, id);
        id = 3;
        assertEquals(id, Message_Link.NetworkType.MOBILE_3G.getId());
        checkDuplicate(exists, id);
        id = 4;
        assertEquals(id, Message_Link.NetworkType.MOBILE_4G.getId());
        checkDuplicate(exists, id);
        id = 5;
        assertEquals(id, Message_Link.NetworkType.MOBILE_5G.getId());
        checkDuplicate(exists, id);
    }

    @Test
    public void testNetwork() throws IOException {
        MockPersistent mp = new MockPersistent();
        for (Message_Link.NetworkType nt : Message_Link.NetworkType.values()) {
            String detail = Double.toString(Math.random());
            Message_Link.Network network = new Message_Link.Network(nt, detail);
            //
            assertEquals(nt, network.type);
            assertEquals(detail, network.detail);
            //
            Message_Link.Network network2 = new Message_Link.Network(nt, detail);
            assertEquals(network2, network);
            //
            assertEquals(network, network);
            assertFalse(network.equals(null));
            assertFalse(network.equals(new Object()));
            //
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(mp.openOutput()));
            network.serialize(writer);
            Misc.close(writer);
            assertTrue(mp.getData().length > 0);
        }
    }

    @Test
    public void testWiFiAccelInfo() throws JSONException {
        boolean[] supportList = new boolean[]{false, true};
        boolean[] openList = new boolean[]{false, true};
        Integer[] durationList = new Integer[]{null, 123};
        Integer[] trafficList = new Integer[]{null, 456};
        for (boolean support : supportList) {
            for (boolean open : openList) {
                for (Integer duration : durationList) {
                    for (Integer trafiic : trafficList) {
                        Message_Link.WiFiAccelInfo wifiAccelInfo = new Message_Link.WiFiAccelInfo(support, open, duration, trafiic);
                        assertEquals(support, wifiAccelInfo.support);
                        assertEquals(open, wifiAccelInfo.open);
                        //
                        String json = wifiAccelInfo.toString();
                        Message_Link.WiFiAccelInfo parsed = parseWiFiAccelInfoFromJson(json);
                        assertEquals(wifiAccelInfo, parsed);
                        //
                        assertEquals(wifiAccelInfo, wifiAccelInfo);
                        assertFalse(wifiAccelInfo.equals(null));
                        assertFalse(wifiAccelInfo.equals(new Object()));
                        assertFalse(wifiAccelInfo.equals(new Message_Link.QosAccelInfo(
                            wifiAccelInfo.support, wifiAccelInfo.open,
                            wifiAccelInfo.duration, null
                        )));
                    }
                }
            }
        }
    }

    @Test
    public void testQosAccelInfo() throws JSONException, IOException {
        for (boolean support : QosAccelInfoFactory.SUPPORT_LIST) {
            for (boolean open : QosAccelInfoFactory.OPEN_LIST) {
                for (String isp : QosAccelInfoFactory.ISP_LIST) {
                    for (Integer duration : QosAccelInfoFactory.DURATION_LIST) {
                        Message_Link.QosAccelInfo qosAccelInfo = new Message_Link.QosAccelInfo(support, open, duration, isp);
                        assertEquals(support, qosAccelInfo.support);
                        assertEquals(open, qosAccelInfo.open);
                        assertEquals(isp, qosAccelInfo.isp);
                        //
                        String json = qosAccelInfo.toString();
                        Message_Link.QosAccelInfo parsed = parseQosAccelInfoFromJson(json);
                        assertEquals(qosAccelInfo, parsed);
                        //
                        assertEquals(qosAccelInfo, qosAccelInfo);
                        assertFalse(qosAccelInfo.equals(null));
                        assertFalse(qosAccelInfo.equals(new Object()));
                        assertFalse(qosAccelInfo.equals(new Message_Link.WiFiAccelInfo(
                            qosAccelInfo.support, qosAccelInfo.open,
                            qosAccelInfo.duration, null
                        )));
                    }
                }
            }
        }
    }

    static class QosAccelInfoFactory implements Iterable<Message_Link.QosAccelInfo> {
        public static final String[] ISP_LIST = new String[]{null, "", "中国电信"};
        public static final boolean[] SUPPORT_LIST = new boolean[]{false, true};
        public static final boolean[] OPEN_LIST = new boolean[]{false, true};
        public static final Integer[] DURATION_LIST = new Integer[]{null, 0, -3, 123};

        private final List<Message_Link.QosAccelInfo> list = new ArrayList<Message_Link.QosAccelInfo>(16);

        QosAccelInfoFactory() {
            for (boolean support : SUPPORT_LIST) {
                for (boolean open : OPEN_LIST) {
                    for (String isp : ISP_LIST) {
                        for (Integer duration : DURATION_LIST) {
                            Message_Link.QosAccelInfo qosAccelInfo = new Message_Link.QosAccelInfo(support, open, duration, isp);
                            list.add(qosAccelInfo);
                        }
                    }
                }
            }
        }

        @Override
        public Iterator<Message_Link.QosAccelInfo> iterator() {
            return this.list.iterator();
        }
    }

    @Test
    public void delayQualityV2() throws IOException {
        float delayAvg = 1.2f, delaySD = 3.4f, lostRatio = 5.6f, exPktRatio1 = 7.8f, exPktRatio2 = 8.9f, delayAvgRaw = 12.34f;
        Message_Link.DelayQualityV2 target = new Message_Link.DelayQualityV2(delayAvg, delaySD, lostRatio, exPktRatio1, exPktRatio2, delayAvgRaw);
        assertEquals(delayAvg, target.delayAvg, Float.MIN_NORMAL);
        assertEquals(delaySD, target.delaySD, Float.MIN_NORMAL);
        assertEquals(lostRatio, target.lossRatio, Float.MIN_NORMAL);
        assertEquals(exPktRatio1, target.exPktRatio1, Float.MIN_NORMAL);
        assertEquals(exPktRatio2, target.exPktRatio2, Float.MIN_NORMAL);
        //
        String json = target.serializeToJson();
        assertEquals(json, target.toString());
        json = "{\"hello\":\"world\"," + json.substring(1);
        Message_Link.DelayQualityV2 obj2 = Message_Link.DelayQualityV2.parseFromJson(new JsonReader(new StringReader(json)));
        assertEquals(target, obj2);
        //
        assertEquals(target, target);
        assertFalse(target.equals(null));
        assertFalse(target.equals(this));
        //
        StringWriter stringWriter = new StringWriter(256);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        target.serialize(jsonWriter);
        Misc.close(jsonWriter);
        String json2 = stringWriter.toString();
        Message_Link.DelayQualityV2 obj3 = Message_Link.DelayQualityV2.parseFromJson(new JsonReader(new StringReader(json2)));
        assertEquals(target, obj3);
    }

    @Test
    public void accelInfo() throws IOException {
        Message_Link.WiFiAccelInfo wiFiAccelInfo = new Message_Link.WiFiAccelInfo(
            true, false, null, -1
        );
        QosAccelInfoFactory factory = new QosAccelInfoFactory();
        for (Message_Link.QosAccelInfo qosAccelInfo : factory) {
            Message_Link.AccelInfo accelInfo = new Message_Link.AccelInfo(qosAccelInfo, wiFiAccelInfo, null);
            assertNotNull(accelInfo.toString());
            //
            StringWriter stringWriter = new StringWriter(1024);
            JsonWriter writer = new JsonWriter(stringWriter);
            accelInfo.serialize(writer);
            Misc.close(writer);
            assertNotNull(stringWriter.toString()); //TODO
        }
    }
}