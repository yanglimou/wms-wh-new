package com.tsj.common.utils;

/**
 * @Author Carl
 * @DATE 2018/1/12
 */
public class DecodeUtils {

  public static String byteToHex(Byte arg) {
    StringBuffer bf = new StringBuffer();
    int v = (int) ((short) arg & 0x00FF);
    String hv = Integer.toHexString(v);
    if (hv.length() < 2) {
      bf.append("0");
    }
    bf.append(hv);

    return bf.toString().toUpperCase();
  }


}
