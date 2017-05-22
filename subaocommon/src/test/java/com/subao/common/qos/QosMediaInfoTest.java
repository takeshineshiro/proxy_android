package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.RoboBase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class QosMediaInfoTest extends RoboBase {

    public static final String SRC_IP = "192.168.1.241";
    public static final int SRC_PORT = 8467;
    public static final String DST_IP = "192.168.1.243";
    public static final int DST_PORT = 245;
    public static final String PROTOCOL = "UDP";

    private static QosMediaInfo createQosMediaInfo() {
        return new QosMediaInfo(SRC_IP, SRC_PORT, DST_IP, DST_PORT, PROTOCOL);
    }

    public static QosMediaInfo deserialize(JSONObject jsonQosMediaInfo) throws JSONException {
        String srcIp = null, dstIp = null, protocol = null;
        if (jsonQosMediaInfo.has("srcIp")) {
            srcIp = jsonQosMediaInfo.getString("srcIp");
        }
        if (jsonQosMediaInfo.has("dstIp")) {
            dstIp = jsonQosMediaInfo.getString("dstIp");
        }
        if (jsonQosMediaInfo.has("protocol")) {
            protocol = jsonQosMediaInfo.getString("protocol");
        }
        int srcPort = jsonQosMediaInfo.getInt("srcPort");
        int dstPort = jsonQosMediaInfo.getInt("dstPort");
        return new QosMediaInfo(srcIp, srcPort, dstIp, dstPort, protocol);
    }

    @Test
    public void testQosMediaInfo() {
        QosMediaInfo mediaInfo = createQosMediaInfo();
        assertEquals(SRC_IP, mediaInfo.srcIp);
        assertEquals(SRC_PORT, mediaInfo.srcPort);
        assertEquals(DST_IP, mediaInfo.dstIp);
        assertEquals(DST_PORT, mediaInfo.dstPort);
        assertEquals(PROTOCOL, mediaInfo.protocol);
    }

    @Test
    public void serialize() throws JSONException {
        QosMediaInfo mediaInfo = createQosMediaInfo();
        StringWriter stringWriter = new StringWriter(1024);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            mediaInfo.serialize(jsonWriter);
            jsonWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //
        JSONObject jsonObject = new JSONObject(stringWriter.toString());
        QosMediaInfo qosMediaInfo = deserialize(jsonObject);
        assertEquals(qosMediaInfo, mediaInfo);
    }

    @Test
    public void testEquals() {
        QosMediaInfo qosMediaInfo = createQosMediaInfo();
        assertEquals(qosMediaInfo, qosMediaInfo);
        assertFalse(qosMediaInfo.equals(null));
        assertFalse(qosMediaInfo.equals(this));
        assertEquals(qosMediaInfo, createQosMediaInfo());
    }

}