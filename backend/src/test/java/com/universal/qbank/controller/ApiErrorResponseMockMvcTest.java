package com.universal.qbank.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universal.qbank.common.exception.ApiErrorResponseBodyAdvice;
import com.universal.qbank.common.exception.GlobalExceptionHandler;
import com.universal.qbank.service.EmailService;
import com.universal.qbank.service.FileService;
import com.universal.qbank.service.NotificationService;
import com.universal.qbank.service.UserService;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {FileController.class, NotificationController.class, UserController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ApiErrorResponseBodyAdvice.class})
class ApiErrorResponseMockMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private FileService fileService;
  @MockBean private NotificationService notificationService;
  @MockBean private UserService userService;
  @MockBean private EmailService emailService;
  @MockBean private JdbcTemplate jdbcTemplate;

  @Test
  void fileUploadShouldReturnUnifiedBadRequestError() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "avatar.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/api/files/upload").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Only image files are allowed"))
        .andExpect(jsonPath("$.path").value("/api/files/upload"))
        .andExpect(jsonPath("$.timestamp").isNumber());

    verifyNoInteractions(fileService);
  }

  @Test
  void notificationsUnauthorizedShouldReturnUnifiedError() throws Exception {
    mockMvc
        .perform(get("/api/notifications").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Invalid token"))
        .andExpect(jsonPath("$.path").value("/api/notifications"))
        .andExpect(jsonPath("$.timestamp").isNumber());
  }

  @Test
  void userProfileUnauthorizedShouldReturnUnifiedError() throws Exception {
    mockMvc
        .perform(get("/api/user/profile").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Invalid token"))
        .andExpect(jsonPath("$.path").value("/api/user/profile"))
        .andExpect(jsonPath("$.timestamp").isNumber());
  }

  @Test
  void userProfileNotFoundShouldReturnUnifiedError() throws Exception {
    when(userService.getUserById("u1")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/user/profile").header("Authorization", "Bearer dummy-jwt-token-u1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("User not found"))
        .andExpect(jsonPath("$.path").value("/api/user/profile"))
        .andExpect(jsonPath("$.timestamp").isNumber());
  }

  @Test
  void notificationNotFoundShouldReturnUnifiedError() throws Exception {
    when(notificationService.markAsRead("u1", "n1")).thenReturn(false);

    mockMvc
        .perform(
            post("/api/notifications/{id}/read", "n1")
                .header("Authorization", "Bearer dummy-jwt-token-u1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("消息不存在"))
        .andExpect(jsonPath("$.path").value("/api/notifications/n1/read"))
        .andExpect(jsonPath("$.timestamp").isNumber());
  }
}
