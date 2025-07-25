package org.scoula.domain.chat.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.scoula.domain.chat.BadWordFilter;
import org.scoula.domain.chat.dto.*;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.mapper.ChatRoomMapper;
import org.scoula.domain.chat.repository.ChatMessageMongoRepository;
import org.scoula.domain.chat.vo.ChatRoom;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatServiceInterface {

      private final ChatRoomMapper chatRoomMapper;
      private final ChatMessageMongoRepository mongoRepository;
      private final SimpMessagingTemplate messagingTemplate;
      private final S3ServiceInterface s3Service;
      private final UserServiceInterface userService;
      private final BadWordFilter badWordFilter;

      // 온라인 사용자 추적을 위한 Set
      private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

      // 사용자가 현재 어느 채팅방에 있는지 추적
      private final Map<Long, Long> userCurrentChatRoom = new ConcurrentHashMap<>();

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void handleChatMessage(ChatMessageRequestDto dto) {
          log.info("메시지 처리 시작: {}", dto);

          if ("TEXT".equals(dto.getType()) && badWordFilter.containsBadWord(dto.getContent())) {
              log.warn("비속어 포함 메시지 차단 - 사용자: {}, 메시지: {}", dto.getSenderId(), dto.getContent());
              throw new BusinessException(ChatErrorCode.BAD_WORD_DETECTED, "비속어가 포함되어 있습니다.");
          }

          ChatRoom chatRoom = chatRoomMapper.findById(dto.getChatRoomId());
          if (chatRoom == null) {
              log.error("채팅방을 찾을 수 없음: {}", dto.getChatRoomId());
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }
          log.info("채팅방 확인 완료: {}", chatRoom.getChatRoomId());

          if (!isUserInChatRoom(dto.getChatRoomId(), dto.getSenderId())) {
              log.error(
                      "사용자가 채팅방에 속해있지 않음: userId={}, roomId={}",
                      dto.getSenderId(),
                      dto.getChatRoomId());
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }
          log.info("✅ 사용자 권한 확인 완료");

          try {
              boolean receiverInThisChatRoom =
                      isUserInCurrentChatRoom(dto.getReceiverId(), dto.getChatRoomId());

              log.info(
                      "📍 상대방 상태 확인: userId={}, 온라인={}, 현재채팅방접속={}",
                      dto.getReceiverId(),
                      onlineUsers.contains(dto.getReceiverId()),
                      receiverInThisChatRoom);

              ChatMessageDocument message =
                      ChatMessageDocument.builder()
                              .chatRoomId(dto.getChatRoomId())
                              .senderId(dto.getSenderId())
                              .receiverId(dto.getReceiverId())
                              .type(dto.getType())
                              .content(dto.getContent())
                              .fileUrl(dto.getFileUrl())
                              .isRead(receiverInThisChatRoom)
                              .sendTime(Instant.now().toString())
                              .build();

              log.info(
                      "📝 MongoDB 저장: 메시지={}, 즉시읽음={}", message.getContent(), receiverInThisChatRoom);

              mongoRepository.saveMessage(dto.getChatRoomId(), message);

              // 채팅방 최근 메시지 업데이트
              String preview = dto.getType().equals("TEXT") ? dto.getContent() : "[파일]";
              LocalDateTime now = LocalDateTime.now();

              log.info("채팅방 최근 메시지 업데이트: 내용={}, 시간={}", preview, now);
              chatRoomMapper.updateLastMessage(dto.getChatRoomId(), preview, now);

              // 실시간 메시지 전송 (채팅방 구독자들에게)
              String topicPath = "/topic/chatroom/" + dto.getChatRoomId();
              messagingTemplate.convertAndSend(topicPath, message);
              log.info("채팅방 메시지 전송 완료");

              // 각 사용자별로 개별 읽지 않은 메시지 수 계산 및 전송
              // 이미 조회된 chatRoom 객체 재사용 (중복 조회 방지)

              // 소유자(owner)의 읽지 않은 메시지 수
              int ownerUnreadCount =
                      mongoRepository.countUnreadMessages(dto.getChatRoomId(), chatRoom.getOwnerId());
              ChatRoomUpdateDto ownerUpdateDto = new ChatRoomUpdateDto();
              ownerUpdateDto.setRoomId(dto.getChatRoomId());
              ownerUpdateDto.setLastMessage(preview);
              ownerUpdateDto.setTimestamp(now.toString());
              ownerUpdateDto.setUnreadCount(ownerUnreadCount);
              ownerUpdateDto.setSenderId(dto.getSenderId());

              // 구매자(buyer)의 읽지 않은 메시지 수
              int buyerUnreadCount =
                      mongoRepository.countUnreadMessages(dto.getChatRoomId(), chatRoom.getBuyerId());
              ChatRoomUpdateDto buyerUpdateDto = new ChatRoomUpdateDto();
              buyerUpdateDto.setRoomId(dto.getChatRoomId());
              buyerUpdateDto.setLastMessage(preview);
              buyerUpdateDto.setTimestamp(now.toString());
              buyerUpdateDto.setUnreadCount(buyerUnreadCount);
              buyerUpdateDto.setSenderId(dto.getSenderId());

              // 각각 다른 읽지 않은 수로 개별 전송
              String ownerTopic = "/topic/user/" + chatRoom.getOwnerId() + "/chatrooms";
              String buyerTopic = "/topic/user/" + chatRoom.getBuyerId() + "/chatrooms";

              messagingTemplate.convertAndSend(ownerTopic, ownerUpdateDto);
              messagingTemplate.convertAndSend(buyerTopic, buyerUpdateDto);

              log.info("메시지 처리 완료: 소유자읽지않은수={}, 구매자읽지않은수={}", ownerUnreadCount, buyerUnreadCount);

          } catch (Exception e) {
              log.error("메시지 전송 실패", e);
              throw new BusinessException(ChatErrorCode.MESSAGE_SEND_FAILED, e.getMessage());
          }
      }

      // 사용자가 현재 특정 채팅방에 접속해 있는지 확인
      /** {@inheritDoc} */
      private boolean isUserInCurrentChatRoom(Long userId, Long chatRoomId) {
          Long currentRoom = userCurrentChatRoom.get(userId);
          boolean isInRoom = chatRoomId.equals(currentRoom);

          log.info(
                  "사용자 현재 위치: userId={}, 현재채팅방={}, 확인채팅방={}, 결과={}",
                  userId,
                  currentRoom,
                  chatRoomId,
                  isInRoom);

          return isInRoom;
      }

      // 사용자가 채팅방에 입장했을 때 호출
      public void setUserCurrentChatRoom(Long userId, Long chatRoomId) {
          userCurrentChatRoom.put(userId, chatRoomId);
          addOnlineUser(userId); // 채팅방 입장시 온라인으로 설정

          log.info("사용자 채팅방 입장: userId={}, chatRoomId={}", userId, chatRoomId);

          markChatRoomAsRead(chatRoomId, userId);
      }

      // 사용자가 채팅방을 떠났을 때 호출
      public void removeUserFromCurrentChatRoom(Long userId) {
          Long previousRoom = userCurrentChatRoom.remove(userId);

          log.info("사용자 채팅방 퇴장: userId={}, 이전채팅방={}", userId, previousRoom);
      }

      // 사용자 완전 오프라인 (앱 종료 등)
      public void setUserOffline(Long userId) {
          removeOnlineUser(userId);
          removeUserFromCurrentChatRoom(userId);

          log.info("사용자 완전 오프라인: userId={}", userId);
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public Long createChatRoom(Long ownerId, Long buyerId, Long propertyId) {
          if (buyerId.equals(ownerId)) {
              throw new BusinessException(ChatErrorCode.SELF_CHAT_NOT_ALLOWED);
          }

          ChatRoom existing = chatRoomMapper.findByUserAndHome(ownerId, buyerId, propertyId);
          if (existing != null) {
              log.info("기존 채팅방 반환 - ID: {}", existing.getChatRoomId());
              return existing.getChatRoomId();
          }

          try {
              ChatRoom room = new ChatRoom();
              room.setOwnerId(ownerId);
              room.setBuyerId(buyerId);
              room.setHomeId(propertyId);
              room.setCreatedAt(LocalDateTime.now());
              room.setUnreadMessageCount(0);

              chatRoomMapper.insertChatRoom(room);

              log.info(
                      "새 채팅방 생성 완료 - ID: {}, 소유자: {}, 구매자: {}, 매물: {}",
                      room.getChatRoomId(),
                      ownerId,
                      buyerId,
                      propertyId);

              return room.getChatRoomId();
          } catch (Exception e) {
              log.error("채팅방 생성 실패 - 소유자: {}, 구매자: {}, 매물: {}", ownerId, buyerId, propertyId, e);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ALREADY_EXISTS, "채팅방 생성에 실패했습니다");
          }
      }

      /** {@inheritDoc} */
      public List<ChatRoomWithUserInfoDto> getRoomsAsOwner(Long ownerId) {
          List<ChatRoom> rooms = chatRoomMapper.findByOwnerId(ownerId);
          return rooms.stream()
                  .map(
                          room -> {
                              User otherUser = userService.findById(room.getBuyerId());
                              int unreadCount =
                                      mongoRepository.countUnreadMessages(
                                              room.getChatRoomId(), ownerId);
                              ChatRoomWithUserInfoDto dto =
                                      ChatRoomWithUserInfoDto.of(room, otherUser, ownerId);
                              dto.setUnreadMessageCount(unreadCount);
                              return dto;
                          })
                  .collect(Collectors.toList());
      }

      /** {@inheritDoc} */
      @Override
      public List<ChatRoomWithUserInfoDto> getRoomsAsBuyer(Long buyerId) {
          List<ChatRoom> rooms = chatRoomMapper.findByBuyerId(buyerId);

          return rooms.stream()
                  .map(
                          room -> {
                              User otherUser = userService.findById(room.getOwnerId());
                              int unreadCount =
                                      mongoRepository.countUnreadMessages(
                                              room.getChatRoomId(), buyerId);
                              ChatRoomWithUserInfoDto dto =
                                      ChatRoomWithUserInfoDto.of(room, otherUser, buyerId);
                              dto.setUnreadMessageCount(unreadCount);
                              return dto;
                          })
                  .collect(Collectors.toList());
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public List<ChatMessageDocument> getMessages(Long chatRoomId, Long userId) {
          ChatRoom chatRoom = chatRoomMapper.findById(chatRoomId);
          if (chatRoom == null) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }

          if (!isUserInChatRoom(chatRoomId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          try {
              // 메시지 조회 시에도 읽음 처리
              mongoRepository.markAsRead(chatRoomId, userId);

              // 읽지 않은 메시지 수 업데이트
              int unreadCount = mongoRepository.countUnreadMessages(chatRoomId, userId);
              chatRoomMapper.updateUnreadCount(chatRoomId, unreadCount);

              List<ChatMessageDocument> messages = mongoRepository.getMessages(chatRoomId);
              log.info(
                      "채팅 메시지 조회 완료 - 채팅방: {}, 메시지 수: {}, 읽음처리후 남은 읽지않은수: {}",
                      chatRoomId,
                      messages.size(),
                      unreadCount);

              return messages;
          } catch (Exception e) {
              log.error("메시지 조회 실패 - 채팅방: {}, 사용자: {}", chatRoomId, userId, e);
              throw new BusinessException(ChatErrorCode.MESSAGE_SEND_FAILED, "메시지 조회에 실패했습니다");
          }
      }

      /** {@inheritDoc} */
      @Override
      public Long getPropertyOwnerId(Long propertyId) {
          Long ownerId = chatRoomMapper.findPropertyOwnerId(propertyId);

          if (ownerId == null) {
              throw new BusinessException(ChatErrorCode.PROPERTY_NOT_FOUND, "매물 ID: " + propertyId);
          }

          return ownerId;
      }

      /** {@inheritDoc} */
      @Override
      public boolean isUserInChatRoom(Long chatRoomId, Long userId) {
          ChatRoom chatRoom = chatRoomMapper.findById(chatRoomId);

          if (chatRoom == null) {
              return false;
          }

          return userId.equals(chatRoom.getOwnerId()) || userId.equals(chatRoom.getBuyerId());
      }

      /** {@inheritDoc} */
      @Override
      public boolean existsChatRoom(Long ownerId, Long buyerId, Long propertyId) {
          ChatRoom existing = chatRoomMapper.findByUserAndHome(ownerId, buyerId, propertyId);
          return existing != null;
      }

      /** {@inheritDoc} */
      @Override
      public List<Map<String, Object>> getChatMediaFiles(
              Long chatRoomId, int page, int size, String mediaType) {
          ChatRoom chatRoom = chatRoomMapper.findById(chatRoomId);
          if (chatRoom == null) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }

          try {
              List<ChatMessageDocument> allMessages = mongoRepository.getMessages(chatRoomId);

              List<ChatMessageDocument> mediaMessages =
                      allMessages.stream()
                              .filter(message -> isMediaMessage(message, mediaType))
                              .sorted(
                                      (m1, m2) ->
                                              m2.getSendTime().compareTo(m1.getSendTime())) // 최신순 정렬
                              .skip((long) page * size)
                              .limit(size)
                              .collect(Collectors.toList());

              return mediaMessages.stream()
                      .map(this::convertToMediaResponse)
                      .collect(Collectors.toList());
          } catch (Exception e) {
              log.error("미디어 파일 조회 실패 - 채팅방: {}", chatRoomId, e);
              throw new BusinessException(ChatErrorCode.FILE_NOT_FOUND, "미디어 파일 조회에 실패했습니다");
          }
      }

      /** 미디어 메시지인지 확인 */
      private boolean isMediaMessage(ChatMessageDocument message, String mediaType) {
          String messageType = message.getType();

          if (!"FILE".equals(messageType)) {
              return false;
          }

          if (message.getFileUrl() == null || message.getFileUrl().isEmpty()) {
              return false;
          }

          String fileType = determineFileTypeFromUrl(message.getFileUrl());

          switch (mediaType.toUpperCase()) {
              case "IMAGE":
                  return "IMAGE".equals(fileType);
              case "VIDEO":
                  return "VIDEO".equals(fileType);
              case "ALL":
                  return "IMAGE".equals(fileType) || "VIDEO".equals(fileType);
              default:
                  return false;
          }
      }

      /** URL에서 파일 타입 결정 */
      private String determineFileTypeFromUrl(String fileUrl) {
          String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
          String extension = "";

          int lastDotIndex = fileName.lastIndexOf('.');
          if (lastDotIndex != -1) {
              extension = fileName.substring(lastDotIndex + 1).toLowerCase();
          }

          List<String> imageExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
          List<String> videoExtensions = Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "webm");

          if (imageExtensions.contains(extension)) {
              return "IMAGE";
          } else if (videoExtensions.contains(extension)) {
              return "VIDEO";
          }

          return "FILE";
      }

      /** 미디어 응답 형태로 변환 */
      private Map<String, Object> convertToMediaResponse(ChatMessageDocument message) {
          Map<String, Object> response = new HashMap<>();

          String fileUrl = message.getFileUrl();
          String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
          String fileType = determineFileTypeFromUrl(fileUrl);

          response.put("messageId", message.getId());
          response.put("fileUrl", fileUrl);
          response.put("fileName", fileName);
          response.put("fileType", fileType);
          response.put("senderId", message.getSenderId());
          response.put("sendTime", message.getSendTime());

          if ("IMAGE".equals(fileType)) {
              response.put("thumbnailUrl", generateThumbnailUrl(fileUrl));
          }

          try {
              String s3Key = extractS3KeyFromUrl(fileUrl);
              if (s3Key != null && s3Service.fileExists(s3Key)) {
                  response.put("fileSize", s3Service.getFileSize(s3Key));
              }
          } catch (Exception e) {
              log.warn("파일 크기 조회 실패: {}", fileUrl, e);
          }

          return response;
      }

      /** 썸네일 URL 생성 (이미지용) */
      private String generateThumbnailUrl(String originalUrl) {
          return originalUrl; // 현재는 원본 URL 사용
      }

      /** URL에서 S3 키 추출 */
      private String extractS3KeyFromUrl(String fileUrl) {
          try {
              if (fileUrl.contains(".amazonaws.com/")) {
                  String[] parts = fileUrl.split(".amazonaws.com/");
                  if (parts.length > 1) {
                      return parts[1];
                  }
              }
              return null;
          } catch (Exception e) {
              return null;
          }
      }

      /** 사용자 온라인 상태 관리 */
      public void addOnlineUser(Long userId) {
          onlineUsers.add(userId);
          log.info("👤 사용자 온라인: {}, 총 온라인: {}", userId, onlineUsers.size());
      }

      public void removeOnlineUser(Long userId) {
          onlineUsers.remove(userId);
          log.info("👤 사용자 오프라인: {}, 총 온라인: {}", userId, onlineUsers.size());
      }

      public boolean isUserOnline(Long userId) {
          return onlineUsers.contains(userId);
      }

      @Transactional
      public void markChatRoomAsRead(Long chatRoomId, Long userId) {
          log.info("채팅방 읽음 처리: chatRoomId={}, userId={}", chatRoomId, userId);

          mongoRepository.markAsRead(chatRoomId, userId);

          int unreadCount = mongoRepository.countUnreadMessages(chatRoomId, userId);
          chatRoomMapper.updateUnreadCount(chatRoomId, unreadCount);

          log.info("읽음 처리 완료: 남은 읽지 않은 메시지={}", unreadCount);

          ChatRoom chatRoom = chatRoomMapper.findById(chatRoomId);
          if (chatRoom != null) {
              ChatRoomUpdateDto updateDto = new ChatRoomUpdateDto();
              updateDto.setRoomId(chatRoomId);
              updateDto.setUnreadCount(unreadCount);
              updateDto.setTimestamp(LocalDateTime.now().toString());

              if (chatRoom.getLastMessage() != null && !chatRoom.getLastMessage().isEmpty()) {
                  updateDto.setLastMessage(chatRoom.getLastMessage());
              }

              updateDto.setSenderId(null);

              String userTopic = "/topic/user/" + userId + "/chatrooms";
              messagingTemplate.convertAndSend(userTopic, updateDto);

              log.info(
                      "📖 읽음 처리 웹소켓 알림 전송 완료: userId={}, unreadCount={}, lastMessage={}",
                      userId,
                      unreadCount,
                      updateDto.getLastMessage());
          }
      }

      public Map<Long, Long> getCurrentChatRoomStatus() {
          return new HashMap<>(userCurrentChatRoom);
      }

      public Set<Long> getOnlineUsers() {
          return new HashSet<>(onlineUsers);
      }

      @Override
      public ChatRoomInfoDto getChatRoomInfo(Long chatRoomId, Long userId) {
          ChatRoomInfoDto chatRoomInfo =
                  chatRoomMapper.getChatRoomInfoWithProperty(chatRoomId, userId);
          if (chatRoomInfo == null) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }
          return chatRoomInfo;
      }
}
