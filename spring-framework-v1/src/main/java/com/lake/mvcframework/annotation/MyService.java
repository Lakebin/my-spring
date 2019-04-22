package com.lake.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * lake-spring
 *
 * @author Lake Fang
 * @date 2019-03-31 11:32
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@MyComponent
public @interface MyService {
    String value() default "";
}
