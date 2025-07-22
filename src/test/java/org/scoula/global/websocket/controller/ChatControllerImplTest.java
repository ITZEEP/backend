package org.scoula.global.websocket.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("채팅 컨트롤러 구현체 단위 테스트")
class ChatControllerImplTest {

      @Mock private SimpMessagingTemplate messagingTemplate;

      @InjectMocks private ChatControllerImpl chatController;

      private Map<String, String> validChatMessage;

      @BeforeEach
      void setUp() {
          validChatMessage = new HashMap<>();
          validChatMessage.put("content", "안녕하세요!");
          validChatMessage.put("sender", "testuser");
      }

      @Test
      @DisplayName("공개 메시지 전송 - 성공")
      void sendMessage_ShouldReturnChatResponse() {
          // when
          ChatControllerImpl.ChatResponse response = chatController.sendMessage(validChatMessage);

          // then
          assertThat(response).isNotNull();
          assertThat(response.getType()).isEqualTo("CHAT");
          assertThat(response.getContent()).isEqualTo("안녕하세요!");
          assertThat(response.getSender()).isEqualTo("testuser");
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("공개 메시지 전송 - 빈 메시지로 실패")
      void sendMessage_ShouldThrowException_WhenContentIsEmpty() {
          // given
          Map<String, String> invalidMessage = new HashMap<>();
          invalidMessage.put("content", "");
          invalidMessage.put("sender", "testuser");

          // when & then
          assertThatThrownBy(() -> chatController.sendMessage(invalidMessage))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("메시지 내용이 비어있을 수 없습니다");
      }

      @Test
      @DisplayName("공개 메시지 전송 - 너무 긴 메시지로 실패")
      void sendMessage_ShouldThrowException_WhenContentIsTooLong() {
          // given
          Map<String, String> longMessage = new HashMap<>();
          longMessage.put("content", "a".repeat(1001)); // 1000자 초과
          longMessage.put("sender", "testuser");

          // when & then
          assertThatThrownBy(() -> chatController.sendMessage(longMessage))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("메시지가 너무 깁니다");
      }

      @Test
      @DisplayName("사용자 추가 - 성공")
      void addUser_ShouldReturnJoinResponse() {
          // given
          SimpMessageHeaderAccessor headerAccessor = mock(SimpMessageHeaderAccessor.class);
          Map<String, Object> sessionAttributes = new HashMap<>();
          when(headerAccessor.getSessionAttributes()).thenReturn(sessionAttributes);
          when(headerAccessor.getSessionId()).thenReturn("session123");

          // when
          ChatControllerImpl.ChatResponse response =
                  chatController.addUser(validChatMessage, headerAccessor);

          // then
          assertThat(response).isNotNull();
          assertThat(response.getType()).isEqualTo("JOIN");
          assertThat(response.getContent()).contains("testuser님이 채팅에 참여하셨습니다!");
          assertThat(response.getSender()).isEqualTo("testuser");
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("사용자 추가 - 빈 사용자명으로 Anonymous 처리")
      void addUser_ShouldUseAnonymous_WhenUsernameIsEmpty() {
          // given
          SimpMessageHeaderAccessor headerAccessor = mock(SimpMessageHeaderAccessor.class);
          Map<String, Object> sessionAttributes = new HashMap<>();
          when(headerAccessor.getSessionAttributes()).thenReturn(sessionAttributes);
          when(headerAccessor.getSessionId()).thenReturn("session123");
          Map<String, String> emptyUserMessage = new HashMap<>();
          emptyUserMessage.put("sender", "");

          // when
          ChatControllerImpl.ChatResponse response =
                  chatController.addUser(emptyUserMessage, headerAccessor);

          // then
          assertThat(response).isNotNull();
          assertThat(response.getType()).isEqualTo("JOIN");
          assertThat(response.getSender()).isEqualTo("Anonymous");
          assertThat(response.getContent()).contains("Anonymous님이 채팅에 참여하셨습니다!");
      }

      @Test
      @DisplayName("개인 메시지 전송 - 성공")
      void sendPrivateMessage_ShouldSendPrivateMessage() {
          // given
          Map<String, String> privateMessage = new HashMap<>();
          privateMessage.put("content", "안녕하세요!");
          privateMessage.put("sender", "sender");
          privateMessage.put("toUser", "receiver");

          // when
          chatController.sendPrivateMessage(privateMessage);

          // 메서드가 void이므로 예외가 발생하지 않으면 성공
      }

      @Test
      @DisplayName("개인 메시지 전송 - 자기 자신에게 보내기로 실패")
      void sendPrivateMessage_ShouldThrowException_WhenSendingToSelf() {
          // given
          Map<String, String> selfMessage = new HashMap<>();
          selfMessage.put("content", "안녕하세요!");
          selfMessage.put("sender", "testuser");
          selfMessage.put("toUser", "testuser");

          // when & then
          assertThatThrownBy(() -> chatController.sendPrivateMessage(selfMessage))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("자기 자신에게 메시지를 보낼 수 없습니다");
      }

      @Test
      @DisplayName("타이핑 상태 전송 - 성공")
      void typing_ShouldReturnTypingResponse() {
          // given
          String roomId = "testroom";
          Map<String, String> typingMessage = new HashMap<>();
          typingMessage.put("sender", "testuser");
          typingMessage.put("isTyping", "true");

          // when
          ChatControllerImpl.ChatResponse response = chatController.typing(roomId, typingMessage);

          // then
          assertThat(response).isNotNull();
          assertThat(response.getType()).isEqualTo("TYPING");
          assertThat(response.getRoomId()).isEqualTo(roomId);
          assertThat(response.getSender()).isEqualTo("testuser");
          assertThat(response.isTyping()).isTrue();
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("방 참여 - 성공")
      void joinRoom_ShouldReturnJoinResponse() {
          // given
          String roomId = "testroom";
          Map<String, String> userMessage = new HashMap<>();
          userMessage.put("sender", "testuser");

          SimpMessageHeaderAccessor headerAccessor = mock(SimpMessageHeaderAccessor.class);
          when(headerAccessor.getSessionId()).thenReturn("session123");

          // when
          ChatControllerImpl.ChatResponse response =
                  chatController.joinRoom(roomId, userMessage, headerAccessor);

          // then
          assertThat(response).isNotNull();
          assertThat(response.getType()).isEqualTo("JOIN");
          assertThat(response.getRoomId()).isEqualTo(roomId);
          assertThat(response.getContent()).contains("testuser님이 방에 참여하셨습니다!");
          assertThat(response.getSender()).isEqualTo("testuser");
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("입력 검증 - null 메시지")
      void validateChatMessage_ShouldThrowException_WhenMessageIsNull() {
          // when & then
          assertThatThrownBy(() -> chatController.sendMessage(null))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("채팅 메시지가 null일 수 없습니다");
      }

      @Test
      @DisplayName("방 ID 검증 - 빈 방 ID")
      void validateRoomId_ShouldThrowException_WhenRoomIdIsEmpty() {
          // when & then
          assertThatThrownBy(() -> chatController.sendRoomMessage("", validChatMessage))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("방 ID가 비어있을 수 없습니다");
      }

      @Test
      @DisplayName("방 ID 검증 - 너무 긴 방 ID")
      void validateRoomId_ShouldThrowException_WhenRoomIdIsTooLong() {
          // given
          String longRoomId = "a".repeat(51);

          // when & then
          assertThatThrownBy(() -> chatController.sendRoomMessage(longRoomId, validChatMessage))
                  .isInstanceOf(BusinessException.class)
                  .hasMessageContaining("방 ID가 너무 깁니다");
      }

      @Test
      @DisplayName("콘텐츠 정제 - 특수문자 제거")
      void sanitizeContent_ShouldRemoveSpecialCharacters() {
          // given
          Map<String, String> messageWithSpecialChars = new HashMap<>();
          messageWithSpecialChars.put("content", "안녕하세요! <script>alert('xss')</script>");
          messageWithSpecialChars.put("sender", "testuser");

          // when
          ChatControllerImpl.ChatResponse response =
                  chatController.sendMessage(messageWithSpecialChars);

          // then
          assertThat(response.getContent()).isEqualTo("안녕하세요! scriptalert(xss)/script");
      }

      @Test
      @DisplayName("사용자명 정제 - 특수문자 제거 및 길이 제한")
      void sanitizeSender_ShouldRemoveSpecialCharactersAndLimitLength() {
          // given
          Map<String, String> messageWithLongSender = new HashMap<>();
          messageWithLongSender.put("content", "테스트 메시지");
          messageWithLongSender.put("sender", "verylongusername@#$%^&*()1234567890");

          // when
          ChatControllerImpl.ChatResponse response =
                  chatController.sendMessage(messageWithLongSender);

          // then
          assertThat(response.getSender()).isEqualTo("verylongusername1234");
          assertThat(response.getSender().length()).isLessThanOrEqualTo(20);
      }
}
