package com.tsj.common.netty;

import com.jfinal.log.Log;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.Charset;


/**
 * @className: NettyServer
 * @description: Netty通信服务
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
public class NettyServer {
    protected Log logger = Log.getLog(this.getClass());
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workGroup = new NioEventLoopGroup();

    private Channel tcpChannel;

    public void bind(int port, MyNetty myNetty) {
        ChannelFuture f = null;
        try {
            ServerBootstrap sb = new ServerBootstrap();
            sb.option(ChannelOption.SO_BACKLOG, 1024);
            sb.group(workGroup, bossGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new StringEncoder(Charset.forName("UTF-8")));
                            ch.pipeline().addLast(new NettyHandler(myNetty));
                            ch.pipeline().addLast(new ByteArrayEncoder());
                        }
                    });
            ChannelFuture cf = sb.bind(port).syncUninterruptibly();
            tcpChannel = cf.channel();

            logger.info(" monitor: {}", tcpChannel.localAddress());

            // 等待服务端监听端口关闭
            tcpChannel.closeFuture().syncUninterruptibly();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    public void destroy() {
        if (tcpChannel != null) {
            tcpChannel.close();
        }
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

}
