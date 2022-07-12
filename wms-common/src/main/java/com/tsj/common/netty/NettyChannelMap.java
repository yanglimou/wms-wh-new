package com.tsj.common.netty;

import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * note connect channel
 *
 * @author Frank
 * @DATE 2018/1/29
 */
public class NettyChannelMap {
    private static Map<String, SocketChannel> map = new ConcurrentHashMap<String, SocketChannel>();

    public static void add(String clientId, SocketChannel socketChannel) {
        if (map.containsKey(clientId)) {
            map.remove(clientId);
        }
        map.put(clientId, socketChannel);
    }

    public static Channel get(String clientId) {
        return map.get(clientId);
    }

    public static void remove(SocketChannel socketChannel) {
        String deviceId = null;
        for (Map.Entry entry : map.entrySet()) {
            if (entry.getValue() == socketChannel) {
                deviceId = (String) entry.getKey();
                map.remove(entry.getKey());
                break;
            }
        }
    }
}