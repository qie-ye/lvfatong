package com.lvatong.lft.security.encryption;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要加密的字段
 * 使用AES-256-GCM算法进行加密
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
    /**
     * 加密算法，默认AES-256-GCM
     */
    String algorithm() default "AES-256-GCM";
}