package com.subao.common.utils;

import java.util.Calendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class CalendarUtils {

    public static final long MILLISECONDS_PER_DAY = 1000L * 3600 * 24;
    public static final long MILLISECONDS_OFFSET_TIMEZONE_BEIJIN = 1000L * 3600 * 8;

    public static final TimeZone TIME_ZONE_OF_UTC = new SimpleTimeZone(0, "UTC");
    public static final TimeZone TIME_ZONE_OF_BEIJING = new SimpleTimeZone((int) MILLISECONDS_OFFSET_TIMEZONE_BEIJIN, "CST");

    /**
     * 毫秒数转换为天数
     *
     * @param milliseconds 毫秒数
     * @return 天数
     */
    public static int dayFrom_Milliseconds(long milliseconds) {
        return (int) (milliseconds / MILLISECONDS_PER_DAY);
    }

    public static int dayFrom_CalendarLocal(Calendar c) {
        return dayFrom_Milliseconds(c.getTimeInMillis() + c.get(Calendar.ZONE_OFFSET));
    }

    public static int todayUTC() {
        return dayFrom_Milliseconds(System.currentTimeMillis());
    }

    public static int todayLocal() {
        return dayFrom_Milliseconds(System.currentTimeMillis() + TimeZone.getDefault().getRawOffset());
    }

    /**
     * 返回北京时间的“今天”
     */
    public static int todayCST() {
        return dayFrom_Milliseconds(System.currentTimeMillis() + MILLISECONDS_OFFSET_TIMEZONE_BEIJIN);
    }

    public static Calendar nowOfCST() {
        return Calendar.getInstance(TIME_ZONE_OF_BEIJING, Locale.CHINA);
    }

    public static Calendar calendarLocal_FromMilliseconds(long msUTC) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(msUTC);
        return c;
    }

    public static Calendar calendarFromDays(long days) {
        return calendarLocal_FromMilliseconds(days * MILLISECONDS_PER_DAY - TimeZone.getDefault().getRawOffset());
    }

    /**
     * 给定两个UNIX毫秒数（自UTC的1970.1.1以来的毫秒数），判断它们的北京时间是否在同一天
     *
     * @param millisOfUTC_1 UTC毫秒数
     * @param millisOfUTC_2 UTC毫秒数
     * @return true 表示同一天，false表示不是同一天
     */
    public static boolean isSameDayOfCST(long millisOfUTC_1, long millisOfUTC_2) {
        int day1 = CalendarUtils.dayFrom_Milliseconds(millisOfUTC_1 + MILLISECONDS_OFFSET_TIMEZONE_BEIJIN);
        int day2 = CalendarUtils.dayFrom_Milliseconds(millisOfUTC_2 + MILLISECONDS_OFFSET_TIMEZONE_BEIJIN);
        return day1 == day2;
    }

    /**
     * 给定两个UNIX毫秒数（自UTC的1970.1.1以来的毫秒数），判断它们的北京时间是否在同一自然月份
     *
     * @param millisOfUTC_1 UTC毫秒数
     * @param millisOfUTC_2 UTC毫秒数
     * @return true 表示同一月，false表示不是同一月
     */
    public static boolean isSameMonthOfCST(long millisOfUTC_1, long millisOfUTC_2) {
        // 如果相差大于31天，则肯定不在同一月份内
        long delta = Math.abs(millisOfUTC_1 - millisOfUTC_2);
        if (delta > MILLISECONDS_PER_DAY * 31) {
            return false;
        }
        // 计算年月
        Calendar calendar = Calendar.getInstance(TIME_ZONE_OF_BEIJING);
        calendar.setTimeInMillis(millisOfUTC_1);
        int month1 = calendar.get(Calendar.MONTH);
        calendar.setTimeInMillis(millisOfUTC_2);
        int month2 = calendar.get(Calendar.MONTH);
        return month1 == month2;    // 不用判断年份了（如果月份相同，相差又小于等于31天，肯定不会跨年）
    }

    /**
     * {@link #calendarToString(Calendar, int)}的format参数的位域值。
     * <p><b>指明输出结果里包含年月日</b></p>
     */
    public static final int FORMAT_DATE = 1;

    /**
     * {@link #calendarToString(Calendar, int)}的format参数的位域值。
     * <p><b>指明输出结果里包含时分秒</b></p>
     */
    public static final int FORMAT_TIME = 2;

    /**
     * {@link #calendarToString(Calendar, int)}的format参数的位域值。
     * <p><b>指明输出结果里包含时区信息</b></p>
     */
    public static final int FORMAT_ZONE = 4;

    /**
     * 将指定的{@link Calendar}转换成字符串
     *
     * @param calendar 指定的{@link Calendar}
     * @param format   指定格式，其值为{@link #FORMAT_DATE}、{@link #FORMAT_TIME}和{@link #FORMAT_ZONE}的位组合
     * @return 转换后的字符串，形如 <i>"yyyy-mm-dd hh:mm:ss +8"</i>
     */
    public static String calendarToString(Calendar calendar, int format) {
        StringBuilder sb = new StringBuilder(64);
        boolean hasDate = (format & FORMAT_DATE) != 0;
        if (hasDate) {
            appendIntToStringBuilder(sb, calendar.get(Calendar.YEAR)).append('-');
            appendIntToStringBuilder(sb, calendar.get(Calendar.MONTH) + 1).append('-');
            appendIntToStringBuilder(sb, calendar.get(Calendar.DAY_OF_MONTH));
        }
        boolean hasTime = (format & FORMAT_TIME) != 0;
        if (hasTime) {
            if (hasDate) {
                sb.append(' ');
            }
            appendIntToStringBuilder(sb, calendar.get(Calendar.HOUR_OF_DAY)).append(':');
            appendIntToStringBuilder(sb, calendar.get(Calendar.MINUTE)).append(':');
            appendIntToStringBuilder(sb, calendar.get(Calendar.SECOND));
        }
        if ((format & FORMAT_ZONE) != 0) {
            if (hasDate || hasTime) {
                sb.append(' ');
            }
            int offset = calendar.get(Calendar.ZONE_OFFSET) / (1000 * 3600);
            if (offset >= 0) {
                sb.append('+');
            }
            sb.append(offset);
        }
        return sb.toString();
    }

    private static StringBuilder appendIntToStringBuilder(StringBuilder sb, int value) {
        if (value < 10) {
            sb.append('0');
        }
        return sb.append(value);
    }

}
