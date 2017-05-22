package com.subao.common.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

public class StringUtils {

    public static final String EMPTY = "";
    public static final String STRING_NULL = "null";

    public static boolean isStringEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }

    /**
     * 判断两个字符串是否全等。注意：null和empty是不等的!（参考{@code isStringSame}）
     *
     * @param s1
     * @param s2
     * @return
     */
    public static boolean isStringEqual(CharSequence s1, CharSequence s2) {
        if (s1 == s2) {
            return true;
        }
        // null is not equal empty !!!
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
    }

    /**
     * 判断两个字符串是否相同。注意：null和empty是相同的！（参考{@code isStringEqual}）
     *
     * @param s1
     * @param s2
     * @return
     */
    public static boolean isStringSame(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        // null is same as empty !!!
        boolean s1_empty = isStringEmpty(s1);
        boolean s2_empty = isStringEmpty(s2);
        if (s1_empty) {
            return s2_empty;
        }
        if (s2_empty) {
            return s1_empty;
        }
        return s1.equals(s2);
    }

    /**
     * 比较两个字符串。规则如下：<br />
     * * 两个字符串为同一对象，返回0<br />
     * * 否则当前一字符串为null时，返回-1<br />
     * * 否则当后一字会串为null时，返回1<br />
     * * 否则返回s1.compareTo(s2)
     *
     * @return 比较结果
     */
    public static int compare(String s1, String s2) {
        if (s1 == s2) {
            return 0;
        }
        if (s1 == null) {
            return -1;
        }
        if (s2 == null) {
            return 1;
        }
        return s1.compareTo(s2);
    }

    /**
     * 辅助函数：将一个不超过15的值转成16进制表示
     *
     * @param halfByte 值不大于15的无符号值
     * @param a        字符A的ASCII码（如果要换成小写，就用'a'，要换成大写，就用'A'）
     * @return '0'~'9'或'A'~'F'（视参数a是大写还是小写）
     */
    private static char halfByteToChar(int halfByte, int a) {
        if (halfByte < 10) {
            return (char) ('0' + halfByte);
        } else {
            return (char) (a + (halfByte - 10));
        }
    }

    /**
     * 将给定bytes数组转成可打印的Hex字符串
     *
     * @param input     Bytes数组
     * @param start     从哪个位置开始？
     * @param end       到哪个位置结束？
     * @param upperCase 是否使用大写字母？
     * @return 如果input为空或start不小于end，返回""，否则返回转换后的字符串
     */
    public static String toHexString(byte[] input, int start, int end, boolean upperCase) {
        if (input == null || start >= end || input.length == 0 || start >= input.length) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder(input.length << 1);
        return toHexString(sb, input, start, end, upperCase ? 'A' : 'a').toString();
    }

    /**
     * 等同于toHexString(input, 0, input.length, upperCase)
     *
     * @see #toHexString(byte[], int, int, boolean)
     */
    public static String toHexString(byte[] input, boolean upperCase) {
        if (input == null || input.length == 0) {
            return EMPTY;
        }
        return toHexString(input, 0, input.length, upperCase);
    }

    private static StringBuilder toHexString(StringBuilder sb, byte[] input, int start, int end, char a) {
        for (int i = start; i < end; ++i) {
            byte b = input[i];
            sb.append(halfByteToChar((b >> 4) & 0xf, a));
            sb.append(halfByteToChar(b & 0xf, a));
        }
        return sb;
    }

    public static String toGUIDString(byte[] input, boolean upperCase) {
        if (input == null || input.length != 16) {
            return "";
        }
        char a = upperCase ? 'A' : 'a';
        StringBuilder sb = new StringBuilder(input.length * 2 + 4);
        toHexString(sb, input, 0, 4, a).append('-');
        toHexString(sb, input, 4, 6, a).append('-');
        toHexString(sb, input, 6, 8, a).append('-');
        toHexString(sb, input, 8, 10, a).append('-');
        toHexString(sb, input, 10, 16, a);
        return sb.toString();
    }

    public static byte[] guidStringToByteArray(String guidString) {
        if (guidString == null) {
            return null;
        }
        return guidStringToByteArray(guidString, 0, guidString.length());
    }

    public static byte[] guidStringToByteArray(String guidString, int start, int end) {
        if (guidString == null || end - start != 36) {
            return null;
        }
        byte[] result = new byte[16];
        try {
            int idx = hexStringToByteArray(guidString, 0, 8, result, 0);
            idx = hexStringToByteArray(guidString, 9, 13, result, idx);
            idx = hexStringToByteArray(guidString, 14, 18, result, idx);
            idx = hexStringToByteArray(guidString, 19, 23, result, idx);
            hexStringToByteArray(guidString, 24, 36, result, idx);
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }

    private static int hexStringToByteArray(String hexString, int start, int end, byte[] target, int idxTarget) {
        while (start < end) {
            char high = hexString.charAt(start++);
            char low = hexString.charAt(start++);
            int v = numCharToByte(high, low);
            target[idxTarget++] = (byte) v;
        }
        return idxTarget;
    }

    private static int numCharToByte(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        throw new NumberFormatException();
    }

    private static int numCharToByte(char high, char low) {
        int h = numCharToByte(high);
        int l = numCharToByte(low);
        return (h << 4) | l;
    }

    public static int parsePositiveInteger(String s, int start, int end) {
        int result = 0;
        for (int i = start; i < end; ++i) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') {
                result = result * 10 + (ch - '0');
            } else {
                return -1;
            }
        }
        return result;
    }

    public static int parsePositiveInteger(String s) {
        return parsePositiveInteger(s, 0, s.length());
    }

    public static int compareVersion(String v1, String v2) {
        if (v1 == null) {
            if (v2 == null) {
                return 0;
            } else {
                return -1;
            }
        }
        if (v2 == null) {
            return 1;
        }
        //
        String[] v1Fields = v1.split("\\.");
        String[] v2Fields = v2.split("\\.");
        int len1 = v1Fields.length;
        int len2 = v2Fields.length;
        int maxIndex = Math.min(len1, len2);
        for (int i = 0; i < maxIndex; ++i) {
            String f1 = v1Fields[i];
            String f2 = v2Fields[i];
            try {
                int value1 = Integer.parseInt(f1);
                int value2 = Integer.parseInt(f2);
                if (value1 < value2) {
                    return -1;
                } else if (value1 > value2) {
                    return 1;
                }
            } catch (NumberFormatException e) {
                int n = f1.compareTo(f2);
                if (n != 0) {
                    return n;
                }
            }
        }
        if (len1 < len2) {
            return -1;
        } else if (len1 > len2) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 将指定容器里的每一项，以指定分隔符分隔，依次序列化
     * <p>
     * 每项里如果有逗号，将被前缀以'\\'字符进行转义，单个的'\\'也将被转义
     * </p>
     *
     * @throws IOException
     */
    public static <T> void serializeList(Writer writer, Iterable<T> list, char separtor) throws IOException {
        boolean first = true;
        for (T t : list) {
            if (t == null) {
                continue;
            }
            String s = t.toString();
            if (isStringEmpty(s)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                writer.append(separtor);
            }
            for (int i = 0; i < s.length(); ++i) {
                char ch = s.charAt(i);
                if (ch == separtor || ch == '\\') {
                    writer.append('\\');
                }
                writer.append(ch);
            }
        }
    }

    public static <T> void serializeList(Writer writer, Iterable<T> list) throws IOException {
        serializeList(writer, list, ',');
    }


    /**
     * 从Reader里解析以指定分隔符分隔的各项，写入到指定的list里
     *
     * @return 本次总共解析了多少项？
     * @throws IOException
     */
    public static int deserializeList(Reader reader, List<String> list, char separtor) throws IOException {
        int oldListSize = list.size();
        StringBuilder sb = new StringBuilder(32);
        while (true) {
            int n = reader.read();
            if (n < 0) {
                break;
            }
            char ch = (char) n;
            if (ch == '\\') {
                // 遇到斜杠符，判断是否是转义
                int next = reader.read();
                if (next < 0) {
                    // 结束了，添加这个字符，并将本项加入list，然后跳出循环
                    sb.append(ch);
                    break;
                }
                ch = (char) next;
                if (ch != '\\' && ch != separtor) {
                    sb.append('\\');
                }
                sb.append(ch);
            } else if (ch == separtor) {
                // 遇到分隔符了，解析出一个字段
                if (sb.length() > 0) {
                    list.add(sb.toString());
                    sb.delete(0, sb.length());
                }
            } else {
                // 非分隔符，直接添加
                sb.append(ch);
            }
        }
        // 别忘了加入最后一项
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        // 返回“解析了多少项”
        return list.size() - oldListSize;
    }

    public static int deserializeList(Reader reader, List<String> list) throws IOException {
        return deserializeList(reader, list, ',');
    }

    public static String objToString(Object obj) {
        return obj == null ? STRING_NULL : obj.toString();
    }

    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return STRING_NULL;
        }
        int total = bytes.length;
        int count = Math.min(8, total);
        StringBuilder sb = new StringBuilder(128);
        sb.append('[');
        for (int i = 0; i < count; ++i) {
            sb.append("0x");
            toHexString(sb, bytes, i, i + 1, 'A');
            if (i < count - 1) {
                sb.append(", ");
            } else {
                break;
            }
        }
        if (count < total) {
            sb.append(", ... (Total ").append(total).append(" bytes)");
        }
        sb.append(']');
        return sb.toString();
    }
}
