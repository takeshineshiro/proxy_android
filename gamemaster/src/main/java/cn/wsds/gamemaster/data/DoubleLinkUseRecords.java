package cn.wsds.gamemaster.data;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.JsonReader;
import android.util.JsonWriter;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.doublelink.DoubleAccelStatisticManage;

import com.subao.common.utils.CalendarUtils;
import com.subao.utils.FileUtils;
import com.subao.utils.Misc;

/**
 * 双链路数据类
 * Created by hujd on 16-5-6.
 */
public class DoubleLinkUseRecords {
    private static final String DOUBLELINKFILE = "double_link_used";
    private static final String WIFI_ACCEL_TCP_PROTOCOL = "tcp";
    private static final String WIFI_ACCEL_UDP_PROTOCOL = "udp";

    public static final long MB = 1024 * 1024;

    public RecordList getRecordList() {
        return recordList;
    }

    private RecordList recordList = new RecordList();

    private static final DoubleLinkUseRecords INSTANCE = new DoubleLinkUseRecords();
    public static DoubleLinkUseRecords getInstance() {
        return INSTANCE;
    }
    private DoubleLinkUseRecords() {
        loadRecordList();
        //testData();
    }

    /**
     * 构造使用记录
     * @param uid 游戏id
     * @param protocol
     * @param flowBytes 流量使用 （单位字节）
     * @param percent  流量和wifi的占比
     */
    public void createRecords(int uid, String protocol, long flowBytes, int percent) {

        if(WIFI_ACCEL_TCP_PROTOCOL.equalsIgnoreCase(protocol)) {
            long tcpFlowBytes = ConfigManager.getInstance().getLastTcpFlowOfDoubleAccel() + flowBytes;
            DoubleAccelStatisticManage.processFlowChangedEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_TCP,tcpFlowBytes);
            ConfigManager.getInstance().setLastTcpFlowOfDoubleAccel(tcpFlowBytes);
            reportTcpFlowEvent(flowBytes);
        }

        if(WIFI_ACCEL_UDP_PROTOCOL.equalsIgnoreCase(protocol)) {
            long udpFlowBytes = ConfigManager.getInstance().getLastUdpFlowOfDoubleAccel() + flowBytes;
            DoubleAccelStatisticManage.processFlowChangedEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW_UDP, udpFlowBytes);
            ConfigManager.getInstance().setLastUdpFlowOfDoubleAccel(udpFlowBytes);
            reportUdpFlowEvent(flowBytes);
        }

        long lastFlowOfDoubleAccel = ConfigManager.getInstance().getLastFlowOfDoubleAccel();
        long lastDayFlowUsed = flowBytes + lastFlowOfDoubleAccel;
        Record record = recordList.get(uid);
        if (record != null) {
            record.setUsedCount(record.getUsedCount() + 1);
            record.setUsedFlow(record.getUsedFlow() + flowBytes);
        } else {
            record = new Record(uid, 1, flowBytes);
            recordList.add(record);
        }

        //更新本次并联加速消耗流量
        ConfigManager.getInstance().setLastFlowOfDoubleAccel(lastDayFlowUsed);

        save();
        Statistic.addEvent(AppMain.getContext(), Statistic.Event.ACC_DUAL_NETWORK_TRIGGER, percent + "%");

        DoubleAccelStatisticManage.processFlowChangedEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_DUAL_NETWORK_USE_FLOW, getUsedFlowTotal());
    }

    private void reportTcpFlowEvent(long flowBytes) {
        int today = CalendarUtils.todayLocal();
        int dayOfTcpFlow = ConfigManager.getInstance().getDayOfTcpFlow();
        if (dayOfTcpFlow == today && flowBytes > 0) {
		   long flow = flowBytes + ConfigManager.getInstance().getLastAccelTcpFlow();
			ConfigManager.getInstance().setLastAccelTcpFlow(flow);
		} else if (dayOfTcpFlow > 0 && dayOfTcpFlow != today) {
			Statistic.addEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_DUAL_NETWORK_DAY_FLOW_OVER0, "TCP");
			ConfigManager.getInstance().setLastAccelTcpFlow(0);
		}
    }

    private void reportUdpFlowEvent(long flowBytes) {
        int today = CalendarUtils.todayLocal();
        int dayOfUdpFlow = ConfigManager.getInstance().getDayOfUdpFlow();
        if (dayOfUdpFlow == today && flowBytes > 0) {
            long flow = flowBytes + ConfigManager.getInstance().getLastAccelUdpFlow();
            ConfigManager.getInstance().setLastAccelUdpFlow(flow);
        } else if (dayOfUdpFlow > 0 && dayOfUdpFlow != today) {
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_DUAL_NETWORK_DAY_FLOW_OVER0, "UDP");
            ConfigManager.getInstance().setLastAccelUdpFlow(0);
        }
    }

    /**
     * 获取总共消耗流量
     * @return
     */
    public long getUsedFlowTotal() {
        long total = 0;
        for (Record record : recordList.getList()) {
            total += record.getUsedFlow();
        }
        return total;
    }
