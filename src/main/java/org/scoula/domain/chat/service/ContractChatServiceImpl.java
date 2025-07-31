package org.scoula.domain.chat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.scoula.domain.chat.dto.ContractChatDocument;
import org.scoula.domain.chat.dto.ContractChatMessageRequestDto;
import org.scoula.domain.chat.exception.ChatErrorCode;
import org.scoula.domain.chat.mapper.ChatRoomMapper;
import org.scoula.domain.chat.mapper.ContractChatMapper;
import org.scoula.domain.chat.repository.ContractChatMessageRepository;
import org.scoula.domain.chat.vo.ChatRoom;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.EntityNotFoundException;
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

      // 🔧 계약 채팅 전용 온라인 상태 관리 (일반 채팅과 분리)
      private final Map<String, Set<Long>> contractChatOnlineUsers = new ConcurrentHashMap<>();
      private final RedisTemplate<String, String> stringRedisTemplate;

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
      public List<ContractChatDocument> getContractMessages(Long contractChatId, Long userId) {
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
      public String setEndPointAndExport(Long contractChatId, Long userId) {
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
          System.out.println("시작 시간: " + startTime);

          if (startTime == null || startTime.trim().isEmpty()) {
              throw new BusinessException(ChatErrorCode.START_POINT_NOT_SET);
          }

          String endTime = LocalDateTime.now().toString();
          System.out.println("종료 시간: " + endTime);

          contractChatMapper.updateEndTime(contractChatId, endTime);

          List<ContractChatDocument> exportMessages =
                  contractChatMessageRepository.getMessagesBetweenTime(
                          contractChatId, startTime, endTime);

          StringBuilder sb = new StringBuilder();

          if (exportMessages != null && !exportMessages.isEmpty()) {

              for (int i = 0; i < exportMessages.size(); i++) {

                  ContractChatDocument msg = exportMessages.get(i);
                  Long senderId = msg.getSenderId();
                  String content = msg.getContent();
                  String senderRole;
                  if (senderId.equals(buyerId)) {
                      senderRole = "구매자";
                  } else if (senderId.equals(ownerId)) {
                      senderRole = "판매자";
                  } else {
                      senderRole = "조회실패";
                  }
                  if (!content.equals("임대인이 특약 대화 종료 및 내보내기를 요청했습니다.")
                          && !content.equals("임차인이 특약 대화를 더 요청했습니다.")) {
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
          System.out.println(result);
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
}
