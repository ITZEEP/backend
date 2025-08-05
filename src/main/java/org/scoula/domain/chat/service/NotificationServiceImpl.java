// NotificationServiceImpl.java - 읽음 처리 시 자동 삭제하는 서비스 구현체
package org.scoula.domain.chat.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.scoula.domain.chat.dto.NotificationDto;
import org.scoula.domain.chat.dto.NotificationListResponseDto;
import org.scoula.domain.chat.fcm.FCMService;
import org.scoula.domain.chat.mapper.NotificationMapper;
import org.scoula.domain.chat.vo.Notification;
import org.scoula.domain.chat.vo.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class NotificationServiceImpl implements NotificationServiceInterface {

      private final NotificationMapper notificationMapper;
      private final FCMService fcmService;
      private final ObjectMapper objectMapper;

      /** {@inheritDoc} */
      @Override
      public void createChatNotification(
              Long userId,
              String senderName,
              String message,
              Long chatRoomId,
              Map<String, String> fcmData) {

          log.info("=== createChatNotification 호출 ===");
          log.info("userId: {}, senderName: {}, chatRoomId: {}", userId, senderName, chatRoomId);
          log.info("fcmData: {}", fcmData);

          // 스택 트레이스로 호출 위치 확인
          StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
          log.info(
                  "호출 위치: {}.{}() - line {}",
                  stackTrace[2].getClassName(),
                  stackTrace[2].getMethodName(),
                  stackTrace[2].getLineNumber());

          try {
              String title = senderName + "님의 새 메시지";
              String content = truncateMessage(message, 100);

              Notification notification =
                      Notification.builder()
                              .userId(userId)
                              .title(title)
                              .content(content)
                              .type(NotificationType.CHAT.name())
                              .relatedId(chatRoomId)
                              .isRead(false)
                              .createAt(LocalDateTime.now())
                              .data(convertMapToJson(fcmData))
                              .build();

              log.info("알림 DB 저장 시도: notification={}", notification);
              notificationMapper.insertNotification(notification);
              log.info(
                      "채팅 알림 DB 저장 완료: userId={}, chatRoomId={}, notiId={}",
                      userId,
                      chatRoomId,
                      notification.getNotiId());

              boolean fcmSuccess = fcmService.sendNotification(userId, title, content, fcmData);
              log.info("FCM 알림 전송 결과: userId={}, success={}", userId, fcmSuccess);

          } catch (Exception e) {
              log.error("채팅 알림 생성 실패: userId={}, chatRoomId={}", userId, chatRoomId, e);
          }
      }

      /** {@inheritDoc} */
      @Override
      @Transactional(readOnly = true)
      public NotificationListResponseDto getNotifications(Long userId, int page, int size) {
          try {
              size = Math.min(size, 100);
              int offset = page * size;

              List<Notification> notifications =
                      notificationMapper.findByUserId(userId, size, offset);
              List<NotificationDto> notificationDtos =
                      notifications.stream().map(this::convertToDto).collect(Collectors.toList());

              int unreadCount = notificationMapper.countUnreadNotifications(userId);
              int totalCount = notificationMapper.countByUserId(userId);
              boolean hasNext = (offset + size) < totalCount;

              return NotificationListResponseDto.builder()
                      .notifications(notificationDtos)
                      .unreadCount(unreadCount)
                      .currentPage(page)
                      .pageSize(size)
                      .hasNext(hasNext)
                      .totalCount(totalCount)
                      .build();

          } catch (Exception e) {
              log.error("알림 목록 조회 실패: userId={}, page={}, size={}", userId, page, size, e);
              return createEmptyResponse(page, size);
          }
      }

      /** {@inheritDoc} */
      @Override
      @Transactional(readOnly = true)
      public NotificationListResponseDto getNotificationsByType(
              Long userId, String type, int page, int size) {
          try {
              size = Math.min(size, 100);
              int offset = page * size;

              List<Notification> notifications =
                      notificationMapper.findByUserIdAndType(userId, type, size, offset);
              List<NotificationDto> notificationDtos =
                      notifications.stream().map(this::convertToDto).collect(Collectors.toList());

              int totalCount = notificationMapper.countByUserIdAndType(userId, type);
              int unreadCount = notificationMapper.countUnreadNotifications(userId);
              boolean hasNext = (offset + size) < totalCount;

              return NotificationListResponseDto.builder()
                      .notifications(notificationDtos)
                      .unreadCount(unreadCount)
                      .currentPage(page)
                      .pageSize(size)
                      .hasNext(hasNext)
                      .totalCount(totalCount)
                      .build();

          } catch (Exception e) {
              log.error("타입별 알림 목록 조회 실패: userId={}, type={}", userId, type, e);
              return createEmptyResponse(page, size);
          }
      }

      /** {@inheritDoc} */
      @Override
      @Transactional(readOnly = true)
      public int getUnreadCount(Long userId) {
          try {
              return notificationMapper.countUnreadNotifications(userId);
          } catch (Exception e) {
              log.error("읽지 않은 알림 수 조회 실패: userId={}", userId, e);
              return 0;
          }
      }

      /** {@inheritDoc} */
      @Override
      @Transactional(readOnly = true)
      public List<NotificationDto> getLatestNotifications(Long userId, int limit) {
          try {
              List<Notification> notifications = notificationMapper.findLatestByUserId(userId, limit);
              return notifications.stream().map(this::convertToDto).collect(Collectors.toList());
          } catch (Exception e) {
              log.error("최신 알림 조회 실패: userId={}, limit={}", userId, limit, e);
              return List.of();
          }
      }

      /** {@inheritDoc} 🔧 수정: 읽음 처리 대신 바로 삭제 */
      @Override
      public void markAsRead(Long notiId) {
          try {
              log.info("알림 읽음 처리 요청 -> 자동 삭제 실행: notiId={}", notiId);
              notificationMapper.deleteNotification(notiId);
              log.info("알림 읽음 처리(자동 삭제) 완료: notiId={}", notiId);
          } catch (Exception e) {
              log.error("알림 읽음 처리(자동 삭제) 실패: notiId={}", notiId, e);
              throw e;
          }
      }

      /** {@inheritDoc} 🔧 수정: 읽음 처리 대신 바로 삭제 */
      @Override
      public void markAsRead(List<Long> notiIds) {
          try {
              if (notiIds != null && !notiIds.isEmpty()) {
                  log.info("여러 알림 읽음 처리 요청 -> 자동 삭제 실행: count={}", notiIds.size());
                  notificationMapper.deleteNotifications(notiIds);
                  log.info("여러 알림 읽음 처리(자동 삭제) 완료: count={}", notiIds.size());
              }
          } catch (Exception e) {
              log.error("여러 알림 읽음 처리(자동 삭제) 실패: notiIds={}", notiIds, e);
              throw e;
          }
      }

      /** {@inheritDoc} 🔧 수정: 읽음 처리 대신 바로 삭제 */
      @Override
      public void markAllAsRead(Long userId) {
          try {
              log.info("모든 알림 읽음 처리 요청 -> 자동 삭제 실행: userId={}", userId);
              notificationMapper.deleteAllByUserId(userId);
              log.info("모든 알림 읽음 처리(자동 삭제) 완료: userId={}", userId);
          } catch (Exception e) {
              log.error("모든 알림 읽음 처리(자동 삭제) 실패: userId={}", userId, e);
              throw e;
          }
      }

      /** {@inheritDoc} */
      @Override
      public void deleteNotification(Long notiId) {
          try {
              notificationMapper.deleteNotification(notiId);
              log.info("알림 삭제 완료: notiId={}", notiId);
          } catch (Exception e) {
              log.error("알림 삭제 실패: notiId={}", notiId, e);
              throw e;
          }
      }

      /** {@inheritDoc} */
      @Override
      public void deleteNotifications(List<Long> notiIds) {
          try {
              if (notiIds != null && !notiIds.isEmpty()) {
                  notificationMapper.deleteNotifications(notiIds);
                  log.info("여러 알림 삭제 완료: count={}", notiIds.size());
              }
          } catch (Exception e) {
              log.error("여러 알림 삭제 실패: notiIds={}", notiIds, e);
              throw e;
          }
      }

      /** {@inheritDoc} */
      @Override
      public void deleteAllNotifications(Long userId) {
          try {
              notificationMapper.deleteAllByUserId(userId);
              log.info("모든 알림 삭제 완료: userId={}", userId);
          } catch (Exception e) {
              log.error("모든 알림 삭제 실패: userId={}", userId, e);
              throw e;
          }
      }

      /** 빈 응답 생성 */
      private NotificationListResponseDto createEmptyResponse(int page, int size) {
          return NotificationListResponseDto.builder()
                  .notifications(List.of())
                  .unreadCount(0)
                  .currentPage(page)
                  .pageSize(size)
                  .hasNext(false)
                  .totalCount(0)
                  .build();
      }

      /** 메시지 길이 제한 */
      private String truncateMessage(String message, int maxLength) {
          if (!StringUtils.hasText(message)) {
              return "";
          }
          return message.length() > maxLength ? message.substring(0, maxLength) + "..." : message;
      }

      /** Map을 JSON 문자열로 변환 */
      private String convertMapToJson(Map<String, String> map) {
          if (map == null || map.isEmpty()) {
              return null;
          }
          try {
              return objectMapper.writeValueAsString(map);
          } catch (Exception e) {
              log.warn("Map to JSON 변환 실패", e);
              return null;
          }
      }

      /** Notification을 NotificationDto로 변환 */
      private NotificationDto convertToDto(Notification notification) {
          return NotificationDto.builder()
                  .notiId(notification.getNotiId())
                  .userId(notification.getUserId())
                  .title(notification.getTitle())
                  .content(notification.getContent())
                  .type(notification.getType())
                  .relatedId(notification.getRelatedId())
                  .isRead(notification.getIsRead())
                  .createAt(notification.getCreateAt())
                  .data(notification.getData())
                  .timeAgo(calculateTimeAgo(notification.getCreateAt()))
                  .relatedInfo(generateRelatedInfo(notification))
                  .typeDescription(NotificationType.getDescriptionByType(notification.getType()))
                  .build();
      }

      /** 시간 차이 계산 (예: "2분 전", "1시간 전") */
      private String calculateTimeAgo(LocalDateTime createAt) {
          if (createAt == null) {
              return "알 수 없음";
          }

          LocalDateTime now = LocalDateTime.now();
          long minutes = ChronoUnit.MINUTES.between(createAt, now);
          long hours = ChronoUnit.HOURS.between(createAt, now);
          long days = ChronoUnit.DAYS.between(createAt, now);

          if (minutes < 1) {
              return "방금 전";
          } else if (minutes < 60) {
              return minutes + "분 전";
          } else if (hours < 24) {
              return hours + "시간 전";
          } else if (days < 30) {
              return days + "일 전";
          } else {
              return createAt.toLocalDate().toString();
          }
      }

      /** 관련 정보 생성 (알림 타입에 따라) */
      private String generateRelatedInfo(Notification notification) {
          try {
              switch (NotificationType.valueOf(notification.getType())) {
                  case CHAT:
                      return "채팅방 #" + notification.getRelatedId();
                  case CONTRACT_REQUEST:
                  case CONTRACT_ACCEPT:
                  case CONTRACT_REJECT:
                      return "계약 #" + notification.getRelatedId();
                  case SYSTEM:
                      return "시스템 알림";
                  default:
                      return "";
              }
          } catch (Exception e) {
              return "";
          }
      }
}
