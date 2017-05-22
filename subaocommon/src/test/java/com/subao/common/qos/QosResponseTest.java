package com.subao.common.qos;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class QosResponseTest extends RoboBase {

    @Test
    public void testParseFromJson() throws Exception {
        int resultCode = 123;
        String errorInfo = "There is a error";
        String jsonStr = String.format(
            "{\"resultCode\": %d, \"hello\": null, \"errorInfo\": \"%s\"}",
            resultCode, errorInfo);
        QosResponse qosResponse = QosResponse.parseFromJson(new ByteArrayInputStream(jsonStr.getBytes()));
        assertEquals(resultCode, qosResponse.resultCode);
        assertEquals(errorInfo, qosResponse.errorInfo);
        //
        jsonStr = String.format(
            "{\"resultCode\": %d, \"hello\": null, \"errorInfo\": null}",
            resultCode);
        qosResponse = QosResponse.parseFromJson(new ByteArrayInputStream(jsonStr.getBytes()));
        assertEquals(resultCode, qosResponse.resultCode);
        assertNull(qosResponse.errorInfo);
    }

    @Test(expected = IOException.class)
    public void parseFromJsonException() throws IOException {
        String json = "{\"resultCode\":[]";
        QosResponse.parseFromJson(new ByteArrayInputStream(json.getBytes()));
    }
}