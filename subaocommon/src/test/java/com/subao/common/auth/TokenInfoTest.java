package com.subao.common.auth;

import com.subao.common.RoboBase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by hujd on 16-7-19.
 */
public class TokenInfoTest extends RoboBase {

    @Test
    public void testParseJson() {
        String token = "Hello";
        int expire = 123;
        String json = String.format("{\"access_token\": \"%s\", \"salt\":null, \"expires_in\": %d}", token, expire);
        TokenInfo tokenInfo = TokenInfo.createFromJson(json);
        assertEquals(token, tokenInfo.token);
        assertEquals(expire, tokenInfo.expires_in);
        assertNull(TokenInfo.createFromJson("{}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseJsonNull() {
        TokenInfo.createFromJson(null);
    }

    @Test
    public void testParseException() {
        assertNull(TokenInfo.createFromJson("{\"access_token\": null}"));
        assertNull(TokenInfo.createFromJson("{\"a\": {[}"));
    }


}