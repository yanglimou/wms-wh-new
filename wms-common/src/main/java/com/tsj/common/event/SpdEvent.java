package com.tsj.common.event;

import com.alibaba.fastjson.JSONObject;
import com.tsj.common.constant.SpdUrl;

/**
 * @className: SpdEvent
 * @description: SPD事件
 * @author: Frank
 * @create: 2020-04-01 14:53
 */
public class SpdEvent {

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(int executeTime) {
        this.executeTime = executeTime;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    private String module;
    private String method;
    private String url;
    private JSONObject jsonObject;
    private String errorMessage;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    private String result;
    private int executeTime;

    public SpdEvent(String method, String url, JSONObject jsonObject, String errorMessage, String result, int executeTime) {
        this.method = method;
        this.url = url;
        this.jsonObject = jsonObject;
        this.errorMessage = errorMessage;
        this.result = result;
        this.executeTime = executeTime;

        SpdUrl spdUrl = SpdUrl.fromUrl(url);
        if (spdUrl != null) {
            this.method = spdUrl.getModule();
        }
    }
}