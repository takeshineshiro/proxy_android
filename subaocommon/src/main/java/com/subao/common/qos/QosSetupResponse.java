package com.subao.common.qos;

import android.util.JsonReader;

import com.subao.common.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

class QosSetupResponse {

    /**
     * 0：代表成功，非0，代表失败，具体原因值待规划
     * <p>
     * 是否必选：是
     * </p>
     */
    public final int resultCode;

    /**
     * 成功时不需携带
     * <p>
     * 是否必选：否
     * </p>
     */
    public final String errorInfo;

    /**
     * 临时分配的会话ID，用于区分业务
     * <p>
     * 是否必选：是
     * </p>
     */

    public final String sessionId;
    /**
     * 提速网关分配的提速ID
     * <p>
     * 是否必选：是
     * </p>
     */

    public final String speedingId;

    /**
     * 运营商，包含省局信息
     * <p>
     * 是否必选：是
     * </p>
     */
    public final String operator;

    /**
     * 运营商号，主要指区号
     * <p>
     * 是否必选：否
     * </p>
     */
    public final String operatorCode;

    /**
     * 第三方厂商
     * <p>
     * 是否必选：是
     * </p>
     */
    public final String vendor;

    private QosSetupResponse(int resultCode, String errorInfo, String sessionId, String speedingId, String operator, String operatorCode, String vendor) {
        this.resultCode = resultCode;
        this.errorInfo = errorInfo;
        this.sessionId = sessionId;
        this.speedingId = speedingId;
        this.operator = operator;
        this.operatorCode = operatorCode;
        this.vendor = vendor;
    }

    public static QosSetupResponse parseFromJson(byte[] data) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(data)));
        return parseFromJson(reader);
    }

    public static QosSetupResponse parseFromJson(JsonReader reader) throws IOException {
        int resultCode = 0;
        String errorInfo = null;
        String sessionId = null;
        String speedingId = null;
        String operator = null;
        String operatorCode = null;
        String vendor = null;
        //
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("resultCode".equals(name)) {
                resultCode = reader.nextInt();
            } else if ("errorInfo".equals(name)) {
                errorInfo = JsonUtils.readNextString(reader);
            } else if ("sessionId".equals(name)) {
                sessionId = JsonUtils.readNextString(reader);
            } else if ("speedingId".equals(name)) {
                speedingId = JsonUtils.readNextString(reader);
            } else if ("operator".equals(name)) {
                operator = JsonUtils.readNextString(reader);
            } else if ("operatorCode".equals(name)) {
                operatorCode = JsonUtils.readNextString(reader);
            } else if ("vendor".equals(name)) {
                vendor = JsonUtils.readNextString(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new QosSetupResponse(resultCode, errorInfo, sessionId, speedingId, operator, operatorCode, vendor);
    }

}
