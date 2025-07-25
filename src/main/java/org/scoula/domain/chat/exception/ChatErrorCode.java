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

      // 파일 업로드 관련 에러
      FILE_UPLOAD_FAILED("CHAT_009", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다"),
      FILE_NOT_FOUND("CHAT_010", HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"),
      FILE_SIZE_EXCEEDED("CHAT_011", HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다"),
      UNSUPPORTED_FILE_TYPE("CHAT_012", HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다"),
      BAD_WORD_DETECTED("CHAT_0013", HttpStatus.BAD_REQUEST, "비속어가 포함되어 있습니다.");

      private final String code;
      private final HttpStatus httpStatus;
      private final String message;
}
