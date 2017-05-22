package com.subao.common.data;

import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.mock.MockPersistent;
import com.subao.common.utils.JsonUtils;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ParallelConfigDownloaderTest extends RoboBase {

    private static final String TEST_JSON = "{\n" +
        "        \"switch\": \"1\",\n" +
        "        \"model\": \"M1,M2\",\n" +
        "        \"cpu\": \"c1,C2\"\n" +
        "}";

    private static String createDataJson(String model, String cpu, boolean enabled) throws IOException {
        StringWriter stringWriter = new StringWriter(1024);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            jsonWriter.beginObject();
//            JsonUtils.writeString(jsonWriter, "cache_tag", tag);
            JsonUtils.writeString(jsonWriter, "model", model);
            JsonUtils.writeString(jsonWriter, "cpu", cpu);
            JsonUtils.writeString(jsonWriter, "switch", enabled ? "1" : "0");
            JsonUtils.writeString(jsonWriter, "other", "test");
            jsonWriter.endObject();
        } finally {
            Misc.close(jsonWriter);
        }
        return stringWriter.toString();
    }

    @Test
    public void testData() throws IOException {

        List<String> modelList = new ArrayList<String>(4);
        modelList.add("module1");
        modelList.add("module2");
        modelList.add("Module3");

        List<String> cpuList = new ArrayList<String>(4);
        cpuList.add("cpu1");
        cpuList.add("cpu2");
        cpuList.add("Cpu3");

        ParallelConfigDownloader.Config config = ParallelConfigDownloader.Config.create(true, modelList, cpuList);
        assertNotNull(config);
        assertTrue(config.isEnabled());
        assertNotNull(config.toString());
        assertTrue(config.isCpuMatch("Cpu1"));
        assertTrue(config.isCpuMatch("cpu2"));
        assertFalse(config.isCpuMatch("Cpu3"));
        assertTrue(config.isModelMatch("module1"));
        assertTrue(config.isModelMatch("MODULE2"));
        assertFalse(config.isModelMatch("Module3"));
    }

    @Test
    public void testDataCreator() throws IOException {
        String json = createDataJson("H60-L11,MODULE2,Module3", "Hisicon,Cpu1,cpu2,CPU3", true);
        ParallelConfigDownloader.Config config = ParallelConfigDownloader.Config.parse(new PortalDataEx("tag", 1234L, "version", json.getBytes()));
        assertNotNull(config);
        assertTrue(config.isEnabled());
        assertTrue(config.isCpuMatch("Hisicon"));
        assertTrue(config.isCpuMatch("cpu1"));
        assertTrue(config.isCpuMatch("CPU2"));
        assertTrue(config.isCpuMatch("cPu3"));
        assertFalse(config.isCpuMatch("cpu4"));
        //
        assertTrue(config.isModelMatch("H60-L11"));
        assertTrue(config.isModelMatch("module2"));
        assertTrue(config.isModelMatch("module3"));
        assertFalse(config.isModelMatch("module4"));

    }

    @Test
    public void canParallelAccel() throws IOException {
        MockPersistent mockPersistent = new MockPersistent();
        mockPersistent.setData(TEST_JSON.getBytes());
        ParallelConfigDownloader.Config config = ParallelConfigDownloader.Config.parse(new PortalDataEx("1234", 1234L, "vvv", TEST_JSON.getBytes()));
//        MockNetTypeDetector mockNetTypeDetector = new MockNetTypeDetector();
//        ParallelConfigDownloader config = new ParallelConfigDownloader(mockNetTypeDetector, mockPersistent, defaultData, null);
//        assertFalse(config.canParallelAccel(20, "m1", "c1"));
//        assertTrue(config.canParallelAccel(21, "m1", "cc"));
//        assertTrue(config.canParallelAccel(21, "mm", "c2"));
//        assertTrue(config.canParallelAccel(21, "m1", "c2"));
//        //
//        int sdk_int = Build.VERSION.SDK_INT;
//        boolean result = config.canParallelAccel(0, "m1", "c1");
//        assertEquals(sdk_int >= 21, result);
//        //
//        String model = Build.MODEL;
//        result = config.canParallelAccel(21, model, "cccc");
//        assertEquals(model.equalsIgnoreCase("m1") || model.equalsIgnoreCase("m2"),
//            result);
//        //
//        config.canParallelAccel(21, "mmmm", null);
//        //
//        assertNotNull(config.toString());
    }


}