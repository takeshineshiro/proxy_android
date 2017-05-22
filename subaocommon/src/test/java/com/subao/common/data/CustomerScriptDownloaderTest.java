package com.subao.common.data;

import android.os.ConditionVariable;
import android.text.TextUtils;

import com.subao.common.RoboBase;
import com.subao.common.mock.MockNetTypeDetector;
import com.subao.common.net.ShadowHttpForRuntimeException;
import com.subao.common.thread.ThreadPool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * CustomerScriptDownloaderTest
 * <p>Created by YinHaiBo on 2017/2/9.</p>
 */
public class CustomerScriptDownloaderTest extends RoboBase {

    private static final String CLIENT_TYPE = "abcd-ef-223";
    private static final String CLIENT_VERSION = "2.0.0";
    private static final String SUBAO_ID = "Subao Id";
    private static final String SERVICE_ID = "Service Id/";
    private static final String USER_ID = "User:Id";
    private static final String JWT_TOKEN = "Come on BB";

    private static final String SCRIPT = "Hello, world";

    private MockWebServer mockWebServer;
    private MyDispatcher myDispatcher;
    private MockNetTypeDetector mockNetTypeDetector;

    private static Downloader createDownloadNormal(HRDataTrans.Arguments arguments) {
        return new Downloader(arguments, JWT_TOKEN, null, USER_ID, SERVICE_ID);
    }

    @Before
    public void setUp() throws IOException, NoSuchAlgorithmException {
        mockNetTypeDetector = new MockNetTypeDetector();
        mockWebServer = new MockWebServer();
        myDispatcher = new MyDispatcher();
        mockWebServer.setDispatcher(myDispatcher);
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        mockWebServer = null;
    }

    private HRDataTrans.Arguments createArguments() {
        return new HRDataTrans.Arguments(
            CLIENT_TYPE, CLIENT_VERSION,
            new ServiceLocation("http", mockWebServer.getHostName(), mockWebServer.getPort()),
            mockNetTypeDetector
        );
    }

    @Test
    public void params() {
        HRDataTrans.Arguments arguments = createArguments();
        assertEquals(arguments.clientType, CLIENT_TYPE);
        assertEquals(arguments.version, CLIENT_VERSION);
        assertEquals(arguments.serviceLocation.host, mockWebServer.getHostName());
        assertEquals(arguments.serviceLocation.port, mockWebServer.getPort());
        assertEquals(arguments.netTypeDetector, mockNetTypeDetector);
    }

    /**
     * 正常情况
     */
    @Test
    public void hasScript() {
        HRDataTrans.Arguments arguments = createArguments();
        Downloader downloader = createDownloadNormal(arguments);
        assertEquals(downloader.arguments, arguments);
        assertEquals(downloader.userInfo.jwtToken, JWT_TOKEN);
        assertEquals(downloader.userInfo.userId, USER_ID);
        assertNull(downloader.userInfoEx.subaoId);
        assertEquals(downloader.userInfoEx.serviceId, SERVICE_ID);
        downloader.executeOnExecutor(ThreadPool.getExecutor());
        downloader.flag.block();
        CustomerScriptDownloader.DownloadData downloadData = downloader.downloadData;
        assertEquals(200, downloadData.response.code);
        assertArrayEquals(SCRIPT.getBytes(), downloadData.response.data);
    }

    /**
     * 正常情况，但没有脚本可下载
     */
    @Test
    public void noScript() {
        HRDataTrans.Arguments arguments = createArguments();
        Downloader downloader = new Downloader(
            arguments, JWT_TOKEN, SUBAO_ID, USER_ID + 1, SERVICE_ID);
        downloader.executeOnExecutor(ThreadPool.getExecutor());
        downloader.flag.block();
        CustomerScriptDownloader.DownloadData downloadData = downloader.downloadData;
        assertEquals(404, downloadData.response.code);
    }

    @Test
    public void badRequest() {
        HRDataTrans.Arguments arguments = new HRDataTrans.Arguments(
            CLIENT_TYPE, "",
            new ServiceLocation("https", mockWebServer.getHostName(), mockWebServer.getPort()),
            mockNetTypeDetector
        );
        Downloader downloader = createDownloadNormal(arguments);
        downloader.executeOnExecutor(ThreadPool.getExecutor());
        downloader.flag.block();
        CustomerScriptDownloader.DownloadData downloadData = downloader.downloadData;
        assertEquals(400, downloadData.response.code);
    }

