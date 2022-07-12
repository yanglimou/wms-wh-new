package com.tsj.common.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;

/**
 * @Author Carl
 * @DATE 2019/3/1
 */
public class DateUtils {

    private static final double hourSecondConstant = 3600000;

    public static long getTimestamp() {
        Long milliSecond = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        return milliSecond;
    }

    public static double calHourDiffByTimestamp(long start, long end) {
        double hour = (end - start) / hourSecondConstant;
        return hour;
    }

    public static String getCurrentTimeToFileName() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");//设置日期格式
        return df.format(new Date());
    }

    public static String getCurrentTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        return df.format(new Date());
    }

    public static String getCurrentDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
        return df.format(new Date());
    }

    /**
     * 当前日期推迟月份
     *
     * @param month
     * @return
     */
    public static String addMonth(String s, int month) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            Calendar cd = Calendar.getInstance();
            cd.setTime(sdf.parse(s));
            cd.add(Calendar.MONTH, month);//增加一个月

            return sdf.format(cd.getTime());

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 当前日期推迟天数
     *
     * @param day
     * @return
     */
    public static String addDay(String s, int day) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Calendar cd = Calendar.getInstance();
            cd.setTime(sdf.parse(s));
            cd.add(Calendar.DATE, day);

            return sdf.format(cd.getTime());

        } catch (Exception e) {
            return null;
        }
    }

    public static String beginEndtime(String dt1, String dt2) {
        //将用户输入的日期字符串转换为date类
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date begin = null;
        Date end = null;
        try {
            begin = sdf.parse(dt1);
            end = sdf.parse(dt2);
            //计算时间差
            long diff = end.getTime() - begin.getTime();
            //计算天数
            long days = diff / (1000 * 60 * 60 * 24);
            //计算小时
            long hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
            //计算分钟
            long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
            //计算秒
            long seconds = (diff % (1000 * 60)) / 1000;
            //输出
            return days + "天" + hours + "小时" + minutes + "分" + seconds + "秒";
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前月份的天数
     *
     * @return
     */
    public static int getDaysOfMonth(String dateStr) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(dateStr));
            return c.getActualMaximum(Calendar.DAY_OF_MONTH);
        } catch (Exception ex) {
            return 0;
        }
    }

    public static boolean compare(String dt1, String dt2) {
        if (!dt1.contains(":")) {
            dt1 += " 00:00:00";
        }
        if (!dt2.contains(":")) {
            dt2 += " 00:00:00";
        }

        //将用户输入的日期字符串转换为date类
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            //计算时间差
            long diff = sdf.parse(dt1).getTime() - sdf.parse(dt2).getTime();
            return diff > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public static long getSeconds(String dt1, String dt2) {
        //将用户输入的日期字符串转换为date类
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date begin = sdf.parse(dt1);
            Date end = sdf.parse(dt2);
            //计算时间差
            long diff = end.getTime() - begin.getTime();
            return diff / 1000;
        } catch (ParseException exception) {
            return -1;
        }
    }

}
