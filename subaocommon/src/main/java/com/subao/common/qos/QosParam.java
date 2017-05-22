package com.subao.common.qos;

/**
 * QosParam
 * <p>Created by YinHaiBo on 2017/2/19.</p>
 */
public class QosParam {

    public static final int DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN = 0;
    public static final int DEFAULT_ACCEL_TIME = 900;
    public static final int DEFAULT_THRESHOLD_DROP_RATE_PERCENT = 0;
    public static final int DEFAULT_THRESHOLD_SD_PERCENT = 0;

    public static final QosParam DEFAULT = createDefaultQosParam(Provider.DEFAULT);

    public final Provider provider;
    /**
     * 决定是否开启Qos加速的对比测速差值门限<br />
     */
    public final int deltaThresholdForQosOpen;
    /**
     * 单次Qos提速时长，单位秒<br />
     */
    public final int accelTime;


// SDK v2.0废弃
//    /**
//     * 当测速值（代理或透传的测速均值）大于此阈值时才开启Qos<br />
//     * <i>对应 {@link com.subao.common.data.Defines.VPNJniKey#KEY_ENABLE_QOS_AVERAGE}</i>
//     */
//    public final int thresholdForQosOpen;

    /**
     * 丢包率阈值（百分比）
     */
    public final int thresholdDropPercent;

    /**
     * 标准差阈值（百分比）
     */
    public final int thresholdSDPercent;

    /**
     * 构造QosParam对象
     *
     * @param deltaThresholdForQosOpen 决定是否开启Qos加速的对比测速差值门限
     * @param accelTime                单次Qos提速时长，单位秒
     * @param provider                 {@link Provider} Qos供应商
     * @param thresholdDropPercent     丢包率阈值（百分比）
     * @param thresholdSDPercent       标准差阈值（百分比）
     */
    public QosParam(int deltaThresholdForQosOpen, int accelTime, Provider provider, int thresholdDropPercent, int thresholdSDPercent) {
        this.deltaThresholdForQosOpen = deltaThresholdForQosOpen;
        this.accelTime = accelTime;
        this.provider = provider;
        this.thresholdDropPercent = thresholdDropPercent;
        this.thresholdSDPercent = thresholdSDPercent;
    }

    public static QosParam createDefaultQosParam(Provider provider) {
        return new QosParam(
            DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN,
            DEFAULT_ACCEL_TIME,
            provider,
            DEFAULT_THRESHOLD_DROP_RATE_PERCENT,
            DEFAULT_THRESHOLD_SD_PERCENT);
    }

    public static QosParam deserialize(String s) {
        Integer[] integers = parseIntegerList(s);
        if (integers == null) {
            return QosParam.DEFAULT;
        }
        return new QosParam(
            transFromIntegerArray(integers, 0, QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN),
            transFromIntegerArray(integers, 1, QosParam.DEFAULT_ACCEL_TIME),
            Provider.fromId(transFromIntegerArray(integers, 3, Provider.DEFAULT.id)),
            transFromIntegerArray(integers, 4, QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT),
            transFromIntegerArray(integers, 5, QosParam.DEFAULT_THRESHOLD_SD_PERCENT)
        );
    }

    private static int transFromIntegerArray(Integer[] integers, int idx, int defaultValue) {
        if (integers == null || integers.length <= idx) {
            return defaultValue;
        }
        Integer i = integers[idx];
        return (i == null) ? defaultValue : i;
    }

    static Integer[] parseIntegerList(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        String[] fields = s.split(",");
        Integer[] integers = new Integer[fields.length];
        for (int i = 0, len = fields.length; i < len; ++i) {
            Integer value;
            try {
                value = Integer.parseInt(fields[i].trim());
            } catch (NumberFormatException e) {
                value = null;
            }
            integers[i] = value;
        }
        return integers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof QosParam)) {
            return false;
        }
        QosParam other = (QosParam) o;
        return this.deltaThresholdForQosOpen == other.deltaThresholdForQosOpen
            && this.accelTime == other.accelTime
            && this.provider == other.provider
            && this.thresholdDropPercent == other.thresholdDropPercent
            && this.thresholdSDPercent == other.thresholdSDPercent;
    }

    @Override
    public String toString() {
        return serialize();
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    public String serialize() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(deltaThresholdForQosOpen);    // 对比测速延迟差值大于多少才进行Qos提速
        sb.append(',').append(accelTime);   // 加速时长
        sb.append(',').append(0);   // 废弃字段，填0
        sb.append(',').append(provider.id); // 供应商
        sb.append(',').append(this.thresholdDropPercent);   // 丢包率阈值百分比
        sb.append(',').append(this.thresholdSDPercent); // 标准差阈值百分比
        return sb.toString();
    }

    /**
     * Qos供应商
     */
    public enum Provider {
        /**
         * 缺省的Qos供应商
         */
        DEFAULT(0),

        /**
         * Qos供应商：爱唯光石<br />
         * 发起请求前需要取SecurityToken
         */
        IVTIME(1),

        /**
         * Qos供应商：中兴<br />
         * 发起请求前需要取电话号码
         */
        ZTE(2),

        /**
         * Qos供应商：华为
         */
        HUAWEI(3);

        public final int id;

        Provider(int id) {
            this.id = id;
        }

        public static Provider fromId(int id) {
            for (Provider provider : Provider.values()) {
                if (id == provider.id) {
                    return provider;
                }
            }
            return Provider.DEFAULT;
        }
    }
}
