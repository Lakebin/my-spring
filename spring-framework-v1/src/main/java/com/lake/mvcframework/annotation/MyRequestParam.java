package com.lake.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * lake-spring
 *
 * @author Lake Fang
 * @date 2019-03-31 11:31
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {
    String value() default "";
}
