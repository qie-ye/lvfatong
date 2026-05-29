package com.lvatong.lft.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 增强审计日志注解
 * 用于记录关键操作的详细审计信息
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogEnhanced {
    /**
     * 操作类型
     */
    String action();

    /**
     * 资源类型
     */
    String resource();

    /**
     * 是否记录请求参数
     */
    boolean logParameters() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResponse() default false;

    /**
     * 是否记录IP地址
     */
    boolean logIpAddress() default true;

    /**
     * 是否记录用户代理
     */
    boolean logUserAgent() default false;
}