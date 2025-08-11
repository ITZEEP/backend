package org.scoula.domain.chat.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.scoula.domain.chat.document.ContractChatDocument;
import org.scoula.domain.chat.document.FinalSpecialContractDocument;
import org.scoula.domain.chat.document.SpecialContractFixDocument;
import org.scoula.domain.chat.dto.ContractChatMessageRequestDto;
import org.scoula.domain.chat.dto.SpecialContractUserViewDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveResponseDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.repository.SpecialContractMongoRepository;
import org.scoula.domain.chat.service.ContractChatServiceInterface;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.domain.precontract.service.PreContractDataService;
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
      private final PreContractDataService preContractDataService;
      private final SpecialContractMongoRepository specialContractMongoRepository;

      public ContractChatControllerImpl(
              ContractChatServiceInterface contractChatService,
              UserServiceInterface userService,
              ContractChatMapper contractChatMapper,
              SimpMessagingTemplate messagingTemplate,
              PreContractDataService preContractDataService,
              SpecialContractMongoRepository specialContractMongoRepository) {
          this.contractChatService = contractChatService;
          this.userService = userService;
          this.contractChatMapper = contractChatMapper;
          this.messagingTemplate = messagingTemplate;
          this.preContractDataService = preContractDataService;
          this.specialContractMongoRepository = specialContractMongoRepository;
      }

      @Autowired private RedisTemplate<String, String> stringRedisTemplate;

      /** Authentication에서 사용자 ID 추출하는 헬퍼 메서드 기존 프로젝트의 방식을 따라 수정 */
      private Long getUserIdFromAuthentication(Authentication authentication) {
          String currentUserEmail = authentication.getName();
          Optional<User> currentUserOpt = userService.findByEmail(currentUserEmail);

          if (currentUserOpt.isEmpty()) {
              throw new BusinessException(ChatErrorCode.USER_NOT_FOUND);
          }

          return currentUserOpt.get().getUserId();
      }

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
                  contractChatService.getContractMessages(contractChatId);

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

      @Override
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

      @Override
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
      public ResponseEntity<ApiResponse<ClauseImproveResponseDto>> setEndPointAndExport(
              @PathVariable Long contractChatId,
              @RequestParam Long order,
              Authentication authentication) {
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

              boolean importClause =
                      contractChatService.setEndPointAndExport(contractChatId, userId, order);
              //
              //              log.info("메시지 내용:");
              //              System.out.println(importClause);
              //
              //              ApiResponse<String> response =
              //                      ApiResponse.success(importClause, "특약 대화가 성공적으로 내보내졌습니다.");

              return ResponseEntity.ok(ApiResponse.success());
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
      public void leaveContractChatRoom(@Payload Map<String, Long> payload) {
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
      public void setContractUserOffline(@Payload Map<String, Long> payload) {
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
              String role = userId == contractChat.getOwnerId() ? "임대인입니다" : "임차인입니다";

              Map<String, Object> contractInfo =
                      Map.of(
                              "contractChatId",
                              contractChat.getContractChatId(),
                              "ownerId",
                              contractChat.getOwnerId(),
                              "buyerId",
                              contractChat.getBuyerId(),
                              "homeId",
                              contractChat.getHomeId(),
                              "contractStartAt",
                              contractChat.getContractStartAt(),
                              "lastMessage",
                              contractChat.getLastMessage(),
                              "status",
                              contractChat.getStatus(),
                              "role",
                              role);

              return ResponseEntity.ok(ApiResponse.success(contractInfo, "계약 채팅방 정보 조회 성공"));

          } catch (Exception e) {
              log.error("계약 채팅방 정보 조회 실패", e);
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("계약 채팅방 정보 조회에 실패했습니다: " + e.getMessage()));
          }
      }

      // 특약 관련 메서드
      // 수정된 ContractChatControllerImpl.java 구현 메서드들
      // ApiResponse 사용법을 기존 프로젝트에 맞게 수정

      @Override
      @PostMapping("/special-contracts/{contractChatId}/submit-selection")
      public ResponseEntity<ApiResponse<Object>> submitSpecialContractSelection(
              @PathVariable Long contractChatId,
              @RequestBody Map<Integer, Boolean> selections,
              Authentication authentication) {
          Long userId = getUserIdFromAuthentication(authentication);

          if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("ACCESS_DENIED", "해당 계약 채팅방에 접근 권한이 없습니다."));
          }

          Object result = contractChatService.submitUserSelection(contractChatId, userId, selections);

          return ResponseEntity.ok(ApiResponse.success(result));
      }

      @Override
      @PostMapping("/special-contract")
      public ResponseEntity<ApiResponse<SpecialContractFixDocument>> createSpecialContract(
              @RequestParam Long contractChatId,
              @RequestParam Long order,
              Authentication authentication) {
          try {
              Long userId = getUserIdFromAuthentication(authentication);

              if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
                  return ResponseEntity.badRequest()
                          .body(ApiResponse.error("ACCESS_DENIED", "해당 계약 채팅방에 접근 권한이 없습니다."));
              }

              SpecialContractFixDocument result =
                      contractChatService.createSpecialContract(contractChatId, order);

              return ResponseEntity.ok(ApiResponse.success(result, "특약 문서가 성공적으로 생성되었습니다."));
          } catch (IllegalArgumentException e) {
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("CREATION_FAILED", e.getMessage()));
          } catch (Exception e) {
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "특약 문서 생성 중 오류가 발생했습니다."));
          }
      }

      @GetMapping("/special-contract/{contractChatId}/all-rounds")
      public ResponseEntity<ApiResponse<Map<String, Object>>> getAllRoundsSpecialContract(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              Long userId = getUserIdFromAuthentication(authentication);

              if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
                  return ResponseEntity.badRequest()
                          .body(ApiResponse.error("ACCESS_DENIED", "해당 계약 채팅방에 접근 권한이 없습니다."));
              }

              Map<String, Object> result =
                      contractChatService.getAllRoundsSpecialContract(contractChatId, userId);

              return ResponseEntity.ok(ApiResponse.success(result, "전체 라운드 특약 문서 조회 성공"));
          } catch (IllegalArgumentException e) {
              log.error("전체 라운드 특약 문서 조회 실패 - IllegalArgumentException: {}", e.getMessage());
              return ResponseEntity.badRequest().body(ApiResponse.error("NOT_FOUND", e.getMessage()));
          } catch (Exception e) {
              log.error("전체 라운드 특약 문서 조회 실패 - Exception", e);
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "전체 라운드 특약 문서 조회 중 오류가 발생했습니다."));
          }
      }

      @Override
      @GetMapping("/special-contract/{contractChatId}")
      public ResponseEntity<ApiResponse<SpecialContractUserViewDto>> getSpecialContractForUser(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              Long userId = getUserIdFromAuthentication(authentication);

              if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
                  return ResponseEntity.badRequest()
                          .body(ApiResponse.error("ACCESS_DENIED", "해당 계약 채팅방에 접근 권한이 없습니다."));
              }

              SpecialContractUserViewDto result =
                      contractChatService.getSpecialContractForUserByStatus(contractChatId, userId);

              return ResponseEntity.ok(ApiResponse.success(result, "사용자별 특약 문서 조회 성공"));
          } catch (IllegalArgumentException e) {
              log.error("특약 문서 조회 실패 - IllegalArgumentException: {}", e.getMessage());
              return ResponseEntity.badRequest().body(ApiResponse.error("NOT_FOUND", e.getMessage()));
          } catch (Exception e) {
              log.error("특약 문서 조회 실패 - Exception", e);
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "특약 문서 조회 중 오류가 발생했습니다."));
          }
      }

      @PutMapping("/special-contract/{contractChatId}/recent")
      public ResponseEntity<ApiResponse<SpecialContractFixDocument>> updateRecentData(
              @PathVariable Long contractChatId,
              @RequestParam Long order,
              @RequestParam(required = false) String messages,
              Authentication authentication) {
          try {
              Long userId = getUserIdFromAuthentication(authentication);

              if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
                  return ResponseEntity.badRequest()
                          .body(ApiResponse.error("ACCESS_DENIED", "해당 계약 채팅방에 접근 권한이 없습니다."));
              }

              SpecialContractFixDocument updatedDocument =
                      contractChatService.updateRecentData(contractChatId, order, messages);

              contractChatService.setStartPoint(contractChatId, userId);

              return ResponseEntity.ok(
                      ApiResponse.success(updatedDocument, "특약 내용이 성공적으로 업데이트되었습니다."));
          } catch (IllegalArgumentException e) {
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("UPDATE_FAILED", e.getMessage()));
          } catch (Exception e) {
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "특약 내용 업데이트 중 오류가 발생했습니다."));
          }
      }

      @Override
      @PostMapping("/special-contract/{contractChatId}/next-round-auto")
      public ResponseEntity<ApiResponse<List<SpecialContractFixDocument>>> proceedToNextRoundAuto(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              Long userId = getUserIdFromAuthentication(authentication);

              if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
                  return ResponseEntity.badRequest()
                          .body(ApiResponse.error("ACCESS_DENIED", "해당 계약 채팅방에 접근 권한이 없습니다."));
              }

              List<SpecialContractFixDocument> results =
                      contractChatService.proceedAllIncompleteToNextRound(contractChatId);

              return ResponseEntity.ok(
                      ApiResponse.success(results, results.size() + "개의 특약이 다음 라운드로 진행되었습니다."));
          } catch (IllegalArgumentException | IllegalStateException e) {
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("AUTO_ROUND_PROCEED_FAILED", e.getMessage()));
          } catch (Exception e) {
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "자동 라운드 진행 중 오류가 발생했습니다."));
          }
      }

      @Override
      @PatchMapping("/special-contract/{contractChatId}/complete")
      public ResponseEntity<ApiResponse<SpecialContractFixDocument>> completeSpecialContract(
              @PathVariable Long contractChatId,
              @RequestParam Long order,
              Authentication authentication) {
          try {
              Long userId = getUserIdFromAuthentication(authentication);

              if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
                  return ResponseEntity.badRequest()
                          .body(ApiResponse.error("ACCESS_DENIED", "해당 계약 채팅방에 접근 권한이 없습니다."));
              }

              SpecialContractFixDocument result =
                      contractChatService.markSpecialContractAsPassed(contractChatId, order);

              return ResponseEntity.ok(ApiResponse.success(result, "특약이 완료되었습니다."));
          } catch (IllegalArgumentException e) {
              return ResponseEntity.badRequest()
                      .body(ApiResponse.error("COMPLETION_FAILED", e.getMessage()));
          } catch (Exception e) {
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "특약 완료 처리 중 오류가 발생했습니다."));
          }
      }

      @Override
      @GetMapping("/special-contract/completed")
      public ResponseEntity<ApiResponse<List<SpecialContractFixDocument>>>
              getCompletedSpecialContracts(Authentication authentication) {
          try {
              List<SpecialContractFixDocument> result =
                      contractChatService.getCompletedSpecialContracts();

              return ResponseEntity.ok(ApiResponse.success(result, "완료된 특약 문서 목록 조회 성공"));
          } catch (Exception e) {
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "완료된 특약 문서 목록 조회 중 오류가 발생했습니다."));
          }
      }

      // 컨트롤러에 추가
      @Override
      @GetMapping("/special-contract/{contractChatId}/incomplete")
      public ResponseEntity<ApiResponse<List<SpecialContractFixDocument>>>
              getIncompleteSpecialContracts(
                      @PathVariable Long contractChatId, Authentication authentication) {

          try {
              Long userId = getUserIdFromAuthentication(authentication);
              List<SpecialContractFixDocument> result =
                      contractChatService.getIncompleteSpecialContractsByChat(contractChatId, userId);

              return ResponseEntity.ok(ApiResponse.success(result, "미완료 특약 목록 조회 성공"));
          } catch (Exception e) {
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "미완료 특약 목록 조회 중 오류가 발생했습니다."));
          }
      }

      @Override
      @PostMapping("/special-contract/{contractChatId}/ai")
      public ResponseEntity<ApiResponse<String>> sendAiMessage(
              @PathVariable Long contractChatId, @RequestParam Long order) {
          String aiContent = order + "번 특약에 대한 대화를 시작합니다!";
          contractChatService.AiMessage(contractChatId, aiContent);
          return ResponseEntity.ok(ApiResponse.success(aiContent));
      }

      @Override
      @GetMapping("/final-contract/{contractChatId}")
      public ResponseEntity<ApiResponse<FinalSpecialContractDocument>> getFinalSpecialContract(
              @PathVariable Long contractChatId, Authentication authentication) {
          try {
              Long userId = getUserIdFromAuthentication(authentication);

              if (!contractChatService.isUserInContractChat(contractChatId, userId)) {
                  return ResponseEntity.badRequest()
                          .body(ApiResponse.error("ACCESS_DENIED", "해당 계약 채팅방에 접근 권한이 없습니다."));
              }

              Optional<FinalSpecialContractDocument> finalContract =
                      specialContractMongoRepository.findFinalContractByContractChatId(
                              contractChatId);

              if (finalContract.isEmpty()) {
                  return ResponseEntity.badRequest()
                          .body(ApiResponse.error("NOT_FOUND", "최종 특약서가 아직 생성되지 않았습니다."));
              }

              return ResponseEntity.ok(ApiResponse.success(finalContract.get(), "최종 특약서 조회 성공"));

          } catch (Exception e) {
              log.error("최종 특약서 조회 실패", e);
              return ResponseEntity.internalServerError()
                      .body(ApiResponse.error("INTERNAL_ERROR", "최종 특약서 조회 중 오류가 발생했습니다."));
          }
      }
}
