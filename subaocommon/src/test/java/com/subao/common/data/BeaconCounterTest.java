package com.subao.common.data;

import android.os.ConditionVariable;

import com.subao.common.MockWebServerDispatcher;
import com.subao.common.RoboBase;
import com.subao.common.collection.Ref;
import com.subao.common.net.Http;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * BeaconCounterTest
 * <p>Created by YinHaiBo on 2017/3/6.</p>
 */
public class BeaconCounterTest extends RoboBase {

    private static final String VALID_COUNTER_TYPE = "counter_type";

    private MockWebServer mockWebServer;
    private MockWebServerDispatcher dispatcher;
    private ServiceLocation serviceLocation;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        mockWebServer.start();
        this.serviceLocation = new ServiceLocation(null, mockWebServer.getHostName(), mockWebServer.getPort());

    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void test201() {
        dispatcher.setResponseCode(201);
        final ConditionVariable flag = new ConditionVariable();
        final Ref<Boolean> result = new Ref<Boolean>();
        BeaconCounter.start(
            Defines.REQUEST_CLIENT_TYPE_FOR_APP,
            serviceLocation, VALID_COUNTER_TYPE, new BeaconCounter.Callback() {
                @Override
                public void onCounter(boolean succeed) {
                    result.set(succeed);
                    flag.open();
                }
            });
        flag.block();
        assertTrue(result.get());
        RecordedRequest request = dispatcher.getLastRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/android/counters/" + VALID_COUNTER_TYPE, request.getPath());
    }

    @Test
    public void testFail() {
        dispatcher.setResponseCode(500);
        final ConditionVariable flag = new ConditionVariable();
        final Ref<Boolean> result = new Ref<Boolean>();
        BeaconCounter.start(
            Defines.REQUEST_CLIENT_TYPE_FOR_APP,
            serviceLocation, VALID_COUNTER_TYPE, new BeaconCounter.Callback() {
                @Override
                public void onCounter(boolean succeed) {
                    result.set(succeed);
                    flag.open();
                }
            });
        flag.block();
        assertFalse(result.get());
        RecordedRequest request = dispatcher.getLastRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/android/counters/" + VALID_COUNTER_TYPE, request.getPath());
    }

    @Test
    public void testIOException() throws IOException {
        mockWebServer.shutdown();
        final ConditionVariable flag = new ConditionVariable();
        final Ref<Boolean> result = new Ref<Boolean>();
        BeaconCounter.start(
            Defines.REQUEST_CLIENT_TYPE_FOR_APP,
            serviceLocation, VALID_COUNTER_TYPE, new BeaconCounter.Callback() {
                @Override
                public void onCounter(boolean succeed) {
                    result.set(succeed);
                    flag.open();
                }
            });
        flag.block();
        assertFalse(result.get());
    }

    @Test
    @org.robolectric.annotation.Config(shadows = ShadowHttp.class)
    public void testRuntimeException() {
        dispatcher.setResponseCode(500);
        final ConditionVariable flag = new ConditionVariable();
        final Ref<Boolean> result = new Ref<Boolean>();
        BeaconCounter.start(
            Defines.REQUEST_CLIENT_TYPE_FOR_APP,
            serviceLocation, VALID_COUNTER_TYPE, new BeaconCounter.Callback() {
                @Override
                public void onCounter(boolean succeed) {
                    result.set(succeed);
                    flag.open();
                }
            });
        flag.block();
        assertFalse(result.get());
    }

    @Implements(value = Http.class)
    public static class ShadowHttp {
        @Implementation
        public HttpURLConnection createHttpUrlConnection(URL url, Http.Method method, String contentType) throws IOException {
            throw new SecurityException();
        }
    }
}