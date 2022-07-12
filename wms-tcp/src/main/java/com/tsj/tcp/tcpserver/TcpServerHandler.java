package com.tsj.tcp.tcpserver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tsj.tcp.tcpserver.business.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@ChannelHandler.Sharable
@Slf4j
public class TcpServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.debug("有新的客户端连接到tcp服务器");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String message) throws Exception {
        log.debug("收到客户端消息：{}", message);
        JSONObject jsonObject = JSON.parseObject(message);
        String order = jsonObject.getString("order");
        switch (order) {
            case "heart":
                Heart.run(channelHandlerContext, jsonObject);
                break;
            case "power":
                Power.run(channelHandlerContext, jsonObject);
                break;
            case "product":
                Product.run(channelHandlerContext, jsonObject);
                break;
            case "total":
                Total.run(channelHandlerContext, jsonObject);
                break;
            case "person":
                Person.run(channelHandlerContext, jsonObject);
                break;
            default:
                Other.run(channelHandlerContext, jsonObject);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
