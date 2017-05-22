package com.subao.common.net;

import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.net.Http.ContentType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.exceptions.verification.TooManyActualInvocations;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Config(sdk = 23)
public class HttpTest extends RoboBase {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8000;
    private MockWebServer mockWebServer;

    private static URL createTestURL() throws MalformedURLException {
        return new URL("http", SERVER_HOST, SERVER_PORT, "/");
    }

    @Before
    public void setUp() throws IOException {
        Logger.setLoggableChecker(new Logger.LoggableChecker() {
            @Override
            public boolean isLoggable(String tag, int level) {
                return true;
            }
        });
        mockWebServer = new MockWebServer();
        mockWebServer.start(InetAddress.getByName(SERVER_HOST), SERVER_PORT);
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        Logger.setLoggableChecker(null);
    }

    @Test
    public void testMethod() {
        assertEquals("GET", Http.Method.GET.str);
        assertEquals("POST", Http.Method.POST.str);
        assertEquals("PUT", Http.Method.PUT.str);
        assertEquals("DELETE", Http.Method.DELETE.str);
        assertEquals(4, Http.Method.values().length);
    }

    @Test
    public void testContentType() {
        assertEquals("*", ContentType.ANY.str);
        assertEquals("text/html", ContentType.HTML.str);
        assertEquals("application/json", ContentType.JSON.str);
        assertEquals("application/x-protobuf", ContentType.PROTOBUF.str);
        assertEquals(4, ContentType.values().length);
    }

    @Test
    public void testResponse() {
        Http.Response r = new Http.Response(123, new byte[]{1, 2, 3});
        assertEquals(123, r.code);
        assertEquals(1, r.data[0]);
        assertEquals(2, r.data[1]);
        assertEquals(3, r.data[2]);
        r.toString();
    }

    @Test
    public void testConstructor() {
        Http http = new Http(123, 456);
        assertEquals(123, http.getConnectTimeout());
        assertEquals(456, http.getReadTimeout());
    }

    @Test(expected = IOException.class)
    public void testCreateHttpURL() throws IOException {
        URL url = Http.createHttpURL("www.example.com", 8888, "/hello");
        assertEquals("http://www.example.com:8888/hello", url.toString());
        String urlString = "http://xunyou.mobi/test?hello=world";
        url = Http.createURL(urlString);
        assertEquals(urlString, url.toString());
        Http.createHttpURL(null, 0, null);
    }

    @Test(expected = IOException.class)
    public void testCreateURLNULL() throws IOException {
        Http.createURL(null);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateHttpUrlConnection_NullURL() throws IOException {
        Http http = new Http(1000, 2000);
        http.createHttpUrlConnection(null, Http.Method.GET, ContentType.HTML.str);
    }

    @Test
    public void createHttpUrlConnection() throws IOException {
        Http http = new Http(1000, 2000);
        URL url = createTestURL();
        http.createHttpUrlConnection(url, Http.Method.GET, ContentType.HTML.str);
        http.createHttpUrlConnection(url, null, ContentType.HTML.str);
        http.createHttpUrlConnection(new URL("https://xunyou.mobi"), Http.Method.GET, null);
    }

    @Test
    public void testUrlConnection() throws IOException {
        Http http = new Http(500, 500);
        URL url = createTestURL();
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(404);
        mockResponse.setBody("Hello");
        mockWebServer.enqueue(mockResponse);
        HttpURLConnection conn = http.createHttpUrlConnection(url, Http.Method.GET, ContentType.HTML.str);
        Http.Response response = Http.readDataFromURLConnection(conn);
        assertEquals(404, response.code);
        assertEquals("Hello", new String(response.data));
    }

    @Test
    public void readDataFromURLConnectionException() throws IOException {
//        MockResponse mockResponse = new MockResponse();
//        mockResponse.setResponseCode(404);
//        mockResponse.setBody("Hello");
//        mockWebServer.enqueue(mockResponse);
        //
        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getInputStream()).thenThrow(RuntimeException.class);
        try {
            Http.readDataFromURLConnection(conn);
            fail();
        } catch (IOException e) {}
    }

    @Test
    public void doGet() throws IOException {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setBody("hello");
        mockWebServer.enqueue(mockResponse);
        //
        Http http = new Http(5000, 5000);
        URL url = createTestURL();
        Http.Response response = http.doGet(url, ContentType.HTML.str);
        assertEquals(200, response.code);
        assertEquals("hello", new String(response.data));
    }

    @Test
    public void doPost() throws IOException, InterruptedException {
        doPost(false);
    }

