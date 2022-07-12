package com.tsj.common.annotation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
/**
 * @className: NotNull
 * @description: 参数判空注解
 * @author: Frank
 * @create: 2020-04-07 15:23
 */
public @interface NotNull {
    String[] value() default {};
}
