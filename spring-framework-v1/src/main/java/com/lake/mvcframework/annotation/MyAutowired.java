package com.lake.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 依赖注入
 *
 * @author Lake Fang
 * @date 2019-03-31 11:36
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    String value() default "";
}
