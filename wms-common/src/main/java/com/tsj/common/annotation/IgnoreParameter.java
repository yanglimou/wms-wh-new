package com.tsj.common.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
@Inherited
/**
 * @className: IgnoreParameter
 * @description: 可忽略的参数注解
 * @author: Frank
 * @create: 2020-04-07 15:23
 */
public @interface IgnoreParameter {

}
