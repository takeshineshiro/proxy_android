package com.subao.common.data;

import com.subao.common.Misc;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 对Portal数据的封装
 */
public class PortalDataEx {

    /**
     * 完整的{@link PortalDataEx}对象序列化后至少应该有多少字节？
     */
    static final int MIN_SIZE = 24;
    static final int MAX_SIZE = 1024 * 1024 * 8;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Cache-Tag，数据来源为HTTP-Response-Header，将被存入本地文件中
     */
    public final String cacheTag;

    /**
     * 客户端版本号，数据来源为客户端（SDK或APP）的版本号，将被存入本地文件中
     */
    public final String version;

    /**
     * 数据过期时刻。UTC毫秒数（自1970.1.1以来）
     */
    private long expireTime;

    /**
     * 二进制数据本身，数据来源为HTTP-Response-Body，将被存入本地文件中
     */
    public final byte[] data;

    /**
     * 本对象中的数据，是否是服务器下载到的新数据<br />
     * true表示本对象中的数据，是因为"本地Cache-Tag与服务器不符"而重新下载到的新数据（HTTP服务器返回200而不是403）
     * <p><b>本字段不会被持久化存储</b></p>
     */
    public final boolean isNewByDownload;

    /**
     * 构造对象实例
     *
     * @param cacheTag   缓存Tag
     * @param expireTime 数据过期时刻（UTC毫秒数）
     * @param version    版本号
     * @param data       数据体
     */
    public PortalDataEx(String cacheTag, long expireTime, String version, byte[] data) {
        this(cacheTag, expireTime, version, data, false);
    }

    /**
     * 构造对象实例
     *
     * @param cacheTag        缓存Tag
     * @param expireTime      数据过期时刻（UTC毫秒数）
     * @param version         版本号
     * @param data            数据体
     * @param isNewByDownload 本对象是从网上刚下载到的新数据吗？
     */
    public PortalDataEx(String cacheTag, long expireTime, String version, byte[] data, boolean isNewByDownload) {
        this.cacheTag = cacheTag;
        this.expireTime = expireTime;
        this.version = version;
        this.data = data;
        this.isNewByDownload = isNewByDownload;
    }

    /**
     * 辅助函数，向{@link ByteBuffer}填入一个字节数组
     *
     * @param byteBuffer 目标{@link ByteBuffer}
     * @param data       要填入的字节数组，允许为null
     */
    private static void putByteArray(ByteBuffer byteBuffer, byte[] data) {
        if (data == null) {
            byteBuffer.putInt(-1);
        } else {
            byteBuffer.putInt(data.length);
            byteBuffer.put(data);
        }
    }

    /**
     * 辅助函数，从{@link ByteBuffer}读入一个字符串
     *
     * @param byteBuffer 源{@link ByteBuffer}
     * @return 读到的字符串，或null
     * @throws IOException
     */
    private static String getNextString(ByteBuffer byteBuffer) throws IOException {
        byte[] block = getNextBlock(byteBuffer);
        if (block != null) {
            return new String(block);
        } else {
            return null;
        }
    }

    /**
     * 从Buffer里读出一个Long型数
     *
     * @param byteBuffer 源
     * @return 读取到的Long型整数
     * @throws EOFException 如果Buffer里剩余的字字数不足8，抛出此异常
     */
    private static long getNextLong(ByteBuffer byteBuffer) throws EOFException {
        if (byteBuffer.remaining() >= 8) {
            return byteBuffer.getLong();
        } else {
            throw new EOFException();
        }
    }

    /**
     * 辅助函数，从{@link ByteBuffer}读入下一个块
     *
     * @param byteBuffer 源{@link ByteBuffer}
     * @return “块”。如果块长度为0，则直接返回null
     * @throws IOException
     */
    private static byte[] getNextBlock(ByteBuffer byteBuffer) throws IOException {
        if (byteBuffer.remaining() >= 4) {
            int size = byteBuffer.getInt();
            if (size == 0) {
                return EMPTY_BYTE_ARRAY;
            } else if (size < 0) {
                return null;
            } else if (byteBuffer.remaining() >= size) {
                byte[] result = new byte[size];
                System.arraycopy(byteBuffer.array(), byteBuffer.position(), result, 0, size);
                byteBuffer.position(byteBuffer.position() + size);
                return result;
            }
        }
        throw new EOFException();
    }

