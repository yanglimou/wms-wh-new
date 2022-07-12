package com.tsj.tcp.tcpserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 去除报文里面的%start%
 */
@Slf4j
public class TcpServerSubstringHandler extends SimpleChannelInboundHandler<String> {


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        if (msg != null && msg.indexOf("%start%")>=0) {
            String message = msg.substring(msg.indexOf("%start%")+7);
            channelHandlerContext.fireChannelRead(message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
