package org.scoula.domain.chat.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements IErrorCode {
      // 기본 에러
      INVALID_INPUT_VALUE("CHAT_001", HttpStatus.BAD_REQUEST, "잘못된 입력값입니다"),

      // 채팅방 관련 에러
      CHAT_ROOM_NOT_FOUND("CHAT_002", HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"),
      CHAT_ROOM_ALREADY_EXISTS("CHAT_003", HttpStatus.CONFLICT, "이미 존재하는 채팅방입니다"),
      CHAT_ROOM_ACCESS_DENIED("CHAT_004", HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다"),
      SELF_CHAT_NOT_ALLOWED("CHAT_005", HttpStatus.BAD_REQUEST, "자신의 매물에는 채팅을 할 수 없습니다"),

      // 사용자/매물 관련 에러
      USER_NOT_FOUND("CHAT_006", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
      PROPERTY_NOT_FOUND("CHAT_007", HttpStatus.NOT_FOUND, "매물을 찾을 수 없습니다"),

      // 메시지 관련 에러
      MESSAGE_SEND_FAILED("CHAT_008", HttpStatus.INTERNAL_SERVER_ERROR, "메시지 전송에 실패했습니다"),
      BAD_WORD_DETECTED("CHAT_009", HttpStatus.BAD_REQUEST, "비속어가 포함되어 있습니다"),

      // 파일 업로드 관련 에러
      FILE_UPLOAD_FAILED("CHAT_010", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다"),
      FILE_NOT_FOUND("CHAT_011", HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"),
      FILE_SIZE_EXCEEDED("CHAT_012", HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다"),
      UNSUPPORTED_FILE_TYPE("CHAT_013", HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다"),

      // 계약 채팅 관련 에러
      CONTRACT_CHAT_NOT_FOUND("CHAT_020", HttpStatus.NOT_FOUND, "계약 채팅방을 찾을 수 없습니다"),
      CONTRACT_ALREADY_EXISTS("CHAT_021", HttpStatus.CONFLICT, "이미 존재하는 계약입니다"),
      START_POINT_NOT_SET("CHAT_022", HttpStatus.BAD_REQUEST, "특약 시작점이 설정되지 않았습니다"),
      END_POINT_NOT_SET("CHAT_023", HttpStatus.BAD_REQUEST, "특약 종료점이 설정되지 않았습니다"),
      NO_MESSAGES_FOUND("CHAT_024", HttpStatus.NOT_FOUND, "해당 기간에 메시지가 없습니다"),

      // 계약 요청/수락 관련 에러
      // ChatErrorCode enum에 추가
      ONLY_OWNER_CAN_REJECT_CONTRACT("CHAT_100", HttpStatus.FORBIDDEN, "매물 소유자만 계약을 거절할 수 있습니다."),
      ONLY_BUYER_CAN_REQUEST_CONTRACT("CHAT_030", HttpStatus.FORBIDDEN, "임차인만 계약을 요청할 수 있습니다"),
      ONLY_OWNER_CAN_ACCEPT_CONTRACT("CHAT_031", HttpStatus.FORBIDDEN, "임대인만 계약 요청을 수락할 수 있습니다"),
      CONTRACT_REQUEST_ALREADY_SENT("CHAT_032", HttpStatus.CONFLICT, "이미 계약 요청을 보냈습니다"),
      CONTRACT_REQUEST_NOT_FOUND("CHAT_033", HttpStatus.NOT_FOUND, "계약 요청을 찾을 수 없습니다"),

      // 온라인 상태 관련 에러
      USER_OFFLINE("CHAT_040", HttpStatus.FORBIDDEN, "상대방이 오프라인 상태입니다"),
      BOTH_USERS_MUST_BE_ONLINE("CHAT_041", HttpStatus.FORBIDDEN, "양쪽 사용자 모두 온라인 상태여야 합니다"),
      CONTRACT_CHAT_OFFLINE_RESTRICTION(
              "CHAT_042", HttpStatus.FORBIDDEN, "계약 채팅에서는 양측 모두 온라인 상태에서만 메시지를 보낼 수 있습니다"),
      RECEIVER_NOT_IN_CHATROOM("CHAT_043", HttpStatus.BAD_REQUEST, "수신자가 채팅방에 접속하지 않은 상태입니다"),

      // 권한 관련 에러
      INSUFFICIENT_PERMISSION("CHAT_050", HttpStatus.FORBIDDEN, "해당 작업을 수행할 권한이 없습니다"),
      OWNER_PERMISSION_REQUIRED("CHAT_051", HttpStatus.FORBIDDEN, "임대인 권한이 필요합니다"),
      BUYER_PERMISSION_REQUIRED("CHAT_052", HttpStatus.FORBIDDEN, "임차인 권한이 필요합니다"),

      // 특약 관련 에러
      SPECIAL_TERMS_NOT_STARTED("CHAT_060", HttpStatus.BAD_REQUEST, "특약 대화가 시작되지 않았습니다"),
      SPECIAL_TERMS_ALREADY_ENDED("CHAT_061", HttpStatus.CONFLICT, "특약 대화가 이미 종료되었습니다"),
      SPECIAL_TERMS_EXPORT_FAILED("CHAT_062", HttpStatus.INTERNAL_SERVER_ERROR, "특약 내용 내보내기에 실패했습니다"),
      END_REQUEST_ALREADY_PENDING("CHAT_063", HttpStatus.CONFLICT, "이미 종료 요청이 진행 중입니다"),
      END_REQUEST_EXPIRED("CHAT_064", HttpStatus.BAD_REQUEST, "종료 요청이 만료되었습니다"),
      CONTRACT_END_REQUEST_ALREADY_EXISTS("CHAT_013", HttpStatus.CONFLICT, "이미 특약 종료 요청이 진행 중입니다."),

      // ChatErrorCode enum에 추가
      CONTRACT_END_REQUEST_NOT_FOUND("CHAT_011", HttpStatus.NOT_FOUND, "특약 종료 요청을 찾을 수 없습니다."),
      CONTRACT_END_REQUEST_INVALID("CHAT_012", HttpStatus.BAD_REQUEST, "특약 종료 요청 정보가 유효하지 않습니다."),
      // 시스템 에러
      WEBSOCKET_CONNECTION_FAILED("CHAT_070", HttpStatus.INTERNAL_SERVER_ERROR, "웹소켓 연결에 실패했습니다"),
      DATABASE_CONNECTION_FAILED("CHAT_071", HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 연결에 실패했습니다"),
      REDIS_CONNECTION_FAILED("CHAT_072", HttpStatus.INTERNAL_SERVER_ERROR, "Redis 연결에 실패했습니다"),
      MONGODB_CONNECTION_FAILED("CHAT_073", HttpStatus.INTERNAL_SERVER_ERROR, "MongoDB 연결에 실패했습니다"),

      // 검증 에러
      INVALID_CHAT_ROOM_ID("CHAT_080", HttpStatus.BAD_REQUEST, "유효하지 않은 채팅방 ID입니다"),
      INVALID_USER_ID("CHAT_081", HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 ID입니다"),
      INVALID_MESSAGE_CONTENT("CHAT_082", HttpStatus.BAD_REQUEST, "유효하지 않은 메시지 내용입니다"),
      INVALID_FILE_FORMAT("CHAT_083", HttpStatus.BAD_REQUEST, "유효하지 않은 파일 형식입니다"),
      INVALID_DATE_RANGE("CHAT_084", HttpStatus.BAD_REQUEST, "유효하지 않은 날짜 범위입니다");

      private final String code;
      private final HttpStatus httpStatus;
      private final String message;
}
