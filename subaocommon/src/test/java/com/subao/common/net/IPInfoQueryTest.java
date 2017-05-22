package com.subao.common.net;

import android.os.ConditionVariable;

import com.subao.common.Logger;
import com.subao.common.RoboBase;
import com.subao.common.data.ChinaISP;
import com.subao.common.data.ServiceLocation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * IPInfoQueryTest
 * <p>Created by YinHaiBo on 2017/2/27.</p>
 */
public class IPInfoQueryTest extends RoboBase {

    private MockWebServer mockWebServer;
    private MyDispatcher myDispatcher;
    private ServiceLocation serviceLocation;

    private static void sleepSometime() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void normalizeDispatcher(MyDispatcher myDispatcher, String ip) {
        myDispatcher.responseCode = 200;
        myDispatcher.responseBody = String.format("{\n" +
            "    \"hello\": \"world\",\n" +
            "    \"ip\": \"%s\",\n" +
            "    \"ipLib\": {\n" +
            "        \"province\": 51,\n" +
            "        \"operators\": 7,\n" +
            "        \"detail\": \"this is a detail\",\n" +
            "        \"other\": null\n" +
            "    }\n" +
            "}", ip);
        myDispatcher.expectedUrlPath = String.format("/resolve?ip=%s", ip);
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
        myDispatcher = new MyDispatcher();
        mockWebServer.setDispatcher(myDispatcher);
        mockWebServer.start();
        serviceLocation = new ServiceLocation(null, mockWebServer.getHostName(), mockWebServer.getPort());
        IPInfoQuery.onUserAuthComplete(true, serviceLocation);
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        Logger.setLoggableChecker(null);
    }

    @Test
    public void testMyIP_1() throws IOException {
        // 连续两次请求本机IP，优化请求为一次
        MyCallback callback1 = new MyCallback();
        MyCallback callback2 = new MyCallback();
        IPInfoQuery.executeThenCallbackInWorkThread(null, callback1, 123);
        IPInfoQuery.executeThenCallbackInWorkThread(null, callback2, 456);
        callback1.block();
        callback2.block();
        assertEquals(123, callback1.callbackContext);
        assertEquals(456, callback2.callbackContext);
    }

    @Test
    public void testMyIP_2() throws IOException {
        // 连续两次请求本机IP，优化请求为一次
        MyCallback callback1 = new MyCallback();
        MyCallback callback2 = new MyCallback();
        IPInfoQuery.execute(null, callback2, 456);
        IPInfoQuery.executeThenCallbackInWorkThread(null, callback1, 123);
        callback1.block();
        assertEquals(123, callback1.callbackContext);
    }

    @Test
    public void test_Normal() throws IOException {
        String ip = "61.139.2.69";
        normalizeDispatcher(myDispatcher, ip);
        IPInfoQuery.Result expectedResult = new IPInfoQuery.Result(ip, 51, 7, "this is a detail");
        //
        MyCallback callback = new MyCallback();
        IPInfoQuery.executeThenCallbackInWorkThread(ip, callback, 123);
        callback.block();
        assertEquals(123, callback.callbackContext);
        assertEquals(expectedResult, callback.result);
    }

    @Test
    public void testResult() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        testPrivateConstructor(IPInfoQuery.class);

