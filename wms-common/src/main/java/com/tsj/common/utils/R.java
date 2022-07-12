package com.tsj.common.utils;

import com.tsj.common.constant.ResultCode;

import java.util.LinkedHashMap;

/**
 * 统一API返回json工具类
 */
public class R extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public void setResultCode(ResultCode code) {
        this.put("code", code.getCode());
        this.put("msg", code.getMessage());
    }

    public static R ok() {
        R res = new R();
        res.setResultCode(ResultCode.SUCCESS);
        return res;
    }

    public static R error(ResultCode errorCode, String... errorMsg) {
        R r = new R();
        r.setResultCode(errorCode);
        if (errorMsg != null && errorMsg.length > 0) {
            r.put("msg", errorCode.getMessage() + "，" + errorMsg[0]);
        }
        return r;
    }

    @Override
    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * 设置默认返回对象data
     *
     * @param data 返回对象
     * @return
     */
    public R putData(Object data) {
        this.put("data", data);
        return this;
    }

    public boolean isSuccess() {
        return super.containsKey("code") && super.get("code").equals(0);
    }
}
