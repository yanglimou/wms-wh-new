package com.tsj.tcp.tcpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpServer {
    private int port;
    private String host;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public TcpServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void onStart() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();

                            //编码 string—>byte
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

                            //发送出去的消息增加开始标识和结束标识
                            pipeline.addLast(new TcpServerStringWrapHandler());

                            //结束标识 %end%
                            pipeline.addLast(new DelimiterBasedFrameDecoder(1024 * 4, Unpooled.copiedBuffer("%end%", CharsetUtil.UTF_8)));
                            //解码 byte->string
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            //开始标识
                            pipeline.addLast(new TcpServerSubstringHandler());
                            //具体处理
                            pipeline.addLast(new TcpServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelFuture = serverBootstrap.bind(host, port).sync();
            log.debug("tcp server start,port:{}", port);
//            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) channelFuture1 -> onStop());
        } catch (Exception exception) {
            log.error("tcp server error", exception);
        }
    }

    public void onStop() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
