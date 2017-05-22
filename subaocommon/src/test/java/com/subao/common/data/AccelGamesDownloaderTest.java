package com.subao.common.data;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.io.Persistent;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.mock.MockPersistent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * AccelGamesDownloaderTest
 * <p>Created by YinHaiBo on 2017/3/27.</p>
 */
public class AccelGamesDownloaderTest extends RoboBase {

    private static final String VERSION = "2.1.1";

    private static final String JSON = "{\"hello\":null,\n" +
        "    \"gameList\": [\n" +
        "        {\n" +
        "            \"appLabel\": \"王者荣耀\",\n" +
        "            \"accelMode\": 1,\n" +
        "            \"bitFlag\": 30,\n" +
        "            \"salt\": 12345,\n" +
        "            \"whitePorts\": [\n" +
        "                {\n" +
        "                    \"start\": 41000,\n" +
        "                    \"salt\": null,\n" +
        "                    \"end\": 41999\n" +
        "                },\n" +
        "                {\n" +
        "                    \"start\": 43000,\n" +
        "                    \"end\": 43001\n" +
        "                }\n" +
        "            ],\n" +
        "            \"blackPorts\": [\n" +
        "                {\n" +
        "                    \"start\": 8080,\n" +
        "                    \"end\": 8081\n" +
        "                }\n" +
        "            ],\n" +
        "            \"blackIps\": [\n" +
        "                \"140.206.160.117\",\n" +
        "                \"101.226.76.200\"\n" +
        "            ],\n" +
        "            \"whiteIps\": [\n" +
        "                \"140.21.160.117/32\",\n" +
        "                \"101.22.76.1/24\"\n" +
        "            ]\n" +
        "        },\n" +
        "        {\n" +
        "            \"appLabel\": \"ABC\",\n" +
        "            \"accelMode\": 1,\n" +
        "            \"bitFlag\": 50\n" +
        "        }\n" +
        "    ]\n" +
        "}";

    private MockWebServer mockWebServer;

    private static void testList(List<AccelGame> list) {
        assertEquals(2, list.size());
        AccelGame accelGame = list.get(0);
        assertEquals("王者荣耀", accelGame.appLabel);
        assertEquals(1, accelGame.accelMode);
        assertEquals(30, accelGame.flags);
        assertEquals(false, accelGame.isLabelThreeAsciiChar);
        AccelGameTest.testIPList(new String[]{
            "140.206.160.117", "101.226.76.200"
        }, accelGame.getBlackIps());
        AccelGameTest.testIPList(new String[]{
            "140.21.160.117/32", "101.22.76.1/24"
        }, accelGame.getWhiteIps());
        AccelGameTest.testPortRanges(new AccelGame.PortRange[]{
            new AccelGame.PortRange(41000, 41999),
            new AccelGame.PortRange(43000, 43001)
        }, accelGame.getWhitePorts());
        AccelGameTest.testPortRanges(new AccelGame.PortRange[]{
            new AccelGame.PortRange(8080, 8081),
        }, accelGame.getBlackPorts());
        //
        accelGame = list.get(1);
        assertEquals("ABC", accelGame.appLabel);
        assertEquals(1, accelGame.accelMode);
        assertEquals(50, accelGame.flags);
        assertEquals(true, accelGame.isLabelThreeAsciiChar);
        assertNull(accelGame.getWhiteIps());
        assertNull(accelGame.getBlackIps());
        assertNull(accelGame.getWhitePorts());
        assertNull(accelGame.getBlackPorts());
    }

    private Arguments buildArguments(byte[] data) throws IOException {
        Arguments arguments = new Arguments(mockWebServer);
        PortalDataEx portalDataEx = new PortalDataEx("tag", System.currentTimeMillis() + 10000L, VERSION, data);
        ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
        portalDataEx.serialize(output);
        arguments.persistentData = output.toByteArray();
        return arguments;
    }

    @Before
    public void setUp() throws Exception {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse());
        mockWebServer.start();
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
        Logger.setLoggableChecker(null);
    }

    @Test
    public void startWhenNoLocalCache() throws Exception {
        Arguments arguments = new Arguments(mockWebServer);
        List<AccelGame> list = AccelGamesDownloader.start(arguments, 4, null, null);
        assertNull(list);
        list = AccelGamesDownloader.start(arguments, 4, null, JSON.getBytes());
        testList(list);
        // length < 2
        list = AccelGamesDownloader.start(arguments, 4, null, "{".getBytes());
        assertNull(list);
        // IOException
        list = AccelGamesDownloader.start(arguments, 4, null, "{\"name\":{[}".getBytes());
        assertNull(list);
        // RuntimeException
        list = AccelGamesDownloader.start(arguments, 4, null, "{\"gameList\":null}".getBytes());
        assertNull(list);
    }

    @Test
    public void startWhenLocalCache() throws IOException {
        Arguments arguments = buildArguments(JSON.getBytes());
        List<AccelGame> list = AccelGamesDownloader.start(arguments, 4, null, null);
        //
        testList(list);
    }

    @Test
    public void startWhenInvalidLocalData() throws IOException {
        Arguments arguments = buildArguments(null);
        List<AccelGame> list = AccelGamesDownloader.start(arguments, 4, null, null);
        assertNull(list);
    }

    @Test
    public void startWhenInvalidLocalDataRuntimeException() throws IOException {
        Arguments arguments = buildArguments("[{".getBytes());
        List<AccelGame> list = AccelGamesDownloader.start(arguments, 4, null, null);
        assertNull(list);
    }

    @Test
    public void testMisc() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        testPrivateConstructor(AccelGamesDownloader.JsonParser.class);
    }

    @Test
    public void testOnPostExecute() throws IOException {
        MockListener mockListener = new MockListener();
        AccelGamesDownloader downloader = new AccelGamesDownloader(buildArguments(JSON.getBytes()), 4, mockListener);
        downloader.onPostExecute(null);
        assertNull(mockListener.list);
        downloader.onPostExecute(new PortalDataEx("tag", 1234L, VERSION, JSON.getBytes(), true));
        testList(mockListener.list);
    }

    private static class Arguments extends PortalDataDownloader.Arguments {

        byte[] persistentData;

        public Arguments(MockWebServer mockWebServer) {
            super("android", VERSION,
                new ServiceLocation(null, mockWebServer.getHostName(), mockWebServer.getPort()),
                new MockNetTypeDetector());
        }

        @Override
        public Persistent createPersistent(String filename) {
            MockPersistent mockPersistent = new MockPersistent();
            mockPersistent.setData(persistentData);
            return mockPersistent;
        }
    }

    private static class MockListener implements AccelGamesDownloader.Listener {

        List<AccelGame> list;

        @Override
        public void onAccelGameListDownload(List<AccelGame> list) {
            this.list = list;
        }
    }

}