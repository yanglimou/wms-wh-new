package com.tsj.common.utils;

import java.util.Random;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

/**
 * MD5加密类
 *
 * @Author 陈昆鹏
 * @DATE 2017/11/16
 */
public class EncryptMd5 {

  public static final String DEFAULT_PWD = "BJT123456";

  /**
   * 生成含有随机盐的密码
   */
  public static String generate(String password) {
    Random r = new Random();
    StringBuilder sb = new StringBuilder(16);
    sb.append(r.nextInt(99999999)).append(r.nextInt(99999999));
    int len = sb.length();
    if (len < 16) {
      for (int i = 0; i < 16 - len; i++) {
        sb.append("0");
      }
    }
    String salt = sb.toString();
    password = md5Hex(password + salt);
    char[] cs = new char[48];
    for (int i = 0; i < 48; i += 3) {
      cs[i] = password.charAt(i / 3 * 2);
      char c = salt.charAt(i / 3);
      cs[i + 1] = c;
      cs[i + 2] = password.charAt(i / 3 * 2 + 1);
    }
    return new String(cs);
  }

  /**
   * 校验密码是否正确
   */
  public static boolean verify(String password, String oriPwd) {
    char[] cs1 = new char[32];
    char[] cs2 = new char[16];
    for (int i = 0; i < 48; i += 3) {
      cs1[i / 3 * 2] = oriPwd.charAt(i);
      cs1[i / 3 * 2 + 1] = oriPwd.charAt(i + 2);
      cs2[i / 3] = oriPwd.charAt(i + 1);
    }
    String salt = new String(cs2);
    return md5Hex(password + salt).equals(new String(cs1));
  }

}
