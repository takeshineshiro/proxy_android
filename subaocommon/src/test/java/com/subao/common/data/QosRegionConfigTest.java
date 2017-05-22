package com.subao.common.data;

import android.util.JsonReader;

import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.ProxyEngineCommunicator;
import com.subao.common.RoboBase;
import com.subao.common.io.Persistent;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.mock.MockPersistent;
import com.subao.common.qos.QosParam;
import com.subao.common.utils.JsonUtils;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * QosRegionConfigTest
 * <p>Created by YinHaiBo on 2016/10/26.</p>
 */
public class QosRegionConfigTest extends RoboBase {

    private static final String JSON = "{\n" +
        "    \"cfg_33.10\": \"2000,600,2001,1\",\n" +
        "    \"white_list\": \"32.10,32.12,31.10\",\n" +
        "    \"cfg_61.10\": \"2000,600,2001,1\",\n" +
        "    \"white_list\": \"32.10,32.12,31.10\",\n" +
        "    \"cfg_31.10\": \"-2000,600,0,0\",\n" +
        "    \"cfg_51.10\": \"2000,600,2001,1\",\n" +
        "    \"cfg_43.10\": \"2000,600,2001,1\",\n" +
        "    \"cfg_13.10\": \"2000,600,2001,1\",\n" +
        "    \"cfg_32.10\": \"-2000,600,0,1\",\n" +
        "    \"cfg_44.12\": \"-2000,1200,0,2\",\n" +
        "    \"cfg_34.10\": \"2000,600,2001,1\",\n" +
        "    \"cfg_35.10\": \"2000,600,2001,1\",\n" +
        "    \"cfg_32.12\": \"-2000,600,0,0\",\n" +
        "    \"cfg_42.10\": \"2000,600,2001,1\",\n" +
        "    \"cfg_44.10\": \"2000,600,2001,1\"\n" +
        "}";

    @Before
    public void setUp() {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
    }

    @After
    public void tearDown() {
        Logger.setLoggableChecker(null);
    }

    @Test
    public void testConfigEquals() {
        QosRegionConfig.Config config = new QosRegionConfig.Config(null);
        assertTrue(config.isConfigEmpty());
        assertTrue(config.equals(config));
        assertFalse(config.equals(null));
        assertFalse(config.equals(this));
        assertTrue(config.equals(new QosRegionConfig.Config(null)));
        //
        Map<RegionAndISP, QosParam> map = new HashMap<RegionAndISP, QosParam>(1);
        map.put(new RegionAndISP(1, 1), QosParam.DEFAULT);
        assertFalse(config.equals(new QosRegionConfig.Config(map)));
    }

    @Test
    public void testIsAllow1() {
        Map<RegionAndISP, QosParam> map = new HashMap<RegionAndISP, QosParam>(4);
        map.put(new RegionAndISP(1, 2), QosParam.DEFAULT);
        map.put(new RegionAndISP(1, 3), QosParam.DEFAULT);
        map.put(new RegionAndISP(2, 2), QosParam.DEFAULT);
        map.put(new RegionAndISP(2, 3), QosParam.DEFAULT);
        QosRegionConfig.Config config = new QosRegionConfig.Config(map);
        for (RegionAndISP r : map.keySet()) {
            assertNotNull(config.getQosParam(r.region, r.isp));
        }
        assertNull(config.getQosParam(1, 4));
        assertNull(config.getQosParam(3, 3));
    }

    @Test
    public void testIsAllow2() {
        Map<RegionAndISP, QosParam> map = new HashMap<RegionAndISP, QosParam>(4);
        map.put(new RegionAndISP(1, 2), QosParam.DEFAULT);
        map.put(new RegionAndISP(-1, 3), QosParam.DEFAULT);
        map.put(new RegionAndISP(4, -1), QosParam.DEFAULT);
        QosRegionConfig.Config config = new QosRegionConfig.Config(map);
        assertNotNull(config.getQosParam(10, 3));
        assertNotNull(config.getQosParam(4, 20));
    }

