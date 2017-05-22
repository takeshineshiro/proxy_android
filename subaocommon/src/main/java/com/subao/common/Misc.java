package com.subao.common;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Misc {

    private Misc() {}

    /**
     * 安全关闭一个{@link Cloneable}对象
     */
    public static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (RuntimeException e) {
            } catch (IOException e) {
            }
        }
    }

    /**
     * 从输入流中读入全部数据到byte数组里
     */
    public static byte[] readStreamToByteArray(InputStream input) throws IOException {
        if (input == null) {
            return null;
        }
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream(1024);
        byte[] buff = new byte[1024];
        while (true) {
            int rc = input.read(buff, 0, buff.length);
            if (rc > 0) {
                swapStream.write(buff, 0, rc);
            } else {
                break;
            }
        }
        return swapStream.toByteArray();
    }

    /**
     * 从一个缓冲区里解析出整型值来
     *
     * @param buffer       缓冲区
     * @param start        起始位置(include)
     * @param end          结束位置(exclude)
     * @param defaultValue 如果解析失败，用此值作为函数返回值
     */
    public static long extractLong(byte[] buffer, int start, int end, long defaultValue) {
        if (end > buffer.length) {
            end = buffer.length;
        }
        int index = start;
        while (index < end) {
            int ch = buffer[index];
            if (Character.isDigit(ch)) {
                long result = ch - '0';
                ++index;
                while (index < end) {
                    ch = buffer[index];
                    if (Character.isDigit(ch)) {
                        result = result * 10 + (ch - '0');
                    } else {
                        break;
                    }
                    ++index;
                }
                return result;
            }
            ++index;
        }
        return defaultValue;
    }

    /**
     * 判断给定的UID是否为Application的UID
     */
    public static boolean isApplicationsUID(int uid) {
        return uid >= android.os.Process.FIRST_APPLICATION_UID && uid <= android.os.Process.LAST_APPLICATION_UID;
    }

    public static <T> boolean isEquals(T t1, T t2) {
        if (t1 == t2) {
            return true;
        }
        if (t1 == null || t2 == null) {
            return false;
        }
        return t1.equals(t2);
    }

    public static boolean isEquals(Float f1, Float f2) {
        if (f1 == f2) {
            return true;
        }
        if (f1 == null || f2 == null) {
            return false;
        }
        return Float.compare(f1, f2) == 0;
    }

    /**
     * 判断给定的输入值，如果给定的值为null，则返回给定的替换值，否则直接返回输入值本身
     *
     * @param src                      输入值
     * @param replacementWhenSrcIsNull 替换值
     * @param <T>
     * @return 如果输入值为null，则返回替换值，否则返回输入值本身
     */
    public static <T> T transNullValue(T src, T replacementWhenSrcIsNull) {
        return (src == null) ? replacementWhenSrcIsNull : src;
    }

    /**
     * 对给定的字符串，使用UTF8进行URL编码
     *
     * @param value 给定的字符串
     * @return 按URL规范进行编码后的字串
     */
    public static String encodeUrl(String value) {
        return encodeUrl(value, "UTF-8");
    }

    /**
     * 对给定的字符串，用给定的字符集进行URL编码
     *
     * @param value       给定的字符串
     * @param charsetName 字符集
     * @return 按URL规范进行编码后的字串
     */
    public static String encodeUrl(String value, String charsetName) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            return URLEncoder.encode(value, charsetName);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
