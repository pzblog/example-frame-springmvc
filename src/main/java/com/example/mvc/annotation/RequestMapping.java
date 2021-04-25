package com.example.mvc.annotation;

import java.lang.annotation.*;

/**
 * 请求路径注解
 * @RequestMapping
 * @author panzhi
 * @version v1.0, 2018.8.7
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {

    String value() default "";
}
