package com.lake.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * lake-spring
 *
 * @author Lake Fang
 * @date 2019-03-31 14:05
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyComponent {
    String value() default "";
}