    @Test
    public void testIsAllow3() {
        Map<RegionAndISP, QosParam> map = new HashMap<RegionAndISP, QosParam>();
        QosRegionConfig.Config config = new QosRegionConfig.Config(map);
        assertNull(config.getQosParam(1, 1));
        config = new QosRegionConfig.Config(null);
        assertNull(config.getQosParam(1, 2));
    }

    @Test
    public void testGetUrlPart() {
        QosRegionConfig cfg = new QosRegionConfig(new PortalDataDownloader.Arguments("C", "V", null, new MockNetTypeDetector()) {
            @Override
            public Persistent createPersistent(String filename) {
                return new MockPersistent();
            }
        });
        assertNotNull(cfg.getUrlPart());
        assertNotNull(cfg.getId());
    }

    @Test
    public void testQosParamEquals() {
        QosParam q1 = new QosParam(123, 456, QosParam.Provider.DEFAULT, 1, 2);
        assertEquals(q1, q1);
        assertFalse(q1.equals(null));
        assertFalse(q1.equals(new Object()));
        assertEquals(q1, new QosParam(123, 456, QosParam.Provider.DEFAULT, 1, 2));
        assertFalse(q1.equals(new QosParam(123, 455, QosParam.Provider.DEFAULT, 1, 2)));
        assertFalse(q1.equals(new QosParam(111, 456, QosParam.Provider.DEFAULT, 1, 2)));
        assertFalse(q1.equals(new QosParam(123, 455, QosParam.Provider.DEFAULT, 1, 2)));
        assertNotEquals(q1, new QosParam(123, 456, QosParam.Provider.IVTIME, 1, 2));
        assertNotEquals(q1, new QosParam(123, 456, QosParam.Provider.DEFAULT, 2, 2));
        assertNotEquals(q1, new QosParam(123, 456, QosParam.Provider.DEFAULT, 1, 3));
    }

    @Test
    public void testDataToString() {
        Map<RegionAndISP, QosParam> map = new HashMap<RegionAndISP, QosParam>();
        QosRegionConfig.Config config = new QosRegionConfig.Config(map);
        assertNotNull(config.toString());
    }

