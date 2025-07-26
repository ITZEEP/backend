package org.scoula.domain.chat.service;

import java.util.List;
import java.util.Map;

import org.scoula.domain.chat.dto.ChatMessageDocument;
import org.scoula.domain.chat.dto.ChatMessageRequestDto;
import org.scoula.domain.chat.dto.ChatRoomInfoDto;
import org.scoula.domain.chat.dto.ChatRoomWithUserInfoDto;

/**
 * 채팅 서비스 인터페이스
 *
 * <p>실시간 채팅, 채팅방 관리, 메시지 처리 등 채팅 관련 비즈니스 로직의 계약을 정의합니다. WebSocket 기반의 실시간 메시징과 MongoDB를 이용한 메시지 저장,
 * MySQL을 이용한 채팅방 메타데이터 관리를 담당합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface ChatServiceInterface {

      /**
       * 채팅 메시지를 처리합니다.
       *
       * <p>WebSocket을 통해 받은 채팅 메시지를 MongoDB에 저장하고, 채팅방의 마지막 메시지 정보를 업데이트한 후, 해당 채팅방의 모든 참여자에게 실시간으로
       * 메시지를 전송합니다. 욕설 필터링, 읽음 처리, 알림 전송 등의 부가 기능도 수행합니다.
       *
       * @param dto 채팅 메시지 요청 DTO (채팅방ID, 송신자ID, 수신자ID, 메시지 타입, 내용, 파일URL 등)
       * @throws IllegalArgumentException 필수 파라미터가 누락된 경우
       * @throws RuntimeException MongoDB 저장 실패 또는 WebSocket 전송 실패
       */
      void handleChatMessage(ChatMessageRequestDto dto);

      /**
       * 새로운 채팅방을 생성합니다.
       *
       * <p>매물 소유자와 구매 희망자 간의 1:1 채팅방을 생성합니다. 동일한 매물에 대해 같은 사용자 간의 채팅방이 이미 존재하는 경우 기존 채팅방 ID를 반환하여 중복
       * 생성을 방지합니다.
       *
       * @param ownerId 매물 소유자 ID (null 불가)
       * @param buyerId 구매 희망자 ID (null 불가, ownerId와 달라야 함)
       * @param propertyId 매물 ID (null 불가, 존재하는 매물이어야 함)
       * @return 생성된 채팅방 ID (기존 채팅방이 있는 경우 기존 채팅방 ID)
       * @throws IllegalArgumentException 파라미터가 null이거나 유효하지 않은 경우
       * @throws RuntimeException 채팅방 생성 실패
       */
      Long createChatRoom(Long ownerId, Long buyerId, Long propertyId);

      /**
       * 매물 소유자로서 참여 중인 채팅방 목록을 조회합니다.
       *
       * <p>해당 사용자가 매물 소유자로 참여하고 있는 모든 채팅방을 마지막 메시지 시간 기준으로 내림차순 정렬하여 반환합니다. 각 채팅방의 상대방(구매자) 정보, 읽지 않은
       * 메시지 수, 마지막 메시지 등의 정보가 함께 포함됩니다.
       *
       * @param ownerId 매물 소유자 ID (null 불가)
       * @return 상대방 정보가 포함된 채팅방 목록 (빈 리스트 가능)
       * @throws IllegalArgumentException ownerId가 null인 경우
       */
      List<ChatRoomWithUserInfoDto> getRoomsAsOwner(Long ownerId);

      /**
       * 구매 희망자로서 참여 중인 채팅방 목록을 조회합니다.
       *
       * <p>해당 사용자가 구매 희망자로 참여하고 있는 모든 채팅방을 마지막 메시지 시간 기준으로 내림차순 정렬하여 반환합니다. 각 채팅방의 상대방(소유자) 정보, 읽지 않은
       * 메시지 수, 마지막 메시지 등의 정보가 함께 포함됩니다.
       *
       * @param buyerId 구매 희망자 ID (null 불가)
       * @return 상대방 정보가 포함된 채팅방 목록 (빈 리스트 가능)
       * @throws IllegalArgumentException buyerId가 null인 경우
       */
      List<ChatRoomWithUserInfoDto> getRoomsAsBuyer(Long buyerId);

      /**
       * 특정 채팅방의 메시지 목록을 조회합니다.
       *
       * <p>MongoDB에서 해당 채팅방의 모든 메시지를 시간순으로 조회하고, 요청한 사용자의 읽지 않은 메시지를 자동으로 읽음 처리합니다. 텍스트 메시지와 파일 메시지
       * 모두 포함됩니다.
       *
       * @param chatRoomId 채팅방 ID (null 불가, 존재하는 채팅방이어야 함)
       * @param userId 요청하는 사용자 ID (null 불가, 채팅방 참여자여야 함)
       * @return 채팅 메시지 목록 (시간순 정렬, 빈 리스트 가능)
       * @throws IllegalArgumentException 파라미터가 null이거나 사용자가 채팅방에 참여하지 않은 경우
       * @throws RuntimeException MongoDB 조회 실패
       */
      List<ChatMessageDocument> getMessages(Long chatRoomId, Long userId);

      /**
       * 매물 ID로 매물 소유자 ID를 조회합니다.
       *
       * <p><strong>임시 메서드:</strong> 매물 도메인이 완성되면 PropertyService로 이동 예정입니다. 현재는 home 테이블에서 직접 조회하여 매물
       * 소유자 정보를 반환합니다.
       *
       * @param propertyId 매물 ID (null 불가, 존재하는 매물이어야 함)
       * @return 매물 소유자 ID
       * @throws IllegalArgumentException propertyId가 null이거나 존재하지 않는 매물인 경우
       * @deprecated 매물 도메인 완성 후 PropertyService.getOwnerId(propertyId)로 대체 예정
       */
      Long getPropertyOwnerId(Long propertyId);

      /**
       * 사용자가 해당 채팅방에 참여했는지 확인합니다.
       *
       * <p>채팅방의 소유자(owner) 또는 구매자(buyer) 중 하나와 일치하는지 확인하여 해당 사용자가 채팅방에 접근할 권한이 있는지 검증합니다. 보안 검증 및 권한
       * 체크에 사용됩니다.
       *
       * @param chatRoomId 채팅방 ID (null 불가)
       * @param userId 확인할 사용자 ID (null 불가)
       * @return 참여 여부 (true: 참여 중, false: 참여하지 않음)
       * @throws IllegalArgumentException 파라미터가 null인 경우
       */
      boolean isUserInChatRoom(Long chatRoomId, Long userId);

      /**
       * 동일한 조건의 채팅방이 이미 존재하는지 확인합니다.
       *
       * <p>같은 매물에 대해 동일한 소유자와 구매자 간의 채팅방이 이미 존재하는지 확인합니다. 중복 채팅방 생성을 방지하는 데 사용되며, 채팅방 생성 전에 반드시 호출되어야
       * 합니다.
       *
       * @param ownerId 매물 소유자 ID (null 불가)
       * @param buyerId 구매 희망자 ID (null 불가)
       * @param propertyId 매물 ID (null 불가)
       * @return 채팅방 존재 여부 (true: 존재함, false: 존재하지 않음)
       * @throws IllegalArgumentException 파라미터가 null인 경우
       */
      boolean existsChatRoom(Long ownerId, Long buyerId, Long propertyId);

      /**
       * 채팅방의 미디어 파일 목록을 조회합니다.
       *
       * <p>MongoDB에 저장된 채팅 메시지 중 파일 타입의 메시지만 필터링하여 이미지와 동영상 파일 목록을 페이지네이션으로 제공합니다. S3에서 파일 메타데이터(크기,
       * 타입 등)도 함께 조회하여 반환하며, 미디어 갤러리 기능에 사용됩니다.
       *
       * @param chatRoomId 채팅방 ID (null 불가, 존재하는 채팅방이어야 함)
       * @param page 페이지 번호 (0부터 시작, 음수 불가)
       * @param size 페이지 크기 (1 이상, 최대 100)
       * @param mediaType 미디어 타입 필터 ("ALL": 모든 미디어, "IMAGE": 이미지만, "VIDEO": 동영상만)
       * @return 미디어 파일 정보 목록 (파일 URL, 이름, 크기, 타입, 전송자, 전송 시간 등)
       * @throws IllegalArgumentException 파라미터가 유효하지 않은 경우
       * @throws RuntimeException MongoDB 조회 실패 또는 S3 메타데이터 조회 실패
       */
      List<Map<String, Object>> getChatMediaFiles(
              Long chatRoomId, int page, int size, String mediaType);

      /**
       * 채팅방의 상세 정보를 조회합니다.
       *
       * <p>채팅방 기본 정보와 함께 연결된 매물의 상세 정보(주소, 가격, 타입, 이미지 등)를 함께 조회하여 반환합니다. 채팅방 헤더에 매물 정보를 표시하는 용도로
       * 사용됩니다.
       *
       * @param chatRoomId 채팅방 ID (null 불가, 존재하는 채팅방이어야 함)
       * @param userId 요청하는 사용자 ID (null 불가, 채팅방 참여자여야 함)
       * @return 채팅방 및 매물 상세 정보 DTO
       * @throws IllegalArgumentException 파라미터가 null이거나 사용자가 채팅방에 참여하지 않은 경우
       * @throws RuntimeException 채팅방 또는 매물 정보 조회 실패
       */
      ChatRoomInfoDto getChatRoomInfo(Long chatRoomId, Long userId);
}
