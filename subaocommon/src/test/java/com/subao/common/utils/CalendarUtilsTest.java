package com.subao.common.utils;

import android.annotation.SuppressLint;

import org.junit.Test;

import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by hujd on 16-6-7.
 */
public class CalendarUtilsTest {

    private void doTestSameDay(int year, int month, int day) {
        Calendar c = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_BEIJING);
        c.set(2001, month, day, 0, 0, 0);
        long millis = c.getTimeInMillis();
        assertTrue(CalendarUtils.isSameDayOfCST(millis, millis + 3600 * 1000 * 24 - 1000));
        assertFalse(CalendarUtils.isSameDayOfCST(millis, millis + 3600 * 1000 * 24));
        assertFalse(CalendarUtils.isSameDayOfCST(millis, millis - 1000));
    }

    @Test
    public void testSameDay() {
        for (int month = 0; month < 12; ++month) {
            for (int day = 1; day < 32; ++day) {
                int maxDay;
                switch (month) {
                case 0:
                case 2:
                case 4:
                case 6:
                case 7:
                    maxDay = 31;
                case 1:
                    maxDay = 28;
                default:
                    maxDay = 30;
                }
                if (day <= maxDay) {
                    doTestSameDay(1999, month, day);
                } else {
                    break;
                }

            }
        }
        // 测试闰年
        doTestSameDay(2004, 1, 29);
    }

    @Test
    public void testSameDayUTC() {
        // UTC是同一天的，北京时间可能不同天
        Calendar c1 = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_UTC);
        c1.set(2000, 1, 1, 15, 0, 0);    // UTC的15点=CST的23点
        Calendar c2 = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_UTC);
        c2.set(2000, 1, 1, 17, 0, 0);    // UTC的17点=CST第二天凌晨1点
        assertFalse(CalendarUtils.isSameDayOfCST(c1.getTimeInMillis(), c2.getTimeInMillis()));
    }

    @Test
    public void testIsSameMonth() {
        for (int year = 1999; year <= 2001; ++year) {
            for (int month = 0; month < 12; ++month) {
                Calendar c1 = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_BEIJING);
                c1.set(year, month, 1);
                Calendar c2 = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_BEIJING);
                c2.set(year, month, 20);
                assertTrue(CalendarUtils.isSameMonthOfCST(c1.getTimeInMillis(), c2.getTimeInMillis()));
                c2.set(year, (month + 1) % 12, 1);
                assertFalse(CalendarUtils.isSameMonthOfCST(c1.getTimeInMillis(), c2.getTimeInMillis()));
            }
        }
        //
        Calendar c1 = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_BEIJING);
        c1.set(2000, 11, 31);
        Calendar c2 = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_BEIJING);
        c2.set(2001, 11, 1);
        assertFalse(CalendarUtils.isSameMonthOfCST(c1.getTimeInMillis(), c2.getTimeInMillis()));
        c2.set(2000, 11, 1);
        assertTrue(CalendarUtils.isSameMonthOfCST(c1.getTimeInMillis(), c2.getTimeInMillis()));
    }

    private static int calcDays(long milliseconds) {
        return (int) (milliseconds / (1000L * 3600 * 24));
    }

    @Test
    public void testDayFrom_CalendarLocal() {
        Calendar c = Calendar.getInstance();
        int day = calcDays(c.getTimeInMillis() + c.get(Calendar.ZONE_OFFSET));
        assertEquals(day, CalendarUtils.dayFrom_CalendarLocal(c));
    }

    @Test
    public void testToDay() {
        CalendarUtils.todayLocal();
        int utc = CalendarUtils.todayUTC();
        int cst = CalendarUtils.todayCST();
        assertTrue(utc == cst || cst - 1 == utc);
    }

    @Test
    public void testNowOfCST() {
        Calendar c1 = CalendarUtils.nowOfCST();
        Calendar c2 = Calendar.getInstance(new SimpleTimeZone(1000 * 3600 * 8, "BeiJing"));
        int field = Calendar.YEAR;
        assertEquals(c2.get(field), c1.get(field));
        field = Calendar.MONTH;
        assertEquals(c2.get(field), c1.get(field));
        field = Calendar.DAY_OF_MONTH;
        assertEquals(c2.get(field), c1.get(field));
        field = Calendar.HOUR;
        assertEquals(c2.get(field), c1.get(field));
        field = Calendar.MINUTE;
        assertEquals(c2.get(field), c1.get(field));
    }

    @Test
    public void testCalendarLocal_FromMilliseconds() {
        Calendar c = CalendarUtils.calendarLocal_FromMilliseconds(0);
        assertEquals(1970, c.get(Calendar.YEAR));
        assertEquals(0, c.get(Calendar.MONTH));
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testCalendarFromDays() {
        Calendar c = CalendarUtils.calendarFromDays(0);
        assertEquals(1970, c.get(Calendar.YEAR));
        assertEquals(0, c.get(Calendar.MONTH));
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testConstructor() {
        new CalendarUtils();
    }

    @SuppressLint("DefaultLocale")
    @Test
    public void testCalendarToString() {
        int year = 2016;
        int hour = 19;
        int minute = 59;
        int second = 31;
        for (int zone = -11; zone <= 11; ++zone) {
            for (int month = 0; month <= 11; ++month) {
                for (int day = 1; day < 25; ++day) {
                    TimeZone timeZone = new SimpleTimeZone(zone * 3600 * 1000, "test");
                    Calendar calendar = Calendar.getInstance(timeZone);
                    calendar.set(2016, month, day, hour, minute, second);
                    //
                    int format = CalendarUtils.FORMAT_DATE;
                    String s = CalendarUtils.calendarToString(calendar, format);
                    assertEquals(String.format("%04d-%02d-%02d", year, month + 1, day), s);
                    format |= CalendarUtils.FORMAT_TIME;
                    s = CalendarUtils.calendarToString(calendar, format);
                    assertEquals(String.format("%04d-%02d-%02d %02d:%02d:%02d",
                        year, month + 1, day, hour, minute, second), s);
                    format |= CalendarUtils.FORMAT_ZONE;
                    s = CalendarUtils.calendarToString(calendar, format);
                    assertEquals(String.format("%04d-%02d-%02d %02d:%02d:%02d %+d",
                        year, month + 1, day, hour, minute, second, zone), s);
                    //
                    // 无年月日
                    format = CalendarUtils.FORMAT_TIME;
                    s = CalendarUtils.calendarToString(calendar, format);
                    assertEquals(String.format("%02d:%02d:%02d", hour, minute, second), s);
                    format |= CalendarUtils.FORMAT_ZONE;
                    s = CalendarUtils.calendarToString(calendar, format);
                    assertEquals(String.format("%02d:%02d:%02d %+d", hour, minute, second, zone), s);
                    //
                    // 无时分秒
                    format = CalendarUtils.FORMAT_DATE | CalendarUtils.FORMAT_ZONE;
                    s = CalendarUtils.calendarToString(calendar, format);
                    assertEquals(String.format("%04d-%02d-%02d %+d", year, month + 1, day, zone), s);
                    //
                    // 只有时区
                    format = CalendarUtils.FORMAT_ZONE;
                    s = CalendarUtils.calendarToString(calendar, format);
                    assertEquals(String.format("%+d", zone), s);
                    //
                    // 什么都没有
                    assertEquals("", CalendarUtils.calendarToString(calendar, 0));
                }
            }
        }
    }
}