    /**
     * 辅助函数，取指定byte[]的长度
     *
     * @param bytes 要求长度的字节数组，允许为null
     * @return 如果指定的字节数组为null则返回0，否则为byte[].length
     */
    private static int getBytesLength(byte[] bytes) {
        return bytes == null ? 0 : bytes.length;
    }

    /**
     * 将给定的String转换为byte[]
     *
     * @param s 源字串
     * @return 如果源字串不为NULL则转换为字节数组，否则返回null
     */
    private static byte[] strToBytes(String s) {
        return s == null ? null : s.getBytes();
    }

    /**
     * 从输入流中加载，并生成{@link PortalDataEx}
     *
     * @param input 输入流
     * @return {@link PortalDataEx}
     * @throws IOException
     */
    public static PortalDataEx deserialize(InputStream input) throws IOException {
        ByteBuffer byteBuffer = instanceByteBuffer(4);
        if (input.read(byteBuffer.array()) != 4) {
            throw new EOFException();
        }
        int totalSize = byteBuffer.getInt();
        if (totalSize < MIN_SIZE || totalSize > MAX_SIZE) {
            throw new IOException("Invalid total size");
        }
        byteBuffer = instanceByteBuffer(totalSize - 4);
        if (input.read(byteBuffer.array()) != byteBuffer.limit()) {
            throw new EOFException();
        }
        String eTag = getNextString(byteBuffer);
        long expireTime = getNextLong(byteBuffer);
        String version = getNextString(byteBuffer);
        byte[] data = getNextBlock(byteBuffer);
        return new PortalDataEx(eTag, expireTime, version, data);
    }

    private static ByteBuffer instanceByteBuffer(int capacity) {
        byte[] array = new byte[capacity];
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        return byteBuffer;
    }

    private static StringBuffer appendField(StringBuffer sb, String name, String value) {
        sb.append(name).append('=');
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(value).append('"');
        }
        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof PortalDataEx)) {
            return false;
        }
        PortalDataEx other = (PortalDataEx) o;
        return this.isNewByDownload == other.isNewByDownload
            && this.expireTime == other.expireTime
            && Misc.isEquals(this.cacheTag, other.cacheTag)
            && Misc.isEquals(this.version, other.version)
            && Arrays.equals(this.data, other.data);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(256);
        sb.append('[');
        appendField(sb, "CacheTag", cacheTag);
        sb.append(", Expire=").append(expireTime);
        sb.append(", ");
        appendField(sb, "Version", version);
        sb.append(", ");
        sb.append("Data=");
        if (data == null) {
            sb.append("null");
        } else {
            sb.append(data.length);
        }
        sb.append(", new-download=").append(isNewByDownload);
        sb.append(']');
        return sb.toString();
    }

    public byte[] getData() {
        return this.data;
    }

    public int getDataSize() {
        return data == null ? 0 : data.length;
    }

    public String getCacheTag() {
        return this.cacheTag;
    }

    public String getVersion() {
        return this.version;
    }

    /**
     * 将本对象实例序列化到输出流，并<b>关闭输出流</b>
     *
     * @param output 输出流。本函数返回时将被{@link OutputStream#close()}
     * @throws IOException
     */
    public void serialize(OutputStream output) throws IOException {
        byte[] bytesOfCacheTag = strToBytes(cacheTag);
        byte[] bytesOfVersion = strToBytes(version);
        int totalSize = 4 + // 4字节的“总长度”
            4 + getBytesLength(bytesOfCacheTag) + // 4字节的块长度 + CacheTag块
            8 + // 8字节的到期时刻
            4 + getBytesLength(bytesOfVersion) + // 4字节的块长度 + 版本块
            4 + getBytesLength(data) // 4字节的块长度 + 数据块
            ;
        ByteBuffer byteBuffer = instanceByteBuffer(totalSize);
        byteBuffer.putInt(totalSize);   // 首4字节记录总大小
        putByteArray(byteBuffer, bytesOfCacheTag);
        byteBuffer.putLong(this.expireTime);
        putByteArray(byteBuffer, bytesOfVersion);
        putByteArray(byteBuffer, data);
        output.write(byteBuffer.array(), 0, byteBuffer.position());
        Misc.close(output);
    }

    public void setExpireTime(long value) {
        this.expireTime = value;
    }

    public long getExpireTime() {
        return expireTime;
    }
}
