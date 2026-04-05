package com.universal.qbank.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universal.qbank.common.exception.ApiErrorResponseBodyAdvice;
import com.universal.qbank.common.exception.GlobalExceptionHandler;
import com.universal.qbank.service.AnnouncementService;
import com.universal.qbank.service.OllamaAiService;
import com.universal.qbank.service.OllamaStartupService;
import com.universal.qbank.service.OperationLogService;
import com.universal.qbank.service.StatisticsService;
import com.universal.qbank.service.SystemConfigService;
import com.universal.qbank.service.SystemMonitorService;
import com.universal.qbank.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ApiErrorResponseBodyAdvice.class})
class AdminControllerMockMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserService userService;
  @MockBean private SystemConfigService systemConfigService;
  @MockBean private OperationLogService operationLogService;
  @MockBean private AnnouncementService announcementService;
  @MockBean private StatisticsService statisticsService;
  @MockBean private SystemMonitorService systemMonitorService;
  @MockBean private OllamaAiService ollamaAiService;
  @MockBean private OllamaStartupService ollamaStartupService;
  @MockBean private JdbcTemplate jdbcTemplate;

  @Test
  void updateSystemSettingsShouldReturnValidationErrorEnvelope() throws Exception {
    mockMvc
        .perform(
            put("/api/admin/system/settings")
                .header("Authorization", "Bearer dummy-jwt-token-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionTimeout\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message", containsString("sessionTimeout")))
        .andExpect(jsonPath("$.path").value("/api/admin/system/settings"))
        .andExpect(jsonPath("$.timestamp").isNumber());

    verifyNoInteractions(systemConfigService);
  }

  @Test
  void setAiModelShouldReturnUnifiedBadRequestWhenModelMissing() throws Exception {
    mockMvc
        .perform(
            put("/api/admin/ai/model")
                .header("Authorization", "Bearer dummy-jwt-token-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("model 不能为空"))
        .andExpect(jsonPath("$.path").value("/api/admin/ai/model"))
        .andExpect(jsonPath("$.timestamp").isNumber());
  }
}
