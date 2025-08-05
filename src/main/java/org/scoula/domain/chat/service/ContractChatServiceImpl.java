package org.scoula.domain.chat.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.scoula.domain.chat.document.ContractChatDocument;
import org.scoula.domain.chat.document.SpecialContractDocument;
import org.scoula.domain.chat.document.SpecialContractFixDocument;
import org.scoula.domain.chat.document.SpecialContractSelectionDocument;
import org.scoula.domain.chat.dto.ContentDataDto;
import org.scoula.domain.chat.dto.ContractChatMessageRequestDto;
import org.scoula.domain.chat.dto.SpecialContractDto;
import org.scoula.domain.chat.dto.SpecialContractUserViewDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.mapper.ChatRoomMapper;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.repository.ContractChatMessageRepository;
import org.scoula.domain.chat.repository.SpecialContractMongoRepository;
import org.scoula.domain.chat.vo.ChatRoom;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
          if (dto.getContractChatId() == null
                  || dto.getSenderId() == null
                  || dto.getReceiverId() == null
                  || dto.getContent() == null) {
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
      public String setEndPointAndExport(Long contractChatId, Long userId, Long order) {
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
          } else {
              sb.append("조회된 특약 메시지가 없습니다.");
          }

          stringRedisTemplate.delete(redisKey);
          contractChatMapper.clearTimePoints(contractChatId);

          String result = sb.toString();

          updateRecentData(contractChatId, order, result);

          return result;
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

      // 특약 관련 메서드
      //      @Override
      //      @Transactional
      //      public Object submitUserSelection(
      //              Long contractChatId, Long userId, Map<Integer, Boolean> selections) {
      //          // 사용자 역할 확인 (임대인/임차인)
      //          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
      //          if (contractChat == null) {
      //              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다.");
      //          }
      //
      //          boolean isOwner = userId.equals(contractChat.getOwnerId());
      //          boolean isTenant = userId.equals(contractChat.getBuyerId());
      //
      //          if (!isOwner && !isTenant) {
      //              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
      //          }
      //
      //          // 기존 선택 상태 조회 또는 새 문서 생성
      //          Optional<SpecialContractSelectionDocument> existingOpt =
      //
      // specialContractMongoRepository.findSelectionByContractChatId(contractChatId);
      //
      //          SpecialContractSelectionDocument document;
      //          if (existingOpt.isPresent()) {
      //              document = existingOpt.get();
      //          } else {
      //              document =
      //                      SpecialContractSelectionDocument.builder()
      //                              .contractChatId(contractChatId)
      //                              .ownerSelections(new HashMap<>())
      //                              .tenantSelections(new HashMap<>())
      //                              .ownerCompleted(false)
      //                              .tenantCompleted(false)
      //                              .processed(false)
      //                              .build();
      //          }
      //
      //          // 사용자 역할에 따라 선택 저장
      //          if (isOwner) {
      //              document.setOwnerSelections(selections);
      //              document.setOwnerCompleted(true);
      //              log.info("임대인 선택 저장 완료: contractChatId={}", contractChatId);
      //          } else {
      //              document.setTenantSelections(selections);
      //              document.setTenantCompleted(true);
      //              log.info("임차인 선택 저장 완료: contractChatId={}", contractChatId);
      //          }
      //
      //          // MongoDB에 저장
      //          specialContractMongoRepository.saveSelectionStatus(document);
      //
      //          // 양쪽 모두 완료되었는지 확인
      //          if (!document.isOwnerCompleted() || !document.isTenantCompleted()) {
      //              String waitingFor = isOwner ? "임차인" : "임대인";
      //              return Map.of(
      //                      "message",
      //                      "선택이 저장되었습니다. " + waitingFor + "의 선택을 기다리는 중입니다.",
      //                      "yourRole",
      //                      isOwner ? "owner" : "tenant",
      //                      "completed",
      //                      false);
      //          }
      //
      //          // 이미 처리되었는지 확인
      //          if (document.isProcessed()) {
      //              return Map.of(
      //                      "message",
      //                      "이미 처리된 선택입니다.",
      //                      "yourRole",
      //                      isOwner ? "owner" : "tenant",
      //                      "completed",
      //                      true,
      //                      "createdOrders",
      //                      List.of());
      //          }
      //
      //          // 양쪽 선택이 모두 완료되었으면 특약 문서 생성 진행
      //          List<Long> rejectedOrders =
      //                  findRejectedOrders(document.getOwnerSelections(),
      // document.getTenantSelections());
      //
      //          // ✅ 수정: 조건을 명확하게 분리
      //          if (rejectedOrders.isEmpty()) {
      //              // 모든 특약에 동의 - 협상 불필요
      //              document.setProcessed(true);
      //              specialContractMongoRepository.saveSelectionStatus(document);
      //
      //              return Map.of(
      //                      "message",
      //                      "모든 특약에 동의하셨습니다. 별도 협상이 필요하지 않습니다.",
      //                      "yourRole",
      //                      isOwner ? "owner" : "tenant",
      //                      "completed",
      //                      true,
      //                      "createdOrders",
      //                      rejectedOrders);
      //          } else {
      //              // ✅ 수정: 특약이 있는 경우에만 상태 변경 및 문서 생성
      //              // MySQL 상태를 ROUND0으로 변경
      //              contractChatMapper.updateStatus(contractChatId,
      // ContractChat.ContractStatus.ROUND0);
      //
      //              // N이 선택된 특약들에 대해 빈 문서 생성
      //              List<Long> createdOrders = new ArrayList<>();
      //              for (Long order : rejectedOrders) {
      //                  try {
      //                      createSpecialContract(contractChatId, order);
      //                      createdOrders.add(order);
      //                      log.info("특약 빈 문서 생성 완료: contractChatId={}, order={}", contractChatId,
      // order);
      //                  } catch (IllegalArgumentException e) {
      //                      log.warn("특약이 이미 존재합니다: contractChatId={}, order={}", contractChatId,
      // order);
      //                  }
      //              }
      //
      //              // 처리 완료 상태로 업데이트
      //              document.setProcessed(true);
      //              specialContractMongoRepository.saveSelectionStatus(document);
      //
      //              log.info(
      //                      "특약 빈 문서 자동 생성 완료: contractChatId={}, 생성된 특약={}",
      //                      contractChatId,
      //                      createdOrders);
      //
      //              return Map.of(
      //                      "message",
      //                      createdOrders.size() + "개의 특약 협상이 시작됩니다.",
      //                      "yourRole",
      //                      isOwner ? "owner" : "tenant",
      //                      "completed",
      //                      true,
      //                      "createdOrders",
      //                      createdOrders,
      //                      "rejectedOrders",
      //                      rejectedOrders);
      //          }
      //      }

      @Override
      @Transactional
      public Object submitUserSelection(
              Long contractChatId, Long userId, Map<Integer, Boolean> selections) {
          // 사용자 역할 확인 (임대인/임차인)
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          if (contractChat == null) {
              throw new EntityNotFoundException("계약 채팅방을 찾을 수 없습니다.");
          }

          boolean isOwner = userId.equals(contractChat.getOwnerId());
          boolean isTenant = userId.equals(contractChat.getBuyerId());

          if (!isOwner && !isTenant) {
              throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
          }

          // 기존 선택 상태 조회 또는 새 문서 생성
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

          // 사용자 역할에 따라 선택 저장
          if (isOwner) {
              document.setOwnerSelections(selections);
              document.setOwnerCompleted(true);
              log.info("임대인 선택 저장 완료: contractChatId={}", contractChatId);
          } else {
              document.setTenantSelections(selections);
              document.setTenantCompleted(true);
              log.info("임차인 선택 저장 완료: contractChatId={}", contractChatId);
          }

          // MongoDB에 저장
          specialContractMongoRepository.saveSelectionStatus(document);

          // 양쪽 모두 완료되었는지 확인
          if (!document.isOwnerCompleted() || !document.isTenantCompleted()) {
              String waitingFor = isOwner ? "임차인" : "임대인";
              return Map.of(
                      "message",
                      "선택이 저장되었습니다. " + waitingFor + "의 선택을 기다리는 중입니다.",
                      "yourRole",
                      isOwner ? "owner" : "tenant",
                      "completed",
                      false);
          }

          // 이미 처리되었는지 확인
          if (document.isProcessed()) {
              return Map.of(
                      "message",
                      "이미 처리된 선택입니다.",
                      "yourRole",
                      isOwner ? "owner" : "tenant",
                      "completed",
                      true,
                      "createdOrders",
                      List.of());
          }

          // 양쪽 선택이 모두 완료되었으면 특약 문서 생성 진행
          List<Long> rejectedOrders =
                  findRejectedOrders(document.getOwnerSelections(), document.getTenantSelections());

          if (rejectedOrders.isEmpty()) {
              // 처리 완료 상태로 업데이트
              document.setProcessed(true);
              specialContractMongoRepository.saveSelectionStatus(document);

              return Map.of(
                      "message",
                      "모든 특약에 동의하셨습니다. 별도 협상이 필요하지 않습니다.",
                      "yourRole",
                      isOwner ? "owner" : "tenant",
                      "completed",
                      true,
                      "createdOrders",
                      rejectedOrders);
          }

          // N이 선택된 특약들에 대해 빈 문서 생성
          List<Long> createdOrders = new ArrayList<>();
          for (Long order : rejectedOrders) {
              try {
                  createSpecialContract(contractChatId, order);
                  createdOrders.add(order);
                  log.info("특약 빈 문서 생성 완료: contractChatId={}, order={}", contractChatId, order);
              } catch (IllegalArgumentException e) {
                  log.warn("특약이 이미 존재합니다: contractChatId={}, order={}", contractChatId, order);
              }
          }

          // 처리 완료 상태로 업데이트
          contractChatMapper.updateStatus(contractChatId, ContractChat.ContractStatus.ROUND0);
          document.setProcessed(true);
          specialContractMongoRepository.saveSelectionStatus(document);

          log.info("특약 빈 문서 자동 생성 완료: contractChatId={}, 생성된 특약={}", contractChatId, createdOrders);

          return Map.of(
                  "message",
                  "특약 협상이 시작됩니다.",
                  "yourRole",
                  isOwner ? "owner" : "tenant",
                  "completed",
                  true,
                  "createdOrders",
                  createdOrders,
                  "rejectedOrders",
                  rejectedOrders);
      }

      //    @Override
      //    public List<Long> createSpecialContractsFromSelections(SpecialContractSelectionDto
      // selectionDto, Long userId) {
      //        Long contractChatId = selectionDto.getContractChatId();
      //
      //        if (!isUserInContractChat(contractChatId, userId)) {
      //            throw new BusinessException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
      //        }
      //
      //        Map<Integer, Boolean> ownerSelections = selectionDto.getOwnerSelections();
      //        Map<Integer, Boolean> tenantSelections = selectionDto.getTenantSelections();
      //
      //        List<Long> rejectedOrders = new ArrayList<>();
      //
      //        for (int order = 1; order <= 6; order++) {
      //            Boolean ownerChoice = ownerSelections.get(order);
      //            Boolean tenantChoice = tenantSelections.get(order);
      //
      //            if (Boolean.FALSE.equals(ownerChoice) || Boolean.FALSE.equals(tenantChoice)) {
      //                rejectedOrders.add((long) order);
      //            }
      //        }
      //
      //        List<Long> createdOrders = new ArrayList<>();
      //
      //        for (Long order : rejectedOrders) {
      //            try {
      //                createSpecialContract(contractChatId, order);
      //                createdOrders.add(order);
      //                log.info("특약 문서 생성 완료: contractChatId={}, order={}", contractChatId, order);
      //            } catch (IllegalArgumentException e) {
      //                // 이미 존재하는 경우 스킵
      //                log.warn("특약이 이미 존재합니다: contractChatId={}, order={}", contractChatId, order);
      //            }
      //        }
      //
      //        log.info("특약 선택 처리 완료: contractChatId={}, 생성된 특약={}", contractChatId, createdOrders);
      //        return createdOrders;
      //    }
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
      public SpecialContractUserViewDto getSpecialContractForUser(Long contractChatId, Long userId) {
          log.info("=== 사용자별 특약 문서 조회 시작 ===");
          log.info("contractChatId: {}, userId: {}", contractChatId, userId);

          SpecialContractDocument document =
                  specialContractMongoRepository
                          .findSpecialContractDocumentByContractChatId(contractChatId)
                          .orElseThrow(
                                  () -> {
                                      log.warn("특약 문서를 찾을 수 없음 - contractChatId: {}", contractChatId);
                                      return new IllegalArgumentException(
                                              "해당 특약 문서를 찾을 수 없습니다: " + contractChatId);
                                  });

          log.info(
                  "특약 문서 조회 완료 - round: {}, totalClauses: {}",
                  document.getRound(),
                  document.getTotalClauses());

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

          String userRole = isOwner ? "owner" : "tenant";
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

          log.info("사용자별 특약 문서 조회 완료 - userRole: {}, clauses: {}", userRole, userClauses.size());
          return result;
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
      public SpecialContractFixDocument proceedToNextRound(
              Long contractChatId, Long order, int prevDataIndex) {
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

          if (prevDataIndex < 0 || prevDataIndex >= document.getPrevData().size()) {
              throw new IllegalArgumentException(
                      "유효하지 않은 prevData 인덱스입니다: " + prevDataIndex + " (0-1 사이여야 함)");
          }

          // 현재 recentData를 prevData의 지정된 인덱스에 저장
          ContentDataDto prevDataToStore =
                  ContentDataDto.builder()
                          .title(document.getRecentData().getTitle())
                          .content(document.getRecentData().getContent())
                          .messages(document.getRecentData().getMessages())
                          .build();

          List<ContentDataDto> updatedPrevData = new ArrayList<>(document.getPrevData());
          updatedPrevData.set(prevDataIndex, prevDataToStore);

          // 라운드 증가 및 recentData 초기화 (모든 필드를 빈 상태로)
          document.setPrevData(updatedPrevData);
          document.setRecentData(createEmptyContentData()); // 완전히 빈 상태로 초기화
          contractChatMapper.proceedToNextRound(contractChatId);
          ContractChat updatedContractChat = contractChatMapper.findByContractChatId(contractChatId);
          Long newRound = updatedContractChat.getCurrentRound();

          // 새 라운드의 특약 내용 가져오기
          SpecialContractDocument newRoundContract =
                  specialContractMongoRepository
                          .findSpecialContractDocumentByContractChatIdAndRound(
                                  contractChatId, newRound)
                          .orElseThrow(() -> new IllegalArgumentException("새 라운드의 특약 문서를 찾을 수 없습니다"));

          SpecialContractDocument.Clause newClause =
                  newRoundContract.getClauses().stream()
                          .filter(clause -> clause.getOrder().equals(order.intValue()))
                          .findFirst()
                          .orElseThrow(() -> new IllegalArgumentException("새 라운드의 특약 조항을 찾을 수 없습니다"));

          ContentDataDto newRecentData =
                  ContentDataDto.builder()
                          .title(newClause.getTitle())
                          .content(newClause.getContent())
                          .messages("")
                          .build();

          document.setRecentData(newRecentData);

          return specialContractMongoRepository.updateSpecialContract(document);
      }

      @Override
      public SpecialContractFixDocument proceedToNextRoundAuto(Long contractChatId, Long order) {
          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          Long currentRound = contractChat.getCurrentRound();

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

          int targetIndex = (int) (currentRound - 1);

          if (targetIndex >= 2) {
              throw new IllegalStateException(
                      "최대 2개의 이전 데이터만 저장할 수 있습니다. 현재 라운드: " + document.getRound());
          }

          return proceedToNextRound(contractChatId, order, targetIndex);
      }

      @Override
      public SpecialContractDto formatForAI(Long contractChatId, Long order) {
          SpecialContractFixDocument document =
                  specialContractMongoRepository
                          .findByContractChatIdAndOrder(contractChatId, order)
                          .orElseThrow(() -> new IllegalArgumentException("해당 특약 문서를 찾을 수 없습니다."));

          ContractChat contractChat = contractChatMapper.findByContractChatId(contractChatId);
          Long currentRound = contractChat.getCurrentRound();
          return SpecialContractDto.builder()
                  .contractChatId(document.getContractChatId())
                  .order(document.getOrder())
                  .round(currentRound)
                  .prevData(document.getPrevData())
                  .recentData(document.getRecentData())
                  .build();
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
      public void deleteSpecialContract(Long contractChatId) {
          SpecialContractFixDocument document = findSpecialContract(contractChatId);
          specialContractMongoRepository.deleteSpecialContract(document);
      }

      @Override
      public boolean existsSpecialContract(Long contractChatId) {
          return specialContractMongoRepository.existsByContractChatId(contractChatId);
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

      @Override
      public String saveOwnerSelectionToRedis(Long contractChatId, Map<Integer, Boolean> selections) {
          String redisKey = "special-contract:owner:" + contractChatId;

          try {
              // Map을 JSON 문자열로 변환하여 Redis에 저장
              ObjectMapper objectMapper = new ObjectMapper();
              String selectionsJson = objectMapper.writeValueAsString(selections);

              // 24시간 TTL 설정
              stringRedisTemplate
                      .opsForValue()
                      .set(redisKey, selectionsJson, java.time.Duration.ofHours(24));

              log.info("임대인 선택 Redis 저장 완료: contractChatId={}", contractChatId);

              return "임대인 선택이 저장되었습니다. 임차인의 선택을 기다리는 중입니다.";

          } catch (Exception e) {
              log.error("임대인 선택 Redis 저장 실패: contractChatId={}", contractChatId, e);
              throw new RuntimeException("Redis 저장 실패", e);
          }
      }

      @Override
      public Object saveTenantSelectionAndProcess(
              Long contractChatId, Map<Integer, Boolean> selections) {
          String ownerRedisKey = "special-contract:owner:" + contractChatId;
          String tenantRedisKey = "special-contract:tenant:" + contractChatId;

          try {
              // 임차인 선택을 Redis에 저장
              ObjectMapper objectMapper = new ObjectMapper();
              String selectionsJson = objectMapper.writeValueAsString(selections);
              stringRedisTemplate
                      .opsForValue()
                      .set(tenantRedisKey, selectionsJson, java.time.Duration.ofHours(24));

              log.info("임차인 선택 Redis 저장 완료: contractChatId={}", contractChatId);

              // 임대인 선택이 있는지 확인
              String ownerSelectionsJson = stringRedisTemplate.opsForValue().get(ownerRedisKey);

              if (ownerSelectionsJson == null) {
                  return "임차인 선택이 저장되었습니다. 임대인의 선택을 기다리는 중입니다.";
              }

              // 양쪽 선택이 모두 있으면 특약 문서 생성 진행
              Map<Integer, Boolean> ownerSelections =
                      objectMapper.readValue(
                              ownerSelectionsJson, new TypeReference<Map<Integer, Boolean>>() {});

              // N이 선택된 특약들 찾기
              List<Long> rejectedOrders = findRejectedOrders(ownerSelections, selections);

              if (rejectedOrders.isEmpty()) {
                  // Redis 정리
                  stringRedisTemplate.delete(ownerRedisKey);
                  stringRedisTemplate.delete(tenantRedisKey);

                  return Map.of(
                          "message",
                          "모든 특약에 동의하셨습니다. 별도 협상이 필요하지 않습니다.",
                          "createdOrders",
                          rejectedOrders);
              }

              // N이 선택된 특약들에 대해 문서 생성
              List<Long> createdOrders = new ArrayList<>();
              for (Long order : rejectedOrders) {
                  try {
                      createSpecialContract(contractChatId, order);
                      createdOrders.add(order);
                      log.info("특약 문서 생성 완료: contractChatId={}, order={}", contractChatId, order);
                  } catch (IllegalArgumentException e) {
                      log.warn("특약이 이미 존재합니다: contractChatId={}, order={}", contractChatId, order);
                  }
              }

              // Redis 정리
              stringRedisTemplate.delete(ownerRedisKey);
              stringRedisTemplate.delete(tenantRedisKey);

              log.info("특약 자동 생성 완료: contractChatId={}, 생성된 특약={}", contractChatId, createdOrders);

              return Map.of(
                      "message",
                      createdOrders.size() + "개의 특약 협상이 시작됩니다.",
                      "createdOrders",
                      createdOrders);

          } catch (Exception e) {
              log.error("임차인 선택 처리 실패: contractChatId={}", contractChatId, e);
              throw new RuntimeException("선택 처리 실패", e);
          }
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
}
