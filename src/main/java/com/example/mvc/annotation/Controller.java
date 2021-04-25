package com.example.mvc.annotation;

import java.lang.annotation.*;

/**
 * 控制层注解
 * @Controller
 * @author panzhi
 * @version v1.0, 2018.8.7
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {

    String value() default "";
}