    @Test
    public void doPostCompress() throws IOException, InterruptedException {
        doPost(true);
    }

    private void doPost(boolean compress) throws IOException, InterruptedException {
        String responseBody = "hello, world";
        MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(responseBody);
        mockWebServer.enqueue(mockResponse);
        //
        Http http = new Http(500, 500);
        URL url = createTestURL();
        byte[] postData = new byte[128];
        for (int i = 0, len = postData.length; i < len; ++i) {
            postData[i] = (byte) (i % 16);
        }
        Http.Response response;
        if (compress) {
            response = http.doPost(url, postData, ContentType.HTML.str, compress);
        } else {
            response = http.doPost(url, postData, ContentType.HTML.str);
        }
        assertEquals(200, response.code);
        assertEquals(responseBody, new String(response.data));
        //
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        Buffer body = recordedRequest.getBody();
        //
        int bodySize = (int) recordedRequest.getBodySize();
        byte[] bodyData = body.readByteArray();
        if (compress) {
            assertEquals("gzip", recordedRequest.getHeader("Content-Encoding"));
            byte[] outputBuffer = new byte[postData.length];
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bodyData));
            try {
                assertEquals(outputBuffer.length, gzipInputStream.read(outputBuffer));
            } finally {
                Misc.close(gzipInputStream);
            }
            bodyData = outputBuffer;
        } else {
            assertEquals(postData.length, bodySize);
        }
        Arrays.equals(bodyData, postData);
    }

    @Test(expected = TooManyActualInvocations.class)
    public void testSetRequestContentType() throws IOException {
        HttpURLConnection mockConn = mock(HttpURLConnection.class);
        Http.setRequestContentType(mockConn, "abcd");
        verify(mockConn).setRequestProperty("Content-Type", "abcd");
        Http.setRequestContentType(mockConn, ContentType.ANY);
        verify(mockConn).setRequestProperty("Content-Type", ContentType.ANY.str);
        Http.setRequestContentType(mockConn, (String) null);
        verify(mockConn).setRequestProperty(anyString(), anyString());
    }

    @Test
    public void testSetRequestAccept() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) createTestURL().openConnection();
        Http.setRequestAccept(conn, (String) null);
        Http.setRequestAccept(conn, (ContentType) null);
        Http.setRequestAccept(conn, "abcd");
        Http.setRequestAccept(conn, ContentType.ANY);
    }

    @Test
    public void testDefaultStrictVerifier() {
        Http.StrictVerifier strictVerifier = new Http.DefaultStrictVerifier();
        assertFalse(strictVerifier.verify(null));
        assertFalse(strictVerifier.verify(""));
        assertFalse(strictVerifier.verify("api.xunyou.mobi"));
        assertFalse(strictVerifier.verify("uat.xunyou.mobi"));

        assertTrue(strictVerifier.verify("www.baidu.com"));
        assertTrue(strictVerifier.verify("1.2.3"));
        assertTrue(strictVerifier.verify("122.211.32."));
        assertTrue(strictVerifier.verify("333.1.2.3"));
        assertTrue(strictVerifier.verify("61.139.2.69"));
    }

    @Test(expected = IOException.class)
    public void getHttpResponseCodeException1() throws IOException {
        Http http = new Http(1000, 1000);
        String url = createTestURL().toString();
        http.getHttpResponseCode(url);
    }

    @Test(expected = IOException.class)
    public void getHttpResponseCodeException2() throws IOException {
        Http http = new Http(100, 100);
        HttpURLConnection urlConnection = http.createHttpUrlConnection(createTestURL(), Http.Method.GET, null);
        Http.getResponseCode(urlConnection);
    }

    @Test(expected = IOException.class)
    public void getHttpResponseCodeException3() throws IOException {
        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        when(mockConnection.getResponseCode()).thenThrow(RuntimeException.class);
        Http.getResponseCode(mockConnection);
    }

    @Test
    public void getHttpResponseCode() throws IOException {
        MockResponse mockResponse = new MockResponse();
        int code = 204;
        mockResponse.setResponseCode(code);
        mockWebServer.enqueue(mockResponse);
        //
        String url = createTestURL().toString();
        assertEquals(code, new Http(1000, 1000).getHttpResponseCode(url));
    }

    @Test
    public void setStrictVerifier() {
        Http.setStrictVerifier(new Http.StrictVerifier() {
            @Override
            public boolean verify(String hostname) {
                return false;
            }
        });
        Http.setStrictVerifier(null);
    }

}
