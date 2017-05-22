package com.subao.common.net;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.JsonReader;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.data.Address;
import com.subao.common.data.ChinaISP;
import com.subao.common.data.ServiceLocation;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.JsonUtils;
import com.subao.common.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询指定IP的地理位置、运营商等信息的查询器
 * <p>Created by YinHaiBo on 2017/2/27.</p>
 */

public final class IPInfoQuery {

    private static final String TAG = LogTag.NET;
    private static final List<Query> existQueryList = new ArrayList<Query>(4);

    private static Worker currentWorker = new WorkerByDNS();

    private IPInfoQuery() {
    }

    /**
     * 当用户鉴权完成后被调用
     *
     * @param isVIP true表示VIP用户
     */
    public static void onUserAuthComplete(boolean isVIP, ServiceLocation serviceLocation) {
        if (isVIP) {
            currentWorker = new WorkerBySubao(serviceLocation);
        } else {
            currentWorker = new WorkerByDNS();
        }
    }

    /**
     * 执行查询操作，并在主线程里执行回调
     *
     * @param ip              要查询的IP，如果为null或空串，表示查本机
     * @param callback        回调接口
     * @param callbackContext 回调接口所需的上下文
     */
    public static void execute(
        String ip,
        Callback callback, Object callbackContext
    ) {
        execute(ip, callback, callbackContext, false, null);
    }

    /**
     * 执行查询操作，并在工作线程里执行回调
     *
     * @param ip              要查询的IP，如果为null或空串，表示查本机
     * @param callback        回调接口
     * @param callbackContext 回调接口所需的上下文
     */
    public static void executeThenCallbackInWorkThread(
        String ip,
        Callback callback, Object callbackContext
    ) {
        execute(ip, callback, callbackContext, true, null);
    }

    /**
     * 以VIP模式（速宝服务）查询指定IP的归属地
     *
     * @param ip                   要查询的IP，如果为null或空串，表示查本机
     * @param callback             回调接口
     * @param callbackContext      回调接口所需的上下文
     * @param callbackInWorkThread true表示直接在工作线程（非主线程）里回调, false表示总是在主线程里回调
     * @param serviceLocation      速宝服务的服务地址
     */
    public static void executeByVIPMode(
        String ip,
        Callback callback, Object callbackContext,
        boolean callbackInWorkThread,
        ServiceLocation serviceLocation
    ) {
        execute(ip, callback, callbackContext, callbackInWorkThread, new WorkerBySubao(serviceLocation));
    }

    /**
     * 执行查询操作，并根据要求，在主线程或工作线程里执行回调
     *
     * @param ip                   要查询的IP，如果为null或空串，表示查本机
     * @param callback             回调接口
     * @param callbackContext      回调接口所需的上下文
     * @param callbackInWorkThread true表示直接在工作线程（非主线程）里回调, false表示总是在主线程里回调
     * @param worker               指定{@link Worker}，如果为null表示自动使用当前适合的Worker
     */
    private static void execute(
        String ip,
        Callback callback, Object callbackContext,
        boolean callbackInWorkThread,
        Worker worker
    ) {
        Query query = new Query(callback, callbackContext, callbackInWorkThread);
        // 如果是解析本机IP，看能不能优化一下
        if (TextUtils.isEmpty(ip)) {
            boolean needCreateWorker;
            synchronized (existQueryList) {
                needCreateWorker = existQueryList.isEmpty(); // 如果没有未决的本机请求，说明还没有Worker在运行
                existQueryList.add(query);
            }
            if (!needCreateWorker) {
                return;
            }
        }
        Task task = new Task(worker, ip, query);
        task.executeOnExecutor(ThreadPool.getExecutor());
    }

    public interface Callback {
        void onIPInfoQueryResult(Object callbackContext, Result result);
    }

    private interface Worker {
        Result execute(String ip) throws IOException;
    }

    static class Query {
        public final Callback callback;
        public final Object callbackContext;
        public final boolean callbackInWorkThread;

        public Query(Callback callback, Object callbackContext, boolean callbackInWorkThread) {
            this.callback = callback;
            this.callbackContext = callbackContext;
            this.callbackInWorkThread = callbackInWorkThread;
        }

