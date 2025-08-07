package org.scoula.domain.chat.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.scoula.domain.chat.document.*;
import org.scoula.domain.chat.dto.ContentDataDto;
import org.scoula.domain.chat.dto.ContractChatMessageRequestDto;
import org.scoula.domain.chat.dto.SpecialContractUserViewDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveRequestDto;
import org.scoula.domain.chat.dto.ai.ClauseImproveResponseDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.mapper.ChatRoomMapper;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.repository.ContractChatMessageRepository;
import org.scoula.domain.chat.repository.SpecialContractMongoRepository;
import org.scoula.domain.chat.vo.ChatRoom;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.domain.precontract.service.PreContractDataService;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractChatServiceImpl implements ContractChatServiceInterface {

      private final ContractChatMapper contractChatMapper;
      private final ChatRoomMapper chatRoomMapper;
      private final ContractChatMessageRepository contractChatMessageRepository;
      private final SimpMessagingTemplate messagingTemplate;
      @Lazy private final ChatServiceInterface chatService;
      private final AiClauseImproveService aiClauseImproveService;
      private final PreContractDataService preContractDataService;

      private final Map<String, Set<Long>> contractChatOnlineUsers = new ConcurrentHashMap<>();
      private final RedisTemplate<String, String> stringRedisTemplate;
      @Autowired private SpecialContractMongoRepository specialContractMongoRepository;

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
          final Long ai = Long.MAX_VALUE - 1;

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
          if (!isUserInContractChat(contractChatId, userId)) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          setContractChatUserOnline(userId, contractChatId);
      }

      /** {@inheritDoc} */
      @Override
      @Transactional
      public void leaveContractChatRoom(Long contractChatId, Long userId) {
          setContractChatUserOffline(userId, contractChatId);
      }

      /** {@inheritDoc} */
      @Override
      public Map<String, Object> getContractChatOnlineStatus(Long contractChatId, Long userId) {
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

          return Map.of(
                  "ownerInContractRoom", ownerInContractRoom,
                  "buyerInContractRoom", buyerInContractRoom,
                  "bothInRoom", bothInRoom,
                  "canChat", bothInRoom,
                  "ownerId", contractChat.getOwnerId(),
                  "buyerId", contractChat.getBuyerId());
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
          String key = "contract-chat-" + contractChatId;
          contractChatOnlineUsers
                  .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                  .add(userId);
      }

      /** {@inheritDoc} */
      private void setContractChatUserOffline(Long userId, Long contractChatId) {
          String key = "contract-chat-" + contractChatId;
          Set<Long> users = contractChatOnlineUsers.get(key);
          if (users != null) {
              users.remove(userId);
              if (users.isEmpty()) {
                  contractChatOnlineUsers.remove(key);
              }
          }
      }

      /** {@inheritDoc} */
      private boolean isUserInContractChatRoom(Long userId, Long contractChatId) {
          String key = "contract-chat-" + contractChatId;
          Set<Long> users = contractChatOnlineUsers.get(key);
          boolean isOnline = users != null && users.contains(userId);
          return isOnline;
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

          SpecialContractDocument latestDocument =
                  specialContractMongoRepository
                          .findSpecialContractDocumentByContractChatId(contractChatId)
                          .orElseThrow(() -> new IllegalArgumentException("기존 특약 문서를 찾을 수 없습니다"));

          Long newRound = latestDocument.getRound() + 1;
          log.info("새 라운드: {}", newRound);

          List<SpecialContractDocument.Clause> newClauses = new ArrayList<>();

          for (Long passedOrder : passedOrders) {
              latestDocument.getClauses().stream()
                      .filter(clause -> clause.getOrder().equals(passedOrder.intValue()))
                      .findFirst()
                      .ifPresent(
                              clause -> {
                                  SpecialContractDocument.Clause copiedClause =
                                          SpecialContractDocument.Clause.builder()
                                                  .order(clause.getOrder())
                                                  .title(clause.getTitle())
                                                  .content(clause.getContent())
                                                  .assessment(
                                                          SpecialContractDocument.Assessment.builder()
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
                                  newClauses.add(copiedClause);
                                  log.info("통과된 특약 {}번 내용 복사 완료", passedOrder);
                              });
          }

          for (Long rejectedOrder : rejectedOrders) {
              SpecialContractDocument.Clause emptyClause =
                      SpecialContractDocument.Clause.builder()
                              .order(rejectedOrder.intValue())
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
              log.info("거부된 특약 {}번 빈 껍데기 생성 완료", rejectedOrder);
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
          log.info("통과된 특약: {}, 거부된 특약: {}", passedOrders, rejectedOrders);
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
                  int targetIndex = (int) (currentRound - 1);

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

                  SpecialContractFixDocument updated =
                          specialContractMongoRepository.updateSpecialContract(document);
                  updatedContracts.add(updated);

                  log.info("특약 {}번 라운드 진행 완료", document.getOrder());

              } catch (Exception e) {
                  log.error("특약 {}번 라운드 진행 실패: {}", document.getOrder(), e.getMessage());
              }
          }

          if (!updatedContracts.isEmpty()) {
              contractChatMapper.proceedToNextRound(contractChatId);
              log.info("MySQL 라운드 상태 업데이트 완료");
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
          log.info("현재 계약 상태: {}", currentStatus);

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
          AiMessage(contractChatId, "특약 대화가 시작됩니다!");
          return processRoundResults(contractChatId, document, currentStatus, isOwner);
      }

      /** 현재 상태에 따른 선택 가능한 특약들 반환 */
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

      /** 라운드별 결과 처리 (기존 로직 + 라운드 진행) */
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

              return Map.of(
                      "message", "특약 협상이 시작됩니다.", "completed", true, "createdOrders", createdOrders);
          } else {
              if (rejectedOrders.isEmpty()) {
                  try {
                      FinalSpecialContractDocument finalContract =
                              saveFinalSpecialContract(contractChatId);

                      AiMessage(contractChatId, "🎉 모든 특약 협상이 완료되었습니다! 최종 특약서가 생성되었습니다.");

                      return Map.of(
                              "message", "모든 특약이 완료되었습니다!",
                              "completed", true,
                              "finalContractId", finalContract.getId(),
                              "totalFinalClauses", finalContract.getTotalFinalClauses());
                  } catch (Exception e) {
                      log.error("최종 특약 저장 실패", e);
                      return Map.of("message", "특약은 완료되었지만 최종 저장 중 오류가 발생했습니다.", "completed", true);
                  }
              }

              try {
                  createNextRoundSpecialContractDocument(contractChatId, rejectedOrders, passedOrders);
              } catch (Exception e) {
                  log.error("새 라운드 SPECIAL_CONTRACT 문서 생성 실패", e);
              }

              resetSelectionDocument(contractChatId);

              return Map.of(
                      "message", "협상을 계속 진행해주세요.",
                      "completed", true);
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

          Map<String, SpecialContractUserViewDto> allRounds = new HashMap<>();
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
                          .findSpecialContractDocumentByContractChatId(contractChatId)
                          .orElseThrow(
                                  () ->
                                          new IllegalArgumentException(
                                                  "특약 원본 문서를 찾을 수 없습니다: " + contractChatId));

          if (!specialContract.getRound().equals(currentRound)) {
              throw new IllegalArgumentException(
                      "라운드가 일치하지 않습니다. 현재 라운드: "
                              + currentRound
                              + ", 특약 라운드: "
                              + specialContract.getRound());
          }

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

      /** 빈 ContentDataDto 생성 헬퍼 메서드 */
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
          List<SpecialContractFixDocument> incompleteContracts =
                  specialContractMongoRepository.findByContractChatIdAndIsPassed(
                          contractChatId, false);

          if (!incompleteContracts.isEmpty()) {
              throw new IllegalStateException(
                      "아직 완료되지 않은 특약이 " + incompleteContracts.size() + "개 있습니다.");
          }

          List<SpecialContractFixDocument> completedContracts =
                  specialContractMongoRepository.findByContractChatIdAndIsPassed(
                          contractChatId, true);

          if (completedContracts.isEmpty()) {
              throw new IllegalStateException("완료된 특약이 없습니다.");
          }

          List<FinalSpecialContractDocument.FinalClause> finalClauses = new ArrayList<>();

          for (SpecialContractFixDocument completedContract : completedContracts) {
              Long order = completedContract.getOrder();

              Optional<SpecialContractDocument> latestRoundDoc =
                      findLatestRoundForOrder(contractChatId, order);

              if (latestRoundDoc.isPresent()) {
                  SpecialContractDocument doc = latestRoundDoc.get();

                  doc.getClauses().stream()
                          .filter(clause -> clause.getOrder().equals(order.intValue()))
                          .findFirst()
                          .ifPresent(
                                  clause -> {
                                      FinalSpecialContractDocument.FinalClause finalClause =
                                              FinalSpecialContractDocument.FinalClause.builder()
                                                      .order(clause.getOrder())
                                                      .title(clause.getTitle())
                                                      .content(clause.getContent())
                                                      .sourceRound(doc.getRound())
                                                      .build();

                                      finalClauses.add(finalClause);
                                      log.info(
                                              "특약 {}번 최종 저장 완료 - sourceRound: {}",
                                              order,
                                              doc.getRound());
                                  });
              }
          }

          FinalSpecialContractDocument finalDocument =
                  FinalSpecialContractDocument.builder()
                          .contractChatId(contractChatId)
                          .totalFinalClauses(finalClauses.size())
                          .finalClauses(finalClauses)
                          .build();

          FinalSpecialContractDocument savedDocument =
                  specialContractMongoRepository.saveFinalSpecialContract(finalDocument);

          log.info("최종 특약 저장 완료 - 총 {}개 조항", finalClauses.size());

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
                specialContractMongoRepository.findByContractChatIdAndIsPassed(contractChatId, false);

        if (incompleteContracts.isEmpty()) {
            log.info("미완료 특약이 없어서 라운드 증가 체크 불필요");
            return;
        }

        Set<Integer> incompleteOrders = incompleteContracts.stream()
                .map(doc -> doc.getOrder().intValue())
                .collect(Collectors.toSet());

        log.info("미완료 특약 번호들: {}", incompleteOrders);

        boolean allIncompleteClausesAreFilled = incompleteOrders.stream()
                .allMatch(order -> isClauseFilled(document, order));

        log.info("모든 미완료 특약이 꽉 찼는지: {}", allIncompleteClausesAreFilled);

        if (allIncompleteClausesAreFilled) {
            ContractChat.ContractStatus nextStatus = getNextStatus(currentStatus);
            if (nextStatus != null) {
                contractChatMapper.updateStatus(contractChatId, nextStatus);
                log.info("라운드 자동 증가: {} → {}", currentStatus, nextStatus);
                String aimsg=getRoundIncrementMessage(nextStatus);
                AiMessage(contractChatId, aimsg);
            }
        } else {
            log.info("아직 모든 특약이 꽉 차지 않아서 라운드 유지");
        }
    }

    private boolean isClauseFilled(SpecialContractDocument document, Integer order) {
        return document.getClauses().stream()
                .filter(clause -> clause.getOrder().equals(order))
                .findFirst()
                .map(clause -> {
                    boolean isFilled = !clause.getTitle().isEmpty() && !clause.getContent().isEmpty();
                    log.debug("특약 {}번 채움 상태: title='{}', content='{...}', filled={}",
                            order, clause.getTitle(), isFilled);
                    return isFilled;
                })
                .orElse(false);
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
    private String getRoundIncrementMessage(ContractChat.ContractStatus status) {
        switch (status) {
            case ROUND1:
                return "1차 수정이 완료되었습니다! 1차 협상 라운드가 시작됩니다.";
            case ROUND2:
                return "2차 수정이 완료되었습니다! 2차 협상 라운드가 시작됩니다.";
            case ROUND3:
                return "3차 수정이 완료되었습니다! 최종 협상 라운드가 시작됩니다.";
            default:
                return "새로운 협상 라운드가 시작됩니다.";
        }
    }
}
