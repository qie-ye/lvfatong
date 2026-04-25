package com.lvatong.lft.common.ratelimit;

import java.lang.annotation.*;

/**
 * API限流注解
 * 基于令牌桶算法，按IP+用户维度限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 每秒允许的请求数
     */
    double permitsPerSecond() default 10.0;

    /**
     * 限流维度: IP, USER, GLOBAL
     */
    String dimension() default "USER";

    /**
     * 被限流时的提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
