package com.subao.common.data;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.io.Persistent;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.mock.MockPersistent;
import com.subao.common.net.NetTypeDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AccelNodesDownloaderTest
 * <p>Created by YinHaiBo on 2017/2/25.</p>
 */
public class AccelNodesDownloaderTest extends RoboBase {

    private static final String JSON = "[{\"test\":null},{\"hello\":123,\"id\":166,\"ip\":\"43.243.130.141\",\"isp\":\"bgp\"},{\"id\":166,\"ip\":\"117.28.254.140\",\"isp\":\"hw\"},{\"id\":75,\"ip\":\"203.69.139.182\",\"isp\":\"hw\"},{\"id\":163,\"ip\":\"47.89.182.15\",\"isp\":\"hw\"},{\"id\":3,\"ip\":\"221.180.237.196\",\"isp\":\"cmcc\"},{\"id\":3,\"ip\":\"124.95.147.25\",\"isp\":\"cnc\"},{\"id\":3,\"ip\":\"123.244.9.6\",\"isp\":\"ctc\"},{\"id\":146,\"ip\":\"117.184.37.131\",\"isp\":\"cmcc\"},{\"id\":146,\"ip\":\"112.65.217.131\",\"isp\":\"cnc\"},{\"id\":146,\"ip\":\"103.28.213.211\",\"isp\":\"ctc\"},{\"id\":157,\"ip\":\"47.88.106.28\",\"isp\":\"hw\"},{\"id\":156,\"ip\":\"47.88.190.84\",\"isp\":\"hw\"},{\"id\":168,\"ip\":\"120.77.169.236\",\"isp\":\"bgp\"},{\"id\":167,\"ip\":\"139.196.123.21\",\"isp\":\"bgp\"},{\"id\":8,\"ip\":\"111.59.152.135\",\"isp\":\"cmcc\"},{\"id\":8,\"ip\":\"221.7.232.82\",\"isp\":\"cnc\"},{\"id\":8,\"ip\":\"113.14.240.80\",\"isp\":\"ctc\"},{\"id\":145,\"ip\":\"223.99.60.54\",\"isp\":\"cmcc\"},{\"id\":145,\"ip\":\"58.58.177.72\",\"isp\":\"ctc\"},{\"id\":145,\"ip\":\"61.133.67.237\",\"isp\":\"cnc\"},{\"id\":143,\"ip\":\"112.44.203.118\",\"isp\":\"cmcc\"},{\"id\":143,\"ip\":\"119.4.113.24\",\"isp\":\"cnc\"},{\"id\":143,\"ip\":\"118.122.88.83\",\"isp\":\"ctc\"},{\"id\":18,\"ip\":\"223.68.197.18\",\"isp\":\"cmcc\"},{\"id\":18,\"ip\":\"58.240.173.75\",\"isp\":\"cnc\"},{\"id\":18,\"ip\":\"180.97.75.86\",\"isp\":\"ctc\"},{\"id\":1,\"ip\":\"183.224.78.231\",\"isp\":\"cmcc\"},{\"id\":1,\"ip\":\"221.213.100.179\",\"isp\":\"cnc\"},{\"id\":1,\"ip\":\"116.249.126.149\",\"isp\":\"ctc\"},{\"id\":160,\"ip\":\"123.138.36.79\",\"isp\":\"cnc\"},{\"id\":160,\"ip\":\"117.34.22.149\",\"isp\":\"ctc\"},{\"id\":104,\"ip\":\"117.184.37.138\",\"isp\":\"cmcc\"},{\"id\":104,\"ip\":\"112.65.217.138\",\"isp\":\"cnc\"},{\"id\":104,\"ip\":\"103.28.213.218\",\"isp\":\"ctc\"},{\"id\":93,\"ip\":\"112.65.217.135\",\"isp\":\"cnc\"},{\"id\":93,\"ip\":\"103.28.213.215\",\"isp\":\"ctc\"},{\"id\":115,\"ip\":\"111.47.204.96\",\"isp\":\"cmcc\"},{\"id\":115,\"ip\":\"119.97.172.185\",\"isp\":\"ctc\"},{\"id\":102,\"ip\":\"120.192.101.30\",\"isp\":\"cmcc\"},{\"id\":102,\"ip\":\"124.133.240.8\",\"isp\":\"cnc\"},{\"id\":102,\"ip\":\"58.56.9.149\",\"isp\":\"ctc\"},{\"id\":158,\"ip\":\"47.90.89.219\",\"isp\":\"hw\"},{\"id\":112,\"ip\":\"103.36.31.34\",\"isp\":\"bgp\"},{\"id\":131,\"ip\":\"116.255.253.197\",\"isp\":\"bgp\"},{\"id\":78,\"ip\":\"103.21.119.29\",\"isp\":\"bgp\"},{\"id\":139,\"ip\":\"222.73.103.103\",\"isp\":\"ctc\"},{\"id\":107,\"ip\":\"112.65.217.141\",\"isp\":\"cnc\"},{\"id\":107,\"ip\":\"103.28.213.221\",\"isp\":\"ctc\"},{\"id\":106,\"ip\":\"117.184.37.140\",\"isp\":\"cmcc\"},{\"id\":106,\"ip\":\"112.65.217.140\",\"isp\":\"cnc\"},{\"id\":106,\"ip\":\"103.28.213.220\",\"isp\":\"ctc\"},{\"id\":105,\"ip\":\"117.184.37.139\",\"isp\":\"cmcc\"},{\"id\":105,\"ip\":\"112.65.217.139\",\"isp\":\"cnc\"},{\"id\":105,\"ip\":\"103.28.213.219\",\"isp\":\"ctc\"},{\"id\":91,\"ip\":\"117.184.37.136\",\"isp\":\"cmcc\"},{\"id\":91,\"ip\":\"112.65.217.136\",\"isp\":\"cnc\"},{\"id\":91,\"ip\":\"103.28.213.216\",\"isp\":\"ctc\"},{\"id\":89,\"ip\":\"117.184.37.134\",\"isp\":\"cmcc\"},{\"id\":89,\"ip\":\"112.65.217.134\",\"isp\":\"cnc\"},{\"id\":89,\"ip\":\"103.28.213.214\",\"isp\":\"ctc\"},{\"id\":88,\"ip\":\"117.184.37.133\",\"isp\":\"cmcc\"},{\"id\":88,\"ip\":\"112.65.217.133\",\"isp\":\"cnc\"},{\"id\":88,\"ip\":\"103.28.213.213\",\"isp\":\"ctc\"},{\"id\":67,\"ip\":\"117.184.37.132\",\"isp\":\"cmcc\"},{\"id\":67,\"ip\":\"112.65.217.132\",\"isp\":\"cnc\"},{\"id\":67,\"ip\":\"103.28.213.212\",\"isp\":\"ctc\"},{\"id\":96,\"ip\":\"112.65.217.137\",\"isp\":\"cnc\"},{\"id\":96,\"ip\":\"103.28.213.217\",\"isp\":\"ctc\"},{\"id\":129,\"ip\":\"112.26.31.79\",\"isp\":\"cmcc\"},{\"id\":129,\"ip\":\"42.157.3.62\",\"isp\":\"cnc\"},{\"id\":129,\"ip\":\"60.174.237.91\",\"isp\":\"ctc\"},{\"id\":128,\"ip\":\"112.26.31.78\",\"isp\":\"cmcc\"},{\"id\":128,\"ip\":\"42.157.3.60\",\"isp\":\"cnc\"},{\"id\":128,\"ip\":\"60.174.237.90\",\"isp\":\"ctc\"},{\"id\":68,\"ip\":\"112.26.31.75\",\"isp\":\"cmcc\"},{\"id\":68,\"ip\":\"42.157.3.92\",\"isp\":\"cnc\"},{\"id\":68,\"ip\":\"60.174.237.56\",\"isp\":\"ctc\"},{\"id\":124,\"ip\":\"183.230.134.21\",\"isp\":\"cmcc\"},{\"id\":124,\"ip\":\"113.207.26.136\",\"isp\":\"cnc\"},{\"id\":124,\"ip\":\"222.180.162.77\",\"isp\":\"ctc\"},{\"id\":54,\"ip\":\"183.230.134.19\",\"isp\":\"cmcc\"},{\"id\":54,\"ip\":\"113.207.26.184\",\"isp\":\"cnc\"},{\"id\":54,\"ip\":\"222.180.162.13\",\"isp\":\"ctc\"},{\"id\":5,\"ip\":\"111.40.8.39\",\"isp\":\"cmcc\"},{\"id\":5,\"ip\":\"222.171.242.209\",\"isp\":\"ctc\"},{\"id\":5,\"ip\":\"221.208.195.67\",\"isp\":\"cnc\"},{\"id\":62,\"ip\":\"183.232.71.166\",\"isp\":\"cmcc\"},{\"id\":62,\"ip\":\"122.13.78.59\",\"isp\":\"cnc\"},{\"id\":62,\"ip\":\"183.61.111.67\",\"isp\":\"ctc\"},{\"id\":50,\"ip\":\"183.232.71.167\",\"isp\":\"cmcc\"},{\"id\":50,\"ip\":\"122.13.78.56\",\"isp\":\"cnc\"},{\"id\":50,\"ip\":\"183.61.111.68\",\"isp\":\"ctc\"},{\"id\":130,\"ip\":\"211.138.144.153\",\"isp\":\"cmcc\"},{\"id\":130,\"ip\":\"36.250.13.24\",\"isp\":\"cnc\"},{\"id\":130,\"ip\":\"117.27.158.67\",\"isp\":\"ctc\"},{\"id\":111,\"ip\":\"60.12.202.171\",\"isp\":\"cnc\"},{\"id\":111,\"ip\":\"115.238.147.235\",\"isp\":\"ctc\"},{\"id\":37,\"ip\":\"183.232.71.165\",\"isp\":\"cmcc\"},{\"id\":37,\"ip\":\"122.13.68.173\",\"isp\":\"cnc\"},{\"id\":37,\"ip\":\"183.61.111.66\",\"isp\":\"ctc\"},{\"id\":142,\"ip\":\"116.211.86.172\",\"isp\":\"ctc\"},{\"id\":141,\"ip\":\"183.60.183.180\",\"isp\":\"ctc\"},{\"id\":134,\"ip\":\"171.34.34.34\",\"isp\":\"cnc\"},{\"id\":134,\"ip\":\"117.41.237.34\",\"isp\":\"ctc\"},{\"id\":25,\"ip\":\"183.232.67.77\",\"isp\":\"cmcc\"},{\"id\":25,\"ip\":\"122.13.18.58\",\"isp\":\"cnc\"},{\"id\":25,\"ip\":\"125.88.253.189\",\"isp\":\"ctc\"},{\"id\":122,\"ip\":\"183.232.67.74\",\"isp\":\"cmcc\"},{\"id\":122,\"ip\":\"122.13.18.61\",\"isp\":\"cnc\"},{\"id\":122,\"ip\":\"125.88.253.188\",\"isp\":\"ctc\"},{\"id\":114,\"ip\":\"223.68.197.19\",\"isp\":\"cmcc\"},{\"id\":114,\"ip\":\"58.240.173.71\",\"isp\":\"cnc\"},{\"id\":114,\"ip\":\"180.97.75.84\",\"isp\":\"ctc\"},{\"id\":113,\"ip\":\"223.68.197.17\",\"isp\":\"cmcc\"},{\"id\":113,\"ip\":\"58.240.173.74\",\"isp\":\"cnc\"},{\"id\":113,\"ip\":\"180.97.75.83\",\"isp\":\"ctc\"},{\"id\":109,\"ip\":\"111.47.123.68\",\"isp\":\"cmcc\"},{\"id\":109,\"ip\":\"183.95.88.59\",\"isp\":\"cnc\"},{\"id\":109,\"ip\":\"116.211.86.171\",\"isp\":\"ctc\"},{\"id\":29,\"ip\":\"124.160.150.37\",\"isp\":\"cnc\"},{\"id\":29,\"ip\":\"115.236.59.44\",\"isp\":\"ctc\"},{\"id\":66,\"ip\":\"140.207.216.163\",\"isp\":\"cnc\"},{\"id\":66,\"ip\":\"222.73.103.100\",\"isp\":\"ctc\"},{\"id\":63,\"ip\":\"140.207.216.168\",\"isp\":\"cnc\"},{\"id\":63,\"ip\":\"222.73.103.80\",\"isp\":\"ctc\"},{\"id\":38,\"ip\":\"140.207.216.169\",\"isp\":\"cnc\"},{\"id\":38,\"ip\":\"222.73.103.82\",\"isp\":\"ctc\"},{\"id\":103,\"ip\":\"13.76.208.108\",\"isp\":\"hw\"},{\"id\":61,\"ip\":\"122.13.18.62\",\"isp\":\"cnc\"},{\"id\":61,\"ip\":\"125.88.253.187\",\"isp\":\"ctc\"},{\"id\":35,\"ip\":\"58.20.35.21\",\"isp\":\"cnc\"},{\"id\":35,\"ip\":\"124.232.160.200\",\"isp\":\"ctc\"},{\"id\":30,\"ip\":\"111.47.123.67\",\"isp\":\"cmcc\"},{\"id\":30,\"ip\":\"183.95.88.57\",\"isp\":\"cnc\"},{\"id\":30,\"ip\":\"116.211.86.169\",\"isp\":\"ctc\"},{\"id\":72,\"ip\":\"122.13.18.57\",\"isp\":\"cnc\"},{\"id\":72,\"ip\":\"125.88.253.185\",\"isp\":\"ctc\"},{\"id\":65,\"ip\":\"183.232.5.116\",\"isp\":\"cmcc\"},{\"id\":65,\"ip\":\"221.5.100.114\",\"isp\":\"cnc\"},{\"id\":65,\"ip\":\"183.2.217.101\",\"isp\":\"ctc\"},{\"id\":49,\"ip\":\"124.160.150.39\",\"isp\":\"cnc\"},{\"id\":49,\"ip\":\"115.236.77.203\",\"isp\":\"ctc\"},{\"id\":33,\"ip\":\"111.47.123.66\",\"isp\":\"cmcc\"},{\"id\":33,\"ip\":\"183.95.88.58\",\"isp\":\"cnc\"},{\"id\":33,\"ip\":\"116.211.86.170\",\"isp\":\"ctc\"},{\"id\":26,\"ip\":\"183.232.5.115\",\"isp\":\"cmcc\"},{\"id\":26,\"ip\":\"221.5.100.115\",\"isp\":\"cnc\"},{\"id\":26,\"ip\":\"183.2.217.102\",\"isp\":\"ctc\"},{\"id\":60,\"ip\":\"42.62.106.156\",\"isp\":\"bgp\"},{\"id\":6,\"ip\":\"42.62.106.173\",\"isp\":\"bgp\"},{\"id\":47,\"ip\":\"221.182.171.23\",\"isp\":\"cmcc\"},{\"id\":47,\"ip\":\"153.0.136.226\",\"isp\":\"cnc\"},{\"id\":47,\"ip\":\"220.174.161.157\",\"isp\":\"ctc\"},{\"id\":48,\"ip\":\"221.208.198.119\",\"isp\":\"cnc\"},{\"id\":48,\"ip\":\"222.171.242.186\",\"isp\":\"ctc\"},{\"id\":77,\"ip\":\"42.62.106.154\",\"isp\":\"bgp\"},{\"id\":76,\"ip\":\"42.62.106.152\",\"isp\":\"bgp\"},{\"id\":70,\"ip\":\"140.207.216.167\",\"isp\":\"cnc\"},{\"id\":70,\"ip\":\"222.73.103.81\",\"isp\":\"ctc\"},{\"id\":69,\"ip\":\"42.62.106.155\",\"isp\":\"bgp\"},{\"id\":87,\"ip\":\"43.243.148.95\",\"isp\":\"cnc\"},{\"id\":87,\"ip\":\"117.135.141.217\",\"isp\":\"cmcc\"},{\"id\":87,\"ip\":\"222.73.146.217\",\"isp\":\"ctc\"},{\"id\":86,\"ip\":\"43.243.148.90\",\"isp\":\"cnc\"},{\"id\":86,\"ip\":\"117.135.141.216\",\"isp\":\"cmcc\"},{\"id\":86,\"ip\":\"222.73.146.216\",\"isp\":\"ctc\"},{\"id\":81,\"ip\":\"43.243.148.93\",\"isp\":\"cnc\"},{\"id\":81,\"ip\":\"117.135.141.214\",\"isp\":\"cmcc\"},{\"id\":81,\"ip\":\"222.73.146.214\",\"isp\":\"ctc\"},{\"id\":80,\"ip\":\"43.243.148.92\",\"isp\":\"cnc\"},{\"id\":80,\"ip\":\"117.135.141.213\",\"isp\":\"cmcc\"},{\"id\":80,\"ip\":\"222.73.146.213\",\"isp\":\"ctc\"},{\"id\":71,\"ip\":\"43.243.148.91\",\"isp\":\"cnc\"},{\"id\":71,\"ip\":\"117.135.141.210\",\"isp\":\"cmcc\"},{\"id\":71,\"ip\":\"222.73.146.210\",\"isp\":\"ctc\"},{\"id\":64,\"ip\":\"140.207.216.173\",\"isp\":\"cnc\"},{\"id\":64,\"ip\":\"222.73.103.102\",\"isp\":\"ctc\"},{\"id\":11,\"ip\":\"43.243.148.94\",\"isp\":\"cnc\"},{\"id\":11,\"ip\":\"117.135.141.215\",\"isp\":\"cmcc\"},{\"id\":11,\"ip\":\"222.73.146.215\",\"isp\":\"ctc\"},{\"id\":39,\"ip\":\"140.207.216.162\",\"isp\":\"cnc\"},{\"id\":39,\"ip\":\"222.73.103.83\",\"isp\":\"ctc\"},{\"id\":28,\"ip\":\"140.207.216.170\",\"isp\":\"cnc\"},{\"id\":28,\"ip\":\"222.73.103.68\",\"isp\":\"ctc\"},{\"id\":56,\"ip\":\"198.23.97.138\",\"isp\":\"hw\"},{\"id\":34,\"ip\":\"60.18.150.43\",\"isp\":\"cnc\"},{\"id\":34,\"ip\":\"219.149.103.14\",\"isp\":\"ctc\"},{\"id\":14,\"ip\":\"119.81.172.98\",\"isp\":\"hw\"}]";

