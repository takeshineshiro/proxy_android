package com.subao.common.auth;

import com.subao.common.RoboBase;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Created by hujd on 16-7-19.
 */
public class JWTTokenReqTest extends RoboBase {

    private JWTTokenReq jwtTokenReq;
//    private static final String TAG = JWTTokenReqTest.class.toString();;

    @Before
    public void setup() {
        ShadowLog.stream = System.out;
        jwtTokenReq = new JWTTokenReq("userid", "token", "appid");
    }

    @Test
    public void testSerialize() throws Exception {
        byte[] bytes = AuthService.serializeToBytes(jwtTokenReq);
        assertNotNull(bytes);
        JSONObject obj = new JSONObject(new String(bytes));
        assertEquals(jwtTokenReq.userId, obj.getString("userId"));
        assertEquals(jwtTokenReq.token, obj.getString("token"));
        assertEquals(jwtTokenReq.appId, obj.getString("appId"));
    }
}