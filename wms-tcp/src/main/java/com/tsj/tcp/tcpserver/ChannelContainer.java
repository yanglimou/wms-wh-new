package com.tsj.tcp.tcpserver;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChannelContainer {
    private static final ConcurrentHashMap<String, Channel> map = new ConcurrentHashMap();

    private ChannelContainer() {

    }

    public static void put(String id, Channel channel) {
        map.put(id, channel);
    }

    public static Channel get(String id) {
        return map.get(id);
    }

    public static void remove(String id, Channel channel) {
        map.remove(id);
    }

    public static void sendAll(String body) {
        map.forEach((s, channel) -> {
            if (channel.isWritable()) {
                log.debug("给【{}】耗材柜发送指令：{}", s, body);
                channel.writeAndFlush(body);
            }
        });
    }
}
