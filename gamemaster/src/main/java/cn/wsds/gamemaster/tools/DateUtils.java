package cn.wsds.gamemaster.tools;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static final String SERVER_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static long string2long(String time , String format){
        long outTime = 0 ;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date date = sdf.parse(time);
            outTime = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return outTime ;
    }


    public static String longToDate(Long date , String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date d = new Date(date);
        return sdf.format(d);
    }


    public static Calendar long2Calender(long time){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar ;
    }

    public static Calendar string2Calendar(String time , String format){

        return long2Calender(string2long(time,format)) ;
    }
}
