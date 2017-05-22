package com.subao.common.net;

import android.annotation.SuppressLint;
import android.os.ConditionVariable;

import com.subao.common.Misc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by hujd on 16-7-21.
 */
@RunWith(CustomShadowTestRunner.class)
@Config(shadows = {MyShadowAsyncTask.class}, manifest = Config.NONE)
public class HttpClientTest {

    private ResponseCallback callback;
    private List<RequestProperty> header;

    @Before
    public void setup() {
        header = new ArrayList<RequestProperty>(1);
        callback = new ResponseCallback(null, 1) {

            @Override
            protected void onSuccess(int code, byte[] response) {
                assertTrue(code >= 200 && code < 300);
            }

            @Override
            protected void onFail(int code, byte[] responseData) {
                assertFalse(code >= 200 && code < 300);
            }

            @Override
            protected String getEventName() {
                return null;
            }
        };
    }

    @Test
    public void testGet() throws InterruptedException {
        HttpClient.get(header, callback, "http://www.baidu.com");
    }

    @Test
    public void testPost() throws Exception {
        HttpClient.post(header, callback, "http://www.baidu.com", null);
    }

//    @Test
//    public void testDelete() throws Exception {
//        HttpClient.delete(header, callback, "http://www.baidu.com");
//    }

    @Test
    public void testHttpHandler() throws IOException {
        int port = 8182;
        MockServer mockServer = new MockServer(port);
        mockServer.start();
        mockServer.waitForReady();
        header.add(new RequestProperty("Authorization", "test"));
        @SuppressLint("DefaultLocale") HttpClient.Requestor requestor = new HttpClient.Requestor(
            callback,
            String.format("http://127.0.0.1:%d", port),
            Http.Method.POST,
            "hello".getBytes(), header);
        try {
            assertNotNull(requestor.httpHandle());
        } catch (IOException e) {}

//        requestor = new HttpClient.Requestor(callback, "http://192.168.1.75", Http.Method.POST,
//                "hello".getBytes(), header);
//        assertNull(requestor.httpHandle());
    }

    @Test(expected = NullPointerException.class)
    public void testRequestorparamsNull() {
        new HttpClient.Requestor(null, null, null, null, null);
    }

    private static class MockServer extends Thread {

        private final int port;
        private final Map<String, String> headers = new HashMap<String, String>();

        private final ConditionVariable ready = new ConditionVariable();

        public MockServer(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                ServerSocket server = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
                try {
                    ready.open();
                    Socket client = server.accept();
                    try {
                        processClient(client);
                    } finally {
                        try {
                            client.close();
                        } catch (IOException e) {
                        }
                    }
                } finally {
                    try {
                        server.close();
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e) {
            }
        }

        private void processClient(Socket client) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            try {
                reader.readLine();
                headers.clear();
                while (true) {
                    String header = reader.readLine();
                    if (header.length() == 0) {
                        break;
                    }
                    int colon = header.indexOf(":");
                    String key = header.substring(0, colon);
                    String value = header.substring(colon + 1).trim();
                    headers.put(key, value);
                }

                PrintWriter writer = new PrintWriter(client.getOutputStream());
                try {
                    writer.println("HTTP/1.1 200 OK");
                    String body = "Hello";
                    writer.println("Content-Length: " + body.length());
                    writer.println("");
                    writer.print(body);
                    writer.flush();
                } finally {
                    Misc.close(writer);
                }
            } finally {
                Misc.close(reader);
            }
        }

        public void waitForReady() {
            this.ready.block();
        }
    }

}