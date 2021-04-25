package com.example.mvc.annotation;

import java.lang.annotation.*;

/**
 * 参数注解
 * @RequestParam
 * @author panzhi
 * @version v1.0, 2018.8.7
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

    String value() default "";
}
