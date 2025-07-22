package org.scoula.global.websocket.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.scoula.global.common.constant.Constants;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequiredArgsConstructor
@Log4j2
public class ChatControllerImpl implements ChatController {

      private final SimpMessagingTemplate messagingTemplate;

      private static final DateTimeFormatter TIMESTAMP_FORMATTER =
              DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

      private final Map<String, Map<String, String>> roomUsers = new ConcurrentHashMap<>();

      @Data
      @Builder
      public static class ChatMessage {
          private String type;
          private String content;
          private String sender;
          private String roomId;
          private String toUser;
          private String timestamp;
          private boolean isTyping;
      }

      @Data
      @Builder
      public static class ChatResponse {
          private String type;
          private String content;
          private String sender;
          private String roomId;
          private String timestamp;
          private boolean isTyping;
          private Map<String, Object> metadata;
      }

      /** {@inheritDoc} */
      @Override
      @MessageMapping("/chat.sendMessage")
      @SendTo("/topic/public")
      public ChatResponse sendMessage(@Payload Map<String, String> chatMessage) {
          validateChatMessage(chatMessage);

          String content = chatMessage.get("content");
          String sender = chatMessage.get("sender");

          log.info(
                  "공개 메시지 수신 - {}: {}",
                  sender,
                  content.length() > 50 ? content.substring(0, 50) + "..." : content);

          return ChatResponse.builder()
                  .type("CHAT")
                  .content(sanitizeContent(content))
                  .sender(sanitizeSender(sender))
                  .timestamp(getCurrentTimestamp())
                  .build();
      }

