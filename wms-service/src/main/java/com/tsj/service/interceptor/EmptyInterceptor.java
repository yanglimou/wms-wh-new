package com.tsj.service.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import com.tsj.common.annotation.NotNull;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.utils.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: EmptyInterceptor
 * @description: 参数不能为空注解验证
 * @author: Frank
 * @create: 2020-04-07 10:53
 */
public class EmptyInterceptor implements Interceptor {

    @Override
    public void intercept(Invocation inv) {
        NotNull annotation = inv.getMethod().getAnnotation(NotNull.class);
        if (annotation != null) {
            noEmpty(annotation, inv);
        } else {
            inv.invoke();
        }
    }

    public void noEmpty(NotNull annotation, Invocation inv) {
        Controller controller = inv.getController();
        List<String> blankParameterList = new ArrayList<>();
        for (String v : annotation.value()) {
            String parameter = controller.getRequest().getParameter(v);
            if (parameter == null || parameter.trim().length() == 0) {
                blankParameterList.add(v);
            }
        }

        if (blankParameterList.isEmpty()) {
            inv.invoke();
        } else {
            controller.renderJson(R.error(ResultCode.PARAM_IS_BLANK).putData(blankParameterList));
        }
    }
}
