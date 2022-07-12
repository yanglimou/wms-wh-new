package com.tsj.tcp.tcpserver.business;

import com.alibaba.fastjson.JSONObject;
import com.tsj.tcp.tcpserver.ChannelContainer;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
public class Person {
    public static void deleteAll() {
        send("3", null, null);
    }

    public static void deleteOnePerson(List<String> fingerNoList) {
        fingerNoList.forEach(fingerNo -> send("2", fingerNo, null));
    }

    public static void addOne(String fingerNo, String fingerValue) {
        send("2", fingerNo, null);
        send("1", fingerNo, fingerValue);
    }

    /**
     * type 1为添加指纹，2为删除指纹，3为删除所有指纹
     * code 指纹编号
     * tzz 指纹特征值
     */

    private static void send(String type, String code, String tzz) {
        /**
         * {
         * 	"order": "person",
         * 	"number": "32 位字符串",
         * 	"type": "1",
         * 	"code": "编号",
         * 	"tzz": "指纹的特征值"
         * }
         */

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("order", "person");
        jsonObject.put("number", UUID.randomUUID().toString());
        jsonObject.put("type", type);
        jsonObject.put("code", StringUtils.isEmpty(code) ? "" : code);
        jsonObject.put("tzz", StringUtils.isEmpty(tzz) ? "" : tzz);
        String body = jsonObject.toJSONString();
        log.debug("给所有在线耗材柜发送指纹配置指令:{}", body);
        ChannelContainer.sendAll(body);
    }

    public static void run(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject) {
        log.debug("指纹下发结果：{}", jsonObject.toJSONString());
        //下发结果不处理
    }
}
