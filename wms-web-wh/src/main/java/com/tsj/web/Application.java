package com.tsj.web;

import com.jfinal.server.undertow.UndertowServer;
import com.tsj.web.common.MyConfig;

/**
 * @className: Application
 * @description: 主运行程序
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
public class Application {
    public static void main(String[] args) {
        UndertowServer.create(MyConfig.class, "config/undertow.txt").start();
    }
}
