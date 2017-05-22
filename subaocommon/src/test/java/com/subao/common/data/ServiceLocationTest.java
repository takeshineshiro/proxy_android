package com.subao.common.data;

import com.subao.common.RoboBase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * ServiceLocationTest
 * <p>Created by YinHaiBo on 2016/11/22.</p>
 */
public class ServiceLocationTest extends RoboBase {

    public static final String PROTOCOL = "http";
    public static final String HOST = "api.xunyou.mobi";
    public static final int PORT = -1;

    @Test
    public void constructor() {
        ServiceLocation serviceLocation = new ServiceLocation(PROTOCOL, HOST, PORT);
        assertEquals(PROTOCOL, serviceLocation.protocol);
        assertEquals(HOST, serviceLocation.host);
        assertEquals(PORT, serviceLocation.port);
        serviceLocation = new ServiceLocation(null, HOST, PORT);
        assertEquals("http", serviceLocation.protocol);
        serviceLocation = new ServiceLocation("", HOST, PORT);
        assertEquals("http", serviceLocation.protocol);
    }

    @Test
    public void equals() throws Exception {
        ServiceLocation serviceLocation = new ServiceLocation(PROTOCOL, HOST, PORT);
        assertEquals(serviceLocation, serviceLocation);
        assertFalse(serviceLocation.equals(null));
        assertFalse(serviceLocation.equals(this));
        ServiceLocation obj = new ServiceLocation(PROTOCOL, HOST, PORT);
        assertEquals(obj, serviceLocation);
        assertFalse(serviceLocation.equals(new ServiceLocation(PROTOCOL + "1", HOST, PORT)));
        assertFalse(serviceLocation.equals(new ServiceLocation(PROTOCOL, HOST + "1", PORT)));
        assertFalse(serviceLocation.equals(new ServiceLocation(PROTOCOL, HOST, PORT + 100)));
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("http://hello", new ServiceLocation(null, "hello", -1).toString());
        assertEquals("http://hello", new ServiceLocation("http", "hello", -1).toString());
        assertEquals("https://hello", new ServiceLocation("https", "hello", -1).toString());
        assertEquals("http://hello:8080", new ServiceLocation(null, "hello", 8080).toString());
    }

    @Test
    public void parse() {
        assertNull(ServiceLocation.parse(null));
        assertNull(ServiceLocation.parse(""));
        assertNull(ServiceLocation.parse(":"));
        assertNull(ServiceLocation.parse("/"));
        assertNull(ServiceLocation.parse("//"));
        assertNull(ServiceLocation.parse("://h"));
        assertNull(ServiceLocation.parse(":80"));

        //
        ParseTestParam[] params = new ParseTestParam[] {
            new ParseTestParam("https://www.google.com:8080/hello/world", "https", "www.google.com", 8080),
            new ParseTestParam("http://www.google.com:8080/hello/world", "http", "www.google.com", 8080),
            new ParseTestParam("www.google.com", "http", "www.google.com", -1),
            new ParseTestParam("https://hello", "https", "hello", -1),
            new ParseTestParam("https://hello:world", "https", "hello", -1),
        };
        for (ParseTestParam param : params) {
            ServiceLocation serviceLocation = ServiceLocation.parse(param.url);
            assertEquals(param.protocol, serviceLocation.protocol);
            assertEquals(param.host, serviceLocation.host);
            assertEquals(param.port, serviceLocation.port);
        }
    }

    private static class ParseTestParam {
        public final String url;
        public final String protocol;
        public final String host;
        public final int port;

        public ParseTestParam(String url, String protocol, String host, int port) {
            this.url = url;
            this.protocol = protocol;
            this.host = host;
            this.port = port;
        }
    }
}