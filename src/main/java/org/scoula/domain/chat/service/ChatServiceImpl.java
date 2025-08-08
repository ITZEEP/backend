package org.scoula.domain.chat.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.scoula.domain.chat.BadWordFilter;
import org.scoula.domain.chat.document.ChatMessageDocument;
import org.scoula.domain.chat.dto.*;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.fcm.FCMService;
import org.scoula.domain.chat.mapper.ChatRoomMapper;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.repository.ChatMessageMongoRepository;
import org.scoula.domain.chat.vo.ChatRoom;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class ChatServiceImpl implements ChatServiceInterface {

      private final ChatRoomMapper chatRoomMapper;
      private final ChatMessageMongoRepository mongoRepository;
      private final SimpMessagingTemplate messagingTemplate;
      private final S3ServiceInterface s3Service;
      private final UserServiceInterface userService;
      private final BadWordFilter badWordFilter;
      private final FCMService fcmService;
      private final NotificationServiceInterface notificationService;

      // 온라인 사용자 추적을 위한 Set
      private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

      // 사용자가 현재 어느 채팅방에 있는지 추적
      private final Map<Long, Long> userCurrentChatRoom = new ConcurrentHashMap<>();

      private final ContractChatMapper contractChatMapper;
      private final RedisTemplate<String, String> stringRedisTemplate;

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void handleChatMessage(ChatMessageRequestDto dto) {
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

          try {
              boolean receiverInThisChatRoom =
                      isUserInCurrentChatRoom(dto.getReceiverId(), dto.getChatRoomId());

              log.info(
                      "메시지 읽음 상태 계산: receiverId={}, chatRoomId={}, 현재접속={}",
                      dto.getReceiverId(),
                      dto.getChatRoomId(),
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

              mongoRepository.saveMessage(dto.getChatRoomId(), message);

              String preview = dto.getType().equals("TEXT") ? dto.getContent() : "[파일]";
              LocalDateTime now = LocalDateTime.now();

              chatRoomMapper.updateLastMessage(dto.getChatRoomId(), preview, now);

              String topicPath = "/topic/chatroom/" + dto.getChatRoomId();
              messagingTemplate.convertAndSend(topicPath, message);

              boolean ownerInThisChatRoom =
                      isUserInCurrentChatRoom(chatRoom.getOwnerId(), dto.getChatRoomId());
              int ownerUnreadCount;

              if (ownerInThisChatRoom) {
                  ownerUnreadCount = 0;
                  log.info("소유자 현재 접속 중 - 읽지 않은 메시지 수: 0, ownerId={}", chatRoom.getOwnerId());
              } else {
                  ownerUnreadCount =
                          mongoRepository.countUnreadMessages(
                                  dto.getChatRoomId(), chatRoom.getOwnerId());
                  log.info(
                          "소유자 접속하지 않음 - 읽지 않은 메시지 수: {}, ownerId={}",
                          ownerUnreadCount,
                          chatRoom.getOwnerId());
              }

              boolean buyerInThisChatRoom =
                      isUserInCurrentChatRoom(chatRoom.getBuyerId(), dto.getChatRoomId());
              int buyerUnreadCount;

              if (buyerInThisChatRoom) {
                  buyerUnreadCount = 0;
                  log.info("구매자 현재 접속 중 - 읽지 않은 메시지 수: 0, buyerId={}", chatRoom.getBuyerId());
              } else {
                  buyerUnreadCount =
                          mongoRepository.countUnreadMessages(
                                  dto.getChatRoomId(), chatRoom.getBuyerId());
                  log.info(
                          "구매자 접속하지 않음 - 읽지 않은 메시지 수: {}, buyerId={}",
                          buyerUnreadCount,
                          chatRoom.getBuyerId());
              }

              // 소유자용 업데이트 DTO 생성
              ChatRoomUpdateDto ownerUpdateDto = new ChatRoomUpdateDto();
              ownerUpdateDto.setRoomId(dto.getChatRoomId());
              ownerUpdateDto.setLastMessage(preview);
              ownerUpdateDto.setTimestamp(now.toString());
              ownerUpdateDto.setUnreadCount(ownerUnreadCount);
              ownerUpdateDto.setSenderId(dto.getSenderId());

              // 구매자용 업데이트 DTO 생성
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

              if (!"START".equals(dto.getType())) {
                  try {
                      User sender = userService.findById(dto.getSenderId());
                      String senderName = sender != null ? sender.getNickname() : "알 수 없는 사용자";

                      Map<String, String> baseNotificationData = new HashMap<>();
                      baseNotificationData.put("chatRoomId", dto.getChatRoomId().toString());
                      baseNotificationData.put("senderId", dto.getSenderId().toString());
                      baseNotificationData.put("senderName", senderName);
                      baseNotificationData.put("type", "chat_message");
                      baseNotificationData.put(
                              "timestamp", String.valueOf(System.currentTimeMillis()));

                      String notificationBody;
                      String notificationTitle;

                      switch (dto.getType()) {
                          case "TEXT":
                              notificationBody = dto.getContent();
                              notificationTitle = senderName + "님의 새 메시지";
                              break;
                          case "FILE":
                              notificationBody =
                                      "[파일] "
                                              + (dto.getContent() != null
                                                      ? dto.getContent()
                                                      : "파일을 보냈습니다");
                              notificationTitle = senderName + "님이 파일을 보냈습니다";
                              break;
                          case "CONTRACT_REQUEST":
                              notificationBody = "계약을 요청했습니다";
                              notificationTitle = senderName + "님의 계약 요청";
                              break;
                          case "CONTRACT_REJECT":
                              notificationBody = "계약 요청을 거절했습니다";
                              notificationTitle = senderName + "님의 계약 거절";
                              break;
                          default:
                              notificationBody =
                                      dto.getContent() != null ? dto.getContent() : "새 메시지";
                              notificationTitle = senderName + "님의 새 메시지";
                              break;
                      }

                      // 메시지 수신자 결정 (발신자가 아닌 사용자)
                      Long receiverId =
                              dto.getSenderId().equals(chatRoom.getOwnerId())
                                      ? chatRoom.getBuyerId()
                                      : chatRoom.getOwnerId();

                      receiverInThisChatRoom =
                              isUserInCurrentChatRoom(receiverId, dto.getChatRoomId());

                      log.info("=== 알림 생성 체크 ===");
                      log.info(
                              "발신자: {}, 수신자: {}, 수신자 접속 여부: {}",
                              dto.getSenderId(),
                              receiverId,
                              receiverInThisChatRoom);
                      log.info("채팅방 소유자: {}, 구매자: {}", chatRoom.getOwnerId(), chatRoom.getBuyerId());

                      // 수신자가 현재 채팅방에 접속하지 않은 경우에만 알림 생성
                      if (!receiverInThisChatRoom) {
                          int receiverUnreadCount =
                                  receiverId.equals(chatRoom.getOwnerId())
                                          ? ownerUnreadCount
                                          : buyerUnreadCount;

                          Map<String, String> receiverNotificationData =
                                  new HashMap<>(baseNotificationData);
                          receiverNotificationData.put(
                                  "unreadCount", String.valueOf(receiverUnreadCount));
                          receiverNotificationData.put("userId", receiverId.toString());

                          log.info(
                                  "알림 생성 시작: receiverId={}, notificationData={}",
                                  receiverId,
                                  receiverNotificationData);

                          notificationService.createChatNotification(
                                  receiverId,
                                  senderName,
                                  notificationBody,
                                  dto.getChatRoomId(),
                                  receiverNotificationData);

                          log.info(
                                  "수신자 알림 처리 완료: receiverId={}, senderId={}, unreadCount={}",
                                  receiverId,
                                  dto.getSenderId(),
                                  receiverUnreadCount);
                      } else {
                          log.info(
                                  "수신자가 현재 채팅방에 접속 중이므로 알림 생성 안함: receiverId={}, chatRoomId={}",
                                  receiverId,
                                  dto.getChatRoomId());
                      }

                  } catch (Exception notificationException) {
                      log.error(
                              "통합 알림 처리 실패 - 채팅방: {}, 발신자: {}, 수신자들: [소유자={}, 구매자={}]",
                              dto.getChatRoomId(),
                              dto.getSenderId(),
                              chatRoom.getOwnerId(),
                              chatRoom.getBuyerId(),
                              notificationException);
                  }
              } else {
                  log.info("START 메시지는 알림 제외: chatRoomId={}", dto.getChatRoomId());
              }
          } catch (Exception e) {
              log.error("메시지 전송 실패", e);
              throw new BusinessException(ChatErrorCode.MESSAGE_SEND_FAILED, e.getMessage());
          }
      }

      // 사용자가 현재 특정 채팅방에 접속해 있는지 확인
      /** {@inheritDoc} */
      private boolean isUserInCurrentChatRoom(Long userId, Long chatRoomId) {
          Long currentRoom = userCurrentChatRoom.get(userId);
          return chatRoomId.equals(currentRoom);
      }

      // 사용자가 채팅방에 입장했을 때 호출
      public void setUserCurrentChatRoom(Long userId, Long chatRoomId) {
          userCurrentChatRoom.put(userId, chatRoomId);
          addOnlineUser(userId);
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

              Long chatRoomId = room.getChatRoomId();
              ChatMessageRequestDto startMessage =
                      ChatMessageRequestDto.builder()
                              .chatRoomId(chatRoomId)
                              .senderId(buyerId)
                              .receiverId(ownerId)
                              .content("채팅을 시작합니다! 상대방을 위해 채팅 에티켓을 지켜서 진행해주세요!")
                              .type("START")
                              .build();
              handleChatMessage(startMessage);
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
      @Override
      public void addOnlineUser(Long userId) {
          onlineUsers.add(userId);
      }

      public void removeOnlineUser(Long userId) {
          onlineUsers.remove(userId);
      }

      @Override
      public boolean isUserOnline(Long userId) {
          return onlineUsers.contains(userId);
      }

      @Transactional
      public void markChatRoomAsRead(Long chatRoomId, Long userId) {

          mongoRepository.markAsRead(chatRoomId, userId);

          int unreadCount = mongoRepository.countUnreadMessages(chatRoomId, userId);
          chatRoomMapper.updateUnreadCount(chatRoomId, unreadCount);

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
          }
      }

      public Map<Long, Long> getCurrentChatRoomStatus() {
          return new HashMap<>(userCurrentChatRoom);
      }

      public Set<Long> getOnlineUsers() {
          return new HashSet<>(onlineUsers);
      }

      // 🔧 추가: 사용자가 특정 계약 채팅방에 있는지 확인
      @Override
      public boolean isUserInContractChatRoom(Long userId, Long contractChatId) {
          Long currentRoom = userCurrentChatRoom.get(userId);
          boolean isInRoom = contractChatId.equals(currentRoom);
          boolean isOnline = onlineUsers.contains(userId);

          log.debug(
                  "사용자 계약 채팅방 상태 확인: userId={}, contractChatId={}, currentRoom={}, isInRoom={},"
                          + " isOnline={}",
                  userId,
                  contractChatId,
                  currentRoom,
                  isInRoom,
                  isOnline);

          return isInRoom && isOnline;
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

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void sendContractRequest(Long chatRoomId, Long userId) {
          if (!isUserInChatRoom(chatRoomId, userId)) {
              log.warn("사용자가 채팅방에 속하지 않음: chatRoomId={}, userId={}", chatRoomId, userId);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ChatRoom chatRoom = getChatRoomById(chatRoomId);
          if (chatRoom == null) {
              log.error("채팅방을 찾을 수 없음: chatRoomId={}", chatRoomId);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }

          if (!userId.equals(chatRoom.getBuyerId())) {
              log.warn("구매자가 아닌 사용자의 계약 요청: userId={}, buyerId={}", userId, chatRoom.getBuyerId());
              throw new BusinessException(ChatErrorCode.ONLY_BUYER_CAN_REQUEST_CONTRACT);
          }

          ContractChat existingContract =
                  contractChatMapper.findByUserAndHome(
                          chatRoom.getOwnerId(), chatRoom.getBuyerId(), chatRoom.getHomeId());

          if (existingContract != null) {
              log.warn("이미 계약 채팅방이 존재함: contractChatId={}", existingContract.getContractChatId());
              throw new BusinessException(ChatErrorCode.CONTRACT_ALREADY_EXISTS, "이미 계약 채팅방이 존재합니다.");
          }
          String key = "chat:request-contract:" + chatRoomId;
          String existingValue = stringRedisTemplate.opsForValue().get(key);
          if (existingValue != null) {
              log.warn("이미 계약 요청이 존재함: key={}, value={}", key, existingValue);
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_ALREADY_EXISTS, "이미 계약 요청이 진행 중입니다.");
          }

          ChatMessageRequestDto contractRequestMessage =
                  ChatMessageRequestDto.builder()
                          .chatRoomId(chatRoomId)
                          .senderId(userId)
                          .receiverId(chatRoom.getOwnerId())
                          .content("계약을 요청했습니다.")
                          .type("CONTRACT_REQUEST")
                          .build();

          String value = userId.toString();
          stringRedisTemplate.opsForValue().set(key, value);

          handleChatMessage(contractRequestMessage);
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void rejectContractRequest(Long chatRoomId, Long userId) {
          log.info("=== 계약 요청 거절 처리 시작 ===");
          log.info("chatRoomId: {}, userId: {}", chatRoomId, userId);

          // 1. 사용자가 채팅방에 속해있는지 확인
          if (!isUserInChatRoom(chatRoomId, userId)) {
              log.warn("사용자가 채팅방에 속하지 않음: chatRoomId={}, userId={}", chatRoomId, userId);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          // 2. 채팅방 정보 조회
          ChatRoom chatRoom = getChatRoomById(chatRoomId);
          if (chatRoom == null) {
              log.error("채팅방을 찾을 수 없음: chatRoomId={}", chatRoomId);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }

          // 3. 거절자가 소유자(판매자)인지 확인
          if (!userId.equals(chatRoom.getOwnerId())) {
              log.warn("소유자가 아닌 사용자의 계약 거절: userId={}, ownerId={}", userId, chatRoom.getOwnerId());
              throw new BusinessException(ChatErrorCode.ONLY_OWNER_CAN_REJECT_CONTRACT);
          }

          log.info("계약 거절 권한 확인 완료: 소유자={}, 구매자={}", chatRoom.getOwnerId(), chatRoom.getBuyerId());

          ChatMessageRequestDto contractRejectMessage =
                  ChatMessageRequestDto.builder()
                          .chatRoomId(chatRoomId)
                          .senderId(userId)
                          .receiverId(chatRoom.getBuyerId())
                          .content("계약 요청을 거절했습니다.")
                          .type("CONTRACT_REJECT")
                          .build();

          String key = "chat:request-contract:" + chatRoomId;
          stringRedisTemplate.delete(key);
          handleChatMessage(contractRejectMessage);
      }

      /** {@inheritDoc} */
      @Override
      @Transactional(rollbackFor = Exception.class)
      public Long acceptContractRequest(Long chatRoomId, Long userId) {
          ChatRoom originalChatRoom = chatRoomMapper.findById(chatRoomId);
          if (originalChatRoom == null) {
              log.error("채팅방을 찾을 수 없음: {}", chatRoomId);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }

          if (!userId.equals(originalChatRoom.getOwnerId())) {
              log.error("계약 수락 권한 없음: userId={}, ownerId={}", userId, originalChatRoom.getOwnerId());
              throw new BusinessException(
                      ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "매물 소유자만 계약을 수락할 수 있습니다.");
          }

          String key = "chat:request-contract:" + chatRoomId;
          String storedBuyerId = stringRedisTemplate.opsForValue().get(key);

          if (storedBuyerId == null) {
              log.warn("계약 요청이 존재하지 않음: chatRoomId={}", chatRoomId);
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_REQUEST_NOT_FOUND, "계약 요청이 존재하지 않습니다.");
          }

          if (!storedBuyerId.equals(originalChatRoom.getBuyerId().toString())) {
              log.warn(
                      "Redis 구매자 ID 불일치: stored={}, expected={}",
                      storedBuyerId,
                      originalChatRoom.getBuyerId());
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_REQUEST_NOT_FOUND, "계약 요청 정보가 유효하지 않습니다.");
          }

          ContractChat existingContract =
                  contractChatMapper.findByUserAndHome(
                          originalChatRoom.getOwnerId(),
                          originalChatRoom.getBuyerId(),
                          originalChatRoom.getHomeId());

          if (existingContract != null) {
              stringRedisTemplate.delete(key);
              return existingContract.getContractChatId();
          }

          stringRedisTemplate.delete(key);
          ContractChat contractChat = new ContractChat();
          contractChat.setHomeId(originalChatRoom.getHomeId());
          contractChat.setOwnerId(originalChatRoom.getOwnerId());
          contractChat.setBuyerId(originalChatRoom.getBuyerId());
          contractChat.setContractStartAt(LocalDateTime.now());
          contractChat.setLastMessage("계약 채팅방이 생성되었습니다.");
          contractChat.setStatus(ContractChat.ContractStatus.STEP0);

          contractChatMapper.createContractChat(contractChat);
          Long contractChatRoomId = contractChat.getContractChatId();
          ChatMessageRequestDto acceptMessage =
                  ChatMessageRequestDto.builder()
                          .chatRoomId(chatRoomId)
                          .senderId(userId)
                          .receiverId(originalChatRoom.getBuyerId())
                          .content("계약 요청을 수락했습니다. 계약 채팅방이 생성되었습니다.")
                          .type("TEXT")
                          .build();

          handleChatMessage(acceptMessage);
          String contractChatUrl =
                  "http://localhost:5173/pre-contract/"
                          + contractChatRoomId.toString()
                          + "/owner?step=1"
                          + "/n"
                          + "http://localhost:5173/pre-contract/"
                          + contractChatRoomId.toString()
                          + "/buyer?step=1";

          ChatMessageRequestDto linkMessage =
                  ChatMessageRequestDto.builder()
                          .chatRoomId(chatRoomId)
                          .senderId(userId)
                          .receiverId(originalChatRoom.getBuyerId())
                          .content(contractChatUrl)
                          .type("TEXT")
                          .build();
          handleChatMessage(linkMessage);

          return contractChatRoomId;
      }

      /** {@inheritDoc} */
      @Override
      public ChatRoom getChatRoomById(Long chatRoomId) {

          ChatRoom chatRoom = chatRoomMapper.findById(chatRoomId);
          if (chatRoom == null) {
              log.error("채팅방을 찾을 수 없음: {}", chatRoomId);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }

          return chatRoom;
      }
}
