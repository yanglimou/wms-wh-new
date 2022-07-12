package com.tsj.common.utils;

import com.jfinal.kit.Kv;
import com.tsj.common.constant.SysConstant;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: QueryConditionBuilder
 * @description: SQL查询条件创建工厂
 * @author: Frank
 * @create: 2020-01-15 14:22
 */
public class QueryConditionBuilder extends SysConstant {
    private Kv cond;
    private boolean byWhere = false;
    private String order = "";
    private List<SqlQuery> sqlQueryList = new ArrayList<>();

    /**
     * 添加条件键值对
     *
     * @param cond
     * @return
     */
    public static QueryConditionBuilder by(Kv cond, boolean byWhere) {
        QueryConditionBuilder builder = new QueryConditionBuilder();
        builder.cond = cond;
        builder.byWhere = byWhere;
        return builder;
    }

    public QueryConditionBuilder put(String sqlPattern, String condName, String parameterName) {
        SqlQuery sqlQuery = new SqlQuery(sqlPattern, condName, parameterName);
        return put(sqlQuery);
    }

    public QueryConditionBuilder put(String sqlPattern, String condName) {
        SqlQuery sqlQuery = new SqlQuery(sqlPattern, condName);
        return put(sqlQuery);
    }

    private QueryConditionBuilder put(SqlQuery sqlQuery) {
        if (cond.containsKey(sqlQuery.getCondName()) && StringUtils.isNotEmpty(cond.getStr(sqlQuery.getCondName()))) {
            sqlQueryList.add(sqlQuery);
        }
        return this;
    }

    /**
     * 增加排序规则
     *
     * @param sqlSort       排序规格
     * @param parameterName 排序字段
     * @return
     */
    public QueryConditionBuilder order(String sqlSort, String parameterName) {
        if (order.contains("order")) {
            order = order + " , " + parameterName + " " + sqlSort;
        } else {
            order = " order by " + parameterName + " " + sqlSort;
        }
        return this;
    }

    /**
     * 返回完整的sql语句
     *
     * @return
     */
    public QueryCondition build() {
        List<Object> objectList = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();

        for (SqlQuery sqlQuery : sqlQueryList) {
            String parameterName = sqlQuery.getParameterName();
            String condName = sqlQuery.getCondName();
            String sqlPattern = sqlQuery.getSqlPattern();

            switch (sqlPattern) {
                case SQL_PATTERN_EQUAL:
                    stringBuilder.append(" and " + parameterName + " = ? ");
                    objectList.add(cond.get(condName));
                    break;

                case SQL_PATTERN_NOT_EQUAL:
                    stringBuilder.append(" and " + parameterName + " != ? ");
                    objectList.add(cond.get(condName));
                    break;

                case SQL_PATTERN_LESS:
                    stringBuilder.append(" and " + parameterName + " < ? ");
                    objectList.add(cond.getInt(condName));
                    break;

                case SQL_PATTERN_MORE:
                    stringBuilder.append(" and " + parameterName + " > ? ");
                    objectList.add(cond.getInt(condName));
                    break;

                case SQL_PATTERN_LIKE:
                    stringBuilder.append(" and " + parameterName + " like concat('%', ?, '%') ");
                    objectList.add(cond.get(condName));
                    break;

                case SQL_PATTERN_BETWEEN:
                    stringBuilder.append(" and " + parameterName + " between ? and ?");

                    String[] paraArray = null;
                    String para = cond.getStr(condName);
                    if (para.contains(" - ")) {
                        paraArray = cond.getStr(condName).split(" - ");
                    } else if (para.contains(",")) {
                        paraArray = cond.getStr(condName).split(",");
                    } else {
                        break;
                    }

                    //TODO 针对日期查询条件补全时间
                    if (condName.equals("dateRange")) {
                        if (paraArray[0].length() <= 10) {
                            paraArray[0] = paraArray[0] + " 00:00:00";
                        }
                        if (paraArray[1].length() <= 10) {
                            paraArray[1] = paraArray[1] + " 23:59:59";
                        }
                    }

                    objectList.add(paraArray[0]);
                    objectList.add(paraArray[1]);
                    break;

                case SQL_PATTERN_IN:
                    String[] split = cond.getStr(condName).split(",");
                    String aa = "";

                    for (String s : split) {
                        aa += ",?";
                        objectList.add(s);
                    }

                    stringBuilder.append(" and " + parameterName + " in (" + aa.substring(1) + ") ");
                    break;
            }

        }
        if (StringUtils.isNotEmpty(order)) {
            stringBuilder.append(order);
        }

        if (byWhere) {
            stringBuilder.insert(0, " where 1=1");
        }

        QueryCondition condition = new QueryCondition();
        condition.setSql(stringBuilder.toString());
        condition.setParas(objectList.toArray());
        return condition;
    }

    /**
     * 内部封装查询条件对象
     */
    class SqlQuery {
        private String sqlPattern;
        private String condName;
        private String parameterName;

        public String getSqlPattern() {
            return sqlPattern;
        }

        public String getCondName() {
            return condName;
        }

        public String getParameterName() {
            return parameterName;
        }

        public SqlQuery(String sqlPattern, String condName, String parameterName) {
            this.sqlPattern = sqlPattern;
            this.condName = condName;
            this.parameterName = parameterName;
        }

        public SqlQuery(String sqlPattern, String condName) {
            this.sqlPattern = sqlPattern;
            this.condName = condName;
            this.parameterName = condName;
        }
    }
}