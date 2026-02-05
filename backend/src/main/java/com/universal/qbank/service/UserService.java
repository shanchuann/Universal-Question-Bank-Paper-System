package com.universal.qbank.service;

import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.UserRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

  @Autowired private UserRepository userRepository;
  
  @Autowired private SystemConfigService systemConfigService;

  public UserEntity register(String username, String password, String role) {
    // 检查是否允许注册
    if (!systemConfigService.getBooleanConfig(SystemConfigService.ALLOW_REGISTRATION, true)) {
      throw new RuntimeException("系统当前不允许注册新用户");
    }
    
    if (userRepository.findByUsername(username).isPresent()) {
      throw new RuntimeException("用户名已存在");
    }
    
    // 检查密码最小长度
    int minLength = systemConfigService.getIntConfig(SystemConfigService.PASSWORD_MIN_LENGTH, 6);
    if (password == null || password.length() < minLength) {
      throw new RuntimeException("密码长度不能少于 " + minLength + " 个字符");
    }
    
    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setNickname(username); // Default nickname is username
    user.setPassword(password); // In production, hash this!
    user.setRole(role != null ? role : "USER");
    return userRepository.save(user);
  }

  public Optional<UserEntity> login(String username, String password) {
    Optional<UserEntity> user = userRepository.findByUsername(username);
    if (user.isPresent() && user.get().getPassword().equals(password)) {
      return user;
    }
    return Optional.empty();
  }

  public Optional<UserEntity> getUserById(String id) {
    return userRepository.findById(id);
  }

  public UserEntity updateProfile(String id, String username, String email, String avatarUrl, String nickname, String currentPassword) {
    UserEntity user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

    // Validate Nickname
    if (nickname != null) {
        if (nickname.length() < 2 || nickname.length() > 20) {
            throw new RuntimeException("Nickname must be between 2 and 20 characters");
        }
        // Allow letters, numbers, spaces, underscores, hyphens, and Chinese characters
        if (!nickname.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9 _-]+$")) {
            throw new RuntimeException("Nickname contains invalid characters");
        }
        user.setNickname(nickname);
    }

    // Validate Username
    if (username != null && !username.isEmpty() && !username.equals(user.getUsername())) {
      if (userRepository.findByUsername(username).isPresent()) {
        throw new RuntimeException("Username already exists");
      }
      user.setUsername(username);
    }

    // Validate Email and Check Password if Email is changing
    if (email != null && !email.equals(user.getEmail())) {
        // Validate Email Format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new RuntimeException("Invalid email format");
        }

        // Validate Email Uniqueness
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already in use");
        }
        
        // Require password verification for sensitive change
        if (currentPassword == null || !currentPassword.equals(user.getPassword())) {
            throw new RuntimeException("Current password is required to change email");
        }
        user.setEmail(email);
    }

    if (avatarUrl != null) {
      user.setAvatarUrl(avatarUrl);
    }
    
    return userRepository.save(user);
  }

  public void updatePassword(String id, String oldPassword, String newPassword) {
    UserEntity user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    
    if (!user.getPassword().equals(oldPassword)) {
      throw new RuntimeException("旧密码错误");
    }
    
    // 检查密码最小长度
    int minLength = systemConfigService.getIntConfig(SystemConfigService.PASSWORD_MIN_LENGTH, 6);
    if (newPassword == null || newPassword.length() < minLength) {
      throw new RuntimeException("新密码长度不能少于 " + minLength + " 个字符");
    }
    
    user.setPassword(newPassword);
    userRepository.save(user);
  }

  public org.springframework.data.domain.Page<UserEntity> getAllUsers(
      org.springframework.data.domain.Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  public void deleteUser(String id) {
    userRepository.deleteById(id);
  }

  public UserEntity updateUserStatus(String id, String status) {
    UserEntity user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    user.setStatus(status);
    return userRepository.save(user);
  }

  public UserEntity updateUserRole(String id, String role) {
    UserEntity user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    user.setRole(role);
    return userRepository.save(user);
  }

  public org.springframework.data.domain.Page<UserEntity> getUsers(
      String role, org.springframework.data.domain.Pageable pageable) {
    if (role != null && !role.isEmpty()) {
      return userRepository.findByRole(role, pageable);
    }
    return userRepository.findAll(pageable);
  }

  /** 保存用户 */
  public UserEntity saveUser(UserEntity user) {
    return userRepository.save(user);
  }

  /** 检查邮箱是否已注册 */
  public boolean existsByEmail(String email) {
    return userRepository.findByEmail(email).isPresent();
  }

  /** 通过邮箱重置密码 */
  public void resetPasswordByEmail(String email, String newPassword) {
    UserEntity user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("该邮箱未注册"));
    
    // 检查密码最小长度
    int minLength = systemConfigService.getIntConfig(SystemConfigService.PASSWORD_MIN_LENGTH, 6);
    if (newPassword == null || newPassword.length() < minLength) {
      throw new RuntimeException("密码长度不能少于 " + minLength + " 个字符");
    }
    
    user.setPassword(newPassword);
    userRepository.save(user);
  }

  /** 通过邮箱查找用户 */
  public Optional<UserEntity> getUserByEmail(String email) {
    return userRepository.findByEmail(email);
  }
}
