package com.subao.common.qos;

import com.subao.common.RoboBase;
import com.subao.common.data.AppType;
import com.subao.common.msg.Message_EventMsg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * QosEventBuilderTest
 * <p>Created by YinHaiBo on 2017/3/20.</p>
 */
public class QosEventBuilderTest extends RoboBase {

    @Test
    public void test() {
        QosManager.Action action = QosManager.Action.OPEN;
        QosEventBuilder qosEventBuilder = new QosEventBuilder(action, 123);
        RuntimeException exception = new RuntimeException("test");
        qosEventBuilder.setException(exception);
        String raw = "Hello, world";
        qosEventBuilder.setRawData(raw.getBytes());
        //
        int srcPort = 3456;
        String privateIp = "privateIp";
        String publicIp = "publicIp";
        String imsi = "imsi";
        String msisdn = "msisdn";
        QosTerminalInfo terminalInfo = new QosTerminalInfo(privateIp, srcPort, publicIp, imsi, msisdn);
//        int dstPort = 5678;
        QosMediaInfo qosMediaInfo = null; //new QosMediaInfo("srcIp", srcPort, "dstIp", dstPort, "TCP");
        QosSetupRequest qosSetupRequest = new QosSetupRequest(
            AppType.ANDROID_SDK, "GUID",
            "2.0.0", "SubaoId", 5 * 60,
            terminalInfo, qosMediaInfo
        );
        qosSetupRequest.setOperator("32.10");
        qosEventBuilder.setQosSetupRequest(qosSetupRequest);
        Message_EventMsg.Event event = qosEventBuilder.build();
        //
        assertEquals("qos_error", event.id);
        assertEquals(action.getDesc(), event.getParamValue("action"));
        assertEquals(exception.getClass().getName(), event.getParamValue("ex_type"));
        assertEquals(exception.getMessage(), event.getParamValue("ex_msg"));
        assertEquals(Integer.toString(123), event.getParamValue("error"));
        //
        assertEquals(qosSetupRequest.getOperator(), event.getParamValue("operator"));
        assertEquals(terminalInfo.getPrivateIp(), event.getParamValue("private_ip"));
        assertEquals(terminalInfo.getMSISDN(), event.getParamValue("msisdn"));
        assertEquals(terminalInfo.getSecurityToken(), event.getParamValue("token"));
        //
        assertEquals(raw, event.getParamValue("raw"));
    }

    @Test
    public void test2() {
        QosManager.Action action = QosManager.Action.MODIFY;
        QosEventBuilder qosEventBuilder = new QosEventBuilder(action, 2);
        Message_EventMsg.Event event = qosEventBuilder.build();
        assertEquals("qos_error", event.id);
        assertEquals(2, event.getParamsCount());
        assertEquals(action.getDesc(), event.getParamValue("action"));
        assertEquals("2", event.getParamValue("error"));
    }

}