        void notifyCallback(Result result) {
            this.callback.onIPInfoQueryResult(this.callbackContext, result);
        }
    }

    static class Task extends AsyncTask<Void, Void, Result> {

        private final Worker worker;
        private final String ip;
        private final Query query;

        public Task(Worker worker, String ip, Query query) {
            this.worker = (worker != null) ? worker : IPInfoQuery.currentWorker;
            this.ip = ip;
            this.query = query;
        }

        @Override
        protected Result doInBackground(Void... params) {
            Result result = null;
            try {
                result = this.worker.execute(ip);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            if (Logger.isLoggableDebug(TAG)) {
                Logger.d(TAG, "IPInfoQuery Result: " + StringUtils.objToString(result));
            }
            doCallback(result, true);
            return result;
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            doCallback(result, false);
        }

        private void doCallback(Result result, boolean inWorkThread) {
            if (TextUtils.isEmpty(ip)) {
                List<Query> queries = null;
                synchronized (existQueryList) {
                    int count = existQueryList.size();
                    if (count > 0) {
                        queries = new ArrayList<Query>(count);
                        for (int i = count - 1; i >= 0; --i) {
                            Query q = existQueryList.get(i);
                            if (q.callbackInWorkThread == inWorkThread) {
                                queries.add(q);
                                existQueryList.remove(i);
                            }
                        }
                    }
                }
                if (queries != null) {
                    for (Query q : queries) {
                        q.notifyCallback(result);
                    }
                }
            } else if (query.callbackInWorkThread == inWorkThread) {
                query.notifyCallback(result);
            }
        }

    }

    public static class Result {

        /**
         * {@link #ispFlags}的位掩码：电信
         */
        private static final int ISP_FLAG_MASK_CTC = (1 << 3);

        /**
         * {@link #ispFlags}的位掩码：联通
         */
        private static final int ISP_FLAG_MASK_CNC = (1 << 2);

        /**
         * {@link #ispFlags}的位掩码：移动
         */
        private static final int ISP_FLAG_MASK_CMC = (1 << 1);

        /**
         * {@link #ispFlags}的位掩码：铁通
         */
        private static final int ISP_FLAG_MASK_CRC = 1;

        public final String ip;
        public final int region;

        /**
         * ISP的位组合值
         *
         * @see #ISP_FLAG_MASK_CTC
         * @see #ISP_FLAG_MASK_CNC
         * @see #ISP_FLAG_MASK_CMC
         * @see #ISP_FLAG_MASK_CRC
         */
        public final int ispFlags;
        public final String detail;

        public Result(String ip, int region, int ispFlags, String detail) {
            this.ip = ip;
            this.region = region;
            this.ispFlags = ispFlags;
            this.detail = detail;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (!(o instanceof Result)) {
                return false;
            }
            Result other = (Result) o;
            return this.region == other.region
                && this.ispFlags == other.ispFlags
                && Misc.isEquals(this.ip, other.ip)
                && Misc.isEquals(this.detail, other.detail);
        }

        @Override
        public String toString() {
            ChinaISP chinaISP = getISP();
            return String.format("[%s, (%d.%d (%s)) (%s)]",
                ip, region, ispFlags,
                chinaISP == null ? "unknown" : Integer.toString(chinaISP.num),
                detail);
        }


        /**
         * 将{@link #ispFlags}字段，按电信、联通、移动的优先级，转换为单ISP的{@link ChinaISP}表示
         *
         * @return {@link ChinaISP}，或null
         */
        public ChinaISP getISP() {
            if (isTelecomInclude()) {
                return ChinaISP.CHINA_TELECOM;
            }
            if (isUnicomInclude()) {
                return ChinaISP.CHINA_UNICOM;
            }
            if (isMobilecomInclude() || isRailcomInclude()) {
                return ChinaISP.CHINA_MOBILE;
            }
            return null;
        }

        private boolean isTelecomInclude() {
            return isISPFlagBitSet(ISP_FLAG_MASK_CTC);
        }

        private boolean isUnicomInclude() {
            return isISPFlagBitSet(ISP_FLAG_MASK_CNC);
        }

