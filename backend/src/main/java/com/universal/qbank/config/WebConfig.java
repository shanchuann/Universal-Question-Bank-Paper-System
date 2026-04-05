package com.universal.qbank.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    if (baseDir.getFileName() != null
        && "backend".equalsIgnoreCase(baseDir.getFileName().toString())) {
      baseDir = baseDir.getParent();
    }
    String rootUploadPath = baseDir.resolve("uploads").toUri().toString();
    String backendUploadPath = baseDir.resolve("backend").resolve("uploads").toUri().toString();

    registry
        .addResourceHandler("/uploads/**")
        .addResourceLocations(rootUploadPath, backendUploadPath);
  }
}
