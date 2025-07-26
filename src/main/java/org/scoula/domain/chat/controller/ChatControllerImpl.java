package org.scoula.domain.chat.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.scoula.domain.chat.dto.ChatMessageRequestDto;
import org.scoula.domain.chat.dto.ChatRoomInfoDto;
import org.scoula.domain.chat.dto.ChatRoomWithUserInfoDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.service.ChatServiceImpl;
import org.scoula.domain.chat.service.ChatServiceInterface;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.file.service.S3ServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat")
public class ChatControllerImpl implements ChatController {

      private final ChatServiceInterface chatService;
      private final UserServiceInterface userService;
      private final S3ServiceInterface s3Service;

      private static final List<String> ALLOWED_IMAGE_EXTENSIONS =
              Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
      private static final List<String> ALLOWED_VIDEO_EXTENSIONS =
              Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "webm");
      private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS =
              Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar");

      private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

      public ChatControllerImpl(
              ChatServiceInterface chatService,
              UserServiceInterface userService,
              S3ServiceInterface s3Service) {
          this.chatService = chatService;
          this.userService = userService;
          this.s3Service = s3Service;
      }

      @Override
      @MessageMapping("/chat/send")
      public void sendMessage(@Payload ChatMessageRequestDto message, Principal principal) {
          chatService.handleChatMessage(message);
      }

      @MessageMapping("/chat/enter")
      public void enterChatRoom(@Payload Map<String, Long> payload) {
          Long userId = payload.get("userId");
          Long chatRoomId = payload.get("chatRoomId");
          ((ChatServiceImpl) chatService).setUserCurrentChatRoom(userId, chatRoomId);
      }

      @MessageMapping("/chat/leave")
      public void leaveChatRoom(@Payload Map<String, Long> payload) {
          Long userId = payload.get("userId");
          ((ChatServiceImpl) chatService).removeUserFromCurrentChatRoom(userId);
      }

      @MessageMapping("/user/offline")
      public void setUserOffline(@Payload Map<String, Long> payload) {
          Long userId = payload.get("userId");
          ((ChatServiceImpl) chatService).setUserOffline(userId);
      }

      @MessageMapping("/user/online")
      public void updateOnlineStatus(@Payload Map<String, Object> payload) {
          Long userId = Long.parseLong(payload.get("userId").toString());
          Boolean isOnline = (Boolean) payload.get("isOnline");
          if (isOnline) {
              ((ChatServiceImpl) chatService).addOnlineUser(userId);
          } else {
              ((ChatServiceImpl) chatService).setUserOffline(userId);
          }
      }

      @Override
      @PostMapping("/rooms")
      public ResponseEntity<ApiResponse<Long>> createChatRoom(
              @RequestParam Long propertyId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long buyerId = currentUser.getUserId();

          Long ownerId = chatService.getPropertyOwnerId(propertyId);

          if (buyerId.equals(ownerId)) {
              throw new BusinessException(ChatErrorCode.SELF_CHAT_NOT_ALLOWED);
          }

          if (chatService.existsChatRoom(ownerId, buyerId, propertyId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ALREADY_EXISTS);
          }

          Long roomId = chatService.createChatRoom(ownerId, buyerId, propertyId);
          return ResponseEntity.ok(ApiResponse.success(roomId));
      }

      @Override
      @GetMapping("/rooms/owner")
      public ResponseEntity<ApiResponse<List<ChatRoomWithUserInfoDto>>> getOwnerRooms(
              Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long ownerId = currentUser.getUserId();

          List<ChatRoomWithUserInfoDto> ownerRooms = chatService.getRoomsAsOwner(ownerId);
          return ResponseEntity.ok(ApiResponse.success(ownerRooms));
      }

      @Override
      @GetMapping("/rooms/buyer")
      public ResponseEntity<ApiResponse<List<ChatRoomWithUserInfoDto>>> getBuyerRooms(
              Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long buyerId = currentUser.getUserId();

          List<ChatRoomWithUserInfoDto> buyerRooms = chatService.getRoomsAsBuyer(buyerId);
          return ResponseEntity.ok(ApiResponse.success(buyerRooms));
      }

      @Override
      @GetMapping("/rooms/{chatRoomId}/info")
      public ResponseEntity<ApiResponse<ChatRoomInfoDto>> getChatRoomInfo(
              @PathVariable Long chatRoomId, Authentication authentication) {

          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();
          ChatRoomInfoDto chatRoomInfo = chatService.getChatRoomInfo(chatRoomId, userId);
          return ResponseEntity.ok(ApiResponse.success(chatRoomInfo));
      }

      @Override
      @GetMapping("/messages/{chatRoomId}")
      public ResponseEntity<ApiResponse<?>> getMessages(
              @PathVariable Long chatRoomId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          if (!chatService.isUserInChatRoom(chatRoomId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          return ResponseEntity.ok(ApiResponse.success(chatService.getMessages(chatRoomId, userId)));
      }

      @Override
      @PostMapping("/upload")
      public ResponseEntity<ApiResponse<?>> uploadChatFile(
              @RequestParam("file") MultipartFile file,
              @RequestParam("chatRoomId") Long chatRoomId,
              @RequestParam("receiverId") Long receiverId,
              Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          if (!chatService.isUserInChatRoom(chatRoomId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          validateFile(file);

          String fileName = generateChatFileName(chatRoomId, userId, file.getOriginalFilename());
          String s3Key = s3Service.uploadFile(file, fileName);
          String fileUrl = s3Service.getFileUrl(s3Key);
          String fileType = determineFileType(file.getOriginalFilename());

          ChatMessageRequestDto fileMessage =
                  ChatMessageRequestDto.builder()
                          .chatRoomId(chatRoomId)
                          .senderId(userId)
                          .receiverId(receiverId)
                          .content(file.getOriginalFilename())
                          .type("FILE")
                          .fileUrl(fileUrl)
                          .build();

          chatService.handleChatMessage(fileMessage);

          System.err.println("파일 업로드 및 메시지 전송 완료: " + file.getOriginalFilename());

          Map<String, Object> response = new HashMap<>();
          response.put("fileUrl", fileUrl);
          response.put("fileName", file.getOriginalFilename());
          response.put("fileSize", file.getSize());
          response.put("fileType", fileType);
          response.put("s3Key", s3Key);
          response.put("messageSent", true);
          response.put("messageType", "FILE");

          return ResponseEntity.ok(ApiResponse.success(response));
      }

      @Override
      @GetMapping("/media/{chatRoomId}")
      public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChatMediaFiles(
              @PathVariable Long chatRoomId,
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "20") int size,
              @RequestParam(defaultValue = "ALL") String mediaType,
              Authentication authentication) {

          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          if (!chatService.isUserInChatRoom(chatRoomId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          List<Map<String, Object>> mediaFiles =
                  chatService.getChatMediaFiles(chatRoomId, page, size, mediaType);
          return ResponseEntity.ok(ApiResponse.success(mediaFiles));
      }

      @PostMapping("/rooms/{chatRoomId}/read")
      public ResponseEntity<ApiResponse<String>> markChatRoomAsRead(
              @PathVariable Long chatRoomId, Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          if (!chatService.isUserInChatRoom(chatRoomId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ((ChatServiceImpl) chatService).markChatRoomAsRead(chatRoomId, userId);
          return ResponseEntity.ok(ApiResponse.success("읽음 처리 완료"));
      }

      @GetMapping("/user")
      public ResponseEntity<ApiResponse<Map<String, Long>>> getCurrentUserId(
              Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          Long userId = currentUserOpt.get().getUserId();
          Map<String, Long> response = new HashMap<>();
          response.put("userId", userId);

          return ResponseEntity.ok(ApiResponse.success(response));
      }

      @GetMapping("/debug/status")
      public ResponseEntity<ApiResponse<Map<String, Object>>> getDebugStatus(
              Authentication authentication) {
          Map<String, Object> status = new HashMap<>();
          status.put("onlineUsers", ((ChatServiceImpl) chatService).getOnlineUsers());
          status.put("currentChatRooms", ((ChatServiceImpl) chatService).getCurrentChatRoomStatus());

          return ResponseEntity.ok(ApiResponse.success(status));
      }

      /** 파일 유효성 검사 */
      private void validateFile(MultipartFile file) {
          if (file.isEmpty()) {
              throw new BusinessException(ChatErrorCode.INVALID_INPUT_VALUE, "파일이 비어있습니다.");
          }

          if (file.getSize() > MAX_FILE_SIZE) {
              throw new BusinessException(ChatErrorCode.FILE_SIZE_EXCEEDED);
          }

          String originalFilename = file.getOriginalFilename();
          if (originalFilename == null || originalFilename.trim().isEmpty()) {
              throw new BusinessException(ChatErrorCode.INVALID_INPUT_VALUE, "파일명이 유효하지 않습니다.");
          }

          String extension = getFileExtension(originalFilename).toLowerCase();
          if (!isAllowedExtension(extension)) {
              throw new BusinessException(ChatErrorCode.UNSUPPORTED_FILE_TYPE, "파일 형식: " + extension);
          }
      }

      /** 채팅용 파일명 생성 */
      private String generateChatFileName(Long chatRoomId, Long userId, String originalFilename) {
          String extension = getFileExtension(originalFilename);
          String timestamp = String.valueOf(System.currentTimeMillis());
          return String.format("chat/%d/%d_%s.%s", chatRoomId, userId, timestamp, extension);
      }

      /** 파일 확장자 추출 */
      private String getFileExtension(String filename) {
          int lastDotIndex = filename.lastIndexOf('.');
          if (lastDotIndex == -1) {
              return "";
          }
          return filename.substring(lastDotIndex + 1);
      }

      /** 허용된 확장자인지 확인 */
      private boolean isAllowedExtension(String extension) {
          return ALLOWED_IMAGE_EXTENSIONS.contains(extension)
                  || ALLOWED_VIDEO_EXTENSIONS.contains(extension)
                  || ALLOWED_DOCUMENT_EXTENSIONS.contains(extension);
      }

      /** 파일 타입 결정 */
      private String determineFileType(String filename) {
          String extension = getFileExtension(filename).toLowerCase();

          if (ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
              return "IMAGE";
          } else if (ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
              return "VIDEO";
          } else if (ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
              return "DOCUMENT";
          }

          return "FILE";
      }
}
