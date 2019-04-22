package com.lake.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * lake-spring
 *
 * @author Lake Fang
 * @date 2019-03-31 11:22
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value() default "";
}