    private MockNetTypeDetector mockNetTypeDetector;
    private MockWebServer mockWebServer;
    private Arguments arguments;

    @Before
    public void setUp() throws IOException {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        mockNetTypeDetector = new MockNetTypeDetector();
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                MockResponse mockResponse = new MockResponse();
                mockResponse.setBody(JSON).setResponseCode(200);
                return mockResponse;
            }
        });
        mockWebServer.start();
        arguments = new Arguments(
            new ServiceLocation(null, mockWebServer.getHostName(), mockWebServer.getPort()),
            mockNetTypeDetector);
    }

    @After
    public void tearDown() throws IOException {
        Logger.setLoggableChecker(null);
        mockWebServer.shutdown();
    }

    @Test
    public void startWithNoLocalData() {
        AccelNodesDownloader.NodesInfo nodesInfo = AccelNodesDownloader.start(arguments);
        assertNull(nodesInfo.dataForJNI);
        assertEquals(0, nodesInfo.count);
    }

    @Test
    public void startWithLocalData() throws IOException, JSONException {
        PortalDataEx portalData = new PortalDataEx("cache", 1234L, "2.0.0", JSON.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);
        portalData.serialize(outputStream);
        outputStream.close();
        arguments.mockPersistent.setData(outputStream.toByteArray());
        AccelNodesDownloader.NodesInfo nodesInfo = AccelNodesDownloader.start(arguments);
        assertNotNull(nodesInfo.dataForJNI);
        assertEquals(188, nodesInfo.count);
        //
        JSONObject jsonObject = new JSONObject("{\"array\":" + JSON + "}");
        JSONArray array = jsonObject.getJSONArray("array");
        StringBuilder sb = new StringBuilder(8192);
        for (int i = 0, len = array.length(); i < len; ++i) {
            JSONObject item = array.getJSONObject(i);
            if (item.has("id")) {
                sb.append(item.getInt("id"));
                sb.append(':').append(item.getString("ip"));
                String[] isp = item.getString("isp").split(",");
                for (String s : isp) {
                    sb.append(':').append(s);
                }
                sb.append(',');
            }
        }
        assertEquals(sb.toString(), nodesInfo.dataForJNI);
    }

    @Test
    public void extractDataForJNI() {
        assertNull(AccelNodesDownloader.extractDataForJNI(null));
        assertNull(AccelNodesDownloader.extractDataForJNI(new PortalDataEx("cache", 1234L, "2.0.0", new byte[7])));
        assertNull(AccelNodesDownloader.extractDataForJNI(new PortalDataEx("cache", 1234L, "2.0.0",
            "{\"id\":166,\"ip\":\"43.243.130.141\",\"isp\":\"bgp\"},{[\"".getBytes())));
        assertNull(AccelNodesDownloader.extractDataForJNI(new PortalDataEx("cache", 1234L, "2.0.0",
            "ABC{\"id\":166,\"ip\":\"43.243.130.141\",\"isp\":\"bgp\"},[\"".getBytes())));
    }

    @Test
    public void nodesInfo() {
        String dataForJNI = "This is a test";
        AccelNodesDownloader.NodesInfo nodesInfo = new AccelNodesDownloader.NodesInfo(
            3, dataForJNI
        );
        assertEquals(3, nodesInfo.count);
        assertEquals(dataForJNI, nodesInfo.dataForJNI);
        assertEquals(nodesInfo, nodesInfo);
        assertFalse(nodesInfo.equals(null));
        assertFalse(nodesInfo.equals(this));
        assertEquals(nodesInfo, new AccelNodesDownloader.NodesInfo(3, dataForJNI));
        assertFalse(nodesInfo.equals(new AccelNodesDownloader.NodesInfo(4, dataForJNI)));
        assertFalse(nodesInfo.equals(new AccelNodesDownloader.NodesInfo(3, "test")));
        assertNotNull(nodesInfo.toString());
    }

    @Test
    public void checkDownloadData() {
        AccelNodesDownloader downloader = new AccelNodesDownloader(arguments);
        assertFalse(downloader.checkDownloadData(null));
        assertFalse(downloader.checkDownloadData(new PortalDataEx("", 1234L, "", new byte[15])));
        assertTrue(downloader.checkDownloadData(new PortalDataEx("", 1234L, "", new byte[17])));
    }

    private static class Arguments extends PortalDataDownloader.Arguments {

        final MockPersistent mockPersistent = new MockPersistent();

        public Arguments(ServiceLocation serviceLocation, NetTypeDetector netTypeDetector) {
            super("android", "2.0.0", serviceLocation, netTypeDetector);
        }

        @Override
        public Persistent createPersistent(String filename) {
            return mockPersistent;
        }
    }

}