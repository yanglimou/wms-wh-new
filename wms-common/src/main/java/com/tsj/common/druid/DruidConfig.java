package com.tsj.common.druid;

import com.jfinal.plugin.druid.IDruidStatViewAuth;

import javax.servlet.http.HttpServletRequest;

/**
 * @className: DruidConfig
 * @description: 配置druid的进入规则，暂定为只要登录就可查看
 * @author: Frank
 * @create: 2020-03-13 15:23
 */
public class DruidConfig implements IDruidStatViewAuth {
    @Override
    public boolean isPermitted(HttpServletRequest request) {
//        String token = BaseUtil.getToken(request);
//        return Redis.use().exists(token);

        return  true;
    }
}
