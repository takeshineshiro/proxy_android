package com.subao.common.qos;

import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.ProxyEngineCommunicator;
import com.subao.common.data.ChinaISP;
import com.subao.common.data.Defines;
import com.subao.common.data.QosRegionConfig;
import com.subao.common.data.RegionAndISP;
import com.subao.common.net.IPInfoQuery;
import com.subao.common.utils.StringUtils;

/**
 * 此类负责检测和记录用户4G所在地区和ISP
 */
public class QosUser4GRegionAndISP {

    private static final String TAG = LogTag.QOS;
    private static final QosUser4GRegionAndISP instance = new QosUser4GRegionAndISP();

    /**
     * 如果当前网络是4G，则为当前的ISP和地区。如果不是4G，则为null
     */
    private RegionAndISP current;

    private QosUser4GRegionAndISP() {
    }

    public static QosUser4GRegionAndISP getInstance() {
        return instance;
    }

    static boolean notifyProxyQosParams(ProxyEngineCommunicator proxyEngineCommunicator, QosParam qosParam) {
        if (proxyEngineCommunicator == null) {
            return false;
        }
        proxyEngineCommunicator.setInt(0, Defines.VPNJniStrKey.KEY_ENABLE_QOS, qosParam != null ? 1 : 0);
        if (qosParam != null) {
            proxyEngineCommunicator.defineConst("QOS.AccelTime", Integer.toString(qosParam.accelTime));
            proxyEngineCommunicator.defineConst("QOS.AccelThreshold", Integer.toString(qosParam.deltaThresholdForQosOpen));
            proxyEngineCommunicator.defineConst("QOS.DropThreshold", Integer.toString(qosParam.thresholdDropPercent));
            proxyEngineCommunicator.defineConst("QOS.StandardThreshold", Integer.toString(qosParam.thresholdDropPercent));
        }
        return true;
    }

    /**
     * 通知Proxy层：Qos相关配置参数
     */
    public void notifyProxyQosParams() {
        notifyProxyQosParams(ProxyEngineCommunicator.Instance.get(), instance.getQosParam());
    }

    /**
     * 取用户当前的4G的Region和ISP
     *
     * @return null表示当前不是4G，或者检测失败，没有值
     */
    public final RegionAndISP getCurrent() {
        return current;
    }

    void setCurrent(RegionAndISP value) {
        if (Logger.isLoggableDebug(TAG)) {
            String msg = String.format(
                "Current=%s, setTo=%s",
                StringUtils.objToString(current), StringUtils.objToString(value));
            Log.d(TAG, msg);
        }
        if (current != value) {
            current = value;
            notifyProxyQosParams();
        }
    }

    /**
     * 返回当前地区和ISP的{@link QosParam}
     *
     * @return null表示当前地区和ISP不支持Qos，否则为相应的配置参数
     */
    public QosParam getQosParam() {
        boolean log = Logger.isLoggableDebug(TAG);
        if (!QosRegionConfig.getSwitch()) {
            // 总开关Off时，总是不支持Qos
            if (log) {
                Log.d(TAG, "Qos switch off, getQosParam() return null");
            }
            return null;
        }
        //
        RegionAndISP current = getCurrent();
        if (log) {
            Log.d(TAG, "Current Region-ISP: " + StringUtils.objToString(current));
        }
        QosParam result = (current == null) ? null : QosRegionConfig.getQosParam(current.region, current.isp);
        if (log) {
            Log.d(TAG, "User region and ISP qos param is: " + StringUtils.objToString(result));
        }
        return result;
    }

    /**
     * 当网络切换到非4G的时候被调用
     */
    public void onNetChangeToNot4G() {
        setCurrent(null);
    }

    /**
     * 当网络切换到4G的时候被调用
     */
    public void onNetChangeTo4G() {
        Logger.d(TAG, "Network change to 4G");
        IPInfoQuery.execute(null, new IPInfoQuery.Callback() {
            @Override
            public void onIPInfoQueryResult(Object callbackContext, IPInfoQuery.Result result) {
                RegionAndISP regionAndISP = null;
                if (result != null) {
                    ChinaISP chinaISP = result.getISP();
                    if (chinaISP != null) {
                        regionAndISP = new RegionAndISP(result.region, chinaISP.num);
                    }
                }
                setCurrent(regionAndISP);
            }
        }, null);
    }

}
