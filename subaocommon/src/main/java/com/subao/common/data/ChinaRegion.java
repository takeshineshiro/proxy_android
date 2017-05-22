package com.subao.common.data;

/**
 * 中国行政区拼音、缩写、汉字及代码
 * Created by nosound on 2016/10/18.
 */
public enum ChinaRegion {
    Beijing(11, "北京", "BJ", "Beijing"),
    Tianjin(12, "天津", "TJ", "Tianjin"),
    Hebei(13, "河北", "HE", "Hebei"),
    Shanxi(14, "山西", "SX", "Shanxi"),
    Nei(15, "内蒙古", "NM", "Nei"),
    Liaoning(21, "辽宁", "LN", "Liaoning"),
    Jilin(22, "吉林", "JL", "Jilin"),
    Heilongjiang(23, "黑龙江", "HL", "Heilongjiang"),
    Shanghai(31, "上海", "SH", "Shanghai"),
    Jiangsu(32, "江苏", "JS", "Jiangsu"),
    Zhejiang(33, "浙江", "ZJ", "Zhejiang"),
    Anhui(34, "安徽", "AH", "Anhui"),
    Fujian(35, "福建", "FJ", "Fujian"),
    jiangxi(36, "江西", "JX", "jiangxi"),
    Shandong(37, "山东", "SD", "Shandong"),
    Henan(41, "河南", "HA", "Henan"),
    Hubei(42, "湖北", "HB", "Hubei"),
    Hunan(43, "湖南", "HN", "Hunan"),
    Guangdong(44, "广东", "GD", "Guangdong"),
    Guangxi(45, "广西", "GX", "Guangxi"),
    Hainan(46, "海南", "HI", "Hainan"),
    Chongqing(50, "重庆", "CQ", "Chongqing"),
    Sichuan(51, "四川", "SC", "Sichuan"),
    Guizhou(52, "贵州", "GZ", "Guizhou"),
    Yunnan(53, "云南", "YN", "Yunnan"),
    Xizang(54, "西藏", "XZ", "Xizang"),
    Shaanxi(61, "陕西", "SN", "Shaanxi"),
    Gansu(62, "甘肃", "GS", "Gansu"),
    Qinghai(63, "青海", "QH", "Qinghai"),
    Ningxia(64, "宁夏", "NX", "Ningxia"),
    Xinjiang(65, "新疆", "XJ", "Xinjiang"),
    Taiwan(71, "台湾", "TW", "Taiwan"),
    Hongkong(81, "香港", "HK", "Hongkong");

    public final int num;
    public final String chinese;
    public final String code;
    public final String pinyin;

    ChinaRegion(int num, String chinese, String code, String pinyin) {
        this.num = num;
        this.chinese = chinese;
        this.code = code;
        this.pinyin = pinyin;
    }
}
