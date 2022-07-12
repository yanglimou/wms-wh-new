package com.tsj.tcp.tcpserver.business;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.kit.Kv;
import com.tsj.common.utils.QueryCondition;
import com.tsj.common.utils.QueryConditionBuilder;
import com.tsj.domain.model.User;
import com.tsj.domain.model.UserFinger;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.tsj.common.constant.SysConstant.SQL_PATTERN_EQUAL;
import static com.tsj.common.constant.SysConstant.SQL_PATTERN_LIKE;

@Slf4j
public class Power {
    public static User getUser(Kv cond) {
        cond.set("enable", "true");
        cond.set("roles", "GoodsManage");
        QueryCondition condition = QueryConditionBuilder.by(cond, true)
                .put(SQL_PATTERN_EQUAL, "username")
                .put(SQL_PATTERN_EQUAL, "password")
                .put(SQL_PATTERN_EQUAL, "id")
                .put(SQL_PATTERN_EQUAL, "code")
                .put(SQL_PATTERN_EQUAL, "enable")
                .put(SQL_PATTERN_LIKE, "roles")
                .build();

        String select = "select * from base_user";
        return User.dao.findFirst(select + condition.getSql(), condition.getParas());
    }

    public static void run(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject) {
        log.debug("权限获取");
        //耗材柜编号
        String code = jsonObject.getString("code");
        //1为刷指纹，2为刷卡，3为账号和密码
        String type = jsonObject.getString("type");
        //若type为1或2，则data直接发送指纹编号或卡号（卡号为十六进制）。若type为3，则data发送”账号+密码”（账号和密码中间以+号连接）。
        String data = jsonObject.getString("data");
        jsonObject.remove("code");
        jsonObject.remove("type");
        jsonObject.remove("data");
        jsonObject.put("message", "0");
        log.debug("收到{}耗材柜的获取权限指令", code);
        User user = null;
        if ("2".equals(type) || "3".equals(type)) {
            Kv kv = null;
            switch (type) {
                case "2"://刷卡
                    log.debug("刷卡");
                    kv = Kv.by("code", data);
                    break;
                case "3"://用户名+密码
                    log.debug("用户名+密码");
                    String[] strings = data.split("\\+");
                    kv = Kv.by("username", strings[0]).set("password", strings[1]);
                    break;
            }
            user = getUser(kv);
        } else if ("1".equals(type)) {//指纹
            log.debug("指纹");
            UserFinger userFinger = UserFinger.dao.findFirst("SELECT * FROM sys_user_finger WHERE fingerValue=?", data);
            if (userFinger != null) {
                user = getUser(Kv.by("id", userFinger.getFingerUserId()));
            }
        }
        JSONObject userData = new JSONObject();
        if (user != null) {
            log.debug("查询出来的用户信息：{}", user.toJson());
            userData.put("name", user.getName());
            userData.put("code", user.getId());
            userData.put("card", data);
            userData.put("type", type);
        } else {
            log.debug("用户不存在或信息有误或无权限");
            userData.put("name", "");
            userData.put("code", "");
            userData.put("card", "");
            userData.put("type", "");
        }
        jsonObject.put("data", userData);
        String result = jsonObject.toJSONString();
        log.debug("获取权限响应：{}", result);
        channelHandlerContext.writeAndFlush(result);
    }
}
