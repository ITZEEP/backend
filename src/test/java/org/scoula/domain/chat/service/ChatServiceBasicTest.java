package org.scoula.domain.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.domain.chat.BadWordFilter;
import org.scoula.domain.chat.fcm.FCMService;
import org.scoula.domain.chat.mapper.ChatRoomMapper;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.repository.ChatMessageMongoRepository;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatServiceImpl 기본 테스트")
class ChatServiceBasicTest {

      @Mock private ChatRoomMapper chatRoomMapper;
      @Mock private ChatMessageMongoRepository mongoRepository;
      @Mock private SimpMessagingTemplate messagingTemplate;
      @Mock private S3ServiceInterface s3Service;
      @Mock private UserServiceInterface userService;
      @Mock private BadWordFilter badWordFilter;
      @Mock private FCMService fcmService;
      @Mock private NotificationServiceInterface notificationService;
      @Mock private ContractChatMapper contractChatMapper;
      @Mock private RedisTemplate<String, String> stringRedisTemplate;

      @InjectMocks private ChatServiceImpl chatService;

      @BeforeEach
      void setUp() {
          ReflectionTestUtils.setField(chatService, "URL", "http://localhost:3000");
      }

      @Test
      @DisplayName("사용자 온라인 상태 관리 테스트")
      void userOnlineStatusTest() {
          // Given
          Long userId = 1L;

          // When
          chatService.addOnlineUser(userId);

          // Then
          assertTrue(chatService.isUserOnline(userId));

          // When
          chatService.removeOnlineUser(userId);

          // Then
          assertFalse(chatService.isUserOnline(userId));
      }

      @Test
      @DisplayName("현재 채팅방 상태 관리 테스트")
      void currentChatRoomStatusTest() {
          // Given
          Long userId = 1L;
          Long chatRoomId = 100L;

          // When
          chatService.setUserCurrentChatRoom(userId, chatRoomId);

          // Then
          assertTrue(chatService.getCurrentChatRoomStatus().containsKey(userId));
          assertEquals(chatRoomId, chatService.getCurrentChatRoomStatus().get(userId));

          // When
          chatService.removeUserFromCurrentChatRoom(userId);

          // Then
          assertFalse(chatService.getCurrentChatRoomStatus().containsKey(userId));
      }

      @Test
      @DisplayName("사용자 오프라인 설정 테스트")
      void setUserOfflineTest() {
          // Given
          Long userId = 1L;
          Long chatRoomId = 100L;

          chatService.addOnlineUser(userId);
          chatService.setUserCurrentChatRoom(userId, chatRoomId);

          // When
          chatService.setUserOffline(userId);

          // Then
          assertFalse(chatService.isUserOnline(userId));
          assertFalse(chatService.getCurrentChatRoomStatus().containsKey(userId));
      }

      @Test
      @DisplayName("매물 소유자 ID 조회 테스트")
      void getPropertyOwnerIdTest() {
          // Given
          Long propertyId = 1L;
          Long ownerId = 2L;

          when(chatRoomMapper.findPropertyOwnerId(propertyId)).thenReturn(ownerId);

          // When
          Long result = chatService.getPropertyOwnerId(propertyId);

          // Then
          assertEquals(ownerId, result);
          verify(chatRoomMapper).findPropertyOwnerId(propertyId);
      }

      @Test
      @DisplayName("온라인 사용자 목록 조회 테스트")
      void getOnlineUsersTest() {
          // Given
          Long userId1 = 1L;
          Long userId2 = 2L;

          chatService.addOnlineUser(userId1);
          chatService.addOnlineUser(userId2);

          // When
          var result = chatService.getOnlineUsers();

          // Then
          assertNotNull(result);
          assertEquals(2, result.size());
          assertTrue(result.contains(userId1));
          assertTrue(result.contains(userId2));
      }
}
