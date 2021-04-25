package com.example.mvc.annotation;

import java.lang.annotation.*;

/**
 * 自动装载注解
 * @Autowrited
 * @author panzhi
 * @version v1.0, 2018.8.7
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

    String value() default "";
}