    @Test
    public void testDeserialize() throws IOException {
        String json = "{\n" +
            "    \"white_list\": \"32.10,32.12,31.10\",\n" +
            "    \"cfg_32.10\": \"-100,600\",\n" +
            "    \"cfg_31.10\": \"0, 500\",\n" +
            "    \"cfg_31.11\": \"\",\n" +
            "    \"cfg_31.12\": \"1\",\n" +
            "    \"cfg_33.10\": \"1,\",\n" +
            "    \"cfg_33.11\": \"1, 2,\",\n" +
            "    \"cfg_33.12\": \",2 \",\n" +
            "    \"cfg_34.10\": \",2,\",\n" +
            "    \"c_34.11\": \"1,2\",\n" +
            "    \"cfg_\": \"3,4\",\n" +
            "    \"cfg_35.10\": \"1,,10\",\n" +
            "    \"cfg_35.11\": \"1, 2, 10,\",\n" +
            "    \"cfg_35.12\": \",, 10 \",\n" +
            "    \"cfg_a.b\": \"-100,2,3,1\",\n" +
            "    \"cfg_51.*\": \"1,2,3,1\",\n" +
            "    \"cfg_11.11\": \"1,2,3,2\",\n" +
            "    \"cfg_11.10\": null\n" +
            "}";
        int[][] expectedValues = new int[][]{
            {32, 10, -100, 600, 0, QosParam.Provider.DEFAULT.id},
            {31, 10, 0, 500, 0, QosParam.Provider.DEFAULT.id},
            {31, 11, QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN, QosParam.DEFAULT_ACCEL_TIME, 0, QosParam.Provider.DEFAULT.id},
            {31, 12, 1, QosParam.DEFAULT_ACCEL_TIME, 0, QosParam.Provider.DEFAULT.id},
            {33, 10, 1, QosParam.DEFAULT_ACCEL_TIME, 0, QosParam.Provider.DEFAULT.id},
            {33, 11, 1, 2, 0, QosParam.Provider.DEFAULT.id},
            {33, 12, QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN, 2, 0, QosParam.Provider.DEFAULT.id},
            {34, 10, QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN, 2, 0, QosParam.Provider.DEFAULT.id},
            {35, 10, 1, QosParam.DEFAULT_ACCEL_TIME, 10, QosParam.Provider.DEFAULT.id},
            {35, 11, 1, 2, 10, QosParam.Provider.DEFAULT.id},
            {35, 12, QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN, QosParam.DEFAULT_ACCEL_TIME, 10, QosParam.Provider.DEFAULT.id},
            {51, -1, 1, 2, 3, QosParam.Provider.IVTIME.id},
            {11, 11, 1, 2, 3, QosParam.Provider.ZTE.id},
            {11, 10, QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN, QosParam.DEFAULT_ACCEL_TIME, 0, QosParam.Provider.DEFAULT.id},
        };
        QosRegionConfig.Config config = new QosRegionConfig.Config(new HashMap<RegionAndISP, QosParam>(16));
        JsonReader jsonReader = new JsonReader(new InputStreamReader(new StringInputStream(json)));
        try {
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String key = jsonReader.nextName();
                String value = JsonUtils.readNextString(jsonReader);
                config.parseKeyValues(key, value);
            }
            jsonReader.endObject();
        } finally {
            Misc.close(jsonReader);
        }
        //
        for (int[] array : expectedValues) {
            QosParam qosParam = config.getQosParam(array[0], array[1]);
            assertEquals(array[2], qosParam.deltaThresholdForQosOpen);
            assertEquals(array[3], qosParam.accelTime);
            assertEquals(QosParam.Provider.fromId(array[5]), qosParam.provider);
        }
    }

    @Test
    public void testStart() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new MyDispatcher());
        mockWebServer.start();
        try {
            Arguments arguments = new Arguments(mockWebServer);
            QosRegionConfig.start(arguments);
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {}
            assertTrue(QosRegionConfig.getSwitch());
            testParseResult();
        } finally {
            mockWebServer.shutdown();
        }
    }

    private void testParseResult() {
        QosParam qosParam;
        qosParam = QosRegionConfig.getQosParam(33, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(61, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(31, 10);
        assertEquals(-2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(0, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(51, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(43, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(13, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(32, 10);
        assertEquals(-2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(44, 12);
        assertEquals(-2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(1200, qosParam.accelTime);
        assertEquals(2, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(34, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(35, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(32, 12);
        assertEquals(-2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(0, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(42, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = QosRegionConfig.getQosParam(44, 10);
        assertEquals(2000, qosParam.deltaThresholdForQosOpen);
        assertEquals(600, qosParam.accelTime);
        assertEquals(1, qosParam.provider.id);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
    }

    private static class Arguments extends PortalDataDownloader.Arguments {

        public Arguments(MockWebServer mockWebServer) {
            super("android", "2.0.0",
                new ServiceLocation(null, mockWebServer.getHostName(), mockWebServer.getPort()),
                new MockNetTypeDetector()
            );
        }

        @Override
        public Persistent createPersistent(String filename) {
            MockPersistent persistent = new MockPersistent();
            PortalDataEx portalData = new PortalDataEx("cache", 1234L, "2.0.0", JSON.getBytes());
            try {
                portalData.serialize(persistent.openOutput());
            } catch (IOException e) {
            }
            return persistent;
        }
    }

    private static class ProxyEngineCommunicatorImpl implements ProxyEngineCommunicator {

        String lastKey;
        int lastIntValue;
        String lastStringValue;

        Map<String, String> definedConstList = new HashMap<String, String>(16);

        @Override
        public void setInt(int cid, String key, int value) {
            lastKey = key;
            lastIntValue = value;
        }

        @Override
        public void setString(int cid, String key, String value) {
            lastKey = key;
            lastStringValue = value;
        }

        @Override
        public void defineConst(String key, String value) {
            definedConstList.put(key, value);
        }
    }

    private static class MyDispatcher extends Dispatcher {

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            MockResponse response = new MockResponse();
            response.setBody(JSON).setResponseCode(200);
            return response;
        }
    }
}