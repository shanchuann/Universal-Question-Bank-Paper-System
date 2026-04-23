package com.universal.qbank.repository;

import com.universal.qbank.entity.UserNotificationEntity;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotificationEntity, String> {

  List<UserNotificationEntity> findByReceiverIdOrderByCreatedAtDesc(
      String receiverId, Pageable pageable);

  List<UserNotificationEntity> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(
      String receiverId, Pageable pageable);

  List<UserNotificationEntity> findByReceiverIdAndIsReadFalse(String receiverId);

  long countByReceiverIdAndIsReadFalse(String receiverId);

  long countByReceiverIdAndSenderIdAndIsReadFalseAndTypeInAndCreatedAtAfter(
      String receiverId, String senderId, Collection<String> types, OffsetDateTime cutoff);

  List<UserNotificationEntity>
      findByReceiverIdAndSenderIdAndIsReadFalseAndTypeInAndCreatedAtAfterOrderByCreatedAtDesc(
          String receiverId, String senderId, Collection<String> types, OffsetDateTime cutoff);

  long deleteByCreatedAtBefore(OffsetDateTime cutoff);

  @Query(
      "SELECT n FROM UserNotificationEntity n "
          + "WHERE n.createdAt >= :cutoff "
          + "AND n.type IN :types "
          + "AND ((n.senderId = :userId AND n.receiverId = :peerId) "
          + "OR (n.senderId = :peerId AND n.receiverId = :userId)) "
          + "ORDER BY n.createdAt ASC")
  List<UserNotificationEntity> findChatMessagesBetweenUsers(
      @Param("userId") String userId,
      @Param("peerId") String peerId,
      @Param("types") Collection<String> types,
      @Param("cutoff") OffsetDateTime cutoff,
      Pageable pageable);

  @Query(
      "SELECT n FROM UserNotificationEntity n "
          + "WHERE n.createdAt >= :cutoff "
          + "AND n.type IN :types "
          + "AND (n.receiverId = :userId OR n.senderId = :userId) "
          + "ORDER BY n.createdAt DESC")
  List<UserNotificationEntity> findRecentChatMessagesByUser(
      @Param("userId") String userId,
      @Param("types") Collection<String> types,
      @Param("cutoff") OffsetDateTime cutoff,
      Pageable pageable);

  @Query(
      "SELECT n FROM UserNotificationEntity n "
          + "WHERE n.createdAt >= :cutoff "
          + "AND n.type IN :types "
          + "AND ((n.senderId = :userId AND n.receiverId = :peerId) "
          + "OR (n.senderId = :peerId AND n.receiverId = :userId)) "
          + "ORDER BY n.createdAt DESC")
  List<UserNotificationEntity> findLatestChatMessageBetweenUsers(
      @Param("userId") String userId,
      @Param("peerId") String peerId,
      @Param("types") Collection<String> types,
      @Param("cutoff") OffsetDateTime cutoff,
      Pageable pageable);

  Optional<UserNotificationEntity> findByIdAndReceiverId(String id, String receiverId);
}
