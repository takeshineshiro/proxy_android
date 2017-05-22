package com.subao.common.auth;

import com.subao.common.RoboBase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by hujd on 16-7-19.
 */
public class UserAccelStatusTest extends RoboBase {

    @Test
    public void testParseJson(){
        String shortId = "ebd0811a554fd200";
        int status = 1;
        String expiredTime = "2017-05-27 12:00:00";
        String jsonStr = String.format(
            "{\"shortId\": \"%s\", \"salt\":5, \"status\": %d, \"expiredTime\": \"%s\"}",
            shortId, status, expiredTime);
        UserAccelStatus uas = UserAccelStatus.createFromJson(jsonStr);
        assertEquals(shortId, uas.shortId);
        assertEquals(status, uas.status);
        assertEquals(expiredTime, uas.expiredTime);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseJsonNull(){
        UserAccelStatus.createFromJson(null);
    }

    @Test
    public void testParseJsonError(){
        assertNull(UserAccelStatus.createFromJson("{}"));
        assertNull(UserAccelStatus.createFromJson("{\"a\": {[}")); // IOException
        assertNull(UserAccelStatus.createFromJson("{\"shortId\":null}")); // RuntimeException
    }
}