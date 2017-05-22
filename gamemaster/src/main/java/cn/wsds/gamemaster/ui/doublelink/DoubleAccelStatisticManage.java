package cn.wsds.gamemaster.ui.doublelink;

import java.util.ArrayList;
import java.util.List;

import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.CalendarUtils;

import android.content.Context;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.statistic.Statistic;

/**
 * Created by hujd on 16-5-11.
 */
public class DoubleAccelStatisticManage extends EventObserver {
	
//	private static final String TAG = LogTagData.TAG;
	
    private static final long CHECK_PEROID = 30 * 60 * 1000; //消息扫描周期，毫秒
    private static final long MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000; //并联加速触发时间周期，毫秒
    private List<String> netList = new ArrayList<String>(4);
    private boolean isNetChange;
    private GameInfo lastGameInfo;
    
    private static final long FLOW_5M  = 5*1024*1024 ;
    private static final long FLOW_20M  = 20*1024*1024 ;
    private static final long FLOW_50M  = 50*1024*1024 ;
    private static final long FLOW_150M  = 150*1024*1024 ;
    
    private static final int STAGE_NULL0 = 0;
    private static final int STAGE_MORE_THAN_5M = 1;
    private static final int STAGE_MORE_THAN_20M = 2;
    private static final int STAGE_MORE_THAN_50M = 3;
    private static final int STAGE_MORE_THAN_150M = 4 ;
    
    /**
     * 流量使用统计区间段计算<br />
     * (为便于单元测试，此类定义为public)
     */
    public static class FlowUsedStatistic {
    	public final static int[] FLOW_MB_THRESHOLDS = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 120, 150, 200, 250, 300, 400, 500};
    	
    	/**
    	 * 根据给定的流量值（单位：MB），在区间表里查找到其合适的位置，并返回规定格式的区间段
    	 * <br />格式：%d-%d
    	 * @param flowMill 流量，以MB为单位
    	 * @return 格式为“%d-%d”区间段，或"0"（当流量小于等于0时）、或">500"（当流量大于500时）
    	 */
		public static String makeStatisticParam(int flowMill) {
    		if (flowMill <= 0) {
    			return "0";
    		}
    		int maxIndex = FLOW_MB_THRESHOLDS.length;
    		for (int i = 1; i < maxIndex; ++i) {
    			int high = FLOW_MB_THRESHOLDS[i];
    			if (flowMill <= high) {
    				int low = FLOW_MB_THRESHOLDS[i - 1] + 1;
    				return low + "-" + high;
    			}
    		}
    		return ">" + FLOW_MB_THRESHOLDS[maxIndex - 1];
		}
    }
    

    public DoubleAccelStatisticManage() {
        MainHandler.getInstance().post(new RunnableSwtichEvent());
    }

    @Override
    public void onNetChange(NetTypeDetector.NetType state) {
        if (lastGameInfo == null || state == NetTypeDetector.NetType.DISCONNECT) {
            return;
        }

        if (!isNetChange) {
            isNetChange = true;
            addNetworkName(state, false);
        }

        addNetworkName(state, true);
    }
    
    private String addNetworkName(NetTypeDetector.NetType state, boolean isCurrentNetwork) {
        String networkName;
        if (state == NetTypeDetector.NetType.WIFI) {
            if (!isCurrentNetwork) {
                networkName = "数据";
            } else {
                networkName = "WiFi";
            }
        } else {
            if (!isCurrentNetwork) {
                networkName = "WiFi";
            } else {
                networkName = "数据";
            }
        }
        netList.add(networkName);
        return networkName;
    }

    @Override
    public void onTopTaskChange(GameInfo info) {
        if (lastGameInfo == info) {
            return;
        }

        if (lastGameInfo != null && !netList.isEmpty()) {
            // upload event what network changed times in game
            uploadEvent();
            netList.clear();

        }
        lastGameInfo = info;
    }



    private void uploadEvent() {
        StringBuilder stringBuffer = new StringBuilder();
        for (String str : netList) {
            stringBuffer.append(str).append("-");
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - "-".length());
        Statistic.addEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_USER_NETWORK_CHANGE, stringBuffer.toString());
    }

    /**
     * 发送并联加速开关状态
     */
    private static class RunnableSwtichEvent implements Runnable {
        private boolean isFirst;

        @Override
        public void run() {
            MainHandler.getInstance().postDelayed(this, CHECK_PEROID);
            long now = System.currentTimeMillis();
            long lastSubmitTime = ConfigManager.getInstance().getLastTimeOfSubmitDoubleAccel();

            if(!CalendarUtils.isSameMonthOfCST(now, lastSubmitTime)) {
                //清除本月流量消耗
                ConfigManager.getInstance().setLastFlowOfDoubleAccel(0);
            }

            if (lastSubmitTime > now) {
                // 手机时间改动过（从未来改到现在），那么就假设24小时前报过一次了
                lastSubmitTime = 0;
            }
            // （因为每24小时只上报一次）
            if ((now - lastSubmitTime) > MILLISECONDS_PER_DAY) {
                if (!isFirst) {
                    uploadDoubleAccelStatus(now);

                    //uploadUsedFlowEvent();
                } else {
                    if (now - AppMain.getStartTime() > MILLISECONDS_PER_DAY) {
                        uploadDoubleAccelStatus(now);

                        //uploadUsedFlowEvent();
                    }
                }
            }
            isFirst = true;

        }

        private void uploadDoubleAccelStatus(long now) {
            boolean isEnable = ConfigManager.getInstance().isEnableDoubleAccel();
            String status = isEnable ? "开" : "关";
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.INTERACTIVE_DUAL_NETWORK_SWITCH_STATUS, status);
            ConfigManager.getInstance().setLastTimeOfSubmitDoubleAccel(now);
        }

