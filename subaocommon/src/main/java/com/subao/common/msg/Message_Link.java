package com.subao.common.msg;

import android.annotation.SuppressLint;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.JsonSerializable;
import com.subao.common.utils.JsonUtils;
import com.subao.common.LogTag;
import com.subao.common.Misc;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Message_Link {

    public static final String TAG = LogTag.MESSAGE;

    /**
     * 网络类型
     */
    public enum NetworkType implements MessageEnum {
        UNKNOWN_NETWORKTYPE(0),
        WIFI(1),
        MOBILE_2G(2),
        MOBILE_3G(3),
        MOBILE_4G(4),
        MOBILE_5G(5);

        private final int id;

        NetworkType(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return this.id;
        }
    }

    public static class DelayQuality implements JsonSerializable {
        public static final String NAME_DELAY_AVG = "delayAvg";
        public static final String NAME_DELAY_SD = "delaySD";
        public static final String NAME_LOSS_RATIO = "lossRatio";
        public static final String NAME_DELAY_MAX = "delayMax";
        public static final String NAME_DELAY_MIN = "delayMin";
        public static final String NAME_EX_PKT_NUM = "exPktNum";
        /**
         * 延迟均值
         */
        public final float delayAvg; //必选字段

        /**
         * 延迟标准差
         */
        public final float delaySD; //必选字段

        /**
         * 掉包率
         */
        public final float lossRatio; //必选字段

        /**
         * 延迟最大值
         */
        public final Float delayMax; //可选字段

        /**
         * 延迟最小值
         */
        public final Float delayMin; //可选字段

        /**
         * 异常包个数
         */
        public final Integer exPktNum; // 可选字段

        public DelayQuality(float delayAvg, float delaySD, float lossRatio, Float delayMax, Float delayMin, Integer exPktNum) {
            this.delayAvg = delayAvg;
            this.delaySD = delaySD;
            this.lossRatio = lossRatio;
            this.delayMax = delayMax;
            this.delayMin = delayMin;
            this.exPktNum = exPktNum;
        }

        @SuppressLint("DefaultLocale")
        private static StringBuilder appendFloat(StringBuilder sb, String name, Float value) {
            if (value != null) {
                sb.append(String.format(",\"%s\":%.2f", name, value));
            }
            return sb;
        }

        @SuppressLint("DefaultLocale")
        private static StringBuilder appendInteger(StringBuilder sb, String name, Integer value) {
            if (value != null) {
                sb.append(String.format(",\"%s\":%d", name, value));
            }
            return sb;
        }

        public static DelayQuality parseFromJson(JsonReader reader) throws IOException {
            float delayAvg = 0, delaySD = 0, lossRatio = 0;
            Float delayMax = null, delayMin = null;
            Integer exPktNum = null;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (NAME_DELAY_AVG.equals(name)) {
                    delayAvg = (float) reader.nextDouble();
                } else if (NAME_DELAY_SD.equals(name)) {
                    delaySD = (float) reader.nextDouble();
                } else if (NAME_LOSS_RATIO.equals(name)) {
                    lossRatio = (float) reader.nextDouble();
                } else if (NAME_DELAY_MAX.equals(name)) {
                    delayMax = (float) reader.nextDouble();
                } else if (NAME_DELAY_MIN.equals(name)) {
                    delayMin = (float) reader.nextDouble();
                } else if (NAME_EX_PKT_NUM.equals(name)) {
                    exPktNum = reader.nextInt();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return new DelayQuality(delayAvg, delaySD, lossRatio, delayMax, delayMin, exPktNum);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof DelayQuality)) {
                return false;
            }
            DelayQuality other = (DelayQuality) o;
            return Misc.isEquals(this.exPktNum, other.exPktNum)
                && Float.compare(this.delayAvg, other.delayAvg) == 0
                && Float.compare(this.delaySD, other.delaySD) == 0
                && Float.compare(this.lossRatio, other.lossRatio) == 0
                && Misc.isEquals(this.delayMax, other.delayMax)
                && Misc.isEquals(this.delayMin, other.delayMin);
        }

        @Override
        public String toString() {
            return serializeToJson();
        }

        @SuppressLint("DefaultLocale")
        public String serializeToJson() {
            StringBuilder sb = new StringBuilder(256);
            sb.append(String.format("{\"%s\":%.2f,\"%s\":%.2f,\"%s\":%.2f",
                NAME_DELAY_AVG, this.delayAvg,
                NAME_DELAY_SD, this.delaySD,
                NAME_LOSS_RATIO, this.lossRatio));
            appendFloat(sb, NAME_DELAY_MAX, this.delayMax);
            appendFloat(sb, NAME_DELAY_MIN, this.delayMin);
            appendInteger(sb, NAME_EX_PKT_NUM, this.exPktNum);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            JsonUtils.writeNumber(writer, NAME_DELAY_AVG, this.delayAvg);
            JsonUtils.writeNumber(writer, NAME_DELAY_SD, this.delaySD);
            JsonUtils.writeNumber(writer, NAME_LOSS_RATIO, this.lossRatio);
            JsonUtils.writeNumber(writer, NAME_DELAY_MAX, this.delayMax);
            JsonUtils.writeNumber(writer, NAME_DELAY_MIN, this.delayMin);
            JsonUtils.writeNumber(writer, NAME_EX_PKT_NUM, this.exPktNum);
            writer.endObject();
        }
    }

    public static class DelayQualityV2 implements JsonSerializable {
        public static final String NAME_DELAY_AVG = "delayAvg";
        public static final String NAME_DELAY_SD = "delaySD";
        public static final String NAME_LOSS_RATIO = "lossRatio";
        public static final String NAME_EX_PKT_RATIO_1 = "exPktRatio1";
        public static final String NAME_EX_PKT_RATIO_2 = "exPKtRatio2";
        public static final String NAME_DELAY_AVG_RAW = "delayAvgRaw";

        /**
         * 延迟均值
         */
        public final float delayAvg; //必选字段

        /**
         * 延迟标准差
         */
        public final float delaySD; //必选字段

        /**
         * 掉包率
         */
        public final float lossRatio; //必选字段

        /**
         * 异常包占比1
         */
        public final float exPktRatio1;

        public final float exPktRatio2;

        public final float delayAvgRaw;

        public DelayQualityV2(float delayAvg, float delaySD, float lossRatio, float exPktRatio1, float exPktRatio2, float delayAvgRaw) {
            this.delayAvg = delayAvg;
            this.delaySD = delaySD;
            this.lossRatio = lossRatio;
            this.exPktRatio1 = exPktRatio1;
            this.exPktRatio2 = exPktRatio2;
            this.delayAvgRaw = delayAvgRaw;
        }

        public static DelayQualityV2 parseFromJson(JsonReader reader) throws IOException {
            float delayAvg = 0f, delaySD = 0f, lossRatio = 0f,
                exPktRatio1 = 0f, exPktRatio2 = 0f,
                delayAvgRaw = 0f;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (NAME_DELAY_AVG.equals(name)) {
                    delayAvg = (float) reader.nextDouble();
                } else if (NAME_DELAY_SD.equals(name)) {
                    delaySD = (float) reader.nextDouble();
                } else if (NAME_LOSS_RATIO.equals(name)) {
                    lossRatio = (float) reader.nextDouble();
                } else if (NAME_EX_PKT_RATIO_1.equals(name)) {
                    exPktRatio1 = (float) reader.nextDouble();
                } else if (NAME_EX_PKT_RATIO_2.equals(name)) {
                    exPktRatio2 = (float) reader.nextDouble();
                } else if (NAME_DELAY_AVG_RAW.equals(name)) {
                    delayAvgRaw = (float) reader.nextDouble();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return new DelayQualityV2(delayAvg, delaySD, lossRatio, exPktRatio1, exPktRatio2, delayAvgRaw);
        }

        private static StringBuilder appendField(StringBuilder sb, NumberFormat fmt, String name, float value) {
            return sb.append('"').append(name).append('"').append(':').append(fmt.format(value));
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof DelayQualityV2)) {
                return false;
            }
            DelayQualityV2 other = (DelayQualityV2) o;
            return Float.compare(this.delayAvg, other.delayAvg) == 0
                && Float.compare(this.delaySD, other.delaySD) == 0
                && Float.compare(this.lossRatio, other.lossRatio) == 0
                && Float.compare(this.exPktRatio1, other.exPktRatio1) == 0
                && Float.compare(this.exPktRatio2, other.exPktRatio2) == 0
                && Float.compare(this.delayAvgRaw, other.delayAvgRaw) == 0;
        }

        @Override
        public String toString() {
            return serializeToJson();
        }

        public String serializeToJson() {
            StringBuilder sb = new StringBuilder(256);
            DecimalFormat fmt = new DecimalFormat("0.00");
            sb.append('{');
            appendField(sb, fmt, NAME_DELAY_AVG, this.delayAvg).append(',');
            appendField(sb, fmt, NAME_DELAY_SD, this.delaySD).append(',');
            appendField(sb, fmt, NAME_LOSS_RATIO, this.lossRatio).append(',');
            appendField(sb, fmt, NAME_EX_PKT_RATIO_1, this.exPktRatio1).append(',');
            appendField(sb, fmt, NAME_EX_PKT_RATIO_2, this.exPktRatio2).append(',');
            appendField(sb, fmt, NAME_DELAY_AVG_RAW, this.delayAvgRaw).append('}');
            return sb.toString();
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            JsonUtils.writeNumber(writer, NAME_DELAY_AVG, this.delayAvg);
            JsonUtils.writeNumber(writer, NAME_DELAY_SD, this.delaySD);
            JsonUtils.writeNumber(writer, NAME_LOSS_RATIO, this.lossRatio);
            JsonUtils.writeNumber(writer, NAME_EX_PKT_RATIO_1, this.exPktRatio1);
            JsonUtils.writeNumber(writer, NAME_EX_PKT_RATIO_2, this.exPktRatio2);
            JsonUtils.writeNumber(writer, NAME_DELAY_AVG_RAW, this.delayAvgRaw);
            writer.endObject();
        }
    }

    public static class Network implements JsonSerializable {

        private static final String NAME_DETAIL = "detail";
        private static final String NAME_TYPE = "type";

        public final NetworkType type; //必选字段
        public final String detail; //可选字段 如果是WiFI则填SSID，如果是移动数据则上报基站信息（或其它能获取到的）

        public Network(NetworkType type, String detail) {
            this.type = type;
            this.detail = detail;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof Network)) {
                return false;
            }
            Network other = (Network) o;
            return this.detail.equals(other.detail)
                && this.type.equals(other.type);
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            MessageJsonUtils.serializeEnum(writer, NAME_TYPE, type);
            JsonUtils.writeString(writer, NAME_DETAIL, detail);
            writer.endObject();
        }
    }

    private abstract static class AccelInfoBase implements JsonSerializable {
        private static final String NAME_SUPPORT = "support";
        private static final String NAME_OPEN = "open";
        private static final String NAME_DURATION = "duration";

        public final boolean support;
        public final boolean open;
        public final Integer duration;

        protected AccelInfoBase(boolean support, boolean open, Integer duration) {
            this.support = support;
            this.open = open;
            this.duration = duration;
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name(NAME_SUPPORT).value(this.support);
            writer.name(NAME_OPEN).value(this.open);
            JsonUtils.writeNumber(writer, NAME_DURATION, this.duration);
            serializeOtherFields(writer);
            writer.endObject();
        }

        @Override
        public String toString() {
            try {
                return JsonUtils.serializeToString(this);
            } catch (IOException e) {
                return super.toString();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (!(o instanceof AccelInfoBase)) {
                return false;
            }
            AccelInfoBase other = (AccelInfoBase) o;
            return this.support == other.support
                && this.open == other.open
                && Misc.isEquals(this.duration, other.duration)
                && otherFieldsEqual(other);
        }

        protected abstract boolean otherFieldsEqual(AccelInfoBase other);

        protected abstract void serializeOtherFields(JsonWriter writer) throws IOException;
    }

    /**
     * Qos相关信息
     */
    static class QosAccelInfo extends AccelInfoBase {

        public final String isp;

        public QosAccelInfo(boolean support, boolean open, Integer duration, String isp) {
            super(support, open, duration);
            this.isp = isp;
        }

        @Override
        protected boolean otherFieldsEqual(AccelInfoBase o) {
            if (!(o instanceof QosAccelInfo)) {
                return false;
            }
            QosAccelInfo other = (QosAccelInfo) o;
            return Misc.isEquals(this.isp, other.isp);
        }

        @Override
        protected void serializeOtherFields(JsonWriter writer) throws IOException {
            JsonUtils.writeString(writer, "isp", this.isp);
        }

    }

    /**
     * WiFi加速相关信息
     */
    static class WiFiAccelInfo extends AccelInfoBase {

        private final Integer traffic;

        public WiFiAccelInfo(boolean support, boolean open, Integer duration, Integer traffic) {
            super(support, open, duration);
            this.traffic = traffic;
        }

        @Override
        protected boolean otherFieldsEqual(AccelInfoBase other) {
            if (!(other instanceof WiFiAccelInfo)) {
                return false;
            }
            return Misc.isEquals(((WiFiAccelInfo) other).traffic, this.traffic);
        }

        @Override
        protected void serializeOtherFields(JsonWriter writer) throws IOException {
            JsonUtils.writeNumber(writer, "traffic", this.traffic);
        }
    }

    static class AccelInfo implements JsonSerializable {
        final QosAccelInfo qosInfo;
        final WiFiAccelInfo multipathInfo;
        final Integer accelMethod;

        public AccelInfo(QosAccelInfo qosInfo, WiFiAccelInfo multipathInfo, Integer accelMethod) {
            this.qosInfo = qosInfo;
            this.multipathInfo = multipathInfo;
            this.accelMethod = accelMethod;
        }

        @Override
        public void serialize(JsonWriter writer) throws IOException {
            writer.beginObject();
            JsonUtils.writeSerializable(writer, "qosInfo", this.qosInfo);
            JsonUtils.writeSerializable(writer, "multipathInfo", this.multipathInfo);
            JsonUtils.writeNumber(writer, "method", this.accelMethod);
            writer.endObject();
        }
    }
}
