package com.universal.qbank.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色检查注解
 * 
 * 用于标记需要特定角色才能访问的Controller方法
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredRole {
    
    /**
     * 角色编码，支持多个（满足任一即可）
     */
    String[] value();
    
    /**
     * 是否需要满足所有角色（默认false，即满足任一即可）
     */
    boolean requireAll() default false;
}
