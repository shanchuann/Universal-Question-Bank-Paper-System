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
@RequestMapping("/files")
@CrossOrigin(origins = "*")
public class FileController {

  @Autowired private FileService fileService;

  @PostMapping("/upload")
  public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
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
