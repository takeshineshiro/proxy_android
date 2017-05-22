package com.subao.common.data;

import android.annotation.SuppressLint;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.qos.QosParam;
import com.subao.common.qos.QosUser4GRegionAndISP;
import com.subao.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**************************************************************
 * 注意，为兼容老版本的代码，Portal上关于qos_regio的配置，必须继续保持老版(2016.10.26以前）的配置格式，即：
 * {"white_list":"32.10,32.12,31.10"}这样的配置
 * <p>
 * 新的代码（2016.10.26以后），不再使用white_list字段，改用
 * {"cfg_32.10":"-100,0", "cfg_31.*":""} 这样的配置
 *********************************************************************/


/**
 * 负责：“Qos按地区和ISP进行开关”的在线参数
 */
@SuppressLint("DefaultLocale")
public class QosRegionConfig extends PortalKeyValuesDownloader {

    private static final String TAG = LogTag.DATA;
    private static final String KEY_PREFIX_CFG = "cfg_";

    /**
     * 全局使用的配置
     */
    private static Config config = null; //Config.createDefaultData();

    /**
     * 本对象实例在解析的时候暂存到此对象中，处理完后置为全局配置 {@link #config}
     */
    private Config pendingConfig = new Config(new HashMap<RegionAndISP, QosParam>(16));

    protected QosRegionConfig(Arguments arguments) {
        super(arguments);
    }

    public static void start(Arguments arguments) {
        QosRegionConfig downloader = new QosRegionConfig(arguments);
        PortalKeyValuesDownloader.start(downloader);
    }

    /**
     * 取Qos总开关
     */
    public static boolean getSwitch() {
        Config config = QosRegionConfig.config;
        return config != null && !config.isConfigEmpty();
    }

    /**
     * 取指定区域和ISP的 {@link QosParam}
     *
     * @param region 区域
     * @param isp    ISP
     * @return null表示指定的区域和ISP不支持Qos
     */
    public static QosParam getQosParam(int region, int isp) {
        Config config = QosRegionConfig.config;
        QosParam qosParam = (config == null) ? null : config.getQosParam(region, isp);
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, String.format("getQosParam(%d, %d) return %s",
                region, isp,
                StringUtils.objToString(qosParam)));
        }
        return qosParam;
    }

    @Override
    protected void process(String name, String value) {
        this.pendingConfig.parseKeyValues(name, value);
    }

    @Override
    protected void onAllProcessed() {
        super.onAllProcessed();
        QosRegionConfig.config = this.pendingConfig;
        QosUser4GRegionAndISP.getInstance().notifyProxyQosParams();
    }

    @Override
    protected String getUrlPart() {
        return "configs/qos_region";
    }

    @Override
    protected String getId() {
        return "QosRegion";
    }

    /**
     * QosRegionConfig的数据
     */
    static class Config {

        /**
         * 白名单
         */
        private final Map<RegionAndISP, QosParam> paramList;

        Config(Map<RegionAndISP, QosParam> paramList) {
            this.paramList = paramList;
        }

        // TODO: 默认数据
        static Config createDefaultData() {
            Map<RegionAndISP, QosParam> map = new HashMap<RegionAndISP, QosParam>(4);
            map.put(new RegionAndISP(ChinaRegion.Jiangsu.num, ChinaISP.CHINA_TELECOM.num),
                QosParam.createDefaultQosParam(QosParam.Provider.IVTIME));
            map.put(new RegionAndISP(ChinaRegion.Jiangsu.num, ChinaISP.CHINA_MOBILE.num), QosParam.DEFAULT);
            map.put(new RegionAndISP(ChinaRegion.Shanghai.num, ChinaISP.CHINA_TELECOM.num), QosParam.DEFAULT);
            map.put(new RegionAndISP(ChinaRegion.Guangdong.num, ChinaISP.CHINA_MOBILE.num),
                new QosParam(QosParam.DEFAULT_DELTA_THRESHOLD_FOR_QOS_OPEN, 20 * 60,
                    QosParam.Provider.ZTE,
                    QosParam.DEFAULT_THRESHOLD_DROP_RATE_PERCENT,
                    QosParam.DEFAULT_THRESHOLD_SD_PERCENT
                ));
            return new Config(map);
        }

        /**
         * 将特定格式的字符串，解析为一个{@link RegionAndISP}
         * <p>
         * 字符串的格式为：用小数点分隔的两个整型数字，分别代表{@link RegionAndISP}里的region和isp字段
         * </p>
         * <p>
         * 如果数字被*号所代替，对应字段被解析为-1
         * </p>
         */
        private static RegionAndISP parseRegionAndISP(String field) {
            String[] fields = field.split("\\.");
            if (fields.length < 2) {
                return null;
            }
            try {
                int region = parseInt(fields[0]);
                int isp = parseInt(fields[1]);
                return new RegionAndISP(region, isp);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static int parseInt(String s) {
            if ("*".equals(s)) {
                return -1;
            }
            return Integer.parseInt(s);
        }

        void parseKeyValues(String key, String value) {
            if (key == null || !key.startsWith(KEY_PREFIX_CFG)) {
                return;
            }
            if (value == null) {
                value = "";
            }
            RegionAndISP regionAndISP = parseRegionAndISP(key.substring(KEY_PREFIX_CFG.length()));
            if (regionAndISP != null) {
                QosParam qosParam = QosParam.deserialize(value);
                paramList.put(regionAndISP, qosParam);
            }
        }

        boolean isConfigEmpty() {
            return this.paramList == null || this.paramList.isEmpty();
        }

        /**
         * 获取指定{@line RegionAndISP}的参数
         *
         * @return null表示指定的区域和ISP不支持Qos，否则为 {@link QosParam}
         */
        QosParam getQosParam(int region, int isp) {
            if (this.paramList != null) {
                for (Map.Entry<RegionAndISP, QosParam> entry : this.paramList.entrySet()) {
                    RegionAndISP regionAndISP = entry.getKey();
                    if (regionAndISP.region < 0 || regionAndISP.region == region) {
                        if (regionAndISP.isp < 0 || regionAndISP.isp == isp) {
                            return entry.getValue();
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (!(o instanceof Config)) {
                return false;
            }
            Config other = (Config) o;
            return Misc.isEquals(this.paramList, other.paramList);
        }

        @Override
        public String toString() {
            return String.format("[QosRegion and Params: count=%d]", this.paramList == null ? 0 : this.paramList.size());
        }

    }

}
