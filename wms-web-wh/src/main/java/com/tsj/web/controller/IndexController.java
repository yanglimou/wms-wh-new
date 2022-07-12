package com.tsj.web.controller;

import com.jfinal.aop.Clear;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.constant.SysConstant;
import com.tsj.common.utils.IDGenerator;
import com.tsj.common.utils.R;
import com.tsj.domain.model.*;
import com.tsj.service.CacheService;
import com.tsj.service.SysService;
import com.tsj.common.config.CommonConfig;
import com.tsj.service.interceptor.AuthInterceptor;
import com.tsj.web.common.MyController;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: WebIndexController
 * @description: 系统登录控制器
 * @author: Frank
 * @create: 2020-07-07 09:19
 */
@Clear(AuthInterceptor.class)
public class IndexController extends MyController {

    @Inject
    private CacheService cacheService;

    @Inject
    private SysService sysService;

    @OperateLog("跳转到登录页面")
    public void index() {
        render("/login.html");
    }

    @OperateLog("用户登出")
    public void logout() {
        cacheService.removeAccessToken(getPara("accessToken"));
        renderJson(R.ok());
    }

    @NotNull({"username", "password"})
    @OperateLog("用户登录")
    public void login(String username, String password) {
        User user = sysService.getUser(Kv.by("username", username));
        if (user == null) {
            renderJson(R.error(ResultCode.USER_NOT_EXIST));
            return;
        }

        //保存人员，在日志拦截器中获取
        setAttr("user", user);

        if (!password.equals(user.getPassword())) {
            renderJson(R.error(ResultCode.USER_LOGIN_ERROR));
            return;
        }

        if (SysConstant.StateUnable.equals(user.getEnable())) {
            renderJson(R.error(ResultCode.USER_ACCOUNT_FORBIDDEN));
            return;
        }

        //生成AccessToken,有效期一天
        String accessToken = IDGenerator.makeAccessToken();
        cacheService.saveAccessToken(accessToken, user);

        //根据用户角色展示对应的菜单
        String roles = user.getRoles();
        List<String> menus = new ArrayList<>();
        if (roles.contains("Configuration")) {
            menus.add("sys");
        }
        if (roles.contains("DataQuery")) {
            menus.add("levelTwo");
        }

        Kv kv = Kv.create();
        kv.set("user", user);
        kv.set("accessToken", accessToken);
        kv.set("expiresIn", 24 * 3600);
        kv.set("wmsInitData", sysService.getDataDicList());
        kv.set("version", CommonConfig.prop.get("jfinal.serverVersion"));
        kv.set("loginState", cacheService.getLoginState(username));
        kv.set("menus", String.join(",", menus));
        renderJson(R.ok().putData(kv));
    }
}