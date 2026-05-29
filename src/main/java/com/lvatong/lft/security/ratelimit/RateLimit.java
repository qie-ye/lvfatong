package com.lvatong.lft.security.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流次数
     */
    int limit() default 100;

    /**
     * 时间窗口（秒）
     */
    int window() default 60;

    /**
     * 限流Key类型
     */
    KeyType keyType() default KeyType.IP;

    /**
     * 限流Key类型枚举
     */
    enum KeyType {
        IP,      // 基于IP地址
        USER,    // 基于用户ID
        GLOBAL   // 全局限流
    }
}