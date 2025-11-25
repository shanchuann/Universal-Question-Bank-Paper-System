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

  public UserEntity register(String username, String password, String role) {
    if (userRepository.findByUsername(username).isPresent()) {
      throw new RuntimeException("Username already exists");
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

  public UserEntity updateProfile(String id, String email, String avatarUrl, String nickname) {
    UserEntity user =
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

    if (email != null) {
      user.setEmail(email);
    }
    if (avatarUrl != null) {
      user.setAvatarUrl(avatarUrl);
    }
    if (nickname != null && !nickname.isEmpty()) {
      user.setNickname(nickname);
    }
    return userRepository.save(user);
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
}
