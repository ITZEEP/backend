package org.scoula.domain.chat.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.scoula.domain.chat.dto.ContractChatDocument;
import org.scoula.domain.chat.dto.ContractChatMessageRequestDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.service.ContractChatServiceInterface;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.domain.user.service.UserServiceInterface;
import org.scoula.domain.user.vo.User;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat/contract")
@Slf4j
public class ContractChatControllerImpl implements ContractChatController {

      private final ContractChatServiceInterface contractChatService;
      private final UserServiceInterface userService;
      private final ContractChatMapper contractChatMapper;
      private final SimpMessagingTemplate messagingTemplate;

      public ContractChatControllerImpl(
              ContractChatServiceInterface contractChatService,
              UserServiceInterface userService,
              ContractChatMapper contractChatMapper,
              SimpMessagingTemplate messagingTemplate) {
          this.contractChatService = contractChatService;
          this.userService = userService;
          this.contractChatMapper = contractChatMapper;
          this.messagingTemplate = messagingTemplate;
      }

      @Autowired private RedisTemplate<String, String> stringRedisTemplate;

      @Override
      @PostMapping("/rooms")
      public ResponseEntity<ApiResponse<Long>> createContractChat(
              @RequestParam Long chatRoomId, Authentication authentication) {
          try {
              String currentUserEmail = authentication.getName();
              Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

              if (currentUserOpt.isEmpty()) {
                  throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
              }

              Long userId = currentUserOpt.get().getUserId();
              Long contractChatId = contractChatService.createContractChat(chatRoomId, userId);

              return ResponseEntity.ok(ApiResponse.success(contractChatId, "계약 채팅방이 성공적으로 생성되었습니다."));
          } catch (Exception e) {
              log.error("계약 채팅방 생성 실패", e);
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("계약 채팅방 생성에 실패했습니다: " + e.getMessage()));
          }
      }

      @Override
      @MessageMapping("/contract/chat/send")
      public void sendContractMessage(
              @Payload ContractChatMessageRequestDto message, Principal principal) {
          try {
              if (principal != null) {
                  log.info("인증된 사용자: {}", principal.getName());
              } else {
                  log.warn("Principal이 null - 인증 정보 없이 진행");
              }

              boolean canSend =
                      contractChatService.canSendContractMessage(message.getContractChatId());

              if (!canSend) {
                  log.warn("=== 메시지 전송 차단 ===");
                  log.warn(
                          "contractChatId: {}, senderId: {}",
                          message.getContractChatId(),
                          message.getSenderId());

                  Map<String, Object> errorInfo =
                          Map.of(
                                  "error", "OFFLINE_USER",
                                  "message", "상대방이 오프라인 상태입니다. 상대방이 접속한 후 메시지를 보내주세요.");

                  messagingTemplate.convertAndSendToUser(
                          message.getSenderId().toString(), "/queue/contract/error", errorInfo);

                  log.warn("에러 메시지 전송 완료");
                  return;
              }

              contractChatService.handleContractChatMessage(message);

          } catch (Exception e) {
              log.error("계약 채팅 메시지 전송 실패", e);
          }
      }

      @Override
      @GetMapping("/messages/{contractChatId}")
      public ResponseEntity<ApiResponse<List<ContractChatDocument>>> getContractMessages(
              @PathVariable Long contractChatId, Authentication authentication) {

          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          User currentUser = currentUserOpt.get();
          Long userId = currentUser.getUserId();

          // 권한 확인을 컨트롤러에서 직접 수행
          if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          List<ContractChatDocument> messages =
                  contractChatService.getContractMessages(contractChatId, userId);

          return ResponseEntity.ok(ApiResponse.success(messages, "계약 채팅 메시지 목록을 성공적으로 조회했습니다."));
      }

      @Override
      @PostMapping("/{contractChatId}/start-point")
      public ResponseEntity<ApiResponse<String>> setStartPoint(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              String currentUserEmail = authentication.getName();
              Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

              if (currentUserOpt.isEmpty()) {
                  throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
              }

              Long userId = currentUserOpt.get().getUserId();
              String startPoint = contractChatService.setStartPoint(contractChatId, userId);

              return ResponseEntity.ok(ApiResponse.success(startPoint, "특약 대화 시작점이 설정되었습니다."));
          } catch (Exception e) {
              log.error("특약 대화 시작점 설정 실패", e);
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("시작점 설정에 실패했습니다: " + e.getMessage()));
          }
      }

      @PostMapping("/{contractChatId}/request-end")
      public ResponseEntity<ApiResponse<String>> requestEndPointExport(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              String currentUserEmail = authentication.getName();
              Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

              if (currentUserOpt.isEmpty()) {
                  throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
              }

              Long userId = currentUserOpt.get().getUserId();

              contractChatService.requestEndPointExport(contractChatId, userId);

              return ResponseEntity.ok(ApiResponse.success("특약 종료 요청이 임차인에게 전송되었습니다."));
          } catch (Exception e) {
              log.error("특약 종료 요청 실패", e);
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("특약 종료 요청에 실패했습니다: " + e.getMessage()));
          }
      }

      @PostMapping("/{contractChatId}/end-point-reject")
      public ResponseEntity<ApiResponse<String>> rejectEndPointExport(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              String currentUserEmail = authentication.getName();
              Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

              if (currentUserOpt.isEmpty()) {
                  throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
              }

              Long userId = currentUserOpt.get().getUserId();

              ContractChat contractChat =
                      contractChatService.getContractChatInfo(contractChatId, userId);
              Long buyerId = contractChat.getBuyerId();
              Long ownerId = contractChat.getOwnerId();

              if (!userId.equals(buyerId)) {
                  throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
              }

              String redisKey = "contract:request-end:" + contractChatId;
              String storedOwnerId = stringRedisTemplate.opsForValue().get(redisKey);

              if (storedOwnerId == null || !storedOwnerId.equals(ownerId.toString())) {
                  throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
              }

              contractChatService.rejectEndPointExport(contractChatId, userId);

              return ResponseEntity.ok(ApiResponse.success("특약 종료 요청이 거절되었습니다."));
          } catch (Exception e) {
              log.error("특약 종료 요청 거절 실패", e);
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("특약 종료 요청 거절에 실패했습니다: " + e.getMessage()));
          }
      }

      @Override
      @PostMapping("/{contractChatId}/end-point-export")
      public ResponseEntity<ApiResponse<String>> setEndPointAndExport(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              String currentUserEmail = authentication.getName();
              Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

              if (currentUserOpt.isEmpty()) {
                  throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
              }

              Long userId = currentUserOpt.get().getUserId();
              ContractChat contractChat =
                      contractChatService.getContractChatInfo(contractChatId, userId);
              Long buyerId = contractChat.getBuyerId();
              Long ownerId = contractChat.getOwnerId();

              if (!userId.equals(buyerId)) {
                  throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
              }

              String redisKey = "contract:request-end:" + contractChatId;
              String storedOwnerId = stringRedisTemplate.opsForValue().get(redisKey);

              if (storedOwnerId == null || !storedOwnerId.equals(ownerId.toString())) {
                  throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
              }

              String formattedMessages =
                      contractChatService.setEndPointAndExport(contractChatId, userId);

              log.info("메시지 내용:");
              System.out.println(formattedMessages);

              ApiResponse<String> response =
                      ApiResponse.success(formattedMessages, "특약 대화가 성공적으로 내보내졌습니다.");

              return ResponseEntity.ok(response);
          } catch (Exception e) {
              log.error("특약 대화 내보내기 실패", e);
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("특약 대화 내보내기에 실패했습니다: " + e.getMessage()));
          }
      }

      @Override
      @MessageMapping("/contract/chat/enter")
      public void enterContractChatRoom(@Payload Map<String, Long> payload, Principal principal) {
          try {
              Long userId = payload.get("userId");
              Long contractChatId = payload.get("contractChatId");
              contractChatService.enterContractChatRoom(contractChatId, userId);

              notifyContractChatOnlineStatus(contractChatId, userId, true);
          } catch (Exception e) {
              log.error("계약 채팅방 입장 실패", e);
          }
      }

      @Override
      @MessageMapping("/contract/chat/leave")
      public void leaveContractChatRoom(@Payload Map<String, Long> payload, Principal principal) {
          try {
              Long userId = payload.get("userId");
              Long contractChatId = payload.get("contractChatId");

              contractChatService.leaveContractChatRoom(contractChatId, userId);

              notifyContractChatOnlineStatus(contractChatId, userId, false);
          } catch (Exception e) {
              log.error("계약 채팅방 퇴장 실패", e);
          }
      }

      @Override
      @MessageMapping("/contract/user/offline")
      public void setContractUserOffline(@Payload Map<String, Long> payload, Principal principal) {
          try {
              Long userId = payload.get("userId");
              Long contractChatId = payload.get("contractChatId");

              contractChatService.setContractUserOffline(userId, contractChatId);

              if (contractChatId != null) {
                  notifyContractChatOnlineStatus(contractChatId, userId, false);
              }
          } catch (Exception e) {
              log.error("계약 채팅 오프라인 처리 실패", e);
          }
      }

      @Override
      @GetMapping("/{contractChatId}/online-status")
      public ResponseEntity<ApiResponse<Map<String, Object>>> getContractChatOnlineStatus(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              String currentUserEmail = authentication.getName();
              Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

              if (currentUserOpt.isEmpty()) {
                  throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
              }

              Long userId = currentUserOpt.get().getUserId();

              Map<String, Object> statusMap =
                      contractChatService.getContractChatOnlineStatus(contractChatId, userId);

              return ResponseEntity.ok(ApiResponse.success(statusMap, "온라인 상태 조회 성공"));

          } catch (Exception e) {
              log.error("온라인 상태 조회 실패", e);
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("온라인 상태 조회에 실패했습니다: " + e.getMessage()));
          }
      }

      private void notifyContractChatOnlineStatus(
              Long contractChatId, Long userId, boolean isOnline) {
          try {
              ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
              if (contractChat == null) return;

              Long otherUserId =
                      userId.equals(contractChat.getOwnerId())
                              ? contractChat.getBuyerId()
                              : contractChat.getOwnerId();

              Map<String, Object> statusInfo =
                      Map.of(
                              "userId", userId,
                              "isOnline", isOnline,
                              "contractChatId", contractChatId,
                              "timestamp", LocalDateTime.now().toString());

              messagingTemplate.convertAndSendToUser(
                      otherUserId.toString(), "/queue/contract/online-status", statusInfo);

          } catch (Exception e) {
              log.error("온라인 상태 알림 실패", e);
          }
      }

      @Override
      @GetMapping("/rooms/{contractChatId}/info")
      public ResponseEntity<ApiResponse<Map<String, Object>>> getContractChatInfo(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              String currentUserEmail = authentication.getName();
              Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

              if (currentUserOpt.isEmpty()) {
                  throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
              }

              Long userId = currentUserOpt.get().getUserId();

              if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
                  throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
              }

              ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
              if (contractChat == null) {
                  throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
              }

              Map<String, Object> contractInfo =
                      Map.of(
                              "contractChatId", contractChat.getContractChatId(),
                              "ownerId", contractChat.getOwnerId(),
                              "buyerId", contractChat.getBuyerId(),
                              "homeId", contractChat.getHomeId(),
                              "contractStartAt", contractChat.getContractStartAt(),
                              "lastMessage", contractChat.getLastMessage());

              return ResponseEntity.ok(ApiResponse.success(contractInfo, "계약 채팅방 정보 조회 성공"));

          } catch (Exception e) {
              log.error("계약 채팅방 정보 조회 실패", e);
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("계약 채팅방 정보 조회에 실패했습니다: " + e.getMessage()));
          }
      }
}
