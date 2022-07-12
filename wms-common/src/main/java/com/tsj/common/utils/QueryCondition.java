package com.tsj.common.utils;

/**
 * SQL查询条件
 */
public class QueryCondition {
    private String sql;
    private Object[] paras;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Object[] getParas() {
        return paras;
    }

    public void setParas(Object[] paras) {
        this.paras = paras;
    }
}
