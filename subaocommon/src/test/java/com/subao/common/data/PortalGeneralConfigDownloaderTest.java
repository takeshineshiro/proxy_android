package com.subao.common.data;

import android.os.ConditionVariable;

import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.auth.AuthExecutor;
import com.subao.common.io.Persistent;
import com.subao.common.jni.JniWrapper;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.mock.MockPersistent;
import com.subao.common.net.Http;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.net.ShadowHttpForRuntimeException;
import com.subao.common.thread.ThreadPool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * PortalGeneralConfigDownloaderTest
 * <p>Created by YinHaiBo on 2017/2/16.</p>
 */
public class PortalGeneralConfigDownloaderTest extends RoboBase {

    private static final String RESPONSE_BODY = "{\"hello\":\"world\",\"question\":\"answer\"}";
    private static final String ARGUMENT_VERSION = "2.0.0";
    private static final String ARGUMENT_CLIENT_TYPE = "client-type";
    private static final String CACHE_TAG = "12345";
    private static final long EXPIRE_TIME = 1234L;

    private final Queue<String> processes = new LinkedList<String>();

    private MockWebServer mockWebServer;
    private MockNetTypeDetector mockNetTypeDetector;
    private MockArguments mockArguments;

    private Downloader downloader;

    @Before
    public void setUp() throws IOException {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        processes.clear();
        JniWrapper jniWrapper = mock(JniWrapper.class);
        Answer<Void> answer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String key = invocation.getArgument(0);
                String value = invocation.getArgument(1);
                processes.offer(key);
                processes.offer(value);
                return null;
            }
        };
        doAnswer(answer).when(jniWrapper).defineConst(anyString(), anyString());
        //
        mockNetTypeDetector = new MockNetTypeDetector();
        mockNetTypeDetector.setCurrentNetworkType(NetTypeDetector.NetType.WIFI);
        //
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockArguments = new MockArguments(
            new ServiceLocation("http", mockWebServer.getHostName(), mockWebServer.getPort()),
            mockNetTypeDetector);
        downloader = new Downloader(mockArguments, jniWrapper);
        //
        AuthExecutor.init(null, "test", null);
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        mockWebServer = null;
        processes.clear();
        Logger.setLoggableChecker(null);
    }

    @Test
    public void defaultServiceLocation() throws MalformedURLException {
        MockArguments mockArguments = new MockArguments(null, mockNetTypeDetector);
        Downloader downloader = new Downloader(mockArguments, null);
        URL url = downloader.buildUrl();
        String expected = String.format("http://%s/api/v1/%s/%s",
            Address.EndPoint.PORTAL.host, ARGUMENT_CLIENT_TYPE, downloader.getUrlPart());
        assertEquals(expected, url.toString());
    }

    @Test
    public void test1() {
        assertEquals("general", downloader.getId());
        assertEquals("configs/general", downloader.getUrlPart());
        assertEquals(mockArguments, downloader.getArguments());
    }

    @Test
    public void testDisconnect() {
        mockNetTypeDetector.setCurrentNetworkType(NetTypeDetector.NetType.DISCONNECT);
        startDownloaderAndWaitComplete();
        assertNull(downloader.portalData);
        downloader.onPostExecute(null);
    }

    @Test
    public void testNetTimeout() {
        startDownloaderAndWaitComplete();
        assertNull(downloader.portalData);
    }

    @Test
    public void testEmptyResponse() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        dispatcher.responseBody = "{}";
        mockWebServer.setDispatcher(dispatcher);
        startDownloaderAndWaitComplete();
        assertEquals(2, downloader.portalData.getDataSize());
        downloader.onPostExecute(downloader.portalData);
        assertTrue(processes.isEmpty());
    }

    @Test
    public void testInvalidResponse_IOException() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        dispatcher.responseBody = "{{{{{}";
        mockWebServer.setDispatcher(dispatcher);
        startDownloaderAndWaitComplete();
        downloader.onPostExecute(downloader.portalData);
        assertTrue(processes.isEmpty());
    }

    @Test
    public void testInvalidResponse_RuntimeException() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        dispatcher.responseBody = "{\"result\":[]}";
        mockWebServer.setDispatcher(dispatcher);
        startDownloaderAndWaitComplete();
        downloader.onPostExecute(downloader.portalData);
        assertTrue(processes.isEmpty());
    }

    @Test
    public void testNormal() {
        mockWebServer.setDispatcher(new MockWebServerDispatcher());
        startDownloaderAndWaitComplete();
        assertEquals(mockArguments.filename, "general.portal2");
        //
        assertEquals(RESPONSE_BODY.getBytes().length, downloader.portalData.getDataSize());
        downloader.onPostExecute(downloader.portalData);
        assertEquals(4, processes.size());
        assertEquals("hello", processes.poll());
        assertEquals("world", processes.poll());
        assertEquals("question", processes.poll());
        assertEquals("answer", processes.poll());
    }

    @Test
    public void testNotModify() throws IOException {
        PortalDataEx data = new PortalDataEx(CACHE_TAG, EXPIRE_TIME, ARGUMENT_VERSION, "content".getBytes());
        byte[] bytes = PortalDataExTest.serializeToByteArray(data);
        mockArguments.mockPersistent.setData(bytes);
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        startDownloaderAndWaitComplete();
        // 除了ExpireTime，别的都应该一样
        assertEquals(data.cacheTag, downloader.portalData.cacheTag);
        assertEquals(data.version, downloader.portalData.version);
        assertArrayEquals(data.getData(), downloader.portalData.getData());
        assertEquals(data.isNewByDownload, downloader.portalData.isNewByDownload);
        // ExpireTime 应该是当前时间加上max-age
        long delta = System.currentTimeMillis() + 300 * 1000L - downloader.portalData.getExpireTime();
        assertTrue(delta < 100 && delta > -100);
        //
        assertEquals(CACHE_TAG, dispatcher.cacheTag);
        assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, dispatcher.lastResponseCode);
    }

    @Test
    public void testInvalidResponseCode() throws IOException {
        PortalDataEx data = new PortalDataEx(null, EXPIRE_TIME, ARGUMENT_VERSION, "content".getBytes());
        byte[] bytes = PortalDataExTest.serializeToByteArray(data);
        mockArguments.mockPersistent.setData(bytes);
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        dispatcher.responseCode = 500;
        mockWebServer.setDispatcher(dispatcher);
        startDownloaderAndWaitComplete();
        assertEquals(data, downloader.portalData);
        assertEquals(500, dispatcher.lastResponseCode);
    }

    @Test
    @Config(shadows = ShadowHttpForRuntimeException.class)
    public void testHttpRuntimeException() throws IOException {
        PortalDataEx data = new PortalDataEx(null, EXPIRE_TIME, ARGUMENT_VERSION, "content".getBytes());
        byte[] bytes = PortalDataExTest.serializeToByteArray(data);
        mockArguments.mockPersistent.setData(bytes);
        //
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        startDownloaderAndWaitComplete();
        assertEquals(data, downloader.portalData);
    }

    @Test
    public void testLoadFromPersistent() {
        mockArguments.mockPersistent.setData("{}".getBytes());
        startDownloaderAndWaitComplete();
        assertNull(downloader.portalData);
    }

    @Test
    public void checkDownloadData() {
        downloader.checkDownloadDataFail = true;
        mockWebServer.setDispatcher(new MockWebServerDispatcher());
        startDownloaderAndWaitComplete();
        assertNull(downloader.portalData);
    }

    @Test
    public void serializeDownloadDataException() {
        mockWebServer.setDispatcher(new MockWebServerDispatcher());
        mockArguments.mockPersistent.ioExceptionWhenOpenOutput = true;
        startDownloaderAndWaitComplete();
        assertNotNull(downloader.portalData);
    }

    @Test
    public void testArguments() {
        String[] stringList = new String[]{null, "", "test"};
        ServiceLocation[] serviceLocations = new ServiceLocation[]{
            null, new ServiceLocation("http", "localhost", 1234)
        };
        NetTypeDetector[] netTypeDetectors = new NetTypeDetector[]{null, new MockNetTypeDetector()};
        for (String clientType : stringList) {
            for (String version : stringList) {
                for (ServiceLocation serviceLocation : serviceLocations) {
                    for (NetTypeDetector netTypeDetector : netTypeDetectors) {
                        PortalDataDownloader.Arguments arguments = new PortalDataDownloader.Arguments(
                            clientType, version, serviceLocation, netTypeDetector
                        ) {
                            @Override
                            public Persistent createPersistent(String filename) {
                                return null;
                            }
                        };
                        assertEquals(clientType, arguments.clientType);
                        assertEquals(version, arguments.version);
                        ServiceLocation expectedServiceLocation;
                        if (serviceLocation == null) {
                            expectedServiceLocation = new ServiceLocation("http", Address.EndPoint.PORTAL.host, Address.EndPoint.PORTAL.port);
                        } else {
                            expectedServiceLocation = serviceLocation;
                        }
                        assertEquals(expectedServiceLocation, arguments.serviceLocation);
                        assertEquals(netTypeDetector, arguments.netTypeDetector);
                    }
                }
            }
        }
    }

    @Test
    public void testCacheControl1() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        dispatcher.cacheControl = null;
        startDownloaderAndWaitComplete();
        assertEquals(0L, downloader.portalData.getExpireTime());
    }

    @Test
    public void testCacheControl2() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        dispatcher.cacheControl = "max-age";
        startDownloaderAndWaitComplete();
        assertEquals(0L, downloader.portalData.getExpireTime());
    }

    @Test
    public void testCacheControl3() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        dispatcher.cacheControl = "max-age=";
        startDownloaderAndWaitComplete();
        assertEquals(0L, downloader.portalData.getExpireTime());
    }

    @Test
    public void testCacheControl4() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        dispatcher.cacheControl = "Hello world, ha ha ha";
        startDownloaderAndWaitComplete();
        assertEquals(0L, downloader.portalData.getExpireTime());
    }

    @Test
    public void testCacheControl5() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        dispatcher.cacheControl = "max-age=hello";
        startDownloaderAndWaitComplete();
        assertEquals(0L, downloader.portalData.getExpireTime());
    }

    @Test
    public void testCacheControl6() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        dispatcher.cacheControl = "max-age=123";
        startDownloaderAndWaitComplete();
        long delta = System.currentTimeMillis() + 123L * 1000 - downloader.portalData.getExpireTime();
        assertTrue(delta < 1000L);
    }

    @Test
    public void testExpireTime() throws IOException {
        PortalDataEx portalDataEx = new PortalDataEx(null, System.currentTimeMillis() + 100000L, "2.0.0", null);
        mockWebServer.shutdown();
        downloader.executeOnExecutor(ThreadPool.getExecutor(), portalDataEx);
        downloader.flag.block();
        assertEquals(portalDataEx, downloader.portalData);
    }

    @Test
    public void test404() {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        dispatcher.responseCode = 404;
        MockPersistent mockPersistent = mockArguments.mockPersistent;
        mockPersistent.setData(new byte[128]);
        assertTrue(mockPersistent.exists());
        startDownloaderAndWaitComplete();
        assertFalse(mockPersistent.exists());
    }

    private void startDownloaderAndWaitComplete() {
        downloader.executeOnExecutor(ThreadPool.getExecutor());
        downloader.flag.block();
    }

    private static class MockWebServerDispatcher extends Dispatcher {

        String responseBody = RESPONSE_BODY;
        int responseCode = 200;

        String requestPath;
        String cacheTag;
        int lastResponseCode;

        String cacheControl = "max-age=300";

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            this.requestPath = request.getPath();
            this.cacheTag = request.getHeader(Http.CACHE_IF_NONE_MATCH);
            MockResponse response = new MockResponse();
            //
            if (Misc.isEquals(this.cacheTag, CACHE_TAG)) {
                response.setResponseCode(lastResponseCode = HttpURLConnection.HTTP_NOT_MODIFIED);
                setCacheControlField(response);
            } else {
                setCacheControlField(response);
                response.setResponseCode(lastResponseCode = responseCode);
                if (responseBody != null) {
                    response.setBody(responseBody);
                }
            }
            return response;
        }

        private void setCacheControlField(MockResponse response) {
            if (cacheControl != null) {
                response.setHeader("Cache-Control", cacheControl);
            }
        }
    }

    private static class MockArguments extends PortalDataDownloader.Arguments {

        MockPersistent mockPersistent = new MockPersistent();
        String filename;

        public MockArguments(ServiceLocation serviceLocation, NetTypeDetector netTypeDetector) {
            super(ARGUMENT_CLIENT_TYPE, ARGUMENT_VERSION, serviceLocation, netTypeDetector);
        }

        @Override
        public Persistent createPersistent(String filename) {
            this.filename = filename;
            return mockPersistent;
        }
    }

    private class Downloader extends PortalGeneralConfigDownloader {

        final ConditionVariable flag = new ConditionVariable();

        PortalDataEx portalData;

        boolean checkDownloadDataFail;

        public Downloader(Arguments arguments, JniWrapper jniWrapper) {
            super(arguments, jniWrapper);
        }

        @Override
        protected boolean checkDownloadData(PortalDataEx data) {
            if (checkDownloadDataFail) {
                return false;
            }
            return super.checkDownloadData(data);
        }

        @Override
        protected PortalDataEx doInBackground(PortalDataEx... params) {
            try {
                this.portalData = super.doInBackground(params);
            } finally {
                flag.open();
            }
            return this.portalData;
        }
    }

}