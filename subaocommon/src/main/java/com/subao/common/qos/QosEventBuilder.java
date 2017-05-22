package com.subao.common.qos;

import com.subao.common.msg.Message_EventMsg;

import java.util.HashMap;
import java.util.Map;

/**
 * Qos请求过程中的事件数据
 * <p>Created by YinHaiBo on 2017/3/20.</p>
 */
public class QosEventBuilder {

    private final QosManager.Action action;
    private final int errorCode;

    private Exception exception;
    private QosSetupRequest qosSetupRequest;
    private byte[] rawData;

    public QosEventBuilder(QosManager.Action action, int errorCode) {
        this.action = action;
        this.errorCode = errorCode;
    }

    private static Map<String, String> put(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
        return map;
    }

    public Message_EventMsg.Event build() {
        Map<String, String> params = new HashMap<String, String>(16);
        params.put("error", Integer.toString(errorCode));
        params.put("action", action.getDesc());
        if (this.exception != null) {
            put(params, "ex_type", this.exception.getClass().getName());
            put(params, "ex_msg", this.exception.getMessage());
        }
        if (this.qosSetupRequest != null) {
            put(params, "operator", this.qosSetupRequest.getOperator());
            QosTerminalInfo terminalInfo = this.qosSetupRequest.terminalInfo;
            if (terminalInfo != null) {
                put(params, "private_ip", terminalInfo.getPrivateIp());
                put(params, "msisdn", terminalInfo.getMSISDN());
                put(params, "token", terminalInfo.getSecurityToken());
            }
        }
        if (this.rawData != null) {
            put(params, "raw", new String(this.rawData));
        }
        return new Message_EventMsg.Event("qos_error", params);
    }

    public void setException(Exception e) {
        this.exception = e;
    }

    public void setQosSetupRequest(QosSetupRequest qosSetupRequest) {
        this.qosSetupRequest = qosSetupRequest;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }
}
