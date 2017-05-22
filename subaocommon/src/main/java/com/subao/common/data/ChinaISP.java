package com.subao.common.data;

/**
 * 中国ISP
 * Created by nosound on 2016/10/18.
 */
public enum ChinaISP {
    CHINA_TELECOM(10, "中国电信", "CT"),
    CHINA_UNICOM(11, "中国联通", "CU"),
    CHINA_MOBILE(12, "中国移动", "CM");

    public final int num;
    public final String chinese;
    public final String code;

    ChinaISP(int num, String chinese, String code) {
        this.num = num;
        this.chinese = chinese;
        this.code = code;
    }
}
