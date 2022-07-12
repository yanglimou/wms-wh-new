package com.tsj.web.common;

import com.jfinal.core.Controller;
import com.jfinal.log.Log;
import com.tsj.domain.model.User;

/**
 * @className: WebController
 * @description: 控制器基类
 * @author: Frank
 * @create: 2020-07-08 17:26
 */
public class MyController extends Controller {
    protected static final Log logger = Log.getLog(MyController.class);

    /**
     * 获取登录用户ID
     *
     * @return
     */
    protected String getLoginUserId() {
        User loginUser = getAttr("user");
        if (loginUser == null) {
            return "";
        } else {
            return loginUser.getId();
        }
    }

}