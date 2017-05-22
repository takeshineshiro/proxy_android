package com.subao.common.auth;

import android.util.JsonReader;

import com.subao.common.Misc;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by hujd on 16-7-8.
 */
public class UserAccelStatus {

    private static final String KEY_SHORT_ID = "shortId";
    private static final String KEY_STATUS = "status";
    private static final String KEY_EXPIRED_TIME = "expiredTime";

    public final String shortId;
    public final int status;
    public final String expiredTime;

    public UserAccelStatus(String userId, int status, String expiredTime) {
        this.shortId = userId;
        this.status = status;
        this.expiredTime = expiredTime;
    }

    public static UserAccelStatus createFromJson(String jsonStr) {
        if (jsonStr == null) {
            throw new IllegalArgumentException("parameters error");
        }
        String userId = null;
        String expiredTime = null;
        int status = -1;
        JsonReader reader = new JsonReader(new StringReader(jsonStr));
        reader.setLenient(true);
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (KEY_SHORT_ID.equals(name)) {
                    userId = reader.nextString();
                } else if (KEY_STATUS.equals(name)) {
                    status = reader.nextInt();
                } else if (KEY_EXPIRED_TIME.equals(name)) {
                    expiredTime = reader.nextString();
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

        if (userId != null && expiredTime != null) {
            return new UserAccelStatus(userId, status, expiredTime);
        } else {
            return null;
        }
    }
}
