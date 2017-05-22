package com.subao.common.qos;

import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;
import com.subao.common.data.AppType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class QosSetupRequestTest extends RoboBase {

    public static final AppType APP_TYPE = AppType.ANDROID_APP;
    public static final String CHANNEL = "g_official";
    public static final int TIME_LENGTH = 1000;
    public static final String VERSION_NUM = "2.2.4";
    public static final String USER_ID = "userId";
    public static final String PRIVATE_IP = "192.168.1.241";
    public static final int SRC_PORT = 8765;
    public static final String PUBLIC_IP = "192.168.1.222";
    public static final String IMSI = "imsi";
    public static final String MSISDN = "msisdn";
    public static final String SRC_IP = "192.168.1.241";
    public static final String DST_IP = "192.168.1.243";
    public static final int DST_PORT = 245;
    public static final String PROTOCOL = "UDP";

    private static QosSetupRequest createQosSetupRequest() {
        QosTerminalInfo terminalInfo = createQosTerminalInfo();
        QosMediaInfo mediaInfo = createQosMediaInfo();
        return new QosSetupRequest(
            APP_TYPE,
            CHANNEL, VERSION_NUM, USER_ID,
            TIME_LENGTH,
            terminalInfo,
            mediaInfo);
    }

    private static QosMediaInfo createQosMediaInfo() {
        return new QosMediaInfo(SRC_IP, SRC_PORT, DST_IP, DST_PORT, PROTOCOL);
    }

    private static QosTerminalInfo createQosTerminalInfo() {
        return new QosTerminalInfo(PRIVATE_IP, SRC_PORT, PUBLIC_IP, IMSI, MSISDN);
    }

    @Test
    public void testQosSetupRequest() {
        QosSetupRequest request = createQosSetupRequest();
        assertEquals(APP_TYPE, request.appType);
        assertEquals(CHANNEL, request.channel);
        assertEquals(VERSION_NUM, request.versionNum);
        assertEquals(USER_ID, request.subaoId);
        assertEquals(TIME_LENGTH, request.timeLength);
        assertNull(request.getOperator());
        assertEquals(createQosTerminalInfo(), request.terminalInfo);
        assertEquals(createQosMediaInfo(), request.getMediaInfo(0));
        assertEquals(1, request.getMediaInfoCount());
        //
        request = new QosSetupRequest(
            APP_TYPE,
            CHANNEL, VERSION_NUM, USER_ID,
            TIME_LENGTH,
            createQosTerminalInfo(), null);
        assertEquals(0, request.getMediaInfoCount());
    }

    @Test
    public void setSecurityToken() {
        QosSetupRequest request = createQosSetupRequest();
        assertNull(request.terminalInfo.getSecurityToken());
        String token = "The Security Token";
        request.setSecurityToken(token);
        assertEquals(token, request.terminalInfo.getSecurityToken());
    }

    @Test
    public void setPhoneNumber() {
        QosSetupRequest request = createQosSetupRequest();
        assertNull(request.terminalInfo.getSecurityToken());
        String phoneNumber = "1234567890";
        request.setPhoneNumber(phoneNumber);
        assertEquals(phoneNumber, request.terminalInfo.getMSISDN());
    }

    @Test
    public void setPrivateIp() {
        QosSetupRequest request = createQosSetupRequest();
        assertEquals(PRIVATE_IP, request.terminalInfo.getPrivateIp());
        String newIP = PRIVATE_IP + "_new";
        request.setPrivateIp(newIP);
        assertEquals(newIP, request.terminalInfo.getPrivateIp());
    }

    @Test
    public void setOperator() {
        QosSetupRequest request = createQosSetupRequest();
        assertNull(request.getOperator());
        String operator = "32.11";
        request.setOperator(operator);
        assertEquals(operator, request.getOperator());
    }

    @Test
    public void serialize() throws JSONException {
        QosSetupRequest request = createQosSetupRequest();
        String phoneNumber = "123456";
        request.setPhoneNumber(phoneNumber);
        String privateIp = "the private ip";
        request.setPrivateIp(privateIp);
        String operator = "32.11";
        request.setOperator(operator);
        //
        StringWriter stringWriter = new StringWriter(1024);
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            request.serialize(jsonWriter);
            jsonWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Misc.close(jsonWriter);
        }
        //
        JSONObject jsonObject = new JSONObject(stringWriter.toString());
        assertEquals(APP_TYPE.getId(), jsonObject.getInt("appType"));
        assertEquals(CHANNEL, jsonObject.getString("channel"));
        assertEquals(VERSION_NUM, jsonObject.getString("versionNum"));
        assertEquals(USER_ID, jsonObject.getString("userId"));
        assertEquals(TIME_LENGTH, jsonObject.getInt("timeLength"));
        assertEquals(operator, jsonObject.getString("operator"));
        //
        JSONObject jsonTerminalInfo = jsonObject.getJSONObject("terminalInfo");
        QosTerminalInfo qosTerminalInfo = QosTerminalInfoTest.deserialize(jsonTerminalInfo);
        assertEquals(qosTerminalInfo, request.terminalInfo);
        //
        JSONArray jsonQosMediaInfoArray = jsonObject.getJSONArray("mediaInfo");
        assertEquals(1, jsonQosMediaInfoArray.length());
        JSONObject jsonQosMediaInfo = jsonQosMediaInfoArray.getJSONObject(0);
        QosMediaInfo qosMediaInfo = QosMediaInfoTest.deserialize(jsonQosMediaInfo);
        assertEquals(qosMediaInfo, request.getMediaInfo(0));
    }

}