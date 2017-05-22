package com.subao.common.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.subao.common.Misc;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("DefaultLocale")
public class InfoUtils {

    public static final String IMSI_EMPTY = "000000000000000";

    private static FileUtils.FileOperator fileOperator = FileUtils.FileOperator.DEFAULT;

    synchronized static FileUtils.FileOperator setFileReaderCreator(FileUtils.FileOperator fileOperator) {
        FileUtils.FileOperator old = InfoUtils.fileOperator;
        InfoUtils.fileOperator = (fileOperator == null) ? FileUtils.FileOperator.DEFAULT : fileOperator;
        return old;
    }

    public static boolean isEmptyUniqueID(String id) {
        return IMSI_EMPTY.equals(id);
    }

    /**
     * 传递给VPNJni的一个UniqueID
     */
    @SuppressLint("HardwareIds")
    public static String getUniqueID(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            return IMSI_EMPTY;
        }
        String imsi;
        try {
            imsi = tm.getSubscriberId();
        } catch (RuntimeException e) {
            imsi = null;
        }
        if (isIMSIValid(imsi)) {
            return imsi;
        }
        try {
            imsi = tm.getDeviceId();
        } catch (RuntimeException e) {
            return IMSI_EMPTY;
        }
        return isIMSIValid(imsi) ? imsi : IMSI_EMPTY;
    }

    /**
     * 判断一个IMSI/IMEI字串是否合法 按目前系统约定，合法的IMSI/IMEI字串应该是不为空且不超过24位的纯数字字串
     */
    public static Boolean isIMSIValid(String s) {
        if (s == null) {
            return false;
        }
        int len = s.length();
        if (len == 0 || len > 24) {
            return false;
        }
        for (int i = 0; i < len; ++i) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取手机的IMSI
     */
    @SuppressLint("HardwareIds")
    public static String getIMSI(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                return tm.getSubscriberId();
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    /**
     * 获取设备的IMEI
     */
    @SuppressLint("HardwareIds")
    public static String getIMEI(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                return tm.getDeviceId();
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    /**
     * 取设备的Mac地址
     */
    @SuppressWarnings("MissingPermission")
    @SuppressLint("HardwareIds")
    public static String getMacAddress(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo connectionInfo = wm.getConnectionInfo();
                if (connectionInfo != null) {
                    return connectionInfo.getMacAddress();
                }
            }
        } catch (RuntimeException e) {
            // 在某个坑爹的手机上，getConnectionInfo()会抛异常
        }
        return null;
    }

    /**
     * 取指定APK的VersionName
     *
     * @param context     {@link Context}
     * @param packageName 如果为null，表示取context自己的APK
     * @return null或VersionName
     */
    public static String getVersionName(Context context, String packageName) {
        PackageInfo packInfo = getPackageInfo(context, packageName);
        return packInfo == null ? null : packInfo.versionName;
    }

    /**
     * 取APP自身的VersionName
     */
    public static String getVersionName(Context context) {
        return getVersionName(context, null);
    }

    /**
     * 取指定APK的VersionCode
     *
     * @param context     {@link Context}
     * @param packageName 如果为null，表示取context自己的APK
     * @return 成功返回VersionCode，失败返回-1
     */
    public static int getVersionCode(Context context, String packageName) {
        PackageInfo packInfo = getPackageInfo(context, packageName);
        return packInfo == null ? -1 : packInfo.versionCode;
    }

    /**
     * 取自身的VersionCode
     */
    public static int getVersionCode(Context context) {
        return getVersionCode(context, null);
    }

    /**
     * 取给定{@link ApplicationInfo}的Label
     *
     * @param context         {@link Context}
     * @param applicationInfo 给定的{@link ApplicationInfo}，如果为null，则使用context参数的Application Info
     * @return null或AppLabel
     */
    public static String loadAppLabel(Context context, ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            applicationInfo = context.getApplicationInfo();
        }
        if (applicationInfo != null) {
            PackageManager packageManager = context.getPackageManager();
            if (packageManager != null) {
                CharSequence appLabel = applicationInfo.loadLabel(packageManager);
                if (appLabel != null) {
                    return appLabel.toString();
                }
            }
        }
        return null;
    }


    static PackageInfo getPackageInfo(Context context, String packageName) {
        try {
            if (TextUtils.isEmpty(packageName)) {
                packageName = context.getPackageName();
            }
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getPackageInfo(packageName, 0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取系统信息
     */
    public static String getSystemProperty(String propName) {
        return getSystemProperty(Runtime.getRuntime(), propName);
    }

    static String getSystemProperty(Runtime runtime, String propName) {
        String result;
        BufferedReader input = null;
        try {
            Process p = runtime.exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 256);
            result = input.readLine();
        } catch (Exception ex) {
            result = null;
        } finally {
            Misc.close(input);
        }
        return result;
    }

    /**
     * 辅助函数，从文件里查找关键行，并取出这行里的数值
     */
    private static long parseFileForValue(String filename, String textToMatch, long defaultValue) {
        byte[] buffer;
        try {
            buffer = FileUtils.read(new File(filename), 1024 * 1024);
        } catch (IOException e) {
            return defaultValue;
        }
        return parseForValue(buffer, textToMatch, defaultValue);
    }

    static long parseForValue(byte[] data, String textToMatch, long defaultValue) {
        int length = data.length;
        for (int i = 0; i < length; i++) {
            int ch = data[i];
            if (i == 0 || ch == '\n') {
                if (ch == '\n') {
                    ++i;
                }
                // 这里 i 为一行的行首
                // 现在开始查找这一行是不是以textToMatch开始
                for (int j = i; j < length; j++) {
                    int textIndex = j - i;
                    if (data[j] != textToMatch.charAt(textIndex)) {
                        // 不是，跳出
                        break;
                    }
                    if (textIndex == textToMatch.length() - 1) {
                        // 是的，析出值并返回
                        return Misc.extractLong(data, j, data.length, -1);
                    }
                }
            }
        }
        return defaultValue;
    }

    /**
     * 取总内存大小。
     *
     * @return 以字节为单位的总内存大小。如果失败返回-1
     */
    public static long getTotalMemory(Context context) {
        return getTotalMemory(context, Build.VERSION.SDK_INT);
    }

    static long getTotalMemory(Context context, int androidVersion) {
        // memInfo.totalMem not supported in pre-Jelly Bean APIs.
        if (androidVersion >= Build.VERSION_CODES.JELLY_BEAN) {
            return getTotalMemoryVerHigh(context);
        } else {
            return getTotalMemoryVerLow();
        }
    }

    static long getTotalMemoryVerLow() {
        long totalMem = parseFileForValue("/proc/meminfo", "MemTotal", -1L);
        if (totalMem != -1) {
            return totalMem * 1024;
        } else {
            return totalMem;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static long getTotalMemoryVerHigh(Context context) {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(memInfo);
        return memInfo.totalMem;
    }

    public static class CPU {

        static final String FILENAME_CPU_INFO = "/proc/cpuinfo";
        static final String DIRECTORY_CPU = "/sys/devices/system/cpu/";

        /**
         * 获取CPU名字
         */
        public static String getCpuName() {
            try {
                return getCpuName(fileOperator.openReader("/proc/cpuinfo"));
            } catch (IOException e) {
                return null;
            } catch (RuntimeException e) {
                return null;
            }
        }

        private static String getCpuName(Reader reader) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(reader);
            try {
                Pattern pattern = Pattern.compile("Hardware\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE);
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        throw new EOFException();
                    }
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String s = m.group(1);
                        if (s != null) {
                            return s.trim();
                        }
                    }
                }
            } finally {
                Misc.close(bufferedReader);
            }
        }

        /**
         * 获取CPU内核数
         */
        public static int getCores() {
            return getCores(Build.VERSION.SDK_INT);
        }

        static int getCores(int androidSDKVersion) {
            if (androidSDKVersion <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                // Gingerbread doesn't support giving a single application access to both cores, but a
                // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
                // chipset and Gingerbread; that can let an app in the background run without impacting
                // the foreground application. But for our purposes, it makes them single core.
                return 1;
            }
            try {
                int count = getCpuFileCount(fileOperator.createFile(DIRECTORY_CPU));
                return count > 1 ? count : 1;
            } catch (RuntimeException e) {
                return 1;
            }
        }

        private static int getCpuFileCount(File dir) {
            File[] files = dir.listFiles(
                new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        String path = pathname.getName();
                        //regex is slow, so checking char by char.
                        if (path.startsWith("cpu")) {
                            for (int i = 3; i < path.length(); i++) {
                                if (!Character.isDigit(path.charAt(i))) {
                                    return false;
                                }
                            }
                            return true;
                        }
                        return false;
                    }
                }
            );
            return (files == null) ? 0 : files.length;
        }


        /**
         * 获取CPU主频
         */
        public static long getMaxFreqKHz() {
            long maxFreq = -1L;
            try {
                int cores = getCores();
                for (int i = 0; i < cores; i++) {
                    String filename = String.format("%s/cpu%d/cpufreq/cpuinfo_max_freq", "/sys/devices/system/cpu/", i);
                    long freq = parseLongFromFile(filename);
                    if (freq > maxFreq) {
                        maxFreq = freq;
                    }
                }
                if (maxFreq <= 0) {
                    long freqBound = parseFileForValue("/proc/cpuinfo", "cpu MHz", -1L);
                    if (freqBound != -1L) {
                        maxFreq = freqBound * 1000;
                    }
                }
            } catch (IOException e) {
                // do nothing
            } catch (RuntimeException e) {
                // do nothing
            }
            return maxFreq;
        }

        private static long parseLongFromFile(String filename) throws IOException {
            InputStream input = null;
            try {
                input = fileOperator.openInput(filename);
                byte[] buffer = new byte[128];
                int len = input.read(buffer);
                return Misc.extractLong(buffer, 0, len, -1L);
            } finally {
                Misc.close(input);
            }
        }
    }

}
