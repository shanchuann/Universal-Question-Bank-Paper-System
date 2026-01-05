package com.universal.qbank.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限检查注解
 * 
 * 用于标记需要特定权限才能访问的Controller方法
 * 权限编码格式: resource:action[:scope]
 * 例如: question:create, paper:read:own, exam:schedule
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredPermission {
    
    /**
     * 权限编码，支持多个（满足任一即可）
     */
    String[] value();
    
    /**
     * 是否需要满足所有权限（默认false，即满足任一即可）
     */
    boolean requireAll() default false;
}
