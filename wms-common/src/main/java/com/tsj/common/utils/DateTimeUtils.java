package com.tsj.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Carl
 * @DATE 2017/12/28
 */
public class DateTimeUtils {

  public static final String SEC_MOD = "yyyy-MM-dd HH:mm:ss";
  private static final String SEC_MOD_WS = "yyyyMMddHHmmss";
  private static final String DATE_MOD_WS = "yyyyMMdd";
  /**
   * 锁对象
   */
  private static final Object LOCK_OBJ = new Object();
  private static Logger logger = LoggerFactory.getLogger(DateTimeUtils.class);
  /**
   * 存放不同的日期模板格式的sdf的Map
   */
  private static Map<String, ThreadLocal<SimpleDateFormat>> SDF_MAP = new HashMap<String, ThreadLocal<SimpleDateFormat>>();

  /**
   * 返回一个ThreadLocal的sdf,每个线程只会new一次sdf
   */
  private static SimpleDateFormat getSdf(final String pattern) {
    ThreadLocal<SimpleDateFormat> tl = SDF_MAP.get(pattern);

    // 此处的双重判断和同步是为了防止sdfMap这个单例被多次put重复的sdf
    if (tl == null) {
      synchronized (LOCK_OBJ) {
        tl = SDF_MAP.get(pattern);
        if (tl == null) {
          // 只有Map中还没有这个pattern的sdf才会生成新的sdf并放入map
          logger.info("put new sdf of pattern " + pattern + " to map");

          // 这里是关键,使用ThreadLocal<SimpleDateFormat>替代原来直接new SimpleDateFormat
          tl = new ThreadLocal<SimpleDateFormat>() {

            @Override
            protected SimpleDateFormat initialValue() {
              return new SimpleDateFormat(pattern);
            }
          };
          SDF_MAP.put(pattern, tl);
        }
      }
    }

    return tl.get();
  }

  public static Date getDateFromString(String ts) throws ParseException {
    if (!StringUtils.isEmpty(ts)) {
      Date result = getSdf(SEC_MOD_WS).parse(ts);
      return result;
    } else {
      return null;
    }
  }

  /**
   * 获取当前时间的字符串格式
   *
   * @Author 陈昆鹏
   * @DATE 2017/12/28
   */
  public static String getDateTimeStringWithoutSeparator() {
    String dateString = getSdf(SEC_MOD_WS).format(new Date());
    return dateString;
  }

  /**
   * 通过时间戳（精确到秒）获取当前时间的字符串格式
   *
   * @Author 陈昆鹏
   * @DATE 2017/12/28
   */
  public static String getDateTimeStringWithoutSeparator(String timestamp) {
    Date date = new Date(Long.parseLong(timestamp) * 1000);
    String dateString = getSdf(SEC_MOD_WS).format(date);
    return dateString;
  }

  /**
   * 获取当前日期的字符串格式
   *
   * @Author 陈昆鹏
   * @DATE 2017/12/28
   */
  public static String getDateStringWithoutSeparator() {
    Date date = new Date();
    String dateString = getSdf(DATE_MOD_WS).format(date);
    return dateString;
  }

  /**
   * 根据传入格式返回时间字符串
   */
  public static String getString(String pattern) {
    if (StringUtils.isEmpty(pattern)) {
      pattern = SEC_MOD_WS;
    }
    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
    String dateString = sdf.format(new Date());
    return dateString;
  }

  public static String revertTimeFromStringWithoutSeparator(String time) {
    String result = null;
    if (!StringUtils.isEmpty(time)) {
      try {
        Date date = getSdf(SEC_MOD_WS).parse(time);
        result = getSdf(SEC_MOD).format(date);
      } catch (ParseException e) {
        logger.error("time parse error:" + e.toString());
      }
    }
    return result;
  }


}
