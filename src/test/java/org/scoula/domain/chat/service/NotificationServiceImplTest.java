package org.scoula.domain.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.chat.dto.NotificationDto;
import org.scoula.domain.chat.dto.NotificationListResponseDto;
import org.scoula.domain.chat.fcm.FCMService;
import org.scoula.domain.chat.mapper.NotificationMapper;
import org.scoula.domain.chat.vo.Notification;
import org.scoula.domain.chat.vo.NotificationType;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl 테스트")
class NotificationServiceImplTest {

      @Mock private NotificationMapper notificationMapper;

      @Mock private FCMService fcmService;

      @Mock private ObjectMapper objectMapper;

      @InjectMocks private NotificationServiceImpl notificationService;

      private Long userId;
      private Long chatRoomId;
      private String senderName;
      private String message;
      private Map<String, String> fcmData;
      private Notification notification;
      private NotificationDto notificationDto;

      @BeforeEach
      void setUp() {
          userId = 1L;
          chatRoomId = 100L;
          senderName = "홍길동";
          message = "안녕하세요, 테스트 메시지입니다.";
          fcmData = new HashMap<>();
          fcmData.put("type", "CHAT");
          fcmData.put("chatRoomId", String.valueOf(chatRoomId));

          notification =
                  Notification.builder()
                          .notiId(1L)
                          .userId(userId)
                          .title(senderName + "님의 새 메시지")
                          .content(message)
                          .type(NotificationType.CHAT.name())
                          .relatedId(chatRoomId)
                          .isRead(false)
                          .createAt(LocalDateTime.now())
                          .build();

          notificationDto =
                  NotificationDto.builder()
                          .notiId(notification.getNotiId())
                          .userId(notification.getUserId())
                          .title(notification.getTitle())
                          .content(notification.getContent())
                          .type(notification.getType())
                          .relatedId(notification.getRelatedId())
                          .isRead(notification.getIsRead())
                          .createAt(notification.getCreateAt())
                          .timeAgo("방금 전")
                          .relatedInfo("채팅방 #" + chatRoomId)
                          .typeDescription("채팅 알림")
                          .build();
      }

      @Nested
      @DisplayName("createChatNotification 메서드")
      class CreateChatNotification {

          @Test
          @DisplayName("채팅 알림 생성 성공")
          void createChatNotification_Success() throws Exception {
              // Given
              when(objectMapper.writeValueAsString(fcmData)).thenReturn("{\"type\":\"CHAT\"}");
              when(fcmService.sendNotification(userId, senderName + "님의 새 메시지", message, fcmData))
                      .thenReturn(true);

              // When
              notificationService.createChatNotification(
                      userId, senderName, message, chatRoomId, fcmData);

              // Then
              verify(notificationMapper).insertNotification(any(Notification.class));
              verify(fcmService).sendNotification(userId, senderName + "님의 새 메시지", message, fcmData);
              verify(objectMapper).writeValueAsString(fcmData);
          }

          @Test
          @DisplayName("긴 메시지 truncate 처리")
          void createChatNotification_LongMessage() throws Exception {
              // Given
              String longMessage = "a".repeat(150);
              String expectedContent = "a".repeat(100) + "...";
              when(objectMapper.writeValueAsString(fcmData)).thenReturn("{\"type\":\"CHAT\"}");
              when(fcmService.sendNotification(anyLong(), anyString(), anyString(), any()))
                      .thenReturn(true);

              // When
              notificationService.createChatNotification(
                      userId, senderName, longMessage, chatRoomId, fcmData);

              // Then
              verify(fcmService)
                      .sendNotification(
                              eq(userId),
                              eq(senderName + "님의 새 메시지"),
                              eq(expectedContent),
                              eq(fcmData));
          }

          @Test
          @DisplayName("FCM 전송 실패해도 예외 발생하지 않음")
          void createChatNotification_FCMFailure() throws Exception {
              // Given
              when(objectMapper.writeValueAsString(fcmData)).thenReturn("{\"type\":\"CHAT\"}");
              when(fcmService.sendNotification(anyLong(), anyString(), anyString(), any()))
                      .thenReturn(false);

              // When & Then
              assertDoesNotThrow(
                      () ->
                              notificationService.createChatNotification(
                                      userId, senderName, message, chatRoomId, fcmData));
              verify(notificationMapper).insertNotification(any(Notification.class));
          }

