package cn.wsds.gamemaster.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.AppProfile;

import com.subao.common.utils.InfoUtils;
import com.subao.data.InstalledAppInfo;

/**
 * Created by hujd on 15-10-23.
 */
public class SystemInfoUtil {
//    private static final int DEVICEINFO_UNKNOWN = -1;
//	private final static String TAG = "SystemInfoUntil";

    /**
     * 取内存占用百分比
     *
     * @param context {@link Context}
     * @return 返回[0, 100]之间的百分比整数，失败时返回-1
     */
    public static int getMemoryUsage(Context context) {
        long totalMemSize = InfoUtils.getTotalMemory(context);
        if (totalMemSize <= 0) {
            return -1;
        }
        long availMemSize = getSystemAvaialbeMemorySize(context);
        if (totalMemSize <= availMemSize) {
            return 100;
        }
        return (int) (100 * (totalMemSize - availMemSize) / totalMemSize);
    }

    /**
     * 返回当前正在运行的APP。线程安全的
     *
     * @return 当前正在运行的APP的信息（包名和名字）。如果失败返回null
     */
    public static List<AppProfile> getRunningAppList() {
        InstalledAppInfo[] infoList = GameManager.getInstance().getInstalledApps();
        if (infoList == null || infoList.length == 0) {
            return null;
        }
        List<String> listRunningProcess = getRunningAllProcess();
        if (listRunningProcess == null || listRunningProcess.isEmpty()) {
            return null;
        }
        List<AppProfile> result = new ArrayList<AppProfile>(listRunningProcess.size());
        for (InstalledAppInfo inst : infoList) {
            String packageName = inst.getPackageName();
            if (listRunningProcess.contains(packageName)) {
                result.add(new AppProfile(packageName, inst.getAppLabel()));
            }
        }
        return result;
    }

    /**
     * 返回当前CPU占用百分比
     * <p><b>注意，本函数会阻塞（休眠）几百毫秒，故不宜在主线程里使用！！</b></p>
     *
     * @return [0, 100]区间内的整数。失败则返回-1
     */
    public static int getCpuUsage() {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile("/proc/stat", "r");
            CpuUsage p1 = parseCpuInfoFromFile(file);
            try {
                Thread.sleep(360);
            } catch (InterruptedException e) {
            }
            CpuUsage p2 = parseCpuInfoFromFile(file);
            //
            int delta = (p2.cpu + p2.idle) - (p1.cpu + p1.idle);
            if (delta == 0) {
                return -1;
            }
            int result = 100 * (p2.cpu - p1.cpu) / delta;
            if (result < 0) {
                return -1;
            } else if (result >= 100) {
                return 100;
            } else {
                return result;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NumberFormatException ex) {

        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                }
            }
        }

        return -1;
    }

    private static class CpuUsage {
        public final int idle;
        public final int cpu;

        public CpuUsage(int idle, int cpu) {
            this.idle = idle;
            this.cpu = cpu;
        }
    }

    private static CpuUsage parseCpuInfoFromFile(RandomAccessFile file) throws IOException {
        file.seek(0);
        String load = file.readLine();
        try {
            String[] toks = load.split("\\s+");
            if (toks == null || toks.length <= 8) {
                throw new IOException();
            }
            int idle = Integer.parseInt(toks[4]);
            int cpu = Integer.parseInt(toks[2]) + Integer.parseInt(toks[3]) + Integer.parseInt(toks[5])
                    + Integer.parseInt(toks[6]) + Integer.parseInt(toks[7]) + Integer.parseInt(toks[8]);
            return new CpuUsage(idle, cpu);
        } catch (RuntimeException re) {
            throw new IOException();
        }
    }

    /**
     * 返回可用内存大小
     *
     * @return 以字节为单位的可用内存大小
     */
    private static long getSystemAvaialbeMemorySize(Context context) {
        //获得MemoryInfo对象
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        //获得系统可用内存，保存在MemoryInfo对象上
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }


    /**
     * 取当前正在运行的进程列表。本函数线程安全
     *
     * @return 进程列表或null
     */
    private static List<String> getRunningAllProcess() {
        List<String> lineList = execute("ps");
        if (lineList != null) {
            List<String> appList = new ArrayList<String>(lineList.size());
            for (String line : lineList) {
                String[] tokenStr = line.split("\\s+");
                //Log.i("hujd", "app: " + tokenStr[tokenStr.length -1]);
                appList.add(tokenStr[tokenStr.length - 1]);
            }
            return appList;
        }
        return null;
    }

    /**
     * 执行一段命令，并返回其输出
     *
     * @param cmd 命令
     * @return 该命令的输出结果，每一行为List的一项。如果出错，则返回null
     */
    private static List<String> execute(String cmd) {
        java.lang.Process process = null;
        BufferedReader in = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> list = new ArrayList<String>(16);
            String line;
            while ((line = in.readLine()) != null) {
                list.add(line);
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        return null;
    }


    /**
     * 判断当前接入的WiFi热点是否不安全（没有密码）
     *
     * @return true表示“当前接入的WiFi热点没有密码”，false表示没有接入WiFi或WiFi是安全的（有密码）
     */
    public static boolean isCurrentWiFiConnectionUnsafe(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                return false;
            }
            String currentBSSID = wifiInfo.getBSSID();
            if (currentBSSID == null) {
                return false;
            }
            List<ScanResult> resultList = wifiManager.getScanResults();
            if (resultList == null) {
                return false;
            }
            for (ScanResult scanResult : resultList) {
                if (currentBSSID.equals(scanResult.BSSID)) {
                    String str = scanResult.capabilities;
                    if ("".equals(str) || "[ESS]".equalsIgnoreCase(str)) {
                        return true;
                    }
                    return false;
                }
            }
        } catch (SecurityException e) {
            // 在某个手机上会出现这个异常
        } catch (RuntimeException e) {
            // 在某个联想设备上会出现NullPointer异常
        }
        return false;
    }

    /**
     *  获取CPU名字 */
    public static String getCpuName() {
        String lastLine = readHardwareLine("/proc/cpuinfo");
        if(lastLine != null) {
            String[] array = lastLine.split(":\\s+", 2);
            if(array.length > 1) {
                return array[1];
            }
        }
        return null;
    }

    /**
     * 读取Hardware那一行
     * @return
     */
    private static String readHardwareLine(String fileName) {
        RandomAccessFile fr = null;
        try {
            fr = new RandomAccessFile(fileName, "r");
            fr.seek(0);
            String str = fr.readLine();
            while(str != null) {
                if(str.contains("Hardware")) {
                    return str;
                }
                str = fr.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null)
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }
    
    /**
     * 是否为5.0以上Android版本
     * @return
     */
    public static boolean isStrictOs(){
    	return android.os.Build.VERSION.SDK_INT>=21 ; 
    }
}

