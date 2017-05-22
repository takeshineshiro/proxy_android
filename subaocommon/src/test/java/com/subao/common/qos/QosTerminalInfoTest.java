package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QosTerminalInfoTest extends RoboBase {

    public static QosTerminalInfo deserialize(JSONObject jsonObject) throws JSONException {
        String privateIp = null;
        String publicIp = null;
        String imsi = null;
        String msisdn = null;
        String securityToken = null;
        if (jsonObject.has("privateIp")) {
            privateIp = jsonObject.getString("privateIp");
        }
        if (jsonObject.has("privateIp")) {
            privateIp = jsonObject.getString("privateIp");
        }
        int srcPort = jsonObject.getInt("srcPort");
        if (jsonObject.has("publicIp")) {
            publicIp = jsonObject.getString("publicIp");
        }
        if (jsonObject.has("imsi")) {
            imsi = jsonObject.getString("imsi");
        }
        if (jsonObject.has("msisdn")) {
            msisdn = jsonObject.getString("msisdn");
        }
        if (jsonObject.has("securityToken")) {
            securityToken = jsonObject.getString("securityToken");
        }
        QosTerminalInfo qosTerminalInfo = new QosTerminalInfo(privateIp, srcPort, publicIp, imsi, msisdn);
        qosTerminalInfo.setSecurityToken(securityToken);
        return qosTerminalInfo;
    }

    @Test
    public void testQosTerminalInfo() throws IOException, JSONException {
        String privateIp = "192.168.1.241";
        int srcPort = 8765;
        String publicIp = "192.168.1.222";
        String imsi = "imsi";
        String msisdn = "msisdn";
        String token = "token";
        //
        QosTerminalInfo terminalInfo = new QosTerminalInfo(privateIp, srcPort, publicIp, imsi, msisdn);
        assertNull(terminalInfo.getSecurityToken());
        terminalInfo.setSecurityToken(token);
        assertEquals(token, terminalInfo.getSecurityToken());
        assertEquals(msisdn, terminalInfo.getMSISDN());
        terminalInfo.setMSISDN("temp");
        assertEquals("temp", terminalInfo.getMSISDN());
        terminalInfo.setMSISDN(msisdn);
        assertEquals(privateIp, terminalInfo.getPrivateIp());
        terminalInfo.setPrivateIp("xxx");
        assertEquals("xxx", terminalInfo.getPrivateIp());
        terminalInfo.setPrivateIp(privateIp);
        //
        StringWriter stringWriter = new StringWriter(1024);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            terminalInfo.serialize(jsonWriter);
        } finally {
            Misc.close(jsonWriter);
        }
        QosTerminalInfo info2 = deserialize(new JSONObject(stringWriter.toString()));
        //
        assertEquals(terminalInfo, terminalInfo);
        assertEquals(terminalInfo, info2);
        assertFalse(terminalInfo.equals(null));
        assertFalse(terminalInfo.equals(this));
        assertTrue(terminalInfo.equals(new QosTerminalInfo(privateIp, srcPort, publicIp, imsi, msisdn, token)));
        assertFalse(terminalInfo.equals(new QosTerminalInfo(privateIp + "x", srcPort, publicIp, imsi, msisdn, token)));
        assertFalse(terminalInfo.equals(new QosTerminalInfo(privateIp, srcPort + 1, publicIp, imsi, msisdn, token)));
        assertFalse(terminalInfo.equals(new QosTerminalInfo(privateIp, srcPort, publicIp + "x", imsi, msisdn, token)));
        assertFalse(terminalInfo.equals(new QosTerminalInfo(privateIp, srcPort, publicIp, imsi + "x", msisdn, token)));
        assertFalse(terminalInfo.equals(new QosTerminalInfo(privateIp, srcPort, publicIp, imsi, msisdn + "x", token)));
        assertFalse(terminalInfo.equals(new QosTerminalInfo(privateIp, srcPort, publicIp, imsi, msisdn, token + "x")));
        //
    }
}