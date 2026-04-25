package com.lvatong.lft.common.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    String action();
    String resource() default "";
}
