package com.tsj.tcp.tcpserver;

import com.alibaba.fastjson.JSON;
import com.tsj.tcp.tcpserver.blockingmap.BlockingHashMap;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class ResponseContainer {
    private static final BlockingHashMap<String, String> map = new BlockingHashMap<>();

    public static void put(String id, String result) {
        map.put(id, result);
    }

    public static String take(String id) throws InterruptedException {
        return map.take(id, 1000L, TimeUnit.SECONDS);
    }

    public static String list() {
        return JSON.toJSONString(map.size());
    }

}
