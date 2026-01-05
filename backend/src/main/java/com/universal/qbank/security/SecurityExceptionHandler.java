package com.universal.qbank.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 安全相关异常处理器
 */
@RestControllerAdvice
public class SecurityExceptionHandler {

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
    Map<String, Object> response = new HashMap<>();
    response.put("error", "PERMISSION_DENIED");
    response.put("message", ex.getMessage());
    response.put("timestamp", System.currentTimeMillis());
    
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }
}
