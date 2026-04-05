package com.universal.qbank.common.exception;

import com.universal.qbank.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiError> handleSecurityException(
      SecurityException ex, HttpServletRequest request) {
    return build(
        HttpStatus.FORBIDDEN,
        "PERMISSION_DENIED",
        sanitizeMessage(ex.getMessage(), "Access denied"),
        request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "Access denied", request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST,
        "BAD_REQUEST",
        sanitizeMessage(ex.getMessage(), "请求参数不合法"),
        request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    FieldError fieldError = ex.getBindingResult().getFieldError();
    String message =
        fieldError == null
            ? "请求参数校验失败"
            : fieldError.getField() + " " + fieldError.getDefaultMessage();
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiError> handleRuntime(RuntimeException ex, HttpServletRequest request) {
    log.warn("Business runtime error on {}: {}", request.getRequestURI(), ex.getMessage());
    return build(
        HttpStatus.BAD_REQUEST,
        "BUSINESS_ERROR",
        sanitizeMessage(ex.getMessage(), "请求处理失败"),
        request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception on {}", request.getRequestURI(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务器内部错误", request);
  }

  private ResponseEntity<ApiError> build(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    String path = request == null ? "" : request.getRequestURI();
    return ResponseEntity.status(status).body(ApiError.of(code, message, path));
  }

  private String sanitizeMessage(String message, String fallback) {
    if (message == null || message.isBlank()) {
      return fallback;
    }
    return message.trim();
  }
}
