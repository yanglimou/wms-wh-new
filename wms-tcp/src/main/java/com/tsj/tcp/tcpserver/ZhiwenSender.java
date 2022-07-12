package com.tsj.tcp.tcpserver;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;

public class ZhiwenSender {
    public static String send(String body) throws Exception {
        //增加下发指令
//        String body="{\n" +
//                "            \"order\": \"person\",\n" +
//                "                \"number\": \"32 位字符串\",\n" +
//                "                \"type\": \"1\",\n" +
//                "                \"code\": \"编号\",\n" +
//                "                \"tzz\": \"指纹的特征值\"\n" +
//                "        }";
        String number = JSON.parseObject(body).getString("number");
        String id = JSON.parseObject(body).getString("code");
        Channel channel = ChannelContainer.get(id);
        channel.writeAndFlush(body);
        //阻塞等待结果
        try {
            //阻塞等待结果
            return ResponseContainer.take(number);
            //具体操作？？
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("发送失败");
        }
    }
}
