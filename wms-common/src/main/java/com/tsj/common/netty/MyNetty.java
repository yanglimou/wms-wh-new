package com.tsj.common.netty;

import io.netty.channel.Channel;

/**
 * @className: MyNetty
 * @description: Netty基础类
 * @author: Frank
 * @create: 2020-03-23 16:03
 */
public interface MyNetty {

    void ChannelRead(Channel channel, byte[] data);

    void ChannelClose(Channel channel);

}