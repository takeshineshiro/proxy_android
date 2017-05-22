package com.subao.common.auth;

import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.Misc;
import com.subao.common.RoboBase;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by hujd on 16-7-19.
 */
public class JWTTokenRespTest extends RoboBase {

    private static final String token = "token";
    private static final int expiresIn = 1234;
    private static final String shortId = "shortId";
    private static final int userStatus = 2;
    private static final String time = "time";
    private static final int totalAccelDays = 7;
    private static final long CURRENT_TIME = 135234283L;

    public static JWTTokenResp createJWTTokenResp() {
        return new JWTTokenResp(token, expiresIn, shortId, userStatus, time, totalAccelDays, CURRENT_TIME);
    }

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testConstructor() {
        JWTTokenResp resp = createJWTTokenResp();
        assertEquals(token, resp.accelToken);
        assertEquals(expiresIn, resp.expiresIn);
        assertEquals(shortId, resp.shortId);
        assertEquals(userStatus, resp.userStatus);
        assertEquals(time, resp.accelExpiredTime);
        assertEquals(totalAccelDays, resp.totalAccelDays);
        assertEquals(CURRENT_TIME, resp.currentTime);
    }

    @Test
    public void testSame() {
        JWTTokenResp r1 = createJWTTokenResp();
        assertEquals(r1, r1);
        assertTrue(r1.same(r1, 0));
        assertFalse(r1.equals(null));
        assertFalse(r1.same(null, 0));
    }

    @Test
    public void testParseJson() throws IOException {
        String jsonStr = "{\"accelToken\": \"ebd0811a554fd200\",\"expiresIn\": 1800," +
            "\"shortId\": \"811a554fd200\",\"userStatus\": 1,\"accelExpiredTime\": \"2016-04-20 20:28:00\"," +
            "\"currentTime\":13579}";
        JWTTokenResp tokenResp = JWTTokenResp.createFromJson(new ByteArrayInputStream(jsonStr.getBytes()));
        assertNotNull(tokenResp);
        assertEquals(13579L, tokenResp.currentTime);
    }

    @Test(expected = RuntimeException.class)
    public void testParseJsonNull1() throws IOException {
        JWTTokenResp.createFromJson((InputStream) null);
    }

    @Test(expected = RuntimeException.class)
    public void testParseJsonNull2() throws IOException {
        JWTTokenResp.createFromJson((JsonReader) null);
    }

    @Test(expected = IOException.class)
    public void testParseJsonEmptyString() throws IOException {
        JWTTokenResp.createFromJson(new ByteArrayInputStream("{ ".getBytes()));
    }

    @Test(expected = IOException.class)
    public void testParseJsonError() throws IOException {
        JWTTokenResp.createFromJson(new ByteArrayInputStream("{\"tokne\": 123}".getBytes()));
    }

    @Test(expected = IOException.class)
    public void testParseJsonException() throws IOException {
        JWTTokenResp.createFromJson(new ByteArrayInputStream("{\"accelToken\": }".getBytes()));
    }

    @Test(expected = IOException.class)
    public void testParseJsonException2() throws IOException {
        JWTTokenResp.createFromJson(new StringInputStream("{\"accelToken\":[]}"));
    }

    @Test
    public void testSerialize() throws IOException {
        JWTTokenResp resp = createJWTTokenResp();
        //
        StringWriter sw = new StringWriter(1024);
        JsonWriter writer = new JsonWriter(sw);
        try {
            resp.serialize(writer);
        } finally {
            Misc.close(writer);
        }
        JWTTokenResp resp2 = JWTTokenResp.createFromJson(new ByteArrayInputStream(sw.toString().getBytes()));
        //
        assertFalse(resp.equals(null));
        assertFalse(resp.equals(new Object()));
        assertEquals(resp2, resp);
        assertEquals(resp, resp);
    }

    @Test
    public void testIncExpire() {
        JWTTokenResp resp1 = createJWTTokenResp();
        long now = System.currentTimeMillis();
        JWTTokenResp resp2 = resp1.cloneWithNewExpiredIn(resp1.expiresIn * 1000 + now);
        assertFalse(resp1.equals(resp2));
        assertEquals(resp1.expiresIn * 1000 + now, resp2.expiresIn);
        JWTTokenResp resp3 = resp2.cloneWithNewExpiredIn((resp2.expiresIn - now) / 1000);
        assertEquals(resp1, resp3);
    }

}