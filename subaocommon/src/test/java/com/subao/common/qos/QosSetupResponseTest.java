package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class QosSetupResponseTest extends RoboBase {
    @Test
    public void testQosSetupResponse() throws IOException {
        String sessionId = "sessionId";
        String speedingId = "speedingId";
        String operator = "operator";
        String operatorCode = "operatorCode";
        String vendor = "vendor";
        //
        StringWriter stringWriter = new StringWriter(1024);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            jsonWriter.beginObject();
            jsonWriter.name("resultCode").value(200);
            jsonWriter.name("errorInfo").nullValue();
            jsonWriter.name("sessionId").value(sessionId);
            jsonWriter.name("speedingId").value(speedingId);
            jsonWriter.name("operator").value(operator);
            jsonWriter.name("operatorCode").value(operatorCode);
            jsonWriter.name("vendor").value(vendor);
            jsonWriter.name("other").value("other");
            jsonWriter.endObject();
            jsonWriter.flush();
            //
            QosSetupResponse response = QosSetupResponse.parseFromJson(stringWriter.toString().getBytes());
            assertEquals(200, response.resultCode);
            assertEquals(sessionId, response.sessionId);
            assertEquals(speedingId, response.speedingId);
            assertEquals(operator, response.operator);
            assertEquals(operatorCode, response.operatorCode);
            assertEquals(vendor, response.vendor);
        } finally {
            Misc.close(jsonWriter);
        }

    }

//    @Test
//    public void testParseFromJson() throws IOException {
//        String jsonStr = "{\"resultCode\": 200, \"errorInfo\": \"fuck\", \"sessionId\": \"session\", \"speedingId\": \"xxx\", \"operator\": \"xxx\", " +
//            "\"operatorCode\": \"code\", \"vendor\": \"vend\"}";
//        assertNotNull(QosSetupResponse.parseFromJson(jsonStr.getBytes()));
//    }

    @Test(expected = NullPointerException.class)
    public void testParseFromJsonNull() throws IOException {
        QosSetupResponse.parseFromJson((byte[]) null);
    }
}