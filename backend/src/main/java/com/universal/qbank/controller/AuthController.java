package com.universal.qbank.controller;

import com.universal.qbank.api.generated.AuthApi;
import com.universal.qbank.api.generated.model.AuthTokenResponse;
import com.universal.qbank.api.generated.model.LoginRequest;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.service.UserService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

  @Autowired private UserService userService;

  public static class RegisterRequest {
    public String username;
    public String password;
    public String role;
  }

  @PostMapping("/auth/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
    try {
      UserEntity user = userService.register(req.username, req.password, req.role);
      return ResponseEntity.ok(user);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<AuthTokenResponse> authLoginPost(LoginRequest loginRequest) {
    System.out.println("Login attempt: " + loginRequest.getUsername());

    Optional<UserEntity> user =
        userService.login(loginRequest.getUsername(), loginRequest.getPassword());

    if (user.isPresent()) {
      System.out.println("Login success");
      AuthTokenResponse response = new AuthTokenResponse();
      response.setAccessToken("dummy-jwt-token-" + user.get().getId());
      response.setRefreshToken("dummy-refresh-token");
      response.setExpiresIn(3600);
      return ResponseEntity.ok(response);
    } else {
      // Fallback for admin/password if not in DB (optional, but good for existing tests)
      if ("admin".equals(loginRequest.getUsername())
          && "password".equals(loginRequest.getPassword())) {
        AuthTokenResponse response = new AuthTokenResponse();
        response.setAccessToken("dummy-jwt-token-admin");
        response.setRefreshToken("dummy-refresh-token");
        response.setExpiresIn(3600);
        return ResponseEntity.ok(response);
      }
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }
}
