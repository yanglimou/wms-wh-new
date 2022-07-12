package com.tsj.common.event;

import java.util.Map;

/**
 * @className: LogEvent
 * @description: 日志事件
 * @author: Frank
 * @create: 2020-03-13 15:23
 */
public class LogEvent {
    private String module;
    private String ip;
    private String url;
    private String method;
    private Map<String, String[]> paraMap;
    private String errorMessage;
    private String createUserId;
    private int executeTime;

    public LogEvent(String module, String ip, String url, String method, Map<String, String[]> paraMap, String errorMessage, String createUserId, int executeTime) {
        this.module = module;
        this.ip = ip;
        this.url = url;
        this.method = method;
        this.paraMap = paraMap;
        this.errorMessage = errorMessage;
        this.createUserId = createUserId;
        this.executeTime = executeTime;
    }

    public String getModule() {
        return module;
    }

    public String getIp() {
        return ip;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String[]> getParaMap() {
        return paraMap;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public int getExecuteTime() {
        return executeTime;
    }
}