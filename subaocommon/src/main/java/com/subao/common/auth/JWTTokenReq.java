package com.subao.common.auth;

import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;

import java.io.IOException;

/**
 * Created by hujd on 16-7-6.
 */
public class JWTTokenReq implements JsonSerializable{

    /**
     * shortId : ”811a554fd200”
     * token : ”a8a2-3b37ce01918b”
     */

    public final String userId;
    public final String token;
    public final String appId;

    public JWTTokenReq(String userId, String token, String appId) {
        this.userId = userId;
        this.token = token;
        this.appId = appId;
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        JsonUtils.writeString(writer, "userId", this.userId);
        JsonUtils.writeString(writer, "token", this.token);
        JsonUtils.writeString(writer, "appId", this.appId);
        writer.endObject();
    }
}
