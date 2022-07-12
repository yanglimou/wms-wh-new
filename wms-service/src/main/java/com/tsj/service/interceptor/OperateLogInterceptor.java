package com.tsj.service.interceptor;

import com.alibaba.fastjson.JSON;
import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import com.jfinal.kit.Kv;
import com.jfinal.render.JsonRender;
import com.tsj.common.annotation.OperateLog;
import com.tsj.common.constant.ResultCode;
import com.tsj.common.utils.IpUtil;
import com.tsj.common.utils.R;
import com.tsj.domain.model.User;
import com.tsj.common.event.LogEvent;
import net.dreamlu.event.EventKit;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * @className: OperateLogInterceptor
 * @description: 操作日志统一处理
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
public class OperateLogInterceptor implements Interceptor {

    @Override
    public void intercept(Invocation inv) {
        Instant startTime = Instant.now();
        String errorMessage = null;
        try {
            inv.invoke();
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);

            errorMessage = sw.toString();
            inv.getController().renderJson(R.error(ResultCode.SERVER_ERROR));
        }

        OperateLog annotation = inv.getMethod().getAnnotation(OperateLog.class);
        if (annotation != null) {
            int millis = (int) Duration.between(startTime, Instant.now()).toMillis();
            getOperateLog(annotation, inv, errorMessage, millis);
        }
    }

    public void getOperateLog(OperateLog annotation, Invocation inv, String errorMessage, int millis) {
        Controller controller = inv.getController();
        if (controller.getRender() instanceof JsonRender) {
            JsonRender jsonRender = (JsonRender) controller.getRender();
            Kv kv = JSON.parseObject(jsonRender.getJsonText(), Kv.class);
            if (StringUtils.isEmpty(errorMessage) && kv.getInt("code") != 0) {
                errorMessage = kv.getStr("msg");
            }

            String controllerKey = inv.getControllerKey();
            String methodName = inv.getMethodName();
            if (controllerKey.equals("/")) {
                controllerKey = "";
            }
            String url = controllerKey + "/" + methodName;

            String ip = IpUtil.getIpAddr(controller.getRequest());
            String method = controller.getRequest().getMethod();

            String createUserId = "";
            User loginUser = controller.getAttr("user");
            if (loginUser != null) {
                createUserId = loginUser.getId();
            } else {
                createUserId = controller.getPara("userId");
            }

            Map<String, String[]> paraMap = controller.getParaMap();

            EventKit.post(new LogEvent(annotation.value(), ip, url, method, paraMap, errorMessage, createUserId, millis));
        }
    }
}