package com.tsj.service.interceptor;

import com.jfinal.aop.Inject;
import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.utils.R;
import com.tsj.domain.model.User;
import com.tsj.service.CacheService;

/**
 * 登录拦截器
 */
public class AuthInterceptor implements Interceptor {

    @Inject
    private CacheService cacheService;

    @Override
    public void intercept(Invocation ai) {
        Controller controller = ai.getController();

        //将人员信息返回给controller
        String accessToken = controller.getPara("accessToken");
        User user = cacheService.getUserByToken(accessToken);
        if (user == null) {
            controller.renderJson(R.error(ResultCode.TOKEN_INVALID, "请重新登录"));
            return;
        }
        controller.setAttr("user", user);

        //调用目标接口
        ai.invoke();
    }
}