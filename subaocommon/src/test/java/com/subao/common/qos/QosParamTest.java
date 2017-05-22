package com.subao.common.qos;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * QosParamTest
 * <p>Created by YinHaiBo on 2017/2/19.</p>
 */
public class QosParamTest {

    @Test
    public void test() throws Exception {
        assertEquals(0, QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN);
        assertEquals(900, QosParam.DEFAULT_ACCEL_TIME);
        assertEquals(0, QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT);
        assertEquals(0, QosParam.DEFAULT_THRESHOLD_SD_PERCENT);

        QosParam qosParam = QosParam.DEFAULT;
        assertEquals(QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN, qosParam.deltaThresholdForQosOpen);
        assertEquals(QosParam.DEFAULT_ACCEL_TIME, qosParam.accelTime);
        assertEquals(QosParam.Provider.DEFAULT, qosParam.provider);
        assertEquals(QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT, qosParam.thresholdDropPercent);
        assertEquals(QosParam.DEFAULT_THRESHOLD_SD_PERCENT, qosParam.thresholdSDPercent);
        //
        qosParam = new QosParam(1, 2, QosParam.Provider.HUAWEI, 0, 0);
        assertNotNull(qosParam.toString());
        assertEquals(qosParam, qosParam);
        assertNotEquals(qosParam, null);
        assertNotEquals(qosParam, this);
        assertEquals(qosParam, new QosParam(1, 2, QosParam.Provider.DEFAULT.HUAWEI, 0, 0));
        assertNotEquals(qosParam, new QosParam(1, 2, QosParam.Provider.IVTIME, 0, 0));
    }

    @Test
    public void testProvider() {
        assertEquals(QosParam.Provider.DEFAULT, QosParam.Provider.fromId(0));
        assertEquals(QosParam.Provider.IVTIME, QosParam.Provider.fromId(1));
        assertEquals(QosParam.Provider.ZTE, QosParam.Provider.fromId(2));
        assertEquals(QosParam.Provider.HUAWEI, QosParam.Provider.fromId(3));
        assertEquals(QosParam.Provider.DEFAULT, QosParam.Provider.fromId(4));
        assertEquals(4, QosParam.Provider.values().length);
    }

    @Test
    public void testSerialize() {
        QosParam qosParam = new QosParam(-12, 34, QosParam.Provider.DEFAULT, 1, 2);
        String s = qosParam.serialize();
        assertEquals("-12,34,0,0,1,2", s);
        QosParam qosParam2 = QosParam.deserialize(s);
        assertEquals(qosParam, qosParam2);
        QosParam qosParam3 = QosParam.deserialize(null);
        assertEquals(QosParam.createDefaultQosParam(QosParam.Provider.DEFAULT), qosParam3);
        //
        s = "1";
        qosParam = QosParam.deserialize(s);
        assertEquals(qosParam.deltaThresholdForQosOpen, 1);
        assertEquals(qosParam.accelTime, QosParam.DEFAULT_ACCEL_TIME);
        assertEquals(qosParam.provider, QosParam.Provider.DEFAULT);
        assertEquals(qosParam.thresholdDropPercent, QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT);
        assertEquals(qosParam.thresholdSDPercent, QosParam.DEFAULT_THRESHOLD_SD_PERCENT);
        //
        s = "1, 2";
        qosParam = QosParam.deserialize(s);
        assertEquals(qosParam.deltaThresholdForQosOpen, 1);
        assertEquals(qosParam.accelTime, 2);
        assertEquals(qosParam.provider, QosParam.Provider.DEFAULT);
        assertEquals(qosParam.thresholdDropPercent, QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT);
        assertEquals(qosParam.thresholdSDPercent, QosParam.DEFAULT_THRESHOLD_SD_PERCENT);
        //
        s = "1, 2, 3";
        qosParam = QosParam.deserialize(s);
        assertEquals(qosParam.deltaThresholdForQosOpen, 1);
        assertEquals(qosParam.accelTime, 2);
        assertEquals(qosParam.provider, QosParam.Provider.DEFAULT);
        assertEquals(qosParam.thresholdDropPercent, QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT);
        assertEquals(qosParam.thresholdSDPercent, QosParam.DEFAULT_THRESHOLD_SD_PERCENT);
        //
        s = "1, 2, 3, 1";
        qosParam = QosParam.deserialize(s);
        assertEquals(qosParam.deltaThresholdForQosOpen, 1);
        assertEquals(qosParam.accelTime, 2);
        assertEquals(qosParam.provider, QosParam.Provider.IVTIME);
        assertEquals(qosParam.thresholdDropPercent, QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT);
        assertEquals(qosParam.thresholdSDPercent, QosParam.DEFAULT_THRESHOLD_SD_PERCENT);
        //
        s = "1, 2, 3, 1, 5";
        qosParam = QosParam.deserialize(s);
        assertEquals(qosParam.deltaThresholdForQosOpen, 1);
        assertEquals(qosParam.accelTime, 2);
        assertEquals(qosParam.provider, QosParam.Provider.IVTIME);
        assertEquals(qosParam.thresholdDropPercent, 5);
        assertEquals(qosParam.thresholdSDPercent, QosParam.DEFAULT_THRESHOLD_SD_PERCENT);
        //
        s = "1, 2, 3, 1, 5, 6";
        qosParam = QosParam.deserialize(s);
        assertEquals(qosParam.deltaThresholdForQosOpen, 1);
        assertEquals(qosParam.accelTime, 2);
        assertEquals(qosParam.provider, QosParam.Provider.IVTIME);
        assertEquals(qosParam.thresholdDropPercent, 5);
        assertEquals(qosParam.thresholdSDPercent, 6);
        //
        //
        s = "1, 2, 3, 1, 5, 6, 7";
        qosParam = QosParam.deserialize(s);
        assertEquals(qosParam.deltaThresholdForQosOpen, 1);
        assertEquals(qosParam.accelTime, 2);
        assertEquals(qosParam.provider, QosParam.Provider.IVTIME);
        assertEquals(qosParam.thresholdDropPercent, 5);
        assertEquals(qosParam.thresholdSDPercent, 6);
        //
        s = "1, a, 3, 1";
        qosParam = QosParam.deserialize(s);
        assertEquals(qosParam.deltaThresholdForQosOpen, 1);
        assertEquals(qosParam.accelTime, QosParam.DEFAULT_ACCEL_TIME);
        assertEquals(qosParam.provider, QosParam.Provider.IVTIME);
        assertEquals(qosParam.thresholdDropPercent, QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT);
        assertEquals(qosParam.thresholdSDPercent, QosParam.DEFAULT_THRESHOLD_SD_PERCENT);
    }

}