          @Test
          @DisplayName("예외 발생 시 로그만 남기고 예외 던지지 않음")
          void createChatNotification_Exception() throws Exception {
              // Given
              when(objectMapper.writeValueAsString(fcmData))
                      .thenThrow(new RuntimeException("JSON 변환 실패"));

              // When & Then
              assertDoesNotThrow(
                      () ->
                              notificationService.createChatNotification(
                                      userId, senderName, message, chatRoomId, fcmData));
          }
      }

      @Nested
      @DisplayName("getNotifications 메서드")
      class GetNotifications {

          @Test
          @DisplayName("알림 목록 조회 성공")
          void getNotifications_Success() {
              // Given
              int page = 0;
              int size = 10;
              List<Notification> notifications = Arrays.asList(notification);

              when(notificationMapper.findByUserId(userId, size, 0)).thenReturn(notifications);
              when(notificationMapper.countUnreadNotifications(userId)).thenReturn(1);
              when(notificationMapper.countByUserId(userId)).thenReturn(1);

              // When
              NotificationListResponseDto result =
                      notificationService.getNotifications(userId, page, size);

              // Then
              assertNotNull(result);
              assertEquals(1, result.getNotifications().size());
              assertEquals(1, result.getUnreadCount());
              assertEquals(0, result.getCurrentPage());
              assertEquals(10, result.getPageSize());
              assertFalse(result.isHasNext());
              assertEquals(1, result.getTotalCount());
          }

          @Test
          @DisplayName("페이지 크기 100 초과 시 100으로 제한")
          void getNotifications_MaxSize() {
              // Given
              int page = 0;
              int size = 200;
              List<Notification> notifications = Arrays.asList(notification);

              when(notificationMapper.findByUserId(userId, 100, 0)).thenReturn(notifications);
              when(notificationMapper.countUnreadNotifications(userId)).thenReturn(1);
              when(notificationMapper.countByUserId(userId)).thenReturn(1);

              // When
              NotificationListResponseDto result =
                      notificationService.getNotifications(userId, page, size);

              // Then
              verify(notificationMapper).findByUserId(userId, 100, 0);
              assertEquals(100, result.getPageSize());
          }

          @Test
          @DisplayName("음수 페이지 번호 0으로 변경")
          void getNotifications_NegativePage() {
              // Given
              int page = -1;
              int size = 10;
              List<Notification> notifications = Arrays.asList(notification);

              when(notificationMapper.findByUserId(userId, size, 0)).thenReturn(notifications);
              when(notificationMapper.countUnreadNotifications(userId)).thenReturn(0);
              when(notificationMapper.countByUserId(userId)).thenReturn(1);

              // When
              NotificationListResponseDto result =
                      notificationService.getNotifications(userId, page, size);

              // Then
              verify(notificationMapper).findByUserId(userId, size, 0);
              assertEquals(0, result.getCurrentPage());
          }

          @Test
          @DisplayName("예외 발생 시 빈 응답 반환")
          void getNotifications_Exception() {
              // Given
              int page = 0;
              int size = 10;
              when(notificationMapper.findByUserId(userId, size, 0))
                      .thenThrow(new RuntimeException("DB 조회 실패"));

              // When
              NotificationListResponseDto result =
                      notificationService.getNotifications(userId, page, size);

              // Then
              assertNotNull(result);
              assertTrue(result.getNotifications().isEmpty());
              assertEquals(0, result.getUnreadCount());
              assertEquals(0, result.getTotalCount());
              assertFalse(result.isHasNext());
          }
      }

      @Nested
      @DisplayName("getNotificationsByType 메서드")
      class GetNotificationsByType {

          @Test
          @DisplayName("타입별 알림 조회 성공")
          void getNotificationsByType_Success() {
              // Given
              String type = "CHAT";
              int page = 0;
              int size = 10;
              List<Notification> notifications = Arrays.asList(notification);

              when(notificationMapper.findByUserIdAndType(userId, type, size, 0))
                      .thenReturn(notifications);
              when(notificationMapper.countByUserIdAndType(userId, type)).thenReturn(1);
              when(notificationMapper.countUnreadNotifications(userId)).thenReturn(1);

              // When
              NotificationListResponseDto result =
                      notificationService.getNotificationsByType(userId, type, page, size);

              // Then
              assertNotNull(result);
              assertEquals(1, result.getNotifications().size());
              assertEquals(1, result.getUnreadCount());
              assertEquals(1, result.getTotalCount());
          }
      }

      @Nested
      @DisplayName("getUnreadCount 메서드")
      class GetUnreadCount {

