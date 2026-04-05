package com.universal.qbank.common.api;

/** 统一错误响应体。 */
public class ApiError {

  private final String code;
  private final String message;
  private final String path;
  private final long timestamp;

  public ApiError(String code, String message, String path, long timestamp) {
    this.code = code;
    this.message = message;
    this.path = path;
    this.timestamp = timestamp;
  }

  public static ApiError of(String code, String message, String path) {
    return new ApiError(code, message, path, System.currentTimeMillis());
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public String getPath() {
    return path;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
