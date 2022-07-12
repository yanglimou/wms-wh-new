package com.tsj.common.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
/**
 * @className: OperateLog
 * @description: 操作日志注解
 * @author: Frank
 * @create: 2020-03-13 15:23
 */
public @interface OperateLog {
    /**
     * 操作方法
     *
     * @return
     */
    String value() default "";
}