      /** {@inheritDoc} */
      @Override
      @MessageMapping("/chat.addUser")
      @SendTo("/topic/public")
      public ChatResponse addUser(
              @Payload Map<String, String> chatMessage, SimpMessageHeaderAccessor headerAccessor) {
          String username = sanitizeSender(chatMessage.get("sender"));

          if (!StringUtils.hasText(username)) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "사용자명은 비어있을 수 없습니다");
          }

          headerAccessor.getSessionAttributes().put("username", username);
          addUserToRoom("public", username, headerAccessor.getSessionId());

          log.info("사용자가 공개 채팅에 참여: {}", username);

          return ChatResponse.builder()
                  .type("JOIN")
                  .content(username + "님이 채팅에 참여하셨습니다!")
                  .sender(username)
                  .timestamp(getCurrentTimestamp())
                  .build();
      }

      /** {@inheritDoc} */
      @Override
      @MessageMapping("/chat/{roomId}/sendMessage")
      @SendTo("/topic/room/{roomId}")
      public ChatResponse sendRoomMessage(
              @DestinationVariable String roomId, @Payload Map<String, String> chatMessage) {
          validateRoomId(roomId);
          validateChatMessage(chatMessage);

          String content = chatMessage.get("content");
          String sender = chatMessage.get("sender");

          if (!isUserInRoom(roomId, sender)) {
              throw new BusinessException(
                      CommonErrorCode.UNAUTHORIZED_ACCESS, "방에 대한 접근 권한이 없습니다: " + roomId);
          }

          log.info(
                  "방 {} 메시지 수신 - {}: {}",
                  roomId,
                  sender,
                  content.length() > 50 ? content.substring(0, 50) + "..." : content);

          return ChatResponse.builder()
                  .type("CHAT")
                  .roomId(roomId)
                  .content(sanitizeContent(content))
                  .sender(sanitizeSender(sender))
                  .timestamp(getCurrentTimestamp())
                  .build();
      }

      /** {@inheritDoc} */
      @Override
      @MessageMapping("/chat.sendPrivate")
      public void sendPrivateMessage(@Payload Map<String, String> chatMessage) {
          validatePrivateMessage(chatMessage);

          String content = chatMessage.get("content");
          String sender = chatMessage.get("sender");
          String toUser = chatMessage.get("toUser");

          log.info(
                  "개인 메시지 전송 - {}에서 {}에게: {}",
                  sender,
                  toUser,
                  content.length() > 50 ? content.substring(0, 50) + "..." : content);

          ChatResponse response =
                  ChatResponse.builder()
                          .type("PRIVATE")
                          .content(sanitizeContent(content))
                          .sender(sanitizeSender(sender))
                          .timestamp(getCurrentTimestamp())
                          .build();

          try {
              messagingTemplate.convertAndSendToUser(toUser, "/queue/private", response);
              log.debug("개인 메시지 전송 성공: {}", toUser);
          } catch (Exception e) {
              log.error("개인 메시지 전송 실패: {}", toUser, e);
              throw new BusinessException(
                      CommonErrorCode.INTERNAL_SERVER_ERROR, "개인 메시지 전송에 실패했습니다", e);
          }
      }

      /** {@inheritDoc} */
      @Override
      @MessageMapping("/chat/{roomId}/typing")
      @SendTo("/topic/room/{roomId}/typing")
      public ChatResponse typing(
              @DestinationVariable String roomId, @Payload Map<String, String> typingMessage) {
          validateRoomId(roomId);
          validateTypingMessage(typingMessage);

          String sender = sanitizeSender(typingMessage.get("sender"));
          boolean isTyping = Boolean.parseBoolean(typingMessage.get("isTyping"));

          return ChatResponse.builder()
                  .type("TYPING")
                  .roomId(roomId)
                  .sender(sender)
                  .isTyping(isTyping)
                  .timestamp(getCurrentTimestamp())
                  .build();
      }

      /** {@inheritDoc} */
      @Override
      @MessageMapping("/chat/{roomId}/join")
      @SendTo("/topic/room/{roomId}")
      public ChatResponse joinRoom(
              @DestinationVariable String roomId,
              @Payload Map<String, String> userMessage,
              SimpMessageHeaderAccessor headerAccessor) {
          validateRoomId(roomId);

          String username = sanitizeSender(userMessage.get("sender"));
          String sessionId = headerAccessor.getSessionId();

          addUserToRoom(roomId, username, sessionId);

          log.info("사용자 {}가 방에 참여: {}", username, roomId);

          return ChatResponse.builder()
                  .type("JOIN")
                  .roomId(roomId)
                  .content(username + "님이 방에 참여하셨습니다!")
                  .sender(username)
                  .timestamp(getCurrentTimestamp())
                  .build();
      }

      private String getCurrentTimestamp() {
          return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
      }

      private String sanitizeContent(String content) {
          if (!StringUtils.hasText(content)) {
              return "";
          }
          return content.replaceAll("[<>\"'&]", "");
      }

      private String sanitizeSender(String sender) {
          if (!StringUtils.hasText(sender)) {
              return "Anonymous";
          }
          String sanitized =
                  sender.replaceAll("[<>\"'&@#$%^*()+={}\\[\\]|\\\\:;,.<>?/~`]", "").trim();
          return sanitized.length() > 20 ? sanitized.substring(0, 20) : sanitized;
      }

      private void addUserToRoom(String roomId, String username, String sessionId) {
          roomUsers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(sessionId, username);
      }

      private boolean isUserInRoom(String roomId, String username) {
          Map<String, String> users = roomUsers.get(roomId);
          return users != null && users.containsValue(username);
      }

      private void validateChatMessage(Map<String, String> chatMessage) {
          if (chatMessage == null) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "채팅 메시지가 null일 수 없습니다");
          }

          String content = chatMessage.get("content");
          if (!StringUtils.hasText(content)) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "메시지 내용이 비어있을 수 없습니다");
          }

          if (content.length() > Constants.Chat.MAX_MESSAGE_LENGTH) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "메시지가 너무 깁니다 (최대 1000자)");
          }

          String sender = chatMessage.get("sender");
          if (!StringUtils.hasText(sender)) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "보낸 사람이 비어있을 수 없습니다");
          }
      }

      private void validatePrivateMessage(Map<String, String> chatMessage) {
          validateChatMessage(chatMessage);

          String toUser = chatMessage.get("toUser");
          if (!StringUtils.hasText(toUser)) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "받는 사람이 비어있을 수 없습니다");
          }

          String sender = chatMessage.get("sender");
          if (sender.equals(toUser)) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "자기 자신에게 메시지를 보낼 수 없습니다");
          }
      }

      private void validateTypingMessage(Map<String, String> typingMessage) {
          if (typingMessage == null) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "입력 메시지가 null일 수 없습니다");
          }

          String sender = typingMessage.get("sender");
          if (!StringUtils.hasText(sender)) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "보낸 사람이 비어있을 수 없습니다");
          }
      }

      private void validateRoomId(String roomId) {
          if (!StringUtils.hasText(roomId)) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "방 ID가 비어있을 수 없습니다");
          }

          if (roomId.length() > 50) {
              throw new BusinessException(
                      CommonErrorCode.INVALID_INPUT_VALUE, "방 ID가 너무 깁니다 (최대 50자)");
          }

          if (!roomId.matches("^[a-zA-Z0-9_-]+$")) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "잘못된 방 ID 형식입니다");
          }
      }
}