          @Test
          @DisplayName("읽지 않은 알림 수 조회 성공")
          void getUnreadCount_Success() {
              // Given
              when(notificationMapper.countUnreadNotifications(userId)).thenReturn(5);

              // When
              int result = notificationService.getUnreadCount(userId);

              // Then
              assertEquals(5, result);
          }

          @Test
          @DisplayName("예외 발생 시 0 반환")
          void getUnreadCount_Exception() {
              // Given
              when(notificationMapper.countUnreadNotifications(userId))
                      .thenThrow(new RuntimeException("DB 조회 실패"));

              // When
              int result = notificationService.getUnreadCount(userId);

              // Then
              assertEquals(0, result);
          }
      }

      @Nested
      @DisplayName("getLatestNotifications 메서드")
      class GetLatestNotifications {

          @Test
          @DisplayName("최신 알림 조회 성공")
          void getLatestNotifications_Success() {
              // Given
              int limit = 5;
              List<Notification> notifications = Arrays.asList(notification);
              when(notificationMapper.findLatestByUserId(userId, limit)).thenReturn(notifications);

              // When
              List<NotificationDto> result =
                      notificationService.getLatestNotifications(userId, limit);

              // Then
              assertNotNull(result);
              assertEquals(1, result.size());
          }

          @Test
          @DisplayName("예외 발생 시 빈 리스트 반환")
          void getLatestNotifications_Exception() {
              // Given
              int limit = 5;
              when(notificationMapper.findLatestByUserId(userId, limit))
                      .thenThrow(new RuntimeException("DB 조회 실패"));

              // When
              List<NotificationDto> result =
                      notificationService.getLatestNotifications(userId, limit);

              // Then
              assertNotNull(result);
              assertTrue(result.isEmpty());
          }
      }

      @Nested
      @DisplayName("markAsRead 메서드")
      class MarkAsRead {

          @Test
          @DisplayName("단일 알림 읽음 처리 시 삭제")
          void markAsReadSingle_Success() {
              // Given
              Long notiId = 1L;

              // When
              notificationService.markAsRead(notiId);

              // Then
              verify(notificationMapper).deleteNotification(notiId);
          }

          @Test
          @DisplayName("여러 알림 읽음 처리 시 삭제")
          void markAsReadMultiple_Success() {
              // Given
              List<Long> notiIds = Arrays.asList(1L, 2L, 3L);

              // When
              notificationService.markAsRead(notiIds);

              // Then
              verify(notificationMapper).deleteNotifications(notiIds);
          }

          @Test
          @DisplayName("빈 리스트 처리")
          void markAsReadMultiple_EmptyList() {
              // Given
              List<Long> notiIds = Arrays.asList();

              // When
              notificationService.markAsRead(notiIds);

              // Then
              verify(notificationMapper, never()).deleteNotifications(any());
          }

          @Test
          @DisplayName("null 리스트 처리")
          void markAsReadMultiple_NullList() {
              // Given
              List<Long> notiIds = null;

              // When
              notificationService.markAsRead(notiIds);

              // Then
              verify(notificationMapper, never()).deleteNotifications(any());
          }
      }

      @Nested
      @DisplayName("markAllAsRead 메서드")
      class MarkAllAsRead {

          @Test
          @DisplayName("모든 알림 읽음 처리 시 삭제")
          void markAllAsRead_Success() {
              // When
              notificationService.markAllAsRead(userId);

              // Then
              verify(notificationMapper).deleteAllByUserId(userId);
          }
      }

      @Nested
      @DisplayName("deleteNotification 메서드")
      class DeleteNotification {

          @Test
          @DisplayName("단일 알림 삭제 성공")
          void deleteNotificationSingle_Success() {
              // Given
              Long notiId = 1L;

              // When
              notificationService.deleteNotification(notiId);

              // Then
              verify(notificationMapper).deleteNotification(notiId);
          }

          @Test
          @DisplayName("여러 알림 삭제 성공")
          void deleteNotificationMultiple_Success() {
              // Given
              List<Long> notiIds = Arrays.asList(1L, 2L, 3L);

              // When
              notificationService.deleteNotifications(notiIds);

              // Then
              verify(notificationMapper).deleteNotifications(notiIds);
          }
      }

      @Nested
      @DisplayName("deleteAllNotifications 메서드")
      class DeleteAllNotifications {

          @Test
          @DisplayName("모든 알림 삭제 성공")
          void deleteAllNotifications_Success() {
              // When
              notificationService.deleteAllNotifications(userId);

              // Then
              verify(notificationMapper).deleteAllByUserId(userId);
          }
      }
}
