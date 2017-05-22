package com.subao.common.auth;

import android.util.JsonReader;

import com.subao.common.Misc;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by hujd on 16-7-1.
 */
public class TokenInfo {

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_EXPIRES_IN = "expires_in";
    /**
     * Token
     */
    public final String token;
    /**
     * 过期时间
     */
    public final int expires_in;

    public TokenInfo(String token, int expires_in) {
        this.token = token;
        this.expires_in = expires_in;
    }

    public static TokenInfo createFromJson(String jsonStr) {
        if (jsonStr == null) {
            throw new IllegalArgumentException("parameters error");
        }
        String accessToken = null;
        int expires = 0;
        JsonReader reader = new JsonReader(new StringReader(jsonStr));
        reader.setLenient(true);
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (KEY_ACCESS_TOKEN.equals(name)) {
                    accessToken = reader.nextString();
                } else if (KEY_EXPIRES_IN.equals(name)) {
                    expires = reader.nextInt();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (RuntimeException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            Misc.close(reader);
        }

        if (accessToken != null) {
            return new TokenInfo(accessToken, expires);
        } else {
            return null;
        }
    }
}