    @Test
    public void badAuth() throws IOException {
        HRDataTrans.Arguments arguments = createArguments();
        Downloader downloader = new Downloader(
            arguments, JWT_TOKEN + "bad", SUBAO_ID, USER_ID, SERVICE_ID);
        downloader.executeOnExecutor(ThreadPool.getExecutor());
        downloader.flag.block();
        CustomerScriptDownloader.DownloadData downloadData = downloader.downloadData;
        assertEquals(403, downloadData.response.code);
    }

    /**
     * IOException
     *
     * @throws IOException
     */
    @Test
    public void ioException() throws IOException {
        HRDataTrans.Arguments arguments = createArguments();
        Downloader downloader = createDownloadNormal(arguments);
        mockWebServer.shutdown();
        downloader.executeOnExecutor(ThreadPool.getExecutor());
        downloader.flag.block();
        CustomerScriptDownloader.DownloadData downloadData = downloader.downloadData;
        assertNull(downloadData.md5);
        assertNull(downloadData.response);
    }

    @Test
    @Config(shadows = ShadowHttpForRuntimeException.class)
    public void runtimeException() {
        HRDataTrans.Arguments arguments = createArguments();
        Downloader downloader = createDownloadNormal(arguments);
        downloader.executeOnExecutor(ThreadPool.getExecutor());
        downloader.flag.block();
        CustomerScriptDownloader.DownloadData downloadData = downloader.downloadData;
        assertNull(downloadData.md5);
        assertNull(downloadData.response);
    }

    private static class Downloader extends CustomerScriptDownloader {

        final ConditionVariable flag = new ConditionVariable();
        DownloadData downloadData;

        public Downloader(HRDataTrans.Arguments arguments, String jwtToken, String subaoId, String userId, String serviceId) {
            super(arguments, new UserInfo(userId, jwtToken), new UserInfoEx(serviceId, subaoId));
        }

        @Override
        protected Result doInBackground(Void... params) {
            Result result;
            try {
                result = super.doInBackground(params);
                downloadData = parseDownloadDataFromResult(result);
            } finally {
                flag.open();
            }
            return result;
        }

        @Override
        protected boolean useBearerAuth() {
            return true;
        }

        @Override
        protected String getUrlProtocol() {
            return "http";
        }
    }

    private static class MyDispatcher extends Dispatcher {

        Map<String, String> params = new HashMap<String, String>(8);

        private static MockResponse createResponseWithBadRequest() {
            return new MockResponse().setResponseCode(400);
        }

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            if (!"GET".equals(request.getMethod())) {
                return createResponseWithBadRequest();
            }
            // Http Header
            String bearer = request.getHeader("Authorization");
            if (TextUtils.isEmpty(bearer)
                || !bearer.startsWith("Bearer ")
                || !JWT_TOKEN.equals(bearer.substring(7))) {
                return new MockResponse().setResponseCode(403);
            }
            //
            String path = request.getPath();
            Pattern pattern = Pattern.compile("/api/v2/(.+)/scripts\\?");
            Matcher matcher = pattern.matcher(path);
            if (!matcher.find()) {
                return createResponseWithBadRequest();
            }
            String clientType = matcher.group(1);
            //
            String[] params = path.substring(matcher.end()).split("&");
            for (int i = 0, len = params.length; i < len; ++i) {
                String[] entry = params[i].split("=");
                if (entry.length == 2) {
                    String key = URLDecoder.decode(entry[0]);
                    String value = URLDecoder.decode(entry[1]);
                    this.params.put(key, value);
                } else if (entry.length != 1) {
                    return createResponseWithBadRequest();
                }
            }
            // 检查必填项
            String clientVersion = this.params.get("clientVersion");
            String userId = this.params.get("userId");
            String serviceId = this.params.get("serviceId");
            String subaoId = this.params.get("subaoId");
            if (clientVersion == null || userId == null || serviceId == null) {
                return createResponseWithBadRequest();
            }
            // 如果几要素均正常，返回Script
            if (USER_ID.equals(userId) && SERVICE_ID.equals(serviceId) && CLIENT_VERSION.equals(clientVersion)) {
                return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(SCRIPT);
            }
            return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
        }
    }
}