package org.scoula.global.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 공통 에러 코드 시스템 전반에서 사용되는 공통적인 에러 코드들을 정의 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements IErrorCode {

      // 일반적인 요청 오류
      INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
      METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드입니다"),
      ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다"),
      INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "내부 서버 오류가 발생했습니다"),
      INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "타입이 올바르지 않습니다"),
      HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "접근이 거부되었습니다"),
      INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "C007", "JSON 형식이 올바르지 않습니다"),

      // 파일 관련 오류
      FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일 업로드에 실패했습니다"),
      FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F002", "파일 다운로드에 실패했습니다"),
      FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "파일 삭제에 실패했습니다"),
      FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F004", "파일을 찾을 수 없습니다"),
      FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "F005", "파일 크기가 제한을 초과했습니다"),
      FILE_ACCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F006", "파일 접근에 실패했습니다"),
      INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "F007", "지원하지 않는 파일 형식입니다"),

      // 토큰 관련 오류
      INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "T001", "유효하지 않은 토큰입니다"),

      // 접근 권한 관련 오류
      UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "A001", "접근 권한이 없습니다"),

      // 데이터베이스 관련 오류
      DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "D001", "데이터베이스 접근 중 오류가 발생했습니다"),
      DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "D002", "데이터 무결성 제약 조건을 위반했습니다"),
      DUPLICATE_KEY_ERROR(HttpStatus.CONFLICT, "D003", "중복된 키 값입니다"),

      // 외부 서비스 오류
      EXTERNAL_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E001", "외부 서비스 오류가 발생했습니다"),
      SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "E002", "서비스를 사용할 수 없습니다"),
      OPERATION_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "E003", "작업이 시간 초과되었습니다"),

      // 이메일 관련 오류
      INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "V003", "올바른 이메일 형식이 아닙니다"),
      EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M001", "이메일 전송에 실패했습니다"),

      // 인증 관련 오류
      AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A002", "인증에 실패했습니다"),
      ACCESS_DENIED(HttpStatus.FORBIDDEN, "A003", "접근 권한이 없습니다"),
      TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A004", "토큰이 만료되었습니다");

      private final HttpStatus httpStatus;
      private final String code;
      private final String message;
}
