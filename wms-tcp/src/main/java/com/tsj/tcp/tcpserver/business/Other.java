package com.tsj.tcp.tcpserver.business;

import com.alibaba.fastjson.JSONObject;
import com.tsj.tcp.tcpserver.ChannelContainer;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Other {
    public static void run(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject) {
        log.debug("其他指令，直接丢弃");
    }
}
