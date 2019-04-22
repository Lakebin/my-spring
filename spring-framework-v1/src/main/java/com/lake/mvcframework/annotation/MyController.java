package com.lake.mvcframework.annotation;


import java.lang.annotation.*;

/**
 * 标明控制器
 *
 * @author Lake Fang
 * @date 2019-03-31 11:20
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@MyComponent
public @interface MyController {
    String value() default "";
}
