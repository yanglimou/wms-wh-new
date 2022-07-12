package com.tsj.common.utils;

import com.jfinal.log.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MyKit {
    private static Log logger = Log.getLog(MyKit.class);

    /**
     * 获取某日期区间的所有日期  日期倒序
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param dateFormat 日期格式
     * @return 区间内所有日期
     */
    public static List<String> getPerDaysByStartAndEndDate(String startDate, String endDate, String dateFormat) {
        DateFormat format = new SimpleDateFormat(dateFormat);
        try {
            Date sDate = format.parse(startDate);
            Date eDate = format.parse(endDate);
            long start = sDate.getTime();
            long end = eDate.getTime();
            if (start > end) {
                return null;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eDate);
            List<String> res = new ArrayList<String>();
            while (end >= start) {
                res.add(format.format(calendar.getTime()));
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                end = calendar.getTimeInMillis();
            }
            Collections.reverse(res);
            return res;
        } catch (ParseException e) {
            logger.error(e.toString());
        }

        return null;
    }

    /**
     * 寻找一串自然数中缺失的数字
     *
     * @param src        数组
     * @param maxElement 数组中最大元素
     * @return
     */
    public static int getMissingNumber(int[] src, int maxElement) {
        int[] arrays = new int[maxElement + 1];
        for (int value : src) {
            arrays[value] = 1;
        }

        for (int i = 0, length = arrays.length; i < length; i++) {
            if (arrays[i] == 0) {
                return i;
            }
        }
        return maxElement + 1;
    }

    public static void main(String[] args) {
        int[] arr = new int[]{1, 10, 7, 4};
        //System.out.println(Arrays.stream(arr).max().getAsInt());
        //System.out.println(Arrays.stream(arr).min().getAsInt());
        System.out.println(getMissingNumber(arr, Arrays.stream(arr).max().getAsInt()));
    }
}
