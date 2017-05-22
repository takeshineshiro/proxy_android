package com.subao.common.auth;

import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by hujd on 16-7-6.
 */
public class JWTTokenResp implements JsonSerializable {

    public final String accelToken;
    public final long expiresIn;
    public final String shortId;
    public final int userStatus;
    public final String accelExpiredTime;
    public final int totalAccelDays;

    /**
     * 服务器当前时间
     */
    public final long currentTime;

    private static final String KEY_ACCESS_TOKEN = "accelToken";
    private static final String KEY_EXPIRES_IN = "expiresIn";
    private static final String KEY_SHORT_ID = "shortId";
    private static final String KEY_USER_STATUS = "userStatus";
    private static final String KEY_ACCEL_EXPIRED_TIME = "accelExpiredTime";
    private static final String KEY_TOTAL_ACCEL_DAYS = "totalAccelDays";
    private static final String KEY_CURRENT_TIME = "currentTime";

    public JWTTokenResp(String accelToken, long expiresIn, String shortId,
                        int userStatus, String accelExpiredTime, int totalAccelDays,
                        long currentTime
    ) {
        this.accelToken = accelToken;
        this.expiresIn = expiresIn;
        this.shortId = shortId;
        this.userStatus = userStatus;
        this.accelExpiredTime = accelExpiredTime;
        this.totalAccelDays = totalAccelDays;
        this.currentTime = currentTime;
    }

    public boolean same(JWTTokenResp other, long diffExpiresIn) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        return this.totalAccelDays == other.totalAccelDays
            && this.userStatus == other.userStatus
            && this.currentTime == other.currentTime
            && Math.abs(this.expiresIn - other.expiresIn) <= diffExpiresIn
            && Misc.isEquals(this.accelToken, other.accelToken)
            && Misc.isEquals(this.shortId, other.shortId)
            && Misc.isEquals(this.accelExpiredTime, other.accelExpiredTime);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof JWTTokenResp)) {
            return false;
        }
        JWTTokenResp other = (JWTTokenResp) o;
        return this.expiresIn == other.expiresIn
            && this.totalAccelDays == other.totalAccelDays
            && this.userStatus == other.userStatus
            && this.currentTime == other.currentTime
            && Misc.isEquals(this.accelToken, other.accelToken)
            && Misc.isEquals(this.shortId, other.shortId)
            && Misc.isEquals(this.accelExpiredTime, other.accelExpiredTime);
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        JsonUtils.writeString(writer, KEY_ACCESS_TOKEN, accelToken);
        writer.name(KEY_EXPIRES_IN).value(expiresIn);
        JsonUtils.writeString(writer, KEY_SHORT_ID, shortId);
        writer.name(KEY_USER_STATUS).value(userStatus);
        JsonUtils.writeString(writer, KEY_ACCEL_EXPIRED_TIME, accelExpiredTime);
        writer.name(KEY_TOTAL_ACCEL_DAYS).value(totalAccelDays);
        writer.name(KEY_CURRENT_TIME).value(currentTime);
        writer.endObject();
    }

    public static JWTTokenResp createFromJson(InputStream input) throws IOException {
        if (input == null) {
            throw new NullPointerException();
        }
        JWTTokenResp result;
        JsonReader reader = new JsonReader(new InputStreamReader(input));
        try {
            result = createFromJson(reader);
        } finally {
            Misc.close(reader);
        }
        return result;
    }

    public static JWTTokenResp createFromJson(JsonReader reader) throws IOException {
        if (reader == null) {
            throw new NullPointerException();
        }
        String accessToken = null, shortId = null, accelExpiredTime = null;
        long expires = 0;
        int userStatus = 0, totalAccelDays = 0;
        long currentTime = 0;
        try {
            reader.setLenient(true);
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (KEY_ACCESS_TOKEN.equals(name)) {
                    accessToken = reader.nextString();
                } else if (KEY_EXPIRES_IN.equals(name)) {
                    expires = reader.nextLong();
                } else if (KEY_SHORT_ID.equals(name)) {
                    shortId = reader.nextString();
                } else if (KEY_USER_STATUS.equals(name)) {
                    userStatus = reader.nextInt();
                } else if (KEY_ACCEL_EXPIRED_TIME.equals(name)) {
                    accelExpiredTime = reader.nextString();
                } else if (KEY_TOTAL_ACCEL_DAYS.equals(name)) {
                    totalAccelDays = reader.nextInt();
                } else if (KEY_CURRENT_TIME.equals(name)) {
                    currentTime = reader.nextLong();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage());
        }
        if (accessToken == null) {
            throw new IOException("Create fail");
        }
        return new JWTTokenResp(accessToken, expires, shortId, userStatus, accelExpiredTime, totalAccelDays, currentTime);
    }

    public JWTTokenResp cloneWithNewExpiredIn(long newExpiredIn) {
        return new JWTTokenResp(
            this.accelToken, newExpiredIn,
            this.shortId, this.userStatus,
            this.accelExpiredTime, this.totalAccelDays,
            this.currentTime
        );
    }
}