        private boolean isMobilecomInclude() {
            return isISPFlagBitSet(ISP_FLAG_MASK_CMC);
        }

        private boolean isRailcomInclude() {
            return isISPFlagBitSet(ISP_FLAG_MASK_CRC);
        }

        private boolean isISPFlagBitSet(int mask) {
            return (this.ispFlags & mask) == mask;
        }
    }

    static class WorkerByDNS implements Worker {

        private final AddressDetermine addressDetermine;

        WorkerByDNS() {
            this(new DefaultAddressDetermine());
        }

        WorkerByDNS(AddressDetermine addressDetermine) {
            this.addressDetermine = (addressDetermine == null) ? new DefaultAddressDetermine() : addressDetermine;
        }

        @Override
        public Result execute(String ipNeedResolve) throws IOException {
            if (ipNeedResolve != null && ipNeedResolve.length() > 0) {
                // 不支持查询非本机的归属地
                throw new UnsupportedOperationException();
            }
            InetAddress inetAddress = addressDetermine.execute(Address.HostName.ISP_MAP);
            if (inetAddress == null) {
                throw new UnknownHostException();
            }
            byte[] ip = inetAddress.getAddress();
            if (ip == null || ip.length < 4) {
                return null;
            }
            if (ip[0] != -84 || ip[1] != 16) {
                // 第一字节和第二字节必须是172和16
                // 注意：byte是有符号的，这里不能与172进行比较
                // -84的二进制表示（补码）：10101100 与 无符号的172相同
                return null;
            }
            // 第3段标示地区
            // 第四段标示ISP （10：电信，11：联通，12：移动/铁通）
            int isp;
            switch (ip[3]) {
            case 10:
                isp = Result.ISP_FLAG_MASK_CTC;
                break;
            case 11:
                isp = Result.ISP_FLAG_MASK_CNC;
                break;
            case 12:
                isp = Result.ISP_FLAG_MASK_CMC;
                break;
            default:
                isp = 0;
                break;
            }
            return new Result(null, ip[2], isp, null);
        }

        interface AddressDetermine {
            InetAddress execute(String host) throws UnknownHostException;
        }

        static class DefaultAddressDetermine implements AddressDetermine {

            @Override
            public InetAddress execute(String host) throws UnknownHostException {
                return InetAddress.getByName(host);
            }
        }
    }

    private static class WorkerBySubao implements Worker {

        private final ServiceLocation serviceLocation;

        WorkerBySubao(ServiceLocation serviceLocation) {
            this.serviceLocation = serviceLocation;
        }

        @Override
        public Result execute(String ip) throws IOException {
            URL url = buildURL(ip);
            Http http = new Http(2000, 2000);
            Http.Response response = http.doGet(url, null);
            if (response.code == 200) {
                if (response.data == null || response.data.length == 0) {
                    throw new IOException("Response Code is 200, but body is null");
                }
                return parseResultFromJSON(new ByteArrayInputStream(response.data));
            }
            return null;
        }

        private URL buildURL(String ip) throws MalformedURLException {
            StringBuilder sb = new StringBuilder(128);
            sb.append("/resolve");
            if (!TextUtils.isEmpty(ip)) {
                sb.append("?ip=").append(ip);
            }
            return new URL(serviceLocation.protocol, serviceLocation.host, serviceLocation.port, sb.toString());
        }

        private Result parseResultFromJSON(InputStream inputStream) throws IOException {
            String ip = null, detail = null;
            int region = -1, ispFlags = 0;
            JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream));
            try {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if ("ip".equals(name)) {
                        ip = JsonUtils.readNextString(jsonReader);
                    } else if ("ipLib".equals(name)) {
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()) {
                            name = jsonReader.nextName();
                            if ("province".equals(name)) {
                                region = jsonReader.nextInt();
                            } else if ("operators".equals(name)) {
                                ispFlags = jsonReader.nextInt();
                            } else if ("detail".equals(name)) {
                                detail = JsonUtils.readNextString(jsonReader);
                            } else {
                                jsonReader.skipValue();
                            }
                        }
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
            } finally {
                Misc.close(jsonReader);
            }
            return new Result(ip, region, ispFlags, detail);
        }

    }
}