package com.universal.qbank.controller;

import com.universal.qbank.service.FileService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

  @Autowired private FileService fileService;

  @PostMapping("/upload")
  public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
    // Validate file type
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
    }

    // Validate file size (e.g., 5MB)
    if (file.getSize() > 5 * 1024 * 1024) {
      return ResponseEntity.badRequest().body(Map.of("error", "File size must be less than 5MB"));
    }

    String fileName = fileService.storeFile(file);

    String fileDownloadUri =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/")
            .path(fileName)
            .toUriString();

    Map<String, String> response = new HashMap<>();
    response.put("fileName", fileName);
    response.put("fileUrl", fileDownloadUri);

    return ResponseEntity.ok(response);
  }
}