        String ip = "127.0.0.1";
        int region = 31;
        int ispFlags = 10;
        String detail = "This is a detail";
        IPInfoQuery.Result result = new IPInfoQuery.Result(ip, region, ispFlags, detail);
        assertEquals(ip, result.ip);
        assertEquals(region, result.region);
        assertEquals(ispFlags, result.ispFlags);
        assertEquals(detail, result.detail);
        assertEquals(result, result);
        assertFalse(result.equals(null));
        assertFalse(result.equals(this));
        assertEquals(result, new IPInfoQuery.Result(ip, region, ispFlags, detail));
        assertNotNull(result.toString());
        //
        IPInfoQuery.Result result2 = new IPInfoQuery.Result(null, region, ispFlags, detail);
        assertNotNull(result2.toString());
        assertNotEquals(result, result2);
        //
        result2 = new IPInfoQuery.Result(ip, 0, ispFlags, detail);
        assertNotNull(result2.toString());
        assertNotEquals(result, result2);
        //
        result2 = new IPInfoQuery.Result(ip, region, 0, detail);
        assertNotNull(result2.toString());
        assertNotEquals(result, result2);
        //
        result2 = new IPInfoQuery.Result(ip, region, ispFlags, "");
        assertNotNull(result2.toString());
        assertNotEquals(result, result2);
        //
        for (int flag = 0; flag <= 16; ++flag) {
            result = new IPInfoQuery.Result("127.0.0.1", 11, flag, "detail");
            ChinaISP chinaISP;
            if ((flag & 8) != 0) {
                chinaISP = ChinaISP.CHINA_TELECOM;
            } else if ((flag & 4) != 0) {
                chinaISP = ChinaISP.CHINA_UNICOM;
            } else if ((flag & 3) != 0) {
                chinaISP = ChinaISP.CHINA_MOBILE;
            } else {
                chinaISP = null;
            }
            assertEquals(chinaISP, result.getISP());
        }
    }

    @Test
    public void testIOException() throws IOException {
        mockWebServer.shutdown();
        IPInfoQuery.Callback callback = new IPInfoQuery.Callback() {
            @Override
            public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
                assertEquals(123, callbackContext);
                assertNull(result);
            }
        };
        IPInfoQuery.Task task = new IPInfoQuery.Task(null, null, new IPInfoQuery.Query(callback, 123, false));
        assertNull(task.doInBackground());
    }

    @Test
    public void testRuntimeException() throws IOException {
        myDispatcher.responseCode = 200;
        myDispatcher.responseBody = "{" +
            "    \"hello\": \"world\",\n" +
            "    \"ip\": \"127.0.0.1\",\n" +
            "    \"ipLib\": {\n" +
            "        \"province\": [],\n" +
            "        \"operators\": 7,\n" +
            "        \"detail\": \"this is a detail\",\n" +
            "        \"other\": null\n" +
            "    }\n" +
            "}";
        myDispatcher.expectedUrlPath = "/resolve";

        IPInfoQuery.Callback callback = new IPInfoQuery.Callback() {
            @Override
            public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
                assertEquals(123, callbackContext);
                assertNull(result);
            }
        };
        IPInfoQuery.Task task = new IPInfoQuery.Task(null, null, new IPInfoQuery.Query(callback, 123, false));
        assertNull(task.doInBackground());
    }

    @Test
    public void testEmptyResponseBody() throws IOException {
        myDispatcher.responseCode = 200;
        myDispatcher.expectedUrlPath = "/resolve";
        IPInfoQuery.Callback callback = new IPInfoQuery.Callback() {
            @Override
            public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
                assertEquals(123, callbackContext);
                assertNull(result);
            }
        };
        IPInfoQuery.Task task = new IPInfoQuery.Task(null, null, new IPInfoQuery.Query(callback, 123, false));
        assertNull(task.doInBackground());
    }

    @Test
    public void testResponseNot200() throws IOException {
        myDispatcher.responseCode = 500;
        myDispatcher.expectedUrlPath = "/resolve";
        IPInfoQuery.Callback callback = new IPInfoQuery.Callback() {
            @Override
            public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
                assertEquals(123, callbackContext);
                assertNull(result);
            }
        };
        IPInfoQuery.Task task = new IPInfoQuery.Task(null, null, new IPInfoQuery.Query(callback, 123, false));
        assertNull(task.doInBackground());
    }

    @Test
    public void testPostExecute() throws IOException {
        IPInfoQuery.Callback callback = new IPInfoQuery.Callback() {
            @Override
            public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
                assertEquals(123, callbackContext);
                assertNull(result);
            }
        };
        IPInfoQuery.Query query = new IPInfoQuery.Query(
            callback, 123,
            false);
        IPInfoQuery.Task task = new IPInfoQuery.Task(null, "127.0.0.1", query);
        task.onPostExecute(null);
        //
        task = new IPInfoQuery.Task(null, null, query);
        IPInfoQuery.execute(null, callback, 123);
        task.onPostExecute(null);
    }

    @Test
    public void testWorkerByDNS() {
        IPInfoQuery.onUserAuthComplete(false, serviceLocation);
        // 不支持查询非本机的归属地
        MyCallback callback = new MyCallback();
        IPInfoQuery.executeThenCallbackInWorkThread("61.139.2.69", callback, 123);
        callback.block();
        assertNull(callback.result);
        assertEquals(123, callback.callbackContext);
        //
        callback = new MyCallback();
        IPInfoQuery.executeThenCallbackInWorkThread(null, callback, 123);
        callback.block();
        assertNotNull(callback.result);
        assertEquals(123, callback.callbackContext);
    }

    @Test(expected = IOException.class)
    public void testWorkerByDNS_2() throws IOException {
        MockAddressDetermine mockAddressDetermine = new MockAddressDetermine();
        IPInfoQuery.WorkerByDNS worker = new IPInfoQuery.WorkerByDNS(mockAddressDetermine);
        mockAddressDetermine.throwException = true;
        worker.execute(null);
    }

    @Test(expected = IOException.class)
    public void testWorkerByDNS_3() throws IOException {
        MockAddressDetermine mockAddressDetermine = new MockAddressDetermine();
        IPInfoQuery.WorkerByDNS worker = new IPInfoQuery.WorkerByDNS(mockAddressDetermine);
        mockAddressDetermine.address = null;
        worker.execute(null);
    }

    @Test
    public void testWorkerByDNS_4() throws IOException {
        MockAddressDetermine mockAddressDetermine = new MockAddressDetermine();
        IPInfoQuery.WorkerByDNS worker = new IPInfoQuery.WorkerByDNS(mockAddressDetermine);
        InetAddress mockAddress = mock(InetAddress.class);
        doReturn(new byte[3]).when(mockAddress).getAddress();
        mockAddressDetermine.address = mockAddress;
        assertNull(worker.execute(null));
    }

    @Test
    public void testWorkerByDNS_5() throws IOException {
        MockAddressDetermine mockAddressDetermine = new MockAddressDetermine();
        IPInfoQuery.WorkerByDNS worker = new IPInfoQuery.WorkerByDNS(mockAddressDetermine);
        mockAddressDetermine.address = Inet4Address.getByAddress(new byte[]{1, 2, 3, 4});
        assertNull(worker.execute(null));
    }

    @Test
    public void testWorkerByDNS_Ok() throws IOException {
        MockAddressDetermine mockAddressDetermine = new MockAddressDetermine();
        IPInfoQuery.WorkerByDNS worker = new IPInfoQuery.WorkerByDNS(mockAddressDetermine);
        mockAddressDetermine.makeAddress(51, 10);
        IPInfoQuery.Result result = worker.execute(null);
        assertEquals(51, result.region);
        assertEquals(ChinaISP.CHINA_TELECOM, result.getISP());
        //
        mockAddressDetermine.makeAddress(51, 11);
        result = worker.execute(null);
        assertEquals(ChinaISP.CHINA_UNICOM, result.getISP());
        //
        mockAddressDetermine.makeAddress(51, 12);
        result = worker.execute(null);
        assertEquals(ChinaISP.CHINA_MOBILE, result.getISP());
        //
        mockAddressDetermine.makeAddress(51, 1);
        result = worker.execute(null);
        assertNull(result.getISP());
    }

    @Test
    public void testExecuteInVIPMode() {
        String ip = "61.139.2.69";
        normalizeDispatcher(myDispatcher, ip);
        MyCallback myCallback = new MyCallback();
        IPInfoQuery.executeByVIPMode(ip, myCallback, 123, true, serviceLocation);
        myCallback.block();
        assertEquals(123, myCallback.callbackContext);
        assertNotNull(myCallback.result);
    }

    private static class MockAddressDetermine implements IPInfoQuery.WorkerByDNS.AddressDetermine {

        boolean throwException;
        InetAddress address;

        @Override
        public InetAddress execute(String host) throws UnknownHostException {
            if (throwException) {
                throw new UnknownHostException();
            }
            return address;
        }

        void makeAddress(int region, int isp) throws UnknownHostException {
            byte[] ip = new byte[]{(byte) 172, 16, (byte) region, (byte) isp};
            this.address = Inet4Address.getByAddress(ip);
        }

    }

    private static class MyCallback extends ConditionVariable implements IPInfoQuery.Callback {

        public Object callbackContext;
        public IPInfoQuery.Result result;

        @Override
        public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
            this.callbackContext = callbackContext;
            this.result = result;
            this.open();
        }

    }

    private static class MyDispatcher extends Dispatcher {

        int responseCode;
        String responseBody;
        String expectedUrlPath;

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            MockResponse response = new MockResponse();
            if (!"GET".equals(request.getMethod())) {
                return response.setResponseCode(403);
            }
            if (!request.getPath().equals(expectedUrlPath)) {
                return response.setResponseCode(403);
            }
            response.setResponseCode(responseCode);
            if (responseBody != null) {
                response.setBody(responseBody);
            }
            return response;
        }
    }

}