//        private void uploadUsedFlowEvent() {
//            int flowMB = (int)(ConfigManager.getInstance().getLastFlowOfDoubleAccel() / DoubleLinkUseRecords.MB);
//            StatisticDefault.addEvent(AppMain.getContext(), StatisticDefault.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW, FlowUsedStatistic.makeStatisticParam(flowMB));
//            ConfigManager.getInstance().setLastFlowOfDoubleAccel(0);
//        }

    }

    public static void processFlowChangedEvent(Context context, Statistic.Event event, long flow){
    	if((context ==null)||(flow<=0)){
    		return ;
    	}
    	
        int stageCount = getStageCount(flow) ;
    	
    	for(int i = 0; i< stageCount ; i++){   	
    		reportFlowChangedEvent(context, event, i);
    	}    
    }
    
    private static int getStageCount(long flow){  	 
    	
    	int stageIndex = 0;
    	if(flow<=FLOW_5M){  //
    		stageIndex = STAGE_NULL0;
    	}else if(flow<=FLOW_20M){
    		stageIndex = STAGE_MORE_THAN_5M;
    	}else if(flow<=FLOW_50M){
    		stageIndex = STAGE_MORE_THAN_20M;
    	}else if(flow<=FLOW_150M){
    		stageIndex = STAGE_MORE_THAN_50M ;
    	}else{
    		stageIndex = STAGE_MORE_THAN_150M ;
    	}
    	
    	return (stageIndex+1) ;
    }
    
    private static void reportFlowChangedEvent(Context context, Statistic.Event event, int stageIndex){
    	if((context==null)||(stageIndex<0)){
    		return ;
    	}
    	
    	String params;
    	
    	switch(stageIndex){
    	case STAGE_NULL0:
			if (setFlowConfig(event, ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_NULL0,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_NULL0,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_NULL0)) {
				return;
			}
			params = "双链路使用了数据流量累计>0";
//    		event = Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_NULL0;
    		break;
    	case STAGE_MORE_THAN_5M:
    		if(setFlowConfig(event, ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_5M,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_5M,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_5M)){
    			return;
    		}
    		params = "双链路使用了数据流量累计>5M";
//    		event = Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_5M;
    		break;
    	case STAGE_MORE_THAN_20M:
    		if(setFlowConfig(event, ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_20M,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_20M,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_20M)){
    			return;
    		}
    		params = "双链路使用了数据流量累计>20M";
//    		event = Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_20M;
    		break;
    	case STAGE_MORE_THAN_50M:
    		if(setFlowConfig(event, ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_50M,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_50M,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_50M)){
    			return;
    		}
    		params = "双链路使用了数据流量累计>50M";
//    		event = Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_50M;
    		break;
    	case STAGE_MORE_THAN_150M:
    		if(setFlowConfig(event, ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_FLOW_150M,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_TCP_FLOW_150M,
					ConfigManager.DOUBLE_ACCEL_FLAG_HAS_REPORT_UDP_FLOW_150M)){
    			return;
    		}
    		params = "双链路使用了数据流量累计>150M";
//    		event = Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_150M;
    		break;
    	default:
    		return;
    		
    	}

		Statistic.addEvent(context,event,params);
    }

	private static boolean setFlowConfig(Statistic.Event event, long bit, long tcpBit, long udpBit) {
		if(event == Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW ) {
			if (ConfigManager.getInstance().isDoubleAccelFlagHasReportFlow(bit)) {
				return true;
			}
			ConfigManager.getInstance().setDoubleAccelFlagHasReportFlow(bit);
		}

		if(event == Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_TCP ) {
			if(ConfigManager.getInstance().isDoubleAccelFlagHasReportFlow(tcpBit)) {
				return true;
			}
			ConfigManager.getInstance().setDoubleAccelFlagHasReportFlow(tcpBit);
		}

		if(event == Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_UDP ) {
			if (ConfigManager.getInstance().isDoubleAccelFlagHasReportFlow(udpBit)) {
				return true;
			}
			ConfigManager.getInstance().setDoubleAccelFlagHasReportFlow(udpBit);
		}
		return false;
	}


