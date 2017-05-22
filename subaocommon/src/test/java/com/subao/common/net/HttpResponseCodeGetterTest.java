package com.subao.common.net;

import com.subao.common.RoboBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;

public class HttpResponseCodeGetterTest extends RoboBase {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test(expected = IOException.class)
    public void testFail() throws IOException {
        Http.HttpResponseCodeGetter.execute("localhost", 1, 100, 100);
    }

    @Test
    public void testOk() throws IOException {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(400);
        mockWebServer.enqueue(mockResponse);
        assertEquals(400, Http.HttpResponseCodeGetter.execute(
            mockWebServer.getHostName(),
            mockWebServer.getPort(), 3000, 3000));
    }

}
