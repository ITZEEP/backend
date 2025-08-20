package org.scoula.domain.chat.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.scoula.domain.chat.document.*;
import org.scoula.domain.chat.dto.*;
import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveResponseDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.mapper.ChatRoomMapper;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.repository.ContractChatMessageRepository;
import org.scoula.domain.chat.repository.SpecialContractMongoRepository;
import org.scoula.domain.chat.vo.ChatRoom;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.domain.contract.dto.LegalityDTO;
import org.scoula.domain.contract.repository.ContractMongoRepository;
import org.scoula.domain.contract.service.ContractFixServiceInterface;
import org.scoula.domain.precontract.service.PreContractDataService;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ContractChatServiceImpl implements ContractChatServiceInterface {

      private final ContractChatMapper contractChatMapper;
      private final ChatRoomMapper chatRoomMapper;
      private final ContractChatMessageRepository contractChatMessageRepository;
      private final SimpMessagingTemplate messagingTemplate;
      private final ChatServiceInterface chatService;
      private final ContractMongoRepository contractMongoRepository;
      private final AiClauseImproveService aiClauseImproveService;
      private final PreContractDataService preContractDataService;
      private final ContractFixServiceInterface contractFixService;
      private final Map<String, Set<Long>> contractChatOnlineUsers = new ConcurrentHashMap<>();
      private final RedisTemplate<String, String> stringRedisTemplate;
      private final ObjectMapper objectMapper = new ObjectMapper();
      @Autowired private SpecialContractMongoRepository specialContractMongoRepository;

      @Value("${front.base.url}")
      private String baseUrl;

      private String contractChatUrl = "/contract/";

      /** {@inheritDoc} */
      @Override
      @Transactional
      public Long createContractChat(Long chatRoomId, Long userId) {
          ChatRoom chatRoom = chatRoomMapper.findById(chatRoomId);
          if (chatRoom == null) {
              throw new EntityNotFoundException("채팅방을 찾을 수 없습니다: " + chatRoomId);
          }

          if (!chatService.isUserInChatRoom(chatRoomId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ContractChat existingContract =
                  contractChatMapper.findByUserAndHome(
                          chatRoom.getOwnerId(), chatRoom.getBuyerId(), chatRoom.getHomeId());

          if (existingContract != null) {
              return existingContract.getContractChatId();
          }

          ContractChat contractChat = new ContractChat();
          contractChat.setHomeId(chatRoom.getHomeId());
          contractChat.setOwnerId(chatRoom.getOwnerId());
          contractChat.setBuyerId(chatRoom.getBuyerId());
          contractChat.setContractStartAt(LocalDateTime.now());
          contractChat.setLastMessage("계약이 시작되었습니다.");

          contractChatMapper.createContractChat(contractChat);

          return contractChat.getContractChatId();
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void handleContractChatMessage(ContractChatMessageRequestDto dto) {
          if (dto.getContractChatId() == null || dto.getContent() == null) {
              throw new IllegalArgumentException("필수 파라미터가 누락되었습니다.");
          }

          if (!isUserInContractChat(dto.getContractChatId(), dto.getSenderId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }
          enterContractChatRoom(dto.getContractChatId(), dto.getSenderId());

          boolean canSend = canSendContractMessage(dto.getContractChatId());
          if (!canSend) {
              log.warn(
                      "메시지 전송 차단 - contractChatId: {}, senderId: {}",
                      dto.getContractChatId(),
                      dto.getSenderId());

              // 에러 메시지를 발송자에게만 전송 (저장하지 않음)
              Map<String, Object> errorInfo =
                      Map.of(
                              "error", "OFFLINE_USER",
                              "message", "상대방이 오프라인 상태입니다. 상대방이 접속한 후 메시지를 보내주세요.");

              messagingTemplate.convertAndSendToUser(
                      dto.getSenderId().toString(), "/queue/contract/error", errorInfo);

              return;
          }

          ContractChatDocument messageDocument =
                  ContractChatDocument.builder()
                          .contractChatId(dto.getContractChatId().toString())
                          .senderId(dto.getSenderId())
                          .receiverId(dto.getReceiverId())
                          .content(dto.getContent())
                          .sendTime(LocalDateTime.now().toString())
                          .build();

          try {
              ContractChatDocument savedMessage =
                      contractChatMessageRepository.saveMessage(messageDocument);

              contractChatMapper.updateLastMessage(dto.getContractChatId(), dto.getContent());

              messagingTemplate.convertAndSend(
                      "/topic/contract-chat/" + dto.getContractChatId(), savedMessage);

          } catch (Exception e) {
              log.error("메시지 처리 중 오류 발생", e);
              throw e;
          }
      }

      public void AiMessage(Long contractChatId, String content) {
          final Long ai = 9999L;

          ContractChatDocument aiMessage =
                  ContractChatDocument.builder()
                          .contractChatId(contractChatId.toString())
                          .senderId(ai)
                          .receiverId(null)
                          .content(content)
                          .sendTime(LocalDateTime.now().toString())
                          .build();

          contractChatMessageRepository.saveMessage(aiMessage);
          contractChatMapper.updateLastMessage(contractChatId, content);
          messagingTemplate.convertAndSend("/topic/contract-chat/" + contractChatId, aiMessage);
      }

      public void AiMessageNext(Long contractChatId, String content) {
          final Long ai = 9997L;

          ContractChatDocument aiMessage =
                  ContractChatDocument.builder()
                          .contractChatId(contractChatId.toString())
                          .senderId(ai)
                          .receiverId(null)
                          .content(content)
                          .sendTime(LocalDateTime.now().toString())
                          .build();

          contractChatMessageRepository.saveMessage(aiMessage);
          contractChatMapper.updateLastMessage(contractChatId, content);
          messagingTemplate.convertAndSend("/topic/contract-chat/" + contractChatId, aiMessage);
      }

      public void AiMessageBtn(Long contractChatId, String content) {
          final Long ai = 9998L;

          ContractChatDocument aiMessage =
                  ContractChatDocument.builder()
                          .contractChatId(contractChatId.toString())
                          .senderId(ai)
                          .receiverId(null)
                          .content(content)
                          .sendTime(LocalDateTime.now().toString())
                          .build();

          contractChatMessageRepository.saveMessage(aiMessage);
          contractChatMapper.updateLastMessage(contractChatId, content);
          messagingTemplate.convertAndSend("/topic/contract-chat/" + contractChatId, aiMessage);
      }

      public void AiMessageLegal(Long contractChatId, String content) {
          final Long ai = 9996L;

          ContractChatDocument aiMessage =
                  ContractChatDocument.builder()
                          .contractChatId(contractChatId.toString())
                          .senderId(ai)
                          .receiverId(null)
                          .content(content)
                          .sendTime(LocalDateTime.now().toString())
                          .build();

          contractChatMessageRepository.saveMessage(aiMessage);
          contractChatMapper.updateLastMessage(contractChatId, content);
          messagingTemplate.convertAndSend("/topic/contract-chat/" + contractChatId, aiMessage);
      }

      /** {@inheritDoc} */
      @Override
      public List<ContractChatDocument> getContractMessages(Long contractChatId) {
          return contractChatMessageRepository.getMessages(contractChatId);
      }

      /** 스타트 버튼 클릭 - 현재 시간을 시작점으로 설정 */
      @Override
      @Transactional
      public String setStartPoint(Long contractChatId, Long userId) {
          if (!isUserInContractChat(contractChatId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }
          contractChatMapper.clearTimePoints(contractChatId);
          String startTime = LocalDateTime.now().toString();

          contractChatMapper.updateStartTime(contractChatId, startTime);

          return startTime;
      }

      /** {@inheritDoc} */
      @Override
      public ContractChat getContractChatInfo(Long contractChatId, Long userId) {
          if (contractChatId == null || userId == null) {
              throw new IllegalArgumentException("contractChatId와 userId는 null일 수 없습니다.");
          }

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!userId.equals(contractChat.getOwnerId())
                  && !userId.equals(contractChat.getBuyerId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          return contractChat;
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public boolean setEndPointAndExport(Long contractChatId, Long userId, Long order) {
          if (!isUserInContractChat(contractChatId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          Long ownerId = contractChat.getOwnerId();
          Long buyerId = contractChat.getBuyerId();

          if (!userId.equals(buyerId)) {
              throw new BusinessException(
                      ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "구매자만 특약 내보내기를 수락할 수 있습니다.");
          }

          String redisKey = "contract:request-end:" + contractChatId;
          String storedOwnerId = stringRedisTemplate.opsForValue().get(redisKey);

          if (storedOwnerId == null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_NOT_FOUND, "특약 종료 요청이 존재하지 않습니다.");
          }

          if (!storedOwnerId.equals(ownerId.toString())) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_INVALID, "특약 종료 요청 정보가 유효하지 않습니다.");
          }

          String startTime = contractChat.getStartPoint();
          if (startTime == null || startTime.trim().isEmpty()) {
              throw new BusinessException(ChatErrorCode.START_POINT_NOT_SET);
          }

          String endTime = LocalDateTime.now().toString();
          contractChatMapper.updateEndTime(contractChatId, endTime);

          List<ContractChatDocument> exportMessages =
                  contractChatMessageRepository.getMessagesBetweenTime(
                          contractChatId, startTime, endTime);

          StringBuilder sb = new StringBuilder();
          if (exportMessages != null && !exportMessages.isEmpty()) {
              for (ContractChatDocument msg : exportMessages) {
                  Long senderId = msg.getSenderId();
                  String content = msg.getContent();

                  if (!content.equals("임대인이 특약 대화 종료 및 내보내기를 요청했습니다.")
                          && !content.equals("임차인이 특약 대화를 더 요청했습니다.")) {

                      String senderRole =
                              senderId.equals(buyerId)
                                      ? "구매자"
                                      : senderId.equals(ownerId) ? "판매자" : "조회실패";
                      String toai = String.format("%s: %s", senderRole, content);
                      sb.append(toai).append("\n");
                  }
              }
              sb.append("특약 대화가 종료되었습니다.");
          } else {
              sb.append("조회된 특약 메시지가 없습니다.");
          }

          stringRedisTemplate.delete(redisKey);

          String result = sb.toString();

          SpecialContractFixDocument improveClauseRequest =
                  updateRecentData(contractChatId, order, result);
          ClauseImproveResponseDto improveClauseResponse = getAiClauseImprove(improveClauseRequest);

          updateSpecialClause(contractChatId, improveClauseResponse);

          checkAndIncrementRoundIfComplete(contractChatId);
          return true;
      }

      private boolean isRejectedClause(Long contractChatId, Long order) {
          try {
              ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
              Long currentRound = contractChat.getCurrentRound();

              SpecialContractDocument currentDocument =
                      specialContractMongoRepository
                              .findSpecialContractDocumentByContractChatIdAndRound(
                                      contractChatId, currentRound)
                              .orElse(null);

              if (currentDocument == null) {
                  log.warn("현재 라운드 문서를 찾을 수 없음: round {}", currentRound);
                  return false;
              }

              Optional<SpecialContractDocument.Clause> clauseOpt =
                      currentDocument.getClauses().stream()
                              .filter(clause -> clause.getOrder().equals(order.intValue()))
                              .findFirst();

              if (clauseOpt.isEmpty()) {
                  log.warn("특약 {}번을 현재 라운드에서 찾을 수 없음", order);
                  return false;
              }

              SpecialContractDocument.Clause clause = clauseOpt.get();

              boolean isEmpty =
                      (clause.getTitle() == null || clause.getTitle().trim().isEmpty())
                              && (clause.getContent() == null
                                      || clause.getContent().trim().isEmpty());

              log.info(
                      "특약 {}번 상태 체크 - title: '{}', content: '{}', 거부된 특약: {}",
                      order,
                      clause.getTitle(),
                      clause.getContent(),
                      isEmpty);

              return isEmpty;

          } catch (Exception e) {
              log.error("특약 {}번 거부 상태 체크 실패: {}", order, e.getMessage());
              return false;
          }
      }

      private void updateSpecialClause(Long contractChatId, ClauseImproveResponseDto response) {

          Long round = response.getData().getRound();
          Integer order = response.getData().getOrder();
          String content = response.getData().getContent();
          String title = response.getData().getTitle();

          SpecialContractDocument.Assessment assessment =
                  SpecialContractDocument.Assessment.builder()
                          .owner(
                                  SpecialContractDocument.Evaluation.builder()
                                          .level(
                                                  response.getData()
                                                          .getAssessment()
                                                          .getOwner()
                                                          .getLevel())
                                          .reason(
                                                  response.getData()
                                                          .getAssessment()
                                                          .getOwner()
                                                          .getReason())
                                          .build())
                          .tenant(
                                  SpecialContractDocument.Evaluation.builder()
                                          .level(
                                                  response.getData()
                                                          .getAssessment()
                                                          .getTenant()
                                                          .getLevel())
                                          .reason(
                                                  response.getData()
                                                          .getAssessment()
                                                          .getTenant()
                                                          .getReason())
                                          .build())
                          .build();

          SpecialContractDocument.Clause clause =
                  SpecialContractDocument.Clause.builder()
                          .order(order)
                          .title(title)
                          .content(content)
                          .assessment(assessment)
                          .build();

          String id =
                  specialContractMongoRepository.updateSpecialContractForNewOrderAndRound(
                          contractChatId, round, order, clause);
      }

      private ClauseImproveResponseDto getAiClauseImprove(SpecialContractFixDocument scfd) {

          Long contractChatId = scfd.getContractChatId();
          // 1. Owner 데이터 조회
          ClauseImproveRequestDto.OwnerData ownerData =
                  preContractDataService.fetchOwnerData(contractChatId);

          // 2. Tenant 데이터 조회
          ClauseImproveRequestDto.TenantData tenantData =
                  preContractDataService.fetchTenantData(contractChatId);

          // 3. OCR 데이터 조회
          ClauseImproveRequestDto.OcrData ocrData =
                  preContractDataService.fetchOcrData(contractChatId);

          // 4. 이전 특약 데이터 설정 (테스트용)
          List<ContentDataDto> prevClauses = scfd.getPrevData();

          // 5. 최근 특약 데이터 설정 (테스트용)
          ContentDataDto recentClause = scfd.getRecentData();

          // 6. AI 특약 개선 요청
          ClauseImproveRequestDto aiRequest =
                  ClauseImproveRequestDto.builder()
                          .contractChatId(contractChatId)
                          .ocrData(ocrData)
                          .round(scfd.getRound())
                          .order(scfd.getOrder())
                          .ownerData(ownerData)
                          .tenantData(tenantData)
                          .prevData(scfd.getPrevData())
                          .recentData(scfd.getRecentData())
                          .build();

          return aiClauseImproveService.improveClause(aiRequest);
      }

      /** {@inheritDoc} */
      @Override
      public boolean isUserInContractChat(Long contractChatId, Long userId) {
          if (contractChatId == null || userId == null) {
              return false;
          }

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              return false;
          }

          return userId.equals(contractChat.getOwnerId()) || userId.equals(contractChat.getBuyerId());
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void enterContractChatRoom(Long contractChatId, Long userId) {
          log.info("=== enterContractChatRoom 시작 ===");
          log.info("contractChatId: {}, userId: {}", contractChatId, userId);

          if (!isUserInContractChat(contractChatId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          // 방 멤버십 Set에 사용자 추가
          stringRedisTemplate.opsForSet().add(roomKey(contractChatId), userId.toString());
          // 사용자 현재 방 Key에 방 ID 저장 (역참조용)
          stringRedisTemplate
                  .opsForValue()
                  .set(userCurrentRoomKey(userId), contractChatId.toString());

          broadcastPresence(contractChatId);
          log.info("=== enterContractChatRoom 완료 ===");
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void leaveContractChatRoom(Long contractChatId, Long userId) {
          // Redis: 방에서 사용자 제거 및 역참조 정리
          stringRedisTemplate.opsForSet().remove(roomKey(contractChatId), userId.toString());
          stringRedisTemplate.delete(userCurrentRoomKey(userId));
          setContractChatUserOffline(userId, contractChatId);
          broadcastPresence(contractChatId);

          log.info("====== 사용자 채팅방 퇴장 ======");
      }

      /** {@inheritDoc} */
      @Override
      public Map<String, Object> getContractChatOnlineStatus(Long contractChatId, Long userId) {
          log.info("=== getContractChatOnlineStatus(REDIS) 시작 ===");
          log.info("contractChatId: {}, userId: {}", contractChatId, userId);

          // 디버깅용 전체 온라인 사용자 출력
          debugContractChatOnlineUsers(contractChatId);

          if (!isUserInContractChat(contractChatId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          boolean ownerInContractRoom =
                  isUserInContractChatRoom(contractChat.getOwnerId(), contractChatId);
          boolean buyerInContractRoom =
                  isUserInContractChatRoom(contractChat.getBuyerId(), contractChatId);

          boolean bothInRoom = ownerInContractRoom && buyerInContractRoom;

          log.info(
                  "Owner({}) 온라인: {}, Buyer({}) 온라인: {}, 둘 다 온라인: {}",
                  contractChat.getOwnerId(),
                  ownerInContractRoom,
                  contractChat.getBuyerId(),
                  buyerInContractRoom,
                  bothInRoom);

          Map<String, Object> result =
                  Map.of(
                          "ownerInContractRoom", ownerInContractRoom,
                          "buyerInContractRoom", buyerInContractRoom,
                          "bothInRoom", bothInRoom,
                          "canChat", bothInRoom,
                          "ownerId", contractChat.getOwnerId(),
                          "buyerId", contractChat.getBuyerId());

          log.info("=== getContractChatOnlineStatus 완료: {} ===", result);
          return result;
      }

      // 디버깅용 메서드 (REDIS 기반)
      public void debugContractChatOnlineUsers(Long contractChatId) {
          log.info("=== 현재 모든 온라인 사용자 상태(REDIS) ===");
          try {
              String rKey = roomKey(contractChatId);
              Set<String> members = stringRedisTemplate.opsForSet().members(rKey);
              log.info("Redis Key: {}, 온라인 사용자(문자열): {}", rKey, members);
              if (members != null) {
                  Set<Long> asLongs = members.stream().map(Long::valueOf).collect(Collectors.toSet());
                  log.info("계약 채팅방 {} 온라인 사용자(Long): {}", contractChatId, asLongs);
              } else {
                  log.info("계약 채팅방 {} 온라인 사용자 없음", contractChatId);
              }
          } catch (Exception e) {
              log.warn("온라인 사용자 상태 로드 중 오류: {}", e.getMessage());
          }
      }

      /** {@inheritDoc} */
      @Override
      public boolean canSendContractMessage(Long contractChatId) {
          try {
              ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
              if (contractChat == null) {
                  return false;
              }

              boolean ownerInContractRoom =
                      isUserInContractChatRoom(contractChat.getOwnerId(), contractChatId);
              boolean buyerInContractRoom =
                      isUserInContractChatRoom(contractChat.getBuyerId(), contractChatId);

              boolean result = ownerInContractRoom && buyerInContractRoom;
              log.info(
                      "최종 전송 가능 여부: {} (owner: {}, buyer: {})",
                      result,
                      ownerInContractRoom,
                      buyerInContractRoom);

              return result;
          } catch (Exception e) {
              log.error("메시지 전송 가능 여부 확인 실패", e);
              return false;
          }
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void setContractUserOffline(Long userId, Long contractChatId) {
          setContractChatUserOffline(userId, contractChatId);
      }

      /** {@inheritDoc} */
      private void setContractChatUserOnline(Long userId, Long contractChatId) {
          String key = getContractChatKey(contractChatId);
          contractChatOnlineUsers
                  .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                  .add(userId);
          log.debug(
                  "사용자 {}가 계약 채팅방 {} 온라인 상태로 설정. 현재 온라인 사용자: {}",
                  userId,
                  contractChatId,
                  contractChatOnlineUsers.get(key));
      }

      /** {@inheritDoc} */
      private void setContractChatUserOffline(Long userId, Long contractChatId) {
          String key = getContractChatKey(contractChatId);
          Set<Long> users = contractChatOnlineUsers.get(key);
          if (users != null) {
              users.remove(userId);
              log.debug(
                      "사용자 {}가 계약 채팅방 {} 오프라인 상태로 설정. 현재 온라인 사용자: {}", userId, contractChatId, users);
              if (users.isEmpty()) {
                  contractChatOnlineUsers.remove(key);
              }
          }
      }

      /** {@inheritDoc} */
      private boolean isUserInContractChatRoom(Long userId, Long contractChatId) {
          String rKey = roomKey(contractChatId);
          Boolean member = stringRedisTemplate.opsForSet().isMember(rKey, userId.toString());
          boolean isOnline = Boolean.TRUE.equals(member);
          log.debug(
                  "사용자 {} 계약 채팅방 {} 온라인 상태 확인(REDIS): {}, key={}",
                  userId,
                  contractChatId,
                  isOnline,
                  rKey);
          return isOnline;
      }

      private String getContractChatKey(Long contractChatId) {
          return "contract-chat-" + contractChatId;
      }

      private String roomKey(Long contractChatId) {
          return "contract:room:" + contractChatId + ":users";
      }

      private String userCurrentRoomKey(Long userId) {
          return "contract:user:" + userId + ":current-room";
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void requestEndPointExport(Long contractChatId, Long ownerId) {
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!ownerId.equals(contractChat.getOwnerId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ContractChatDocument endRequestMessage =
                  ContractChatDocument.builder()
                          .contractChatId(contractChatId.toString())
                          .senderId(ownerId)
                          .receiverId(contractChat.getBuyerId())
                          .content("임대인이 특약 대화 종료 및 내보내기를 요청했습니다.")
                          .sendTime(LocalDateTime.now().toString())
                          .build();

          String key = "contract:request-end:" + contractChatId;
          String existingValue = stringRedisTemplate.opsForValue().get(key);
          if (existingValue != null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_ALREADY_EXISTS, "이미 특약 종료 요청이 진행 중입니다.");
          }
          String value = ownerId.toString();
          stringRedisTemplate.opsForValue().set(key, value);

          contractChatMessageRepository.saveMessage(endRequestMessage);

          messagingTemplate.convertAndSend(
                  "/topic/contract-chat/" + contractChatId, endRequestMessage);
      }

      /** {@inheritDoc} */
      @Override
      public void rejectEndPointExport(Long contractChatId, Long userId) {
          String redisKey = "contract:request-end:" + contractChatId;
          stringRedisTemplate.delete(redisKey);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          ContractChatDocument rejectNotification =
                  ContractChatDocument.builder()
                          .contractChatId(contractChatId.toString())
                          .senderId(userId)
                          .receiverId(contractChat.getBuyerId())
                          .content("임차인이 특약 대화를 더 요청했습니다.")
                          .sendTime(LocalDateTime.now().toString())
                          .build();

          contractChatMessageRepository.saveMessage(rejectNotification);

          messagingTemplate.convertAndSend(
                  "/topic/contract-chat/" + contractChatId, rejectNotification);
      }

      @Override
      @Transactional
      public void createNextRoundSpecialContractDocument(
              Long contractChatId, List<Long> rejectedOrders, List<Long> passedOrders) {
          log.info("=== 새 라운드 SPECIAL_CONTRACT 문서 생성 시작 ===");
          log.info(
                  "contractChatId: {}, rejectedOrders: {}, passedOrders: {}",
                  contractChatId,
                  rejectedOrders,
                  passedOrders);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          Long currentRound = contractChat.getCurrentRound();

          SpecialContractDocument latestDocument =
                  specialContractMongoRepository
                          .findSpecialContractDocumentByContractChatIdAndRound(
                                  contractChatId, currentRound)
                          .orElseThrow(
                                  () -> new IllegalArgumentException("현재 라운드의 특약 문서를 찾을 수 없습니다"));

          Long newRound = currentRound + 1;
          log.info("새 라운드: {} → {}", currentRound, newRound);

          List<Long> allPassedOrders = new ArrayList<>(passedOrders);

          List<SpecialContractFixDocument> completedContracts =
                  specialContractMongoRepository.findByContractChatIdAndIsPassed(
                          contractChatId, true);

          for (SpecialContractFixDocument completed : completedContracts) {
              if (!allPassedOrders.contains(completed.getOrder())) {
                  allPassedOrders.add(completed.getOrder());
                  log.info("이전 라운드에서 이미 완료된 특약 {}번 추가", completed.getOrder());
              }
          }
          log.info("최종 통과된 특약들 (이전 완료 포함): {}", allPassedOrders);

          List<SpecialContractDocument.Clause> newClauses = new ArrayList<>();

          for (int order = 1; order <= 6; order++) {
              Integer orderInteger = Integer.valueOf(order);
              Long orderLong = Long.valueOf(order);

              if (allPassedOrders.contains(orderLong)) {
                  Optional<SpecialContractDocument.Clause> clauseOpt =
                          findBestClauseForOrder(contractChatId, orderLong);

                  if (clauseOpt.isPresent()) {
                      SpecialContractDocument.Clause clause = clauseOpt.get();
                      SpecialContractDocument.Clause copiedClause =
                              SpecialContractDocument.Clause.builder()
                                      .order(clause.getOrder())
                                      .title(clause.getTitle())
                                      .content(clause.getContent())
                                      .assessment(
                                              SpecialContractDocument.Assessment.builder()
                                                      .owner(
                                                              SpecialContractDocument.Evaluation
                                                                      .builder()
                                                                      .level(
                                                                              clause.getAssessment()
                                                                                      .getOwner()
                                                                                      .getLevel())
                                                                      .reason(
                                                                              clause.getAssessment()
                                                                                      .getOwner()
                                                                                      .getReason())
                                                                      .build())
                                                      .tenant(
                                                              SpecialContractDocument.Evaluation
                                                                      .builder()
                                                                      .level(
                                                                              clause.getAssessment()
                                                                                      .getTenant()
                                                                                      .getLevel())
                                                                      .reason(
                                                                              clause.getAssessment()
                                                                                      .getTenant()
                                                                                      .getReason())
                                                                      .build())
                                                      .build())
                                      .build();
                      newClauses.add(copiedClause);
                      log.info("통과된 특약 {}번 복사 완료", order);
                  }
              } else if (rejectedOrders.contains(orderLong)) {
                  SpecialContractDocument.Clause emptyClause =
                          SpecialContractDocument.Clause.builder()
                                  .order(orderInteger)
                                  .title("")
                                  .content("")
                                  .assessment(
                                          SpecialContractDocument.Assessment.builder()
                                                  .owner(
                                                          SpecialContractDocument.Evaluation.builder()
                                                                  .level("")
                                                                  .reason("")
                                                                  .build())
                                                  .tenant(
                                                          SpecialContractDocument.Evaluation.builder()
                                                                  .level("")
                                                                  .reason("")
                                                                  .build())
                                                  .build())
                                  .build();
                  newClauses.add(emptyClause);
                  log.info("거부된 특약 {}번 빈 껍데기 생성 완료", order);
              } else {
                  latestDocument.getClauses().stream()
                          .filter(clause -> clause.getOrder().equals(orderInteger))
                          .findFirst()
                          .ifPresent(
                                  clause -> {
                                      SpecialContractDocument.Clause maintainedClause =
                                              SpecialContractDocument.Clause.builder()
                                                      .order(clause.getOrder())
                                                      .title(clause.getTitle())
                                                      .content(clause.getContent())
                                                      .assessment(
                                                              SpecialContractDocument.Assessment
                                                                      .builder()
                                                                      .owner(
                                                                              SpecialContractDocument
                                                                                      .Evaluation
                                                                                      .builder()
                                                                                      .level(
                                                                                              clause.getAssessment()
                                                                                                      .getOwner()
                                                                                                      .getLevel())
                                                                                      .reason(
                                                                                              clause.getAssessment()
                                                                                                      .getOwner()
                                                                                                      .getReason())
                                                                                      .build())
                                                                      .tenant(
                                                                              SpecialContractDocument
                                                                                      .Evaluation
                                                                                      .builder()
                                                                                      .level(
                                                                                              clause.getAssessment()
                                                                                                      .getTenant()
                                                                                                      .getLevel())
                                                                                      .reason(
                                                                                              clause.getAssessment()
                                                                                                      .getTenant()
                                                                                                      .getReason())
                                                                                      .build())
                                                                      .build())
                                                      .build();
                                      newClauses.add(maintainedClause);
                                  });
              }
          }

          newClauses.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

          SpecialContractDocument newDocument =
                  SpecialContractDocument.builder()
                          .contractChatId(contractChatId)
                          .round(newRound)
                          .totalClauses(newClauses.size())
                          .clauses(newClauses)
                          .build();

          specialContractMongoRepository.saveSpecialContractForNewRound(newDocument);

          log.info(
                  "새 라운드 SPECIAL_CONTRACT 문서 생성 완료 - round: {}, totalClauses: {}",
                  newRound,
                  newClauses.size());
          log.info(
                  "최종 통과된 특약: {}, 거부된 특약: {}, 유지된 특약: {}",
                  allPassedOrders,
                  rejectedOrders,
                  Arrays.asList(1, 2, 3, 4, 5, 6).stream()
                          .filter(
                                  i ->
                                          !allPassedOrders.contains((long) i)
                                                  && !rejectedOrders.contains((long) i))
                          .collect(Collectors.toList()));
      }

      private Optional<SpecialContractDocument.Clause> findBestClauseForOrder(
              Long contractChatId, Long order) {
          for (Long round = 4L; round >= 1L; round--) {
              Optional<SpecialContractDocument> docOpt =
                      specialContractMongoRepository
                              .findSpecialContractDocumentByContractChatIdAndRound(
                                      contractChatId, round);

              if (docOpt.isPresent()) {
                  SpecialContractDocument doc = docOpt.get();
                  Optional<SpecialContractDocument.Clause> clauseOpt =
                          doc.getClauses().stream()
                                  .filter(clause -> clause.getOrder().equals(order.intValue()))
                                  .filter(
                                          clause ->
                                                  clause.getTitle() != null
                                                          && !clause.getTitle().trim().isEmpty()
                                                          && clause.getContent() != null
                                                          && !clause.getContent().trim().isEmpty())
                                  .findFirst();

                  if (clauseOpt.isPresent()) {
                      log.info("특약 {}번의 최적 조항을 라운드 {}에서 발견", order, round);
                      return clauseOpt;
                  }
              }
          }

          log.warn("특약 {}번의 완성된 조항을 찾을 수 없음", order);
          return Optional.empty();
      }

      @Override
      @Transactional
      public List<SpecialContractFixDocument> proceedAllIncompleteToNextRound(Long contractChatId) {
          log.info("=== 모든 미완료 특약 다음 라운드 진행 시작 ===");
          log.info("contractChatId: {}", contractChatId);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          Long currentRound = contractChat.getCurrentRound();
          log.info("현재 라운드: {}", currentRound);

          List<SpecialContractFixDocument> incompleteContracts =
                  specialContractMongoRepository.findByContractChatIdAndIsPassed(
                          contractChatId, false);

          if (incompleteContracts.isEmpty()) {
              log.info("진행할 미완료 특약이 없습니다.");
              return new ArrayList<>();
          }

          log.info("진행할 특약 개수: {}", incompleteContracts.size());

          List<SpecialContractFixDocument> updatedContracts = new ArrayList<>();

          for (SpecialContractFixDocument document : incompleteContracts) {
              try {
                  int targetIndex = (int) (document.getRound() - 1);

                  if (targetIndex >= 2) {
                      log.warn("특약 {}번: 최대 라운드 도달, 스킵", document.getOrder());
                      continue;
                  }

                  ContentDataDto prevDataToStore =
                          ContentDataDto.builder()
                                  .title(document.getRecentData().getTitle())
                                  .content(document.getRecentData().getContent())
                                  .messages(document.getRecentData().getMessages())
                                  .build();

                  List<ContentDataDto> updatedPrevData = new ArrayList<>(document.getPrevData());
                  updatedPrevData.set(targetIndex, prevDataToStore);

                  document.setPrevData(updatedPrevData);
                  document.setRecentData(createEmptyContentData());
                  document.setRound(document.getRound() + 1);
                  log.info(
                          "특약 {}번: round {} → {} 증가",
                          document.getOrder(),
                          currentRound,
                          currentRound + 1);

                  SpecialContractFixDocument updated =
                          specialContractMongoRepository.updateSpecialContract(document);
                  updatedContracts.add(updated);

                  log.info("특약 {}번 라운드 진행 완료: round={}", document.getOrder(), updated.getRound());

              } catch (Exception e) {
                  log.error("특약 {}번 라운드 진행 실패: {}", document.getOrder(), e.getMessage());
              }
          }

          log.info("=== 모든 미완료 특약 다음 라운드 진행 완료 ===");
          log.info("성공적으로 진행된 특약 개수: {}", updatedContracts.size());

          return updatedContracts;
      }

      @Override
      @Transactional
      public Object submitUserSelection(
              Long contractChatId, Long userId, Map<Integer, Boolean> selections) {
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다.");
          }

          boolean isOwner = userId.equals(contractChat.getOwnerId());
          boolean isTenant = userId.equals(contractChat.getBuyerId());

          if (!isOwner && !isTenant) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ContractChat.ContractStatus currentStatus = contractChat.getStatus();

          List<Integer> availableOrders = getAvailableOrders(contractChatId, currentStatus);
          if (!isValidSelection(selections, availableOrders)) {
              throw new IllegalArgumentException("현재 상태에서 선택할 수 없는 특약입니다. 선택 가능: " + availableOrders);
          }
          Optional<SpecialContractSelectionDocument> existingOpt =
                  specialContractMongoRepository.findSelectionByContractChatId(contractChatId);

          SpecialContractSelectionDocument document;
          if (existingOpt.isPresent()) {
              document = existingOpt.get();
          } else {
              document =
                      SpecialContractSelectionDocument.builder()
                              .contractChatId(contractChatId)
                              .ownerSelections(new HashMap<>())
                              .tenantSelections(new HashMap<>())
                              .ownerCompleted(false)
                              .tenantCompleted(false)
                              .processed(false)
                              .build();
          }

          if (isOwner) {
              document.setOwnerSelections(selections);
              document.setOwnerCompleted(true);
          } else {
              document.setTenantSelections(selections);
              document.setTenantCompleted(true);
          }

          specialContractMongoRepository.saveSelectionStatus(document);

          if (!document.isOwnerCompleted() || !document.isTenantCompleted()) {
              String waitingFor = isOwner ? "임차인" : "임대인";
              return Map.of("message", "선택을 기다리는 중입니다: " + waitingFor, "completed", false);
          }

          if (document.isProcessed()) {
              return Map.of("message", "이미 처리된 선택입니다.", "completed", true);
          }

          Object result = processRoundResults(contractChatId, document, currentStatus, isOwner);

          if (result instanceof Map) {
              Map<String, Object> resultMap = (Map<String, Object>) result;
              boolean hasNextRound = resultMap.containsKey("nextRound");
              boolean isCompleted = resultMap.getOrDefault("completed", false).equals(true);

              if (hasNextRound || !isCompleted) {
                  AiMessage(contractChatId, "특약 대화가 시작됩니다!");
              }
          }

          return result;
      }

      private List<Integer> getAvailableOrders(
              Long contractChatId, ContractChat.ContractStatus status) {
          if (status == ContractChat.ContractStatus.STEP0
                  || status == ContractChat.ContractStatus.STEP1
                  || status == ContractChat.ContractStatus.STEP2) {
              return Arrays.asList(1, 2, 3, 4, 5, 6);
          } else {
              return specialContractMongoRepository
                      .findByContractChatIdAndIsPassed(contractChatId, false)
                      .stream()
                      .map(doc -> doc.getOrder().intValue())
                      .collect(Collectors.toList());
          }
      }

      @Transactional
      public Object processRoundResults(
              Long contractChatId,
              SpecialContractSelectionDocument document,
              ContractChat.ContractStatus currentStatus,
              boolean isOwner) {
          List<Long> rejectedOrders =
                  findRejectedOrders(document.getOwnerSelections(), document.getTenantSelections());
          List<Long> passedOrders =
                  findPassedOrders(document.getOwnerSelections(), document.getTenantSelections());

          for (int order = 1; order <= 6; order++) {
              Boolean ownerChoice = document.getOwnerSelections().get(order);
              Boolean tenantChoice = document.getTenantSelections().get(order);

              if (Boolean.TRUE.equals(ownerChoice) && Boolean.TRUE.equals(tenantChoice)) {
                  try {
                      markSpecialContractAsPassed(contractChatId, (long) order);
                  } catch (Exception e) {
                      log.warn("특약 {}번 완료 처리 실패", order);
                  }
              }
          }
          document.setProcessed(true);
          specialContractMongoRepository.saveSelectionStatus(document);

          if (currentStatus == ContractChat.ContractStatus.STEP0
                  || currentStatus == ContractChat.ContractStatus.STEP1
                  || currentStatus == ContractChat.ContractStatus.STEP2) {

              if (rejectedOrders.isEmpty()) {
                  try {
                      FinalSpecialContractDocument finalContract =
                              saveFinalSpecialContract(contractChatId);

                      AiMessage(contractChatId, "모든 특약에 동의하셨습니다! 최종 특약서가 생성되었습니다.");
                      contractChatMapper.updateStatus(
                              contractChatId, ContractChat.ContractStatus.ROUND4);

                      log.info("초안에서 최종 특약 저장 완료 - finalContractId: {}", finalContract.getId());

                      return Map.of(
                              "message",
                              "모든 특약에 동의했습니다.",
                              "completed",
                              true,
                              "finalContractId",
                              finalContract.getId(),
                              "totalFinalClauses",
                              finalContract.getTotalFinalClauses());
                  } catch (Exception e) {
                      log.error("초안에서 최종 특약 저장 실패", e);
                      return Map.of("message", "모든 특약에 동의했지만 최종 저장 중 오류가 발생했습니다.", "completed", true);
                  }
              }

              List<Long> createdOrders = new ArrayList<>();
              for (Long order : rejectedOrders) {
                  try {
                      createSpecialContract(contractChatId, order);
                      createdOrders.add(order);
                  } catch (IllegalArgumentException e) {
                      log.warn("특약 {}번이 이미 존재합니다", order);
                  }
              }

              try {
                  createNextRoundSpecialContractDocument(
                          contractChatId, rejectedOrders, passedOrders);
              } catch (Exception e) {
                  log.error("새 라운드 SPECIAL_CONTRACT 문서 생성 실패", e);
              }

              contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.ROUND0);
              resetSelectionDocument(contractChatId);

              return Map.of(
                      "message", "특약 협상이 시작됩니다.", "completed", true, "createdOrders", createdOrders);
          } else {
              if (rejectedOrders.isEmpty()) {
                  List<SpecialContractFixDocument> remainingIncompleteContracts =
                          specialContractMongoRepository.findByContractChatIdAndIsPassed(
                                  contractChatId, false);
                  if (remainingIncompleteContracts.isEmpty()) {
                      try {
                          FinalSpecialContractDocument finalContract =
                                  saveFinalSpecialContract(contractChatId);

                          AiMessageNext(contractChatId, "🎉 모든 특약 협상이 완료되었습니다! 최종 특약서가 생성되었습니다.");
                          contractChatMapper.updateStatus(
                                  contractChatId, ContractChat.ContractStatus.ROUND4);

                          return Map.of(
                                  "message",
                                  "모든 특약이 완료되었습니다!",
                                  "completed",
                                  true,
                                  "finalContractId",
                                  finalContract.getId(),
                                  "totalFinalClauses",
                                  finalContract.getTotalFinalClauses());
                      } catch (Exception e) {
                          log.error("최종 특약 저장 실패", e);
                          return Map.of(
                                  "message", "특약은 완료되었지만 최종 저장 중 오류가 발생했습니다.", "completed", true);
                      }
                  }
              }

              try {
                  createNextRoundSpecialContractDocument(
                          contractChatId, rejectedOrders, passedOrders);
              } catch (Exception e) {
                  log.error("새 라운드 SPECIAL_CONTRACT 문서 생성 실패", e);
              }

              resetSelectionDocument(contractChatId);

              return Map.of("message", "특약 협상이 시작됩니다.", "completed", true);
          }
      }

      private List<Long> findPassedOrders(
              Map<Integer, Boolean> ownerSelections, Map<Integer, Boolean> tenantSelections) {
          List<Long> passedOrders = new ArrayList<>();

          for (int order = 1; order <= 6; order++) {
              Boolean ownerChoice = ownerSelections.get(order);
              Boolean tenantChoice = tenantSelections.get(order);

              if (Boolean.TRUE.equals(ownerChoice) && Boolean.TRUE.equals(tenantChoice)) {
                  passedOrders.add((long) order);
              }
          }

          return passedOrders;
      }

      private ContractChat.ContractStatus getNextStatus(ContractChat.ContractStatus current) {
          switch (current) {
              case ROUND0:
                  return ContractChat.ContractStatus.ROUND1;
              case ROUND1:
                  return ContractChat.ContractStatus.ROUND2;
              case ROUND2:
                  return ContractChat.ContractStatus.ROUND3;
              default:
                  return null;
          }
      }

      private boolean isValidSelection(
              Map<Integer, Boolean> selections, List<Integer> availableOrders) {
          return selections.keySet().stream().allMatch(availableOrders::contains);
      }

      private void resetSelectionDocument(Long contractChatId) {
          Optional<SpecialContractSelectionDocument> opt =
                  specialContractMongoRepository.findSelectionByContractChatId(contractChatId);
          if (opt.isPresent()) {
              SpecialContractSelectionDocument doc = opt.get();
              doc.setOwnerSelections(new HashMap<>());
              doc.setTenantSelections(new HashMap<>());
              doc.setOwnerCompleted(false);
              doc.setTenantCompleted(false);
              doc.setProcessed(false);
              specialContractMongoRepository.saveSelectionStatus(doc);
          }
      }

      @Override
      @Transactional
      public SpecialContractFixDocument createSpecialContract(Long contractChatId, Long order) {
          Optional<SpecialContractFixDocument> existing =
                  specialContractMongoRepository.findByContractChatIdAndOrder(contractChatId, order);

          if (existing.isPresent()) {
              throw new IllegalArgumentException(
                      "이미 존재하는 특약입니다: contractChatId=" + contractChatId + ", order=" + order);
          }

          List<ContentDataDto> prevData = new ArrayList<>();
          prevData.add(createEmptyContentData());
          prevData.add(createEmptyContentData());

          ContentDataDto recentData = createEmptyContentData();

          SpecialContractFixDocument document =
                  SpecialContractFixDocument.builder()
                          .contractChatId(contractChatId)
                          .order(order)
                          .round(1L)
                          .isPassed(false)
                          .prevData(prevData)
                          .recentData(recentData)
                          .build();

          return specialContractMongoRepository.createSpecialContract(document);
      }

      @Override
      public Map<String, Object> getAllRoundsSpecialContract(Long contractChatId, Long userId) {
          log.info("=== 전체 라운드 특약 문서 조회 시작 ===");
          log.info("contractChatId: {}, userId: {}", contractChatId, userId);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다.");
          }

          boolean isOwner = userId.equals(contractChat.getOwnerId());
          boolean isTenant = userId.equals(contractChat.getBuyerId());

          if (!isOwner && !isTenant) {
              throw new IllegalArgumentException("해당 계약 채팅방에 접근 권한이 없습니다.");
          }

          String userRole = isOwner ? "owner" : "tenant";

          Map<String, SpecialContractUserViewDto> allRounds = new LinkedHashMap<>();
          int availableRounds = 0;

          for (Long round = 1L; round <= 4L; round++) {
              try {
                  Optional<SpecialContractDocument> documentOpt =
                          specialContractMongoRepository
                                  .findSpecialContractDocumentByContractChatIdAndRound(
                                          contractChatId, round);

                  if (documentOpt.isPresent()) {
                      SpecialContractDocument document = documentOpt.get();

                      List<SpecialContractUserViewDto.ClauseUserView> userClauses =
                              document.getClauses().stream()
                                      .map(
                                              clause -> {
                                                  SpecialContractDocument.Evaluation userEvaluation =
                                                          isOwner
                                                                  ? clause.getAssessment().getOwner()
                                                                  : clause.getAssessment()
                                                                          .getTenant();

                                                  return SpecialContractUserViewDto.ClauseUserView
                                                          .builder()
                                                          .id(clause.getOrder())
                                                          .title(clause.getTitle())
                                                          .content(clause.getContent())
                                                          .level(userEvaluation.getLevel())
                                                          .reason(userEvaluation.getReason())
                                                          .build();
                                              })
                                      .collect(Collectors.toList());

                      SpecialContractUserViewDto roundData =
                              SpecialContractUserViewDto.builder()
                                      .contractChatId(document.getContractChatId())
                                      .round(document.getRound())
                                      .totalClauses(document.getTotalClauses())
                                      .userRole(userRole)
                                      .clauses(userClauses)
                                      .build();

                      allRounds.put("round" + round, roundData);
                      availableRounds++;

                      log.info("라운드 {} 조회 완료 - clauses: {}", round, userClauses.size());
                  } else {
                      log.info("라운드 {} 문서 없음", round);
                      allRounds.put("round" + round, null);
                  }
              } catch (Exception e) {
                  log.error("라운드 {} 조회 실패: {}", round, e.getMessage());
                  allRounds.put("round" + round, null);
              }
          }

          Map<String, Object> result = new HashMap<>();
          result.put("contractChatId", contractChatId);
          result.put("userRole", userRole);
          result.put("currentStatus", contractChat.getStatus());
          result.put("availableRounds", availableRounds);
          result.put("rounds", allRounds);

          log.info("전체 라운드 특약 문서 조회 완료 - 사용 가능한 라운드: {}", availableRounds);

          return result;
      }

      @Override
      public SpecialContractUserViewDto getSpecialContractForUserByStatus(
              Long contractChatId, Long userId) {
          log.info("=== 상태별 특약 문서 조회 시작 ===");
          log.info("contractChatId: {}, userId: {}", contractChatId, userId);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              log.error("계약 채팅방을 찾을 수 없음 - contractChatId: {}", contractChatId);
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다.");
          }

          boolean isOwner = userId.equals(contractChat.getOwnerId());
          boolean isTenant = userId.equals(contractChat.getBuyerId());

          if (!isOwner && !isTenant) {
              log.error("접근 권한 없음 - contractChatId: {}, userId: {}", contractChatId, userId);
              throw new IllegalArgumentException("해당 계약 채팅방에 접근 권한이 없습니다.");
          }

          ContractChat.ContractStatus currentStatus = contractChat.getStatus();
          String userRole = isOwner ? "owner" : "tenant";

          if (currentStatus == ContractChat.ContractStatus.ROUND1
                  || currentStatus == ContractChat.ContractStatus.ROUND2
                  || currentStatus == ContractChat.ContractStatus.ROUND3) {

              log.info("ROUND1~3 상태 - 완료되지 않은 특약 문서만 조회: {}", currentStatus);

              List<SpecialContractFixDocument> incompleteFixDocs =
                      getIncompleteSpecialContractsByChat(contractChatId, userId);

              if (incompleteFixDocs.isEmpty()) {
                  log.warn("완료되지 않은 특약 문서를 찾을 수 없음 - contractChatId: {}", contractChatId);
                  throw new IllegalArgumentException("완료되지 않은 특약 문서를 찾을 수 없습니다: " + contractChatId);
              }

              Set<Integer> targetOrders =
                      incompleteFixDocs.stream()
                              .map(doc -> doc.getOrder().intValue())
                              .collect(Collectors.toSet());

              Long currentRound = getCurrentRoundNumber(currentStatus);
              SpecialContractDocument fullDocument =
                      specialContractMongoRepository
                              .findSpecialContractDocumentByContractChatIdAndRound(
                                      contractChatId, currentRound)
                              .orElseThrow(
                                      () -> {
                                          log.warn(
                                                  "라운드 {}의 특약 문서를 찾을 수 없음 - contractChatId: {}",
                                                  currentRound,
                                                  contractChatId);
                                          return new IllegalArgumentException(
                                                  "라운드 "
                                                          + currentRound
                                                          + "의 특약 문서를 찾을 수 없습니다: "
                                                          + contractChatId);
                                      });

              List<SpecialContractUserViewDto.ClauseUserView> userClauses =
                      fullDocument.getClauses().stream()
                              .filter(clause -> targetOrders.contains(clause.getOrder()))
                              .map(
                                      clause -> {
                                          SpecialContractDocument.Evaluation userEvaluation =
                                                  isOwner
                                                          ? clause.getAssessment().getOwner()
                                                          : clause.getAssessment().getTenant();

                                          log.debug(
                                                  "특약 {} 변환 - title: {}, level: {}",
                                                  clause.getOrder(),
                                                  clause.getTitle(),
                                                  userEvaluation.getLevel());

                                          return SpecialContractUserViewDto.ClauseUserView.builder()
                                                  .id(clause.getOrder())
                                                  .title(clause.getTitle())
                                                  .content(clause.getContent())
                                                  .level(userEvaluation.getLevel())
                                                  .reason(userEvaluation.getReason())
                                                  .build();
                                      })
                              .collect(Collectors.toList());

              SpecialContractUserViewDto result =
                      SpecialContractUserViewDto.builder()
                              .contractChatId(fullDocument.getContractChatId())
                              .round(fullDocument.getRound())
                              .totalClauses(userClauses.size())
                              .userRole(userRole)
                              .clauses(userClauses)
                              .build();

              log.info(
                      "완료되지 않은 특약 문서 조회 완료 - userRole: {}, clauses: {}, round: {}",
                      userRole,
                      userClauses.size(),
                      fullDocument.getRound());

              return result;
          }

          Long targetRound = determineTargetRound(currentStatus);
          log.info("현재 상태: {}, 조회할 라운드: {}", currentStatus, targetRound);

          SpecialContractDocument document =
                  specialContractMongoRepository
                          .findSpecialContractDocumentByContractChatIdAndRound(
                                  contractChatId, targetRound)
                          .orElseThrow(
                                  () -> {
                                      log.warn(
                                              "라운드 {}의 특약 문서를 찾을 수 없음 - contractChatId: {}",
                                              targetRound,
                                              contractChatId);
                                      return new IllegalArgumentException(
                                              "라운드 "
                                                      + targetRound
                                                      + "의 특약 문서를 찾을 수 없습니다: "
                                                      + contractChatId);
                                  });

          log.info(
                  "특약 문서 조회 완료 - round: {}, totalClauses: {}",
                  document.getRound(),
                  document.getTotalClauses());

          log.info("사용자 역할 확인 완료 - userRole: {}", userRole);

          List<SpecialContractUserViewDto.ClauseUserView> userClauses =
                  document.getClauses().stream()
                          .map(
                                  clause -> {
                                      SpecialContractDocument.Evaluation userEvaluation =
                                              isOwner
                                                      ? clause.getAssessment().getOwner()
                                                      : clause.getAssessment().getTenant();

                                      log.debug(
                                              "특약 {} 변환 - title: {}, level: {}",
                                              clause.getOrder(),
                                              clause.getTitle(),
                                              userEvaluation.getLevel());

                                      return SpecialContractUserViewDto.ClauseUserView.builder()
                                              .id(clause.getOrder())
                                              .title(clause.getTitle())
                                              .content(clause.getContent())
                                              .level(userEvaluation.getLevel())
                                              .reason(userEvaluation.getReason())
                                              .build();
                                  })
                          .collect(Collectors.toList());

          SpecialContractUserViewDto result =
                  SpecialContractUserViewDto.builder()
                          .contractChatId(document.getContractChatId())
                          .round(document.getRound())
                          .totalClauses(document.getTotalClauses())
                          .userRole(userRole)
                          .clauses(userClauses)
                          .build();

          log.info(
                  "상태별 특약 문서 조회 완료 - userRole: {}, clauses: {}, round: {}",
                  userRole,
                  userClauses.size(),
                  document.getRound());

          return result;
      }

      private Long getCurrentRoundNumber(ContractChat.ContractStatus status) {
          switch (status) {
              case ROUND1:
                  return 2L;
              case ROUND2:
                  return 3L;
              case ROUND3:
                  return 4L;
              default:
                  return 1L;
          }
      }

      private Long determineTargetRound(ContractChat.ContractStatus status) {
          switch (status) {
              case STEP0:
              case STEP1:
              case STEP2:
                  return 1L;
              case ROUND0:
                  return 1L;
              case ROUND1:
                  return 2L;
              case ROUND2:
                  return 3L;
              case ROUND3:
                  return 4L;
              default:
                  return 1L;
          }
      }

      @Override
      public SpecialContractFixDocument findSpecialContract(Long contractChatId) {
          return specialContractMongoRepository
                  .findByContractChatId(contractChatId)
                  .orElseThrow(
                          () ->
                                  new IllegalArgumentException(
                                          "해당 특약 문서를 찾을 수 없습니다: " + contractChatId));
      }

      @Override
      public SpecialContractFixDocument updateRecentData(
              Long contractChatId, Long order, String messages) {
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          Long currentRound = contractChat.getCurrentRound();

          SpecialContractDocument specialContract =
                  specialContractMongoRepository
                          .findSpecialContractDocumentByContractChatIdAndRound(
                                  contractChatId, currentRound)
                          .orElseThrow(
                                  () ->
                                          new IllegalArgumentException(
                                                  "라운드 "
                                                          + currentRound
                                                          + "의 특약 문서를 찾을 수 없습니다: "
                                                          + contractChatId));

          SpecialContractDocument.Clause targetClause =
                  specialContract.getClauses().stream()
                          .filter(clause -> clause.getOrder().equals(order.intValue()))
                          .findFirst()
                          .orElseThrow(
                                  () ->
                                          new IllegalArgumentException(
                                                  "해당 order의 특약 조항을 찾을 수 없습니다: " + order));
          SpecialContractFixDocument document =
                  specialContractMongoRepository
                          .findByContractChatIdAndOrder(contractChatId, order)
                          .orElseThrow(
                                  () ->
                                          new IllegalArgumentException(
                                                  "해당 특약 문서를 찾을 수 없습니다: contractChatId="
                                                          + contractChatId
                                                          + ", order="
                                                          + order));

          ContentDataDto updatedRecentData =
                  ContentDataDto.builder()
                          .title(targetClause.getTitle())
                          .content(targetClause.getContent())
                          .messages(messages != null ? messages : "")
                          .build();

          document.setRecentData(updatedRecentData);

          return specialContractMongoRepository.updateSpecialContract(document);
      }

      @Override
      public SpecialContractFixDocument markSpecialContractAsPassed(Long contractChatId, Long order) {
          SpecialContractFixDocument document =
                  specialContractMongoRepository
                          .findByContractChatIdAndOrder(contractChatId, order)
                          .orElseThrow(
                                  () ->
                                          new IllegalArgumentException(
                                                  "특약 문서를 찾을 수 없습니다: contractChatId="
                                                          + contractChatId
                                                          + ", order="
                                                          + order));

          document.setIsPassed(true);
          return specialContractMongoRepository.updateSpecialContract(document);
      }

      @Override
      public List<SpecialContractFixDocument> getCompletedSpecialContracts() {
          return specialContractMongoRepository.findByIsPassed(true);
      }

      @Override
      public List<SpecialContractFixDocument> getIncompleteSpecialContractsByChat(
              Long contractChatId, Long userId) {
          if (!isUserInContractChat(contractChatId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          return specialContractMongoRepository.findByContractChatIdAndIsPassed(
                  contractChatId, false);
      }

      @Override
      public List<SpecialContractFixDocument> getIncompleteSpecialContractsWithoutMessage(
              Long contractChatId, Long userId) {
          if (!isUserInContractChat(contractChatId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          return specialContractMongoRepository
                  .findByContractChatIdAndIsPassedAndRecentDataMessagesEmpty(contractChatId, false);
      }

      private ContentDataDto createEmptyContentData() {
          return ContentDataDto.builder().title("").content("").messages("").build();
      }

      private List<Long> findRejectedOrders(
              Map<Integer, Boolean> ownerSelections, Map<Integer, Boolean> tenantSelections) {
          List<Long> rejectedOrders = new ArrayList<>();

          for (int order = 1; order <= 6; order++) {
              Boolean ownerChoice = ownerSelections.get(order);
              Boolean tenantChoice = tenantSelections.get(order);

              if (Boolean.FALSE.equals(ownerChoice) || Boolean.FALSE.equals(tenantChoice)) {
                  rejectedOrders.add((long) order);
              }
          }

          return rejectedOrders;
      }

      @Override
      @Transactional
      public FinalSpecialContractDocument saveFinalSpecialContract(Long contractChatId) {
          log.info("=== 최종 특약 저장 시작 ===");
          log.info("contractChatId: {}", contractChatId);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          SpecialContractDocument latestDocument = null;
          Long latestRound = null;

          for (Long round = 4L; round >= 1L; round--) {
              Optional<SpecialContractDocument> docOpt =
                      specialContractMongoRepository
                              .findSpecialContractDocumentByContractChatIdAndRound(
                                      contractChatId, round);

              if (docOpt.isPresent()) {
                  latestDocument = docOpt.get();
                  latestRound = round;
                  log.info("가장 최근 라운드 발견: {}", round);
                  break;
              }
          }

          if (latestDocument == null) {
              throw new IllegalStateException("특약 문서를 찾을 수 없습니다: " + contractChatId);
          }

          List<FinalSpecialContractDocument.FinalClause> finalClauses = new ArrayList<>();

          for (SpecialContractDocument.Clause clause : latestDocument.getClauses()) {
              if (clause.getOrder() != null) {
                  String title = clause.getTitle();
                  String content = clause.getContent();

                  if (title != null
                          && !title.trim().isEmpty()
                          && content != null
                          && !content.trim().isEmpty()) {

                      FinalSpecialContractDocument.FinalClause finalClause =
                              FinalSpecialContractDocument.FinalClause.builder()
                                      .order(clause.getOrder())
                                      .title(title.trim())
                                      .content(content.trim())
                                      .build();

                      finalClauses.add(finalClause);
                      log.info("특약 {}번 저장 완료 (라운드 {}): {}", clause.getOrder(), latestRound, title);
                  } else {
                      boolean foundInPreviousRound = false;
                      for (Long searchRound = latestRound - 1; searchRound >= 1L; searchRound--) {
                          Optional<SpecialContractDocument> prevDocOpt =
                                  specialContractMongoRepository
                                          .findSpecialContractDocumentByContractChatIdAndRound(
                                                  contractChatId, searchRound);

                          if (prevDocOpt.isPresent()) {
                              SpecialContractDocument prevDoc = prevDocOpt.get();

                              for (SpecialContractDocument.Clause prevClause : prevDoc.getClauses()) {
                                  if (prevClause.getOrder() != null
                                          && prevClause.getOrder().equals(clause.getOrder())) {

                                      String prevTitle = prevClause.getTitle();
                                      String prevContent = prevClause.getContent();

                                      if (prevTitle != null
                                              && !prevTitle.trim().isEmpty()
                                              && prevContent != null
                                              && !prevContent.trim().isEmpty()) {

                                          FinalSpecialContractDocument.FinalClause finalClause =
                                                  FinalSpecialContractDocument.FinalClause.builder()
                                                          .order(prevClause.getOrder())
                                                          .title(prevTitle.trim())
                                                          .content(prevContent.trim())
                                                          .build();

                                          finalClauses.add(finalClause);
                                          log.info(
                                                  "특약 {}번 저장 완료 (이전 라운드 {}): {}",
                                                  prevClause.getOrder(),
                                                  searchRound,
                                                  prevTitle);
                                          foundInPreviousRound = true;
                                          break;
                                      }
                                  }
                              }

                              if (foundInPreviousRound) {
                                  break;
                              }
                          }
                      }

                      if (!foundInPreviousRound) {
                          log.info("특약 {}번: 모든 라운드에서 유효한 내용을 찾을 수 없음 - 건너뜀", clause.getOrder());
                      }
                  }
              }
          }

          finalClauses.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));

          log.info("최종 저장될 특약 개수: {}", finalClauses.size());
          for (FinalSpecialContractDocument.FinalClause clause : finalClauses) {
              log.info("- 특약 {}번: {}", clause.getOrder(), clause.getTitle());
          }

          FinalSpecialContractDocument finalDocument =
                  FinalSpecialContractDocument.builder()
                          .contractChatId(contractChatId)
                          .totalFinalClauses(finalClauses.size())
                          .finalClauses(finalClauses)
                          .build();

          FinalSpecialContractDocument savedDocument =
                  specialContractMongoRepository.saveFinalSpecialContract(finalDocument);

          log.info("=== 최종 특약 저장 완료 ===");
          log.info("저장된 문서 ID: {}", savedDocument.getId());
          log.info("총 특약 개수: {}", savedDocument.getTotalFinalClauses());

          return savedDocument;
      }

      private Optional<SpecialContractDocument> findLatestRoundForOrder(
              Long contractChatId, Long order) {
          for (Long round = 4L; round >= 1L; round--) {
              Optional<SpecialContractDocument> doc =
                      specialContractMongoRepository
                              .findSpecialContractDocumentByContractChatIdAndRound(
                                      contractChatId, round);

              if (doc.isPresent()) {
                  boolean hasOrder =
                          doc.get().getClauses().stream()
                                  .anyMatch(
                                          clause ->
                                                  clause.getOrder().equals(order.intValue())
                                                          && !clause.getTitle().isEmpty()
                                                          && !clause.getContent().isEmpty());

                  if (hasOrder) {
                      return doc;
                  }
              }
          }
          return Optional.empty();
      }

      @Transactional
      public void checkAndIncrementRoundIfComplete(Long contractChatId) {
          log.info("=== 라운드 완료 체크 시작 ===");

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          ContractChat.ContractStatus currentStatus = contractChat.getStatus();

          Long nextRoundNumber = getNextRoundNumber(currentStatus);
          if (nextRoundNumber == null) {
              log.info("더 이상 증가할 라운드가 없음: {}", currentStatus);

              if (currentStatus == ContractChat.ContractStatus.ROUND3) {
                  checkFinalRoundCompletion(contractChatId);
              }
              return;
          }

          log.info("현재 상태: {}, 체크할 라운드: {}", currentStatus, nextRoundNumber);

          Optional<SpecialContractDocument> documentOpt =
                  specialContractMongoRepository.findSpecialContractDocumentByContractChatIdAndRound(
                          contractChatId, nextRoundNumber);

          if (documentOpt.isEmpty()) {
              log.warn("라운드 {}의 문서를 찾을 수 없음", nextRoundNumber);
              return;
          }

          SpecialContractDocument document = documentOpt.get();

          List<SpecialContractFixDocument> incompleteContracts =
                  specialContractMongoRepository.findByContractChatIdAndIsPassed(
                          contractChatId, false);

          if (incompleteContracts.isEmpty()) {
              log.info("미완료 특약이 없어서 라운드 증가 체크 불필요");
              return;
          }

          Set<Integer> incompleteOrders =
                  incompleteContracts.stream()
                          .map(doc -> doc.getOrder().intValue())
                          .collect(Collectors.toSet());

          log.info("미완료 특약 번호들: {}", incompleteOrders);

          boolean allIncompleteClausesAreFilled =
                  incompleteOrders.stream().allMatch(order -> isClauseFilled(document, order));

          log.info("모든 미완료 특약이 꽉 찼는지: {}", allIncompleteClausesAreFilled);

          if (allIncompleteClausesAreFilled) {
              ContractChat.ContractStatus nextStatus = getNextStatus(currentStatus);
              if (nextStatus != null) {
                  contractChatMapper.updateStatus(contractChatId, nextStatus);
                  log.info("라운드 자동 증가: {} → {}", currentStatus, nextStatus);
                  String aimsg = getRoundIncrementMessage(nextStatus);
                  AiMessageBtn(contractChatId, aimsg);

                  if (nextStatus == ContractChat.ContractStatus.ROUND3) {
                      checkFinalRoundCompletion(contractChatId);
                  }
              }
          } else {
              log.info("아직 모든 특약이 꽉 차지 않아서 라운드 유지");
          }
      }

      @Transactional
      public void checkFinalRoundCompletion(Long contractChatId) {
          log.info("=== 최종 라운드(4차) 완료 체크 시작 ===");

          Optional<SpecialContractDocument> round4DocOpt =
                  specialContractMongoRepository.findSpecialContractDocumentByContractChatIdAndRound(
                          contractChatId, 4L);

          if (round4DocOpt.isEmpty()) {
              log.info("4차 라운드 문서가 아직 없음");
              return;
          }

          SpecialContractDocument round4Document = round4DocOpt.get();

          List<SpecialContractFixDocument> incompleteContracts =
                  specialContractMongoRepository.findByContractChatIdAndIsPassed(
                          contractChatId, false);

          if (incompleteContracts.isEmpty()) {
              log.info("이미 모든 특약이 완료됨");
              return;
          }

          Set<Integer> incompleteOrders =
                  incompleteContracts.stream()
                          .map(doc -> doc.getOrder().intValue())
                          .collect(Collectors.toSet());

          log.info("미완료 특약 번호들: {}", incompleteOrders);

          boolean allFinalClausesAreFilled =
                  incompleteOrders.stream().allMatch(order -> isClauseFilled(round4Document, order));

          log.info("4차 라운드 모든 미완료 특약이 작성됨: {}", allFinalClausesAreFilled);

          if (allFinalClausesAreFilled) {
              log.info("🎉 모든 특약 협상이 완료되었습니다! 자동으로 완료 처리합니다.");

              for (SpecialContractFixDocument incompleteContract : incompleteContracts) {
                  try {
                      markSpecialContractAsPassed(contractChatId, incompleteContract.getOrder());
                      log.info("특약 {}번 자동 완료 처리", incompleteContract.getOrder());
                  } catch (Exception e) {
                      log.error("특약 {}번 완료 처리 실패: {}", incompleteContract.getOrder(), e.getMessage());
                  }
              }

              try {
                  FinalSpecialContractDocument finalContract =
                          saveFinalSpecialContract(contractChatId);

                  AiMessageNext(
                          contractChatId,
                          "🎉 3차 수정까지 모든 특약 협상이 완료되었습니다! "
                                  + "최종 특약서가 자동으로 생성되었습니다. "
                                  + "총 "
                                  + finalContract.getTotalFinalClauses()
                                  + "개의 특약이 확정되었습니다.");
                  contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.ROUND4);

                  log.info(
                          "최종 특약 자동 저장 완료 - finalContractId: {}, 총 {}개 조항",
                          finalContract.getId(),
                          finalContract.getTotalFinalClauses());

              } catch (Exception e) {
                  log.error("최종 특약 자동 저장 실패", e);
              }
          } else {
              log.info("아직 4차 라운드의 모든 특약이 작성되지 않음");
          }
      }

      private boolean isClauseFilled(SpecialContractDocument document, Integer order) {
          return document.getClauses().stream()
                  .filter(clause -> Objects.equals(clause.getOrder(), order))
                  .findFirst()
                  .map(
                          clause -> {
                              String title = clause.getTitle();
                              String content = clause.getContent();

                              boolean titleFilled = title != null && !title.trim().isEmpty();
                              boolean contentFilled = content != null && !content.trim().isEmpty();
                              boolean isFilled = titleFilled && contentFilled;

                              log.info("🔍 특약 {}번 상세 체크:", order);
                              log.info("  - title 원본: '{}'", title);
                              log.info("  - title 길이: {}", title != null ? title.length() : "null");
                              log.info(
                                      "  - title trim 후: '{}'",
                                      title != null ? title.trim() : "null");
                              log.info("  - title filled: {}", titleFilled);

                              log.info("  - content 원본: '{}'", content);
                              log.info(
                                      "  - content 길이: {}",
                                      content != null ? content.length() : "null");
                              log.info(
                                      "  - content trim 후: '{}'",
                                      content != null ? content.trim() : "null");
                              log.info("  - content filled: {}", contentFilled);

                              log.info("  - 최종 결과: {}", isFilled);

                              return isFilled;
                          })
                  .orElseGet(
                          () -> {
                              log.warn("⚠️ 특약 {}번을 문서에서 찾을 수 없음!", order);
                              return false;
                          });
      }

      private Long getNextRoundNumber(ContractChat.ContractStatus status) {
          switch (status) {
              case ROUND0:
                  return 2L;
              case ROUND1:
                  return 3L;
              case ROUND2:
                  return 4L;
              case ROUND3:
                  return null;
              default:
                  return null;
          }
      }

      public String getContractChatStatus(ContractChat.ContractStatus status) {
          switch (status) {
              case STEP0:
                  return "?step=1";
              case STEP1:
                  return "?step=2";
              case STEP2:
                  return "?step=3";
              case ROUND0:
                  return "?step=3&round=0";
              case ROUND1:
                  return "?step=3&round=1";
              case ROUND2:
                  return "?step=3&round=2";
              case ROUND3:
                  return "?step=3&round=3";
              case ROUND4:
                  return "?step=3&round=4";
              case COMPLETE:
                  return "?step=3&round=4";
              default:
                  return null;
          }
      }

      @Override
      public String getContractStatusParam(Long contractChatId, Long userId) {
          ContractChat contractChat = getContractChatInfo(contractChatId, userId);
          return getContractChatStatus(contractChat.getStatus());
      }

      private String getRoundIncrementMessage(ContractChat.ContractStatus status) {
          switch (status) {
              case ROUND1:
                  return "1차 수정이 완료되었습니다! 2차 협상 라운드가 시작됩니다.";
              case ROUND2:
                  return "2차 수정이 완료되었습니다! 3차 협상 라운드가 시작됩니다.";
              case ROUND3:
                  return "3차 수정이 완료되었습니다! 최종 협상 라운드가 시작됩니다.";
              default:
                  return "새로운 협상 라운드가 시작됩니다.";
          }
      }

      @Override
      @Transactional
      public Map<String, Object> respondToFinalContractDeletionRequest(
              Long contractChatId, Long buyerId, FinalContractDeletionResponseDto responseDto) {

          log.info("=== 삭제 요청 응답 처리 시작 ===");
          log.info(
                  "contractChatId: {}, buyerId: {}, accepted: {}",
                  contractChatId,
                  buyerId,
                  responseDto.isAccepted());

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!buyerId.equals(contractChat.getBuyerId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임차인만 응답할 수 있습니다.");
          }

          String redisKey =
                  "final-contract:deletion:" + contractChatId + ":" + contractChat.getOwnerId();
          String clauseOrderStr = stringRedisTemplate.opsForValue().get(redisKey);

          if (clauseOrderStr == null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_NOT_FOUND, "삭제 요청이 존재하지 않습니다.");
          }

          Integer clauseOrder = Integer.parseInt(clauseOrderStr);

          Map<String, Object> result = new HashMap<>();
          String resultMessage;

          if (responseDto.isAccepted()) {
              // 삭제 수락 로직
              FinalSpecialContractDocument finalContract =
                      specialContractMongoRepository
                              .findFinalContractByContractChatId(contractChatId)
                              .orElseThrow(() -> new IllegalArgumentException("최종 특약서를 찾을 수 없습니다."));

              List<FinalSpecialContractDocument.FinalClause> updatedClauses =
                      finalContract.getFinalClauses().stream()
                              .filter(clause -> !clause.getOrder().equals(clauseOrder))
                              .collect(Collectors.toList());

              finalContract.setFinalClauses(updatedClauses);
              finalContract.setTotalFinalClauses(updatedClauses.size());

              specialContractMongoRepository.saveFinalSpecialContract(finalContract);

              resultMessage = String.format("임차인이 특약 %d번 삭제 요청을 수락했습니다. 특약이 삭제되었습니다.", clauseOrder);

              result.put("message", "특약이 삭제되었습니다.");
              result.put("deletedClauseOrder", clauseOrder);
              result.put("finalContractId", finalContract.getId());
              result.put("remainingClauses", finalContract.getTotalFinalClauses());

              log.info("특약 {}번 삭제 완료 - contractChatId: {}", clauseOrder, contractChatId);

          } else {
              // 삭제 거절
              resultMessage = String.format("임차인이 특약 %d번 삭제 요청을 거절했습니다. 기존 특약이 유지됩니다.", clauseOrder);

              result.put("message", "삭제 요청을 거절했습니다.");
              result.put("clauseOrder", clauseOrder);

              log.info("특약 {}번 삭제 거절 완료 - contractChatId: {}", clauseOrder, contractChatId);
          }

          stringRedisTemplate.delete(redisKey);
          AiMessage(contractChatId, resultMessage);

          return result;
      }

      @Override
      @Transactional
      public ModificationRequestData requestFinalContractModification(
              Long contractChatId, Long ownerId, FinalContractModificationRequestDto requestDto) {

          log.info("=== 최종 특약서 수정 요청 시작 ===");
          log.info(
                  "contractChatId: {}, ownerId: {}, clauseOrder: {}",
                  contractChatId,
                  ownerId,
                  requestDto.getClauseOrder());

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!ownerId.equals(contractChat.getOwnerId())) {
              throw new BusinessException(
                      ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임대인만 수정 요청할 수 있습니다.");
          }

          Optional<FinalSpecialContractDocument> finalContractOpt =
                  specialContractMongoRepository.findFinalContractByContractChatId(contractChatId);

          if (finalContractOpt.isEmpty()) {
              throw new IllegalArgumentException("최종 특약서가 생성되지 않았습니다.");
          }

          FinalSpecialContractDocument finalContract = finalContractOpt.get();
          boolean clauseExists =
                  finalContract.getFinalClauses().stream()
                          .anyMatch(clause -> clause.getOrder().equals(requestDto.getClauseOrder()));

          if (!clauseExists) {
              throw new IllegalArgumentException(
                      "해당 특약 조항을 찾을 수 없습니다: " + requestDto.getClauseOrder());
          }

          String redisKey = "final-contract:modification:" + contractChatId + ":" + ownerId;

          String existingRequest = stringRedisTemplate.opsForValue().get(redisKey);
          if (existingRequest != null) {
              throw new IllegalArgumentException("해당 조항에 대한 수정 요청이 이미 대기중입니다.");
          }

          ModificationRequestData requestData =
                  ModificationRequestData.builder()
                          .newTitle(requestDto.getNewTitle())
                          .newContent(requestDto.getNewContent())
                          .requesterId(ownerId)
                          .createdAt(LocalDateTime.now().toString())
                          .build();

          try {
              String jsonData = objectMapper.writeValueAsString(requestData);
              String valueData =
                      String.format(
                              "{\"clauseOrder\":%d,\"requestData\":%s}",
                              requestDto.getClauseOrder(), jsonData);
              stringRedisTemplate.opsForValue().set(redisKey, valueData, Duration.ofHours(24));

              String notificationMessage =
                      String.format(
                              "임대인이 특약 %d번 수정을 요청했습니다.\n\n" + "📝 수정 제목: %s\n" + "✏️ 수정 내용: %s\n\n",
                              requestDto.getClauseOrder(),
                              requestDto.getNewTitle(),
                              requestDto.getNewContent());

              AiMessageBtn(contractChatId, notificationMessage);
              log.info("수정 요청 Redis 저장 완료 - key: {}", redisKey);
              return requestData;

          } catch (Exception e) {
              log.error("수정 요청 저장 실패", e);
              throw new RuntimeException("수정 요청 저장 중 오류가 발생했습니다.");
          }
      }

      @Override
      @Transactional
      public FinalSpecialContractDocument respondToModificationRequest(
              Long contractChatId, Long buyerId, FinalContractModificationResponseDto responseDto) {

          log.info("=== 수정 요청 응답 처리 시작 ===");
          log.info(
                  "contractChatId: {}, buyerId: {}, accepted: {}",
                  contractChatId,
                  buyerId,
                  responseDto.isAccepted());

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!buyerId.equals(contractChat.getBuyerId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임차인만 응답할 수 있습니다.");
          }

          String redisKey =
                  "final-contract:modification:" + contractChatId + ":" + contractChat.getOwnerId();
          String valueDataJson = stringRedisTemplate.opsForValue().get(redisKey);

          if (valueDataJson == null) {
              throw new IllegalArgumentException("대기중인 수정 요청이 없습니다.");
          }

          try {
              // JSON에서 clauseOrder와 requestData 추출
              com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(valueDataJson);
              Integer clauseOrder = rootNode.get("clauseOrder").asInt();
              String requestDataJson = rootNode.get("requestData").toString();

              ModificationRequestData requestData =
                      objectMapper.readValue(requestDataJson, ModificationRequestData.class);

              FinalSpecialContractDocument finalContract =
                      specialContractMongoRepository
                              .findFinalContractByContractChatId(contractChatId)
                              .orElseThrow(() -> new IllegalArgumentException("최종 특약서를 찾을 수 없습니다."));

              String resultMessage;

              if (responseDto.isAccepted()) {
                  List<FinalSpecialContractDocument.FinalClause> updatedClauses =
                          finalContract.getFinalClauses().stream()
                                  .map(
                                          clause -> {
                                              if (clause.getOrder().equals(clauseOrder)) {
                                                  return FinalSpecialContractDocument.FinalClause
                                                          .builder()
                                                          .order(clause.getOrder())
                                                          .title(requestData.getNewTitle())
                                                          .content(requestData.getNewContent())
                                                          .build();
                                              }
                                              return clause;
                                          })
                                  .collect(Collectors.toList());

                  finalContract.setFinalClauses(updatedClauses);
                  specialContractMongoRepository.saveFinalSpecialContract(finalContract);

                  resultMessage =
                          String.format("임차인이 특약 %d번 수정 요청을 수락했습니다. 특약이 변경되었습니다.", clauseOrder);
                  log.info("수정 수락 - 최종 특약서 업데이트 완료");

              } else {
                  resultMessage =
                          String.format("임차인이 특약 %d번 수정 요청을 거절했습니다. 기존 특약이 유지됩니다.", clauseOrder);
                  log.info("수정 거절 - 기존 특약서 유지");
              }

              stringRedisTemplate.delete(redisKey);
              AiMessage(contractChatId, resultMessage);
              return finalContract;

          } catch (Exception e) {
              log.error("수정 요청 응답 처리 실패", e);
              throw new RuntimeException("응답 처리 중 오류가 발생했습니다.");
          }
      }

      @Override
      public ModificationRequestData getPendingModificationRequest(
              Long contractChatId, Integer clauseOrder) {
          // 기존 방식 대신 임대인의 요청을 찾도록 수정
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              return null;
          }

          String redisKey =
                  "final-contract:modification:" + contractChatId + ":" + contractChat.getOwnerId();
          String valueDataJson = stringRedisTemplate.opsForValue().get(redisKey);

          if (valueDataJson == null) {
              return null;
          }

          try {
              com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(valueDataJson);
              Integer storedClauseOrder = rootNode.get("clauseOrder").asInt();

              // 요청한 clauseOrder와 저장된 clauseOrder가 일치하는지 확인
              if (!clauseOrder.equals(storedClauseOrder)) {
                  return null;
              }

              String requestDataJson = rootNode.get("requestData").toString();
              return objectMapper.readValue(requestDataJson, ModificationRequestData.class);

          } catch (Exception e) {
              log.error("수정 요청 데이터 파싱 실패", e);
              return null;
          }
      }

      @Override
      public boolean hasPendingModificationRequest(Long contractChatId, Integer clauseOrder) {
          String redisKey = "final-contract:modification:" + contractChatId + ":" + clauseOrder;
          return stringRedisTemplate.hasKey(redisKey);
      }

      @Override
      @Transactional
      public void requestFinalContractConfirmation(Long contractChatId, Long ownerId) {
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!ownerId.equals(contractChat.getOwnerId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          Optional<FinalSpecialContractDocument> finalContractOpt =
                  specialContractMongoRepository.findFinalContractByContractChatId(contractChatId);

          if (finalContractOpt.isEmpty()) {
              throw new IllegalArgumentException("최종 특약서가 생성되지 않았습니다.");
          }

          AiMessageBtn(contractChatId, "임대인이 최종 특약 확정을 요청하였습니다");

          String key = "final-contract:confirmation:" + contractChatId;
          String existingValue = stringRedisTemplate.opsForValue().get(key);
          if (existingValue != null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_ALREADY_EXISTS, "이미 확정 요청이 진행 중입니다.");
          }
          String value = ownerId.toString();
          stringRedisTemplate.opsForValue().set(key, value);
      }

      @Override
      @Transactional
      public Map<String, Object> acceptFinalContractConfirmation(Long contractChatId, Long buyerId) {
          if (!isUserInContractChat(contractChatId, buyerId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          Long ownerId = contractChat.getOwnerId();

          if (!buyerId.equals(contractChat.getBuyerId())) {
              throw new BusinessException(
                      ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임차인만 확정 수락을 할 수 있습니다.");
          }

          String redisKey = "final-contract:confirmation:" + contractChatId;
          String storedOwnerId = stringRedisTemplate.opsForValue().get(redisKey);

          if (storedOwnerId == null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_NOT_FOUND, "확정 요청이 존재하지 않습니다.");
          }

          if (!storedOwnerId.equals(ownerId.toString())) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_INVALID, "확정 요청 정보가 유효하지 않습니다.");
          }

          FinalSpecialContractDocument finalContract =
                  specialContractMongoRepository
                          .findFinalContractByContractChatId(contractChatId)
                          .orElseThrow(() -> new IllegalArgumentException("최종 특약서를 찾을 수 없습니다."));

          contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.STEP4);

          stringRedisTemplate.delete(redisKey);

          String confirmationMessage = "🎉 임차인이 최종 특약서를 수락했습니다! 특약서가 확정되었습니다.";

          AiMessage(contractChatId, confirmationMessage);

          // [적법성 검사] 계약서 1 몽고DB에 특약 저장
          contractFixService.saveSpecialContract(contractChatId, buyerId);
          try {
              Thread.sleep(2000);
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
          }
          AiMessageNext(contractChatId, "다음은 마지막 4단계: '적법성 검토' 단계입니다.");
          try {
              Thread.sleep(2000);
          } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
          }
          AiMessage(contractChatId, "AI가 지금까지 작성된 계약서의 적법성을 분석중이에요!\n 잠시만 기다려주세요!");

          // api/contract/{contractChatId}/legality
          try {
              log.info("적법성 검사 API 호출 시작 - contractChatId: {}", contractChatId);
              Object legalityResponse = contractFixService.getLegality(contractChatId, buyerId);
              String sanitizedLegalityResponse;
              try {
                  ObjectMapper objectMapper = new ObjectMapper();
                  sanitizedLegalityResponse = objectMapper.writeValueAsString(legalityResponse);
              } catch (Exception ex) {
                  sanitizedLegalityResponse = String.valueOf(legalityResponse);
              }
              sanitizedLegalityResponse = sanitizedLegalityResponse.replaceAll("[\\r\\n]", " ");
              log.info("적법성 검사 응답: {}", sanitizedLegalityResponse);
              if (legalityResponse instanceof LegalityDTO) {
                  LegalityDTO legalityDTO = (LegalityDTO) legalityResponse;
                  log.info("LegalityDTO로 응답 파싱 성공");

                  // violations 처리 (중첩 구조로 접근)
                  if (legalityDTO.getData() != null
                          && legalityDTO.getData().getViolations() != null
                          && !legalityDTO.getData().getViolations().isEmpty()) {
                      List<LegalityDTO.Violation> violations = legalityDTO.getData().getViolations();
                      log.info("위반 사항 발견됨: {}개", violations.size());
                      AiMessage(contractChatId, "⚠️ 적법성 검사 결과, 일부 문제점이 발견되었습니다:");

                      for (int i = 0; i < violations.size(); i++) {
                          LegalityDTO.Violation violation = violations.get(i);
                          String sanitizedViolation =
                                  violation == null
                                          ? "null"
                                          : violation.toString().replaceAll("[\\r\\n]", " ");
                          log.info("위반 사항 {}: {}", i + 1, sanitizedViolation);
                          StringBuilder violationMessage = new StringBuilder();

                          violationMessage.append(
                                  // 관련 법령
                                  String.format(
                                          "%s\n" + "\n",
                                          violation.getLawName() != null
                                                  ? violation.getLawName()
                                                  : "정보 없음"));

                          violationMessage.append(
                                  // 위반 내용
                                  String.format(
                                          (i + 1) + ". %s\n" + "\n",
                                          violation.getViolationContent() != null
                                                  ? violation.getViolationContent()
                                                  : "정보 없음"));
                          violationMessage.append(
                                  // 설명
                                  String.format(
                                          "%s\n" + "\n",
                                          violation.getExplanation() != null
                                                  ? violation.getExplanation()
                                                  : "정보 없음"));

                          if (violation.getOriginalClause() != null
                                  && !violation.getOriginalClause().trim().isEmpty()) {
                              violationMessage.append(
                                      String.format(
                                              "📝 문제가 된 조항\n %s\n", violation.getOriginalClause()));
                          }

                          if (violation.getLegalBasis() != null
                                  && !violation.getLegalBasis().trim().isEmpty()) {
                              violationMessage.append(
                                      String.format("📚 법적 근거\n %s\n", violation.getLegalBasis()));
                          }

                          if (violation.getImprovementExample() != null
                                  && !violation.getImprovementExample().trim().isEmpty()) {
                              violationMessage.append(
                                      String.format(
                                              "✅ 개선 방안\n %s\n", violation.getImprovementExample()));
                          }
                          String sanitizedMessage =
                                  violationMessage.toString().replaceAll("[\\r\\n]", " ");
                          log.info("전송할 메시지: {}", sanitizedMessage);
                          AiMessageLegal(contractChatId, violationMessage.toString());

                          try {
                              Thread.sleep(2000);
                          } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                          }
                      }

                      AiMessage(contractChatId, "위 문제점들을 검토하시고 필요시 임대인께서 수정 요청을 해주세요.");
                  } else {
                      log.info("위반 사항 없음");
                      AiMessage(contractChatId, "✅ 적법성 검사 완료! 계약서에 법적 문제가 발견되지 않았습니다.");
                      try {
                          Thread.sleep(2000);
                      } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                      }
                      AiMessage(contractChatId, "최종 계약서 서명하러 갈께요!");
                  }
              } else if (legalityResponse instanceof Map) {
                  // 기존 Map 처리 로직
                  Map<String, Object> responseMap = (Map<String, Object>) legalityResponse;
                  Object violationsObj = responseMap.get("violations");

                  if (violationsObj instanceof List) {
                      List<Map<String, Object>> violations =
                              (List<Map<String, Object>>) violationsObj;
                      if (!violations.isEmpty()) {
                          AiMessage(contractChatId, "⚠️ 적법성 검사 결과, 일부 문제점이 발견되었습니다:");
                      } else {
                          AiMessage(contractChatId, "✅ 적법성 검사 완료! 계약서에 법적 문제가 발견되지 않았습니다.");
                      }
                  }
              } else {
                  log.warn(
                          "응답 타입을 인식할 수 없음: {}",
                          legalityResponse != null ? legalityResponse.getClass() : "null");
                  AiMessage(contractChatId, "❌ 적법성 검사 응답 형식을 인식할 수 없습니다.");
              }
          } catch (Exception e) {
              log.error("적법성 검사 결과 처리 중 오류 발생", e);
              AiMessage(contractChatId, "❌ 적법성 검사 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
          }
          return Map.of(
                  "message",
                  "최종 특약서가 확정되었습니다.",
                  "status",
                  "COMPLETED",
                  "finalContractId",
                  finalContract.getId(),
                  "totalFinalClauses",
                  finalContract.getTotalFinalClauses());
      }

      @Override
      public void rejectFinalContractConfirmation(Long contractChatId, Long buyerId) {
          String redisKey = "final-contract:confirmation:" + contractChatId;
          stringRedisTemplate.delete(redisKey);

          AiMessage(contractChatId, "임차인이 수정을 거절하였습니다.");
      }

      @Override
      @Transactional
      public void requestFinalContractDeletion(
              Long contractChatId, Long ownerId, Integer clauseOrder) {
          log.info("=== 최종 특약 삭제 요청 시작 ===");
          log.info(
                  "contractChatId: {}, ownerId: {}, clauseOrder: {}",
                  contractChatId,
                  ownerId,
                  clauseOrder);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!ownerId.equals(contractChat.getOwnerId())) {
              throw new BusinessException(
                      ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임대인만 삭제 요청할 수 있습니다.");
          }

          Optional<FinalSpecialContractDocument> finalContractOpt =
                  specialContractMongoRepository.findFinalContractByContractChatId(contractChatId);

          if (finalContractOpt.isEmpty()) {
              throw new IllegalArgumentException("최종 특약서가 생성되지 않았습니다.");
          }

          FinalSpecialContractDocument finalContract = finalContractOpt.get();
          boolean clauseExists =
                  finalContract.getFinalClauses().stream()
                          .anyMatch(clause -> clause.getOrder().equals(clauseOrder));

          if (!clauseExists) {
              throw new IllegalArgumentException("해당 특약 조항을 찾을 수 없습니다: " + clauseOrder);
          }

          String redisKey = "final-contract:deletion:" + contractChatId + ":" + ownerId;

          String existingRequest = stringRedisTemplate.opsForValue().get(redisKey);
          if (existingRequest != null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_ALREADY_EXISTS, "이미 삭제 요청이 진행 중입니다.");
          }

          stringRedisTemplate
                  .opsForValue()
                  .set(redisKey, clauseOrder.toString(), Duration.ofHours(24));

          String notificationMessage = String.format("임대인이 특약 %d번 삭제를 요청했습니다.", clauseOrder);

          AiMessageBtn(contractChatId, notificationMessage);

          log.info("삭제 요청 Redis 저장 완료 - key: {}, value: {}", redisKey, ownerId);
      }

      @Override
      @Transactional
      public Map<String, Object> acceptFinalContractDeletion(
              Long contractChatId, Long buyerId, Integer clauseOrder) {
          log.info("=== 최종 특약 삭제 수락 처리 시작 ===");
          log.info(
                  "contractChatId: {}, buyerId: {}, clauseOrder: {}",
                  contractChatId,
                  buyerId,
                  clauseOrder);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!buyerId.equals(contractChat.getBuyerId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임차인만 응답할 수 있습니다.");
          }

          String redisKey = "final-contract:deletion:" + contractChatId + ":" + clauseOrder;
          String storedOwnerId = stringRedisTemplate.opsForValue().get(redisKey);

          if (storedOwnerId == null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_NOT_FOUND, "삭제 요청이 존재하지 않습니다.");
          }

          if (!storedOwnerId.equals(contractChat.getOwnerId().toString())) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_INVALID, "삭제 요청 정보가 유효하지 않습니다.");
          }

          FinalSpecialContractDocument finalContract =
                  specialContractMongoRepository
                          .findFinalContractByContractChatId(contractChatId)
                          .orElseThrow(() -> new IllegalArgumentException("최종 특약서를 찾을 수 없습니다."));

          List<FinalSpecialContractDocument.FinalClause> updatedClauses =
                  finalContract.getFinalClauses().stream()
                          .filter(clause -> !clause.getOrder().equals(clauseOrder))
                          .collect(Collectors.toList());

          finalContract.setFinalClauses(updatedClauses);
          finalContract.setTotalFinalClauses(updatedClauses.size());

          specialContractMongoRepository.saveFinalSpecialContract(finalContract);

          stringRedisTemplate.delete(redisKey);

          String confirmationMessage =
                  String.format("임차인이 특약 %d번 삭제 요청을 수락했습니다. 특약이 삭제되었습니다.", clauseOrder);

          AiMessage(contractChatId, confirmationMessage);

          log.info("특약 {}번 삭제 완료 - contractChatId: {}", clauseOrder, contractChatId);

          return Map.of(
                  "message",
                  "특약이 삭제되었습니다.",
                  "deletedClauseOrder",
                  clauseOrder,
                  "finalContractId",
                  finalContract.getId(),
                  "remainingClauses",
                  finalContract.getTotalFinalClauses());
      }

      @Override
      @Transactional
      public void rejectFinalContractDeletion(
              Long contractChatId, Long buyerId, Integer clauseOrder) {
          log.info("=== 최종 특약 삭제 거절 처리 시작 ===");
          log.info(
                  "contractChatId: {}, buyerId: {}, clauseOrder: {}",
                  contractChatId,
                  buyerId,
                  clauseOrder);

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new IllegalArgumentException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!buyerId.equals(contractChat.getBuyerId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임차인만 응답할 수 있습니다.");
          }

          String redisKey = "final-contract:deletion:" + contractChatId + ":" + clauseOrder;
          String storedOwnerId = stringRedisTemplate.opsForValue().get(redisKey);

          if (storedOwnerId == null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_NOT_FOUND, "삭제 요청이 존재하지 않습니다.");
          }

          stringRedisTemplate.delete(redisKey);

          String rejectionMessage =
                  String.format("임차인이 특약 %d번 삭제 요청을 거절했습니다. 기존 특약이 유지됩니다.", clauseOrder);

          AiMessage(contractChatId, rejectionMessage);

          log.info("특약 {}번 삭제 거절 완료 - contractChatId: {}", clauseOrder, contractChatId);
      }

      public String getContractChatRoomUrl(Long chatRoomId) {
          ChatRoom chatRoom = chatRoomMapper.findById(chatRoomId);
          if (chatRoom == null) {
              log.error("채팅방을 찾을 수 없음: {}", chatRoomId);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }
          ContractChat contractChatId =
                  contractChatMapper.findByUserAndHome(
                          chatRoom.getOwnerId(), chatRoom.getBuyerId(), chatRoom.getHomeId());
          if (contractChatId == null) {
              log.error("채팅방을 찾을 수 없음: {}", chatRoomId);
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
          }
          Long contractChatRoomId = contractChatId.getContractChatId();
          String param = getContractChatStatus(contractChatId.getStatus());
          if (contractChatId.getStatus() == ContractChat.ContractStatus.COMPLETE) {
              return baseUrl + contractChatUrl + "complete/" + (contractChatRoomId.toString());
          } else {
              return baseUrl + contractChatUrl + contractChatRoomId.toString() + param;
          }
      }

      private void broadcastPresence(Long contractChatId) {
          ContractChat c = contractChatMapper.findByContractChatId(contractChatId);
          if (c == null) return;

          boolean ownerIn = isUserInContractChatRoom(c.getOwnerId(), contractChatId);
          boolean buyerIn = isUserInContractChatRoom(c.getBuyerId(), contractChatId);
          boolean both = ownerIn && buyerIn;

          Map<String, Object> payload =
                  Map.of(
                          "type", "PRESENCE",
                          "ownerInContractRoom", ownerIn,
                          "buyerInContractRoom", buyerIn,
                          "bothInRoom", both,
                          "canChat", both,
                          "ownerId", c.getOwnerId(),
                          "buyerId", c.getBuyerId());
          messagingTemplate.convertAndSend("/topic/contract-chat/" + contractChatId, payload);
      }

      @Override
      public void requestFinalContract(Long contractChatId, Long ownerId) {
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          if (!ownerId.equals(contractChat.getOwnerId())) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          Optional<FinalSpecialContractDocument> finalContractOpt =
                  specialContractMongoRepository.findFinalContractByContractChatId(contractChatId);

          if (finalContractOpt.isEmpty()) {
              throw new IllegalArgumentException("최종 특약서가 생성되지 않았습니다.");
          }

          AiMessageBtn(contractChatId, "임대인이 최종 계약서 확인을 요청하였습니다");

          String key = "final-contract:request:" + contractChatId;
          String existingValue = stringRedisTemplate.opsForValue().get(key);
          if (existingValue != null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_ALREADY_EXISTS, "이미 확정 요청이 진행 중입니다.");
          }
          String value = ownerId.toString();
          stringRedisTemplate.opsForValue().set(key, value);
      }

      @Override
      public Map<String, Object> acceptFinalContract(
              Long contractChatId, Long buyerId, Boolean isAccepted) {
          if (!isUserInContractChat(contractChatId, buyerId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
          }

          Long ownerId = contractChat.getOwnerId();

          if (!buyerId.equals(contractChat.getBuyerId())) {
              throw new BusinessException(
                      ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임차인만 확정 수락을 할 수 있습니다.");
          }

          String redisKey = "final-contract:request:" + contractChatId;
          String storedOwnerId = stringRedisTemplate.opsForValue().get(redisKey);

          if (storedOwnerId == null) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_NOT_FOUND, "확정 요청이 존재하지 않습니다.");
          }

          if (!storedOwnerId.equals(ownerId.toString())) {
              throw new BusinessException(
                      ChatErrorCode.CONTRACT_END_REQUEST_INVALID, "확정 요청 정보가 유효하지 않습니다.");
          }

          stringRedisTemplate.delete(redisKey);

          if (isAccepted) {
              contractMongoRepository.clearSpecialContracts(contractChatId);
              contractMongoRepository.saveSpecialContract(contractChatId);
              contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.COMPLETE);
              AiMessage(contractChatId, "임차인이 최종 계약서를 수락했습니다! 계약서 서명하러 갈께요!");
          } else {
              AiMessage(contractChatId, "임차인이 최종 계약서를 거절했습니다. 추가 협상이 필요합니다.");
          }

          return Map.of("accepted", isAccepted);
      }

      //      private void broadcastPresence(Long contractChatId) {
      //          ContractChat c = contractChatMapper.findByContractChatId(contractChatId);
      //          if (c == null) return;
      //
      //          boolean ownerIn = isUserInContractChatRoom(c.getOwnerId(), contractChatId);
      //          boolean buyerIn = isUserInContractChatRoom(c.getBuyerId(), contractChatId);
      //          boolean both = ownerIn && buyerIn;
      //
      //          Map<String, Object> payload =
      //                  Map.of(
      //                          "type", "PRESENCE",
      //                          "ownerInContractRoom", ownerIn,
      //                          "buyerInContractRoom", buyerIn,
      //                          "bothInRoom", both,
      //                          "canChat", both,
      //                          "ownerId", c.getOwnerId(),
      //                          "buyerId", c.getBuyerId());
      //          messagingTemplate.convertAndSend("/topic/contract-chat/" + contractChatId, payload);
      //      }

      //      @Override
      //      public void requestFinalContract(Long contractChatId, Long ownerId) {
      //          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
      //          if (contractChat == null) {
      //              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
      //          }
      //
      //          if (!ownerId.equals(contractChat.getOwnerId())) {
      //              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
      //          }
      //
      //          Optional<FinalSpecialContractDocument> finalContractOpt =
      //
      // specialContractMongoRepository.findFinalContractByContractChatId(contractChatId);
      //
      //          if (finalContractOpt.isEmpty()) {
      //              throw new IllegalArgumentException("최종 특약서가 생성되지 않았습니다.");
      //          }
      //
      //          AiMessageBtn(contractChatId, "임대인이 최종 계약서 확인을 요청하였습니다");
      //
      //          String key = "final-contract:request:" + contractChatId;
      //          String existingValue = stringRedisTemplate.opsForValue().get(key);
      //          if (existingValue != null) {
      //              throw new BusinessException(
      //                      ChatErrorCode.CONTRACT_END_REQUEST_ALREADY_EXISTS, "이미 확정 요청이 진행
      // 중입니다.");
      //          }
      //          String value = ownerId.toString();
      //          stringRedisTemplate.opsForValue().set(key, value);
      //      }

      //      @Override
      //      public Map<String, Object> acceptFinalContract(
      //              Long contractChatId, Long buyerId, Boolean isAccepted) {
      //          if (!isUserInContractChat(contractChatId, buyerId)) {
      //              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
      //          }
      //
      //          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
      //          if (contractChat == null) {
      //              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다: " + contractChatId);
      //          }
      //
      //          Long ownerId = contractChat.getOwnerId();
      //
      //          if (!buyerId.equals(contractChat.getBuyerId())) {
      //              throw new BusinessException(
      //                      ChatErrorCode.CHAT_ROOM_ACCESS_DENIED, "임차인만 확정 수락을 할 수 있습니다.");
      //          }
      //
      //          String redisKey = "final-contract:request:" + contractChatId;
      //          String storedOwnerId = stringRedisTemplate.opsForValue().get(redisKey);
      //
      //          if (storedOwnerId == null) {
      //              throw new BusinessException(
      //                      ChatErrorCode.CONTRACT_END_REQUEST_NOT_FOUND, "확정 요청이 존재하지 않습니다.");
      //          }
      //
      //          if (!storedOwnerId.equals(ownerId.toString())) {
      //              throw new BusinessException(
      //                      ChatErrorCode.CONTRACT_END_REQUEST_INVALID, "확정 요청 정보가 유효하지 않습니다.");
      //          }
      //
      //          stringRedisTemplate.delete(redisKey);
      //
      //          if (isAccepted) {
      //              contractMongoRepository.clearSpecialContracts(contractChatId);
      //              contractMongoRepository.saveSpecialContract(contractChatId);
      //              AiMessage(contractChatId, "임차인이 최종 계약서를 수락했습니다! 계약서 서명하러 갈께요!");
      //          } else {
      //              AiMessage(contractChatId, "임차인이 최종 계약서를 거절했습니다. 추가 협상이 필요합니다.");
      //          }
      //
      //          return Map.of("accepted", isAccepted);
      //      }
}
