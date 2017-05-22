package com.subao.common.data;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.jni.JniWrapper;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.InfoUtils;
import com.subao.common.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("DefaultLocale")
public class ParallelConfigDownloader extends PortalDataDownloader {

    private static final String TAG = LogTag.PARALLEL;

    /**
     * 本机型是否配置为“WiFi加速可用”？
     */
    private static boolean phoneParallelSupported;

    private final JniWrapper jniWrapper;


    protected ParallelConfigDownloader(Arguments arguments, JniWrapper jniWrapper) {
        super(arguments);
        this.jniWrapper = jniWrapper;
    }

    /**
     * 启动WiFi加速机型列表下载
     */
    public static void start(Arguments arguments, JniWrapper jniWrapper) {
        ParallelConfigDownloader downloader = new ParallelConfigDownloader(arguments, jniWrapper);
        PortalDataEx localData = downloader.loadFromPersistent();
        downloader.processData(localData);
        downloader.executeOnExecutor(ThreadPool.getExecutor(), localData);
    }

    /**
     * 本机型是否是5.0以上版本，且配置为“WiFi加速可用”？
     */
    public static boolean isPhoneParallelSupported() {
        return ParallelConfigDownloader.phoneParallelSupported;
    }

    /**
     * 判断给定Android版本、手机型号和CPU是否支持WiFi加速
     *
     * @param config            从Portal下载到的配置数据
     * @param androidSdkVersion Android版本号，为-1时由函数自取
     * @param model             手机型号，为null时由函数自取
     * @param cpu               CPU型号，为null时由函数自取
     * @return true表示支持，false表示不支持
     */
    private static boolean canParallelAccel(Config config, int androidSdkVersion, String model, String cpu) {
        if (androidSdkVersion <= 0) {
            androidSdkVersion = Build.VERSION.SDK_INT;
        }
        if (androidSdkVersion < Build.VERSION_CODES.LOLLIPOP) {
            Logger.d(TAG, "Android SDK version too low");
            return false;
        }
        if (config == null) {
            return false;
        }
        if (!config.isEnabled()) {
            Logger.d(TAG, "Parallel-Accel switch off");
            return false;
        }
        if (model == null) {
            model = Build.MODEL;
        }
        if (config.isModelMatch(model)) {
            Logger.d(TAG, String.format("The model '%s' matched", model));
            return true;
        }
        Logger.d(TAG, String.format("The model '%s' is not matched, check CPU ...", model));
        if (cpu == null) {
            cpu = InfoUtils.CPU.getCpuName();
        }
        if (config.isCpuMatch(cpu)) {
            Logger.d(TAG, String.format("The CPU '%s' matched", cpu));
            return true;
        } else {
            Logger.d(TAG, String.format("The CPU '%s' is not matched", cpu));
            return false;
        }
    }

    @Override
    protected String getId() {
        return "Parallel";
    }

    @Override
    protected String getUrlPart() {
        return "configs/parallel";
    }

    @Override
    protected void onPostExecute(PortalDataEx portalData) {
        super.onPostExecute(portalData);
        if (portalData != null && portalData.isNewByDownload) {
            processData(portalData);
        }
    }

    private void processData(PortalDataEx portalData) {
        phoneParallelSupported = false;
        Config config = Config.parse(portalData);
        boolean canParallelAccel = phoneParallelSupported = canParallelAccel(config, -1, null, null);
        jniWrapper.setInt(0, Defines.VPNJniStrKey.KEY_ENABLE_QPP, canParallelAccel ? 1 : 0);
        if (Logger.isLoggableDebug(TAG)) {
            Log.d(TAG, "Now switch turn to " + (canParallelAccel ? "on" : "off"));
        }
    }

    static class Config {

        private static final String NAME_SWITCH = "switch";
        private static final String NAME_MODEL = "model";
        private static final String NAME_CPU = "cpu";

        private final boolean enable;
        private final List<String> modelList;
        private final List<String> cpuList;

        private Config(boolean enable, List<String> modelList, List<String> cpuList) {
            this.enable = enable;
            this.modelList = modelList;
            this.cpuList = cpuList;
        }

        static Config create(boolean enable, List<String> modelList, List<String> cpuList) {
            if (modelList != null && cpuList != null && !modelList.isEmpty() && !cpuList.isEmpty()) {
                return new Config(enable, modelList, cpuList);
            } else {
                return null;
            }
        }

        public static Config parse(PortalDataEx portalData) {
            if (portalData == null) {
                return null;
            }
            if (portalData.getDataSize() < 8) {
                return null;
            }
            JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(portalData.getData())));
            try {
                return Config.parseFromJson(jsonReader);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                Misc.close(jsonReader);
            }
            return null;

        }

        /**
         * 将指定List里的某一项，以逗号分隔的形式，序列化为字符串
         * <p>
         * 每项里如果有逗号或'\\'，将被前缀以'\\'字符进行转义
         * </p>
         *
         * @throws IOException
         */
        private static String serializeList(List<String> list) throws IOException {
            StringWriter writer = new StringWriter(list.size() * 32);
            try {
                StringUtils.serializeList(writer, list);
                return writer.toString();
            } finally {
                Misc.close(writer);
            }
        }

        private static boolean find(List<String> list, String s) {
            if (list == null || TextUtils.isEmpty(s)) {
                return false;
            }
            s = s.toLowerCase();
            for (String keyword : list) {
                if (s.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }

        private static List<String> createStringList() {
            return new ArrayList<String>(32);
        }

        /**
         * 解析Json，数据置入两个List里
         *
         * @throws IOException
         */
        private static Config parseFromJson(JsonReader reader) throws IOException {
            List<String> modelList = createStringList();
            List<String> cpuList = createStringList();
            boolean enable = false;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (NAME_MODEL.equals(name)) {
                    deserializeList(reader.nextString().toLowerCase(), modelList);
                } else if (NAME_CPU.equals(name)) {
                    deserializeList(reader.nextString().toLowerCase(), cpuList);
                } else if (NAME_SWITCH.equals(name)) {
                    enable = "1".equals(reader.nextString());
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return Config.create(enable, modelList, cpuList);
        }

        /**
         * 从指定格式的字串里解析以逗号分隔的各项
         *
         * @return 本次总共解析了多少项？
         * @throws IOException
         */
        private static int deserializeList(String content, List<String> list) throws IOException {
            if (TextUtils.isEmpty(content)) {
                return 0;
            }
            StringReader reader = new StringReader(content);
            try {
                return StringUtils.deserializeList(reader, list);
            } finally {
                Misc.close(reader);
            }
        }

//        public int getModelCount() {
//            return this.modelList == null ? 0 : this.modelList.size();
//        }
//
//        public int getCpuCount() {
//            return this.cpuList == null ? 0 : this.cpuList.size();
//        }

        public boolean isEnabled() {
            return this.enable;
        }

        public boolean isCpuMatch(String cpu) {
            return find(this.cpuList, cpu);
        }

        public boolean isModelMatch(String model) {
            return find(this.modelList, model);
        }

        @Override
        public String toString() {
            return String.format("[enable=%b, cpu=%d, model=%d]",
                this.enable,
                this.cpuList == null ? 0 : this.cpuList.size(),
                this.modelList == null ? 0 : this.modelList.size());
        }

    }

}
