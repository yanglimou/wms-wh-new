package com.tsj.tcp.tcpserver.business;

import com.alibaba.fastjson.JSONObject;
import com.tsj.tcp.tcpserver.ChannelContainer;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Heart {
    public static void run(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject) {
        String code = jsonObject.getString("code");
        log.debug("收到{}耗材柜的心跳指令", code);
        ChannelContainer.put(code, channelHandlerContext.channel());
        jsonObject.remove("code");
        jsonObject.remove("data");
        jsonObject.put("message", "0");
        String result = jsonObject.toJSONString();
        log.debug("心跳响应：{}", result);
        channelHandlerContext.writeAndFlush(result);
    }
}
