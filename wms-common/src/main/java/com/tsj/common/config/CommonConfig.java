package com.tsj.common.config;

import com.jfinal.kit.Prop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能描述：
 *
 * @Author: aaa
 * @Date: 2021/8/22 8:57
 */
public class CommonConfig {

    public static Prop prop;

    public static Map<String, List<String>> cabinetCameraAddressMap = new HashMap<>();

    public static List<String> getCabinetCameraAddressList(String cabinetId) {
        List<String> list = new ArrayList<>();
        if (cabinetCameraAddressMap.containsKey(cabinetId)) {
            list.addAll(cabinetCameraAddressMap.get(cabinetId));
        }
        return list;
    }

}
