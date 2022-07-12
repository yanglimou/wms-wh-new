package com.tsj.tcp.tcpserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 去除报文里面的%start%
 */
@Slf4j
public class TcpServerStringWrapHandler extends MessageToMessageEncoder<String> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, String message, List<Object> list) throws Exception {
        if (message != null && message.length() != 0) {
            list.add("%start%" + message + "%end%");
        }
    }
}
