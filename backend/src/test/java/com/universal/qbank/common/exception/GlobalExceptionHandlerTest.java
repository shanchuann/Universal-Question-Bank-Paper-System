package com.universal.qbank.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.universal.qbank.common.api.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void shouldReturnForbiddenForSecurityException() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");

    ResponseEntity<ApiError> response =
        handler.handleSecurityException(new SecurityException("Access denied"), request);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("PERMISSION_DENIED", response.getBody().getCode());
    assertEquals("Access denied", response.getBody().getMessage());
    assertEquals("/api/admin/users", response.getBody().getPath());
  }

  @Test
  void shouldReturnBusinessErrorForRuntimeException() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/import/photo");

    ResponseEntity<ApiError> response =
        handler.handleRuntime(new RuntimeException("图片解析失败"), request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("BUSINESS_ERROR", response.getBody().getCode());
    assertEquals("图片解析失败", response.getBody().getMessage());
    assertEquals("/api/import/photo", response.getBody().getPath());
  }
}
