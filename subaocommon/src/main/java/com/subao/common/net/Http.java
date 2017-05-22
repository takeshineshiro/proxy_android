package com.subao.common.net;

import android.annotation.SuppressLint;
import android.util.Log;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

@SuppressLint("DefaultLocale")
public class Http {

    //	public static final String CACHE_CONTROL_NO_CACHE = "no-cache";
    public static final String CACHE_IF_NONE_MATCH = "If-None-Match";
    public static final String CACHE_IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String TAG = LogTag.NET;
    private static StrictVerifier strictVerifier = new DefaultStrictVerifier();
    private final int connectTimeout, readTimeout;

    /**
     * @param connectTimeout 连接超时，单位毫秒
     * @param readTimeout    读超时，单位毫秒
     */
    public Http(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * 设置认证
     */
    public static void setStrictVerifier(StrictVerifier strictVerifier) {
        Http.strictVerifier = (strictVerifier == null) ? new DefaultStrictVerifier() : strictVerifier;
    }

    public static URL createURL(String url) throws IOException {
        try {
            return new URL(url);
        } catch (RuntimeException e) {
            throw new NetIOException();
        }
    }

    public static URL createHttpURL(String host, int port, String file) throws IOException {
        try {
            return new URL("http", host, port, file);
        } catch (RuntimeException e) {
            throw new NetIOException();
        }
    }

    /**
     * 从一个URLConnection里读入数据（服务器返回的）
     *
     * @param connection {@link URLConnection}
     * @return 从服务器得到的数据，或null
     * @throws IOException
     */
    public static Response readDataFromURLConnection(HttpURLConnection connection) throws IOException {
        try {
            int code = getResponseCode(connection);
            byte[] data;
            InputStream in = null;
            try {
                in = connection.getInputStream();
                data = Misc.readStreamToByteArray(in);
            } catch (IOException e) {
                data = null;
            } finally {
                Misc.close(in);
            }
            if (data == null) {
                in = connection.getErrorStream();
                if (in != null) {
                    try {
                        data = Misc.readStreamToByteArray(in);
                    } finally {
                        Misc.close(in);
                    }
                }
            }
            try {
                if (Logger.isLoggableDebug(TAG)) {
                    Log.d(TAG, String.format("[%s] response: code=%d, data size=%d",
                        connection.getURL().toString(),
                        code,
                        data == null ? 0 : data.length));
                }
            } catch (Exception e) {
            }
            return new Response(code, data);
        } catch (IOException e) {
            printExceptionLog(connection, e);
            throw e;
        } catch (RuntimeException e) {
            // 在某个坑爹的联想手机上面，会抛NullPointer异常
            printExceptionLog(connection, e);
            throw new IOException(e.getMessage());
        }
    }

    private static void printExceptionLog(HttpURLConnection connection, Exception e) {
        if (Logger.isLoggableDebug(TAG)) {
            URL url = connection.getURL();
            if (url != null) {
                Logger.w(TAG, url.toString());
            }
            Logger.w(TAG, e.getMessage());
        }
    }

    public static Response doGet(HttpURLConnection connection) throws IOException {
        writeHttpRequestLog(connection);
        try {
            return readDataFromURLConnection(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void writeHttpRequestLog(HttpURLConnection connection) {
        if (Logger.isLoggable(TAG, Log.DEBUG)) {
            Logger.d(TAG, String.format("Try HTTP request (%s): %s", connection.getRequestMethod(), connection.getURL().toString()));
        }
    }

    /**
     * 在给定的{@link HttpURLConnection}上执行post操作（不对数据进行压缩）
     *
     * @param connection {@link HttpURLConnection}
     * @param postData   如果不为空则长度大于零，此数据将被Post到服务器上
     * @return {@link Response}
     * @throws IOException
     */
    public static Response doPost(HttpURLConnection connection, byte[] postData) throws IOException {
        return doPost(connection, postData, false);
    }

    /**
     * 在给定的{@link HttpURLConnection}上执行post操作（不对数据进行压缩）
     *
     * @param connection {@link HttpURLConnection}
     * @param postData   如果不为空则长度大于零，此数据将被Post到服务器上
     * @param compress   是否需要对Post的数据进行GZIP压缩
     * @return {@link Response}
     * @throws IOException
     */
    private static Response doPost(HttpURLConnection connection, byte[] postData, boolean compress) throws IOException {
        writeHttpRequestLog(connection);
        if (postData != null && postData.length > 0) {
            if (compress) {
                connection.setRequestProperty("Content-Encoding", "gzip");
            } else {
                connection.setFixedLengthStreamingMode(postData.length);
            }
            connection.setDoOutput(true);
            OutputStream output = null;
            try {
                output = connection.getOutputStream();
                if (compress) {
                    GZIPOutputStream gzip = new GZIPOutputStream(output);
                    gzip.write(postData);
                    //noinspection Since15
                    gzip.flush();
                    gzip.finish();
                } else {
                    output.write(postData);
                    output.flush();
                }
            } catch (RuntimeException e) {
                throw new NetIOException();
            } finally {
                Misc.close(output);
            }
        }
        return readDataFromURLConnection(connection);
    }

    public static void setRequestContentType(HttpURLConnection conn, String contentType) {
        if (contentType != null) {
            conn.setRequestProperty("Content-Type", contentType);
        }
    }

    public static void setRequestContentType(HttpURLConnection conn, ContentType contentType) {
        if (contentType != null) {
            setRequestContentType(conn, contentType.str);
        }
    }

    public static void setRequestAccept(HttpURLConnection conn, String accept) {
        if (accept != null) {
            conn.setRequestProperty("Accept", accept);
        }
    }

    public static void setRequestAccept(HttpURLConnection conn, ContentType contentType) {
        if (contentType != null) {
            conn.setRequestProperty("Accept", contentType.str);
        }
    }

    public static int getResponseCode(HttpURLConnection connection) throws IOException {
        int code;
        try {
            try {
                code = connection.getResponseCode();
            } catch (IOException e) {
                // 某些手机在第一次getResponseCode()时，会触发 IOException
                // “No authentication challenges found”
                code = connection.getResponseCode();
            }
        } catch (RuntimeException e) {
            // 在一个奇葩手机上，会抛NumberFormatException
            code = -1;
        }
        if (code < 0) {
            throw new IOException("No valid response code.");
        }
        return code;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    /**
     * 根据指定的方法建立{@link HttpURLConnection}
     * <p>
     * 如果指定的不是GET方法，则创建出来的对象会setDoOutput(true)
     * </p>
     *
     * @param url         {@link URL}
     * @param method      {@link Method}
     * @param contentType 请求内容的类型，用于填充请求头的ContentType字段。如果指定null则不填充
     * @return {@link HttpURLConnection}对象
     * @throws IOException
     */
    public HttpURLConnection createHttpUrlConnection(URL url, Method method, String contentType) throws IOException {
        if (url == null) {
            throw new NullPointerException("URL is null");
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                HttpsUtils.SSLParams sslParams = HttpsUtils.createSSLParams(null, null, null);
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                httpsConn.setHostnameVerifier(new HttpsUtils.SafeHostnameVerifier(strictVerifier));
                httpsConn.setSSLSocketFactory(sslParams.socketFactory);
            }
            if (method != null) {
                conn.setRequestMethod(method.str);
            }
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(true);
            setRequestContentType(conn, contentType);
            conn.setRequestProperty("Connection", "Close");
            return conn;
        } catch (RuntimeException e) {
            throw new IOException("网络权限被禁用");
        }
    }

    /**
     * 向指定的URL发起GET请求
     *
     * @param url                URL
     * @param requestContentType 请求头里的ContentType字段值，为null则不填
     * @throws IOException
     * @see #createHttpUrlConnection(URL, Method, String)
     */
    public Response doGet(URL url, String requestContentType) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = createHttpUrlConnection(url, Method.GET, requestContentType);
            return doGet(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 向指定的URL，Post一段二进制数据（不压缩）
     *
     * @param url      向哪个url发送？
     * @param postData 要post的二进制数据
     * @return 成功返回服务器应答的二进制数据， 失败返回null
     * @throws IOException
     */
    public Response doPost(URL url, byte[] postData, String contentType) throws IOException {
        return doPost(url, postData, contentType, false);
    }

    /**
     * 向指定的URL，Post一段二进制数据
     *
     * @param url      向哪个url发送？
     * @param postData 要post的二进制数据
     * @param compress 是否需要对Post的数据进行GZIP压缩
     * @return 成功返回服务器应答的二进制数据， 失败返回null
     * @throws IOException
     */
    public Response doPost(URL url, byte[] postData, String contentType, boolean compress) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = createHttpUrlConnection(url, Method.POST, contentType);
            return doPost(connection, postData, compress);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 直接访问给定的URL，并取得其Response Code
     */
    public int getHttpResponseCode(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = createHttpUrlConnection(url, Method.GET, null);
        return getResponseCode(connection);
    }

    /**
     * HTTP方法
     */
    public enum Method {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE");

        public final String str;

        Method(String s) {
            this.str = s;
        }
    }

    public enum ContentType {
        ANY("*"),
        HTML("text/html"),
        JSON("application/json"),
        PROTOBUF("application/x-protobuf");

        public final String str;

        ContentType(String s) {
            this.str = s;
        }
    }

    /**
     * 自定义验证主机名
     */
    public interface StrictVerifier {
        /**
         * 验证主机名是否通过
         *
         * @param hostname 主机名
         * @return true: 验证hostname通过<br />
         * false: 验证hostname失败
         */
        boolean verify(String hostname);
    }

    static class DefaultStrictVerifier implements StrictVerifier {
        @Override
        public boolean verify(String hostname) {
            if (hostname == null || hostname.length() == 0) {
                return false;
            }
            //TODO 修正
            if ("api.xunyou.mobi".equals(hostname) || "uat.xunyou.mobi".equals(hostname)) {
                return false;
            }
            return true; //null != IPv4.parseIp(hostname);
        }
    }

    /**
     * 服务器的应答数据
     */
    public static class Response {
        /**
         * HTTP应答码
         */
        public final int code;

        /**
         * 内容
         */
        public final byte[] data;

        public Response(int code, byte[] data) {
            this.code = code;
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format("[Response: Code=%d, Data Length=%d])", code, data == null ? 0 : data.length);
        }
    }

    public static class HttpResponseCodeGetter {

        private final static String REQUEST = "GET / HTTP/1.1\r\nHost: %s\r\nConnection: keep-alive\r\nAccept: text/html\r\n\r\n";

        private static int parse(byte[] buf, int begin, int end) throws IOException {
            int i = begin;
            while (i < end) {
                byte b = buf[i++];
                if (b == 0x20) {
                    break;
                }
            }
            if (i >= end) {
                throw new IOException();
            }
            int result = 0;
            int j = i;
            while (j < end) {
                byte b = buf[j];
                if (b < '0' || b > '9') {
                    break;
                }
                result = result * 10 + (b - '0');
                ++j;
            }
            if (i == j) {
                throw new IOException();
            }
            return result;
        }

        private static ByteBuffer readFromSocket(Selector selector, SocketChannel channel, long readTimeout, int bytes) throws IOException {
            ByteBuffer buffer;
            buffer = ByteBuffer.allocate(bytes + 1);
            while (buffer.position() < bytes) {
                if (1 != selector.select(readTimeout)) {
                    throw new IOException("Read timeout");
                }
                selector.selectedKeys().clear();
                if (channel.read(buffer) <= 0) {
                    throw new IOException("Read failed");
                }
            }
            return buffer;
        }

        public static int execute(String host, int port, long connectTimeout, long readTimeout) throws IOException {
            Selector selector = null;
            SocketChannel channel = null;
            try {
                selector = Selector.open();
                channel = SocketChannel.open();
                return parseResponseCode(selector, channel, host, port, connectTimeout, readTimeout);
            } catch (RuntimeException e) {
                throw new IOException();
            } finally {
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (Exception e) {
                    }
                }
                if (selector != null) {
                    try {
                        selector.close();
                    } catch (Exception e) {
                    }
                }
            }
        }

        private static int parseResponseCode(Selector selector, SocketChannel channel, String host, int port, long connectTimeout, long readTimeout)
            throws IOException {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_CONNECT);
            // 连接
            SocketAddress addr = new InetSocketAddress(host, port);
            channel.connect(addr);
            if (1 != selector.select(connectTimeout)) {
                throw new IOException("Connect timeout");
            }
            channel.finishConnect();
            selector.selectedKeys().clear();
            // 发送请求
            ByteBuffer request = ByteBuffer.wrap(String.format(REQUEST, host).getBytes());
            while (request.remaining() > 0) {
                if (channel.write(request) <= 0) {
                    throw new IOException("Send request failed");
                }
            }
            // 读
            channel.register(selector, SelectionKey.OP_READ);
            ByteBuffer buffer = readFromSocket(selector, channel, readTimeout, 32);
            return parse(buffer.array(), 0, buffer.position());
        }

    }
}
