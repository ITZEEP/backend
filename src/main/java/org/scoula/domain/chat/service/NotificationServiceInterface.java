package org.scoula.domain.chat.service;

import java.util.List;
import java.util.Map;

import org.scoula.domain.chat.dto.NotificationDto;
import org.scoula.domain.chat.dto.NotificationListResponseDto;

/**
 * 알림 서비스 인터페이스
 *
 * <p>사용자에게 다양한 유형의 알림을 생성, 전송, 관리하는 기능을 제공하는 서비스입니다. 채팅 메시지 알림, 계약 관련 알림, 시스템 알림 등을 처리하며,
 * FCM(Firebase Cloud Messaging)을 통한 실시간 푸시 알림과 데이터베이스 저장을 담당합니다. 알림 읽음 상태 관리, 페이징 조회, 백그라운드 처리 등의
 * 기능을 지원합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public interface NotificationServiceInterface {

      /**
       * 채팅 메시지 알림을 생성하고 전송합니다.
       *
       * <p>사용자가 채팅 메시지를 받았을 때 FCM을 통한 푸시 알림을 전송하고 데이터베이스에 저장합니다. 알림에는 발신자 정보, 메시지 내용, 채팅방 정보 등이 포함되며,
       * 사용자가 앱을 사용 중이지 않을 때 실시간으로 알림을 받을 수 있도록 합니다.
       *
       * @param userId 알림을 받을 사용자 ID (null 불가, 존재하는 사용자여야 함)
       * @param senderName 메시지 발신자의 이름 (null 불가, 빈 문자열 불가)
       * @param message 채팅 메시지 내용 (null 불가, 알림에 표시될 내용)
       * @param chatRoomId 채팅방 ID (null 불가, 존재하는 채팅방이어야 함)
       * @param fcmData FCM 전송을 위한 추가 데이터 맵 (선택사항, null 가능)
       * @throws IllegalArgumentException 필수 파라미터가 null이거나 유효하지 않은 경우
       * @throws RuntimeException FCM 전송 실패 또는 데이터베이스 저장 실패
       */
      void createChatNotification(
              Long userId,
              String senderName,
              String message,
              Long chatRoomId,
              Map<String, String> fcmData);

      /**
       * 사용자의 알림 목록을 페이징으로 조회합니다.
       *
       * <p>특정 사용자의 모든 알림을 최신순으로 정렬하여 페이지 단위로 조회합니다. 읽음/읽지 않음 상태, 알림 유형, 생성 시간 등의 정보를 포함하며, 클라이언트에서 무한
       * 스크롤이나 페이지네이션을 구현할 때 사용됩니다.
       *
       * @param userId 조회할 사용자 ID (null 불가, 존재하는 사용자여야 함)
       * @param page 페이지 번호 (0부터 시작, 음수 불가)
       * @param size 페이지당 항목 수 (1 이상, 일반적으로 10-50 권장)
       * @return 알림 목록과 페이징 정보를 포함한 응답 DTO
       * @throws IllegalArgumentException 파라미터가 유효하지 않은 경우
       * @throws RuntimeException 데이터베이스 조회 실패
       */
      NotificationListResponseDto getNotifications(Long userId, int page, int size);

      /**
       * 특정 유형의 알림 목록을 조회합니다.
       *
       * <p>사용자의 알림 중에서 특정 유형(채팅, 계약, 시스템)의 알림만 필터링하여 조회합니다. 사용자가 관심 있는 특정 카테고리의 알림만 확인하고 싶을 때 사용되며,
       * 알림 관리 화면에서 탭별로 구분하여 표시할 때 활용됩니다.
       *
       * @param userId 조회할 사용자 ID (null 불가, 존재하는 사용자여야 함)
       * @param type 알림 유형 (CHAT, CONTRACT, SYSTEM 등, null 불가)
       * @param page 페이지 번호 (0부터 시작, 음수 불가)
       * @param size 페이지당 항목 수 (1 이상, 일반적으로 10-50 권장)
       * @return 해당 유형의 알림 목록과 페이징 정보를 포함한 응답 DTO
       * @throws IllegalArgumentException 파라미터가 유효하지 않은 경우
       * @throws RuntimeException 데이터베이스 조회 실패
       */
      NotificationListResponseDto getNotificationsByType(
              Long userId, String type, int page, int size);

      /**
       * 사용자의 읽지 않은 알림 수를 조회합니다.
       *
       * <p>특정 사용자가 아직 읽지 않은 알림의 총 개수를 반환합니다. 앱의 알림 뱃지나 카운터 표시에 사용되며, 사용자가 확인해야 할 새로운 알림이 있는지 한눈에 파악할
       * 수 있도록 합니다.
       *
       * @param userId 조회할 사용자 ID (null 불가, 존재하는 사용자여야 함)
       * @return 읽지 않은 알림 수 (0 이상의 정수)
       * @throws IllegalArgumentException 사용자 ID가 null이거나 유효하지 않은 경우
       * @throws RuntimeException 데이터베이스 조회 실패
       */
      int getUnreadCount(Long userId);

      /**
       * 사용자의 최신 알림을 제한된 개수만큼 조회합니다.
       *
       * <p>특정 사용자의 가장 최근 알림을 지정된 개수만큼 조회합니다. 대시보드나 메인 화면에서 최신 알림 미리보기를 제공할 때 사용되며, 전체 알림 목록으로 이동하기 전에
       * 간략한 정보를 확인할 수 있습니다.
       *
       * @param userId 조회할 사용자 ID (null 불가, 존재하는 사용자여야 함)
       * @param limit 조회할 알림 개수 (1 이상, 일반적으로 5-10 권장)
       * @return 최신 알림 목록 (시간순 정렬, 빈 리스트 가능)
       * @throws IllegalArgumentException 파라미터가 유효하지 않은 경우
       * @throws RuntimeException 데이터베이스 조회 실패
       */
      List<NotificationDto> getLatestNotifications(Long userId, int limit);

      /**
       * 특정 알림을 읽음 상태로 변경합니다.
       *
       * <p>사용자가 알림을 클릭하거나 확인했을 때 해당 알림의 읽음 상태를 업데이트합니다. 읽음 처리된 알림은 읽지 않은 알림 수에서 제외되며, UI에서 다른 스타일로
       * 표시됩니다.
       *
       * @param notiId 읽음 처리할 알림 ID (null 불가, 존재하는 알림이어야 함)
       * @throws IllegalArgumentException 알림 ID가 null이거나 존재하지 않는 경우
       * @throws RuntimeException 읽음 상태 업데이트 실패
       */
      void markAsRead(Long notiId);

      /**
       * 여러 알림을 일괄로 읽음 상태로 변경합니다.
       *
       * <p>사용자가 선택한 여러 개의 알림을 한 번에 읽음 처리합니다. 대량의 알림을 효율적으로 관리할 수 있도록 하며, 체크박스를 통한 다중 선택 기능과 함께 사용됩니다.
       *
       * @param notiIds 읽음 처리할 알림 ID 목록 (null 불가, 빈 리스트 가능)
       * @throws IllegalArgumentException 알림 ID 목록이 null이거나 잘못된 ID가 포함된 경우
       * @throws RuntimeException 일괄 읽음 처리 실패
       */
      void markAsRead(List<Long> notiIds);

      /**
       * 사용자의 모든 알림을 읽음 상태로 변경합니다.
       *
       * <p>특정 사용자의 모든 읽지 않은 알림을 한 번에 읽음 처리합니다. '모두 읽음' 기능을 제공하여 사용자가 쌓인 알림을 빠르게 정리할 수 있도록 하며, 알림 카운터가
       * 0으로 초기화됩니다.
       *
       * @param userId 대상 사용자 ID (null 불가, 존재하는 사용자여야 함)
       * @throws IllegalArgumentException 사용자 ID가 null이거나 유효하지 않은 경우
       * @throws RuntimeException 전체 읽음 처리 실패
       */
      void markAllAsRead(Long userId);

      /**
       * 특정 알림을 삭제합니다.
       *
       * <p>사용자가 더 이상 필요하지 않은 알림을 개별적으로 삭제합니다. 삭제된 알림은 데이터베이스에서 완전히 제거되며, 복구할 수 없습니다. 일반적으로 읽음 처리와 함께
       * 제공되는 관리 기능입니다.
       *
       * @param notiId 삭제할 알림 ID (null 불가, 존재하는 알림이어야 함)
       * @throws IllegalArgumentException 알림 ID가 null이거나 존재하지 않는 경우
       * @throws RuntimeException 알림 삭제 실패
       */
      void deleteNotification(Long notiId);

      /**
       * 여러 알림을 일괄로 삭제합니다.
       *
       * <p>사용자가 선택한 여러 개의 알림을 한 번에 삭제합니다. 체크박스 선택을 통한 다중 삭제 기능을 지원하며, 대량의 알림을 효율적으로 관리할 수 있도록 합니다.
       *
       * @param notiIds 삭제할 알림 ID 목록 (null 불가, 빈 리스트 가능)
       * @throws IllegalArgumentException 알림 ID 목록이 null이거나 잘못된 ID가 포함된 경우
       * @throws RuntimeException 일괄 삭제 실패
       */
      void deleteNotifications(List<Long> notiIds);

      /**
       * 사용자의 모든 알림을 삭제합니다.
       *
       * <p>특정 사용자의 모든 알림을 데이터베이스에서 완전히 삭제합니다. 사용자가 알림 히스토리를 초기화하고 싶을 때 사용되며, 삭제된 데이터는 복구할 수 없으므로 신중하게
       * 사용해야 합니다.
       *
       * @param userId 대상 사용자 ID (null 불가, 존재하는 사용자여야 함)
       * @throws IllegalArgumentException 사용자 ID가 null이거나 유효하지 않은 경우
       * @throws RuntimeException 전체 알림 삭제 실패
       */
      void deleteAllNotifications(Long userId);
}
