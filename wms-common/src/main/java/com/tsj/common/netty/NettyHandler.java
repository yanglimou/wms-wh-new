package com.tsj.common.netty;

import com.jfinal.log.Log;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @className: NettyHandler
 * @description: Netty通信处理器
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
public class NettyHandler extends ChannelInboundHandlerAdapter {
    protected Log logger = Log.getLog(this.getClass());
    private MyNetty myNetty;

    public NettyHandler(MyNetty myNetty) {
        this.myNetty = myNetty;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().localAddress().toString() + " 通道已激活！");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().localAddress().toString() + " 通道不活跃！");
        myNetty.ChannelClose(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] data = new byte[((ByteBuf) msg).readableBytes()];
        myNetty.ChannelRead(ctx.channel(), data);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//    System.out.println("服务端接收数据完毕..");
//    // 第一种方法：写一个空的buf，并刷新写出区域。完成后关闭sock channel连接。
//    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.error("异常信息：\r\n" + cause.getMessage());
        cause.printStackTrace();
    }
}