//    private void testData() {
//        createRecords(10094,  100.11f);
//        createRecords(10234,  11.11f);
//        createRecords(10245,  11.11f);
//        createRecords(10081,  11.11f);
//        createRecords(10086,  11.11f);
//
//    }
    /**
     * 从本地文件加载
     */
    private boolean loadRecordList() {
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(FileUtils.getDataFile(DOUBLELINKFILE)));
            reader.setLenient(true);
            this.recordList.loadFromJson(reader);
            return true;
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        } finally {
            Misc.safeClose(reader);
        }
    }


    /**
     * RecordList的序列化器实现
     */
    public void save() {
        JsonWriter writer = null;
        try {
            writer = new JsonWriter(new FileWriter(FileUtils.getDataFile(DOUBLELINKFILE), false));
            recordList.writeToJson(writer);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Misc.safeClose(writer);
        }
    }

    public static class RecordList implements Iterable<Record> {

        @Override
        public Iterator<Record> iterator() {
            return this.list.iterator();
        }

        private final List<Record> list = new ArrayList<Record>(8);


        private void loadFromJson(JsonReader reader) {
            list.clear();
            try {
                reader.beginArray();
                while (reader.hasNext()) {
                    Record rec = RecordSerializer.createFromJson(reader);
                    this.add(rec);
                }
                reader.endArray();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private void add(Record rec) {
            if (rec == null) {
                return ;
            }
            list.add(rec);
        }


       public List<Record> getList() {
           return this.list;
       }


        /**
         * 序列化
         */
        public boolean writeToJson(JsonWriter writer) {
            try {
                writer.beginArray();
                for (Record rec : this.list) {
                    RecordSerializer.writeToJson(rec, writer);
                }
                writer.endArray();
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * 通过游戏id获取item
         */
        public Record get(int uid) {

            for (Record rec : list) {
                if (uid == rec.uid) {
                    return rec;
                }
            }

            return null;
        }

    }


    /**
     * 负责将{@link Record}序列化的类
     */
    static class RecordSerializer {

        private static final String JSON_KEY_UID = "uid";


        private static final String JSON_KEY_USED_CNT = "usedCount";

        private static final String JSON_KEY_USED_FLOW = "usedFlow";

        /**
         * 将Record序列化到JsonWriter
         */
        static boolean writeToJson(Record record, JsonWriter writer) {
            try {
                writer.beginObject();

                writer.name(JSON_KEY_UID).value(record.uid);

                writer.name(JSON_KEY_USED_CNT).value(record.usedCount);

                writer.name(JSON_KEY_USED_FLOW).value(record.usedFlow);

                writer.endObject();
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * 从JsonReader创建一个Record
         */
        static Record createFromJson(JsonReader reader) {
            int usedCnt = 0, uid = 0;
            long usedFlow = 0;
            try {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (JSON_KEY_UID.equals(name)) {
                        uid = reader.nextInt();
                    } else if (JSON_KEY_USED_CNT.equals(name)) {
                        usedCnt = reader.nextInt();
                    } else if (JSON_KEY_USED_FLOW.equals(name)) {
                        usedFlow = reader.nextLong();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (RuntimeException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            if (uid != 0) {
                return new Record(uid, usedCnt, usedFlow);
            } else {
                return null;
            }
        }

    }

    public static final class Record {
        public final int uid;

        public int getUsedCount() {
            return usedCount;
        }

        public long getUsedFlow() {
            return usedFlow;
        }

        public void setUsedFlow(long usedFlow) {
            this.usedFlow = usedFlow;
        }

        private int usedCount;  //使用次数
        private long usedFlow; //使用流量

        public Record(int uid, int usedCount, long usedFlow) {
            this.uid = uid;
            this.usedCount = usedCount;
            this.usedFlow = usedFlow;
        }

        public void setUsedCount(int usedCount){
            this.usedCount = usedCount;
        }
    }
}
