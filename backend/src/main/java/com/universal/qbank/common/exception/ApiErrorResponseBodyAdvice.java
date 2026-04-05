package com.universal.qbank.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universal.qbank.common.api.ApiError;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiErrorResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body instanceof ApiError) {
      return body;
    }
    if (!(response instanceof ServletServerHttpResponse servletResponse)) {
      return body;
    }

    HttpStatus status = HttpStatus.resolve(servletResponse.getServletResponse().getStatus());
    if (status == null || !status.isError()) {
      return body;
    }

    String message = extractMessage(body);
    if (message == null) {
      message = defaultMessage(status);
    }
    String code = mapCode(status);
    ApiError error = ApiError.of(code, message, request.getURI().getPath());

    if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      try {
        return objectMapper.writeValueAsString(error);
      } catch (Exception ignored) {
        return "{\"code\":\""
            + code
            + "\",\"message\":\""
            + message
            + "\",\"path\":\""
            + request.getURI().getPath()
            + "\",\"timestamp\":"
            + error.getTimestamp()
            + "}";
      }
    }

    return error;
  }

  private String extractMessage(Object body) {
    if (body instanceof String text && !text.isBlank()) {
      return text.trim();
    }
    if (body instanceof Map<?, ?> map) {
      Object error = map.get("error");
      if (error == null) {
        error = map.get("message");
      }
      if (error != null) {
        String text = String.valueOf(error).trim();
        if (!text.isEmpty()) {
          return text;
        }
      }
    }
    return null;
  }

  private String defaultMessage(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> "请求参数不合法";
      case UNAUTHORIZED -> "未登录或凭证无效";
      case FORBIDDEN -> "无权限访问";
      case NOT_FOUND -> "资源不存在";
      case TOO_MANY_REQUESTS -> "请求过于频繁";
      case BAD_GATEWAY -> "上游服务异常";
      case SERVICE_UNAVAILABLE -> "服务暂不可用";
      default -> "请求处理失败";
    };
  }

  private String mapCode(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> "BAD_REQUEST";
      case UNAUTHORIZED -> "UNAUTHORIZED";
      case FORBIDDEN -> "PERMISSION_DENIED";
      case NOT_FOUND -> "NOT_FOUND";
      case TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS";
      case BAD_GATEWAY -> "BAD_GATEWAY";
      case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
      default -> status.name();
    };
  }
}
