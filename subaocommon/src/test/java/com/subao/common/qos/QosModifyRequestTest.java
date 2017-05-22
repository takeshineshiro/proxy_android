package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.RoboBase;

import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class QosModifyRequestTest extends RoboBase {
    @Test
    public void testQosModifyRequest() {
        QosModifyRequest request = new QosModifyRequest(1000, "abc");
        assertNotNull(request);

        StringWriter stringWriter = new StringWriter(1024);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            request.serialize(jsonWriter);
            jsonWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String str = stringWriter.toString();
        assertNotNull(str);
        assertNotNull(request.toString());
    }

    @Test
    @Config(shadows = ShadowQosModifyRequest.class)
    public void testToStringException() {
        QosModifyRequest request = new QosModifyRequest(123, "hello");
        String s = request.toString();
        assertEquals("[time=123, token=hello]", s);
    }

    @Implements(QosModifyRequest.class)
    public static class ShadowQosModifyRequest {
        @Implementation
        public void serialize(JsonWriter writer) throws IOException {
            throw new IOException();
        }
    }
}