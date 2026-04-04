package com.universal.qbank.controller;

import com.universal.qbank.service.SystemConfigService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

  @Autowired private SystemConfigService systemConfigService;

  @GetMapping("/public-settings")
  public ResponseEntity<Map<String, Object>> getPublicSettings() {
    return ResponseEntity.ok(systemConfigService.getPublicSettings());
  }
}
