package com.subao.common;

import android.os.ConditionVariable;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * MockWebServerDispatcher
 * <p>Created by YinHaiBo on 2017/2/10.</p>
 */
public class MockWebServerDispatcher extends Dispatcher {

    private final ConditionVariable overFlag = new ConditionVariable();

    private int responseCode = 200;
    private String responseBody;

    private RecordedRequest lastRequest;

    public static MockWebServerDispatcher createAndSetToWebServer(MockWebServer mockWebServer) {
        MockWebServerDispatcher dispatcher = new MockWebServerDispatcher();
        mockWebServer.setDispatcher(dispatcher);
        return dispatcher;
    }

    public void setResponseCode(int code) {
        this.responseCode = code;
    }

    public void setResponseBody(String body) {
        this.responseBody = body;
    }

    public RecordedRequest getLastRequest() {
        return lastRequest;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        this.lastRequest = request;
        MockResponse response = new MockResponse().setResponseCode(responseCode);
        if (responseBody != null) {
            response.setBody(responseBody);
        }
        overFlag.open();
        return response;
    }

    public void waitForResponse() {
        this.overFlag.block();
    }

    public void waitForResponse(long timeoutMillis) {
        this.overFlag.block(timeoutMillis);
    }

    public void resetOverFlag() {
        this.overFlag.close();
    }
}
