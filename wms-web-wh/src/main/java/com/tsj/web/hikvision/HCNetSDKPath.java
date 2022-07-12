package com.tsj.web.hikvision;

import com.tsj.common.constant.FileConstant;

import java.io.UnsupportedEncodingException;

public class HCNetSDKPath {

    public static String DLL_PATH;

    static {
        try {
            DLL_PATH = java.net.URLDecoder.decode(FileConstant.HIKVISION_PATH.replace("/", "\\"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}