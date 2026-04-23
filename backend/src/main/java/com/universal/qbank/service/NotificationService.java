package com.universal.qbank.service;

import com.universal.qbank.entity.AnnouncementEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.entity.UserNotificationEntity;
import com.universal.qbank.entity.UserOrganizationEntity;
import com.universal.qbank.repository.UserNotificationRepository;
import com.universal.qbank.repository.UserOrganizationRepository;
import com.universal.qbank.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private static final String MESSAGE_TYPE_NORMAL = "MESSAGE";
  private static final String MESSAGE_TYPE_SYSTEM = "SYSTEM_MESSAGE";
  private static final String MESSAGE_TYPE_AD = "AD_MESSAGE";
  private static final List<String> CHAT_TYPES =
      List.of(MESSAGE_TYPE_NORMAL, MESSAGE_TYPE_AD, "PRIVATE_MESSAGE", "TEACHER_MESSAGE");
  private static final int CHAT_RETENTION_DAYS = 3;

  @Autowired private UserNotificationRepository userNotificationRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private UserOrganizationRepository userOrganizationRepository;

  public List<UserNotificationEntity> listNotifications(
      String userId, boolean unreadOnly, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 100));
    PageRequest page = PageRequest.of(0, safeLimit);
    if (unreadOnly) {
      return userNotificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(
          userId, page);
    }
    return userNotificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, page);
  }

  public long countUnread(String userId) {
    return userNotificationRepository.countByReceiverIdAndIsReadFalse(userId);
  }

  @Transactional
  public boolean markAsRead(String userId, String notificationId) {
    return userNotificationRepository
        .findByIdAndReceiverId(notificationId, userId)
        .map(
            notification -> {
              if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                userNotificationRepository.save(notification);
              }
              return true;
            })
        .orElse(false);
  }

  @Transactional
  public int markAllAsRead(String userId) {
    List<UserNotificationEntity> unread =
        userNotificationRepository.findByReceiverIdAndIsReadFalse(userId);
    for (UserNotificationEntity message : unread) {
      message.setIsRead(true);
    }
    if (!unread.isEmpty()) {
      userNotificationRepository.saveAll(unread);
    }
    return unread.size();
  }

  @Transactional
  public UserNotificationEntity sendPrivateMessage(
      String senderId, String receiverId, String title, String content, String requestedType) {
    if (senderId == null || receiverId == null || senderId.equals(receiverId)) {
      throw new RuntimeException("无效的发送对象");
    }
    if (content == null || content.isBlank()) {
      throw new RuntimeException("消息内容不能为空");
    }

    UserEntity sender =
        userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("发送者不存在"));
    UserEntity receiver =
        userRepository.findById(receiverId).orElseThrow(() -> new RuntimeException("接收者不存在"));

    if (!"ACTIVE".equalsIgnoreCase(receiver.getStatus())) {
      throw new RuntimeException("接收者不可用");
    }

    Set<String> senderOrgs =
        userOrganizationRepository.findByUserId(senderId).stream()
            .map(UserOrganizationEntity::getOrganizationId)
            .collect(Collectors.toSet());
    Set<String> receiverOrgs =
        userOrganizationRepository.findByUserId(receiverId).stream()
            .map(UserOrganizationEntity::getOrganizationId)
            .collect(Collectors.toSet());

    boolean hasSharedOrg = senderOrgs.stream().anyMatch(receiverOrgs::contains);
    if (!hasSharedOrg) {
      throw new RuntimeException("仅支持向同班级/同组织成员发送私信");
    }

    String type = normalizeMessageType(requestedType);
    String senderName =
        sender.getNickname() != null && !sender.getNickname().isBlank()
            ? sender.getNickname()
            : sender.getUsername();

    UserNotificationEntity message = new UserNotificationEntity();
    message.setReceiverId(receiverId);
    message.setSenderId(senderId);
    message.setSenderName(senderName);
    message.setSenderRole(sender.getRole());
    message.setType(type);
    message.setTitle((title == null || title.isBlank()) ? "新消息" : title.trim());
    message.setContent(content.trim());
    return userNotificationRepository.save(message);
  }

  public List<Map<String, Object>> getConversations(String userId, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 100));
    OffsetDateTime cutoff = getChatCutoffTime();

    Set<String> myOrgIds =
        userOrganizationRepository.findByUserId(userId).stream()
            .map(UserOrganizationEntity::getOrganizationId)
            .collect(Collectors.toSet());
    if (myOrgIds.isEmpty()) {
      return List.of();
    }

    Set<String> candidateUserIds = new HashSet<>();
    for (String orgId : myOrgIds) {
      for (UserOrganizationEntity relation :
          userOrganizationRepository.findByOrganizationId(orgId)) {
        if (!userId.equals(relation.getUserId())) {
          candidateUserIds.add(relation.getUserId());
        }
      }
    }

    if (candidateUserIds.isEmpty()) {
      return List.of();
    }

    List<UserEntity> peers =
        userRepository.findAllById(candidateUserIds).stream()
            .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
            .sorted(
                Comparator.comparing(
                        (UserEntity user) -> user.getRole() == null ? "ZZZ" : user.getRole())
                    .thenComparing(user -> user.getUsername() == null ? "" : user.getUsername()))
            .collect(Collectors.toList());

    List<Map<String, Object>> result = new ArrayList<>();
    for (UserEntity peer : peers) {
      String peerId = peer.getId();

      List<UserNotificationEntity> lastMsg =
          userNotificationRepository.findLatestChatMessageBetweenUsers(
              userId, peerId, CHAT_TYPES, cutoff, PageRequest.of(0, 1));
      UserNotificationEntity latest = lastMsg.isEmpty() ? null : lastMsg.get(0);

      long unread =
          userNotificationRepository
              .countByReceiverIdAndSenderIdAndIsReadFalseAndTypeInAndCreatedAtAfter(
                  userId, peerId, CHAT_TYPES, cutoff);

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("peerId", peerId);
      item.put("peerName", displayName(peer));
      item.put("peerRole", peer.getRole());
      item.put("peerAvatarUrl", peer.getAvatarUrl());
      item.put("lastMessage", latest != null ? latest.getContent() : "");
      item.put("lastTime", latest != null ? latest.getCreatedAt() : null);
      item.put("lastType", latest != null ? latest.getType() : MESSAGE_TYPE_NORMAL);
      item.put("unreadCount", unread);
      result.add(item);
    }

    result.sort(
        (a, b) -> {
          OffsetDateTime aTime = (OffsetDateTime) a.get("lastTime");
          OffsetDateTime bTime = (OffsetDateTime) b.get("lastTime");
          if (aTime != null && bTime == null) return -1;
          if (aTime == null && bTime != null) return 1;
          if (aTime != null && bTime != null) {
            return bTime.compareTo(aTime);
          }
          return ((String) a.get("peerName")).compareTo((String) b.get("peerName"));
        });

    if (result.size() > safeLimit) {
      return result.subList(0, safeLimit);
    }
    return result;
  }

  public List<UserNotificationEntity> getConversationMessages(
      String userId, String peerId, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 300));

    if (!isUsersShareOrganization(userId, peerId)) {
      throw new RuntimeException("仅支持查看同班级/同组织成员会话");
    }

    OffsetDateTime cutoff = getChatCutoffTime();
    return userNotificationRepository.findChatMessagesBetweenUsers(
        userId, peerId, CHAT_TYPES, cutoff, PageRequest.of(0, safeLimit));
  }

  @Transactional
  public int markConversationAsRead(String userId, String peerId) {
    OffsetDateTime cutoff = getChatCutoffTime();
    List<UserNotificationEntity> unread =
        userNotificationRepository
            .findByReceiverIdAndSenderIdAndIsReadFalseAndTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
                userId, peerId, CHAT_TYPES, cutoff);

    for (UserNotificationEntity item : unread) {
      item.setIsRead(true);
    }
    if (!unread.isEmpty()) {
      userNotificationRepository.saveAll(unread);
    }
    return unread.size();
  }

  @Transactional
  public void createAnnouncementNotifications(AnnouncementEntity announcement) {
    List<UserEntity> users = userRepository.findAll();
    List<UserNotificationEntity> notifications = new ArrayList<>();

    for (UserEntity user : users) {
      if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
        continue;
      }
      UserNotificationEntity message = new UserNotificationEntity();
      message.setReceiverId(user.getId());
      message.setType(MESSAGE_TYPE_SYSTEM);
      message.setTitle("系统公告：" + announcement.getTitle());
      message.setContent(announcement.getContent());
      message.setRelatedId(announcement.getId());
      notifications.add(message);
    }

    if (!notifications.isEmpty()) {
      userNotificationRepository.saveAll(notifications);
    }
  }

  public List<Map<String, Object>> getAvailableRecipients(String userId) {
    Set<String> myOrgIds =
        userOrganizationRepository.findByUserId(userId).stream()
            .map(UserOrganizationEntity::getOrganizationId)
            .collect(Collectors.toSet());
    if (myOrgIds.isEmpty()) {
      return List.of();
    }

    Set<String> candidateUserIds = new HashSet<>();
    for (String orgId : myOrgIds) {
      for (UserOrganizationEntity relation :
          userOrganizationRepository.findByOrganizationId(orgId)) {
        if (!userId.equals(relation.getUserId())) {
          candidateUserIds.add(relation.getUserId());
        }
      }
    }

    if (candidateUserIds.isEmpty()) {
      return List.of();
    }

    List<UserEntity> users = userRepository.findAllById(candidateUserIds);
    return users.stream()
        .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
        .sorted(
            Comparator.comparing(
                    (UserEntity user) -> user.getRole() == null ? "ZZZ" : user.getRole())
                .thenComparing(user -> user.getUsername() == null ? "" : user.getUsername()))
        .map(
            user -> {
              Map<String, Object> item = new LinkedHashMap<>();
              item.put("id", user.getId());
              item.put("username", user.getUsername());
              item.put("nickname", user.getNickname());
              item.put("role", user.getRole());
              item.put("avatarUrl", user.getAvatarUrl());
              return item;
            })
        .collect(Collectors.toList());
  }

  private String normalizeMessageType(String requestedType) {
    if (requestedType != null) {
      String upper = requestedType.trim().toUpperCase();
      if (MESSAGE_TYPE_AD.equals(upper)) {
        return upper;
      }
    }
    return MESSAGE_TYPE_NORMAL;
  }

  private boolean isUsersShareOrganization(String userId, String peerId) {
    if (userId == null || peerId == null || userId.equals(peerId)) {
      return false;
    }
    Set<String> senderOrgs =
        userOrganizationRepository.findByUserId(userId).stream()
            .map(UserOrganizationEntity::getOrganizationId)
            .collect(Collectors.toSet());
    Set<String> receiverOrgs =
        userOrganizationRepository.findByUserId(peerId).stream()
            .map(UserOrganizationEntity::getOrganizationId)
            .collect(Collectors.toSet());
    return senderOrgs.stream().anyMatch(receiverOrgs::contains);
  }

  private String displayName(UserEntity user) {
    if (user.getNickname() != null && !user.getNickname().isBlank()) {
      return user.getNickname();
    }
    return user.getUsername();
  }

  private OffsetDateTime getChatCutoffTime() {
    return OffsetDateTime.now().minusDays(CHAT_RETENTION_DAYS);
  }

  @Scheduled(cron = "0 0 3 * * ?")
  @Transactional
  public void cleanupExpiredMessages() {
    OffsetDateTime cutoff = getChatCutoffTime();
    userNotificationRepository.deleteByCreatedAtBefore(cutoff);
  }
}
