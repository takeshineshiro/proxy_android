package cn.wsds.gamemaster.statistic;

import android.content.Context;
import android.os.Build;

import com.subao.common.net.NetTypeDetector;

import cn.wsds.gamemaster.net.NetSwitcher;

/**
 * 负责统计当前网络（WiFi和Mobile）开关的状态
 */
public class NetSwitcherStatistic {

    private NetSwitcherStatistic() {}

    /**
     * 执行统计操作
     *
     * @param context         {@link Context}
     * @param netTypeDetector 判断当前网络类型的检测接口
     */
    public static void execute(Context context, NetTypeDetector netTypeDetector) {
        if (Build.VERSION.SDK_INT < 21) {
            // 5.0以下不统计
            return;
        }
        //
        Statistic.Event event;
        NetTypeDetector.NetType currentNetType = netTypeDetector.getCurrentNetworkType();
        switch (currentNetType) {
        case WIFI:
            NetSwitcher.SwitchState switchState = NetSwitcher.getMobileDataSwitchState(context);
            switch(switchState) {
            case ON:
                event = Statistic.Event.ACC_GAME_ONTOP_NET_WIFI_AND_FLOW;
                break;
            case OFF:
                event = Statistic.Event.ACC_GAME_ONTOP_NET_WIFI;
                break;
            default:
                event = Statistic.Event.ACC_GAME_ONTOP_WIFI_AND_MOBILE_UNKNOWN;
                break;
            }
            break;
        case MOBILE_2G:
        case MOBILE_3G:
        case MOBILE_4G:
            event = Statistic.Event.ACC_GAME_ONTOP_NET_FLOW;
            break;
        default:
            event = Statistic.Event.ACC_GAME_ONTOP_NET_NULL;
            break;
        }
        Statistic.addEvent(context, event);
    }

}
