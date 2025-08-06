package org.scoula.domain.chat.service;

import java.util.List;
import java.util.Map;

import org.scoula.domain.chat.document.ContractChatDocument;
import org.scoula.domain.chat.document.SpecialContractFixDocument;
import org.scoula.domain.chat.dto.ContractChatMessageRequestDto;
import org.scoula.domain.chat.dto.SpecialContractUserViewDto;
import org.scoula.domain.chat.vo.ContractChat;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.EntityNotFoundException;

/**
 * 계약 채팅 서비스 인터페이스
 *
 * <p>부동산 계약 관련 채팅 기능을 제공하는 서비스입니다. 일반 채팅과 구분되는 계약 전용 채팅방을 관리하며, 특약 사항 협의를 위한 대화 구간 설정 및 내보내기 기능을
 * 지원합니다. MongoDB를 이용한 메시지 저장과 계약 관련 메타데이터 관리를 담당합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface ContractChatServiceInterface {

      /**
       * 계약 채팅방을 생성합니다.
       *
       * <p>기존 일반 채팅방에서 계약이 성사되면 별도의 계약 전용 채팅방을 생성합니다. 계약 진행 상황, 특약 사항 협의, 문서 교환 등의 목적으로 사용되며, 일반 채팅과
       * 분리된 독립적인 채팅 공간을 제공합니다.
       *
       * @param chatRoomId 원본 채팅방 ID (null 불가, 존재하는 채팅방이어야 함)
       * @param userId 계약을 승인하는 사용자 ID (null 불가, 매물 소유자여야 함)
       * @return 생성된 계약 채팅방 ID
       * @throws IllegalArgumentException 파라미터가 null이거나 권한이 없는 경우
       * @throws RuntimeException 계약 채팅방 생성 실패
       */
      Long createContractChat(Long chatRoomId, Long userId);

      /**
       * 계약 채팅 메시지를 처리합니다.
       *
       * <p>계약 채팅방에서 발생한 메시지를 MongoDB에 저장하고, WebSocket을 통해 실시간으로 상대방에게 전송합니다. 계약 관련 파일 첨부, 특약 사항 메모,
       * 진행 상황 업데이트 등의 메시지를 처리합니다.
       *
       * @param dto 계약 채팅 메시지 요청 DTO (계약채팅방ID, 송신자ID, 수신자ID, 메시지 타입, 내용 등)
       * @throws IllegalArgumentException 필수 파라미터가 누락된 경우
       * @throws RuntimeException MongoDB 저장 실패 또는 WebSocket 전송 실패
       */
      void handleContractChatMessage(ContractChatMessageRequestDto dto);

      /**
       * 계약 채팅방의 메시지 목록을 조회합니다.
       *
       * <p>지정된 계약 채팅방의 메시지를 페이지네이션으로 조회합니다. 계약 진행 과정에서 주고받은 모든 대화 내용을 시간순으로 정렬하여 반환하며, 특약 구간 설정을 위한
       * 메시지 선택에도 사용됩니다.
       *
       * @param contractChatId 계약 채팅방 ID (null 불가, 존재하는 계약 채팅방이어야 함)
       * @return 계약 채팅 메시지 목록 (시간순 정렬, 빈 리스트 가능)
       * @throws IllegalArgumentException 파라미터가 유효하지 않거나 사용자가 채팅방에 참여하지 않은 경우
       * @throws RuntimeException MongoDB 조회 실패
       */
      List<ContractChatDocument> getContractMessages(Long contractChatId);

      /**
       * 특약 대화의 시작점을 설정합니다.
       *
       * <p>계약서에 포함될 특약 사항을 협의한 대화의 시작 지점을 지정합니다. 이후 종료점까지의 대화 내용이 특약 사항으로 추출되어 계약서에 반영됩니다. 시작점 설정 후에는
       * 해당 메시지에 마킹되어 사용자가 구간을 시각적으로 확인할 수 있습니다.
       *
       * @param contractChatId 계약 채팅방 ID (null 불가, 존재하는 계약 채팅방이어야 함)
       * @param userId 시작점을 설정하는 사용자 ID (null 불가, 계약 채팅방 참여자여야 함)
       * @return 시작점으로 설정된 메시지 ID
       * @throws IllegalArgumentException 파라미터가 null이거나 권한이 없는 경우
       * @throws RuntimeException 시작점 설정 실패
       */
      String setStartPoint(Long contractChatId, Long userId);

      /**
       * 특약 대화의 종료점을 설정하고 대화 내용을 내보냅니다.
       *
       * <p>특약 사항 협의 대화의 종료 지점을 설정하고, 시작점부터 종료점까지의 모든 대화 내용을 추출하여 반환합니다. 추출된 대화는 계약서의 특약 사항 작성에 활용되며,
       * PDF 또는 텍스트 형태로 내보낼 수 있습니다.
       *
       * @param contractChatId 계약 채팅방 ID (null 불가, 존재하는 계약 채팅방이어야 함)
       * @param userId 종료점을 설정하는 사용자 ID (null 불가, 계약 채팅방 참여자여야 함)
       * @return 시작점부터 종료점까지의 대화 내용 목록 (시간순 정렬)
       * @throws IllegalArgumentException 파라미터가 null이거나 권한이 없거나 시작점이 설정되지 않은 경우
       * @throws RuntimeException 종료점 설정 또는 대화 내용 추출 실패
       */
      boolean setEndPointAndExport(Long contractChatId, Long userId, Long order);

      /**
       * 사용자가 계약 채팅방에 참여했는지 확인합니다.
       *
       * <p>요청한 사용자가 해당 계약 채팅방에 접근할 권한이 있는지 확인합니다. 계약 채팅방의 소유자(매물 소유자) 또는 구매자 중 하나와 일치하는지 검증하여 보안을
       * 강화하고 무단 접근을 방지합니다.
       *
       * @param contractChatId 계약 채팅방 ID (null 불가)
       * @param userId 확인할 사용자 ID (null 불가)
       * @return 참여 여부 (true: 참여 중, false: 참여하지 않음)
       * @throws IllegalArgumentException 파라미터가 null인 경우
       */
      boolean isUserInContractChat(Long contractChatId, Long userId);

      /**
       * 계약 채팅방 입장 처리
       *
       * @param contractChatId 계약 채팅방 ID
       * @param userId 입장하는 사용자 ID
       */
      void enterContractChatRoom(Long contractChatId, Long userId);

      /**
       * 계약 채팅방 퇴장 처리
       *
       * @param contractChatId 계약 채팅방 ID
       * @param userId 퇴장하는 사용자 ID
       */
      void leaveContractChatRoom(Long contractChatId, Long userId);

      /**
       * 계약 채팅방 참여자들의 온라인 상태 조회
       *
       * @param contractChatId 계약 채팅방 ID
       * @param userId 요청하는 사용자 ID
       * @return 온라인 상태 정보 (ownerOnline, buyerOnline, bothOnline, canChat)
       */
      Map<String, Object> getContractChatOnlineStatus(Long contractChatId, Long userId);

      /**
       * 계약 채팅 메시지 전송 가능 여부 확인
       *
       * @param contractChatId 계약 채팅방 ID
       * @return 전송 가능 여부 (둘 다 온라인인 경우만 true)
       */
      boolean canSendContractMessage(Long contractChatId);

      /**
       * 계약 채팅 사용자 오프라인 처리
       *
       * @param userId 오프라인으로 설정할 사용자 ID
       * @param contractChatId 계약 채팅방 ID (선택사항)
       */
      void setContractUserOffline(Long userId, Long contractChatId);

      /**
       * 임대인이 임차인에게 특약 대화 종료 요청을 보냅니다.
       *
       * <p>이 요청은 WebSocket 메시지와 함께 MongoDB에 저장되며, 이후 임차인의 확인 후 종료 및 내보내기 처리로 이어집니다.
       *
       * @param contractChatId 계약 채팅방 ID (null 불가)
       * @param ownerId 임대인 사용자 ID (null 불가)
       * @throws IllegalArgumentException 파라미터가 null이거나 권한이 없는 경우
       * @throws RuntimeException 메시지 전송 실패
       */
      void requestEndPointExport(Long contractChatId, Long ownerId);

      /**
       * 임차인이 특약 대화 종료 요청을 거절합니다.
       *
       * <p>임차인은 임대인의 특약 종료 요청을 거절할 수 있으며, Redis에서 요청 정보를 삭제하고, 임대인에게 WebSocket 알림을 전송합니다.
       *
       * @param contractChatId 계약 채팅방 ID (null 불가)
       * @param buyerId 임차인 사용자 ID (null 불가)
       * @throws IllegalArgumentException 파라미터가 null이거나 권한이 없는 경우
       * @throws RuntimeException 처리 실패
       */
      void rejectEndPointExport(Long contractChatId, Long buyerId);

      /**
       * 계약 채팅방 정보를 조회합니다.
       *
       * <p>지정된 사용자 ID가 계약 채팅방의 owner 또는 buyer인지 권한 검증 후 정보를 반환합니다.
       *
       * @param contractChatId 계약 채팅방 ID (null 불가)
       * @param userId 요청 사용자 ID (null 불가, 접근 권한 확인용)
       * @return 계약 채팅방 정보 객체
       * @throws IllegalArgumentException 파라미터가 null인 경우
       * @throws BusinessException 사용자가 접근 권한이 없는 경우
       * @throws EntityNotFoundException 계약 채팅방이 존재하지 않는 경우
       */
      ContractChat getContractChatInfo(Long contractChatId, Long userId);

      // 특약 추출 메서드
      Object submitUserSelection(Long contractChatId, Long userId, Map<Integer, Boolean> selections);

      // 5. 서비스 구현체에 메서드 추가
      /**
       * 특약 문서 생성 (초기 껍데기)
       *
       * @param contractChatId 특약 대화 ID
       * @param order 순서 (특약 번호와 동일)
       * @return 생성된 특약 문서
       * @throws IllegalArgumentException 이미 존재하는 특약 대화 ID인 경우
       */
      SpecialContractFixDocument createSpecialContract(Long contractChatId, Long order);

      /**
       * 특약 문서 조회
       *
       * @param contractChatId 특약 대화 ID
       * @return 특약 문서
       * @throws IllegalArgumentException 문서를 찾을 수 없는 경우
       */
      SpecialContractFixDocument findSpecialContract(Long contractChatId);

      /**
       * recentData 업데이트
       *
       * @param contractChatId 특약 대화 ID
       * @param messages 대화 메시지
       * @return 업데이트된 특약 문서
       */
      SpecialContractFixDocument updateRecentData(Long contractChatId, Long order, String messages);

      /**
       * 특약 완료 처리
       *
       * @param contractChatId 특약 대화 ID
       * @return 업데이트된 특약 문서
       */
      SpecialContractFixDocument markSpecialContractAsPassed(Long contractChatId, Long order);

      /**
       * 특약 문서 존재 여부 확인
       *
       * @param contractChatId 특약 대화 ID
       * @return 존재 여부
       */
      boolean existsSpecialContract(Long contractChatId);

      /**
       * 완료된 특약 문서들 조회
       *
       * @return 완료된 특약 문서 리스트
       */
      List<SpecialContractFixDocument> getCompletedSpecialContracts();

      /**
       * 미완료 특약 문서들 조회
       *
       * @return 미완료 특약 문서 리스트
       */
      List<SpecialContractFixDocument> getIncompleteSpecialContractsByChat(
              Long contractChatId, Long userId);

      List<SpecialContractFixDocument> proceedAllIncompleteToNextRound(Long contractChatId);

      void createNextRoundSpecialContractDocument(
              Long contractChatId, List<Long> rejectedOrders, List<Long> passedOrders);

      SpecialContractUserViewDto getSpecialContractForUserByStatus(Long contractChatId, Long userId);

      Map<String, Object> getAllRoundsSpecialContract(Long contractChatId, Long userId);
}