//    /**
//     * 发送并联加速开关状态
//     */
//    private static class RunnableFlowEvent implements Runnable {
//        private int[] flowThresholds = {10, 20, 30, 40, 50, 80, 100, 150, 200};
//
//        private void uploadUsedFlowEvent() {
//            int flowMill = 0;
//            DoubleLinkUseRecords.RecordList recordList = DoubleLinkUseRecords.getInstance().getRecordList();
//            if(recordList.getList().isEmpty()) {
//                return;
//            }
//            for (DoubleLinkUseRecords.Record record : recordList) {
//                flowMill += (int) record.getUsedFlow();
//            }
//
//            int find = searchFlow(flowMill);
//            if (find > 0 && find != ConfigManager.getInstance().getLastFlowOfDoubleAccel()) {
//                Log.d("hujd", "BACKSTAGE_DUAL_NETWORK_USE_FLOW event upload");
//                StatisticDefault.addEvent(AppMain.getContext(), StatisticDefault.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW, find + "");
//                ConfigManager.getInstance().setLastFlowOfDoubleAccel(find);
//            }
//        }
//
//        private int searchFlow(int flowMill) {
//            int start = 0, end = flowThresholds.length - 1;
//            int find = -1;
//            while (start <= end) {
//                int middle = start + ((end - start) >> 1);
//                int add = flowMill - flowThresholds[middle];
//                /**相差为2,认为条件成立*/
//                if (Math.abs(add) <= 2) {
//                    find = flowThresholds[middle];
//                    break;
//                } else if (flowMill < flowThresholds[middle]) {
//                    end = middle - 1;
//                } else {
//                    start = middle + 1;
//                }
//            }
//
//            return find;
//        }
//
//        @Override
//        public void run() {
//            MainHandler.getInstance().postDelayed(this, 30000/*CHECK_PEROID / 6*/);
//            uploadUsedFlowEvent();
//        }
//    }
}
